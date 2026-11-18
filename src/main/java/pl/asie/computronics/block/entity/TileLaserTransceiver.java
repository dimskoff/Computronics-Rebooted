package pl.asie.computronics.block.entity;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import pl.asie.computronics.config.ComputronicsServerConfig;
import pl.asie.computronics.init.ModBlockEntities;
import pl.asie.computronics.network.RFJammerManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class TileLaserTransceiver extends TileEntityComputronics {
    public static final int MAX_RANGE = 256;
    private static final int SCAN_INTERVAL = 20;

    private volatile TileLaserTransceiver linkedTransceiver = null;
    private volatile int distanceToLinked = -1;
    private volatile boolean beamBroken = false;

    // Loop protection: packets already seen (forwarded or received via the beam) during the current tick.
    private final Set<Integer> seenThisTick = new HashSet<>();
    private long seenTick = Long.MIN_VALUE;

    public TileLaserTransceiver(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LASER_TRANSCEIVER.get(), pos, state);
        node = Network.newNode(this, Visibility.Network)
                .withComponent("laser_transceiver", Visibility.Network)
                .create();
    }

    public Direction getFacing() {
        if (getBlockState().hasProperty(BlockStateProperties.FACING)) {
            return getBlockState().getValue(BlockStateProperties.FACING);
        }
        return Direction.NORTH;
    }

    private static int maxDistance() {
        try {
            return Math.min(MAX_RANGE, ComputronicsServerConfig.SERVER.laserMaxDistance.get());
        } catch (Exception e) {
            return 64;
        }
    }

    @Override
    public void tickServer(Level level, BlockPos pos, BlockState state) {
        super.tickServer(level, pos, state);
        // stagger scans between transceivers
        if ((level.getGameTime() + (pos.asLong() & 0xF)) % SCAN_INTERVAL == 0) {
            updateLaserLink(level, pos);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        linkedTransceiver = null;
    }

    private void updateLaserLink(Level level, BlockPos pos) {
        Direction dir = getFacing();
        int range = maxDistance();

        // Find the first obstructing block (only in loaded chunks; never load chunks).
        TileLaserTransceiver found = null;
        int blockDist = -1;
        int lastClear = 0;
        for (int i = 1; i <= range; i++) {
            BlockPos current = pos.relative(dir, i);
            if (!level.isLoaded(current)) {
                break;
            }
            BlockState st = level.getBlockState(current);
            if (!st.isAir()) {
                if (level.getBlockEntity(current) instanceof TileLaserTransceiver other
                        && !other.isRemoved() && other.getFacing() == dir.getOpposite()) {
                    found = other;
                    blockDist = i;
                }
                break;
            }
            lastClear = i;
        }

        // Optical tripwire: one entity query over the clear beam segment.
        boolean broken = false;
        if (lastClear > 0) {
            BlockPos end = pos.relative(dir, lastClear);
            AABB beam = new AABB(pos.relative(dir, 1)).minmax(new AABB(end));
            List<Entity> entities = level.getEntities((Entity) null, beam);
            if (!entities.isEmpty()) {
                broken = true;
                Entity nearest = null;
                double best = Double.MAX_VALUE;
                for (Entity e : entities) {
                    double d = e.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    if (d < best) {
                        best = d;
                        nearest = e;
                    }
                }
                if (!beamBroken && node != null && nearest != null) {
                    node.sendToReachable("computer.signal", "tripwire_triggered", nearest.getName().getString(), Math.sqrt(best));
                }
            }
        }

        this.linkedTransceiver = found;
        this.distanceToLinked = blockDist;
        this.beamBroken = broken;
    }

    private boolean markSeen(int key) {
        long now = level != null ? level.getGameTime() : 0;
        if (now != seenTick) {
            seenTick = now;
            seenThisTick.clear();
        }
        return seenThisTick.add(key);
    }

    private static int packetKey(Object[] data) {
        if (data != null && data.length > 0 && data[0] instanceof Packet p) {
            return Objects.hash(p.source(), p.destination(), p.port(), Arrays.deepHashCode(p.data()));
        }
        return Arrays.deepHashCode(data);
    }

    private boolean isLinkUsable(TileLaserTransceiver other) {
        if (other == null || other.isRemoved() || beamBroken || level == null) return false;
        if (RFJammerManager.INSTANCE.isJammed(level, worldPosition)) return false;
        return other.level == null || !RFJammerManager.INSTANCE.isJammed(other.level, other.worldPosition);
    }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        if (!"network.message".equals(message.name())) return;
        TileLaserTransceiver other = linkedTransceiver;
        if (!isLinkUsable(other)) return;
        Node otherNode = other.node();
        if (otherNode == null || message.source() == otherNode) return;

        Object[] data = message.data();
        int key = packetKey(data);
        if (!markSeen(key)) return; // already passed through this transceiver this tick

        Object[] forward = data;
        if (data != null && data.length > 0 && data[0] instanceof Packet packet) {
            if (packet.ttl() <= 0) return;
            Object[] copy = data.clone();
            copy[0] = packet.hop();
            forward = copy;
        }
        // Prevent the packet from bouncing straight back through the far side.
        other.markSeen(key);
        otherNode.sendToReachable("network.message", forward);
    }

    @Callback(direct = true, doc = "function():boolean -- Checks if an active laser connection exists.")
    public Object[] isConnected(Context context, Arguments args) {
        TileLaserTransceiver other = linkedTransceiver;
        return result(other != null && !other.isRemoved() && !beamBroken);
    }

    @Callback(direct = true, doc = "function():number -- Gets distance to connected transceiver.")
    public Object[] getDistance(Context context, Arguments args) {
        return result(distanceToLinked);
    }

    @Callback(direct = true, doc = "function():boolean -- Checks if the optical laser beam is currently broken.")
    public Object[] isBeamBroken(Context context, Arguments args) {
        return result(beamBroken);
    }

    @Callback(doc = "function():boolean -- Checks whether this transceiver is inside an active RF jammer's range.")
    public Object[] isJammed(Context context, Arguments args) {
        return result(level != null && RFJammerManager.INSTANCE.isJammed(level, worldPosition));
    }
}
