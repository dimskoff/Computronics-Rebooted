package pl.asie.computronics.block.entity;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import pl.asie.computronics.init.ModBlockEntities;
import pl.asie.computronics.item.ItemTape;
import pl.asie.computronics.tape.TapeStorage;

import java.nio.charset.StandardCharsets;

public class TileTapeDrive extends TileEntityComputronics {
    private ItemStack tapeStack = ItemStack.EMPTY;
    private String state = "STOPPED";

    public TileTapeDrive(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TAPE_DRIVE.get(), pos, state);
        node = Network.newNode(this, Visibility.Network)
                .withComponent("tape_drive", Visibility.Network)
                .create();
    }

    public ItemStack getTape() {
        return tapeStack;
    }

    public void setTape(ItemStack stack) {
        this.tapeStack = stack != null ? stack : ItemStack.EMPTY;
        setChanged();
    }

    private TapeStorage getStorage() {
        if (!tapeStack.isEmpty() && tapeStack.getItem() instanceof ItemTape itemTape) {
            String id = ItemTape.getOrCreateStorageId(tapeStack);
            return TapeStorage.get(id, itemTape.getCapacity());
        }
        return null;
    }

    public ItemInteractionResult onInteract(Player player, InteractionHand hand) {
        if (level == null || level.isClientSide) return ItemInteractionResult.SUCCESS;

        ItemStack held = player.getItemInHand(hand);
        if (tapeStack.isEmpty()) {
            if (!held.isEmpty() && held.getItem() instanceof ItemTape) {
                tapeStack = held.split(1);
                setChanged();
                return ItemInteractionResult.CONSUME;
            }
        } else {
            if (held.isEmpty()) {
                player.setItemInHand(hand, tapeStack.copy());
                tapeStack = ItemStack.EMPTY;
                state = "STOPPED";
                setChanged();
                return ItemInteractionResult.SUCCESS;
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Callback(direct = true, doc = "function():boolean -- Checks if a tape is inserted.")
    public Object[] isReady(Context context, Arguments args) {
        return result(!tapeStack.isEmpty());
    }

    @Callback(direct = true, doc = "function():number -- Gets the capacity of the inserted tape in bytes.")
    public Object[] getSize(Context context, Arguments args) {
        TapeStorage storage = getStorage();
        return result(storage != null ? storage.getCapacity() : 0);
    }

    @Callback(direct = true, doc = "function():number -- Gets the current position of the tape head.")
    public Object[] getPosition(Context context, Arguments args) {
        TapeStorage storage = getStorage();
        return result(storage != null ? storage.getPosition() : 0);
    }

    @Callback(doc = "function(offset:number):number -- Seeks by the specified amount of bytes.")
    public Object[] seek(Context context, Arguments args) {
        TapeStorage storage = getStorage();
        if (storage == null) return result(null, "no tape inserted");
        int offset = args.checkInteger(0);
        int actual = storage.seek(offset);
        return result(actual);
    }

    @Callback(doc = "function([count:number]):string -- Reads bytes from the current tape position.")
    public Object[] read(Context context, Arguments args) {
        TapeStorage storage = getStorage();
        if (storage == null) return result(null, "no tape inserted");
        int count = args.optInteger(0, 1024);
        count = Math.max(1, Math.min(count, 65536));
        byte[] bytes = storage.read(count);
        return result(bytes);
    }

    @Callback(doc = "function(data:string):number -- Writes bytes to the tape.")
    public Object[] write(Context context, Arguments args) {
        TapeStorage storage = getStorage();
        if (storage == null) return result(null, "no tape inserted");
        byte[] bytes;
        if (args.isByteArray(0)) {
            bytes = args.checkByteArray(0);
        } else {
            bytes = args.checkString(0).getBytes(StandardCharsets.UTF_8);
        }
        int written = storage.write(bytes);
        return result(written);
    }

    @Callback(direct = true, doc = "function():string -- Gets the current drive state.")
    public Object[] getState(Context context, Arguments args) {
        return result(state);
    }

    @Callback(doc = "function():boolean -- Starts playing the tape.")
    public Object[] play(Context context, Arguments args) {
        if (tapeStack.isEmpty()) return result(false, "no tape");
        this.state = "PLAYING";
        return result(true);
    }

    @Callback(doc = "function():boolean -- Stops the tape.")
    public Object[] stop(Context context, Arguments args) {
        this.state = "STOPPED";
        return result(true);
    }

    @Callback(doc = "function():boolean -- Rewinds the tape to the beginning.")
    public Object[] rewind(Context context, Arguments args) {
        TapeStorage storage = getStorage();
        if (storage == null) return result(false, "no tape");
        storage.setPosition(0);
        this.state = "STOPPED";
        return result(true);
    }

    @Callback(direct = true, doc = "function():string -- Gets the tape label.")
    public Object[] getLabel(Context context, Arguments args) {
        if (tapeStack.isEmpty()) return result(null, "no tape");
        return result(ItemTape.getLabel(tapeStack));
    }

    @Callback(doc = "function(label:string):string -- Sets the tape label.")
    public Object[] setLabel(Context context, Arguments args) {
        if (tapeStack.isEmpty()) return result(null, "no tape");
        String label = args.checkString(0);
        ItemTape.setLabel(tapeStack, label);
        setChanged();
        return result(label);
    }

    @Callback(doc = "function():boolean -- Ejects the tape into the world.")
    public Object[] eject(Context context, Arguments args) {
        if (tapeStack.isEmpty() || level == null) return result(false);
        Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, tapeStack);
        tapeStack = ItemStack.EMPTY;
        state = "STOPPED";
        setChanged();
        return result(true);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!tapeStack.isEmpty()) {
            tag.put("tape", tapeStack.save(registries));
        }
        tag.putString("state", state);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("tape")) {
            this.tapeStack = ItemStack.parse(registries, tag.getCompound("tape")).orElse(ItemStack.EMPTY);
        } else {
            this.tapeStack = ItemStack.EMPTY;
        }
        this.state = tag.getString("state");
    }
}
