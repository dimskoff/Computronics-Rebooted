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
            0xCC2020, // Red
            0x20CC40, // Green
            0x2060CC, // Blue
            0xCCCC20, // Yellow
            0xCC7020, // Orange
            0x8020CC, // Purple
            0x20CCCC, // Cyan
            0xCC2080, // Magenta
            0xEEEEEE, // White
            0x404040, // Gunmetal Dark
            0x60EE20, // Lime
            0xEE5050  // Coral
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
        int panelW = 280;
        int panelH = 220;
        int left = cx - panelW / 2;
        int top = cy - panelH / 2;

        // Label Edit Box
        labelEdit = new EditBox(font, left + 75, top + 35, 120, 16, Component.literal("Label"));
        labelEdit.setMaxLength(16);
        labelEdit.setValue(labelText);
        addRenderableWidget(labelEdit);

        // Hex Color Input
        hexColorEdit = new EditBox(font, left + 75, top + 60, 60, 16, Component.literal("Hex"));
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

        // Display Value input for 7-segment
        if (elem != null && elem.type == TileControlPanel.ElementType.SEVEN_SEGMENT) {
            displayValueEdit = new EditBox(font, left + 75, top + 85, 80, 16, Component.literal("Value"));
            displayValueEdit.setMaxLength(8);
            displayValueEdit.setValue(displayValueText);
            addRenderableWidget(displayValueEdit);
        }

        // Bold Button
        boldBtn = Button.builder(Component.literal("Bold: " + (bold ? "ON" : "OFF")), b -> {
            bold = !bold;
            b.setMessage(Component.literal("Bold: " + (bold ? "ON" : "OFF")));
        }).bounds(left + 200, top + 35, 70, 16).build();
        addRenderableWidget(boldBtn);

        // Size / Digit Buttons
        if (elem != null && elem.type == TileControlPanel.ElementType.SEVEN_SEGMENT) {
            // Horizontal multi-digit 7-segment display: 1x1, 1x2, 1x3, 1x4
            for (int digits = 1; digits <= 4; digits++) {
                final int d = digits;
                int btnX = left + 75 + (digits - 1) * 38;
                int btnY = top + 110;
                Button sizeBtn = Button.builder(Component.literal("1x" + d), b -> {
                    selectedSizeX = d;
                    selectedSizeY = 1;
                }).bounds(btnX, btnY, 34, 16).build();
                addRenderableWidget(sizeBtn);
            }
        } else {
            // Square buttons / lamps / switches: 1x1, 2x2, 3x3, 4x4
            int maxS = (elem != null && elem.type == TileControlPanel.ElementType.SWITCH) ? 2 : 4;
            for (int sz = 1; sz <= maxS; sz++) {
                final int s = sz;
                int btnX = left + 75 + (sz - 1) * 38;
                int btnY = top + 90;
                Button sizeBtn = Button.builder(Component.literal(s + "x" + s), b -> {
                    selectedSizeX = s;
                    selectedSizeY = s;
                }).bounds(btnX, btnY, 34, 16).build();
                addRenderableWidget(sizeBtn);
            }
        }

        // Color preset chips buttons
        int chipY = top + (elem != null && elem.type == TileControlPanel.ElementType.SEVEN_SEGMENT ? 135 : 115);
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            final int col = PRESET_COLORS[i];
            int chipX = left + 20 + (i % 6) * 42;
            int row = i / 6;
            int currentChipY = chipY + row * 18;
            Button chip = Button.builder(Component.literal(""), b -> {
                selectedColor = col;
                hexColorEdit.setValue(String.format("#%06X", col & 0xFFFFFF));
            }).bounds(chipX, currentChipY, 36, 16).build();
            addRenderableWidget(chip);
        }

        // Action Buttons at bottom
        int botY = top + panelH - 30;

        Button applyBtn = Button.builder(Component.literal("Apply & Save"), b -> {
            applyAndClose();
        }).bounds(left + 15, botY, 80, 20).build();
        addRenderableWidget(applyBtn);

        Button removeBtn = Button.builder(Component.literal("Remove"), b -> {
            PacketDistributor.sendToServer(new RemovePanelElementPayload(panel.getBlockPos(), slot));
            if (elem != null) {
                elem.type = TileControlPanel.ElementType.NONE;
            }
            onClose();
        }).bounds(left + 100, botY, 75, 20).build();
        addRenderableWidget(removeBtn);

        Button cancelBtn = Button.builder(Component.literal("Cancel"), b -> {
            onClose();
        }).bounds(left + 185, botY, 80, 20).build();
        addRenderableWidget(cancelBtn);
    }

    private void applyAndClose() {
        String label = labelEdit.getValue();
        String disp = displayValueEdit != null ? displayValueEdit.getValue() : "";

        // Immediate client local update
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

        // Sync to server via network packet
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
        renderBackground(g, mouseX, mouseY, partialTick);

        int cx = width / 2;
        int cy = height / 2;
        int panelW = 280;
        int panelH = 220;
        int left = cx - panelW / 2;
        int top = cy - panelH / 2;

        // Dark industrial background
        g.fill(left, top, left + panelW, top + panelH, 0xEE1E2228);
        g.renderOutline(left, top, panelW, panelH, 0xFF4A5568);
        g.fill(left + 1, top + 1, left + panelW - 1, top + 22, 0xFF2D3748);

        // Header Title
        String elemName = elem != null ? elem.type.name() : "ELEMENT";
        g.drawString(font, "CONFIGURING: " + elemName + " [SLOT #" + (slot + 1) + "]", left + 10, top + 7, 0x00FFCC, false);

        // Field Labels
        g.drawString(font, "Label:", left + 20, top + 39, 0xA0AEC0, false);
        g.drawString(font, "Color:", left + 20, top + 64, 0xA0AEC0, false);
        if (elem != null && elem.type == TileControlPanel.ElementType.SEVEN_SEGMENT) {
            g.drawString(font, "Value:", left + 20, top + 89, 0xA0AEC0, false);
        }
        int sizeY = top + (elem != null && elem.type == TileControlPanel.ElementType.SEVEN_SEGMENT ? 114 : 94);
        g.drawString(font, (elem != null && elem.type == TileControlPanel.ElementType.SEVEN_SEGMENT ? "Digits:" : "Size:"), left + 20, sizeY, 0xA0AEC0, false);

        // Color sample box
        g.fill(left + 140, top + 60, left + 175, top + 76, 0xFF000000 | selectedColor);
        g.renderOutline(left + 140, top + 60, 35, 16, 0xFFE2E8F0);

        // Live Preview Box
        int prevX = left + 200;
        int prevY = top + 60;
        g.fill(prevX, prevY, prevX + 68, prevY + 68, 0xFF14181E);
        g.renderOutline(prevX, prevY, 68, 68, 0xFF718096);
        g.drawString(font, "PREVIEW", prevX + 14, prevY + 4, 0x718096, false);

        // Render preview inside
        if (elem != null && elem.type == TileControlPanel.ElementType.BUTTON) {
            int pColor = 0xFF000000 | selectedColor;
            int btnBoxSize = Math.min(48, 16 * selectedSizeX);
            int bx = prevX + (68 - btnBoxSize) / 2;
            int by = prevY + 16 + (48 - btnBoxSize) / 2;
            g.fill(bx, by, bx + btnBoxSize, by + btnBoxSize, pColor);
            g.renderOutline(bx, by, btnBoxSize, btnBoxSize, 0xFFFFFFFF);
            String lbl = labelEdit.getValue();
            if (!lbl.isEmpty()) {
                int lw = font.width(lbl);
                g.drawString(font, lbl, bx + (btnBoxSize - lw) / 2, by + (btnBoxSize - 8) / 2, 0xFFFFFFFF, bold);
            }
        } else if (elem != null && elem.type == TileControlPanel.ElementType.SWITCH) {
            int pColor = 0xFF000000 | selectedColor;
            int bx = prevX + 22;
            int by = prevY + 22;
            g.fill(bx, by, bx + 24, by + 36, 0xFF282828);
            g.renderOutline(bx, by, 24, 36, 0xFF707070);
            g.fill(bx + 4, by + 4, bx + 20, by + 18, pColor); // toggle up
        } else if (elem != null && elem.type == TileControlPanel.ElementType.LAMP) {
            int pColor = 0xFF000000 | selectedColor;
            int lampR = Math.min(22, 8 * selectedSizeX);
            int lx = prevX + 34;
            int ly = prevY + 40;
            g.fill(lx - lampR, ly - lampR, lx + lampR, ly + lampR, pColor);
            g.renderOutline(lx - lampR, ly - lampR, lampR * 2, lampR * 2, 0xFFFFFFFF);
            g.fill(lx - lampR / 2, ly - lampR / 2, lx + lampR / 2, ly + lampR / 2, 0x88FFFFFF);
        } else if (elem != null && elem.type == TileControlPanel.ElementType.SEVEN_SEGMENT) {
            int pColor = 0xFF000000 | selectedColor;
            String val = displayValueEdit != null ? displayValueEdit.getValue() : "8";
            int vw = font.width(val);
            g.drawString(font, val, prevX + (68 - vw) / 2, prevY + 36, pColor, true);
        }

        super.render(g, mouseX, mouseY, partialTick);

        // Overlay colored rectangles over the preset buttons so the player sees the colors
        int chipY = top + (elem != null && elem.type == TileControlPanel.ElementType.SEVEN_SEGMENT ? 135 : 115);
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            int chipX = left + 20 + (i % 6) * 42;
            int row = i / 6;
            int currentChipY = chipY + row * 18;
            g.fill(chipX + 3, currentChipY + 3, chipX + 33, currentChipY + 13, 0xFF000000 | PRESET_COLORS[i]);
            g.renderOutline(chipX + 2, currentChipY + 2, 32, 12, 0xFF000000);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
