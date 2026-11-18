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
import pl.asie.computronics.config.ComputronicsServerConfig;
import pl.asie.computronics.panel.PanelComponent;

/**
 * Client -> server: screwdriver GUI changes to one component. {@code remove=true} dismounts it.
 * Everything is re-validated on the server (reach, existence, lengths, sizes).
 */
public record ConfigurePanelPayload(BlockPos pos, int id, int color, String label, int width, int height, boolean remove)
        implements CustomPacketPayload {

    public static final Type<ConfigurePanelPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Computronics.MODID, "configure_panel"));

    public static final StreamCodec<FriendlyByteBuf, ConfigurePanelPayload> STREAM_CODEC =
            CustomPacketPayload.codec(ConfigurePanelPayload::write, ConfigurePanelPayload::new);

    private ConfigurePanelPayload(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readVarInt(), buf.readInt(), buf.readUtf(256), buf.readVarInt(), buf.readVarInt(), buf.readBoolean());
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(id);
        buf.writeInt(color);
        buf.writeUtf(label == null ? "" : label, 256);
        buf.writeVarInt(width);
        buf.writeVarInt(height);
        buf.writeBoolean(remove);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ConfigurePanelPayload p, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!player.level().isLoaded(p.pos())) return;
            if (player.distanceToSqr(p.pos().getCenter()) > 8 * 8) return;
            if (!player.mayBuild()) return;
            if (!(player.level().getBlockEntity(p.pos()) instanceof TileControlPanel panel)) return;
            PanelComponent c = panel.layout().byId(p.id());
            if (c == null) return;

            if (p.remove()) {
                for (net.minecraft.world.item.ItemStack drop : panel.dismount(c)) {
                    if (!player.getAbilities().instabuild && !player.getInventory().add(drop)) player.drop(drop, false);
                }
                return;
            }

            ComputronicsServerConfig cfg = ComputronicsServerConfig.SERVER;
            int color = cfg.panelAllowCustomColors.get() ? p.color() : c.color;
            String label = p.label().replace("§", "");
            int maxLen = Math.min(PanelComponent.MAX_LABEL, cfg.panelMaxCharactersPerLine.get() * cfg.panelMaxLines.get());
            if (label.length() > maxLen) label = label.substring(0, maxLen);
            String[] lines = label.split("\n", -1);
            if (lines.length > cfg.panelMaxLines.get()) {
                label = String.join("\n", java.util.Arrays.copyOf(lines, cfg.panelMaxLines.get()));
            }
            panel.configure(c, color, label, p.width(), p.height());
        });
    }
}
