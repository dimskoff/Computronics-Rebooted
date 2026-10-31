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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import pl.asie.computronics.init.ModBlockEntities;

import java.util.List;

public class TileInductiveCharger extends TileEntityComputronics {
    private boolean enabled = true;
    private double chargeRate = 100.0; // EU/tick or OC Energy/tick

    public TileInductiveCharger(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INDUCTIVE_CHARGER.get(), pos, state);
        node = Network.newNode(this, Visibility.Network)
                .withComponent("inductive_charger", Visibility.Network)
                .withConnector(10000)
                .create();
    }

    private Connector connector() {
        return (Connector) node;
    }

    @Override
    public void tickServer(Level level, BlockPos pos, BlockState state) {
        super.tickServer(level, pos, state);

        if (!enabled || connector() == null || connector().globalBuffer() <= 0) {
            return;
        }

        AABB chargeBox = new AABB(
                pos.getX(), pos.getY() + 1.0, pos.getZ(),
                pos.getX() + 1.0, pos.getY() + 3.0, pos.getZ() + 1.0
        );

        List<Entity> list = level.getEntities((Entity) null, chargeBox);
        for (Entity entity : list) {
            if (entity instanceof Drone drone && drone.machine() != null) {
                double buffer = connector().globalBuffer();
                double toTransfer = Math.min(chargeRate, buffer);
                if (toTransfer > 0 && connector().tryChangeBuffer(-toTransfer)) {
                    if (drone.machine().node() instanceof Connector droneConn) {
                        droneConn.changeBuffer(toTransfer);
                    }
                }
            }
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
