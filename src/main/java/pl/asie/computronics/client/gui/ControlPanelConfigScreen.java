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
    private int selectedTextColor;
    private int selectedSizeX;
    private int selectedSizeY;
    private boolean bold;

    private final EditBox[] lineEdits = new EditBox[4];
    private EditBox hexBaseColorEdit;
    private EditBox hexTextColorEdit;
    private EditBox displayValueEdit;
    private Button boldBtn;

    private static final int[] BASE_PRESETS = {
            0xCC2020, // Red
            0x20CC40, // Green
            0x2080EE, // Blue
            0xCCCC20, // Amber
            0x00FFCC, // Cyan
            0x383E48  // Dark Gunmetal
    };

    private static final int[] TEXT_PRESETS = {
            0xFFFFFF, // Pure White
            0x00FFCC, // Cyber Cyan
            0xFFFF00, // Electric Yellow
            0xFF4444, // Bright Coral
            0x66FF66, // Lime
            0x1A1A1A  // Charcoal Black
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
        this.selectedTextColor = elem != null ? elem.textColor : 0xFFFFFF;
        this.selectedSizeX = elem != null ? Math.max(1, elem.sizeX) : 1;
        this.selectedSizeY = elem != null ? Math.max(1, elem.sizeY) : 1;
        this.bold = elem == null || elem.bold;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2;
        int panelW = 380;
        int panelH = 248;
        int left = cx - panelW / 2;
        int top = cy - panelH / 2;

        boolean isText = elem != null && elem.type == TileControlPanel.ElementType.TEXT;
        boolean isButton = elem != null && elem.type == TileControlPanel.ElementType.BUTTON;
        boolean isLamp = elem != null && elem.type == TileControlPanel.ElementType.LAMP;
        boolean isSevenSeg = elem != null && elem.type == TileControlPanel.ElementType.SEVEN_SEGMENT;

        // Split existing label into up to 4 lines
        String existingLabel = elem != null && elem.label != null ? elem.label : "";
        String[] lines = existingLabel.split("\\r?\\n");

        if (isText || isButton || isLamp) {
            // 4 Line Input fields
            int lineStartY = top + 30;
            for (int i = 0; i < 4; i++) {
                String val = (i < lines.length) ? lines[i] : "";
                EditBox eb = new EditBox(font, left + 52, lineStartY + i * 18, 128, 15, Component.literal("Line " + (i + 1)));
                eb.setMaxLength(32);
                eb.setValue(val);
                lineEdits[i] = eb;
                addRenderableWidget(eb);
            }

            boldBtn = Button.builder(Component.literal(bold ? "Bold: ON" : "Bold: OFF"), b -> {
                bold = !bold;
                b.setMessage(Component.literal(bold ? "Bold: ON" : "Bold: OFF"));
            }).bounds(left + 185, top + 30, 62, 15).build();
            addRenderableWidget(boldBtn);
        } else if (isSevenSeg) {
            displayValueEdit = new EditBox(font, left + 52, top + 30, 128, 16, Component.literal("Value"));
            displayValueEdit.setMaxLength(16);
            displayValueEdit.setValue(elem != null && elem.displayValue != null ? elem.displayValue : "0");
            addRenderableWidget(displayValueEdit);
        }

        // Base Color Row
        int baseColY = (isText || isButton || isLamp) ? top + 106 : top + 54;
        hexBaseColorEdit = new EditBox(font, left + 70, baseColY, 55, 14, Component.literal("Base Hex"));
        hexBaseColorEdit.setMaxLength(7);
        hexBaseColorEdit.setValue(String.format("#%06X", selectedColor & 0xFFFFFF));
        hexBaseColorEdit.setResponder(val -> {
            try {
                String clean = val.replace("#", "").trim();
                if (clean.length() == 6) {
                    selectedColor = Integer.parseInt(clean, 16);
                }
            } catch (Exception ignored) {}
        });
        addRenderableWidget(hexBaseColorEdit);

        // Base Presets
        for (int i = 0; i < BASE_PRESETS.length; i++) {
            final int col = BASE_PRESETS[i];
            int bx = left + 155 + i * 16;
            Button b = Button.builder(Component.literal(""), btn -> {
                selectedColor = col;
                hexBaseColorEdit.setValue(String.format("#%06X", col & 0xFFFFFF));
            }).bounds(bx, baseColY, 14, 14).build();
            addRenderableWidget(b);
        }

        // Text Color Row (for elements with text)
        if (isText || isButton || isLamp) {
            int textColY = top + 124;
            hexTextColorEdit = new EditBox(font, left + 70, textColY, 55, 14, Component.literal("Text Hex"));
            hexTextColorEdit.setMaxLength(7);
            hexTextColorEdit.setValue(String.format("#%06X", selectedTextColor & 0xFFFFFF));
            hexTextColorEdit.setResponder(val -> {
                try {
                    String clean = val.replace("#", "").trim();
                    if (clean.length() == 6) {
                        selectedTextColor = Integer.parseInt(clean, 16);
                    }
                } catch (Exception ignored) {}
            });
            addRenderableWidget(hexTextColorEdit);

            // Text Presets
            for (int i = 0; i < TEXT_PRESETS.length; i++) {
                final int col = TEXT_PRESETS[i];
                int bx = left + 155 + i * 16;
                Button b = Button.builder(Component.literal(""), btn -> {
                    selectedTextColor = col;
                    hexTextColorEdit.setValue(String.format("#%06X", col & 0xFFFFFF));
                }).bounds(bx, textColY, 14, 14).build();
                addRenderableWidget(b);
            }
        }

        // Size / Digit Buttons
        int sizeRowY = (isText || isButton || isLamp) ? top + 146 : top + 76;
        if (isSevenSeg) {
            for (int digits = 1; digits <= 4; digits++) {
                final int d = digits;
                int btnX = left + 52 + (digits - 1) * 36;
                Button sizeBtn = Button.builder(Component.literal("1x" + d), b -> {
                    selectedSizeX = d;
                    selectedSizeY = 1;
                }).bounds(btnX, sizeRowY, 32, 16).build();
                addRenderableWidget(sizeBtn);
            }
        } else if (isText) {
            int[][] textSizes = {{1, 1}, {2, 1}, {4, 1}, {8, 1}, {2, 2}, {4, 2}};
            for (int idx = 0; idx < textSizes.length; idx++) {
                final int sx = textSizes[idx][0];
                final int sy = textSizes[idx][1];
                int btnX = left + 45 + idx * 34;
                Button sizeBtn = Button.builder(Component.literal(sx + "x" + sy), b -> {
                    selectedSizeX = sx;
                    selectedSizeY = sy;
                }).bounds(btnX, sizeRowY, 30, 16).build();
                addRenderableWidget(sizeBtn);
            }
        } else {
            int maxS = (elem != null && elem.type == TileControlPanel.ElementType.SWITCH) ? 2 : 4;
            for (int sz = 1; sz <= maxS; sz++) {
                final int s = sz;
                int btnX = left + 52 + (sz - 1) * 36;
                Button sizeBtn = Button.builder(Component.literal(s + "x" + s), b -> {
                    selectedSizeX = s;
                    selectedSizeY = s;
                }).bounds(btnX, sizeRowY, 32, 16).build();
                addRenderableWidget(sizeBtn);
            }
        }

        // Action Buttons at bottom
        int botY = top + panelH - 26;

        Button applyBtn = Button.builder(Component.literal("Apply & Save"), b -> applyAndClose())
                .bounds(left + 16, botY, 110, 20).build();
        addRenderableWidget(applyBtn);

        Button removeBtn = Button.builder(Component.literal("Remove Component"), b -> {
            PacketDistributor.sendToServer(new RemovePanelElementPayload(panel.getBlockPos(), slot));
            if (elem != null) {
                elem.type = TileControlPanel.ElementType.NONE;
            }
            onClose();
        }).bounds(left + 134, botY, 130, 20).build();
        addRenderableWidget(removeBtn);

        Button cancelBtn = Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(left + 272, botY, 90, 20).build();
        addRenderableWidget(cancelBtn);
    }

    private String getJoinedText() {
        if (lineEdits[0] == null) return "";
        StringBuilder sb = new StringBuilder();
        int lastNonEmpty = -1;
        for (int i = 3; i >= 0; i--) {
            if (lineEdits[i] != null && !lineEdits[i].getValue().trim().isEmpty()) {
                lastNonEmpty = i;
                break;
            }
        }
        if (lastNonEmpty == -1) return "";
        for (int i = 0; i <= lastNonEmpty; i++) {
            if (i > 0) sb.append("\n");
            sb.append(lineEdits[i] != null ? lineEdits[i].getValue() : "");
        }
        return sb.toString();
    }

    private void applyAndClose() {
        String label = getJoinedText();
        String disp = displayValueEdit != null ? displayValueEdit.getValue() : "";

        if (elem != null) {
            elem.color = selectedColor;
            elem.textColor = selectedTextColor;
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
        // Prevent background world blur shader
        if (this.minecraft != null && this.minecraft.gameRenderer != null) {
            if (this.minecraft.gameRenderer.currentEffect() != null) {
                this.minecraft.gameRenderer.shutdownEffect();
            }
        }

        // Clean subtle dark tint behind the dialog
        g.fillGradient(0, 0, width, height, 0x77000000, 0x88000000);

        int cx = width / 2;
        int cy = height / 2;
        int panelW = 380;
        int panelH = 248;
        int left = cx - panelW / 2;
        int top = cy - panelH / 2;

        // Solid, crisp container box
        g.fill(left, top, left + panelW, top + panelH, 0xFF191D24);
        g.renderOutline(left, top, panelW, panelH, 0xFF3D4756);
        g.renderOutline(left + 1, top + 1, panelW - 2, panelH - 2, 0xFF2A313C);

        // Header bar
        g.fill(left + 1, top + 1, left + panelW - 1, top + 22, 0xFF232832);
        g.hLine(left + 1, left + panelW - 1, top + 22, 0xFF3D4756);

        String elemName = elem != null ? elem.type.name() : "COMPONENT";
        g.drawString(font, "CONTROL PANEL: " + elemName + " [#" + (slot + 1) + "]", left + 10, top + 7, 0x00FFCC, false);

        boolean isText = elem != null && elem.type == TileControlPanel.ElementType.TEXT;
        boolean isButton = elem != null && elem.type == TileControlPanel.ElementType.BUTTON;
        boolean isLamp = elem != null && elem.type == TileControlPanel.ElementType.LAMP;
        boolean isSevenSeg = elem != null && elem.type == TileControlPanel.ElementType.SEVEN_SEGMENT;

        // Field Labels
        if (isText || isButton || isLamp) {
            for (int i = 0; i < 4; i++) {
                g.drawString(font, "L" + (i + 1) + ":", left + 14, top + 34 + i * 18, 0x8FA0B4, false);
            }
            g.drawString(font, "Base:", left + 14, top + 109, 0x8FA0B4, false);
            g.drawString(font, "Text:", left + 14, top + 127, 0x8FA0B4, false);
            g.drawString(font, "Size:", left + 14, top + 150, 0x8FA0B4, false);

            // Swatch rects
            g.fill(left + 130, top + 106, left + 148, top + 120, 0xFF000000 | selectedColor);
            g.renderOutline(left + 130, top + 106, 18, 14, 0xFF888888);

            g.fill(left + 130, top + 124, left + 148, top + 138, 0xFF000000 | selectedTextColor);
            g.renderOutline(left + 130, top + 124, 18, 14, 0xFF888888);
        } else if (isSevenSeg) {
            g.drawString(font, "Val:", left + 14, top + 34, 0x8FA0B4, false);
            g.drawString(font, "Color:", left + 14, top + 57, 0x8FA0B4, false);
            g.drawString(font, "Digits:", left + 14, top + 80, 0x8FA0B4, false);

            g.fill(left + 130, top + 54, left + 148, top + 68, 0xFF000000 | selectedColor);
            g.renderOutline(left + 130, top + 54, 18, 14, 0xFF888888);
        } else {
            g.drawString(font, "Color:", left + 14, top + 57, 0x8FA0B4, false);
            g.drawString(font, "Size:", left + 14, top + 80, 0x8FA0B4, false);

            g.fill(left + 130, top + 54, left + 148, top + 68, 0xFF000000 | selectedColor);
            g.renderOutline(left + 130, top + 54, 18, 14, 0xFF888888);
        }

        // Live Preview Box on the Right
        int prevX = left + 258;
        int prevY = top + 30;
        int prevW = 106;
        int prevH = 140;
        g.fill(prevX, prevY, prevX + prevW, prevY + prevH, 0xFF0E1116);
        g.renderOutline(prevX, prevY, prevW, prevH, 0xFF353E4B);
        g.drawString(font, "LIVE PREVIEW", prevX + (prevW - font.width("LIVE PREVIEW")) / 2, prevY + 5, 0x627284, false);

        renderPreviewElement(g, prevX, prevY + 16, prevW, prevH - 20);

        super.render(g, mouseX, mouseY, partialTick);

        // Overlay colored rects on the preset swatch buttons
        int baseColY = (isText || isButton || isLamp) ? top + 106 : top + 54;
        for (int i = 0; i < BASE_PRESETS.length; i++) {
            int bx = left + 155 + i * 16;
            g.fill(bx + 2, baseColY + 2, bx + 12, baseColY + 12, 0xFF000000 | BASE_PRESETS[i]);
            g.renderOutline(bx + 1, baseColY + 1, 12, 12, 0xFF000000);
        }

        if (isText || isButton || isLamp) {
            int textColY = top + 124;
            for (int i = 0; i < TEXT_PRESETS.length; i++) {
                int bx = left + 155 + i * 16;
                g.fill(bx + 2, textColY + 2, bx + 12, textColY + 12, 0xFF000000 | TEXT_PRESETS[i]);
                g.renderOutline(bx + 1, textColY + 1, 12, 12, 0xFF000000);
            }
        }
    }

    private void renderPreviewElement(GuiGraphics g, int px, int py, int pw, int ph) {
        if (elem == null) return;
        int pColor = 0xFF000000 | selectedColor;
        int tColor = 0xFF000000 | selectedTextColor;
        String raw = getJoinedText();
        String[] lines = raw.isEmpty() ? new String[0] : raw.split("\\r?\\n");

        switch (elem.type) {
            case BUTTON -> {
                int btnSize = Math.min(54, 18 * selectedSizeX);
                int bx = px + (pw - btnSize) / 2;
                int by = py + (ph - btnSize) / 2;
                g.fill(bx, by, bx + btnSize, by + btnSize, pColor);
                g.renderOutline(bx, by, btnSize, btnSize, 0xFFE2E8F0);
                g.fill(bx + 2, by + 2, bx + btnSize - 2, by + 4, 0x77FFFFFF);

                if (lines.length > 0) {
                    float lineHeight = 9.0f;
                    float totalH = lines.length * lineHeight;
                    float startY = by + (btnSize - totalH) / 2.0f;
                    for (int i = 0; i < lines.length; i++) {
                        String line = lines[i];
                        int lw = font.width(line);
                        float scale = lw > (btnSize - 4) ? (float)(btnSize - 4) / lw : 1.0f;
                        g.pose().pushPose();
                        g.pose().translate(bx + btnSize / 2.0f, startY + i * lineHeight, 0);
                        g.pose().scale(scale, scale, 1.0f);
                        g.drawString(font, line, -lw / 2, 0, tColor, bold);
                        g.pose().popPose();
                    }
                }
            }
            case LAMP -> {
                int lampSize = Math.min(50, 16 * selectedSizeX);
                int lx = px + (pw - lampSize) / 2;
                int ly = py + (ph - lampSize) / 2;
                g.fill(lx, ly, lx + lampSize, ly + lampSize, pColor);
                g.renderOutline(lx, ly, lampSize, lampSize, 0xFFFFFFFF);
                g.fill(lx + 4, ly + 4, lx + lampSize - 4, ly + lampSize - 4, 0x44FFFFFF);

                if (lines.length > 0) {
                    float lineHeight = 9.0f;
                    float totalH = lines.length * lineHeight;
                    float startY = ly + (lampSize - totalH) / 2.0f;
                    for (int i = 0; i < lines.length; i++) {
                        String line = lines[i];
                        int lw = font.width(line);
                        float scale = lw > (lampSize - 4) ? (float)(lampSize - 4) / lw : 1.0f;
                        g.pose().pushPose();
                        g.pose().translate(lx + lampSize / 2.0f, startY + i * lineHeight, 0);
                        g.pose().scale(scale, scale, 1.0f);
                        g.drawString(font, line, -lw / 2, 0, tColor, bold);
                        g.pose().popPose();
                    }
                }
            }
            case SWITCH -> {
                int bx = px + (pw - 26) / 2;
                int by = py + (ph - 42) / 2;
                g.fill(bx, by, bx + 26, by + 42, 0xFF24272D);
                g.renderOutline(bx, by, 26, 42, 0xFF4A5568);
                g.fill(bx + 3, by + 4, bx + 23, by + 22, pColor);
                g.fill(bx + 11, by + 8, bx + 15, by + 12, 0xFF00FFCC);
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
                // Pure text component without background box!
                if (lines.length > 0) {
                    float lineHeight = 10.0f;
                    float totalH = lines.length * lineHeight;
                    float startY = py + (ph - totalH) / 2.0f;
                    for (int i = 0; i < lines.length; i++) {
                        String line = lines[i];
                        int lw = font.width(line);
                        float scale = lw > (pw - 8) ? (float)(pw - 8) / lw : 1.0f;
                        g.pose().pushPose();
                        g.pose().translate(px + pw / 2.0f, startY + i * lineHeight, 0);
                        g.pose().scale(scale, scale, 1.0f);
                        g.drawString(font, line, -lw / 2, 0, tColor, bold);
                        g.pose().popPose();
                    }
                } else {
                    g.drawString(font, "LINE 1\nLINE 2", px + 10, py + ph / 2 - 8, 0x445566, false);
                }
            }
            default -> {}
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
