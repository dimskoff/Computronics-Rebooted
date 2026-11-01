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
        renderTabletChassis(ps, buffer);

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

    private void renderTabletChassis(PoseStack ps, MultiBufferSource buffer) {
        VertexConsumer vc = buffer.getBuffer(li.cil.oc.client.renderer.RenderTypes.FONT_QUAD);
        Matrix4f pose = ps.last().pose();

        // Outer Dark Metallic Widescreen Chassis (width: 150, height: 95, depth: 3)
        drawQuad(pose, vc, -5, -5, 155, 100, 2.0f, 24, 28, 34, 255);
        drawQuad(pose, vc, -3, -3, 153, 98, 1.6f, 44, 50, 60, 255);

        // Recessed Screen Bezel
        drawQuad(pose, vc, 4, 4, 146, 91, 1.0f, 15, 18, 22, 255);

        // Screen Background (Deep Dark Terminal Surface)
        drawQuad(pose, vc, 6, 6, 144, 89, 0.5f, 10, 16, 20, 255);

        // Subtle Status LED in top bezel
        drawQuad(pose, vc, 73, 1, 77, 3, 1.7f, 0, 255, 200, 255);
    }

    private void renderScreen(ItemStack stack, PoseStack ps, MultiBufferSource buffer) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();

        boolean active = tag.getBoolean("active");
        double energy = tag.getDouble("energy");
        int port = tag.contains("port") ? tag.getInt("port") : ItemPocketTerminal.DEFAULT_PORT;
        String title = tag.contains("title") ? tag.getString("title") : "WIRELESS MONITOR";
        String fw = tag.contains("firmware") ? tag.getString("firmware") : "EEPROM v2.0";

        Font font = Minecraft.getInstance().font;

        ps.pushPose();
        ps.translate(9.0f, 9.0f, 0.0f);
        ps.scale(0.82f, 0.82f, 1.0f);

        if (!active || energy <= 0) {
            font.drawInBatch("● [OFFLINE - STANDBY]", 2.0f, 2.0f, 0x555555, false, ps.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, 0x00F000F0);
            font.drawInBatch("RIGHT-CLICK TO POWER ON", 2.0f, 16.0f, 0x444444, false, ps.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, 0x00F000F0);
            ps.popPose();
            return;
        }

        // Status Header
        font.drawInBatch(title, 2.0f, 2.0f, 0x00FFCC, false, ps.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, 0x00F000F0);
        String status = String.format("PORT:%d  %.0fEU", port, energy);
        font.drawInBatch(status, 2.0f, 11.0f, 0x00CC88, false, ps.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, 0x00F000F0);

        // Divider
        font.drawInBatch("------------------------------------", 2.0f, 18.0f, 0x006644, false, ps.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, 0x00F000F0);

        // VRAM Matrix Display (up to 10 lines)
        CompoundTag vram = tag.getCompound("vram");
        int yOff = 26;
        for (int line = 0; line < 10; line++) {
            String text = vram.getString("l_" + line);
            if (text.isEmpty()) continue;
            int fg = vram.contains("c_" + line) ? vram.getInt("c_" + line) : 0x00FF88;
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
