package pl.asie.computronics.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import pl.asie.computronics.Computronics;
import pl.asie.computronics.block.entity.TileControlPanel;

public record ConfigurePanelPayload(
        BlockPos pos,
        int slot,
        int color,
        int size,
        String label,
        int textColor,
        boolean bold,
        String displayValue
) implements CustomPacketPayload {

    public static final Type<ConfigurePanelPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Computronics.MODID, "configure_panel"));

    public static final StreamCodec<FriendlyByteBuf, ConfigurePanelPayload> STREAM_CODEC = CustomPacketPayload.codec(
            ConfigurePanelPayload::write,
            ConfigurePanelPayload::new
    );

    public ConfigurePanelPayload(FriendlyByteBuf buf) {
        this(
                buf.readBlockPos(),
                buf.readVarInt(),
                buf.readInt(),
                buf.readVarInt(),
                buf.readUtf(),
                buf.readInt(),
                buf.readBoolean(),
                buf.readUtf()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(slot);
        buf.writeInt(color);
        buf.writeVarInt(size);
        buf.writeUtf(label != null ? label : "");
        buf.writeInt(textColor);
        buf.writeBoolean(bold);
        buf.writeUtf(displayValue != null ? displayValue : "");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ConfigurePanelPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                Level level = player.level();
                if (level.isLoaded(payload.pos()) && level.getBlockEntity(payload.pos()) instanceof TileControlPanel panel) {
                    panel.configureElement(payload.slot(), payload.color(), payload.size(), payload.label(),
                            payload.textColor(), payload.bold(), payload.displayValue());
                }
            }
        });
    }
}
