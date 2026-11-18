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
import pl.asie.computronics.Computronics;
import pl.asie.computronics.block.entity.TileControlPanel;
import pl.asie.computronics.panel.PanelComponent;
import pl.asie.computronics.panel.PanelOrientation;

/**
 * Draws panel components in "panel space" (u right, v down, w towards the viewer, units = blocks),
 * see {@link PanelOrientation}. One transform handles floor, ceiling and all wall orientations.
 */
public class ControlPanelRenderer implements BlockEntityRenderer<TileControlPanel> {
    private static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath(Computronics.MODID, "textures/misc/white.png");
    private static final float PX = 1 / 16f;
    private static final float C = PanelOrientation.CELL;
    private static final float M = PanelOrientation.MARGIN;

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

    @Override
    public void render(TileControlPanel panel, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var components = panel.layout().snapshot();
        if (components.isEmpty()) return;
        long time = panel.getLevel() != null ? panel.getLevel().getGameTime() : 0;

        pose.pushPose();
        pose.mulPose(PanelOrientation.of(panel.getBlockState()).matrix());
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));

        for (PanelComponent c : components) {
            float x0 = M + c.x * C, y0 = M + c.y * C;
            float x1 = x0 + c.width * C, y1 = y0 + c.height * C;
            float pad = C * 0.08f;
            x0 += pad; y0 += pad; x1 -= pad; y1 -= pad;
            switch (c.type) {
                case BUTTON -> renderButton(pose, vc, buffers, c, time, x0, y0, x1, y1, light);
                case SWITCH -> renderSwitch(pose, vc, c, x0, y0, x1, y1, light);
                case LAMP -> renderLamp(pose, vc, buffers, c, x0, y0, x1, y1, light);
                case SEGMENT -> renderSegment(pose, vc, c, x0, y0, x1, y1);
                case LABEL -> drawText(pose, buffers, c.label, c.color, x0, y0, x1, y1, 0.002f, light);
            }
        }
        pose.popPose();
    }

    private void renderButton(PoseStack pose, VertexConsumer vc, MultiBufferSource buffers, PanelComponent c, long time,
                              float x0, float y0, float x1, float y1, int light) {
        boolean pressed = time < c.pressedUntil;
        box(pose, vc, x0, y0, x1, y1, 0, 0.4f * PX, 0x202428, light);                     // bezel
        float in = Math.min(x1 - x0, y1 - y0) * 0.12f;
        float h = pressed ? 0.6f * PX : 1.4f * PX;
        int col = pressed ? shade(c.color, 0.7f) : c.color;
        box(pose, vc, x0 + in, y0 + in, x1 - in, y1 - in, 0.4f * PX, h, col, light);
        drawText(pose, buffers, c.label, contrast(c.color), x0 + in, y0 + in, x1 - in, y1 - in, h + 0.002f, light);
    }

    private void renderSwitch(PoseStack pose, VertexConsumer vc, PanelComponent c, float x0, float y0, float x1, float y1, int light) {
        box(pose, vc, x0, y0, x1, y1, 0, 0.4f * PX, 0x202428, light);
        float w = x1 - x0, h = y1 - y0;
        float cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
        // slot
        box(pose, vc, cx - w * 0.12f, y0 + h * 0.18f, cx + w * 0.12f, y1 - h * 0.18f, 0.4f * PX, 0.5f * PX, 0x0A0A0A, light);
        // lever: up = on
        float ly = c.state ? y0 + h * 0.15f : cy + h * 0.05f;
        box(pose, vc, cx - w * 0.2f, ly, cx + w * 0.2f, ly + h * 0.3f, 0.5f * PX, 2.0f * PX, c.color, light);
        // status LED
        int led = c.state ? 0x30FF50 : 0x401010;
        box(pose, vc, x0 + w * 0.06f, y0 + h * 0.06f, x0 + w * 0.2f, y0 + h * 0.2f, 0.4f * PX, 0.6f * PX, led,
                c.state ? LightTexture.FULL_BRIGHT : light);
    }

    private void renderLamp(PoseStack pose, VertexConsumer vc, MultiBufferSource buffers, PanelComponent c,
                            float x0, float y0, float x1, float y1, int light) {
        box(pose, vc, x0, y0, x1, y1, 0, 0.4f * PX, 0x303438, light);
        float in = Math.min(x1 - x0, y1 - y0) * 0.1f;
        int col = c.state ? c.color : shade(c.color, 0.22f);
        box(pose, vc, x0 + in, y0 + in, x1 - in, y1 - in, 0.4f * PX, 0.9f * PX, col, c.state ? LightTexture.FULL_BRIGHT : light);
        drawText(pose, buffers, c.label, contrast(col), x0 + in, y0 + in, x1 - in, y1 - in, 0.9f * PX + 0.002f,
                c.state ? LightTexture.FULL_BRIGHT : light);
    }

    private void renderSegment(PoseStack pose, VertexConsumer vc, PanelComponent c, float x0, float y0, float x1, float y1) {
        int bg = 0x0C0C0C;
        quad(pose, vc, x0, y0, x1, y1, 0.0015f, bg, LightTexture.FULL_BRIGHT);
        int n = c.digits();
        // right-align the text, '.' attaches to the previous digit
        int[] bits = new int[n];
        boolean[] dots = new boolean[n];
        String v = c.value.toUpperCase();
        int d = n - 1;
        for (int i = v.length() - 1; i >= 0 && d >= 0; i--) {
            char ch = v.charAt(i);
            if (ch == '.') {
                dots[d] = true;
                continue;
            }
            int idx = SEG_CHARS.indexOf(ch);
            bits[d--] = idx >= 0 ? SEG_BITS[idx] : 0x08; // unknown -> underscore
        }
        float dw = (x1 - x0) / n;
        int on = c.color, off = shade(c.color, 0.12f);
        for (int i = 0; i < n; i++) {
            float dx0 = x0 + i * dw + dw * 0.15f, dx1 = x0 + (i + 1) * dw - dw * 0.2f;
            float dy0 = y0 + (y1 - y0) * 0.1f, dy1 = y1 - (y1 - y0) * 0.1f;
            float t = Math.min(dx1 - dx0, dy1 - dy0) * 0.16f;
            float my = (dy0 + dy1) / 2;
            float[][] seg = {
                    {dx0 + t, dy0, dx1 - t, dy0 + t},          // a
                    {dx1 - t, dy0 + t, dx1, my - t / 2},       // b
                    {dx1 - t, my + t / 2, dx1, dy1 - t},       // c
                    {dx0 + t, dy1 - t, dx1 - t, dy1},          // d
                    {dx0, my + t / 2, dx0 + t, dy1 - t},       // e
                    {dx0, dy0 + t, dx0 + t, my - t / 2},       // f
                    {dx0 + t, my - t / 2, dx1 - t, my + t / 2} // g
            };
            for (int s = 0; s < 7; s++) {
                boolean lit = (bits[i] & (1 << s)) != 0;
                quad(pose, vc, seg[s][0], seg[s][1], seg[s][2], seg[s][3], 0.003f, lit ? on : off, LightTexture.FULL_BRIGHT);
            }
            quad(pose, vc, dx1 + dw * 0.04f, dy1 - t, dx1 + dw * 0.04f + t, dy1, 0.003f, dots[i] ? on : off, LightTexture.FULL_BRIGHT);
        }
    }

    // ------------------------------------------------------------------ primitives

    /** Text centered and scaled to fit the rectangle; '\n' separates lines. */
    private void drawText(PoseStack pose, MultiBufferSource buffers, String text, int color,
                          float x0, float y0, float x1, float y1, float w, int light) {
        if (text == null || text.isEmpty()) return;
        String[] lines = text.split("\n");
        int maxWidth = 1;
        for (String l : lines) maxWidth = Math.max(maxWidth, font.width(l));
        float boxW = (x1 - x0) * 0.9f, boxH = (y1 - y0) * 0.9f;
        float scale = Math.min(boxW / maxWidth, boxH / (lines.length * 10f));
        scale = Math.min(scale, C / 9f * 1.2f); // never bigger than ~1 cell per line
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
        // sides
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

    private static int shade(int c, float f) {
        int r = (int) (((c >> 16) & 0xFF) * f), g = (int) (((c >> 8) & 0xFF) * f), b = (int) ((c & 0xFF) * f);
        return (r << 16) | (g << 8) | b;
    }

    private static int contrast(int c) {
        int lum = (((c >> 16) & 0xFF) * 299 + ((c >> 8) & 0xFF) * 587 + (c & 0xFF) * 114) / 1000;
        return lum > 140 ? 0x101010 : 0xFFFFFF;
    }

    @Override
    public boolean shouldRenderOffScreen(TileControlPanel be) {
        return false;
    }
}
