package pl.asie.computronics.oc.environment;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.PacketDistributor;
import pl.asie.computronics.network.BeepPayload;
import pl.asie.computronics.oc.OCData;

import java.util.Map;

/** Beep card, component "beep" (original Computronics API). Tones are synthesized client-side. */
public class EnvironmentCardBeep extends AbstractManagedEnvironment {
    /** Hardcoded: energy per second of each tone, audible range in blocks. */
    public static final double ENERGY_PER_TONE_SECOND = 2.0;
    public static final int RANGE = 32;

    private final EnvironmentHost host;
    private final long[] endTimes = new long[BeepPayload.MAX_TONES];

    public EnvironmentCardBeep(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Neighbors)
                .withComponent("beep", Visibility.Neighbors)
                .withConnector()
                .create());
    }

    private long now() {
        return host.getEnvironmentLevel() != null ? host.getEnvironmentLevel().getGameTime() : 0;
    }

    @Callback(direct = true, doc = "function():number -- Returns the number of beeps currently playing.")
    public Object[] getBeepCount(Context context, Arguments args) {
        long t = now();
        int count = 0;
        for (long end : endTimes) if (end > t) count++;
        return new Object[]{count};
    }

    @Callback(doc = "function(frequencies:table):boolean -- Plays tones of the form {[frequency]=duration,...} "
            + "(max 8 entries, 20-2000 Hz, 0.05-5 seconds, default 0.1).")
    public Object[] beep(Context context, Arguments args) {
        Map<?, ?> map = args.checkTable(0);
        if (map.isEmpty()) return new Object[]{false, "table is empty"};
        if (map.size() > BeepPayload.MAX_TONES) {
            throw new IllegalArgumentException("table must not contain more than 8 frequencies");
        }
        int n = map.size();
        int[] freqs = new int[n], durs = new int[n];
        int i = 0;
        double totalSeconds = 0;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!(e.getKey() instanceof Number key)) throw new IllegalArgumentException("frequency must be a number");
            double f = key.doubleValue();
            if (!(f >= 20 && f <= 2000)) throw new IllegalArgumentException("invalid frequency, must be in [20, 2000]");
            double d = e.getValue() instanceof Number v ? v.doubleValue() : 0.1;
            if (!Double.isFinite(d)) d = 0.1;
            d = Math.max(0.05, Math.min(5.0, d));
            freqs[i] = (int) Math.round(f);
            durs[i] = (int) Math.round(d * 1000);
            totalSeconds += d;
            i++;
        }

        long t = now();
        int free = 0;
        for (long end : endTimes) if (end <= t) free++;
        if (free < n) return new Object[]{false, "too many beeps playing"};
        if (!(host.getEnvironmentLevel() instanceof ServerLevel level)) return new Object[]{false, "world unavailable"};
        if (!OCData.tryConsume(node(), totalSeconds * ENERGY_PER_TONE_SECOND)) {
            return new Object[]{false, "not enough energy"};
        }

        int k = 0;
        for (int slot = 0; slot < endTimes.length && k < n; slot++) {
            if (endTimes[slot] <= t) endTimes[slot] = t + Math.max(1, Math.round(durs[k++] / 50.0));
        }
        PacketDistributor.sendToPlayersNear(level, null, host.xPosition(), host.yPosition(), host.zPosition(), RANGE,
                new BeepPayload(host.xPosition(), host.yPosition(), host.zPosition(), 1.0f, RANGE, freqs, durs));
        return new Object[]{true};
    }
}
