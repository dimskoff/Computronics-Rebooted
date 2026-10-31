package pl.asie.computronics.block.entity;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import pl.asie.computronics.init.ModBlockEntities;

import java.util.List;

public class TileLaserTransceiver extends TileEntityComputronics {
    public static final int MAX_RANGE = 64;
    private TileLaserTransceiver linkedTransceiver = null;
    private int distanceToLinked = -1;
    private boolean beamBroken = false;

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

    @Override
    public void tickServer(Level level, BlockPos pos, BlockState state) {
        super.tickServer(level, pos, state);

        if (level.getGameTime() % 4 == 0) {
            updateLaserLink(level, pos);
        }
    }

    private void updateLaserLink(Level level, BlockPos pos) {
        Direction dir = getFacing();
        TileLaserTransceiver found = null;
        int foundDist = -1;
        boolean broken = false;

        for (int i = 1; i <= MAX_RANGE; i++) {
            BlockPos current = pos.relative(dir, i);
            BlockState st = level.getBlockState(current);

            // Check entity collision (optical tripwire)
            AABB beamBox = new AABB(current);
            List<Entity> entities = level.getEntities((Entity) null, beamBox);
            if (!entities.isEmpty()) {
                broken = true;
                if (!beamBroken && node != null) {
                    Entity collider = entities.get(0);
                    node.sendToReachable("computer.signal", "tripwire_triggered", collider.getName().getString(), (double) i);
                }
                break;
            }

            if (!st.isAir()) {
                if (level.getBlockEntity(current) instanceof TileLaserTransceiver other) {
                    if (other.getFacing() == dir.getOpposite()) {
                        found = other;
                        foundDist = i;
                    }
                }
                break;
            }
        }

        this.linkedTransceiver = found;
        this.distanceToLinked = foundDist;
        this.beamBroken = broken;
    }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        if ("network.message".equals(message.name()) && linkedTransceiver != null && !beamBroken) {
            if (linkedTransceiver.node() != null && message.source() != linkedTransceiver.node()) {
                // Forward packet across laser beam
                linkedTransceiver.node().sendToReachable("network.message", message.data());
            }
        }
    }

    @Callback(direct = true, doc = "function():boolean -- Checks if an active laser connection exists.")
    public Object[] isConnected(Context context, Arguments args) {
        return result(linkedTransceiver != null && !beamBroken);
    }

    @Callback(direct = true, doc = "function():number -- Gets distance to connected transceiver.")
    public Object[] getDistance(Context context, Arguments args) {
        return result(distanceToLinked);
    }

    @Callback(direct = true, doc = "function():boolean -- Checks if the optical laser beam is currently broken.")
    public Object[] isBeamBroken(Context context, Arguments args) {
        return result(beamBroken);
    }
}
