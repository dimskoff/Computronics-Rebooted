package pl.asie.computronics.client.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import pl.asie.computronics.Computronics;
import pl.asie.computronics.network.BeepPayload;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * Client-side square-wave synthesizer for the beep card. Client main thread only.
 * Self-registers its tick/logout listeners on first use; {@link #init()} may be called early instead.
 */
public final class BeepSynth {
    public static final int SAMPLE_RATE = 32768;
    private static final int MAX_SOURCES = 16;
    private static final int AL_DISTANCE_MODEL = 0xD000;
    private static final int AL_LINEAR_DISTANCE = 0xD003;

    private record Voice(int source, int buffer, float volume) {}

    private static final ArrayDeque<Voice> VOICES = new ArrayDeque<>();
    private static boolean registered;
    private static boolean failed;

    private BeepSynth() {}

    /** Registers the client tick / logout listeners (idempotent). */
    public static void init() {
        if (registered) return;
        registered = true;
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> stopAll());
    }

    private static float categoryVolume() {
        return Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.RECORDS);
    }

    public static void play(BeepPayload p) {
        init();
        if (failed || p.frequencies().length == 0) return;
        try {
            byte[] pcm = synthesize(p.frequencies(), p.durationsMs());
            while (VOICES.size() >= MAX_SOURCES) release(VOICES.pollFirst());

            int source = AL10.alGenSources();
            int buffer = AL10.alGenBuffers();
            ByteBuffer buf = BufferUtils.createByteBuffer(pcm.length);
            buf.put(pcm).flip();
            AL10.alBufferData(buffer, AL10.AL_FORMAT_MONO8, buf, SAMPLE_RATE);
            AL10.alSourcei(source, AL10.AL_BUFFER, buffer);
            AL10.alSource3f(source, AL10.AL_POSITION, (float) p.x(), (float) p.y(), (float) p.z());
            AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, AL10.AL_FALSE);
            AL10.alSourcei(source, AL10.AL_LOOPING, AL10.AL_FALSE);
            AL10.alSourcei(source, AL_DISTANCE_MODEL, AL_LINEAR_DISTANCE);
            AL10.alSourcef(source, AL10.AL_MAX_DISTANCE, Math.max(1, p.range()));
            AL10.alSourcef(source, AL10.AL_ROLLOFF_FACTOR, 1f);
            AL10.alSourcef(source, AL10.AL_REFERENCE_DISTANCE, 0f);
            AL10.alSourcef(source, AL10.AL_GAIN, p.volume() * categoryVolume());
            int err = AL10.alGetError();
            if (err != AL10.AL_NO_ERROR) {
                AL10.alDeleteSources(source);
                AL10.alDeleteBuffers(buffer);
                AL10.alGetError();
                return;
            }
            AL10.alSourcePlay(source);
            VOICES.addLast(new Voice(source, buffer, p.volume()));
        } catch (Throwable t) {
            failed = true;
            Computronics.LOGGER.error("Computronics: beep synthesis failed, disabling beeps for this session", t);
            stopAll();
        }
    }

    /** Mixes all tones into one unsigned 8-bit mono buffer. */
    static byte[] synthesize(int[] freqs, int[] durMs) {
        int n = Math.min(freqs.length, durMs.length);
        int[] len = new int[n];
        int total = 0;
        for (int i = 0; i < n; i++) {
            len[i] = (int) ((long) Math.max(50, Math.min(5000, durMs[i])) * SAMPLE_RATE / 1000);
            total = Math.max(total, len[i]);
        }
        byte[] out = new byte[total];
        int fade = SAMPLE_RATE / 500; // 2 ms ramps against clicks
        float amp = 96f / Math.max(1, n);
        for (int s = 0; s < total; s++) {
            float mix = 0;
            for (int i = 0; i < n; i++) {
                if (s >= len[i]) continue;
                double phase = ((double) s * freqs[i] / SAMPLE_RATE) % 1.0;
                float v = phase < 0.5 ? 1f : -1f;
                int edge = Math.min(s, len[i] - 1 - s);
                if (edge < fade) v *= (float) edge / fade;
                mix += v * amp;
            }
            out[s] = (byte) (128 + Math.round(Math.max(-127, Math.min(127, mix))));
        }
        return out;
    }

    /** Frees finished sources and applies the current volume. Call once per client tick. */
    public static void tick() {
        if (VOICES.isEmpty()) return;
        if (Minecraft.getInstance().level == null) {
            stopAll();
            return;
        }
        float category = categoryVolume();
        Iterator<Voice> it = VOICES.iterator();
        while (it.hasNext()) {
            Voice v = it.next();
            if (AL10.alGetSourcei(v.source(), AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING) {
                AL10.alSourcef(v.source(), AL10.AL_GAIN, v.volume() * category);
            } else {
                it.remove();
                release(v);
            }
        }
        AL10.alGetError();
    }

    public static void stopAll() {
        while (!VOICES.isEmpty()) release(VOICES.pollFirst());
        failed = false;
    }

    private static void release(Voice v) {
        if (v == null) return;
        AL10.alSourceStop(v.source());
        AL10.alDeleteSources(v.source());
        AL10.alDeleteBuffers(v.buffer());
        AL10.alGetError();
    }
}
