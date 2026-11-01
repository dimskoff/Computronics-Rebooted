package pl.asie.computronics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Matrix4f;
import pl.asie.computronics.block.entity.TileControlPanel;

public class ControlPanelRenderer implements BlockEntityRenderer<TileControlPanel> {

    // Seven-segment bitmasks for hex characters 0-F (A=0x01, B=0x02, C=0x04, D=0x08, E=0x10, F=0x20, G=0x40)
    private static final byte[] SEG_DIGITS = {
            0x3F, // 0: A B C D E F
            0x06, // 1: B C
            0x5B, // 2: A B D E G
            0x4F, // 3: A B C D G
            0x66, // 4: B C F G
            0x6D, // 5: A C D F G
            0x7D, // 6: A C D E F G
            0x07, // 7: A B C
            0x7F, // 8: A B C D E F G
            0x6F, // 9: A B C D F G
            0x77, // A: A B C E F G
            0x7C, // B: C D E F G
            0x39, // C: A D E F
            0x5E, // D: B C D E G
            0x79, // E: A D E F G
            0x71  // F: A E F G
    };

    public ControlPanelRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(TileControlPanel panel, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState state = panel.getBlockState();
        if (!state.hasProperty(BlockStateProperties.ATTACH_FACE) || !state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return;
        }

        AttachFace face = state.getValue(BlockStateProperties.ATTACH_FACE);
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

        poseStack.pushPose();
        Matrix4f pose = poseStack.last().pose();
        VertexConsumer vc = buffer.getBuffer(li.cil.oc.client.renderer.RenderTypes.FONT_QUAD);
        Font font = Minecraft.getInstance().font;

        for (int slot = 0; slot < TileControlPanel.TOTAL_SLOTS; slot++) {
            TileControlPanel.PanelElement elem = panel.getElement(slot);
            if (elem == null || elem.type == TileControlPanel.ElementType.NONE) continue;

            int gx = slot % TileControlPanel.GRID_SIZE;
            int gy = slot / TileControlPanel.GRID_SIZE;
            int sizeX = Math.max(1, Math.min(4, elem.sizeX));
            int sizeY = Math.max(1, Math.min(4, elem.sizeY));

            float u1 = gx / 8.0f;
            float v1 = gy / 8.0f;
            float u2 = (gx + sizeX) / 8.0f;
            float v2 = (gy + sizeY) / 8.0f;

            float pad = 0.012f;
            float cu1 = u1 + pad;
            float cv1 = v1 + pad;
            float cu2 = u2 - pad;
            float cv2 = v2 - pad;

            if (elem.type == TileControlPanel.ElementType.BUTTON) {
                // Tactile push button
                boolean pressed = (System.currentTimeMillis() - elem.lastPressTime) < 250;
                float h1 = 0.006f;
                float h2 = pressed ? 0.014f : 0.038f;

                int br = (elem.color >> 16) & 0xFF;
                int bg = (elem.color >> 8) & 0xFF;
                int bb = elem.color & 0xFF;

                float capPad = 0.008f;
                renderSurfaceBox(pose, vc, face, facing, cu1 + capPad, cv1 + capPad, cu2 - capPad, cv2 - capPad,
                        h1, h2, br, bg, bb);

                // Top highlight
                renderFaceQuad(pose, vc, face, facing, cu1 + capPad + 0.006f, cv1 + capPad + 0.006f,
                        cu2 - capPad - 0.006f, cv2 - capPad - 0.006f, h2 + 0.001f,
                        Math.min(255, br + 45), Math.min(255, bg + 45), Math.min(255, bb + 45));

            } else if (elem.type == TileControlPanel.ElementType.SWITCH) {
                // Industrial Rocker / Toggle Switch
                // 1. Recessed dark casing
                renderFaceQuad(pose, vc, face, facing, cu1, cv1, cu2, cv2, 0.003f, 25, 28, 32);
                renderFaceQuad(pose, vc, face, facing, cu1 + 0.006f, cv1 + 0.006f, cu2 - 0.006f, cv2 - 0.006f, 0.004f, 10, 12, 14);

                // 2. Rocker lever (tilted up for ON, tilted down for OFF)
                float midV = (cv1 + cv2) / 2.0f;
                int sr = (elem.color >> 16) & 0xFF;
                int sg = (elem.color >> 8) & 0xFF;
                int sb = elem.color & 0xFF;

                float hUp = elem.active ? 0.032f : 0.012f;
                float hDown = elem.active ? 0.012f : 0.032f;

                // Upper half of rocker
                renderSurfaceBox(pose, vc, face, facing, cu1 + 0.010f, cv1 + 0.010f, cu2 - 0.010f, midV,
                        0.005f, hUp, (int)(sr * 0.9), (int)(sg * 0.9), (int)(sb * 0.9));
                // Lower half of rocker
                renderSurfaceBox(pose, vc, face, facing, cu1 + 0.010f, midV, cu2 - 0.010f, cv2 - 0.010f,
                        0.005f, hDown, (int)(sr * 0.6), (int)(sg * 0.6), (int)(sb * 0.6));

                // Glowing indicator dot when ON
                if (elem.active) {
                    renderFaceQuad(pose, vc, face, facing, cu1 + 0.018f, cv1 + 0.014f, cu2 - 0.018f, cv1 + 0.024f,
                            hUp + 0.002f, 0, 255, 120);
                }

            } else if (elem.type == TileControlPanel.ElementType.LAMP) {
                // Sleek, flat indicator lamp
                renderFaceQuad(pose, vc, face, facing, cu1, cv1, cu2, cv2, 0.002f, 50, 56, 64);

                float lensPad = 0.008f;
                int col = elem.active ? elem.color : 0x18201A;
                int r = (col >> 16) & 0xFF;
                int g = (col >> 8) & 0xFF;
                int b = col & 0xFF;

                renderFaceQuad(pose, vc, face, facing, cu1 + lensPad, cv1 + lensPad, cu2 - lensPad, cv2 - lensPad,
                        0.005f, r, g, b);

                if (elem.active) {
                    float corePad = lensPad + (sizeX * 0.015f);
                    renderFaceQuad(pose, vc, face, facing, cu1 + corePad, cv1 + corePad, cu2 - corePad, cv2 - corePad,
                            0.006f, Math.min(255, r + 90), Math.min(255, g + 90), Math.min(255, b + 90));
                }

            } else if (elem.type == TileControlPanel.ElementType.SEVEN_SEGMENT) {
                // Multi-digit horizontal 7-segment digital LED display
                renderFaceQuad(pose, vc, face, facing, cu1, cv1, cu2, cv2, 0.003f, 15, 18, 22);
                renderFaceQuad(pose, vc, face, facing, cu1 + 0.004f, cv1 + 0.004f, cu2 - 0.004f, cv2 - 0.004f,
                        0.004f, 5, 8, 12);

                String text = elem.displayValue != null && !elem.displayValue.isEmpty() ? elem.displayValue : "0";
                renderSevenSegmentDigits(pose, vc, face, facing, cu1 + 0.006f, cv1 + 0.006f, cu2 - 0.006f, cv2 - 0.006f,
                        0.007f, text, elem.color, elem.active, sizeX);
            }
        }

        poseStack.popPose();
    }

    private static void renderSevenSegmentDigits(Matrix4f pose, VertexConsumer vc, AttachFace face, Direction facing,
                                                float u1, float v1, float u2, float v2, float h,
                                                String text, int color, boolean active, int totalDigits) {
        int digits = Math.max(1, totalDigits);
        float totalW = u2 - u1;
        float digitW = totalW / digits;
        float digitH = v2 - v1;

        // Right-align or pad text to match digit count
        if (text.length() < digits) {
            text = " ".repeat(digits - text.length()) + text;
        } else if (text.length() > digits) {
            text = text.substring(text.length() - digits);
        }

        int onR = (color >> 16) & 0xFF;
        int onG = (color >> 8) & 0xFF;
        int onB = color & 0xFF;
        // Dim unlit segment color
        int offR = 12;
        int offG = 20;
        int offB = 16;

        for (int i = 0; i < digits; i++) {
            char ch = text.charAt(i);
            byte mask = active ? getSegmentMask(ch) : 0x00;

            float du1 = u1 + i * digitW + (digitW * 0.12f);
            float du2 = u1 + (i + 1) * digitW - (digitW * 0.12f);
            float dv1 = v1 + (digitH * 0.10f);
            float dv2 = v2 - (digitH * 0.10f);

            float w = du2 - du1;
            float th = w * 0.16f; // segment thickness
            float midV = (dv1 + dv2) / 2.0f;

            // A: Top horizontal
            renderSegment(pose, vc, face, facing, du1 + th, dv1, du2 - th, dv1 + th, h,
                    (mask & 0x01) != 0, onR, onG, onB, offR, offG, offB);
            // B: Top-right vertical
            renderSegment(pose, vc, face, facing, du2 - th, dv1 + th, du2, midV - (th / 2), h,
                    (mask & 0x02) != 0, onR, onG, onB, offR, offG, offB);
            // C: Bottom-right vertical
            renderSegment(pose, vc, face, facing, du2 - th, midV + (th / 2), du2, dv2 - th, h,
                    (mask & 0x04) != 0, onR, onG, onB, offR, offG, offB);
            // D: Bottom horizontal
            renderSegment(pose, vc, face, facing, du1 + th, dv2 - th, du2 - th, dv2, h,
                    (mask & 0x08) != 0, onR, onG, onB, offR, offG, offB);
            // E: Bottom-left vertical
            renderSegment(pose, vc, face, facing, du1, midV + (th / 2), du1 + th, dv2 - th, h,
                    (mask & 0x10) != 0, onR, onG, onB, offR, offG, offB);
            // F: Top-left vertical
            renderSegment(pose, vc, face, facing, du1, dv1 + th, du1 + th, midV - (th / 2), h,
                    (mask & 0x20) != 0, onR, onG, onB, offR, offG, offB);
            // G: Middle horizontal
            renderSegment(pose, vc, face, facing, du1 + th, midV - (th / 2), du2 - th, midV + (th / 2), h,
                    (mask & 0x40) != 0, onR, onG, onB, offR, offG, offB);
        }
    }

    private static void renderSegment(Matrix4f pose, VertexConsumer vc, AttachFace face, Direction facing,
                                     float u1, float v1, float u2, float v2, float h,
                                     boolean on, int onR, int onG, int onB, int offR, int offG, int offB) {
        int r = on ? onR : offR;
        int g = on ? onG : offG;
        int b = on ? onB : offB;
        renderFaceQuad(pose, vc, face, facing, u1, v1, u2, v2, h, r, g, b);
    }

    private static byte getSegmentMask(char ch) {
        ch = Character.toUpperCase(ch);
        if (ch >= '0' && ch <= '9') return SEG_DIGITS[ch - '0'];
        if (ch >= 'A' && ch <= 'F') return SEG_DIGITS[10 + (ch - 'A')];
        if (ch == '-') return 0x40;
        return 0x00;
    }

    private static void renderSurfaceBox(Matrix4f pose, VertexConsumer vc, AttachFace face, Direction facing,
                                         float u1, float v1, float u2, float v2,
                                         float h1, float h2, int r, int g, int b) {
        // Top cap
        renderFaceQuad(pose, vc, face, facing, u1, v1, u2, v2, h2, r, g, b);
        // Sides
        renderFaceQuadSide(pose, vc, face, facing, u1, v1, u2, v1, h1, h2, (int)(r * 0.85), (int)(g * 0.85), (int)(b * 0.85));
        renderFaceQuadSide(pose, vc, face, facing, u1, v2, u2, v2, h1, h2, (int)(r * 0.80), (int)(g * 0.80), (int)(b * 0.80));
        renderFaceQuadSide(pose, vc, face, facing, u1, v1, u1, v2, h1, h2, (int)(r * 0.90), (int)(g * 0.90), (int)(b * 0.90));
        renderFaceQuadSide(pose, vc, face, facing, u2, v1, u2, v2, h1, h2, (int)(r * 0.75), (int)(g * 0.75), (int)(b * 0.75));
    }

    private static float[] mapCoord(AttachFace face, Direction facing, float u, float v, float h) {
        float x, y, z;
        if (face == AttachFace.FLOOR) {
            y = 1.0f + h;
            switch (facing) {
                case NORTH -> { x = u; z = v; }
                case SOUTH -> { x = 1.0f - u; z = 1.0f - v; }
                case WEST  -> { x = v; z = 1.0f - u; }
                case EAST  -> { x = 1.0f - v; z = u; }
                default    -> { x = u; z = v; }
            }
        } else if (face == AttachFace.CEILING) {
            y = 0.0f - h;
            switch (facing) {
                case NORTH -> { x = u; z = 1.0f - v; }
                case SOUTH -> { x = 1.0f - u; z = v; }
                case WEST  -> { x = v; z = u; }
                case EAST  -> { x = 1.0f - v; z = 1.0f - u; }
                default    -> { x = u; z = 1.0f - v; }
            }
        } else { // WALL
            y = 1.0f - v;
            switch (facing) {
                case NORTH -> { z = 0.0f - h; x = u; }
                case SOUTH -> { z = 1.0f + h; x = 1.0f - u; }
                case WEST  -> { x = 0.0f - h; z = 1.0f - u; }
                case EAST  -> { x = 1.0f + h; z = u; }
                default    -> { z = 0.0f - h; x = u; }
            }
        }
        return new float[]{x, y, z};
    }

    private static void renderFaceQuad(Matrix4f pose, VertexConsumer vc, AttachFace face, Direction facing,
                                       float u1, float v1, float u2, float v2, float h,
                                       int r, int g, int b) {
        float[] p1 = mapCoord(face, facing, u1, v1, h);
        float[] p2 = mapCoord(face, facing, u2, v1, h);
        float[] p3 = mapCoord(face, facing, u2, v2, h);
        float[] p4 = mapCoord(face, facing, u1, v2, h);

        vc.addVertex(pose, p1[0], p1[1], p1[2]).setUv(0, 0).setColor(r, g, b, 255);
        vc.addVertex(pose, p2[0], p2[1], p2[2]).setUv(1, 0).setColor(r, g, b, 255);
        vc.addVertex(pose, p3[0], p3[1], p3[2]).setUv(1, 1).setColor(r, g, b, 255);
        vc.addVertex(pose, p4[0], p4[1], p4[2]).setUv(0, 1).setColor(r, g, b, 255);

        // Reverse winding
        vc.addVertex(pose, p4[0], p4[1], p4[2]).setUv(0, 1).setColor(r, g, b, 255);
        vc.addVertex(pose, p3[0], p3[1], p3[2]).setUv(1, 1).setColor(r, g, b, 255);
        vc.addVertex(pose, p2[0], p2[1], p2[2]).setUv(1, 0).setColor(r, g, b, 255);
        vc.addVertex(pose, p1[0], p1[1], p1[2]).setUv(0, 0).setColor(r, g, b, 255);
    }

    private static void renderFaceQuadSide(Matrix4f pose, VertexConsumer vc, AttachFace face, Direction facing,
                                           float u1, float v1, float u2, float v2, float h1, float h2,
                                           int r, int g, int b) {
        float[] p1 = mapCoord(face, facing, u1, v1, h1);
        float[] p2 = mapCoord(face, facing, u2, v2, h1);
        float[] p3 = mapCoord(face, facing, u2, v2, h2);
        float[] p4 = mapCoord(face, facing, u1, v1, h2);

        vc.addVertex(pose, p1[0], p1[1], p1[2]).setUv(0, 0).setColor(r, g, b, 255);
        vc.addVertex(pose, p2[0], p2[1], p2[2]).setUv(1, 0).setColor(r, g, b, 255);
        vc.addVertex(pose, p3[0], p3[1], p3[2]).setUv(1, 1).setColor(r, g, b, 255);
        vc.addVertex(pose, p4[0], p4[1], p4[2]).setUv(0, 1).setColor(r, g, b, 255);

        // Reverse winding
        vc.addVertex(pose, p4[0], p4[1], p4[2]).setUv(0, 1).setColor(r, g, b, 255);
        vc.addVertex(pose, p3[0], p3[1], p3[2]).setUv(1, 1).setColor(r, g, b, 255);
        vc.addVertex(pose, p2[0], p2[1], p2[2]).setUv(1, 0).setColor(r, g, b, 255);
        vc.addVertex(pose, p1[0], p1[1], p1[2]).setUv(0, 0).setColor(r, g, b, 255);
    }
}
