package pl.asie.computronics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.api.event.RackMountableRenderEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import org.joml.Matrix4f;
import pl.asie.computronics.Computronics;
import pl.asie.computronics.init.ModItems;

public class RackMountableRenderer {
    public static final RackMountableRenderer INSTANCE = new RackMountableRenderer();

    private RackMountableRenderer() {}

    @SubscribeEvent
    public void onRackMountableBlock(RackMountableRenderEvent.Block event) {
        if (event.rack == null) return;
        ItemStack stack = event.rack.getItem(event.mountable);
        if (stack.isEmpty()) return;

        Item item = stack.getItem();
        ResourceLocation tex = null;

        if (item == ModItems.RACK_BOARD_LIGHT.get()) {
            tex = ResourceLocation.fromNamespaceAndPath(Computronics.MODID, "block/light_board/mode_1");
        } else if (item == ModItems.RACK_BOARD_SWITCH.get()) {
            tex = ResourceLocation.fromNamespaceAndPath(Computronics.MODID, "block/switch_board");
        } else if (item == ModItems.RACK_BOARD_CAPACITOR.get()) {
            tex = ResourceLocation.fromNamespaceAndPath(Computronics.MODID, "block/rack_capacitor");
        } else if (item == ModItems.RACK_BOARD_BOOM.get()) {
            tex = ResourceLocation.fromNamespaceAndPath(Computronics.MODID, "block/boom_board");
        } else if (item == ModItems.RACK_BOARD_CO_PROCESSOR.get()) {
            tex = ResourceLocation.fromNamespaceAndPath(Computronics.MODID, "block/advanced/cipher_front");
        } else if (item == ModItems.RACK_BOARD_KVM.get()) {
            tex = ResourceLocation.fromNamespaceAndPath(Computronics.MODID, "block/switch_board");
        }

        if (tex != null) {
            TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(tex);
            event.setFrontTextureOverride(sprite);
        }
    }

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
        } else if (stack.getItem() == ModItems.RACK_BOARD_CO_PROCESSOR.get()) {
            renderCoProcessorBoard(event, ps, buffer);
        }
    }

    private void renderLightBoard(RackMountableRenderEvent.BlockEntity e, PoseStack ps, MultiBufferSource buffer) {
        VertexConsumer vc = buffer.getBuffer(li.cil.oc.client.renderer.RenderTypes.ROBOT_LIGHT);
        Matrix4f pose = ps.last().pose();

        // 4 vertical status LEDs (Channel 1 to 4 from top to bottom)
        float vSpan = e.v1 - e.v0;
        for (int i = 0; i < 4; i++) {
            float vCenter = e.v0 + vSpan * (0.18f + i * 0.22f);
            float h = vSpan * 0.08f;
            float u0 = 0.20f;
            float u1 = 0.32f;
            // Shifting activity animation
            long time = (System.currentTimeMillis() / 400 + i) % 4;
            int g = (time == 0) ? 255 : 40;
            drawDoubleSidedQuad(pose, vc, u0, vCenter - h, u1, vCenter + h, -0.003f, 20, g, 80, 255);
        }
    }

    private void renderSwitchBoard(RackMountableRenderEvent.BlockEntity e, PoseStack ps, MultiBufferSource buffer) {
        VertexConsumer vc = buffer.getBuffer(li.cil.oc.client.renderer.RenderTypes.BLOCK_OVERLAY_COLOR);
        Matrix4f pose = ps.last().pose();

        float vSpan = e.v1 - e.v0;
        for (int i = 0; i < 4; i++) {
            float vCenter = e.v0 + vSpan * (0.18f + i * 0.22f);
            float h = vSpan * 0.07f;
            float u0 = 0.65f;
            float u1 = 0.78f;
            drawDoubleSidedQuad(pose, vc, u0, vCenter - h, u1, vCenter + h, -0.003f, 200, 205, 215, 255);
        }
    }

    private void renderKVMBoard(RackMountableRenderEvent.BlockEntity e, PoseStack ps, MultiBufferSource buffer) {
        VertexConsumer vc = buffer.getBuffer(li.cil.oc.client.renderer.RenderTypes.ROBOT_LIGHT);
        Matrix4f pose = ps.last().pose();

        float vSpan = e.v1 - e.v0;

        // Determine currently selected server channel from KVM mountable tag
        int selected = 0;
        if (e.data != null && e.data.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
            net.minecraft.world.item.component.CustomData cd = e.data.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            if (cd != null && cd.contains("selectedServer")) {
                selected = cd.copyTag().getInt("selectedServer");
            }
        }

        // 4 LEDs arranged from Top to Bottom representing Server Slots 0, 1, 2, 3
        for (int i = 0; i < 4; i++) {
            float vCenter = e.v0 + vSpan * (0.18f + i * 0.22f);
            float h = vSpan * 0.08f;
            float u0 = 0.15f;
            float u1 = 0.30f;

            if (i == selected) {
                // Active routed channel: Brilliant Glowing Emerald Green / Aqua
                drawDoubleSidedQuad(pose, vc, u0, vCenter - h, u1, vCenter + h, -0.003f, 0, 255, 120, 255);
                drawDoubleSidedQuad(pose, vc, u0 + 0.03f, vCenter - h * 0.5f, u1 - 0.03f, vCenter + h * 0.5f, -0.004f, 200, 255, 240, 255);
            } else {
                // Check if a server exists in that rack slot
                ItemStack slotStack = e.rack.getItem(i);
                if (!slotStack.isEmpty()) {
                    // Running / Connected server: soft standby amber
                    drawDoubleSidedQuad(pose, vc, u0, vCenter - h, u1, vCenter + h, -0.003f, 220, 150, 30, 255);
                } else {
                    // Empty channel: dark inactive
                    drawDoubleSidedQuad(pose, vc, u0, vCenter - h, u1, vCenter + h, -0.003f, 35, 40, 48, 255);
                }
            }
        }
    }

    private void renderCoProcessorBoard(RackMountableRenderEvent.BlockEntity e, PoseStack ps, MultiBufferSource buffer) {
        VertexConsumer vc = buffer.getBuffer(li.cil.oc.client.renderer.RenderTypes.ROBOT_LIGHT);
        Matrix4f pose = ps.last().pose();

        float vCenter = (e.v0 + e.v1) * 0.5f;
        float h = (e.v1 - e.v0) * 0.20f;

        // Crypto pulse activity LED (Cyan / Blue)
        long time = System.currentTimeMillis() / 200;
        int pulse = (int) (140 + 115 * Math.sin(time * 0.5));
        drawDoubleSidedQuad(pose, vc, 0.70f, vCenter - h, 0.85f, vCenter + h, -0.003f, 0, pulse, 255, 255);
    }

    private void renderBoomBoard(RackMountableRenderEvent.BlockEntity e, PoseStack ps, MultiBufferSource buffer) {
        VertexConsumer vc = buffer.getBuffer(li.cil.oc.client.renderer.RenderTypes.ROBOT_LIGHT);
        Matrix4f pose = ps.last().pose();

        float vCenter = (e.v0 + e.v1) * 0.5f;
        float h = (e.v1 - e.v0) * 0.25f;

        // Blinking hazard strobe LED
        long time = System.currentTimeMillis() / 250;
        int r = (time % 2 == 0) ? 255 : 45;
        drawDoubleSidedQuad(pose, vc, 0.40f, vCenter - h, 0.60f, vCenter + h, -0.003f, r, 20, 20, 255);
    }

    private static void drawDoubleSidedQuad(Matrix4f pose, VertexConsumer vc, float x1, float y1, float x2, float y2, float z, int r, int g, int b, int a) {
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
