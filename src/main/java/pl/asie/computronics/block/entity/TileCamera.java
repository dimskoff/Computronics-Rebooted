package pl.asie.computronics.block.entity;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import pl.asie.computronics.init.ModBlockEntities;

import java.util.HashMap;
import java.util.Map;

public class TileCamera extends TileEntityComputronics {
    public static final double MAX_DISTANCE = 64.0;

    public TileCamera(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CAMERA.get(), pos, state);
        node = Network.newNode(this, Visibility.Network)
                .withComponent("camera", Visibility.Network)
                .create();
    }

    public Direction getFacing() {
        if (getBlockState().hasProperty(BlockStateProperties.FACING)) {
            return getBlockState().getValue(BlockStateProperties.FACING);
        }
        return Direction.NORTH;
    }

    private Vec3 getDirectionVector(double relYaw, double relPitch) {
        Direction facing = getFacing();
        Vec3 forward = Vec3.atLowerCornerOf(facing.getNormal());
        Vec3 upRef = (facing == Direction.UP || facing == Direction.DOWN) ? new Vec3(0, 0, facing == Direction.UP ? -1 : 1) : new Vec3(0, 1, 0);
        Vec3 right = forward.cross(upRef).normalize();
        Vec3 up = right.cross(forward).normalize();

        double radYaw = Math.toRadians(relYaw);
        double radPitch = Math.toRadians(relPitch);

        Vec3 yawDir = forward.scale(Math.cos(radYaw)).add(right.scale(Math.sin(radYaw)));
        Vec3 finalDir = yawDir.scale(Math.cos(radPitch)).add(up.scale(Math.sin(radPitch)));
        return finalDir.normalize();
    }

    @Callback(direct = true, doc = "function():string -- Returns the physical direction the camera lens is facing.")
    public Object[] getFacing(Context context, Arguments args) {
        return result(getFacing().getName());
    }

    @Callback(doc = "function([yaw:number, pitch:number]):number -- Returns distance along lens orientation (defaults to 0,0 forward).")
    public Object[] distance(Context context, Arguments args) {
        double yaw = args.optDouble(0, 0.0);
        double pitch = args.optDouble(1, 0.0);

        if (level == null) return result(-1.0);

        Direction facing = getFacing();
        Vec3 normal = Vec3.atLowerCornerOf(facing.getNormal());
        Vec3 start = new Vec3(
                worldPosition.getX() + 0.5 + normal.x * 0.45,
                worldPosition.getY() + 0.5 + normal.y * 0.45,
                worldPosition.getZ() + 0.5 + normal.z * 0.45
        );
        Vec3 dir = getDirectionVector(yaw, pitch);
        Vec3 end = start.add(dir.scale(MAX_DISTANCE));

        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (net.minecraft.world.entity.Entity) null));
        if (hit.getType() == HitResult.Type.BLOCK) {
            double dist = start.distanceTo(hit.getLocation());
            return result(dist);
        }
        return result(-1.0);
    }

    @Callback(doc = "function([yaw:number, pitch:number]):table -- Returns block analysis along lens orientation.")
    public Object[] scan(Context context, Arguments args) {
        double yaw = args.optDouble(0, 0.0);
        double pitch = args.optDouble(1, 0.0);

        if (level == null) return result(null, "world unavailable");

        Direction facing = getFacing();
        Vec3 normal = Vec3.atLowerCornerOf(facing.getNormal());
        Vec3 start = new Vec3(
                worldPosition.getX() + 0.5 + normal.x * 0.45,
                worldPosition.getY() + 0.5 + normal.y * 0.45,
                worldPosition.getZ() + 0.5 + normal.z * 0.45
        );
        Vec3 dir = getDirectionVector(yaw, pitch);
        Vec3 end = start.add(dir.scale(MAX_DISTANCE));

        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (net.minecraft.world.entity.Entity) null));
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos targetPos = hit.getBlockPos();
            BlockState targetState = level.getBlockState(targetPos);
            double dist = start.distanceTo(hit.getLocation());

            Map<String, Object> data = new HashMap<>();
            data.put("distance", dist);
            data.put("block", BuiltInRegistries.BLOCK.getKey(targetState.getBlock()).toString());
            data.put("hardness", (double) targetState.getDestroySpeed(level, targetPos));
            data.put("light", level.getMaxLocalRawBrightness(targetPos.relative(hit.getDirection())));
            return result(data);
        }
        return result(null, "no block hit");
    }
}
