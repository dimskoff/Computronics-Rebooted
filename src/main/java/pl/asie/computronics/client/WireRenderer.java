package pl.asie.computronics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import pl.asie.computronics.Computronics;
import pl.asie.computronics.signal.ISignalNode;
import pl.asie.computronics.signal.SignalLink;
import pl.asie.computronics.signal.Signals;

/**
 * Draws signal wires (slightly sagging cables) from a node's outputs to the inputs they feed.
 * Wires glow brighter while the signal is on.
 */
public final class WireRenderer {
    public static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath(Computronics.MODID, "textures/misc/white.png");
    private static final int SEGMENTS = 12;
    private static final float RADIUS = 0.018f;
    private static final int OFF = 0x6A1414, ON = 0xFF3A2A;

    private WireRenderer() {
    }

    public static void renderLinks(Level level, BlockPos origin, ISignalNode node, PoseStack pose, MultiBufferSource buffers, int light) {
        if (node.links().isEmpty() || level == null) return;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        for (SignalLink l : node.links()) {
            ISignalNode target = Signals.node(level, l.target());
            if (target == null || !target.hasPort(l.inPort(), false)) continue;
            Vec3 a = node.portPosition(l.outPort(), true).subtract(origin.getX(), origin.getY(), origin.getZ());
            Vec3 b = target.portPosition(l.inPort(), false).subtract(origin.getX(), origin.getY(), origin.getZ());
            boolean on = node.getOutput(l.outPort()) > 0;
            drawCable(pose, vc, a, b, on ? ON : OFF, on ? LightTexture.FULL_BRIGHT : light);
        }
    }

    /** Bounding box covering every wire leaving this node, for culling. */
    public static AABB bounds(BlockPos origin, ISignalNode node) {
        AABB box = new AABB(origin);
        for (SignalLink l : node.links()) box = box.minmax(new AABB(l.target()));
        return box.inflate(0.5);
    }

    private static void drawCable(PoseStack pose, VertexConsumer vc, Vec3 a, Vec3 b, int color, int light) {
        double len = a.distanceTo(b);
        double sag = Math.min(0.6, len * 0.08);
        Vec3 prev = a;
        for (int i = 1; i <= SEGMENTS; i++) {
            double t = (double) i / SEGMENTS;
            Vec3 p = a.lerp(b, t).subtract(0, sag * 4 * t * (1 - t), 0);
            segment(pose, vc, prev, p, color, light);
            prev = p;
        }
    }

    private static void segment(PoseStack pose, VertexConsumer vc, Vec3 a, Vec3 b, int color, int light) {
        Vec3 d = b.subtract(a);
        if (d.lengthSqr() < 1e-8) return;
        Vec3 up = Math.abs(d.normalize().y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 p1 = d.cross(up).normalize().scale(RADIUS);
        Vec3 p2 = d.cross(p1).normalize().scale(RADIUS);
        quad(pose, vc, a.add(p1), a.subtract(p1), b.subtract(p1), b.add(p1), color, light);
        quad(pose, vc, a.add(p2), a.subtract(p2), b.subtract(p2), b.add(p2), color, light);
    }

    private static void quad(PoseStack pose, VertexConsumer vc, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color, int light) {
        PoseStack.Pose p = pose.last();
        for (Vec3 v : new Vec3[]{a, b, c, d}) {
            vc.addVertex(p, (float) v.x, (float) v.y, (float) v.z)
                    .setColor((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 255)
                    .setUv(0.5f, 0.5f)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(p, 0, 1, 0);
        }
    }
}
