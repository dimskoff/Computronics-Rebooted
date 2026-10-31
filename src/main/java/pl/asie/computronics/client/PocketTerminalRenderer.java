package pl.asie.computronics.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import pl.asie.computronics.item.ItemPocketTerminal;

public class PocketTerminalRenderer {
    public static final PocketTerminalRenderer INSTANCE = new PocketTerminalRenderer();

    private PocketTerminalRenderer() {}

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) {
            return;
        }

        ItemStack stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof ItemPocketTerminal)) {
            stack = mc.player.getOffhandItem();
            if (!(stack.getItem() instanceof ItemPocketTerminal)) {
                return;
            }
        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();

        boolean active = tag.getBoolean("active");
        double charge = tag.getDouble("energy");
        int fgCol = tag.contains("fg_col") ? tag.getInt("fg_col") : 0x00FF66;
        int bgCol = tag.contains("bg_col") ? tag.getInt("bg_col") : 0x08150A;
        String title = tag.contains("title") ? tag.getString("title") : "POCKET TERMINAL";
        String termId = tag.contains("term_id") ? tag.getString("term_id") : "--------";
        String shortId = termId.length() >= 8 ? termId.substring(0, 8) : termId;

        GuiGraphics g = event.getGuiGraphics();
        int screenW = g.guiWidth();
        int screenH = g.guiHeight();

        int frameW = 200;
        int frameH = 112;
        int x = screenW - frameW - 10;
        int y = screenH - frameH - 10;

        // Outer Bezel Frame
        g.fill(x, y, x + frameW, y + frameH, 0xEE1E2228);
        g.renderOutline(x, y, frameW, frameH, 0xFF353C45);
        g.renderOutline(x + 1, y + 1, frameW - 2, frameH - 2, 0xFF14171A);

        // Header
        int ledCol = active ? 0xFF00FF55 : 0xFF551111;
        g.fill(x + 6, y + 5, x + 10, y + 9, ledCol);
        g.drawString(mc.font, title, x + 14, y + 4, 0xFFCCDDEE, false);

        int pct = (int) Math.round(charge / ItemPocketTerminal.MAX_CHARGE * 100.0);
        int battColor = pct > 50 ? 0xFF00FF88 : (pct > 20 ? 0xFFFFCC00 : 0xFFFF4444);
        String battStr = pct + "%";
        int battStrW = mc.font.width(battStr);
        g.drawString(mc.font, battStr, x + frameW - battStrW - 6, y + 4, battColor, false);

        // Screen area
        int screenX = x + 5;
        int screenY = y + 15;
        int screenBoxW = frameW - 10;
        int screenBoxH = frameH - 25;
        int bgAlphaColor = 0xFF000000 | bgCol;

        g.fill(screenX, screenY, screenX + screenBoxW, screenY + screenBoxH, bgAlphaColor);
        g.renderOutline(screenX - 1, screenY - 1, screenBoxW + 2, screenBoxH + 2, 0xFF0D1217);

        if (active && charge > 0.0) {
            int fgAlphaColor = 0xFF000000 | fgCol;
            for (int r = 0; r < ItemPocketTerminal.SCREEN_HEIGHT; r++) {
                String lineKey = "line_" + r;
                if (tag.contains(lineKey)) {
                    String line = tag.getString(lineKey);
                    g.drawString(mc.font, line, screenX + 3, screenY + 2 + r * 8, fgAlphaColor, false);
                }
            }
        } else if (active) {
            int msgW1 = mc.font.width("[ BATTERY DEPLETED ]");
            g.drawString(mc.font, "[ BATTERY DEPLETED ]", screenX + (screenBoxW - msgW1) / 2, screenY + 26, 0xFFFF4444, false);
            int msgW2 = mc.font.width("Charge on Inductive Pad");
            g.drawString(mc.font, "Charge on Inductive Pad", screenX + (screenBoxW - msgW2) / 2, screenY + 38, 0xFFAA6644, false);
        } else {
            int msgW1 = mc.font.width("[ STANDBY / OFF ]");
            g.drawString(mc.font, "[ STANDBY / OFF ]", screenX + (screenBoxW - msgW1) / 2, screenY + 26, 0xFF667788, false);
            int msgW2 = mc.font.width("Right-Click to Turn ON");
            g.drawString(mc.font, "Right-Click to Turn ON", screenX + (screenBoxW - msgW2) / 2, screenY + 38, 0xFF445566, false);
        }

        // Footer info
        String idLabel = "ID: " + shortId;
        g.drawString(mc.font, idLabel, x + 6, y + frameH - 9, 0xFF556677, false);

        String prompt = "WIRELESS CH:404";
        int pW = mc.font.width(prompt);
        g.drawString(mc.font, prompt, x + frameW - pW - 6, y + frameH - 9, 0xFF445566, false);
    }
}
