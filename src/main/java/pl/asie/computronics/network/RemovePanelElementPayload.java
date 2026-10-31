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

public record RemovePanelElementPayload(
        BlockPos pos,
        int slot
) implements CustomPacketPayload {

    public static final Type<RemovePanelElementPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Computronics.MODID, "remove_panel_element"));

    public static final StreamCodec<FriendlyByteBuf, RemovePanelElementPayload> STREAM_CODEC = CustomPacketPayload.codec(
            RemovePanelElementPayload::write,
            RemovePanelElementPayload::new
    );

    public RemovePanelElementPayload(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readVarInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(slot);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RemovePanelElementPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                Level level = player.level();
                if (level.isLoaded(payload.pos()) && level.getBlockEntity(payload.pos()) instanceof TileControlPanel panel) {
                    panel.removeElementAndDrop(payload.slot(), player);
                }
            }
        });
    }
}
