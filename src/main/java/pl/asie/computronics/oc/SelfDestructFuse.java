package pl.asie.computronics.oc;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

/**
 * Shared fuse logic of the self-destruct card and the rack detonator board
 * (original Computronics API: start([seconds=5]), time()).
 */
public final class SelfDestructFuse {
    private static final String TAG_FUSE = "fuseTicks";

    private final EnvironmentHost host;
    private int fuseTicks = -1;

    public SelfDestructFuse(EnvironmentHost host) {
        this.host = host;
    }

    public boolean isRunning() {
        return fuseTicks >= 0;
    }

    /** @return {seconds} on success, {-1, error} if the fuse already burns. */
    public Object[] start(double seconds, int minTicks) {
        if (isRunning()) return new Object[]{-1, "fuse has already been set"};
        if (!Double.isFinite(seconds)) seconds = 5;
        int ticks = Math.max(Math.max(1, minTicks), (int) Math.round(Math.max(0, Math.min(seconds, 3600)) * 20.0));
        fuseTicks = ticks;
        host.markChanged();
        return new Object[]{ticks / 20.0};
    }

    public Object[] time() {
        if (!isRunning()) return new Object[]{-1, "fuse has not been set"};
        return new Object[]{fuseTicks / 20.0};
    }

    public void reset() {
        fuseTicks = -1;
    }

    /** Ticks the fuse; returns true exactly once when the fuse runs out. */
    public boolean tick() {
        if (fuseTicks < 0) return false;
        if (fuseTicks > 0) {
            fuseTicks--;
            return false;
        }
        fuseTicks = -1;
        return true;
    }

    /** Resets the fuse when the owning computer starts or stops. */
    public void onMessage(Node own, Message message) {
        if (own == null) return;
        String name = message.name();
        if (("computer.started".equals(name) || "computer.stopped".equals(name)) && own.isNeighborOf(message.source())) {
            reset();
        }
    }

    public void explode(float power, boolean blockDamage) {
        Level level = host.getEnvironmentLevel();
        if (level == null || level.isClientSide()) return;
        Level.ExplosionInteraction interaction = blockDamage ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
        level.explode(null, host.xPosition(), host.yPosition(), host.zPosition(), power, interaction);
    }

    public void load(CompoundTag tag) {
        fuseTicks = tag.contains(TAG_FUSE) ? tag.getInt(TAG_FUSE) : -1;
    }

    public void save(CompoundTag tag) {
        tag.putInt(TAG_FUSE, fuseTicks);
    }

    public int ticksLeft() {
        return fuseTicks;
    }
}
