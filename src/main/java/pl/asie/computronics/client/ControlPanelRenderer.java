package pl.asie.computronics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import pl.asie.computronics.Computronics;
import pl.asie.computronics.block.entity.TileControlPanel;
import pl.asie.computronics.panel.PanelComponent;
import pl.asie.computronics.panel.PanelOrientation;

import java.util.List;

/**
 * Draws the panel plate and its components in "panel space" (u right, v down, w towards the viewer,
 * units = blocks), see {@link PanelOrientation}. One transform handles every mounting, height and tilt.
 */
public class ControlPanelRenderer implements BlockEntityRenderer<TileControlPanel> {
    private static final ResourceLocation WHITE = WireRenderer.WHITE;
    private static final ResourceLocation TEX_FRONT = tex("block/panel");
    private static final ResourceLocation TEX_SIDE = tex("block/panel_side");
    private static final ResourceLocation TEX_BACK = tex("block/machine_bottom");
    private static final float PX = 1 / 16f;
    private static final float C = PanelOrientation.CELL;
    private static final float M = PanelOrientation.MARGIN;
    private static final float T = PanelOrientation.THICKNESS;
    private static final int PORT_OUT = 0xE03030, PORT_IN = 0x30D040;

    // segment bits: a b c d e f g (bit 0 = a)
    private static final String SEG_CHARS = "0123456789ABCDEF- ";
    private static final int[] SEG_BITS = {
            0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7D, 0x07, 0x7F, 0x6F,
            0x77, 0x7C, 0x39, 0x5E, 0x79, 0x71, 0x40, 0x00
    };

    private final Font font;

    public ControlPanelRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    private static ResourceLocation tex(String path) {
        return ResourceLocation.fromNamespaceAndPath(Computronics.MODID, "textures/" + path + ".png");
    }

    @Override
    public void render(TileControlPanel panel, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        long time = panel.getLevel() != null ? panel.getLevel().getGameTime() : 0;
        List<PanelComponent> components = panel.layout().snapshot();

        // wires are drawn in block space
        WireRenderer.renderLinks(panel.getLevel(), panel.getBlockPos(), panel, pose, buffers, light);

        pose.pushPose();
        pose.mulPose(panel.orientation().matrix());
        renderPlate(pose, buffers, light);

        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        for (PanelComponent c : components) {
            float x0 = M + c.x * C, y0 = M + c.y * C;
            float x1 = x0 + c.width * C, y1 = y0 + c.height * C;
            renderPort(pose, vc, c, x0, y0, x1, y1, panel.isWired(c.id()), light);
            float pad = C * 0.08f;
            x0 += pad; y0 += pad; x1 -= pad; y1 -= pad;
            switch (c.type) {
                case BUTTON -> renderButton(pose, vc, buffers, c, time, x0, y0, x1, y1, light, false);
                case HOLD_BUTTON -> renderButton(pose, vc, buffers, c, time, x0, y0, x1, y1, light, true);
                case SWITCH -> renderSwitch(pose, vc, c, x0, y0, x1, y1, light);
                case LAMP -> renderLamp(pose, vc, buffers, c, x0, y0, x1, y1, light);
                case SEGMENT -> renderSegment(pose, vc, c, panel.isWired(c.id()), x0, y0, x1, y1);
                case LABEL -> drawText(pose, buffers, c.label, c.color, x0, y0, x1, y1, 0.002f, light);
            }
        }
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(TileControlPanel panel) {
        return WireRenderer.bounds(panel.getBlockPos(), panel);
    }

    // ------------------------------------------------------------------ plate

    private static void renderPlate(PoseStack pose, MultiBufferSource buffers, int light) {
        PoseStack.Pose p = pose.last();
        VertexConsumer front = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_FRONT));
        tq(front, p, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 0, 1, 1, 1, 0, 0, 1, 0, light, 0, 0, 1);

        VertexConsumer back = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_BACK));
        tq(back, p, 0, 0, -T, 0, 0, 1, 0, -T, 1, 0, 1, 1, -T, 1, 1, 0, 1, -T, 0, 1, light, 0, 0, -1);

        VertexConsumer side = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_SIDE));
        float vt = 1 - T; // side strip uv
        // top edge (v = 0)
        tq(side, p, 0, 0, 0, 0, vt, 1, 0, 0, 1, vt, 1, 0, -T, 1, 1, 0, 0, -T, 0, 1, light, 0, -1, 0);
        // bottom edge (v = 1)
        tq(side, p, 0, 1, -T, 0, vt, 1, 1, -T, 1, vt, 1, 1, 0, 1, 1, 0, 1, 0, 0, 1, light, 0, 1, 0);
        // left edge (u = 0)
        tq(side, p, 0, 0, -T, 0, vt, 0, 1, -T, 1, vt, 0, 1, 0, 1, 1, 0, 0, 0, 0, 1, light, -1, 0, 0);
        // right edge (u = 1)
        tq(side, p, 1, 0, 0, 0, vt, 1, 1, 0, 1, vt, 1, 1, -T, 1, 1, 1, 0, -T, 0, 1, light, 1, 0, 0);
    }

    /** Textured quad: 4 x (x, y, z, u, v). */
    private static void tq(VertexConsumer vc, PoseStack.Pose p,
                           float x0, float y0, float z0, float u0, float v0,
                           float x1, float y1, float z1, float u1, float v1,
                           float x2, float y2, float z2, float u2, float v2,
                           float x3, float y3, float z3, float u3, float v3,
                           int light, float nx, float ny, float nz) {
        tv(vc, p, x0, y0, z0, u0, v0, light, nx, ny, nz);
        tv(vc, p, x1, y1, z1, u1, v1, light, nx, ny, nz);
        tv(vc, p, x2, y2, z2, u2, v2, light, nx, ny, nz);
        tv(vc, p, x3, y3, z3, u3, v3, light, nx, ny, nz);
    }

    private static void tv(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float u, float v, int light, float nx, float ny, float nz) {
        vc.addVertex(p, x, y, z).setColor(255, 255, 255, 255).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
    }

    /** Port socket on the back of the plate, behind the component: red = output, green = input. */
    private static void renderPort(PoseStack pose, VertexConsumer vc, PanelComponent c, float x0, float y0, float x1, float y1, boolean wired, int light) {
        boolean out = c.type.hasOutputPort();
        if (!out && !c.type.hasInputPort()) return;
        float cx = (x0 + x1) / 2, cy = (y0 + y1) / 2, s = 0.035f;
        int color = out ? PORT_OUT : PORT_IN;
        float z = -T - 0.003f;
        quad(pose, vc, cx - s * 1.6f, cy - s * 1.6f, cx + s * 1.6f, cy + s * 1.6f, z + 0.001f, 0x1A1A1A, light);
        quad(pose, vc, cx - s, cy - s, cx + s, cy + s, z, wired ? color : shade(color, 0.55f), light);
    }

    // ------------------------------------------------------------------ components

    private void renderButton(PoseStack pose, VertexConsumer vc, MultiBufferSource buffers, PanelComponent c, long time,
                              float x0, float y0, float x1, float y1, int light, boolean hold) {
        boolean pressed = time < c.pressedUntil;
        box(pose, vc, x0, y0, x1, y1, 0, 0.4f * PX, hold ? 0xD8B020 : 0x202428, light);     // bezel (yellow = hold)
        float in = Math.min(x1 - x0, y1 - y0) * (hold ? 0.18f : 0.12f);
        float h = pressed ? 0.6f * PX : (hold ? 1.8f : 1.4f) * PX;
        int col = pressed ? shade(c.color, 0.7f) : c.color;
        box(pose, vc, x0 + in, y0 + in, x1 - in, y1 - in, 0.4f * PX, h, col, light);
        if (hold) {
            float r = in * 0.6f;   // mushroom cap rim
            box(pose, vc, x0 + r, y0 + r, x1 - r, y1 - r, h - 0.3f * PX, h, shade(col, 0.85f), light);
        }
        drawText(pose, buffers, c.label, contrast(c.color), x0 + in, y0 + in, x1 - in, y1 - in, h + 0.002f, light);
    }

    private void renderSwitch(PoseStack pose, VertexConsumer vc, PanelComponent c, float x0, float y0, float x1, float y1, int light) {
        box(pose, vc, x0, y0, x1, y1, 0, 0.4f * PX, 0x202428, light);
        float w = x1 - x0, h = y1 - y0;
        float cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
        box(pose, vc, cx - w * 0.12f, y0 + h * 0.18f, cx + w * 0.12f, y1 - h * 0.18f, 0.4f * PX, 0.5f * PX, 0x0A0A0A, light);
        float ly = c.state ? y0 + h * 0.15f : cy + h * 0.05f;
        box(pose, vc, cx - w * 0.2f, ly, cx + w * 0.2f, ly + h * 0.3f, 0.5f * PX, 2.0f * PX, c.color, light);
        int led = c.state ? 0x30FF50 : 0x401010;
        box(pose, vc, x0 + w * 0.06f, y0 + h * 0.06f, x0 + w * 0.2f, y0 + h * 0.2f, 0.4f * PX, 0.6f * PX, led,
                c.state ? LightTexture.FULL_BRIGHT : light);
    }

    private void renderLamp(PoseStack pose, VertexConsumer vc, MultiBufferSource buffers, PanelComponent c,
                            float x0, float y0, float x1, float y1, int light) {
        box(pose, vc, x0, y0, x1, y1, 0, 0.4f * PX, 0x303438, light);
        float in = Math.min(x1 - x0, y1 - y0) * 0.1f;
        boolean lit = c.lit();
        int col = lit ? c.color : shade(c.color, 0.22f);
        int l = lit ? LightTexture.FULL_BRIGHT : light;
        box(pose, vc, x0 + in, y0 + in, x1 - in, y1 - in, 0.4f * PX, 0.9f * PX, col, l);
        drawText(pose, buffers, c.label, contrast(col), x0 + in, y0 + in, x1 - in, y1 - in, 0.9f * PX + 0.002f, l);
    }

    private void renderSegment(PoseStack pose, VertexConsumer vc, PanelComponent c, boolean wired, float x0, float y0, float x1, float y1) {
        box(pose, vc, x0, y0, x1, y1, 0, 0.3f * PX, 0x0C0C0C, LightTexture.FULL_BRIGHT);
        float zt = 0.3f * PX + 0.001f;
        int n = c.digits();
        int[] bits = new int[n];
        boolean[] dots = new boolean[n];
        String v = c.shownValue(wired).toUpperCase();
        int d = n - 1;
        for (int i = v.length() - 1; i >= 0 && d >= 0; i--) {
            char ch = v.charAt(i);
            if (ch == '.') {
                dots[d] = true;
                continue;
            }
            int idx = SEG_CHARS.indexOf(ch);
            bits[d--] = idx >= 0 ? SEG_BITS[idx] : 0x08;
        }
        float dw = (x1 - x0) / n;
        int on = c.color, off = shade(c.color, 0.12f);
        for (int i = 0; i < n; i++) {
            float dx0 = x0 + i * dw + dw * 0.15f, dx1 = x0 + (i + 1) * dw - dw * 0.2f;
            float dy0 = y0 + (y1 - y0) * 0.1f, dy1 = y1 - (y1 - y0) * 0.1f;
            float t = Math.min(dx1 - dx0, dy1 - dy0) * 0.16f;
            float my = (dy0 + dy1) / 2;
            float[][] seg = {
                    {dx0 + t, dy0, dx1 - t, dy0 + t},
                    {dx1 - t, dy0 + t, dx1, my - t / 2},
                    {dx1 - t, my + t / 2, dx1, dy1 - t},
                    {dx0 + t, dy1 - t, dx1 - t, dy1},
                    {dx0, my + t / 2, dx0 + t, dy1 - t},
                    {dx0, dy0 + t, dx0 + t, my - t / 2},
                    {dx0 + t, my - t / 2, dx1 - t, my + t / 2}
            };
            for (int s = 0; s < 7; s++) {
                boolean lit = (bits[i] & (1 << s)) != 0;
                quad(pose, vc, seg[s][0], seg[s][1], seg[s][2], seg[s][3], zt, lit ? on : off, LightTexture.FULL_BRIGHT);
            }
            quad(pose, vc, dx1 + dw * 0.04f, dy1 - t, dx1 + dw * 0.04f + t, dy1, zt, dots[i] ? on : off, LightTexture.FULL_BRIGHT);
        }
    }

    // ------------------------------------------------------------------ primitives

    private void drawText(PoseStack pose, MultiBufferSource buffers, String text, int color,
                          float x0, float y0, float x1, float y1, float w, int light) {
        if (text == null || text.isEmpty()) return;
        String[] lines = text.split("\n");
        int maxWidth = 1;
        for (String l : lines) maxWidth = Math.max(maxWidth, font.width(l));
        float boxW = (x1 - x0) * 0.9f, boxH = (y1 - y0) * 0.9f;
        float scale = Math.min(boxW / maxWidth, boxH / (lines.length * 10f));
        scale = Math.min(scale, C / 9f * 1.2f);
        pose.pushPose();
        pose.translate((x0 + x1) / 2, (y0 + y1) / 2, w);
        pose.scale(scale, scale, scale);
        float top = -lines.length * 10 / 2f + 1;
        for (int i = 0; i < lines.length; i++) {
            float lx = -font.width(lines[i]) / 2f;
            font.drawInBatch(lines[i], lx, top + i * 10, 0xFF000000 | color, false, pose.last().pose(), buffers,
                    Font.DisplayMode.POLYGON_OFFSET, 0, light);
        }
        pose.popPose();
    }

    /** Raised box from height h0 to h1 (top face + 4 sides). */
    private static void box(PoseStack pose, VertexConsumer vc, float x0, float y0, float x1, float y1, float h0, float h1, int color, int light) {
        PoseStack.Pose p = pose.last();
        int side = shade(color, 0.7f);
        quad(pose, vc, x0, y0, x1, y1, h1, color, light);
        vert(vc, p, x0, y0, h0, side, light, 0, -1, 0); vert(vc, p, x0, y0, h1, side, light, 0, -1, 0);
        vert(vc, p, x1, y0, h1, side, light, 0, -1, 0); vert(vc, p, x1, y0, h0, side, light, 0, -1, 0);
        vert(vc, p, x0, y1, h0, side, light, 0, 1, 0); vert(vc, p, x1, y1, h0, side, light, 0, 1, 0);
        vert(vc, p, x1, y1, h1, side, light, 0, 1, 0); vert(vc, p, x0, y1, h1, side, light, 0, 1, 0);
        vert(vc, p, x0, y0, h0, side, light, -1, 0, 0); vert(vc, p, x0, y1, h0, side, light, -1, 0, 0);
        vert(vc, p, x0, y1, h1, side, light, -1, 0, 0); vert(vc, p, x0, y0, h1, side, light, -1, 0, 0);
        vert(vc, p, x1, y0, h0, side, light, 1, 0, 0); vert(vc, p, x1, y0, h1, side, light, 1, 0, 0);
        vert(vc, p, x1, y1, h1, side, light, 1, 0, 0); vert(vc, p, x1, y1, h0, side, light, 1, 0, 0);
    }

    private static void quad(PoseStack pose, VertexConsumer vc, float x0, float y0, float x1, float y1, float h, int color, int light) {
        PoseStack.Pose p = pose.last();
        vert(vc, p, x0, y0, h, color, light, 0, 0, 1);
        vert(vc, p, x0, y1, h, color, light, 0, 0, 1);
        vert(vc, p, x1, y1, h, color, light, 0, 0, 1);
        vert(vc, p, x1, y0, h, color, light, 0, 0, 1);
    }

    private static void vert(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, int color, int light, float nx, float ny, float nz) {
        vc.addVertex(p, x, y, z)
                .setColor((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 255)
                .setUv(0.5f, 0.5f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(p, nx, ny, nz);
    }

    static int shade(int c, float f) {
        int r = (int) (((c >> 16) & 0xFF) * f), g = (int) (((c >> 8) & 0xFF) * f), b = (int) ((c & 0xFF) * f);
        return (r << 16) | (g << 8) | b;
    }

    private static int contrast(int c) {
        int lum = (((c >> 16) & 0xFF) * 299 + ((c >> 8) & 0xFF) * 587 + (c & 0xFF) * 114) / 1000;
        return lum > 140 ? 0x101010 : 0xFFFFFF;
    }

    @Override
    public boolean shouldRenderOffScreen(TileControlPanel be) {
        return !be.links().isEmpty();
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
