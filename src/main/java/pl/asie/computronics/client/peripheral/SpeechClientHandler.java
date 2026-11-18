package pl.asie.computronics.client.peripheral;

import com.mojang.text2speech.Narrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import pl.asie.computronics.Computronics;
import pl.asie.computronics.network.SpeechBoxPayload;

/**
 * Client-only: plays speech box text through the system text-to-speech engine,
 * but only if the local player is within the payload's audible range.
 * Note: the OS narrator is not positional and does not support per-message pitch/volume.
 */
public final class SpeechClientHandler {
    private static Narrator narrator;
    private static boolean failed;

    private SpeechClientHandler() {}

    private static Narrator narrator() {
        if (narrator == null && !failed) {
            try {
                narrator = Narrator.getNarrator();
            } catch (Throwable t) {
                failed = true;
                Computronics.LOGGER.warn("SpeechBox: text-to-speech unavailable: {}", t.toString());
            }
        }
        return narrator;
    }

    public static void handle(SpeechBoxPayload payload) {
        try {
            Narrator n = narrator();
            if (n == null || !n.active()) return;

            String text = payload.text();
            if (text == null || text.isEmpty()) {
                n.clear();
                return;
            }

            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null || payload.volume() <= 0.0f) return;
            double range = payload.range();
            if (player.distanceToSqr(Vec3.atCenterOf(payload.pos())) > range * range) return;

            n.say(text, false);
        } catch (Throwable t) {
            Computronics.LOGGER.warn("SpeechBox TTS playback error: {}", t.toString());
        }
    }
}
