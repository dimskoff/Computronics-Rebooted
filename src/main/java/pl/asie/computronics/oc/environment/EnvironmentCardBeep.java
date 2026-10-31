package pl.asie.computronics.oc.environment;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

import java.util.Map;

public class EnvironmentCardBeep extends AbstractManagedEnvironment {
    private final EnvironmentHost host;
    private int activeBeeps = 0;

    public EnvironmentCardBeep(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Neighbors)
                .withComponent("beep", Visibility.Neighbors)
                .withConnector(250)
                .create());
    }

    @Callback(direct = true, doc = "function():number -- Returns the amount of beeps currently playing.")
    public Object[] getBeepCount(Context context, Arguments args) {
        return new Object[]{activeBeeps};
    }

    @Callback(direct = true, limit = 10, doc = "function(freqOrTable:number|table [, duration:number]):boolean -- Plays square-wave frequency beeps.")
    public Object[] beep(Context context, Arguments args) {
        Level world = host != null ? host.getEnvironmentLevel() : null;
        if (world == null || world.isClientSide) return new Object[]{false, "world unavailable"};

        if (args.isTable(0)) {
            Map<?, ?> map = args.checkTable(0);
            if (map.size() > 8) {
                return new Object[]{false, "table must not contain more than 8 frequencies"};
            }
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof Number numKey) {
                    double freq = numKey.doubleValue();
                    double dur = 0.1;
                    if (entry.getValue() instanceof Number numVal) {
                        dur = numVal.doubleValue();
                    }
                    playFrequencyBeep(world, freq, dur);
                }
            }
            return new Object[]{true};
        } else {
            double freq = args.checkDouble(0);
            double dur = args.optDouble(1, 0.1);
            playFrequencyBeep(world, freq, dur);
            return new Object[]{true};
        }
    }

    private void playFrequencyBeep(Level world, double frequency, double duration) {
        frequency = Math.max(20.0, Math.min(frequency, 2000.0));
        float pitch = (float) Math.max(0.5, Math.min(2.0, frequency / 440.0));
        world.playSound(null, host.xPosition(), host.yPosition(), host.zPosition(),
                SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 1.0f, pitch);
    }
}
