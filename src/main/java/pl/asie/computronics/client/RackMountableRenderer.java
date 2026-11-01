package pl.asie.computronics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.api.event.RackMountableRenderEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import org.joml.Matrix4f;
import pl.asie.computronics.Computronics;
import pl.asie.computronics.init.ModItems;
import pl.asie.computronics.item.rack.ItemRackLightBoard;

public class RackMountableRenderer {
    public static final RackMountableRenderer INSTANCE = new RackMountableRenderer();

    private RackMountableRenderer() {}

    @SubscribeEvent
    public void onRackTextureOverride(RackMountableRenderEvent.Block event) {
        if (!pl.asie.computronics.config.ComputronicsConfig.CLIENT.renderRackOverlays.get()) return;
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
        }

        if (tex != null) {
            TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(tex);
            event.setFrontTextureOverride(sprite);
        }
    }

    @SubscribeEvent
    public void onRackMountableRender(RackMountableRenderEvent.BlockEntity event) {
        if (!pl.asie.computronics.config.ComputronicsConfig.CLIENT.renderRackOverlays.get()) return;
        if (event.rack == null) return;
        ItemStack stack = event.rack.getItem(event.mountable);
        if (stack.isEmpty()) return;

        PoseStack ps = event.stack;
        MultiBufferSource buffer = event.typeBuffer;

        if (stack.getItem() == ModItems.RACK_BOARD_LIGHT.get()) {
            renderLightBoard(event, ps, buffer);
        } else if (stack.getItem() == ModItems.RACK_BOARD_SWITCH.get()) {
            renderSwitchBoard(event, ps, buffer);
        } else if (stack.getItem() == ModItems.RACK_BOARD_BOOM.get()) {
            renderBoomBoard(event, ps, buffer);
        } else if (stack.getItem() == ModItems.RACK_BOARD_CO_PROCESSOR.get()) {
            renderCoProcessorBoard(event, ps, buffer);
        }
    }

    private void renderLightBoard(RackMountableRenderEvent.BlockEntity e, PoseStack ps, MultiBufferSource buffer) {
        VertexConsumer vc = buffer.getBuffer(li.cil.oc.client.renderer.RenderTypes.ROBOT_LIGHT);
        Matrix4f pose = ps.last().pose();

        CompoundTag tag = null;
        if (e.data != null && e.data.has(DataComponents.CUSTOM_DATA)) {
            CustomData cd = e.data.get(DataComponents.CUSTOM_DATA);
            if (cd != null) tag = cd.copyTag();
        }

        int mode = (tag != null && tag.contains("mode")) ? Math.max(1, Math.min(5, tag.getInt("mode"))) : 1;
        int count = ItemRackLightBoard.getLightCount(mode);
        float vSpan = e.v1 - e.v0;

        if (mode == 5) { // Total: 42 LEDs in 3 rows of 14
            for (int i = 0; i < 42; i++) {
                int row = i / 14;
                int col = i % 14;
                float vCenter = e.v0 + vSpan * (0.22f + row * 0.28f);
                float h = vSpan * 0.08f;
                float u0 = 0.08f + col * 0.06f;
                float u1 = u0 + 0.045f;

                int color = 0x20CC40;
                boolean act = true;
                if (tag != null) {
                    if (tag.contains("c_" + i)) color = tag.getInt("c_" + i);
                    if (tag.contains("a_" + i)) act = tag.getBoolean("a_" + i);
                }
                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;
                if (!act) { r /= 5; g /= 5; b /= 5; }

                drawDoubleSidedQuad(pose, vc, u0, vCenter - h, u1, vCenter + h, -0.003f, r, g, b, 255);
            }
        } else if (mode == 3) { // 10 LEDs: 2 rows of 5
            for (int i = 0; i < 10; i++) {
                int row = i / 5;
                int col = i % 5;
                float vCenter = e.v0 + vSpan * (0.30f + row * 0.40f);
                float h = vSpan * 0.12f;
                float u0 = 0.15f + col * 0.15f;
                float u1 = u0 + 0.09f;

                int color = 0x00FFCC;
                boolean act = true;
                if (tag != null) {
                    if (tag.contains("c_" + i)) color = tag.getInt("c_" + i);
                    if (tag.contains("a_" + i)) act = tag.getBoolean("a_" + i);
                }
                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;
                if (!act) { r /= 5; g /= 5; b /= 5; }

                drawDoubleSidedQuad(pose, vc, u0, vCenter - h, u1, vCenter + h, -0.003f, r, g, b, 255);
            }
        } else { // Modes 1, 2, 4 (Single or 4 vertical/horizontal LEDs)
            for (int i = 0; i < count; i++) {
                float vCenter = e.v0 + vSpan * (0.18f + (float) i / count * 0.65f);
                float h = vSpan * 0.08f;
                float u0 = 0.18f + (float) i / count * 0.60f;
                float u1 = u0 + 0.08f;

                int color = 0x20CC40;
                boolean act = true;
                if (tag != null) {
                    if (tag.contains("c_" + i)) color = tag.getInt("c_" + i);
                    if (tag.contains("a_" + i)) act = tag.getBoolean("a_" + i);
                } else {
                    long time = (System.currentTimeMillis() / 400 + i) % count;
                    if (time != 0) { color = 0x082210; }
                }

                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;
                if (!act) { r /= 5; g /= 5; b /= 5; }

                drawDoubleSidedQuad(pose, vc, u0, vCenter - h, u1, vCenter + h, -0.003f, r, g, b, 255);
            }
        }
    }

    private void renderSwitchBoard(RackMountableRenderEvent.BlockEntity e, PoseStack ps, MultiBufferSource buffer) {
        VertexConsumer vc = buffer.getBuffer(li.cil.oc.client.renderer.RenderTypes.BLOCK_OVERLAY_COLOR);
        Matrix4f pose = ps.last().pose();

        CompoundTag tag = null;
        if (e.data != null && e.data.has(DataComponents.CUSTOM_DATA)) {
            CustomData cd = e.data.get(DataComponents.CUSTOM_DATA);
            if (cd != null) tag = cd.copyTag();
        }

        float vSpan = e.v1 - e.v0;
        for (int i = 0; i < 4; i++) {
            boolean state = tag != null && tag.getBoolean("switch_" + i);

            float vCenter = e.v0 + vSpan * (0.18f + i * 0.22f);
            float h = vSpan * 0.07f;
            float u0 = 0.65f;
            float u1 = 0.78f;

            // Switch rocker background
            drawDoubleSidedQuad(pose, vc, u0, vCenter - h, u1, vCenter + h, -0.002f, 30, 34, 40, 255);

            // LED indicator light (Green=ON, Red=OFF)
            if (state) {
                drawDoubleSidedQuad(pose, vc, u0 + 0.02f, vCenter - h * 0.5f, u1 - 0.02f, vCenter + h * 0.5f, -0.004f, 32, 220, 64, 255);
            } else {
                drawDoubleSidedQuad(pose, vc, u0 + 0.02f, vCenter - h * 0.5f, u1 - 0.02f, vCenter + h * 0.5f, -0.004f, 180, 32, 32, 255);
            }
        }
    }

    private void renderBoomBoard(RackMountableRenderEvent.BlockEntity e, PoseStack ps, MultiBufferSource buffer) {
        VertexConsumer vc = buffer.getBuffer(li.cil.oc.client.renderer.RenderTypes.ROBOT_LIGHT);
        Matrix4f pose = ps.last().pose();

        float vCenter = (e.v0 + e.v1) * 0.5f;
        float h = (e.v1 - e.v0) * 0.25f;

        long time = System.currentTimeMillis() / 250;
        int r = (time % 2 == 0) ? 255 : 40;
        drawDoubleSidedQuad(pose, vc, 0.40f, vCenter - h, 0.60f, vCenter + h, -0.003f, r, 20, 20, 255);
    }

    private void renderCoProcessorBoard(RackMountableRenderEvent.BlockEntity e, PoseStack ps, MultiBufferSource buffer) {
        VertexConsumer vc = buffer.getBuffer(li.cil.oc.client.renderer.RenderTypes.ROBOT_LIGHT);
        Matrix4f pose = ps.last().pose();

        float vCenter = (e.v0 + e.v1) * 0.5f;
        float h = (e.v1 - e.v0) * 0.20f;

        long time = System.currentTimeMillis() / 200;
        int pulse = (int) (140 + 115 * Math.sin(time * 0.5));
        drawDoubleSidedQuad(pose, vc, 0.35f, vCenter - h, 0.65f, vCenter + h, -0.003f, 20, pulse, 255, 255);
    }

    private static void drawDoubleSidedQuad(Matrix4f pose, VertexConsumer vc,
                                           float u0, float v0, float u1, float v1, float z,
                                           int r, int g, int b, int a) {
        vc.addVertex(pose, u0, v1, z).setUv(u0, v1).setColor(r, g, b, a);
        vc.addVertex(pose, u1, v1, z).setUv(u1, v1).setColor(r, g, b, a);
        vc.addVertex(pose, u1, v0, z).setUv(u1, v0).setColor(r, g, b, a);
        vc.addVertex(pose, u0, v0, z).setUv(u0, v0).setColor(r, g, b, a);

        vc.addVertex(pose, u0, v0, z).setUv(u0, v0).setColor(r, g, b, a);
        vc.addVertex(pose, u1, v0, z).setUv(u1, v0).setColor(r, g, b, a);
        vc.addVertex(pose, u1, v1, z).setUv(u1, v1).setColor(r, g, b, a);
        vc.addVertex(pose, u0, v1, z).setUv(u0, v1).setColor(r, g, b, a);
    }
}
