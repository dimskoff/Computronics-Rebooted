package pl.asie.computronics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import pl.asie.computronics.block.entity.TileControlPanel;

public class ControlPanelRenderer implements BlockEntityRenderer<TileControlPanel> {
    public ControlPanelRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(TileControlPanel panel, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Direction facing = panel.getHorizontalFacing();
        boolean isTop = panel.isTopHalf();

        poseStack.pushPose();
        Matrix4f pose = poseStack.last().pose();

        VertexConsumer vc = buffer.getBuffer(li.cil.oc.client.renderer.RenderTypes.FONT_QUAD);

        // Slab surface normal
        float nx = 0, ny = isTop ? -1.0f : 1.0f, nz = 0;
        float tx = 0, ty = 0, tz = 0; // along u / gx
        float bx = 0, by = 0, bz = 0; // along v / gy
        float baseOffsetX = 0, baseOffsetY = 0.5f, baseOffsetZ = 0;

        switch (facing) {
            case NORTH -> {
                tx = 1; tz = 0;
                bx = 0; bz = 1;
                baseOffsetX = 0; baseOffsetZ = 0;
            }
            case SOUTH -> {
                tx = -1; tz = 0;
                bx = 0; bz = -1;
                baseOffsetX = 1.0f; baseOffsetZ = 1.0f;
            }
            case WEST -> {
                tx = 0; tz = -1;
                bx = 1; bz = 0;
                baseOffsetX = 1.0f; baseOffsetZ = 0;
            }
            case EAST -> {
                tx = 0; tz = 1;
                bx = -1; bz = 0;
                baseOffsetX = 0; baseOffsetZ = 1.0f;
            }
            default -> {
                tx = 1; tz = 0;
                bx = 0; bz = 1;
            }
        }

        Font font = Minecraft.getInstance().font;

        for (int slot = 0; slot < TileControlPanel.TOTAL_SLOTS; slot++) {
            TileControlPanel.PanelElement elem = panel.getElement(slot);
            if (elem == null || elem.type == TileControlPanel.ElementType.NONE) continue;

            int gx = slot % TileControlPanel.GRID_SIZE;
            int gy = slot / TileControlPanel.GRID_SIZE;
            int size = Math.max(1, Math.min(4, elem.size));

            float u = (gx + size * 0.5f) / 8.0f;
            float v = (gy + size * 0.5f) / 8.0f;

            float cx = baseOffsetX + u * tx + v * bx;
            float cy = baseOffsetY + (isTop ? -0.001f : 0.001f);
            float cz = baseOffsetZ + u * tz + v * bz;

            float halfSpan = (size / 8.0f) * 0.5f;
            float baseSize = halfSpan - 0.008f;
            float capSize = halfSpan - 0.016f;

            if (elem.type == TileControlPanel.ElementType.BUTTON) {
                // 1. SMD Baseplate
                renderBox(pose, vc, cx, cy, cz, nx, ny, nz, tx, ty, tz, bx, by, bz,
                        baseSize, baseSize, 0.001f, 0.006f, 38, 42, 48);

                // 2. Tactile Button Cap (Depresses down on click for 250ms then pops up)
                boolean pressed = (System.currentTimeMillis() - elem.lastPressTime) < 250;
                float h1 = 0.006f;
                float h2 = pressed ? 0.014f : 0.042f;

                int br = (elem.color >> 16) & 0xFF;
                int bg = (elem.color >> 8) & 0xFF;
                int bb = elem.color & 0xFF;

                renderBox(pose, vc, cx, cy, cz, nx, ny, nz, tx, ty, tz, bx, by, bz,
                        capSize, capSize, h1, h2, br, bg, bb);

                // Button Bevel / Highlight Pip
                if (elem.label == null || elem.label.isEmpty()) {
                    float pipSize = Math.max(0.012f, capSize * 0.35f);
                    renderQuadAt(pose, vc, cx, cy, cz, nx, ny, nz, tx, ty, tz, bx, by, bz,
                            pipSize, pipSize, h2 + 0.001f,
                            Math.min(255, br + 50), Math.min(255, bg + 50), Math.min(255, bb + 50));
                } else {
                    // Render 3D Label text on the button cap
                    poseStack.pushPose();
                    poseStack.translate(cx, cy + (h2 + 0.002f) * ny, cz);

                    // Align text with panel facing
                    switch (facing) {
                        case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
                        case SOUTH -> {}
                        case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
                        case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270));
                        default -> {}
                    }
                    if (isTop) {
                        poseStack.mulPose(Axis.ZP.rotationDegrees(180));
                    }
                    poseStack.mulPose(Axis.XP.rotationDegrees(90));

                    float textScale = (size * 0.0055f);
                    poseStack.scale(textScale, textScale, textScale);
                    int textW = font.width(elem.label);
                    font.drawInBatch(elem.label, -textW / 2.0f, -4.0f, elem.textColor, false,
                            poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, 0x00F000F0);
                    poseStack.popPose();
                }

            } else if (elem.type == TileControlPanel.ElementType.LAMP) {
                // 1. Chrome Bezel Ring
                renderBox(pose, vc, cx, cy, cz, nx, ny, nz, tx, ty, tz, bx, by, bz,
                        baseSize, baseSize, 0.001f, 0.006f, 50, 56, 64);

                // 2. 3D Glowing LED Dome
                float h1 = 0.006f;
                float h2 = 0.030f;

                int col = elem.active ? elem.color : 0x18201A;
                int r = (col >> 16) & 0xFF;
                int g = (col >> 8) & 0xFF;
                int b = col & 0xFF;

                renderBox(pose, vc, cx, cy, cz, nx, ny, nz, tx, ty, tz, bx, by, bz,
                        capSize, capSize, h1, h2, r, g, b);

                if (elem.active) {
                    float coreSize = capSize * 0.5f;
                    renderQuadAt(pose, vc, cx, cy, cz, nx, ny, nz, tx, ty, tz, bx, by, bz,
                            coreSize, coreSize, h2 + 0.001f,
                            Math.min(255, r + 90), Math.min(255, g + 90), Math.min(255, b + 90));
                }
            }
        }

        poseStack.popPose();
    }

    private static void renderBox(Matrix4f pose, VertexConsumer vc,
                                  float cx, float cy, float cz,
                                  float nx, float ny, float nz,
                                  float tx, float ty, float tz,
                                  float bx, float by, float bz,
                                  float su, float sv, float h1, float h2,
                                  int r, int g, int b) {
        renderQuadAt(pose, vc, cx, cy, cz, nx, ny, nz, tx, ty, tz, bx, by, bz, su, sv, h2, r, g, b);
        renderSideQuad(pose, vc, cx, cy, cz, nx, ny, nz, tx, ty, tz, bx, by, bz,
                su, -sv, su, sv, h1, h2, (int)(r * 0.85), (int)(g * 0.85), (int)(b * 0.85));
        renderSideQuad(pose, vc, cx, cy, cz, nx, ny, nz, tx, ty, tz, bx, by, bz,
                -su, sv, -su, -sv, h1, h2, (int)(r * 0.80), (int)(g * 0.80), (int)(b * 0.80));
        renderSideQuad(pose, vc, cx, cy, cz, nx, ny, nz, tx, ty, tz, bx, by, bz,
                su, sv, -su, sv, h1, h2, (int)(r * 0.90), (int)(g * 0.90), (int)(b * 0.90));
        renderSideQuad(pose, vc, cx, cy, cz, nx, ny, nz, tx, ty, tz, bx, by, bz,
                -su, -sv, su, -sv, h1, h2, (int)(r * 0.75), (int)(g * 0.75), (int)(b * 0.75));
    }

    private static void renderQuadAt(Matrix4f pose, VertexConsumer vc,
                                     float cx, float cy, float cz,
                                     float nx, float ny, float nz,
                                     float tx, float ty, float tz,
                                     float bx, float by, float bz,
                                     float su, float sv, float h,
                                     int r, int g, int b) {
        float x1 = cx + h * nx - su * tx - sv * bx;
        float y1 = cy + h * ny - su * ty - sv * by;
        float z1 = cz + h * nz - su * tz - sv * bz;

        float x2 = cx + h * nx + su * tx - sv * bx;
        float y2 = cy + h * ny + su * ty - sv * by;
        float z2 = cz + h * nz + su * tz - sv * bz;

        float x3 = cx + h * nx + su * tx + sv * bx;
        float y3 = cy + h * ny + su * ty + sv * by;
        float z3 = cz + h * nz + su * tz + sv * bz;

        float x4 = cx + h * nx - su * tx + sv * bx;
        float y4 = cy + h * ny - su * ty + sv * by;
        float z4 = cz + h * nz - su * tz + sv * bz;

        addDoubleSidedQuad(pose, vc, x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, r, g, b, 255);
    }

    private static void renderSideQuad(Matrix4f pose, VertexConsumer vc,
                                       float cx, float cy, float cz,
                                       float nx, float ny, float nz,
                                       float tx, float ty, float tz,
                                       float bx, float by, float bz,
                                       float u1, float v1, float u2, float v2, float h1, float h2,
                                       int r, int g, int b) {
        float x1 = cx + h1 * nx + u1 * tx + v1 * bx;
        float y1 = cy + h1 * ny + u1 * ty + v1 * by;
        float z1 = cz + h1 * nz + u1 * tz + v1 * bz;

        float x2 = cx + h1 * nx + u2 * tx + v2 * bx;
        float y2 = cy + h1 * ny + u2 * ty + v2 * by;
        float z2 = cz + h1 * nz + u2 * tz + v2 * bz;

        float x3 = cx + h2 * nx + u2 * tx + v2 * bx;
        float y3 = cy + h2 * ny + u2 * ty + v2 * by;
        float z3 = cz + h2 * nz + u2 * tz + v2 * bz;

        float x4 = cx + h2 * nx + u1 * tx + v1 * bx;
        float y4 = cy + h2 * ny + u1 * ty + v1 * by;
        float z4 = cz + h2 * nz + u1 * tz + v1 * bz;

        addDoubleSidedQuad(pose, vc, x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, r, g, b, 255);
    }

    private static void addDoubleSidedQuad(Matrix4f pose, VertexConsumer vc,
                                           float x1, float y1, float z1,
                                           float x2, float y2, float z2,
                                           float x3, float y3, float z3,
                                           float x4, float y4, float z4,
                                           int r, int g, int b, int a) {
        vc.addVertex(pose, x1, y1, z1).setUv(0, 0).setColor(r, g, b, a);
        vc.addVertex(pose, x2, y2, z2).setUv(1, 0).setColor(r, g, b, a);
        vc.addVertex(pose, x3, y3, z3).setUv(1, 1).setColor(r, g, b, a);
        vc.addVertex(pose, x4, y4, z4).setUv(0, 1).setColor(r, g, b, a);

        vc.addVertex(pose, x4, y4, z4).setUv(0, 1).setColor(r, g, b, a);
        vc.addVertex(pose, x3, y3, z3).setUv(1, 1).setColor(r, g, b, a);
        vc.addVertex(pose, x2, y2, z2).setUv(1, 0).setColor(r, g, b, a);
        vc.addVertex(pose, x1, y1, z1).setUv(0, 0).setColor(r, g, b, a);
    }
}
