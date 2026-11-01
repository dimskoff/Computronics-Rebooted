package pl.asie.computronics.block.entity;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import pl.asie.computronics.init.ModBlockEntities;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

public class TileSpeechBox extends TileEntityComputronics {
    private float volume = 1.0f;
    private float basePitch = 1.0f;
    private final Queue<Character> phonemeQueue = new ArrayDeque<>();
    private int phonemeTimer = 0;
    private String currentSpeech = "";

    public TileSpeechBox(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPEECH_BOX.get(), pos, state);
        node = Network.newNode(this, Visibility.Network)
                .withComponent("speech_box", Visibility.Network)
                .create();
    }

    public Direction getFacing() {
        if (getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return Direction.NORTH;
    }

    @Override
    public void tickServer(Level level, BlockPos pos, BlockState state) {
        super.tickServer(level, pos, state);
    }

    @Callback(doc = "function(text:string [, pitch:number]):boolean -- Speaks the text aloud via phonetic robotic voice synthesizer.")
    public Object[] say(Context context, Arguments args) {
        if (level == null || level.isClientSide) return new Object[]{false, "world unavailable"};

        String text = args.checkString(0);
        if (text.length() > 256) {
            text = text.substring(0, 256);
        }
        if (args.count() >= 2) {
            this.basePitch = (float) Math.max(0.5, Math.min(args.checkDouble(1), 2.0));
        }

        this.currentSpeech = text;

        // Broadcast real TTS to nearby players within 32 blocks
        if (level instanceof ServerLevel sl) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingChunk(sl, new net.minecraft.world.level.ChunkPos(worldPosition),
                    new pl.asie.computronics.network.SpeechBoxPayload(worldPosition, text, basePitch, volume));
        }

        return new Object[]{true};
    }

    @Callback(doc = "function():boolean -- Stops the current speech transmission.")
    public Object[] stop(Context context, Arguments args) {
        this.phonemeQueue.clear();
        this.currentSpeech = "";
        return new Object[]{true};
    }

    @Callback(direct = true, doc = "function():boolean -- Checks if the speech box is actively speaking.")
    public Object[] isProcessing(Context context, Arguments args) {
        return new Object[]{!phonemeQueue.isEmpty()};
    }

    @Callback(direct = true, doc = "function():number -- Gets the current voice pitch.")
    public Object[] getPitch(Context context, Arguments args) {
        return new Object[]{(double) basePitch};
    }

    @Callback(doc = "function(pitch:number):number -- Sets voice pitch (0.5 to 2.0).")
    public Object[] setPitch(Context context, Arguments args) {
        this.basePitch = (float) Math.max(0.5, Math.min(args.checkDouble(0), 2.0));
        setChanged();
        return new Object[]{(double) basePitch};
    }

    @Callback(direct = true, doc = "function():number -- Gets volume (0.0 to 1.0).")
    public Object[] getVolume(Context context, Arguments args) {
        return new Object[]{(double) volume};
    }

    @Callback(doc = "function(volume:number):number -- Sets volume (0.0 to 1.0).")
    public Object[] setVolume(Context context, Arguments args) {
        this.volume = (float) Math.max(0.0, Math.min(args.checkDouble(0), 1.0));
        setChanged();
        return new Object[]{(double) volume};
    }
}
