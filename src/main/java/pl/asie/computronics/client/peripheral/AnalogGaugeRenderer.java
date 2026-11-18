package pl.asie.computronics.client.peripheral;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Matrix4f;
import pl.asie.computronics.block.entity.TileAnalogGauge;

/**
 * Draws a needle (and the optional label) on the front face of the analog gauge.
 * Front face = the block's HORIZONTAL_FACING side.
 */
public class AnalogGaugeRenderer implements BlockEntityRenderer<TileAnalogGauge> {
    private static final float NEEDLE_LENGTH = 0.36f;
    private static final float NEEDLE_HALF_WIDTH = 0.012f;
    private static final float PIVOT_Y = -0.22f;
    private static final float FACE_OFFSET = 0.502f;

    private final Font font;

    public AnalogGaugeRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(TileAnalogGauge gauge, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Direction facing = Direction.NORTH;
        if (gauge.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = gauge.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        }

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        // After this rotation the front face lies at local z = +0.5, x = viewer's right, y = up.
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));

        int color = gauge.getGaugeColor();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        // Value 0 -> needle pointing left, value 1 -> pointing right (180 degree sweep).
        double angle = Math.PI * (1.0 - gauge.getGaugeValue());
        float dx = (float) Math.cos(angle);
        float dy = (float) Math.sin(angle);
        // perpendicular for needle width
        float px = -dy * NEEDLE_HALF_WIDTH;
        float py = dx * NEEDLE_HALF_WIDTH;

        VertexConsumer vc = buffer.getBuffer(RenderType.debugQuads());
        Matrix4f m = poseStack.last().pose();
        float z = FACE_OFFSET;
        float tipX = dx * NEEDLE_LENGTH;
        float tipY = PIVOT_Y + dy * NEEDLE_LENGTH;

        // Needle
        vc.addVertex(m, px, PIVOT_Y + py, z).setColor(r, g, b, 1.0f);
        vc.addVertex(m, -px, PIVOT_Y - py, z).setColor(r, g, b, 1.0f);
        vc.addVertex(m, tipX - px, tipY - py, z).setColor(r, g, b, 1.0f);
        vc.addVertex(m, tipX + px, tipY + py, z).setColor(r, g, b, 1.0f);

        // Pivot cap
        float c = 0.03f;
        float zc = z + 0.001f;
        vc.addVertex(m, -c, PIVOT_Y - c, zc).setColor(0.15f, 0.15f, 0.15f, 1.0f);
        vc.addVertex(m, c, PIVOT_Y - c, zc).setColor(0.15f, 0.15f, 0.15f, 1.0f);
        vc.addVertex(m, c, PIVOT_Y + c, zc).setColor(0.15f, 0.15f, 0.15f, 1.0f);
        vc.addVertex(m, -c, PIVOT_Y + c, zc).setColor(0.15f, 0.15f, 0.15f, 1.0f);

        String label = gauge.getGaugeLabel();
        if (label != null && !label.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0.0, -0.32, FACE_OFFSET + 0.002);
            float s = 1.0f / 128.0f;
            poseStack.scale(s, -s, s);
            float w = font.width(label);
            font.drawInBatch(label, -w / 2.0f, 0.0f, 0xFFFFFF, false, poseStack.last().pose(), buffer,
                    Font.DisplayMode.NORMAL, 0, packedLight);
            poseStack.popPose();
        }

        poseStack.popPose();
    }
}
