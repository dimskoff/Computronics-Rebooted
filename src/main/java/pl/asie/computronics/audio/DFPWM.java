package pl.asie.computronics.audio;

/**
 * DFPWM1a codec (Dynamic Filter Pulse Width Modulation), the 1-bit audio format used by
 * Computronics tapes and ComputerCraft speakers. One input byte holds 8 samples, LSB first.
 * <p>
 * This is a stateful codec: keep one instance per audio stream and feed it consecutive chunks.
 * Matches the reference implementation (PREC = 10, LPF strength 140) so files produced by
 * LionRay, ffmpeg ({@code -f dfpwm}) or CC:Tweaked play back identically.
 */
public final class DFPWM {
    private static final int PREC = 10;
    private static final int LPF_STRENGTH = 140;
    private static final int MIN_STRENGTH = 2 << (PREC - 8);

    private int charge;
    private int strength;
    private boolean previousBit;
    private int lowPassCharge;

    public void reset() {
        charge = 0;
        strength = 0;
        previousBit = false;
        lowPassCharge = 0;
    }

    /**
     * Decodes {@code src} into signed 8-bit PCM. The output has {@code src.length * 8} samples.
     */
    public byte[] decode(byte[] src) {
        byte[] out = new byte[src.length * 8];
        int o = 0;
        for (byte b : src) {
            int input = b & 0xFF;
            for (int i = 0; i < 8; i++) {
                boolean bit = (input & 1) != 0;
                input >>= 1;
                int target = bit ? 127 : -128;

                int nextCharge = charge + ((strength * (target - charge) + (1 << (PREC - 1))) >> PREC);
                if (nextCharge == charge && nextCharge != target) {
                    nextCharge += bit ? 1 : -1;
                }

                int z = bit == previousBit ? (1 << PREC) - 1 : 0;
                int nextStrength = strength;
                if (strength != z) {
                    nextStrength += bit == previousBit ? 1 : -1;
                }
                if (nextStrength < MIN_STRENGTH) {
                    nextStrength = MIN_STRENGTH;
                }

                // anti-jerk: average with the previous charge when the bit flips
                int blipped = bit == previousBit ? nextCharge : (nextCharge + charge + 1) >> 1;

                charge = nextCharge;
                strength = nextStrength;
                previousBit = bit;

                lowPassCharge += ((blipped - lowPassCharge) * LPF_STRENGTH + 0x80) >> 8;
                out[o++] = (byte) Math.max(-128, Math.min(127, lowPassCharge));
            }
        }
        return out;
    }

    /**
     * Encodes signed 8-bit PCM into DFPWM. {@code pcm.length} should be a multiple of 8.
     */
    public byte[] encode(byte[] pcm) {
        byte[] out = new byte[pcm.length / 8];
        int idx = 0;
        for (int i = 0; i < out.length; i++) {
            int d = 0;
            for (int j = 0; j < 8; j++) {
                int sample = pcm[idx++];
                boolean bit = sample > charge || (sample == charge && charge == 127);
                d = (d >> 1) | (bit ? 0x80 : 0);
                int target = bit ? 127 : -128;
                int nextCharge = charge + ((strength * (target - charge) + (1 << (PREC - 1))) >> PREC);
                if (nextCharge == charge && nextCharge != target) {
                    nextCharge += bit ? 1 : -1;
                }
                int z = bit == previousBit ? (1 << PREC) - 1 : 0;
                int nextStrength = strength;
                if (strength != z) {
                    nextStrength += bit == previousBit ? 1 : -1;
                }
                if (nextStrength < MIN_STRENGTH) {
                    nextStrength = MIN_STRENGTH;
                }
                charge = nextCharge;
                strength = nextStrength;
                previousBit = bit;
            }
            out[i] = (byte) d;
        }
        return out;
    }
}
