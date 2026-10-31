package pl.asie.computronics.oc.environment;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.*;

public class EnvironmentRadarUpgrade extends AbstractManagedEnvironment {
    private final EnvironmentHost host;
    public static final int MAX_RADIUS = 32;

    public EnvironmentRadarUpgrade(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network)
                .withComponent("radar", Visibility.Neighbors)
                .withConnector(500)
                .create());
    }

    private Connector connector() {
        return (Connector) node();
    }

    @Callback(doc = "function([radius:number]):table -- Scans entities around robot/drone.")
    public Object[] getEntities(Context context, Arguments args) {
        int radius = Math.min(args.optInteger(0, 16), MAX_RADIUS);
        double cost = radius * 2.0;
        if (connector() != null && !connector().tryChangeBuffer(-cost)) {
            return new Object[]{null, "not enough energy"};
        }

        Level level = host.getEnvironmentLevel();
        if (level == null) return new Object[]{Collections.emptyList()};

        double cx = host.xPosition();
        double cy = host.yPosition();
        double cz = host.zPosition();
        AABB box = new AABB(cx - radius, cy - radius, cz - radius, cx + radius, cy + radius, cz + radius);

        List<Entity> list = level.getEntities((Entity) null, box, Entity::isAlive);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Entity e : list) {
            double dx = e.getX() - cx;
            double dy = e.getY() - cy;
            double dz = e.getZ() - cz;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist <= radius) {
                Map<String, Object> map = new HashMap<>();
                map.put("name", e.getName().getString());
                map.put("type", BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString());
                map.put("x", dx);
                map.put("y", dy);
                map.put("z", dz);
                map.put("distance", dist);
                result.add(map);
            }
        }
        return new Object[]{result};
    }

    @Callback(doc = "function([radius:number]):table -- Scans players around robot/drone.")
    public Object[] getPlayers(Context context, Arguments args) {
        int radius = Math.min(args.optInteger(0, 16), MAX_RADIUS);
        double cost = radius * 1.5;
        if (connector() != null && !connector().tryChangeBuffer(-cost)) {
            return new Object[]{null, "not enough energy"};
        }

        Level level = host.getEnvironmentLevel();
        if (level == null) return new Object[]{Collections.emptyList()};

        double cx = host.xPosition();
        double cy = host.yPosition();
        double cz = host.zPosition();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Player p : level.players()) {
            double dx = p.getX() - cx;
            double dy = p.getY() - cy;
            double dz = p.getZ() - cz;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist <= radius) {
                Map<String, Object> map = new HashMap<>();
                map.put("name", p.getName().getString());
                map.put("x", dx);
                map.put("y", dy);
                map.put("z", dz);
                map.put("distance", dist);
                result.add(map);
            }
        }
        return new Object[]{result};
    }
}
