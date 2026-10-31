package pl.asie.computronics.block.entity;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Visibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import pl.asie.computronics.init.ModBlockEntities;
import pl.asie.computronics.network.RFJammerManager;

public class TileRFJammer extends TileEntityComputronics {
    private int frequency = 0;       // 0 = broad spectrum
    private double power = 10.0;     // 1.0 to 100.0
    private boolean enabled = false;

    public TileRFJammer(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RF_JAMMER.get(), pos, state);
        node = Network.newNode(this, Visibility.Network)
                .withComponent("rf_jammer", Visibility.Network)
                .withConnector(1000)
                .create();
    }

    private Connector connector() {
        return (Connector) node;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && enabled) {
            RFJammerManager.INSTANCE.register(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        RFJammerManager.INSTANCE.unregister(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        RFJammerManager.INSTANCE.unregister(this);
    }

    @Override
    public void tickServer(Level level, BlockPos pos, BlockState state) {
        super.tickServer(level, pos, state);
        if (enabled && connector() != null) {
            double cost = power * 0.1;
            if (!connector().tryChangeBuffer(-cost)) {
                // Not enough power, auto shutoff
                setEnabledInternal(false);
            }
        }
    }

    public int getFrequency() {
        return frequency;
    }

    public double getPower() {
        return power;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public double getEffectiveRadius() {
        return Math.min(64.0, Math.sqrt(power) * 6.0);
    }

    private void setEnabledInternal(boolean state) {
        this.enabled = state;
        if (state) {
            RFJammerManager.INSTANCE.register(this);
        } else {
            RFJammerManager.INSTANCE.unregister(this);
        }
        setChanged();
    }

    @Callback(direct = true, doc = "function():number -- Gets the current targeted frequency (0 = broad spectrum).")
    public Object[] getFrequency(Context context, Arguments args) {
        return result(frequency);
    }

    @Callback(doc = "function(freq:number):number -- Sets the targeted frequency.")
    public Object[] setFrequency(Context context, Arguments args) {
        this.frequency = Math.max(0, args.checkInteger(0));
        setChanged();
        return result(this.frequency);
    }

    @Callback(direct = true, doc = "function():number -- Gets the transmitter power.")
    public Object[] getPower(Context context, Arguments args) {
        return result(power);
    }

    @Callback(doc = "function(power:number):number -- Sets the transmitter power (1.0 to 100.0).")
    public Object[] setPower(Context context, Arguments args) {
        double val = args.checkDouble(0);
        this.power = Math.max(1.0, Math.min(val, 100.0));
        setChanged();
        return result(this.power);
    }

    @Callback(direct = true, doc = "function():boolean -- Checks if the RF jammer is running.")
    public Object[] isEnabled(Context context, Arguments args) {
        return result(enabled);
    }

    @Callback(doc = "function(state:boolean):boolean -- Enables or disables RF jamming.")
    public Object[] setEnabled(Context context, Arguments args) {
        boolean state = args.checkBoolean(0);
        setEnabledInternal(state);
        return result(this.enabled);
    }

    @Callback(direct = true, doc = "function():number -- Gets the jamming radius in blocks.")
    public Object[] getEffectiveRadius(Context context, Arguments args) {
        return result(getEffectiveRadius());
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("frequency", frequency);
        tag.putDouble("power", power);
        tag.putBoolean("enabled", enabled);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.frequency = tag.getInt("frequency");
        this.power = tag.contains("power") ? tag.getDouble("power") : 10.0;
        this.enabled = tag.getBoolean("enabled");
        if (this.enabled && level != null && !level.isClientSide) {
            RFJammerManager.INSTANCE.register(this);
        }
    }
}
