package pl.asie.computronics.service;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import pl.asie.computronics.config.ComputronicsServerConfig;
import pl.asie.computronics.network.SpeechBoxPayload;

/**
 * Shared logic for the Speech Box block and the Speech upgrade.
 * say()/stop() must run on the server thread (non-direct callbacks).
 */
public final class SpeechService {
    /** Estimated speaking time per character, used for isProcessing(). */
    public static final long MS_PER_CHAR = 70L;
    public static final long BASE_MS = 300L;

    private SpeechService() {}

    public static final class SpeechState {
        private volatile float volume = 1.0f;
        private volatile float pitch = 1.0f;
        private volatile long speakingUntil = 0L;
        private long lastSayTick = Long.MIN_VALUE;

        public float getVolume() {
            return volume;
        }

        public float setVolume(double v) {
            if (Double.isNaN(v)) v = 1.0;
            this.volume = (float) Math.max(0.0, Math.min(v, 1.0));
            return volume;
        }

        public float getPitch() {
            return pitch;
        }

        public float setPitch(double p) {
            if (Double.isNaN(p)) p = 1.0;
            this.pitch = (float) Math.max(0.5, Math.min(p, 2.0));
            return pitch;
        }

        public boolean isProcessing() {
            return System.currentTimeMillis() < speakingUntil;
        }

        public void save(CompoundTag tag) {
            tag.putFloat("speechVolume", volume);
            tag.putFloat("speechPitch", pitch);
        }

        public void load(CompoundTag tag) {
            if (tag.contains("speechVolume")) setVolume(tag.getFloat("speechVolume"));
            if (tag.contains("speechPitch")) setPitch(tag.getFloat("speechPitch"));
        }
    }

    private static int maxRange() {
        try {
            return ComputronicsServerConfig.SERVER.speechBoxMaxRange.get();
        } catch (Exception e) {
            return 32;
        }
    }

    private static int maxTextLength() {
        try {
            return ComputronicsServerConfig.SERVER.speechBoxMaxTextLength.get();
        } catch (Exception e) {
            return 120;
        }
    }

    private static int cooldownTicks() {
        try {
            return ComputronicsServerConfig.SERVER.speechBoxCooldownTicks.get();
        } catch (Exception e) {
            return 40;
        }
    }

    private static double baseVolume() {
        try {
            return ComputronicsServerConfig.SERVER.speechBoxVolume.get();
        } catch (Exception e) {
            return 1.0;
        }
    }

    /** Audible radius: configured max range scaled by the device volume. */
    public static int audibleRange(SpeechState state) {
        return (int) Math.ceil(maxRange() * Math.max(0.0f, Math.min(state.getVolume(), 1.0f)));
    }

    public static Object[] say(SpeechState state, Level level, Vec3 pos, String text) {
        if (!(level instanceof ServerLevel sl) || pos == null) {
            return new Object[]{false, "world unavailable"};
        }
        if (state.isProcessing()) {
            return new Object[]{false, "already speaking"};
        }
        long now = level.getGameTime();
        int cd = cooldownTicks();
        if (cd > 0 && state.lastSayTick != Long.MIN_VALUE && now >= state.lastSayTick && now - state.lastSayTick < cd) {
            return new Object[]{false, "cooldown"};
        }
        String clean = ChatService.stripFormatting(text).replaceAll("\\p{Cntrl}", " ").trim();
        int maxLen = maxTextLength();
        boolean truncated = clean.length() > maxLen;
        if (truncated) clean = clean.substring(0, maxLen);
        if (clean.isEmpty()) {
            return new Object[]{false, "empty text"};
        }

        state.lastSayTick = now;
        state.speakingUntil = System.currentTimeMillis() + BASE_MS + MS_PER_CHAR * clean.length();

        int range = audibleRange(state);
        if (range > 0) {
            BlockPos bp = BlockPos.containing(pos);
            float vol = (float) (state.getVolume() * baseVolume());
            SpeechBoxPayload payload = new SpeechBoxPayload(bp, clean, state.getPitch(), vol, range);
            double r2 = (double) range * range;
            for (ServerPlayer player : sl.players()) {
                if (player.distanceToSqr(pos) <= r2) {
                    PacketDistributor.sendToPlayer(player, payload);
                }
            }
        }
        return truncated ? new Object[]{true, "text truncated"} : new Object[]{true};
    }

    public static Object[] stop(SpeechState state, Level level, Vec3 pos) {
        boolean wasSpeaking = state.isProcessing();
        state.speakingUntil = 0L;
        if (wasSpeaking && level instanceof ServerLevel sl && pos != null) {
            int range = Math.max(audibleRange(state), 1);
            SpeechBoxPayload payload = new SpeechBoxPayload(BlockPos.containing(pos), "", state.getPitch(), 0f, range);
            double r2 = (double) range * range;
            for (ServerPlayer player : sl.players()) {
                if (player.distanceToSqr(pos) <= r2) {
                    PacketDistributor.sendToPlayer(player, payload);
                }
            }
        }
        return new Object[]{true};
    }
}
