package pl.asie.computronics.block.entity;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import pl.asie.computronics.config.ComputronicsServerConfig;
import pl.asie.computronics.init.ModBlockEntities;
import pl.asie.computronics.service.SpeechService;

public class TileSpeechBox extends TileEntityComputronics {
    private final SpeechService.SpeechState speech = new SpeechService.SpeechState();

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

    private static boolean enabled() {
        try {
            return ComputronicsServerConfig.SERVER.enableSpeechBox.get();
        } catch (Exception e) {
            return true;
        }
    }

    @Callback(doc = "function(text:string [, pitch:number]):boolean -- Speaks the text via the listeners' system text-to-speech (only players in range hear it).")
    public Object[] say(Context context, Arguments args) {
        if (level == null || level.isClientSide) return new Object[]{false, "world unavailable"};
        if (!enabled()) {
            return new Object[]{false, "speech box is disabled in server config"};
        }
        String text = args.checkString(0);
        if (args.count() >= 2) {
            speech.setPitch(args.checkDouble(1));
            setChanged();
        }
        return SpeechService.say(speech, level, Vec3.atCenterOf(worldPosition), text);
    }

    @Callback(doc = "function():boolean -- Stops the current speech.")
    public Object[] stop(Context context, Arguments args) {
        return SpeechService.stop(speech, level, Vec3.atCenterOf(worldPosition));
    }

    @Callback(direct = true, doc = "function():boolean -- Checks if the speech box is (estimated to be) still speaking.")
    public Object[] isProcessing(Context context, Arguments args) {
        return new Object[]{speech.isProcessing()};
    }

    @Callback(direct = true, doc = "function():number -- Gets the current voice pitch.")
    public Object[] getPitch(Context context, Arguments args) {
        return new Object[]{(double) speech.getPitch()};
    }

    @Callback(doc = "function(pitch:number):number -- Sets voice pitch (0.5 to 2.0; the system TTS may ignore it).")
    public Object[] setPitch(Context context, Arguments args) {
        float p = speech.setPitch(args.checkDouble(0));
        setChanged();
        return new Object[]{(double) p};
    }

    @Callback(direct = true, doc = "function():number -- Gets volume (0.0 to 1.0).")
    public Object[] getVolume(Context context, Arguments args) {
        return new Object[]{(double) speech.getVolume()};
    }

    @Callback(doc = "function(volume:number):number -- Sets volume (0.0 to 1.0); scales the audible range.")
    public Object[] setVolume(Context context, Arguments args) {
        float v = speech.setVolume(args.checkDouble(0));
        setChanged();
        return new Object[]{(double) v};
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        speech.save(tag);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        speech.load(tag);
    }
}
