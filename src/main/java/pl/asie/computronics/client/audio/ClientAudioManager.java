package pl.asie.computronics.client.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import pl.asie.computronics.Computronics;
import pl.asie.computronics.network.AudioStreamPayload;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Owns all positional PCM streams (tape drives). Lives on the client main thread only.
 */
public final class ClientAudioManager {
    public static final ClientAudioManager INSTANCE = new ClientAudioManager();

    /** Streams with no new data for this long are torn down (ticks). */
    private static final int IDLE_TIMEOUT_TICKS = 60;

    private final Map<BlockPos, StreamingSource> sources = new HashMap<>();
    private boolean failed;

    private ClientAudioManager() {
    }

    public void accept(AudioStreamPayload payload) {
        if (failed) return;
        BlockPos pos = payload.pos().immutable();
        if (payload.isStop()) {
            StreamingSource s = sources.remove(pos);
            if (s != null) s.close();
            return;
        }
        try {
            StreamingSource source = sources.get(pos);
            if (source == null) {
                source = new StreamingSource(pos);
                sources.put(pos, source);
            }
            source.push(payload, categoryVolume());
        } catch (Throwable t) {
            // Never let an audio driver problem crash the client; disable streaming for the session.
            failed = true;
            Computronics.LOGGER.error("Computronics: audio streaming failed, disabling tape playback for this session", t);
            stopAll();
        }
    }

    private static float categoryVolume() {
        return Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.RECORDS);
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        if (sources.isEmpty()) return;
        if (Minecraft.getInstance().level == null) {
            stopAll();
            return;
        }
        float category = categoryVolume();
        boolean paused = Minecraft.getInstance().isPaused();
        Iterator<StreamingSource> it = sources.values().iterator();
        while (it.hasNext()) {
            StreamingSource s = it.next();
            if (s.tick(category, paused) > IDLE_TIMEOUT_TICKS) {
                s.close();
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        stopAll();
        failed = false;
    }

    public void stopAll() {
        sources.values().forEach(StreamingSource::close);
        sources.clear();
    }
}
