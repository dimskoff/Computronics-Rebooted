package pl.asie.computronics.oc.environment;

import li.cil.oc.api.Network;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import pl.asie.computronics.oc.OCData;

import java.util.Comparator;
import java.util.UUID;

/**
 * Drone winch upgrade, component "winch". A rope of variable length (0..16 blocks) hangs below the drone;
 * a grabbed entity is pulled toward the hook every tick while the rope is taut.
 */
public class EnvironmentDroneWinch extends AbstractManagedEnvironment {
    public static final double MAX_LENGTH = 16.0;
    public static final double GRAB_RADIUS = 2.0;
    /** Hardcoded energy per tick while holding an entity. */
    public static final double ENERGY_PER_TICK = 0.05;

    private final EnvironmentHost host;
    private double length = 0;
    private UUID heldId = null;

    public EnvironmentDroneWinch(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network)
                .withComponent("winch", Visibility.Neighbors)
                .withConnector()
                .create());
    }

    private Vec3 hook() {
        return new Vec3(host.xPosition(), host.yPosition() - length, host.zPosition());
    }

    private Entity held() {
        if (heldId == null || !(host.getEnvironmentLevel() instanceof ServerLevel level)) return null;
        Entity e = level.getEntity(heldId);
        return e != null && e.isAlive() ? e : null;
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void update() {
        super.update();
        if (heldId == null) return;
        Entity e = held();
        if (e == null) {
            heldId = null;
            return;
        }
        Vec3 drone = new Vec3(host.xPosition(), host.yPosition(), host.zPosition());
        double dist = e.position().distanceTo(drone);
        if (dist > MAX_LENGTH + 8 || !OCData.tryConsume(node(), ENERGY_PER_TICK)) {
            heldId = null; // rope snapped / no power
            return;
        }
        Vec3 toHook = hook().subtract(e.position());
        double slack = toHook.length();
        if (slack > 0.5) {
            Vec3 pull = toHook.normalize().scale(Math.min(0.4, slack * 0.15));
            e.setDeltaMovement(e.getDeltaMovement().scale(0.6).add(pull));
            e.fallDistance = 0;
            e.hurtMarked = true;
        } else {
            e.setDeltaMovement(e.getDeltaMovement().scale(0.5));
            e.fallDistance = 0;
            e.hurtMarked = true;
        }
    }

    @Callback(doc = "function(length:number):number -- Unwinds the rope by the given amount of blocks; returns the new length.")
    public Object[] deploy(Context context, Arguments args) {
        length = Math.max(0, Math.min(MAX_LENGTH, length + Math.max(0, args.checkDouble(0))));
        return new Object[]{length};
    }

    @Callback(doc = "function(length:number):number -- Winds the rope up by the given amount of blocks; returns the new length.")
    public Object[] retract(Context context, Arguments args) {
        length = Math.max(0, Math.min(MAX_LENGTH, length - Math.max(0, args.checkDouble(0))));
        return new Object[]{length};
    }

    @Callback(direct = true, doc = "function():number -- Returns the current rope length.")
    public Object[] getLength(Context context, Arguments args) {
        return new Object[]{length};
    }

    @Callback(doc = "function():boolean -- Attaches the hook to the nearest entity within 2 blocks of it.")
    public Object[] grab(Context context, Arguments args) {
        if (heldId != null && held() != null) return new Object[]{false, "already holding an entity"};
        if (!(host.getEnvironmentLevel() instanceof ServerLevel level)) return new Object[]{false, "world unavailable"};
        Vec3 h = hook();
        AABB box = new AABB(h.x - GRAB_RADIUS, h.y - GRAB_RADIUS, h.z - GRAB_RADIUS, h.x + GRAB_RADIUS, h.y + GRAB_RADIUS, h.z + GRAB_RADIUS);
        Entity target = level.getEntities((Entity) null, box,
                        e -> e.isAlive() && (e instanceof LivingEntity || e instanceof ItemEntity)
                                && !(e instanceof Player p && (p.isSpectator() || p.isCreative()))
                                && !(host instanceof Entity self && (e == self || e.getUUID().equals(self.getUUID()))))
                .stream()
                .min(Comparator.comparingDouble(e -> e.position().distanceToSqr(h)))
                .orElse(null);
        if (target == null) return new Object[]{false, "nothing to grab"};
        heldId = target.getUUID();
        return new Object[]{true};
    }

    @Callback(doc = "function():boolean -- Releases the held entity.")
    public Object[] release(Context context, Arguments args) {
        boolean released = heldId != null;
        heldId = null;
        return new Object[]{released};
    }

    @Callback(direct = true, doc = "function():boolean -- Whether an entity is attached to the hook.")
    public Object[] isHolding(Context context, Arguments args) {
        return new Object[]{heldId != null};
    }

    @Override
    public void loadData(DataComponentHolder holder) throws UnrecoverablePersistanceException {
        super.loadData(holder);
        CompoundTag tag = OCData.read(holder);
        length = Math.max(0, Math.min(MAX_LENGTH, tag.getDouble("winchLength")));
        heldId = tag.hasUUID("winchHeld") ? tag.getUUID("winchHeld") : null;
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        super.saveData(holder);
        OCData.write(holder, tag -> {
            tag.putDouble("winchLength", length);
            if (heldId != null) tag.putUUID("winchHeld", heldId);
            else tag.remove("winchHeld");
        });
    }
}
