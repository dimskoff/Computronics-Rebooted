package pl.asie.computronics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.api.event.RackMountableRenderEvent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import org.joml.Matrix4f;
import pl.asie.computronics.init.ModItems;

public class RackMountableRenderer {
    public static final RackMountableRenderer INSTANCE = new RackMountableRenderer();

    private RackMountableRenderer() {}

    @SubscribeEvent
    public void onRackMountableRender(RackMountableRenderEvent.BlockEntity event) {
        if (event.rack == null) return;
        ItemStack stack = event.rack.getItem(event.mountable);
        if (stack.isEmpty()) return;

        PoseStack ps = event.stack;
        MultiBufferSource buffer = event.typeBuffer;

        if (stack.getItem() == ModItems.RACK_BOARD_LIGHT.get()) {
            renderLightBoard(event, ps, buffer);
        } else if (stack.getItem() == ModItems.RACK_BOARD_SWITCH.get()) {
            renderSwitchBoard(event, ps, buffer);
        } else if (stack.getItem() == ModItems.RACK_BOARD_KVM.get()) {
            renderKVMBoard(event, ps, buffer);
        } else if (stack.getItem() == ModItems.RACK_BOARD_BOOM.get()) {
            renderBoomBoard(event, ps, buffer);
        }
    }

    private void renderLightBoard(RackMountableRenderEvent.BlockEntity e, PoseStack ps, MultiBufferSource buffer) {
        VertexConsumer vc = buffer.getBuffer(li.cil.oc.client.renderer.RenderTypes.ROBOT_LIGHT);
        Matrix4f pose = ps.last().pose();

        float vCenter = (e.v0 + e.v1) * 0.5f;
        float h = (e.v1 - e.v0) * 0.28f;

        // 4 glowing indicator LEDs on rack face
        for (int i = 0; i < 4; i++) {
            float u0 = 0.12f + (i * 0.22f);
            float u1 = u0 + 0.12f;
            drawQuad(pose, vc, u0, vCenter - h, u1, vCenter + h, -0.002f, 0, 255, 100, 255);
        }
    }

    private void renderSwitchBoard(RackMountableRenderEvent.BlockEntity e, PoseStack ps, MultiBufferSource buffer) {
        VertexConsumer vc = buffer.getBuffer(li.cil.oc.client.renderer.RenderTypes.BLOCK_OVERLAY_COLOR);
        Matrix4f pose = ps.last().pose();

        float vCenter = (e.v0 + e.v1) * 0.5f;
        float h = (e.v1 - e.v0) * 0.22f;

        // 4 toggle switches on rack face
        for (int i = 0; i < 4; i++) {
            float u0 = 0.15f + (i * 0.22f);
            float u1 = u0 + 0.08f;
            drawQuad(pose, vc, u0, vCenter - h, u1, vCenter + h, -0.002f, 190, 195, 205, 255);
        }
    }

    private void renderKVMBoard(RackMountableRenderEvent.BlockEntity e, PoseStack ps, MultiBufferSource buffer) {
        VertexConsumer vc = buffer.getBuffer(li.cil.oc.client.renderer.RenderTypes.ROBOT_LIGHT);
        Matrix4f pose = ps.last().pose();

        float vCenter = (e.v0 + e.v1) * 0.5f;
        float h = (e.v1 - e.v0) * 0.20f;

        // 4 server channel LEDs
        int[][] cols = {{0, 255, 120}, {50, 180, 255}, {255, 200, 40}, {255, 80, 80}};
        for (int i = 0; i < 4; i++) {
            float u0 = 0.14f + (i * 0.22f);
            float u1 = u0 + 0.09f;
            int[] c = cols[i];
            drawQuad(pose, vc, u0, vCenter - h, u1, vCenter + h, -0.002f, c[0], c[1], c[2], 255);
        }
    }

    private void renderBoomBoard(RackMountableRenderEvent.BlockEntity e, PoseStack ps, MultiBufferSource buffer) {
        VertexConsumer vc = buffer.getBuffer(li.cil.oc.client.renderer.RenderTypes.ROBOT_LIGHT);
        Matrix4f pose = ps.last().pose();

        float vCenter = (e.v0 + e.v1) * 0.5f;
        float h = (e.v1 - e.v0) * 0.30f;

        // Blinking hazard LED
        long time = System.currentTimeMillis() / 250;
        int r = (time % 2 == 0) ? 255 : 50;
        drawQuad(pose, vc, 0.42f, vCenter - h, 0.58f, vCenter + h, -0.002f, r, 20, 20, 255);
    }

    private static void drawQuad(Matrix4f pose, VertexConsumer vc, float x1, float y1, float x2, float y2, float z, int r, int g, int b, int a) {
        vc.addVertex(pose, x1, y1, z).setColor(r, g, b, a).setUv(0, 0);
        vc.addVertex(pose, x2, y1, z).setColor(r, g, b, a).setUv(0, 0);
        vc.addVertex(pose, x2, y2, z).setColor(r, g, b, a).setUv(0, 0);
        vc.addVertex(pose, x1, y2, z).setColor(r, g, b, a).setUv(0, 0);
    }
}
