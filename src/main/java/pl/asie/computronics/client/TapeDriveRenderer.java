package pl.asie.computronics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import pl.asie.computronics.block.entity.TileTapeDrive;

public class TapeDriveRenderer implements BlockEntityRenderer<TileTapeDrive> {
    public TapeDriveRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(TileTapeDrive drive, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ItemStack tape = drive.getTape();
        if (tape.isEmpty()) return;

        Direction facing = Direction.NORTH;
        if (drive.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = drive.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        }

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        // Rotate to match block facing
        switch (facing) {
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            case WEST  -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
            case EAST  -> poseStack.mulPose(Axis.YP.rotationDegrees(270));
            case NORTH -> {}
            default -> {}
        }

        // Animation: slides into the cassette slot over ~8 ticks
        long gameTime = drive.getLevel() != null ? drive.getLevel().getGameTime() : 0;
        long delta = gameTime - drive.getInsertTime();
        float t = Math.min(1.0f, Math.max(0.0f, (delta + partialTick) / 8.0f));
        // Smooth sine ease-out
        float anim = (float) Math.sin(t * (Math.PI * 0.5));

        // When inserting, begins slightly outside the drive and slides in
        float zPos = -0.52f + (0.14f * anim);

        // Position cassette inside front tape bay:
        poseStack.translate(0.0, -0.04, zPos);
        poseStack.scale(0.52f, 0.52f, 0.52f);
        // Face forward flat in the drive slot
        poseStack.mulPose(Axis.XP.rotationDegrees(90));

        ItemRenderer ir = Minecraft.getInstance().getItemRenderer();
        ir.renderStatic(tape, ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, buffer, drive.getLevel(), 0);

        poseStack.popPose();
    }
}
