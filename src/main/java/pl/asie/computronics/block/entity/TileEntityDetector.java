package pl.asie.computronics.block.entity;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import pl.asie.computronics.init.ModBlockEntities;

import java.util.*;

public class TileEntityDetector extends TileEntityComputronics {
    public static final int MAX_RANGE = 32;
    private int scanRange = 16;
    private boolean signalEnabled = false;
    private final Set<UUID> knownEntities = new HashSet<>();

    public TileEntityDetector(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENTITY_DETECTOR.get(), pos, state);
        node = Network.newNode(this, Visibility.Network)
                .withComponent("entity_detector", Visibility.Network)
                .create();
    }

    public Direction getFacing() {
        if (getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return Direction.NORTH;
    }

    private AABB getScanBox(int range) {
        Direction facing = getFacing();
        double startX = worldPosition.getX() + 0.5;
        double startY = worldPosition.getY() + 0.5;
        double startZ = worldPosition.getZ() + 0.5;

        double endX = startX + facing.getStepX() * range;
        double endY = startY + facing.getStepY() * range;
        double endZ = startZ + facing.getStepZ() * range;

        double minX = Math.min(startX, endX) - (facing.getStepX() == 0 ? range * 0.5 : 0.5);
        double maxX = Math.max(startX, endX) + (facing.getStepX() == 0 ? range * 0.5 : 0.5);
        double minY = Math.min(startY, endY) - 2.0;
        double maxY = Math.max(startY, endY) + 3.0;
        double minZ = Math.min(startZ, endZ) - (facing.getStepZ() == 0 ? range * 0.5 : 0.5);
        double maxZ = Math.max(startZ, endZ) + (facing.getStepZ() == 0 ? range * 0.5 : 0.5);

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public void tickServer(Level level, BlockPos pos, BlockState state) {
        super.tickServer(level, pos, state);

        if (!signalEnabled || node == null || level.getGameTime() % 10 != 0) {
            return;
        }

        List<Entity> list = level.getEntities((Entity) null, getScanBox(scanRange));
        Set<UUID> current = new HashSet<>();
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;

        for (Entity e : list) {
            if (e.isRemoved()) continue;
            UUID uuid = e.getUUID();
            current.add(uuid);
            if (!knownEntities.contains(uuid)) {
                double dist = Math.sqrt(e.distanceToSqr(cx, cy, cz));
                node.sendToReachable("computer.signal", "entity_detected",
                        e.getName().getString(),
                        e.getStringUUID(),
                        dist,
                        e instanceof Player
                );
            }
        }
        knownEntities.clear();
        knownEntities.addAll(current);
    }

    @Callback(direct = true, doc = "function():string -- Gets the direction this detector is scanning.")
    public Object[] getFacing(Context context, Arguments args) {
        return new Object[]{getFacing().getName()};
    }

    @Callback(direct = true, doc = "function():number -- Gets default scanning range.")
    public Object[] getRange(Context context, Arguments args) {
        return new Object[]{scanRange};
    }

    @Callback(doc = "function(range:number):number -- Sets default scanning range (1-32).")
    public Object[] setRange(Context context, Arguments args) {
        this.scanRange = Math.max(1, Math.min(args.checkInteger(0), MAX_RANGE));
        setChanged();
        return new Object[]{scanRange};
    }

    @Callback(direct = true, doc = "function():boolean -- Checks if automatic signal broadcast on detection is enabled.")
    public Object[] isSignalEnabled(Context context, Arguments args) {
        return new Object[]{signalEnabled};
    }

    @Callback(doc = "function(enabled:boolean):boolean -- Enables or disables automatic signal broadcast.")
    public Object[] setSignalEnabled(Context context, Arguments args) {
        this.signalEnabled = args.checkBoolean(0);
        if (!signalEnabled) {
            knownEntities.clear();
        }
        setChanged();
        return new Object[]{signalEnabled};
    }

    @Callback(direct = true, limit = 16, doc = "function([range:number]):number -- Gets total number of entities in front of the detector.")
    public Object[] count(Context context, Arguments args) {
        if (level == null) return new Object[]{0};
        int range = Math.max(1, Math.min(args.optInteger(0, scanRange), MAX_RANGE));
        List<Entity> list = level.getEntities((Entity) null, getScanBox(range));
        return new Object[]{list.size()};
    }

    @Callback(direct = true, limit = 16, doc = "function([range:number]):number -- Gets number of players in detection cone.")
    public Object[] getPlayerCount(Context context, Arguments args) {
        if (level == null) return new Object[]{0};
        int range = Math.max(1, Math.min(args.optInteger(0, scanRange), MAX_RANGE));
        List<Player> list = level.getEntitiesOfClass(Player.class, getScanBox(range));
        return new Object[]{list.size()};
    }

    @Callback(direct = true, limit = 16, doc = "function([range:number]):boolean -- Checks if any player is in range.")
    public Object[] isPlayerInRange(Context context, Arguments args) {
        if (level == null) return new Object[]{false};
        int range = Math.max(1, Math.min(args.optInteger(0, scanRange), MAX_RANGE));
        List<Player> list = level.getEntitiesOfClass(Player.class, getScanBox(range));
        return new Object[]{!list.isEmpty()};
    }

    @Callback(doc = "function([range:number]):table -- Scans all entities in front, returning detailed status and metrics.")
    public Object[] scan(Context context, Arguments args) {
        if (level == null) return new Object[]{null, "world unavailable"};

        int range = Math.max(1, Math.min(args.optInteger(0, scanRange), MAX_RANGE));
        List<Entity> entities = level.getEntities((Entity) null, getScanBox(range));

        List<Map<String, Object>> result = new ArrayList<>();
        double cx = worldPosition.getX() + 0.5;
        double cy = worldPosition.getY() + 0.5;
        double cz = worldPosition.getZ() + 0.5;

        for (Entity e : entities) {
            if (e.isRemoved()) continue;
            Map<String, Object> data = new HashMap<>();
            data.put("id", e.getId());
            data.put("uuid", e.getStringUUID());
            data.put("name", e.getName().getString());
            data.put("type", BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString());
            data.put("x", e.getX() - cx);
            data.put("y", e.getY() - cy);
            data.put("z", e.getZ() - cz);
            data.put("distance", Math.sqrt(e.distanceToSqr(cx, cy, cz)));
            data.put("isPlayer", e instanceof Player);

            if (e instanceof LivingEntity living) {
                data.put("health", (double) living.getHealth());
                data.put("maxHealth", (double) living.getMaxHealth());
                data.put("armor", living.getArmorValue());
                if (!living.getMainHandItem().isEmpty()) {
                    data.put("mainHand", BuiltInRegistries.ITEM.getKey(living.getMainHandItem().getItem()).toString());
                }
            }

            result.add(data);
        }

        return new Object[]{result};
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("range", scanRange);
        tag.putBoolean("signalEnabled", signalEnabled);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("range")) {
            this.scanRange = tag.getInt("range");
        }
        if (tag.contains("signalEnabled")) {
            this.signalEnabled = tag.getBoolean("signalEnabled");
        }
    }
}
