package pl.asie.computronics.oc.environment;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public class EnvironmentCameraUpgrade extends AbstractManagedEnvironment {
    private final EnvironmentHost host;
    public static final double MAX_DISTANCE = 32.0;

    public EnvironmentCameraUpgrade(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network)
                .withComponent("camera", Visibility.Neighbors)
                .create());
    }

    private Vec3 getDirectionVector(double yaw, double pitch) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double x = -Math.sin(yawRad) * Math.cos(pitchRad);
        double y = -Math.sin(pitchRad);
        double z = Math.cos(yawRad) * Math.cos(pitchRad);
        return new Vec3(x, y, z).normalize();
    }

    @Callback(doc = "function(yaw:number, pitch:number):number -- Returns distance to block from robot/drone.")
    public Object[] distance(Context context, Arguments args) {
        double yaw = args.checkDouble(0);
        double pitch = args.checkDouble(1);

        Level level = host.getEnvironmentLevel();
        if (level == null) return new Object[]{-1.0};

        Vec3 start = new Vec3(host.xPosition(), host.yPosition(), host.zPosition());
        Vec3 dir = getDirectionVector(yaw, pitch);
        Vec3 end = start.add(dir.scale(MAX_DISTANCE));

        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (net.minecraft.world.entity.Entity) null));
        if (hit.getType() == HitResult.Type.BLOCK) {
            return new Object[]{start.distanceTo(hit.getLocation())};
        }
        return new Object[]{-1.0};
    }

    @Callback(doc = "function(yaw:number, pitch:number):table -- Scans block properties.")
    public Object[] scan(Context context, Arguments args) {
        double yaw = args.checkDouble(0);
        double pitch = args.checkDouble(1);

        Level level = host.getEnvironmentLevel();
        if (level == null) return new Object[]{null, "world unavailable"};

        Vec3 start = new Vec3(host.xPosition(), host.yPosition(), host.zPosition());
        Vec3 dir = getDirectionVector(yaw, pitch);
        Vec3 end = start.add(dir.scale(MAX_DISTANCE));

        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (net.minecraft.world.entity.Entity) null));
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos targetPos = hit.getBlockPos();
            BlockState targetState = level.getBlockState(targetPos);
            Map<String, Object> data = new HashMap<>();
            data.put("distance", start.distanceTo(hit.getLocation()));
            data.put("block", BuiltInRegistries.BLOCK.getKey(targetState.getBlock()).toString());
            data.put("hardness", (double) targetState.getDestroySpeed(level, targetPos));
            return new Object[]{data};
        }
        return new Object[]{null, "no block hit"};
    }
}
