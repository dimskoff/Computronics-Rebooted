package pl.asie.computronics.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import pl.asie.computronics.Computronics;

/**
 * Server -> client: speak {@code text} at {@code pos}, audible within {@code range} blocks.
 * An empty text means "stop speaking".
 * The actual text-to-speech call lives in the client-only class
 * {@code pl.asie.computronics.client.peripheral.SpeechClientHandler}.
 */
public record SpeechBoxPayload(
        BlockPos pos,
        String text,
        float pitch,
        float volume,
        int range
) implements CustomPacketPayload {

    public static final int MAX_TEXT = 1024;

    public static final Type<SpeechBoxPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Computronics.MODID, "speech_box_say"));

    public static final StreamCodec<FriendlyByteBuf, SpeechBoxPayload> STREAM_CODEC = CustomPacketPayload.codec(
            SpeechBoxPayload::write,
            SpeechBoxPayload::new
    );

    public SpeechBoxPayload(FriendlyByteBuf buf) {
        this(
                buf.readBlockPos(),
                buf.readUtf(MAX_TEXT),
                buf.readFloat(),
                buf.readFloat(),
                buf.readVarInt()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        String t = text != null ? text : "";
        if (t.length() > MAX_TEXT) t = t.substring(0, MAX_TEXT);
        buf.writeUtf(t, MAX_TEXT);
        buf.writeFloat(pitch);
        buf.writeFloat(volume);
        buf.writeVarInt(range);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SpeechBoxPayload payload, IPayloadContext context) {
        // The lambda body only references the client class when executed on the client.
        context.enqueueWork(() -> pl.asie.computronics.client.peripheral.SpeechClientHandler.handle(payload));
    }
}
