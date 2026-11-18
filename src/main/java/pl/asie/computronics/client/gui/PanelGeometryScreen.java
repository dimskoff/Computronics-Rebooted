package pl.asie.computronics.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import pl.asie.computronics.block.entity.TileControlPanel;
import pl.asie.computronics.network.PanelGeometryPayload;
import pl.asie.computronics.panel.PanelOrientation;

/** Screwdriver on an empty spot of a panel: choose plate height (like a slab) and tilt. */
public class PanelGeometryScreen extends Screen {
    private final BlockPos pos;
    private int offset;
    private int tilt;

    public static void open(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(pos) instanceof TileControlPanel panel) {
            mc.setScreen(new PanelGeometryScreen(pos, panel.offset(), panel.tilt()));
        }
    }

    private PanelGeometryScreen(BlockPos pos, int offset, int tilt) {
        super(Component.translatable("gui.computronics.panel.geometry"));
        this.pos = pos;
        this.offset = offset;
        this.tilt = tilt;
    }

    @Override
    protected void init() {
        int cx = width / 2, y = height / 2 - 40;
        for (int i = 0; i < PanelOrientation.OFFSETS; i++) {
            final int o = i;
            addRenderableWidget(Button.builder(Component.translatable("gui.computronics.panel.offset." + i), b -> apply(o, tilt))
                    .bounds(cx - 120 + i * 60, y, 58, 20).build());
        }
        y += 44;
        for (int i = 0; i < PanelOrientation.TILTS.length; i++) {
            final int t = i;
            addRenderableWidget(Button.builder(Component.literal(fmt(PanelOrientation.TILTS[i]) + "°"), b -> apply(offset, t))
                    .bounds(cx - 90 + i * 60, y, 58, 20).build());
        }
        y += 34;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(cx - 50, y, 100, 20).build());
    }

    private static String fmt(float f) {
        return f == Math.rint(f) ? Integer.toString((int) f) : Float.toString(f);
    }

    private void apply(int o, int t) {
        offset = o;
        tilt = t;
        PacketDistributor.sendToServer(new PanelGeometryPayload(pos, o, t));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int cx = width / 2, y = height / 2 - 40;
        g.drawCenteredString(font, title, cx, y - 30, 0xFFFFFF);
        g.drawString(font, Component.translatable("gui.computronics.panel.height"), cx - 120, y - 12, 0xA0A0A0);
        g.fill(cx - 120 + offset * 60, y + 21, cx - 120 + offset * 60 + 58, y + 23, 0xFF40C0FF);
        y += 44;
        g.drawString(font, Component.translatable("gui.computronics.panel.tilt"), cx - 90, y - 12, 0xA0A0A0);
        g.fill(cx - 90 + tilt * 60, y + 21, cx - 90 + tilt * 60 + 58, y + 23, 0xFF40C0FF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
