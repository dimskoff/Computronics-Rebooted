package pl.asie.computronics.service;

import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import pl.asie.computronics.config.ComputronicsServerConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared radar logic (block + upgrade). Must be called on the server thread (non-direct callbacks).
 */
public final class RadarService {
    public enum Filter { ALL, PLAYERS, MOBS }

    private RadarService() {}

    /** Per-device transient state (cooldown). */
    public static final class RadarState {
        private long lastScanTick = Long.MIN_VALUE;
    }

    public static int maxRadius() {
        try {
            return ComputronicsServerConfig.SERVER.radarMaxRadius.get();
        } catch (Exception e) {
            return 32;
        }
    }

    private static boolean detectPlayers() {
        try {
            return ComputronicsServerConfig.SERVER.radarDetectPlayers.get();
        } catch (Exception e) {
            return true;
        }
    }

    private static boolean detectMobs() {
        try {
            return ComputronicsServerConfig.SERVER.radarDetectMobs.get();
        } catch (Exception e) {
            return true;
        }
    }

    private static int cooldownTicks() {
        try {
            return ComputronicsServerConfig.SERVER.radarScanCooldownTicks.get();
        } catch (Exception e) {
            return 20;
        }
    }

    private static double costPerScan() {
        try {
            return ComputronicsServerConfig.SERVER.radarEnergyCostPerScan.get();
        } catch (Exception e) {
            return 15.0;
        }
    }

    public static Object[] scan(RadarState state, Node node, Level level, Vec3 center, int requestedRadius, Filter filter) {
        if (level == null || level.isClientSide || center == null) {
            return new Object[]{null, "world unavailable"};
        }
        int maxR = maxRadius();
        int radius = Math.max(1, Math.min(requestedRadius, maxR));

        long now = level.getGameTime();
        int cd = cooldownTicks();
        if (cd > 0 && state.lastScanTick != Long.MIN_VALUE && now >= state.lastScanTick && now - state.lastScanTick < cd) {
            return new Object[]{null, "cooldown"};
        }

        double cost = costPerScan() * radius / (double) maxR;
        if (cost > 0) {
            if (!(node instanceof Connector connector) || connector.network() == null) {
                return new Object[]{null, "no power"};
            }
            if (!connector.tryChangeBuffer(-cost)) {
                return new Object[]{null, "not enough energy"};
            }
        }
        state.lastScanTick = now;

        boolean wantPlayers = detectPlayers() && filter != Filter.MOBS;
        boolean wantMobs = detectMobs() && filter != Filter.PLAYERS;
        List<Map<String, Object>> out = new ArrayList<>();
        if (!wantPlayers && !wantMobs) {
            return new Object[]{out};
        }

        double r2 = (double) radius * radius;
        AABB box = new AABB(center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box, Entity::isAlive);
        for (LivingEntity e : entities) {
            boolean isPlayer = e instanceof Player;
            if (isPlayer ? !wantPlayers : !wantMobs) continue;
            if (isPlayer && e.isSpectator()) continue;
            double dx = e.getX() - center.x;
            double dy = e.getY() - center.y;
            double dz = e.getZ() - center.z;
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 > r2 || d2 < 1.0E-6) continue; // skip the host itself (e.g. a drone)
            Map<String, Object> map = new HashMap<>();
            map.put("name", e.getName().getString());
            map.put("type", BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString());
            map.put("isPlayer", isPlayer);
            map.put("x", dx);
            map.put("y", dy);
            map.put("z", dz);
            map.put("distance", Math.sqrt(d2));
            out.add(map);
        }
        return new Object[]{out};
    }
}
