package pl.asie.computronics.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import pl.asie.computronics.block.entity.TileControlPanel;
import pl.asie.computronics.network.ConfigurePanelPayload;
import pl.asie.computronics.network.RemovePanelElementPayload;

public class ControlPanelConfigScreen extends Screen {

    private final TileControlPanel panel;
    private final int slot;
    private final TileControlPanel.PanelElement elem;

    private int selectedColor;
    private int selectedSizeX;
    private int selectedSizeY;
    private int selectedTextColor;
    private boolean bold;
    private String labelText;
    private String displayValueText;

    private EditBox labelEdit;
    private EditBox hexColorEdit;
    private EditBox displayValueEdit;
    private Button boldBtn;

    private static final int[] PRESET_COLORS = {
            0xCC2020, // Industrial Red
            0x20CC40, // Neon Green
            0x2080EE, // Terminal Blue
            0xCCCC20, // Warning Amber / Yellow
            0xCC7020, // Hazard Orange
            0x8020CC, // Synth Purple
            0x00FFCC, // Cyber Cyan
            0xCC2080, // Magenta
            0xEEEEEE, // Industrial White
            0x383E48, // Gunmetal Dark
            0x60EE20, // Lime Green
            0xEE4040  // Bright Coral
    };

    public static void open(TileControlPanel panel, int slot) {
        Minecraft.getInstance().setScreen(new ControlPanelConfigScreen(panel, slot));
    }

    public ControlPanelConfigScreen(TileControlPanel panel, int slot) {
        super(Component.literal("Control Panel Configuration"));
        this.panel = panel;
        this.slot = slot;
        this.elem = panel.getElement(slot);

        this.selectedColor = elem != null ? elem.color : 0xCC2020;
        this.selectedSizeX = elem != null ? Math.max(1, elem.sizeX) : 1;
        this.selectedSizeY = elem != null ? Math.max(1, elem.sizeY) : 1;
        this.selectedTextColor = elem != null ? elem.textColor : 0xFFFFFF;
        this.bold = elem == null || elem.bold;
        this.labelText = elem != null && elem.label != null ? elem.label : "";
        this.displayValueText = elem != null && elem.displayValue != null ? elem.displayValue : "0";
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2;
        int panelW = 350;
        int panelH = 240;
        int left = cx - panelW / 2;
        int top = cy - panelH / 2;

        boolean isText = elem != null && elem.type == TileControlPanel.ElementType.TEXT;
        boolean isButton = elem != null && elem.type == TileControlPanel.ElementType.BUTTON;
        boolean isLamp = elem != null && elem.type == TileControlPanel.ElementType.LAMP;
        boolean isSevenSeg = elem != null && elem.type == TileControlPanel.ElementType.SEVEN_SEGMENT;

        // Label / Text Input
        if (isText || isButton || isLamp) {
            labelEdit = new EditBox(font, left + 55, top + 34, 115, 16, Component.literal("Text"));
            labelEdit.setMaxLength(32);
            labelEdit.setValue(labelText);
            addRenderableWidget(labelEdit);

            boldBtn = Button.builder(Component.literal(bold ? "Bold: ON" : "Bold: OFF"), b -> {
                bold = !bold;
                b.setMessage(Component.literal(bold ? "Bold: ON" : "Bold: OFF"));
            }).bounds(left + 175, top + 34, 55, 16).build();
            addRenderableWidget(boldBtn);
        } else if (isSevenSeg) {
            displayValueEdit = new EditBox(font, left + 55, top + 34, 115, 16, Component.literal("Value"));
            displayValueEdit.setMaxLength(16);
            displayValueEdit.setValue(displayValueText);
            addRenderableWidget(displayValueEdit);
        }

        // Hex Color Input
        hexColorEdit = new EditBox(font, left + 55, top + 56, 60, 16, Component.literal("Hex"));
        hexColorEdit.setMaxLength(7);
        hexColorEdit.setValue(String.format("#%06X", selectedColor & 0xFFFFFF));
        hexColorEdit.setResponder(val -> {
            try {
                String clean = val.replace("#", "").trim();
                if (clean.length() == 6) {
                    selectedColor = Integer.parseInt(clean, 16);
                }
            } catch (Exception ignored) {}
        });
        addRenderableWidget(hexColorEdit);

        // Size / Digit Buttons
        int sizeRowY = top + 78;
        if (isSevenSeg) {
            for (int digits = 1; digits <= 4; digits++) {
                final int d = digits;
                int btnX = left + 55 + (digits - 1) * 38;
                Button sizeBtn = Button.builder(Component.literal("1x" + d), b -> {
                    selectedSizeX = d;
                    selectedSizeY = 1;
                }).bounds(btnX, sizeRowY, 34, 16).build();
                addRenderableWidget(sizeBtn);
            }
        } else if (isText) {
            int[][] textSizes = {{1, 1}, {2, 1}, {4, 1}, {8, 1}, {2, 2}, {4, 2}};
            for (int idx = 0; idx < textSizes.length; idx++) {
                final int sx = textSizes[idx][0];
                final int sy = textSizes[idx][1];
                int btnX = left + 45 + idx * 30;
                Button sizeBtn = Button.builder(Component.literal(sx + "x" + sy), b -> {
                    selectedSizeX = sx;
                    selectedSizeY = sy;
                }).bounds(btnX, sizeRowY, 28, 16).build();
                addRenderableWidget(sizeBtn);
            }
        } else {
            int maxS = (elem != null && elem.type == TileControlPanel.ElementType.SWITCH) ? 2 : 4;
            for (int sz = 1; sz <= maxS; sz++) {
                final int s = sz;
                int btnX = left + 55 + (sz - 1) * 38;
                Button sizeBtn = Button.builder(Component.literal(s + "x" + s), b -> {
                    selectedSizeX = s;
                    selectedSizeY = s;
                }).bounds(btnX, sizeRowY, 34, 16).build();
                addRenderableWidget(sizeBtn);
            }
        }

        // Color preset buttons
        int chipStartY = top + 118;
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            final int col = PRESET_COLORS[i];
            int colIdx = i % 6;
            int row = i / 6;
            int chipX = left + 18 + colIdx * 35;
            int currentChipY = chipStartY + row * 18;

            Button chip = Button.builder(Component.literal(""), b -> {
                selectedColor = col;
                hexColorEdit.setValue(String.format("#%06X", col & 0xFFFFFF));
            }).bounds(chipX, currentChipY, 32, 15).build();
            addRenderableWidget(chip);
        }

        // Action Buttons at bottom
        int botY = top + panelH - 28;

        Button applyBtn = Button.builder(Component.literal("Apply & Save"), b -> {
            applyAndClose();
        }).bounds(left + 20, botY, 95, 20).build();
        addRenderableWidget(applyBtn);

        Button removeBtn = Button.builder(Component.literal("Remove Element"), b -> {
            PacketDistributor.sendToServer(new RemovePanelElementPayload(panel.getBlockPos(), slot));
            if (elem != null) {
                elem.type = TileControlPanel.ElementType.NONE;
            }
            onClose();
        }).bounds(left + 125, botY, 100, 20).build();
        addRenderableWidget(removeBtn);

        Button cancelBtn = Button.builder(Component.literal("Cancel"), b -> {
            onClose();
        }).bounds(left + 235, botY, 95, 20).build();
        addRenderableWidget(cancelBtn);
    }

    private void applyAndClose() {
        String label = labelEdit != null ? labelEdit.getValue() : "";
        String disp = displayValueEdit != null ? displayValueEdit.getValue() : "";

        if (elem != null) {
            elem.color = selectedColor;
            elem.sizeX = selectedSizeX;
            elem.sizeY = selectedSizeY;
            elem.size = Math.max(selectedSizeX, selectedSizeY);
            elem.label = label;
            elem.bold = bold;
            if (!disp.isEmpty()) {
                elem.displayValue = disp;
            }
        }

        PacketDistributor.sendToServer(new ConfigurePanelPayload(
                panel.getBlockPos(),
                slot,
                selectedColor,
                selectedSizeX,
                selectedSizeY,
                label,
                selectedTextColor,
                bold,
                disp
        ));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Render crisp dark gradient overlay - NO BLUR SHADER
        g.fillGradient(0, 0, width, height, 0x990A0D12, 0xBB0A0D12);

        int cx = width / 2;
        int cy = height / 2;
        int panelW = 350;
        int panelH = 240;
        int left = cx - panelW / 2;
        int top = cy - panelH / 2;

        // Dark Industrial Window Frame
        g.fill(left, top, left + panelW, top + panelH, 0xFF14181F);
        g.renderOutline(left, top, panelW, panelH, 0xFF3E4856);
        g.fill(left + 1, top + 1, left + panelW - 1, top + 22, 0xFF1F242D);
        g.hLine(left + 1, left + panelW - 1, top + 22, 0xFF3E4856);

        // Header Title
        String elemName = elem != null ? elem.type.name() : "ELEMENT";
        g.drawString(font, "CONTROL PANEL: " + elemName + " [#" + (slot + 1) + "]", left + 10, top + 7, 0x00FFCC, false);

        // Field Labels
        boolean isText = elem != null && elem.type == TileControlPanel.ElementType.TEXT;
        boolean isButton = elem != null && elem.type == TileControlPanel.ElementType.BUTTON;
        boolean isLamp = elem != null && elem.type == TileControlPanel.ElementType.LAMP;
        boolean isSevenSeg = elem != null && elem.type == TileControlPanel.ElementType.SEVEN_SEGMENT;

        if (isText || isButton || isLamp) {
            g.drawString(font, "Text:", left + 18, top + 38, 0x8FA0B4, false);
        } else if (isSevenSeg) {
            g.drawString(font, "Value:", left + 18, top + 38, 0x8FA0B4, false);
        }

        g.drawString(font, "Color:", left + 18, top + 60, 0x8FA0B4, false);
        g.drawString(font, isSevenSeg ? "Digits:" : "Size:", left + 18, top + 82, 0x8FA0B4, false);
        g.drawString(font, "Palette:", left + 18, top + 104, 0x8FA0B4, false);

        // Color sample box next to hex input
        g.fill(left + 120, top + 56, left + 160, top + 72, 0xFF000000 | selectedColor);
        g.renderOutline(left + 120, top + 56, 40, 16, 0xFF718096);

        // Dedicated Preview Window on the Right
        int prevX = left + 240;
        int prevY = top + 34;
        int prevW = 96;
        int prevH = 120;
        g.fill(prevX, prevY, prevX + prevW, prevY + prevH, 0xFF0D1015);
        g.renderOutline(prevX, prevY, prevW, prevH, 0xFF333B47);
        g.drawString(font, "PREVIEW", prevX + (prevW - font.width("PREVIEW")) / 2, prevY + 5, 0x566375, false);

        // Render preview inside
        renderPreviewElement(g, prevX, prevY + 18, prevW, prevH - 22);

        super.render(g, mouseX, mouseY, partialTick);

        // Draw color fills over the palette preset buttons
        int chipStartY = top + 118;
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            int colIdx = i % 6;
            int row = i / 6;
            int chipX = left + 18 + colIdx * 35;
            int currentChipY = chipStartY + row * 18;
            g.fill(chipX + 2, currentChipY + 2, chipX + 30, currentChipY + 13, 0xFF000000 | PRESET_COLORS[i]);
            g.renderOutline(chipX + 2, currentChipY + 2, 28, 11, 0xFF000000);
        }
    }

    private void renderPreviewElement(GuiGraphics g, int px, int py, int pw, int ph) {
        if (elem == null) return;
        int pColor = 0xFF000000 | selectedColor;

        switch (elem.type) {
            case BUTTON -> {
                int btnSize = Math.min(50, 18 * selectedSizeX);
                int bx = px + (pw - btnSize) / 2;
                int by = py + (ph - btnSize) / 2;
                g.fill(bx, by, bx + btnSize, by + btnSize, pColor);
                g.renderOutline(bx, by, btnSize, btnSize, 0xFFE2E8F0);
                g.fill(bx + 2, by + 2, bx + btnSize - 2, by + 4, 0x88FFFFFF);
                String lbl = labelEdit != null ? labelEdit.getValue() : "";
                if (!lbl.isEmpty()) {
                    int lw = font.width(lbl);
                    float scale = 1.0f;
                    if (lw > btnSize - 6) {
                        scale = (float)(btnSize - 6) / lw;
                    }
                    g.pose().pushPose();
                    g.pose().translate(bx + btnSize / 2.0f, by + btnSize / 2.0f - 4, 0);
                    g.pose().scale(scale, scale, 1.0f);
                    g.drawString(font, lbl, -lw / 2, 0, 0xFFFFFFFF, bold);
                    g.pose().popPose();
                }
            }
            case SWITCH -> {
                int bx = px + (pw - 26) / 2;
                int by = py + (ph - 42) / 2;
                g.fill(bx, by, bx + 26, by + 42, 0xFF24272D);
                g.renderOutline(bx, by, 26, 42, 0xFF4A5568);
                g.fill(bx + 3, by + 4, bx + 23, by + 22, pColor); // rocker up
                g.fill(bx + 11, by + 8, bx + 15, by + 12, 0xFF00FFCC); // LED dot
            }
            case LAMP -> {
                int lampSize = Math.min(46, 16 * selectedSizeX);
                int lx = px + (pw - lampSize) / 2;
                int ly = py + (ph - lampSize) / 2;
                g.fill(lx, ly, lx + lampSize, ly + lampSize, pColor);
                g.renderOutline(lx, ly, lampSize, lampSize, 0xFFFFFFFF);
                g.fill(lx + 4, ly + 4, lx + lampSize - 4, ly + lampSize - 4, 0x66FFFFFF);
                String lbl = labelEdit != null ? labelEdit.getValue() : "";
                if (!lbl.isEmpty()) {
                    int lw = font.width(lbl);
                    float scale = 1.0f;
                    if (lw > lampSize - 4) {
                        scale = (float)(lampSize - 4) / lw;
                    }
                    g.pose().pushPose();
                    g.pose().translate(lx + lampSize / 2.0f, ly + lampSize / 2.0f - 4 * scale, 0);
                    g.pose().scale(scale, scale, 1.0f);
                    g.drawString(font, lbl, -lw / 2, 0, 0xFFFFFFFF, bold);
                    g.pose().popPose();
                }
            }
            case SEVEN_SEGMENT -> {
                String val = displayValueEdit != null && !displayValueEdit.getValue().isEmpty() ? displayValueEdit.getValue() : "8";
                int digits = Math.max(1, selectedSizeX);
                int slotW = pw / digits;
                for (int i = 0; i < Math.min(digits, val.length()); i++) {
                    String d = String.valueOf(val.charAt(i));
                    int dx = px + i * slotW + (slotW - font.width(d)) / 2;
                    g.drawString(font, d, dx, py + ph / 2 - 4, pColor, true);
                }
            }
            case TEXT -> {
                int tw = Math.min(pw - 8, selectedSizeX * 12);
                int th = Math.min(ph - 12, selectedSizeY * 18);
                int tx = px + (pw - tw) / 2;
                int ty = py + (ph - th) / 2;
                g.fill(tx, ty, tx + tw, ty + th, 0xFF1C2026);
                g.renderOutline(tx, ty, tw, th, 0xFF404854);
                String txt = labelEdit != null && !labelEdit.getValue().isEmpty() ? labelEdit.getValue() : "TEXT";
                int strW = font.width(txt);
                float scale = 1.0f;
                if (strW > tw - 4) {
                    scale = (float)(tw - 4) / strW;
                }
                g.pose().pushPose();
                g.pose().translate(tx + tw / 2.0f, ty + th / 2.0f - 4 * scale, 0);
                g.pose().scale(scale, scale, 1.0f);
                g.drawString(font, txt, -strW / 2, 0, pColor, bold);
                g.pose().popPose();
            }
            default -> {}
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
