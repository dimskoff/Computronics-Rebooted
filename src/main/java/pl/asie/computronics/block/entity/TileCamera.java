package pl.asie.computronics.block.entity;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
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

    private Vec3 getDirectionVector(double yaw, double pitch) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double x = -Math.sin(yawRad) * Math.cos(pitchRad);
        double y = -Math.sin(pitchRad);
        double z = Math.cos(yawRad) * Math.cos(pitchRad);
        return new Vec3(x, y, z).normalize();
    }

    @Callback(doc = "function(yaw:number, pitch:number):number -- Returns distance to the block in the given direction.")
    public Object[] distance(Context context, Arguments args) {
        double yaw = args.checkDouble(0);
        double pitch = args.checkDouble(1);

        if (level == null) return result(-1.0);

        Vec3 start = new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
        Vec3 dir = getDirectionVector(yaw, pitch);
        Vec3 end = start.add(dir.scale(MAX_DISTANCE));

        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (net.minecraft.world.entity.Entity) null));
        if (hit.getType() == HitResult.Type.BLOCK) {
            double dist = start.distanceTo(hit.getLocation());
            return result(dist);
        }
        return result(-1.0);
    }

    @Callback(doc = "function(yaw:number, pitch:number):table -- Returns block analysis along the given direction.")
    public Object[] scan(Context context, Arguments args) {
        double yaw = args.checkDouble(0);
        double pitch = args.checkDouble(1);

        if (level == null) return result(null, "world unavailable");

        Vec3 start = new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
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
