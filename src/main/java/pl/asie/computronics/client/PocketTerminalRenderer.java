package pl.asie.computronics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import org.joml.Matrix4f;
import pl.asie.computronics.item.ItemPocketTerminal;

public class PocketTerminalRenderer {
    public static final PocketTerminalRenderer INSTANCE = new PocketTerminalRenderer();

    private PocketTerminalRenderer() {}

    @SubscribeEvent
    public void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !mc.options.getCameraType().isFirstPerson()) {
            return;
        }

        if (!pl.asie.computronics.config.ComputronicsConfig.CLIENT.renderHandheldWirelessMonitor.get()) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemPocketTerminal)) {
            return;
        }

        // Intercept and cancel default item rendering
        event.setCanceled(true);

        PoseStack ps = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();
        int light = event.getPackedLight();
        float equip = event.getEquipProgress();
        float swing = event.getSwingProgress();
        LocalPlayer player = mc.player;

        ps.pushPose();

        // 1. Right Hand / Arm positioning (matching OpenComputers widescreen tablet)
        float side = 1.0f; // Right hand
        ps.translate(side * 0.125f, -0.125f, 0.0f);

        if (!player.isInvisible()) {
            ps.pushPose();
            ps.mulPose(Axis.ZP.rotationDegrees(side * 10.0f));
            renderArm(player, ps, buffer, light, equip, swing);
            ps.popPose();
        }

        // 2. Position handheld widescreen tablet in front of player's right hand
        ps.pushPose();
        ps.translate(side * 0.52f, -0.10f + equip * -1.2f, -0.74f);
        float sqrtSwing = Mth.sqrt(swing);
        float sinSwing = Mth.sin(sqrtSwing * (float) Math.PI);
        ps.translate(
                side * -0.45f * sinSwing,
                0.35f * Mth.sin(sqrtSwing * (float) Math.PI * 2.0f) - 0.25f * sinSwing,
                -0.25f * Mth.sin(swing * (float) Math.PI)
        );
        ps.mulPose(Axis.XP.rotationDegrees(sinSwing * -40.0f));
        ps.mulPose(Axis.YP.rotationDegrees(side * sinSwing * -25.0f));

        // Ergonomic 16:10 landscape tilt facing the player
        ps.mulPose(Axis.YP.rotationDegrees(180.0f));
        ps.mulPose(Axis.ZP.rotationDegrees(180.0f));
        ps.mulPose(Axis.XP.rotationDegrees(-26.0f));
        ps.mulPose(Axis.YP.rotationDegrees(13.0f));

        // Scale tablet model
        ps.scale(0.0058f, 0.0058f, 0.0058f);

        // Center widescreen 16:10 chassis (150 x 95)
        ps.translate(-75.0f, -47.5f, 0.0f);

        // 3. Render 3D Handheld Widescreen Tablet Chassis & Bezel (No antenna)
        CompoundTag screenTag = tagOf(stack);
        int bg = screenTag.contains("bg_col") ? screenTag.getInt("bg_col") & 0xFFFFFF : 0x0A1014;
        renderTabletChassis(ps, buffer, bg);

        // 4. Render Active VRAM Screen Content
        renderScreen(stack, screenTag, ps, buffer);

        ps.popPose();
        ps.popPose();
    }

    private void renderArm(LocalPlayer player, PoseStack ps, MultiBufferSource buffer, int light, float equip, float swing) {
        ps.pushPose();
        float f = Mth.sqrt(swing);
        float f1 = -0.3f * Mth.sin(f * (float) Math.PI);
        float f2 = 0.4f * Mth.sin(f * ((float) Math.PI * 2.0f));
        float f3 = -0.4f * Mth.sin(swing * (float) Math.PI);
        ps.translate(f1 + 0.64f, f2 - 0.6f + equip * -0.6f, f3 - 0.72f);
        ps.mulPose(Axis.YP.rotationDegrees(45.0f));
        ps.mulPose(Axis.YP.rotationDegrees(Mth.sin(f * (float) Math.PI) * 70.0f));
        ps.mulPose(Axis.ZP.rotationDegrees(equip * equip * -20.0f));
        ps.translate(-1.0f, 3.6f, 3.5f);
        ps.mulPose(Axis.ZP.rotationDegrees(120.0f));
        ps.mulPose(Axis.XP.rotationDegrees(200.0f));
        ps.mulPose(Axis.YP.rotationDegrees(-135.0f));
        ps.translate(5.6f, 0.0f, 0.0f);

        PlayerRenderer pr = (PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        if (pr != null) {
            pr.renderRightHand(ps, buffer, light, player);
        }
        ps.popPose();
    }

    private static final int FULL_BRIGHT = 0x00F000F0;

    private void renderTabletChassis(PoseStack ps, MultiBufferSource buffer, int bgColor) {
        VertexConsumer vc = buffer.getBuffer(RenderType.textBackground());
        Matrix4f pose = ps.last().pose();
        drawQuad(pose, vc, -5, -5, 155, 100, 2.0f, 24, 28, 34, 255);
        drawQuad(pose, vc, -3, -3, 153, 98, 1.6f, 44, 50, 60, 255);
        drawQuad(pose, vc, 4, 4, 146, 91, 1.0f, 15, 18, 22, 255);
        drawQuad(pose, vc, 6, 6, 144, 89, 0.5f, (bgColor >> 16) & 0xFF, (bgColor >> 8) & 0xFF, bgColor & 0xFF, 255);
        drawQuad(pose, vc, 73, 1, 77, 3, 1.7f, 0, 255, 200, 255);
    }

    private static CompoundTag tagOf(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null ? customData.copyTag() : new CompoundTag();
    }

    private static void text(Font font, String s, float x, float y, int color, PoseStack ps, MultiBufferSource buffer) {
        font.drawInBatch(s, x, y, 0xFF000000 | color, false, ps.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
    }

    private static void text(Font font, Component s, float x, float y, int color, PoseStack ps, MultiBufferSource buffer) {
        font.drawInBatch(s, x, y, 0xFF000000 | color, false, ps.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
    }

    /**
     * Data contract (written by PocketTerminalManager into CUSTOM_DATA):
     * line_N (String, N = 0..SCREEN_HEIGHT-1), fg_line_N (int RGB), fg_col / bg_col, title, port, energy, active.
     */
    private void renderScreen(ItemStack stack, CompoundTag tag, PoseStack ps, MultiBufferSource buffer) {
        boolean active = tag.getBoolean("active");
        double energy = tag.getDouble("energy");
        int port = tag.contains("port") ? tag.getInt("port") : ItemPocketTerminal.DEFAULT_PORT;
        String title = tag.contains("title") ? tag.getString("title") : "";
        int defaultFg = tag.contains("fg_col") ? tag.getInt("fg_col") & 0xFFFFFF : 0x00FF66;

        Font font = Minecraft.getInstance().font;
        ps.pushPose();
        // Screen area is 138 x 83 units; 32 columns x (header + 12 lines) at scale 0.6.
        ps.translate(8.0f, 8.0f, 0.0f);
        ps.scale(0.6f, 0.6f, 1.0f);

        if (!active || energy <= 0) {
            text(font, Component.translatable("gui.computronics.pocket_terminal.offline"), 2, 2, 0x555555, ps, buffer);
            text(font, Component.translatable("gui.computronics.pocket_terminal.power_hint"), 2, 14, 0x444444, ps, buffer);
            ps.popPose();
            return;
        }

        text(font, title, 0, 0, 0x00FFCC, ps, buffer);
        String status = String.format("%d | %.0f", port, energy);
        text(font, status, 216 - font.width(status), 0, 0x00CC88, ps, buffer);

        float y0 = 12;
        for (int line = 0; line < ItemPocketTerminal.SCREEN_HEIGHT; line++) {
            String s = tag.getString("line_" + line);
            if (s.isEmpty()) continue;
            int fg = tag.contains("fg_line_" + line) ? tag.getInt("fg_line_" + line) & 0xFFFFFF : defaultFg;
            text(font, s, 0, y0 + line * 9, fg, ps, buffer);
        }
        ps.popPose();
    }

    private static void drawQuad(Matrix4f pose, VertexConsumer vc, float x1, float y1, float x2, float y2, float z, int r, int g, int b, int a) {
        vc.addVertex(pose, x1, y2, z).setColor(r, g, b, a).setLight(FULL_BRIGHT);
        vc.addVertex(pose, x2, y2, z).setColor(r, g, b, a).setLight(FULL_BRIGHT);
        vc.addVertex(pose, x2, y1, z).setColor(r, g, b, a).setLight(FULL_BRIGHT);
        vc.addVertex(pose, x1, y1, z).setColor(r, g, b, a).setLight(FULL_BRIGHT);

        vc.addVertex(pose, x1, y1, z).setColor(r, g, b, a).setLight(FULL_BRIGHT);
        vc.addVertex(pose, x2, y1, z).setColor(r, g, b, a).setLight(FULL_BRIGHT);
        vc.addVertex(pose, x2, y2, z).setColor(r, g, b, a).setLight(FULL_BRIGHT);
        vc.addVertex(pose, x1, y2, z).setColor(r, g, b, a).setLight(FULL_BRIGHT);
    }
}