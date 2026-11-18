package pl.asie.computronics.oc.environment;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import pl.asie.computronics.config.ComputronicsServerConfig;
import pl.asie.computronics.service.SpeechService;

public class EnvironmentSpeechUpgrade extends AbstractManagedEnvironment {
    private static final String DATA_KEY = "computronics_speech";

    private final EnvironmentHost host;
    private final SpeechService.SpeechState speech = new SpeechService.SpeechState();

    public EnvironmentSpeechUpgrade(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network)
                .withComponent("speech_box", Visibility.Neighbors)
                .withConnector(250)
                .create());
    }

    private Level level() {
        return host != null ? host.getEnvironmentLevel() : null;
    }

    private Vec3 position() {
        // exact (double) host position -- no (int) truncation for negative coordinates
        return host != null ? new Vec3(host.xPosition(), host.yPosition(), host.zPosition()) : null;
    }

    private static boolean enabled() {
        try {
            return ComputronicsServerConfig.SERVER.enableSpeechUpgrade.get();
        } catch (Exception e) {
            return true;
        }
    }

    @Callback(doc = "function(text:string [, pitch:number]):boolean -- Speaks the text via the listeners' system text-to-speech (only players in range hear it).")
    public Object[] say(Context context, Arguments args) {
        if (!enabled()) {
            return new Object[]{false, "speech upgrade is disabled in server config"};
        }
        String text = args.checkString(0);
        if (args.count() >= 2) {
            speech.setPitch(args.checkDouble(1));
        }
        return SpeechService.say(speech, level(), position(), text);
    }

    @Callback(doc = "function():boolean -- Stops speech.")
    public Object[] stop(Context context, Arguments args) {
        return SpeechService.stop(speech, level(), position());
    }

    @Callback(direct = true, doc = "function():boolean -- Checks if (estimated to be) still speaking.")
    public Object[] isProcessing(Context context, Arguments args) {
        return new Object[]{speech.isProcessing()};
    }

    @Callback(direct = true, doc = "function():number -- Gets pitch.")
    public Object[] getPitch(Context context, Arguments args) {
        return new Object[]{(double) speech.getPitch()};
    }

    @Callback(doc = "function(pitch:number):number -- Sets pitch (0.5 - 2.0; the system TTS may ignore it).")
    public Object[] setPitch(Context context, Arguments args) {
        float p = speech.setPitch(args.checkDouble(0));
        if (host != null) host.markChanged();
        return new Object[]{(double) p};
    }

    @Callback(direct = true, doc = "function():number -- Gets volume (0.0 to 1.0).")
    public Object[] getVolume(Context context, Arguments args) {
        return new Object[]{(double) speech.getVolume()};
    }

    @Callback(doc = "function(volume:number):number -- Sets volume (0.0 to 1.0); scales the audible range.")
    public Object[] setVolume(Context context, Arguments args) {
        float v = speech.setVolume(args.checkDouble(0));
        if (host != null) host.markChanged();
        return new Object[]{(double) v};
    }

    @Override
    public void loadData(DataComponentHolder holder) throws li.cil.oc.api.UnrecoverablePersistanceException {
        super.loadData(holder);
        CustomData data = holder.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            CompoundTag root = data.copyTag();
            if (root.contains(DATA_KEY)) {
                speech.load(root.getCompound(DATA_KEY));
            }
        }
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        super.saveData(holder);
        CustomData existing = holder.get(DataComponents.CUSTOM_DATA);
        CompoundTag root = existing != null ? existing.copyTag() : new CompoundTag();
        CompoundTag mine = new CompoundTag();
        speech.save(mine);
        root.put(DATA_KEY, mine);
        holder.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }
}
