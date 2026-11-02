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

import java.util.ArrayList;
import java.util.List;

public class ControlPanelRenderer implements BlockEntityRenderer<TileControlPanel> {

    // Standard 7-segment bitmasks (A=0x01, B=0x02, C=0x04, D=0x08, E=0x10, F=0x20, G=0x40)
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
            0x7C, // B: C D E F G (b)
            0x39, // C: A D E F
            0x5E, // D: B C D E G (d)
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

        // ====================================================================
        // PASS 1: Render All Solid Geometry (Buttons, Lamps, Switches, 7-Seg)
        // ====================================================================
        poseStack.pushPose();
        Matrix4f pose = poseStack.last().pose();
        VertexConsumer vc = buffer.getBuffer(li.cil.oc.client.renderer.RenderTypes.FONT_QUAD);

        for (int slot = 0; slot < TileControlPanel.TOTAL_SLOTS; slot++) {
            TileControlPanel.PanelElement elem = panel.getElement(slot);
            if (elem == null || elem.type == TileControlPanel.ElementType.NONE) continue;

            int gx = slot % TileControlPanel.GRID_SIZE;
            int gy = slot / TileControlPanel.GRID_SIZE;
            int sizeX = Math.max(1, Math.min(8, elem.sizeX));
            int sizeY = Math.max(1, Math.min(8, elem.sizeY));

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
                renderFaceQuad(pose, vc, face, facing, cu1, cv1, cu2, cv2, 0.003f, 25, 28, 32);
                renderFaceQuad(pose, vc, face, facing, cu1 + 0.006f, cv1 + 0.006f, cu2 - 0.006f, cv2 - 0.006f, 0.004f, 10, 12, 14);

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
                // Sleek indicator lamp
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
                // High-fidelity slanted 7-segment digital LED display matching MicroTechnics diagram
                renderFaceQuad(pose, vc, face, facing, cu1, cv1, cu2, cv2, 0.003f, 15, 18, 22);
                renderFaceQuad(pose, vc, face, facing, cu1 + 0.004f, cv1 + 0.004f, cu2 - 0.004f, cv2 - 0.004f,
                        0.004f, 6, 8, 11);

                String text = elem.displayValue != null && !elem.displayValue.isEmpty() ? elem.displayValue : "0";
                renderSevenSegmentDisplay(pose, vc, face, facing, cu1 + 0.006f, cv1 + 0.006f, cu2 - 0.006f, cv2 - 0.006f,
                        0.007f, text, elem.color, elem.active, sizeX);

            } else if (elem.type == TileControlPanel.ElementType.TEXT) {
                // User requested: NO background box on text element, ONLY the text itself!
            }
        }
        poseStack.popPose();

        // ====================================================================
        // PASS 2: Render All In-World Text Labels (Buttons, Lamps, Text Panels)
        // ====================================================================
        Font font = Minecraft.getInstance().font;
        for (int slot = 0; slot < TileControlPanel.TOTAL_SLOTS; slot++) {
            TileControlPanel.PanelElement elem = panel.getElement(slot);
            if (elem == null || elem.type == TileControlPanel.ElementType.NONE) continue;
            if (elem.label == null || elem.label.trim().isEmpty()) continue;

            int gx = slot % TileControlPanel.GRID_SIZE;
            int gy = slot / TileControlPanel.GRID_SIZE;
            int sizeX = Math.max(1, Math.min(8, elem.sizeX));
            int sizeY = Math.max(1, Math.min(8, elem.sizeY));

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
                boolean pressed = (System.currentTimeMillis() - elem.lastPressTime) < 250;
                float h2 = pressed ? 0.014f : 0.038f;
                float capPad = 0.008f;
                renderInWorldText(poseStack, buffer, font, face, facing,
                        cu1 + capPad, cv1 + capPad, cu2 - capPad, cv2 - capPad, h2 + 0.002f,
                        elem.label, elem.textColor, elem.bold, 0.85f);
            } else if (elem.type == TileControlPanel.ElementType.LAMP) {
                renderInWorldText(poseStack, buffer, font, face, facing,
                        cu1, cv1, cu2, cv2, 0.008f,
                        elem.label, elem.textColor, elem.bold, 0.85f);
            } else if (elem.type == TileControlPanel.ElementType.TEXT) {
                renderInWorldText(poseStack, buffer, font, face, facing,
                        cu1, cv1, cu2, cv2, 0.008f,
                        elem.label, elem.textColor, elem.bold, elem.textScale);
            }
        }
    }

    // ========================================================================
    // 7-Segment High-Fidelity Slanted Geometry (MicroTechnics.ru Diagram)
    // ========================================================================

    private static class DigitSlot {
        char ch;
        boolean hasDot;
        DigitSlot(char ch, boolean hasDot) {
            this.ch = ch;
            this.hasDot = hasDot;
        }
    }

    private static void renderSevenSegmentDisplay(Matrix4f pose, VertexConsumer vc, AttachFace face, Direction facing,
                                                 float u1, float v1, float u2, float v2, float h,
                                                 String rawText, int color, boolean active, int totalDigits) {
        int digitsCount = Math.max(1, totalDigits);

        // Parse text into individual digit slots with decimal points
        List<DigitSlot> parsed = new ArrayList<>();
        int len = rawText.length();
        for (int i = 0; i < len; i++) {
            char c = rawText.charAt(i);
            if (c == '.') {
                if (!parsed.isEmpty()) {
                    parsed.get(parsed.size() - 1).hasDot = true;
                } else {
                    parsed.add(new DigitSlot('0', true));
                }
            } else {
                parsed.add(new DigitSlot(c, false));
            }
        }

        // Align right to match digitsCount
        List<DigitSlot> slots = new ArrayList<>();
        if (parsed.size() < digitsCount) {
            int pad = digitsCount - parsed.size();
            for (int k = 0; k < pad; k++) {
                slots.add(new DigitSlot(' ', false));
            }
            slots.addAll(parsed);
        } else if (parsed.size() > digitsCount) {
            slots = parsed.subList(parsed.size() - digitsCount, parsed.size());
        } else {
            slots = parsed;
        }

        float totalW = u2 - u1;
        float slotW = totalW / digitsCount;
        float totalH = v2 - v1;

        int onR = (color >> 16) & 0xFF;
        int onG = (color >> 8) & 0xFF;
        int onB = color & 0xFF;

        // Faint realistic unlit segment tint
        int offR = Math.max(12, (int)(onR * 0.12f));
        int offG = Math.max(16, (int)(onG * 0.12f));
        int offB = Math.max(18, (int)(onB * 0.12f));

        for (int i = 0; i < digitsCount; i++) {
            DigitSlot slot = slots.get(i);
            byte mask = active ? getSegmentMask(slot.ch) : 0x00;

            float slotU1 = u1 + i * slotW;
            float slotU2 = u1 + (i + 1) * slotW;

            // Digit proportions: Aspect ratio ~1:1.8 (MicroTechnics)
            float digitH = totalH * 0.84f;
            float digitW = Math.min(slotW * 0.70f, digitH * 0.52f);

            float centerSlotU = (slotU1 + slotU2) * 0.5f;
            float du1 = centerSlotU - (digitW * 0.5f);
            float du2 = centerSlotU + (digitW * 0.5f);
            float dv1 = v1 + (totalH - digitH) * 0.5f;
            float dv2 = dv1 + digitH;

            renderSingleSlantedDigit(pose, vc, face, facing, du1, dv1, du2, dv2, h,
                    mask, slot.hasDot && active, onR, onG, onB, offR, offG, offB);
        }
    }

    private static void renderSingleSlantedDigit(Matrix4f pose, VertexConsumer vc, AttachFace face, Direction facing,
                                                float du1, float dv1, float du2, float dv2, float h,
                                                byte mask, boolean dotActive,
                                                int onR, int onG, int onB, int offR, int offG, int offB) {
        float H = dv2 - dv1;
        float W = du2 - du1;
        float th = W * 0.18f;           // Segment thickness
        float gap = th * 0.15f;          // Gap between segments
        float slant = H * 0.11f;         // ~6.3 degree italic slant
        float midV = (dv1 + dv2) * 0.5f;

        // Slant function: shifts U to the right as V goes up towards dv1
        class Slanted {
            float su(float u, float v) {
                return u + ((dv2 - v) / H) * slant;
            }
            void poly(float uA, float vA, float uB, float vB, float uC, float vC, float uD, float vD, boolean on) {
                int r = on ? onR : offR;
                int g = on ? onG : offG;
                int b = on ? onB : offB;
                renderFacePoly4(pose, vc, face, facing,
                        su(uA, vA), vA,
                        su(uB, vB), vB,
                        su(uC, vC), vC,
                        su(uD, vD), vD,
                        h, r, g, b);
            }
        }
        Slanted s = new Slanted();

        // 1. Segment A (Top horizontal trapezoid)
        s.poly(du1 + gap, dv1,
               du2 - gap, dv1,
               du2 - th - gap, dv1 + th,
               du1 + th + gap, dv1 + th,
               (mask & 0x01) != 0);

        // 2. Segment B (Top-right vertical trapezoid)
        s.poly(du2 - th, dv1 + th + gap,
               du2, dv1 + gap,
               du2, midV - gap,
               du2 - th, midV - (th * 0.5f) - gap,
               (mask & 0x02) != 0);

        // 3. Segment C (Bottom-right vertical trapezoid)
        s.poly(du2 - th, midV + (th * 0.5f) + gap,
               du2, midV + gap,
               du2, dv2 - gap,
               du2 - th, dv2 - th - gap,
               (mask & 0x04) != 0);

        // 4. Segment D (Bottom horizontal trapezoid)
        s.poly(du1 + th + gap, dv2 - th,
               du2 - th - gap, dv2 - th,
               du2 - gap, dv2,
               du1 + gap, dv2,
               (mask & 0x08) != 0);

        // 5. Segment E (Bottom-left vertical trapezoid)
        s.poly(du1, midV + gap,
               du1 + th, midV + (th * 0.5f) + gap,
               du1 + th, dv2 - th - gap,
               du1, dv2 - gap,
               (mask & 0x10) != 0);

        // 6. Segment F (Top-left vertical trapezoid)
        s.poly(du1, dv1 + gap,
               du1 + th, dv1 + th + gap,
               du1 + th, midV - (th * 0.5f) - gap,
               du1, midV - gap,
               (mask & 0x20) != 0);

        // 7. Segment G (Middle horizontal hexagon split into 2 trapezoids)
        boolean segG = (mask & 0x40) != 0;
        // Upper half of hexagon
        s.poly(du1 + gap, midV,
               du1 + (th * 0.55f) + gap, midV - (th * 0.5f),
               du2 - (th * 0.55f) - gap, midV - (th * 0.5f),
               du2 - gap, midV,
               segG);
        // Lower half of hexagon
        s.poly(du1 + gap, midV,
               du2 - gap, midV,
               du2 - (th * 0.55f) - gap, midV + (th * 0.5f),
               du1 + (th * 0.55f) + gap, midV + (th * 0.5f),
               segG);

        // 8. Decimal Point (DP) at bottom right
        float dpW = th * 0.85f;
        float dpX1 = du2 + (gap * 2.5f);
        float dpX2 = dpX1 + dpW;
        float dpY1 = dv2 - dpW;
        float dpY2 = dv2;
        s.poly(dpX1, dpY1, dpX2, dpY1, dpX2, dpY2, dpX1, dpY2, dotActive);
    }

    private static byte getSegmentMask(char ch) {
        ch = Character.toUpperCase(ch);
        if (ch >= '0' && ch <= '9') return SEG_DIGITS[ch - '0'];
        if (ch >= 'A' && ch <= 'F') return SEG_DIGITS[10 + (ch - 'A')];
        if (ch == '-') return 0x40;
        if (ch == '_') return 0x08;
        return 0x00;
    }

    // ========================================================================
    // In-World 3D Text Rendering via Font & PoseStack
    // ========================================================================

    private static void renderInWorldText(PoseStack poseStack, MultiBufferSource buffer, Font font,
                                          AttachFace face, Direction facing,
                                          float cu1, float cv1, float cu2, float cv2, float h,
                                          String rawText, int textColor, boolean bold, float textScale) {
        try {
            if (!pl.asie.computronics.config.ComputronicsConfig.CLIENT.renderControlPanelWorldText.get()) return;
        } catch (Throwable ignored) {}
        if (rawText == null || rawText.trim().isEmpty()) return;

        // Multi-line support: split by \n or \r\n (up to 4 lines)
        String[] lines = rawText.split("\\r?\\n");
        if (lines.length > 4) {
            String[] trimmed = new String[4];
            System.arraycopy(lines, 0, trimmed, 0, 4);
            lines = trimmed;
        }

        float uMid = (cu1 + cu2) * 0.5f;
        float vMid = (cv1 + cv2) * 0.5f;

        float[] center = mapCoord(face, facing, uMid, vMid, h);

        float maxLineWidth = 0;
        for (String line : lines) {
            maxLineWidth = Math.max(maxLineWidth, font.width(line));
        }
        if (maxLineWidth <= 0) return;

        float boxW = (cu2 - cu1);
        float boxH = (cv2 - cv1);

        float lineHeight = 9.5f;
        float totalTextHeight = lines.length * lineHeight;

        float scale = 0.0035f * Math.max(0.2f, textScale);

        // Keep multi-line text nicely contained inside bounds
        float textWInWorld = maxLineWidth * scale;
        float textHInWorld = totalTextHeight * scale;
        if (textWInWorld > boxW * 0.90f) {
            scale *= (boxW * 0.90f) / textWInWorld;
        }
        if (textHInWorld > boxH * 0.90f) {
            scale *= (boxH * 0.90f) / textHInWorld;
        }

        poseStack.pushPose();
        poseStack.translate(center[0], center[1], center[2]);

        // Orient poseStack according to face and facing
        if (face == AttachFace.FLOOR) {
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0f));
            switch (facing) {
                case NORTH -> {}
                case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180.0f));
                case WEST  -> poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90.0f));
                case EAST  -> poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(270.0f));
                default    -> {}
            }
        } else if (face == AttachFace.CEILING) {
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0f));
            switch (facing) {
                case NORTH -> poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180.0f));
                case SOUTH -> {}
                case WEST  -> poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(270.0f));
                case EAST  -> poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90.0f));
                default    -> {}
            }
        } else { // WALL
            switch (facing) {
                case SOUTH -> {} // Normal +Z (facing south into room)
                case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0f));
                case WEST  -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0f));
                case EAST  -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0f));
                default    -> {}
            }
        }

        // Standard Minecraft 3D font scaling: -scale flips Y for CCW upright glyphs
        poseStack.scale(scale, -scale, scale);

        float startY = - (lines.length * lineHeight) * 0.5f;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            float drawX = - font.width(line) * 0.5f;
            float drawY = startY + i * lineHeight;

            font.drawInBatch(
                    line,
                    drawX,
                    drawY,
                    textColor | 0xFF000000,
                    false,
                    poseStack.last().pose(),
                    buffer,
                    Font.DisplayMode.POLYGON_OFFSET,
                    0,
                    0x00F000F0
            );
        }

        poseStack.popPose();
    }

    // ========================================================================
    // Geometric Helpers & Coordinate Mapping
    // ========================================================================

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
                case SOUTH -> { z = 1.0f + h; x = u; }
                case NORTH -> { z = 0.0f - h; x = 1.0f - u; }
                case WEST  -> { x = 0.0f - h; z = u; }
                case EAST  -> { x = 1.0f + h; z = 1.0f - u; }
                default    -> { z = 1.0f + h; x = u; }
            }
        }
        return new float[]{x, y, z};
    }

    private static void renderFacePoly4(Matrix4f pose, VertexConsumer vc, AttachFace face, Direction facing,
                                        float uA, float vA, float uB, float vB,
                                        float uC, float vC, float uD, float vD,
                                        float h, int r, int g, int b) {
        float[] p1 = mapCoord(face, facing, uA, vA, h);
        float[] p2 = mapCoord(face, facing, uB, vB, h);
        float[] p3 = mapCoord(face, facing, uC, vC, h);
        float[] p4 = mapCoord(face, facing, uD, vD, h);

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

    private static void renderFaceQuad(Matrix4f pose, VertexConsumer vc, AttachFace face, Direction facing,
                                       float u1, float v1, float u2, float v2, float h,
                                       int r, int g, int b) {
        renderFacePoly4(pose, vc, face, facing, u1, v1, u2, v1, u2, v2, u1, v2, h, r, g, b);
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
