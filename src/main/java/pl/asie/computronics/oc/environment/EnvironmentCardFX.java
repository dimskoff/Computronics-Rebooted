package pl.asie.computronics.oc.environment;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import pl.asie.computronics.oc.OCData;

/** Particle card, component "particle" (original Computronics API). */
public class EnvironmentCardFX extends AbstractManagedEnvironment {
    /** Max distance from the host in blocks (hardcoded; original used a larger value). */
    public static final double MAX_RANGE = 16.0;
    /** Energy per spawned particle (hardcoded). */
    public static final double ENERGY_PER_PARTICLE = 0.5;

    private final EnvironmentHost host;

    public EnvironmentCardFX(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Neighbors)
                .withComponent("particle", Visibility.Neighbors)
                .withConnector()
                .create());
    }

    @Callback(doc = "function(name:string, x:number, y:number, z:number[, velocity:number]):boolean or "
            + "function(name:string, x:number, y:number, z:number, vx:number, vy:number, vz:number):boolean "
            + "-- Spawns a particle relative to the card's host.")
    public Object[] spawn(Context context, Arguments args) {
        String name = args.checkString(0);
        double dx = args.checkDouble(1), dy = args.checkDouble(2), dz = args.checkDouble(3);
        if (!Double.isFinite(dx) || !Double.isFinite(dy) || !Double.isFinite(dz)
                || dx * dx + dy * dy + dz * dz > MAX_RANGE * MAX_RANGE) {
            return new Object[]{false, "location out of bounds"};
        }
        ResourceLocation id = ResourceLocation.tryParse(name);
        ParticleType<?> type = id == null ? null : BuiltInRegistries.PARTICLE_TYPE.get(id);
        if (!(type instanceof SimpleParticleType simple)) {
            return new Object[]{false, "invalid particle type"};
        }
        if (!(host.getEnvironmentLevel() instanceof ServerLevel level)) {
            return new Object[]{false, "world unavailable"};
        }
        if (!OCData.tryConsume(node(), ENERGY_PER_PARTICLE)) {
            return new Object[]{false, "not enough energy"};
        }
        double x = host.xPosition() + dx, y = host.yPosition() + dy, z = host.zPosition() + dz;
        if (args.count() >= 7) {
            double vx = clampV(args.checkDouble(4)), vy = clampV(args.checkDouble(5)), vz = clampV(args.checkDouble(6));
            // count 0: offsets are used as the exact velocity
            level.sendParticles(simple, x, y, z, 0, vx, vy, vz, 1.0);
        } else {
            double v = clampV(args.optDouble(4, 0.0));
            level.sendParticles(simple, x, y, z, 1, 0, 0, 0, Math.abs(v));
        }
        return new Object[]{true};
    }

    private static double clampV(double v) {
        if (!Double.isFinite(v)) return 0;
        return Math.max(-5.0, Math.min(5.0, v));
    }
}
