package pl.asie.computronics.oc.environment;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public class EnvironmentCardFX extends AbstractManagedEnvironment {
    private final EnvironmentHost host;
    private static final Map<String, ParticleOptions> PARTICLES = new HashMap<>();

    static {
        PARTICLES.put("portal", ParticleTypes.PORTAL);
        PARTICLES.put("flame", ParticleTypes.FLAME);
        PARTICLES.put("smoke", ParticleTypes.SMOKE);
        PARTICLES.put("heart", ParticleTypes.HEART);
        PARTICLES.put("note", ParticleTypes.NOTE);
        PARTICLES.put("explosion", ParticleTypes.EXPLOSION);
        PARTICLES.put("crit", ParticleTypes.CRIT);
        PARTICLES.put("enchant", ParticleTypes.ENCHANT);
        PARTICLES.put("firework", ParticleTypes.FIREWORK);
        PARTICLES.put("sculk_charge", ParticleTypes.SCULK_CHARGE_POP);
        PARTICLES.put("electric_spark", ParticleTypes.ELECTRIC_SPARK);
        PARTICLES.put("sonic_boom", ParticleTypes.SONIC_BOOM);
        PARTICLES.put("witch", ParticleTypes.WITCH);
        PARTICLES.put("cloud", ParticleTypes.CLOUD);
    }

    public EnvironmentCardFX(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Neighbors)
                .withComponent("particle_fx", Visibility.Neighbors)
                .withConnector(250)
                .create());
    }

    @Callback(direct = true, doc = "function():table -- Returns all supported particle effect names.")
    public Object[] getAvailableParticles(Context context, Arguments args) {
        return new Object[]{PARTICLES.keySet().toArray(new String[0])};
    }

    @Callback(direct = true, limit = 16, doc = "function(name:string [, dx:number, dy:number, dz:number, count:number, speed:number]):boolean -- Spawns visual particle effect.")
    public Object[] spawn(Context context, Arguments args) {
        Level world = host != null ? host.getEnvironmentLevel() : null;
        if (!(world instanceof ServerLevel serverLevel)) {
            return new Object[]{false, "world unavailable"};
        }

        String name = args.checkString(0).toLowerCase();
        ParticleOptions particle = PARTICLES.get(name);
        if (particle == null) {
            return new Object[]{false, "unknown particle type: " + name};
        }

        double dx = args.optDouble(1, 0.0);
        double dy = args.optDouble(2, 0.5);
        double dz = args.optDouble(3, 0.0);
        int count = Math.max(1, Math.min(args.optInteger(4, 5), 100));
        double speed = Math.max(0.0, Math.min(args.optDouble(5, 0.1), 5.0));

        double x = host.xPosition() + dx;
        double y = host.yPosition() + dy;
        double z = host.zPosition() + dz;

        serverLevel.sendParticles(particle, x, y, z, count, 0.25, 0.25, 0.25, speed);
        return new Object[]{true};
    }
}
