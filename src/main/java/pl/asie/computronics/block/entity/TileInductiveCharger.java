package pl.asie.computronics.block.entity;

import li.cil.oc.api.Network;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.api.internal.Drone;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Visibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import pl.asie.computronics.init.ModBlockEntities;

import java.util.List;

public class TileInductiveCharger extends TileEntityComputronics {
    private volatile boolean enabled = true;
    private volatile double chargeRate = 100.0; // EU/tick or OC Energy/tick

    public TileInductiveCharger(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INDUCTIVE_CHARGER.get(), pos, state);
        node = Network.newNode(this, Visibility.Network)
                .withComponent("inductive_charger", Visibility.Network)
                .withConnector(10000)
                .create();
    }

    private Connector connector() {
        return node instanceof Connector c ? c : null;
    }

    /** Resolves the energy connector of an OC agent (robot / drone) via the public API only. */
    private static Connector agentConnector(li.cil.oc.api.machine.MachineHost host) {
        if (host.machine() != null && host.machine().node() instanceof Connector conn) {
            return conn;
        }
        if (host instanceof li.cil.oc.api.network.Environment env && env.node() instanceof Connector conn) {
            return conn;
        }
        return null;
    }

    /**
     * Moves up to chargeRate energy from our network into the target connector.
     * Connector.changeBuffer returns the part of the delta that could NOT be applied; it is refunded.
     */
    private void chargeConnector(Connector own, Connector target) {
        if (target == null || target == own || (target.network() != null && target.network() == own.network())) {
            return;
        }
        double space = target.globalBufferSize() - target.globalBuffer();
        double toTransfer = Math.min(Math.min(chargeRate, own.globalBuffer()), space);
        if (toTransfer <= 0 || !own.tryChangeBuffer(-toTransfer)) {
            return;
        }
        double leftover = target.changeBuffer(toTransfer);
        if (leftover > 0) {
            own.changeBuffer(leftover);
        }
    }

    /** Chargeable.charge returns the energy that could NOT be stored (leftover). */
    private void chargeItem(Connector own, ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof li.cil.oc.api.driver.item.Chargeable chargeable) || !chargeable.canCharge(stack)) {
            return;
        }
        double toTransfer = Math.min(chargeRate, own.globalBuffer());
        if (toTransfer <= 0) return;
        double wouldRemain = chargeable.charge(stack, toTransfer, true);
        double accepted = toTransfer - Math.max(0.0, wouldRemain);
        if (accepted <= 0 || !own.tryChangeBuffer(-accepted)) {
            return;
        }
        double leftover = chargeable.charge(stack, accepted, false);
        if (leftover > 0) {
            own.changeBuffer(leftover);
        }
    }

    @Override
    public void tickServer(Level level, BlockPos pos, BlockState state) {
        super.tickServer(level, pos, state);

        Connector own = connector();
        if (!enabled || own == null || own.network() == null || own.globalBuffer() <= 0) {
            return;
        }

        // 1. Robot directly above (OC's robot block entity implements api.internal.Robot)
        BlockEntity beAbove = level.getBlockEntity(pos.above());
        if (beAbove instanceof Robot robot) {
            try {
                chargeConnector(own, agentConnector(robot));
            } catch (NoClassDefFoundError | ClassCastException e) {
                // OC internals changed; skip robot charging
            }
        }

        // 2. Drones hovering up to 3 blocks above the pad
        AABB chargeBox = new AABB(
                pos.getX(), pos.getY() + 1.0, pos.getZ(),
                pos.getX() + 1.0, pos.getY() + 4.0, pos.getZ() + 1.0
        );
        for (Entity entity : level.getEntities((Entity) null, chargeBox)) {
            if (own.globalBuffer() <= 0) return;
            if (entity instanceof Drone drone) {
                chargeConnector(own, agentConnector(drone));
            }
        }

        // 3. Chargeable items (tablets etc.) of players standing on the pad
        AABB playerBox = new AABB(
                pos.getX(), pos.getY() + 1.0, pos.getZ(),
                pos.getX() + 1.0, pos.getY() + 2.5, pos.getZ() + 1.0
        );
        List<Player> players = level.getEntitiesOfClass(Player.class, playerBox);
        for (Player p : players) {
            for (ItemStack itemStack : p.getInventory().items) {
                if (own.globalBuffer() <= 0) return;
                chargeItem(own, itemStack);
            }
            if (own.globalBuffer() <= 0) return;
            chargeItem(own, p.getOffhandItem());
        }
    }
    @Callback(direct = true, doc = "function():boolean -- Checks if the charger is enabled.")
    public Object[] isCharging(Context context, Arguments args) {
        return result(enabled);
    }

    @Callback(doc = "function(state:boolean):boolean -- Enables or disables wireless charging.")
    public Object[] setCharging(Context context, Arguments args) {
        this.enabled = args.checkBoolean(0);
        setChanged();
        return result(enabled);
    }

    @Callback(direct = true, doc = "function():number -- Gets the current charge rate.")
    public Object[] getChargeRate(Context context, Arguments args) {
        return result(chargeRate);
    }

    @Callback(doc = "function(rate:number):number -- Sets the charge rate per tick.")
    public Object[] setChargeRate(Context context, Arguments args) {
        this.chargeRate = Math.max(1.0, Math.min(args.checkDouble(0), 1000.0));
        setChanged();
        return result(chargeRate);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("enabled", enabled);
        tag.putDouble("chargeRate", chargeRate);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.enabled = !tag.contains("enabled") || tag.getBoolean("enabled");
        this.chargeRate = tag.contains("chargeRate") ? tag.getDouble("chargeRate") : 100.0;
    }
}
