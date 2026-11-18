package pl.asie.computronics.block.entity;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import pl.asie.computronics.config.ComputronicsServerConfig;
import pl.asie.computronics.init.ModBlockEntities;
import pl.asie.computronics.init.ModSounds;
import pl.asie.computronics.item.ItemTape;
import pl.asie.computronics.network.AudioStreamPayload;
import pl.asie.computronics.tape.TapeStorage;

import java.nio.charset.StandardCharsets;

/**
 * Cassette tape drive. Lua API is compatible with the original Computronics {@code tape_drive}.
 * <p>
 * While PLAYING, the drive reads DFPWM data from the tape at 32768 samples/s * speed (4096 bytes/s)
 * and streams it to nearby clients, which decode and play it through OpenAL.
 */
public class TileTapeDrive extends TileEntityComputronics {
    public enum State { STOPPED, PLAYING, FORWARDING, REWINDING }

    /** DFPWM base sample rate: 32768 Hz = 4096 bytes per second at speed 1.0. */
    public static final int BASE_SAMPLE_RATE = 32768;
    private static final int PACKET_INTERVAL = 5; // ticks (= 1/4 s of audio per packet)
    private static final int PREBUFFER_PACKETS = 2; // extra audio sent on play() to absorb jitter
    private static final int SEEK_BYTES_PER_TICK = 2048; // fast-forward/rewind at ~10x real time

    private static final String TAG_POSITION = "tapePosition";

    private ItemStack tapeStack = ItemStack.EMPTY;
    private long insertTime;
    private State state = State.STOPPED;
    private int position;
    private float speed = 1.0f;
    private float volume = 1.0f;

    // transient playback state
    private int streamId;
    private double byteBudget;
    private int ticksUntilPacket;

    public TileTapeDrive(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TAPE_DRIVE.get(), pos, state);
        node = Network.newNode(this, Visibility.Network)
                .withComponent("tape_drive", Visibility.Network)
                .create();
    }

    // ------------------------------------------------------------------ accessors

    public ItemStack getTape() {
        return tapeStack;
    }

    public long getInsertTime() {
        return insertTime;
    }

    public State getState() {
        return state;
    }

    private TapeStorage storage() {
        if (tapeStack.isEmpty() || !(tapeStack.getItem() instanceof ItemTape itemTape)) return null;
        if (level == null || level.isClientSide) return null;
        return TapeStorage.get(ItemTape.getOrCreateStorageId(tapeStack), itemTape.getCapacity());
    }

    // ------------------------------------------------------------------ insertion / removal

    public ItemInteractionResult onInteract(Player player, InteractionHand hand) {
        if (level == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        ItemStack held = player.getItemInHand(hand);
        if (tapeStack.isEmpty() && held.getItem() instanceof ItemTape) {
            if (!level.isClientSide) insertTape(held.split(1));
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!tapeStack.isEmpty() && held.isEmpty()) {
            if (!level.isClientSide) player.setItemInHand(hand, removeTape());
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private void insertTape(ItemStack stack) {
        tapeStack = stack;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        position = data != null ? data.copyTag().getInt(TAG_POSITION) : 0;
        insertTime = level.getGameTime();
        setState(State.STOPPED);
        playSound(ModSounds.TAPE_INSERT.get());
        markUpdated();
        if (node != null) node.sendToReachable("computer.signal", "tape_inserted", node.address());
    }

    /** Removes the tape, remembering the head position on the item. */
    public ItemStack removeTape() {
        if (tapeStack.isEmpty()) return ItemStack.EMPTY;
        setState(State.STOPPED);
        ItemStack out = tapeStack;
        final int pos = position;
        CustomData.update(DataComponents.CUSTOM_DATA, out, tag -> tag.putInt(TAG_POSITION, pos));
        TapeStorage s = storage();
        if (s != null) s.save();
        tapeStack = ItemStack.EMPTY;
        position = 0;
        insertTime = 0;
        if (level != null) playSound(ModSounds.TAPE_EJECT.get());
        markUpdated();
        if (node != null) node.sendToReachable("computer.signal", "tape_removed", node.address());
        return out;
    }

    private void setState(State newState) {
        if (state == newState) return;
        State old = state;
        state = newState;
        if (old == State.PLAYING) sendStop();
        if (newState == State.PLAYING) {
            streamId++;
            byteBudget = bytesPerSecond() / 20.0 * PACKET_INTERVAL * PREBUFFER_PACKETS;
            ticksUntilPacket = 0;
        }
        markUpdated();
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tickServer(Level level, BlockPos pos, BlockState blockState) {
        super.tickServer(level, pos, blockState);
        switch (state) {
            case PLAYING -> tickPlayback((ServerLevel) level);
            case FORWARDING, REWINDING -> tickSeek();
            default -> { }
        }
    }

    private double bytesPerSecond() {
        return BASE_SAMPLE_RATE / 8.0 * speed;
    }

    private void tickPlayback(ServerLevel level) {
        TapeStorage s = storage();
        if (s == null) {
            setState(State.STOPPED);
            return;
        }
        byteBudget += bytesPerSecond() / 20.0;
        if (--ticksUntilPacket > 0) return;
        ticksUntilPacket = PACKET_INTERVAL;

        int count = (int) byteBudget;
        byteBudget -= count;
        byte[] chunk = s.read(position, count);
        position += chunk.length;
        if (chunk.length > 0) {
            int range = ComputronicsServerConfig.SERVER.tapeDriveAudioRadius.get();
            BlockPos p = getBlockPos();
            PacketDistributor.sendToPlayersNear(level, null, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                    range + 16,
                    new AudioStreamPayload(p, streamId, Math.round(BASE_SAMPLE_RATE * speed), volume, range, chunk));
        }
        if (chunk.length < count || position >= s.getCapacity()) {
            // reached end of tape
            state = State.STOPPED;
            markUpdated();
        }
        setChanged();
    }

    private void tickSeek() {
        TapeStorage s = storage();
        if (s == null) {
            setState(State.STOPPED);
            return;
        }
        int delta = state == State.FORWARDING ? SEEK_BYTES_PER_TICK : -SEEK_BYTES_PER_TICK;
        int target = Math.max(0, Math.min(s.getCapacity(), position + delta));
        position = target;
        if (target == 0 || target == s.getCapacity()) setState(State.STOPPED);
        setChanged();
    }

    private void sendStop() {
        if (level instanceof ServerLevel sl) {
            BlockPos p = getBlockPos();
            int range = ComputronicsServerConfig.SERVER.tapeDriveAudioRadius.get();
            PacketDistributor.sendToPlayersNear(sl, null, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                    range + 32, AudioStreamPayload.stop(p));
        }
    }

    @Override
    public void setRemoved() {
        if (state == State.PLAYING) sendStop();
        TapeStorage s = storage();
        if (s != null) s.save();
        super.setRemoved();
    }

    // ------------------------------------------------------------------ Lua API

    @Callback(direct = true, doc = "function():boolean -- Returns true if a tape is inserted.")
    public Object[] isReady(Context context, Arguments args) {
        return new Object[]{!tapeStack.isEmpty()};
    }

    @Callback(direct = true, doc = "function():boolean -- Returns true if the head is at the end of the tape.")
    public Object[] isEnd(Context context, Arguments args) {
        if (tapeStack.isEmpty()) return new Object[]{true};
        return new Object[]{position >= ((ItemTape) tapeStack.getItem()).getCapacity()};
    }

    @Callback(direct = true, doc = "function():number -- Returns the size of the tape in bytes.")
    public Object[] getSize(Context context, Arguments args) {
        if (tapeStack.isEmpty()) return new Object[]{0};
        return new Object[]{((ItemTape) tapeStack.getItem()).getCapacity()};
    }

    @Callback(direct = true, doc = "function():number -- Returns the current position of the tape head.")
    public Object[] getPosition(Context context, Arguments args) {
        return new Object[]{position};
    }

    @Callback(doc = "function(offset:number):number -- Seeks by the given amount of bytes (negative = backwards). Returns the amount actually moved.")
    public Object[] seek(Context context, Arguments args) {
        TapeStorage s = storage();
        if (s == null) return new Object[]{null, "no tape"};
        int offset = args.checkInteger(0);
        int target = (int) Math.max(0, Math.min((long) s.getCapacity(), (long) position + offset));
        int moved = target - position;
        position = target;
        if (state == State.PLAYING) {
            // restart the stream so the client decoder resyncs
            streamId++;
            byteBudget = bytesPerSecond() / 20.0 * PACKET_INTERVAL * PREBUFFER_PACKETS;
        }
        setChanged();
        return new Object[]{moved};
    }

    @Callback(doc = "function([length:number]):number or string -- Reads one byte (as a number) or up to length bytes (as a string).")
    public Object[] read(Context context, Arguments args) {
        TapeStorage s = storage();
        if (s == null) return new Object[]{null, "no tape"};
        boolean hasLength = args.count() > 0 && (args.isInteger(0) || args.isDouble(0));
        if (!hasLength) {
            int b = s.readByte(position);
            if (b < 0) return new Object[]{null};
            position++;
            setChanged();
            return new Object[]{b};
        }
        int length = Math.max(0, Math.min(args.checkInteger(0), 256 * 1024));
        byte[] data = s.read(position, length);
        position += data.length;
        setChanged();
        return new Object[]{data};
    }

    @Callback(doc = "function(data:number or string):number -- Writes a byte (number) or a string at the head position. Returns bytes written.")
    public Object[] write(Context context, Arguments args) {
        TapeStorage s = storage();
        if (s == null) return new Object[]{null, "no tape"};
        byte[] bytes;
        if (args.isInteger(0) || args.isDouble(0)) {
            bytes = new byte[]{(byte) args.checkInteger(0)};
        } else if (args.isByteArray(0)) {
            bytes = args.checkByteArray(0);
        } else {
            bytes = args.checkString(0).getBytes(StandardCharsets.ISO_8859_1);
        }
        int written = s.write(position, bytes);
        position += written;
        setChanged();
        return new Object[]{written};
    }

    @Callback(direct = true, doc = "function():string -- Returns STOPPED, PLAYING, FORWARDING or REWINDING.")
    public Object[] getState(Context context, Arguments args) {
        return new Object[]{state.name()};
    }

    @Callback(doc = "function():boolean -- Starts playback.")
    public Object[] play(Context context, Arguments args) {
        if (tapeStack.isEmpty()) return new Object[]{false, "no tape"};
        setState(State.PLAYING);
        return new Object[]{true};
    }

    @Callback(doc = "function():boolean -- Stops playback / seeking.")
    public Object[] stop(Context context, Arguments args) {
        setState(State.STOPPED);
        return new Object[]{true};
    }

    @Callback(doc = "function():boolean -- Fast-forwards until stopped or the end is reached.")
    public Object[] forward(Context context, Arguments args) {
        if (tapeStack.isEmpty()) return new Object[]{false, "no tape"};
        setState(State.FORWARDING);
        return new Object[]{true};
    }

    @Callback(doc = "function():boolean -- Stops and rewinds the tape to the beginning.")
    public Object[] rewind(Context context, Arguments args) {
        if (tapeStack.isEmpty()) return new Object[]{false, "no tape"};
        setState(State.STOPPED);
        position = 0;
        playSound(ModSounds.TAPE_REWIND.get());
        setChanged();
        return new Object[]{true};
    }

    @Callback(direct = true, doc = "function():string -- Returns the tape label.")
    public Object[] getLabel(Context context, Arguments args) {
        if (tapeStack.isEmpty()) return new Object[]{""};
        String label = ItemTape.getLabel(tapeStack);
        return new Object[]{label != null ? label : ""};
    }

    @Callback(doc = "function(label:string):string -- Sets the tape label.")
    public Object[] setLabel(Context context, Arguments args) {
        if (tapeStack.isEmpty()) return new Object[]{null, "no tape"};
        String label = args.checkString(0);
        if (label.length() > 64) label = label.substring(0, 64);
        ItemTape.setLabel(tapeStack, label);
        markUpdated();
        return new Object[]{label};
    }

    @Callback(doc = "function():boolean -- Ejects the tape in front of the drive.")
    public Object[] eject(Context context, Arguments args) {
        if (tapeStack.isEmpty() || level == null) return new Object[]{false};
        ItemStack out = removeTape();
        BlockPos p = getBlockPos();
        Containers.dropItemStack(level, p.getX() + 0.5, p.getY() + 0.75, p.getZ() + 0.5, out);
        return new Object[]{true};
    }

    @Callback(direct = true, doc = "function():number -- Returns the playback speed.")
    public Object[] getSpeed(Context context, Arguments args) {
        return new Object[]{(double) speed};
    }

    @Callback(doc = "function(speed:number):boolean -- Sets the playback speed (0.25 - 2.0).")
    public Object[] setSpeed(Context context, Arguments args) {
        double v = args.checkDouble(0);
        if (v < 0.25 || v > 2.0) return new Object[]{false, "speed must be between 0.25 and 2.0"};
        speed = (float) v;
        setChanged();
        return new Object[]{true};
    }

    @Callback(direct = true, doc = "function():number -- Returns the playback volume.")
    public Object[] getVolume(Context context, Arguments args) {
        return new Object[]{(double) volume};
    }

    @Callback(doc = "function(volume:number) -- Sets the playback volume (0.0 - 1.0).")
    public Object[] setVolume(Context context, Arguments args) {
        volume = (float) Math.max(0.0, Math.min(args.checkDouble(0), 1.0));
        setChanged();
        return new Object[]{true};
    }

    // ------------------------------------------------------------------ helpers

    private void playSound(SoundEvent sound) {
        if (level != null && !level.isClientSide) {
            level.playSound(null, getBlockPos(), sound, SoundSource.BLOCKS, 0.6f, 1.0f);
        }
    }

    private void markUpdated() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    // ------------------------------------------------------------------ persistence & sync

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!tapeStack.isEmpty()) tag.put("tape", tapeStack.save(registries));
        tag.putString("state", state == State.PLAYING ? State.PLAYING.name() : State.STOPPED.name());
        tag.putInt("position", position);
        tag.putFloat("speed", speed);
        tag.putFloat("volume", volume);
        TapeStorage s = storage();
        if (s != null) s.save();
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tapeStack = tag.contains("tape") ? ItemStack.parse(registries, tag.getCompound("tape")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        state = parseState(tag.getString("state"));
        if (tapeStack.isEmpty()) state = State.STOPPED;
        position = tag.getInt("position");
        speed = tag.contains("speed") ? tag.getFloat("speed") : 1.0f;
        volume = tag.contains("volume") ? tag.getFloat("volume") : 1.0f;
        if (state == State.PLAYING) {
            streamId++;
            ticksUntilPacket = 0;
        }
    }

    private static State parseState(String s) {
        try {
            return State.valueOf(s);
        } catch (IllegalArgumentException e) {
            return State.STOPPED;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (!tapeStack.isEmpty()) tag.put("tape", tapeStack.save(registries));
        tag.putLong("insertTime", insertTime);
        tag.putString("state", state.name());
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        tapeStack = tag.contains("tape") ? ItemStack.parse(registries, tag.getCompound("tape")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        insertTime = tag.getLong("insertTime");
        state = parseState(tag.getString("state"));
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        handleUpdateTag(pkt.getTag(), registries);
    }
}
