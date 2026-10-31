package pl.asie.computronics.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

    public boolean isAreaJammed(Level level, BlockPos pos, int frequency) {
        for (IRFJammer jammer : activeJammers) {
            if (jammer.getJammerLevel() == level && jammer.isEnabled()) {
                if (jammer.getFrequency() == frequency || jammer.getFrequency() == 0) {
                    BlockPos jammerPos = jammer.getJammerPos();
                    if (jammerPos != null) {
                        double distSq = jammerPos.distSqr(pos);
                        double radius = jammer.getEffectiveRadius();
                        if (distSq <= radius * radius) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public Object[] applyJamming(Level level, BlockPos pos, int frequency, Object[] data) {
        if (!isAreaJammed(level, pos, frequency) || data == null || data.length == 0) {
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
