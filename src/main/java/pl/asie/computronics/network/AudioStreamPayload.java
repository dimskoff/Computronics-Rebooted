package pl.asie.computronics.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import pl.asie.computronics.Computronics;

/**
 * Server -> client chunk of DFPWM audio for a positional stream (tape drive).
 * An empty {@code data} array means "stop this stream now".
 *
 * @param streamId increments every time playback (re)starts, so the client resets its decoder
 */
public record AudioStreamPayload(BlockPos pos, int streamId, int sampleRate, float volume, int range, byte[] data)
        implements CustomPacketPayload {

    public static final Type<AudioStreamPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Computronics.MODID, "audio_stream"));

    public static final StreamCodec<FriendlyByteBuf, AudioStreamPayload> STREAM_CODEC =
            CustomPacketPayload.codec(AudioStreamPayload::write, AudioStreamPayload::new);

    public static AudioStreamPayload stop(BlockPos pos) {
        return new AudioStreamPayload(pos, 0, 0, 0f, 0, new byte[0]);
    }

    private AudioStreamPayload(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readVarInt(), buf.readByteArray(65536));
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(streamId);
        buf.writeVarInt(sampleRate);
        buf.writeFloat(volume);
        buf.writeVarInt(range);
        buf.writeByteArray(data);
    }

    public boolean isStop() {
        return data.length == 0;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AudioStreamPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> pl.asie.computronics.client.audio.ClientAudioManager.INSTANCE.accept(payload));
    }
}
