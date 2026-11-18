package pl.asie.computronics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.AABB;
import pl.asie.computronics.block.entity.TileConnector;

/**
 * Lights up active ports on the connector (green inputs on top, red outputs below) and draws the
 * wires leaving its outputs.
 */
public class ConnectorRenderer implements BlockEntityRenderer<TileConnector> {
    private static final float S = 0.07f;

    public ConnectorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TileConnector be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        WireRenderer.renderLinks(be.getLevel(), be.getBlockPos(), be, pose, buffers, light);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(WireRenderer.WHITE));
        PoseStack.Pose p = pose.last();
        for (int port = 1; port <= TileConnector.PORTS; port++) {
            float x = (float) TileConnector.portX(port), z = (float) TileConnector.portZ(port);
            if (be.input(port) > 0) square(vc, p, x, 1.002f, z, 0x50FF60);
            if (be.output(port) > 0) square(vc, p, x, -0.002f, z, 0xFF4030);
        }
    }

    private static void square(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, int color) {
        float[][] c = {{x - S, z - S}, {x - S, z + S}, {x + S, z + S}, {x + S, z - S}};
        for (float[] v : c) {
            vc.addVertex(p, v[0], y, v[1]).setColor((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 255)
                    .setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(p, 0, y > 0.5f ? 1 : -1, 0);
        }
    }

    @Override
    public AABB getRenderBoundingBox(TileConnector be) {
        return WireRenderer.bounds(be.getBlockPos(), be);
    }

    @Override
    public boolean shouldRenderOffScreen(TileConnector be) {
        return !be.links().isEmpty();
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
