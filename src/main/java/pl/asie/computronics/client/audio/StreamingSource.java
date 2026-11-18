package pl.asie.computronics.client.audio;

import net.minecraft.core.BlockPos;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import pl.asie.computronics.audio.DFPWM;
import pl.asie.computronics.network.AudioStreamPayload;

import java.nio.ByteBuffer;

/**
 * One OpenAL source fed with a queue of 8-bit mono buffers decoded from DFPWM.
 */
final class StreamingSource {
    // OpenAL ids also used by vanilla's Channel (per-source linear distance model).
    private static final int AL_DISTANCE_MODEL = 0xD000;
    private static final int AL_LINEAR_DISTANCE = 0xD003;

    private final BlockPos pos;
    private final DFPWM decoder = new DFPWM();
    private int source;
    private int streamId = Integer.MIN_VALUE;
    private float volume = 1f;
    private int idleTicks;
    private boolean pausedByGame;

    StreamingSource(BlockPos pos) {
        this.pos = pos;
        this.source = AL10.alGenSources();
        checkError("alGenSources");
        AL10.alSource3f(source, AL10.AL_POSITION, pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f);
        AL10.alSourcei(source, AL10.AL_LOOPING, AL10.AL_FALSE);
        AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, AL10.AL_FALSE);
        AL10.alGetError(); // clear anything left by the optional parameters below
    }

    void push(AudioStreamPayload payload, float categoryVolume) {
        if (payload.streamId() != streamId) {
            // new playback session (play pressed / seeked): restart decoder and drop stale audio
            streamId = payload.streamId();
            decoder.reset();
            AL10.alSourceStop(source);
            unqueueProcessed(true);
        }
        idleTicks = 0;
        volume = payload.volume();

        AL10.alSourcei(source, AL_DISTANCE_MODEL, AL_LINEAR_DISTANCE);
        AL10.alSourcef(source, AL10.AL_MAX_DISTANCE, Math.max(1, payload.range()));
        AL10.alSourcef(source, AL10.AL_ROLLOFF_FACTOR, 1f);
        AL10.alSourcef(source, AL10.AL_REFERENCE_DISTANCE, 0f);
        AL10.alSourcef(source, AL10.AL_GAIN, volume * categoryVolume);
        AL10.alGetError();

        unqueueProcessed(false);

        byte[] pcm = decoder.decode(payload.data());
        ByteBuffer buf = BufferUtils.createByteBuffer(pcm.length);
        for (byte b : pcm) buf.put((byte) (b ^ 0x80)); // AL_FORMAT_MONO8 is unsigned
        buf.flip();

        int buffer = AL10.alGenBuffers();
        AL10.alBufferData(buffer, AL10.AL_FORMAT_MONO8, buf, Math.max(1000, payload.sampleRate()));
        AL10.alSourceQueueBuffers(source, buffer);
        checkError("queue buffer");

        if (!pausedByGame && AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
            AL10.alSourcePlay(source);
        }
    }

    /** @return ticks since the last packet */
    int tick(float categoryVolume, boolean gamePaused) {
        idleTicks++;
        if (source == 0) return Integer.MAX_VALUE;
        AL10.alSourcef(source, AL10.AL_GAIN, volume * categoryVolume);
        if (gamePaused != pausedByGame) {
            pausedByGame = gamePaused;
            if (gamePaused) AL10.alSourcePause(source);
            else if (AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED) > 0) AL10.alSourcePlay(source);
        }
        unqueueProcessed(false);
        AL10.alGetError();
        return idleTicks;
    }

    private void unqueueProcessed(boolean all) {
        int count = AL10.alGetSourcei(source, all ? AL10.AL_BUFFERS_QUEUED : AL10.AL_BUFFERS_PROCESSED);
        if (all) {
            // detach every buffer: a stopped source marks all queued buffers as processed
            AL10.alSourceStop(source);
            count = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED);
        }
        for (int i = 0; i < count; i++) {
            int b = AL10.alSourceUnqueueBuffers(source);
            if (b != 0) AL10.alDeleteBuffers(b);
        }
    }

    void close() {
        if (source == 0) return;
        AL10.alSourceStop(source);
        unqueueProcessed(true);
        AL10.alDeleteSources(source);
        AL10.alGetError();
        source = 0;
    }

    BlockPos pos() {
        return pos;
    }

    private static void checkError(String what) {
        int err = AL10.alGetError();
        if (err != AL10.AL_NO_ERROR) {
            throw new IllegalStateException("OpenAL error " + Integer.toHexString(err) + " during " + what);
        }
    }
}
