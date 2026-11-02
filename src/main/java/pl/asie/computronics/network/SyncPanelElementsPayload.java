package pl.asie.computronics.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import pl.asie.computronics.Computronics;
import pl.asie.computronics.block.entity.TileControlPanel;

public record SyncPanelElementsPayload(
        BlockPos pos,
        CompoundTag tag
) implements CustomPacketPayload {

    public static final Type<SyncPanelElementsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Computronics.MODID, "sync_panel_elements"));

    public static final StreamCodec<FriendlyByteBuf, SyncPanelElementsPayload> STREAM_CODEC = CustomPacketPayload.codec(
            SyncPanelElementsPayload::write,
            SyncPanelElementsPayload::new
    );

    public SyncPanelElementsPayload(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), readSafeNbt(buf));
    }

    private static CompoundTag readSafeNbt(FriendlyByteBuf buf) {
        CompoundTag t = buf.readNbt();
        return t != null ? t : new CompoundTag();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeNbt(tag != null ? tag : new CompoundTag());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncPanelElementsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level level = player != null ? player.level() : null;
            if (level != null && level.isLoaded(payload.pos()) && level.getBlockEntity(payload.pos()) instanceof TileControlPanel panel) {
                panel.loadAdditional(payload.tag(), level.registryAccess());
            }
        });
    }
}
