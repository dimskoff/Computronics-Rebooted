package pl.asie.computronics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
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

        // 1. Right Hand / Arm positioning (held like a one-handed map in a boat)
        float side = 1.0f; // Right hand
        ps.translate(side * 0.125f, -0.125f, 0.0f);

        if (!player.isInvisible()) {
            ps.pushPose();
            ps.mulPose(Axis.ZP.rotationDegrees(side * 10.0f));
            renderArm(player, ps, buffer, light, equip, swing);
            ps.popPose();
        }

        // 2. Position handheld terminal in front of the right hand
        ps.pushPose();
        ps.translate(side * 0.51f, -0.08f + equip * -1.2f, -0.75f);
        float sqrtSwing = Mth.sqrt(swing);
        float sinSwing = Mth.sin(sqrtSwing * (float) Math.PI);
        ps.translate(
                side * -0.5f * sinSwing,
                0.4f * Mth.sin(sqrtSwing * (float) Math.PI * 2.0f) - 0.3f * sinSwing,
                -0.3f * Mth.sin(swing * (float) Math.PI)
        );
        ps.mulPose(Axis.XP.rotationDegrees(sinSwing * -45.0f));
        ps.mulPose(Axis.YP.rotationDegrees(side * sinSwing * -30.0f));

        // Natural ergonomic tilt facing the player
        ps.mulPose(Axis.YP.rotationDegrees(180.0f));
        ps.mulPose(Axis.ZP.rotationDegrees(180.0f));
        ps.mulPose(Axis.XP.rotationDegrees(-24.0f));
        ps.mulPose(Axis.YP.rotationDegrees(12.0f));

        // Scale terminal model
        ps.scale(0.0075f, 0.0075f, 0.0075f);

        // Center on screen
        ps.translate(-45.0f, -60.0f, 0.0f);

        // 3. Render 3D Handheld Pager Chassis & Bezel
        renderTerminalChassis(ps, buffer);

        // 4. Render Active VRAM Screen Content
        renderScreen(stack, ps, buffer);

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

    private void renderTerminalChassis(PoseStack ps, MultiBufferSource buffer) {
        VertexConsumer vc = buffer.getBuffer(li.cil.oc.client.renderer.RenderTypes.FONT_QUAD);
        Matrix4f pose = ps.last().pose();

        // Outer Dark Industrial Chassis (width: 90, height: 120, depth: 8)
        drawQuad(pose, vc, -6, -8, 96, 128, 2.0f, 28, 32, 38, 255);
        drawQuad(pose, vc, -4, -6, 94, 126, 1.5f, 44, 50, 58, 255);

        // Antenna stub at top right
        drawQuad(pose, vc, 80, -22, 85, -8, 1.0f, 70, 78, 88, 255);
        drawQuad(pose, vc, 79, -26, 86, -22, 1.2f, 190, 55, 55, 255); // Red rubber antenna cap

        // Recessed Screen Bezel
        drawQuad(pose, vc, 2, 2, 88, 118, 1.0f, 16, 20, 24, 255);

        // Screen Background (Deep Dark Green Matrix CRT)
        drawQuad(pose, vc, 4, 4, 86, 116, 0.5f, 8, 22, 12, 255);

        // Tactile Side Grip Bumpers
        drawQuad(pose, vc, -8, 20, -5, 100, 1.8f, 20, 24, 28, 255);
        drawQuad(pose, vc, 95, 20, 98, 100, 1.8f, 20, 24, 28, 255);
    }

    private void renderScreen(ItemStack stack, PoseStack ps, MultiBufferSource buffer) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();

        boolean active = tag.getBoolean("active");
        double energy = tag.getDouble("energy");
        int port = tag.contains("port") ? tag.getInt("port") : ItemPocketTerminal.DEFAULT_PORT;
        String title = tag.contains("title") ? tag.getString("title") : "VRAM PAGER";
        String fw = tag.contains("firmware") ? tag.getString("firmware") : "EEPROM v2.0";

        Font font = Minecraft.getInstance().font;

        ps.pushPose();
        ps.translate(6.0f, 6.0f, 0.0f);
        ps.scale(0.85f, 0.85f, 1.0f);

        if (!active || energy <= 0) {
            font.drawInBatch("● [OFFLINE]", 2.0f, 2.0f, 0x555555, false, ps.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, 0x00F000F0);
            font.drawInBatch("R-CLICK TO POWER ON", 2.0f, 16.0f, 0x444444, false, ps.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, 0x00F000F0);
            ps.popPose();
            return;
        }

        // Status Header
        font.drawInBatch(title, 2.0f, 2.0f, 0x00FF88, false, ps.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, 0x00F000F0);
        String status = String.format("P:%d %.0fEU", port, energy);
        font.drawInBatch(status, 2.0f, 11.0f, 0x00CC66, false, ps.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, 0x00F000F0);

        // Divider
        font.drawInBatch("----------------", 2.0f, 18.0f, 0x006633, false, ps.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, 0x00F000F0);

        // 32x12 VRAM Matrix Display
        CompoundTag vram = tag.getCompound("vram");
        int yOff = 26;
        for (int line = 0; line < 12; line++) {
            String text = vram.getString("l_" + line);
            if (text.isEmpty()) continue;
            int fg = vram.contains("c_" + line) ? vram.getInt("c_" + line) : 0x00FF66;
            font.drawInBatch(text, 2.0f, yOff + (line * 8.5f), fg, false, ps.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, 0x00F000F0);
        }

        ps.popPose();
    }

    private static void drawQuad(Matrix4f pose, VertexConsumer vc, float x1, float y1, float x2, float y2, float z, int r, int g, int b, int a) {
        // Front winding
        vc.addVertex(pose, x1, y2, z).setUv(0, 1).setColor(r, g, b, a);
        vc.addVertex(pose, x2, y2, z).setUv(1, 1).setColor(r, g, b, a);
        vc.addVertex(pose, x2, y1, z).setUv(1, 0).setColor(r, g, b, a);
        vc.addVertex(pose, x1, y1, z).setUv(0, 0).setColor(r, g, b, a);

        // Reverse winding
        vc.addVertex(pose, x1, y1, z).setUv(0, 0).setColor(r, g, b, a);
        vc.addVertex(pose, x2, y1, z).setUv(1, 0).setColor(r, g, b, a);
        vc.addVertex(pose, x2, y2, z).setUv(1, 1).setColor(r, g, b, a);
        vc.addVertex(pose, x1, y2, z).setUv(0, 1).setColor(r, g, b, a);
    }
}
