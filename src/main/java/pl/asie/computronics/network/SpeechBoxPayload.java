package pl.asie.computronics.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import pl.asie.computronics.Computronics;

public record SpeechBoxPayload(
        BlockPos pos,
        String text,
        float pitch,
        float volume
) implements CustomPacketPayload {

    public static final Type<SpeechBoxPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Computronics.MODID, "speech_box_say"));

    public static final StreamCodec<FriendlyByteBuf, SpeechBoxPayload> STREAM_CODEC = CustomPacketPayload.codec(
            SpeechBoxPayload::write,
            SpeechBoxPayload::new
    );

    public SpeechBoxPayload(FriendlyByteBuf buf) {
        this(
                buf.readBlockPos(),
                buf.readUtf(),
                buf.readFloat(),
                buf.readFloat()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(text != null ? text : "");
        buf.writeFloat(pitch);
        buf.writeFloat(volume);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SpeechBoxPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                com.mojang.text2speech.Narrator narrator = com.mojang.text2speech.Narrator.getNarrator();
                if (narrator.active()) {
                    narrator.say(payload.text(), false);
                }
            } catch (Throwable t) {
                Computronics.LOGGER.warn("SpeechBox TTS playback error: {}", t.getMessage());
            }
        });
    }
}
