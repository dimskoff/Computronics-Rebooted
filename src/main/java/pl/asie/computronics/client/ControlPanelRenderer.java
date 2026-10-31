package pl.asie.computronics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
        Direction facing = panel.getFacing();

        poseStack.pushPose();
        Matrix4f pose = poseStack.last().pose();

        // Use OpenComputers' FONT_QUAD which provides NO_CULL and clean vertex coloring
        VertexConsumer vc = buffer.getBuffer(li.cil.oc.client.renderer.RenderTypes.FONT_QUAD);

        // Coordinate frame for the panel face
        float nx = 0, ny = 0, nz = 0; // Surface normal
        float tx = 0, ty = 0, tz = 0; // Surface tangent (along u / gx)
        float bx = 0, by = 0, bz = 0; // Surface bitangent (along v / gy)
        float baseOffsetX = 0, baseOffsetY = 0, baseOffsetZ = 0;

        switch (facing) {
            case UP -> {
                nx = 0; ny = 1; nz = 0;
                tx = 1; ty = 0; tz = 0;
                bx = 0; by = 0; bz = 1;
                baseOffsetY = 0.125f; // Y = 2/16
            }
            case DOWN -> {
                nx = 0; ny = -1; nz = 0;
                tx = 1; ty = 0; tz = 0;
                bx = 0; by = 0; bz = 1;
                baseOffsetY = 0.875f; // Y = 14/16
            }
            case NORTH -> {
                nx = 0; ny = 0; nz = -1;
                tx = 1; ty = 0; tz = 0;
                bx = 0; by = -1; bz = 0;
                baseOffsetY = 1.0f;
                baseOffsetZ = 0.875f; // Z = 14/16
            }
            case SOUTH -> {
                nx = 0; ny = 0; nz = 1;
                tx = -1; ty = 0; tz = 0;
                bx = 0; by = -1; bz = 0;
                baseOffsetX = 1.0f;
                baseOffsetY = 1.0f;
                baseOffsetZ = 0.125f; // Z = 2/16
            }
            case WEST -> {
                nx = -1; ny = 0; nz = 0;
                tx = 0; ty = 0; tz = -1;
                bx = 0; by = -1; bz = 0;
                baseOffsetX = 0.875f; // X = 14/16
                baseOffsetY = 1.0f;
                baseOffsetZ = 1.0f;
            }
            case EAST -> {
                nx = 1; ny = 0; nz = 0;
                tx = 0; ty = 0; tz = 1;
                bx = 0; by = -1; bz = 0;
                baseOffsetX = 0.125f; // X = 2/16
                baseOffsetY = 1.0f;
            }
        }

        for (int slot = 0; slot < TileControlPanel.TOTAL_SLOTS; slot++) {
            TileControlPanel.PanelElement elem = panel.getElement(slot);
            if (elem == null || elem.type == TileControlPanel.ElementType.NONE) continue;

            int gx = slot % TileControlPanel.GRID_SIZE;
            int gy = slot / TileControlPanel.GRID_SIZE;

            float u = (gx + 0.5f) / 8.0f;
            float v = (gy + 0.5f) / 8.0f;

            // Compute center point of this cell on the panel face
            float cx = baseOffsetX + u * tx + v * bx;
            float cy = baseOffsetY + u * ty + v * by;
            float cz = baseOffsetZ + u * tz + v * bz;

            if (elem.type == TileControlPanel.ElementType.BUTTON) {
                // 1. SMD Square Mounting Baseplate
                float baseSize = 0.046f;
                renderBox(pose, vc, cx, cy, cz, nx, ny, nz, tx, ty, tz, bx, by, bz,
                        baseSize, baseSize, 0.001f, 0.005f, 42, 46, 52);

                // 2. 3D Tactile Push Button Actuator Cap
                boolean pressed = elem.pressTicks > 0;
                float capSize = 0.034f;
                float h1 = 0.005f;
                float h2 = pressed ? 0.012f : 0.036f; // Visibly depresses into the panel

                int br = (elem.color >> 16) & 0xFF;
                int bg = (elem.color >> 8) & 0xFF;
                int bb = elem.color & 0xFF;
                if (elem.color == 0 || elem.color == 0x00FF00) {
                    // Classic tactile red button
                    br = 220; bg = 42; bb = 42;
                }

                renderBox(pose, vc, cx, cy, cz, nx, ny, nz, tx, ty, tz, bx, by, bz,
                        capSize, capSize, h1, h2, br, bg, bb);

                // Tactile cap center Pip / Ring
                renderQuadAt(pose, vc, cx, cy, cz, nx, ny, nz, tx, ty, tz, bx, by, bz,
                        0.014f, 0.014f, h2 + 0.001f,
                        Math.min(255, br + 50), Math.min(255, bg + 50), Math.min(255, bb + 50));

            } else if (elem.type == TileControlPanel.ElementType.LAMP) {
                // 1. Outer Dark Chrome Bezel Ring
                float bezelSize = 0.046f;
                renderBox(pose, vc, cx, cy, cz, nx, ny, nz, tx, ty, tz, bx, by, bz,
                        bezelSize, bezelSize, 0.001f, 0.005f, 35, 40, 48);

                // 2. 3D Raised Glowing LED Dome
                float domeSize = 0.034f;
                float h1 = 0.005f;
                float h2 = 0.026f;

                int col = elem.active ? elem.color : 0x1A221C;
                int r = (col >> 16) & 0xFF;
                int g = (col >> 8) & 0xFF;
                int b = col & 0xFF;

                renderBox(pose, vc, cx, cy, cz, nx, ny, nz, tx, ty, tz, bx, by, bz,
                        domeSize, domeSize, h1, h2, r, g, b);

                // Emissive full-bright core highlight when active
                if (elem.active) {
                    renderQuadAt(pose, vc, cx, cy, cz, nx, ny, nz, tx, ty, tz, bx, by, bz,
                            0.018f, 0.018f, h2 + 0.001f,
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
        // Front / Cap Quad (at h2)
        renderQuadAt(pose, vc, cx, cy, cz, nx, ny, nz, tx, ty, tz, bx, by, bz, su, sv, h2, r, g, b);

        // 4 Side Quads connecting h1 to h2
        // +U side
        renderSideQuad(pose, vc, cx, cy, cz, nx, ny, nz, tx, ty, tz, bx, by, bz,
                su, -sv, su, sv, h1, h2, (int)(r * 0.85), (int)(g * 0.85), (int)(b * 0.85));
        // -U side
        renderSideQuad(pose, vc, cx, cy, cz, nx, ny, nz, tx, ty, tz, bx, by, bz,
                -su, sv, -su, -sv, h1, h2, (int)(r * 0.80), (int)(g * 0.80), (int)(b * 0.80));
        // +V side
        renderSideQuad(pose, vc, cx, cy, cz, nx, ny, nz, tx, ty, tz, bx, by, bz,
                su, sv, -su, sv, h1, h2, (int)(r * 0.90), (int)(g * 0.90), (int)(b * 0.90));
        // -V side
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
        // Front winding
        vc.addVertex(pose, x1, y1, z1).setUv(0, 0).setColor(r, g, b, a);
        vc.addVertex(pose, x2, y2, z2).setUv(1, 0).setColor(r, g, b, a);
        vc.addVertex(pose, x3, y3, z3).setUv(1, 1).setColor(r, g, b, a);
        vc.addVertex(pose, x4, y4, z4).setUv(0, 1).setColor(r, g, b, a);

        // Reverse winding
        vc.addVertex(pose, x4, y4, z4).setUv(0, 1).setColor(r, g, b, a);
        vc.addVertex(pose, x3, y3, z3).setUv(1, 1).setColor(r, g, b, a);
        vc.addVertex(pose, x2, y2, z2).setUv(1, 0).setColor(r, g, b, a);
        vc.addVertex(pose, x1, y1, z1).setUv(0, 0).setColor(r, g, b, a);
    }
}
