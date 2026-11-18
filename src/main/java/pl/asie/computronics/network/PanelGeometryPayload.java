package pl.asie.computronics.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import pl.asie.computronics.Computronics;
import pl.asie.computronics.block.entity.TileControlPanel;

/** Client -> server: plate height offset and tilt chosen in the panel settings screen. */
public record PanelGeometryPayload(BlockPos pos, int offset, int tilt) implements CustomPacketPayload {
    public static final Type<PanelGeometryPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Computronics.MODID, "panel_geometry"));
    public static final StreamCodec<FriendlyByteBuf, PanelGeometryPayload> STREAM_CODEC =
            CustomPacketPayload.codec(PanelGeometryPayload::write, PanelGeometryPayload::new);

    private PanelGeometryPayload(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readByte(), buf.readByte());
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByte(offset);
        buf.writeByte(tilt);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PanelGeometryPayload p, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!player.level().isLoaded(p.pos()) || !player.mayBuild()) return;
            if (player.distanceToSqr(p.pos().getCenter()) > 8 * 8) return;
            if (player.level().getBlockEntity(p.pos()) instanceof TileControlPanel panel) {
                panel.setGeometry(p.offset(), p.tilt());
            }
        });
    }
}
