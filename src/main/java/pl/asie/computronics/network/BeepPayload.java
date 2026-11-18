package pl.asie.computronics.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import pl.asie.computronics.Computronics;

/**
 * Server -> client: play square-wave beeps at a position. Synthesized on the client.
 *
 * @param frequencies Hz (20..2000)
 * @param durationsMs milliseconds (50..5000), same length as frequencies (max 8)
 */
public record BeepPayload(double x, double y, double z, float volume, int range, int[] frequencies, int[] durationsMs)
        implements CustomPacketPayload {
    public static final int MAX_TONES = 8;

    public static final Type<BeepPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Computronics.MODID, "beep"));

    public static final StreamCodec<FriendlyByteBuf, BeepPayload> STREAM_CODEC =
            CustomPacketPayload.codec(BeepPayload::write, BeepPayload::read);

    private static BeepPayload read(FriendlyByteBuf buf) {
        double x = buf.readDouble(), y = buf.readDouble(), z = buf.readDouble();
        float volume = buf.readFloat();
        int range = buf.readVarInt();
        int n = Math.min(MAX_TONES, Math.max(0, buf.readVarInt()));
        int[] f = new int[n], d = new int[n];
        for (int i = 0; i < n; i++) {
            f[i] = buf.readVarInt();
            d[i] = buf.readVarInt();
        }
        return new BeepPayload(x, y, z, volume, range, f, d);
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(volume);
        buf.writeVarInt(range);
        int n = Math.min(MAX_TONES, Math.min(frequencies.length, durationsMs.length));
        buf.writeVarInt(n);
        for (int i = 0; i < n; i++) {
            buf.writeVarInt(frequencies[i]);
            buf.writeVarInt(durationsMs[i]);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BeepPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> pl.asie.computronics.client.audio.BeepSynth.play(payload));
    }
}
