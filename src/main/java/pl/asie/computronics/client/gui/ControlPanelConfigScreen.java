package pl.asie.computronics.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import pl.asie.computronics.block.entity.TileControlPanel;
import pl.asie.computronics.network.ConfigurePanelPayload;
import pl.asie.computronics.panel.PanelComponent;
import pl.asie.computronics.panel.PanelComponentType;

/**
 * Screwdriver settings for a single component: color, label and size, or remove it.
 */
public class ControlPanelConfigScreen extends Screen {
    private static final int[] PALETTE = {
            0xCC2020, 0xFF8000, 0xFFCC00, 0x20CC40, 0x00C8C8, 0x2080FF, 0x8040E0, 0xFF40A0,
            0xFFFFFF, 0xA0A0A0, 0x505860, 0x202020
    };

    private final BlockPos pos;
    private final int id;
    private final PanelComponentType type;
    private int color;
    private int size;
    private EditBox labelBox;
    private EditBox hexBox;

    public static void open(BlockPos pos, int id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(pos) instanceof TileControlPanel panel) {
            PanelComponent c = panel.layout().byId(id);
            if (c != null) mc.setScreen(new ControlPanelConfigScreen(pos, c));
        }
    }

    private ControlPanelConfigScreen(BlockPos pos, PanelComponent c) {
        super(Component.translatable(c.type.translationKey()));
        this.pos = pos;
        this.id = c.id();
        this.type = c.type;
        this.color = c.color;
        this.size = c.type.square ? c.width : (c.type == PanelComponentType.SEGMENT ? c.digits() : c.height);
        this.initialLabel = c.label.replace("\n", "|");
    }

    private final String initialLabel;

    @Override
    protected void init() {
        int cx = width / 2;
        int y = height / 2 - 70;

        if (type.hasLabel()) {
            String current = labelBox != null ? labelBox.getValue() : initialLabel; // keep text across resize
            labelBox = new EditBox(font, cx - 100, y + 14, 200, 18, Component.translatable("gui.computronics.panel.label"));
            labelBox.setMaxLength(PanelComponent.MAX_LABEL);
            labelBox.setValue(current);
            addRenderableWidget(labelBox);
        }
        y += 44;

        for (int i = 0; i < PALETTE.length; i++) {
            final int col = PALETTE[i];
            addRenderableWidget(Button.builder(Component.empty(), b -> setColor(col))
                    .bounds(cx - 100 + i * 17, y, 15, 15).build());
        }
        hexBox = new EditBox(font, cx + 106, y - 1, 60, 17, Component.literal("hex"));
        hexBox.setMaxLength(6);
        hexBox.setValue(String.format("%06X", color));
        hexBox.setResponder(s -> {
            try {
                if (s.length() == 6) color = Integer.parseInt(s, 16);
            } catch (NumberFormatException ignored) {
            }
        });
        addRenderableWidget(hexBox);
        y += 30;

        addRenderableWidget(Button.builder(Component.literal("-"), b -> size = Math.max(1, size - 1)).bounds(cx - 100, y, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> size = Math.min(4, size + 1)).bounds(cx - 20, y, 20, 20).build());
        y += 34;

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> apply(false)).bounds(cx - 100, y, 96, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.computronics.panel.remove"), b -> apply(true)).bounds(cx + 4, y, 96, 20).build());
    }

    private void setColor(int c) {
        color = c;
        hexBox.setValue(String.format("%06X", c));
    }

    private void apply(boolean remove) {
        int[] fp = type.footprint(size);
        String label = labelBox != null ? labelBox.getValue().replace("|", "\n") : "";
        PacketDistributor.sendToServer(new ConfigurePanelPayload(pos, id, color, label, fp[0], fp[1], remove));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int cx = width / 2;
        int y = height / 2 - 70;
        g.drawCenteredString(font, title, cx, y - 16, 0xFFFFFF);
        if (type.hasLabel()) {
            g.drawString(font, Component.translatable("gui.computronics.panel.label"), cx - 100, y + 2, 0xA0A0A0);
        }
        y += 44;
        g.drawString(font, Component.translatable("gui.computronics.panel.color"), cx - 100, y - 11, 0xA0A0A0);
        for (int i = 0; i < PALETTE.length; i++) {
            int x0 = cx - 100 + i * 17;
            g.fill(x0 + 2, y + 2, x0 + 13, y + 13, 0xFF000000 | PALETTE[i]);
        }
        g.fill(cx + 170, y - 1, cx + 186, y + 16, 0xFF000000 | color);
        y += 30;
        int[] fp = type.footprint(size);
        g.drawString(font, Component.translatable("gui.computronics.panel.size"), cx - 100, y - 11, 0xA0A0A0);
        g.drawCenteredString(font, fp[0] + "x" + fp[1], cx - 50, y + 6, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
