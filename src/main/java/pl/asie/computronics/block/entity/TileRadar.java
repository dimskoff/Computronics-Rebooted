package pl.asie.computronics.block.entity;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Visibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import pl.asie.computronics.init.ModBlockEntities;

import java.util.*;

public class TileRadar extends TileEntityComputronics {
    public static final int MAX_RADIUS = 64;
    public static final double ENERGY_PER_BLOCK = 1.5;

    public TileRadar(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADAR.get(), pos, state);
        node = Network.newNode(this, Visibility.Network)
                .withComponent("radar", Visibility.Network)
                .withConnector(1000)
                .create();
    }

    private Connector connector() {
        return (Connector) node;
    }

    @Callback(doc = "function([radius:number]):table -- Returns a list of entities within range.")
    public Object[] getEntities(Context context, Arguments args) {
        int radius = Math.min(args.optInteger(0, 16), MAX_RADIUS);
        if (radius <= 0) {
            return result(Collections.emptyList());
        }

        double energyNeeded = radius * ENERGY_PER_BLOCK * 2;
        if (connector() != null && !connector().tryChangeBuffer(-energyNeeded)) {
            return result(null, "not enough energy");
        }

        if (level == null) return result(Collections.emptyList());

        double cx = worldPosition.getX() + 0.5;
        double cy = worldPosition.getY() + 0.5;
        double cz = worldPosition.getZ() + 0.5;
        AABB box = new AABB(cx - radius, cy - radius, cz - radius, cx + radius, cy + radius, cz + radius);

        List<Entity> entities = level.getEntities((Entity) null, box, e -> e.isAlive());
        List<Map<String, Object>> resultList = new ArrayList<>();

        for (Entity e : entities) {
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
                resultList.add(map);
            }
        }
        return result(resultList);
    }

    @Callback(doc = "function([radius:number]):table -- Returns a list of players within range.")
    public Object[] getPlayers(Context context, Arguments args) {
        int radius = Math.min(args.optInteger(0, 16), MAX_RADIUS);
        if (radius <= 0) {
            return result(Collections.emptyList());
        }

        double energyNeeded = radius * ENERGY_PER_BLOCK;
        if (connector() != null && !connector().tryChangeBuffer(-energyNeeded)) {
            return result(null, "not enough energy");
        }

        if (level == null) return result(Collections.emptyList());

        double cx = worldPosition.getX() + 0.5;
        double cy = worldPosition.getY() + 0.5;
        double cz = worldPosition.getZ() + 0.5;

        List<Map<String, Object>> resultList = new ArrayList<>();
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
                resultList.add(map);
            }
        }
        return result(resultList);
    }

    @Callback(direct = true, doc = "function():number -- Gets the maximum scanning radius.")
    public Object[] getMaxRadius(Context context, Arguments args) {
        return result(MAX_RADIUS);
    }
}
