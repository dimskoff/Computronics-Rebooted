package pl.asie.computronics.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import pl.asie.computronics.config.ComputronicsServerConfig;

import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of active RF jammers. Consulted by Computronics' own wireless features
 * (chat box / chat upgrade broadcasts, laser transceiver forwarding, speech).
 *
 * Limitation: OpenComputers' public API has no hook to intercept or veto its own
 * wireless network card / tunnel packets, so vanilla OC wireless traffic is NOT jammed.
 */
public class RFJammerManager {
    public static final RFJammerManager INSTANCE = new RFJammerManager();
    private final Set<IRFJammer> activeJammers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Random random = new Random();

    public void register(IRFJammer jammer) {
        if (jammer != null) {
            activeJammers.add(jammer);
        }
    }

    public void unregister(IRFJammer jammer) {
        if (jammer != null) {
            activeJammers.remove(jammer);
        }
    }

    public static double maxRange() {
        try {
            return ComputronicsServerConfig.SERVER.rfJammerMaxRange.get();
        } catch (Exception e) {
            return 64.0;
        }
    }

    /** Any active, powered jammer (any frequency) covering this position. */
    public boolean isJammed(Level level, BlockPos pos) {
        return isAreaJammed(level, pos, -1);
    }

    public boolean isJammed(Level level, Vec3 pos) {
        return level != null && pos != null && isJammed(level, BlockPos.containing(pos));
    }

    /**
     * @param frequency frequency to test; -1 matches every jammer regardless of its frequency.
     */
    public boolean isAreaJammed(Level level, BlockPos pos, int frequency) {
        if (level == null || pos == null || activeJammers.isEmpty()) {
            return false;
        }
        double cap = maxRange();
        for (IRFJammer jammer : activeJammers) {
            Level jl = jammer.getJammerLevel();
            if (jl == null || !jammer.isEnabled()) continue;
            if (jl != level && !jl.dimension().equals(level.dimension())) continue;
            if (frequency >= 0 && jammer.getFrequency() != 0 && jammer.getFrequency() != frequency) continue;
            BlockPos jammerPos = jammer.getJammerPos();
            if (jammerPos == null) continue;
            double radius = Math.min(cap, jammer.getEffectiveRadius());
            if (jammerPos.distSqr(pos) <= radius * radius) {
                return true;
            }
        }
        return false;
    }

    /** Corrupts payload data if the position is jammed on the given frequency. */
    public Object[] applyJamming(Level level, BlockPos pos, int frequency, Object[] data) {
        if (data == null || data.length == 0 || !isAreaJammed(level, pos, frequency)) {
            return data;
        }

        Object[] corrupted = new Object[data.length];
        for (int i = 0; i < data.length; i++) {
            Object obj = data[i];
            if (obj instanceof String str) {
                corrupted[i] = corruptString(str);
            } else if (obj instanceof byte[] bytes) {
                corrupted[i] = corruptBytes(bytes);
            } else if (obj instanceof Number num) {
                corrupted[i] = num.doubleValue() + (random.nextDouble() * 20.0 - 10.0);
            } else {
                corrupted[i] = obj;
            }
        }
        return corrupted;
    }

    private String corruptString(String input) {
        char[] chars = input.toCharArray();
        char[] noise = "!@#$%^&*?~_01".toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (random.nextFloat() < 0.35f) {
                chars[i] = noise[random.nextInt(noise.length)];
            }
        }
        return new String(chars);
    }

    private byte[] corruptBytes(byte[] input) {
        byte[] copy = input.clone();
        for (int i = 0; i < copy.length; i++) {
            if (random.nextFloat() < 0.35f) {
                copy[i] = (byte) (copy[i] ^ (1 << random.nextInt(8)));
            }
        }
        return copy;
    }
}
