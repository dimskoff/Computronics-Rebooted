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
import pl.asie.computronics.config.ComputronicsServerConfig;
import pl.asie.computronics.init.ModBlockEntities;
import pl.asie.computronics.network.IRFJammer;
import pl.asie.computronics.network.RFJammerManager;

public class TileRFJammer extends TileEntityComputronics implements IRFJammer {
    private volatile int frequency = 0;       // 0 = broad spectrum
    private volatile double power = 10.0;     // 1.0 to 100.0
    private volatile boolean enabled = false; // switched on by a computer
    private volatile boolean powered = false; // had enough energy on the last tick

    public TileRFJammer(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RF_JAMMER.get(), pos, state);
        node = Network.newNode(this, Visibility.Network)
                .withComponent("rf_jammer", Visibility.Network)
                .withConnector(1000)
                .create();
    }

    @Override
    public Level getJammerLevel() {
        return level;
    }

    @Override
    public BlockPos getJammerPos() {
        return worldPosition;
    }

    private Connector connector() {
        return node instanceof Connector c ? c : null;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
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

    /** Energy drawn per tick: rfJammerEnergyPerSecond / 20, scaled by power (power 10 = 1x). */
    private double energyPerTick() {
        double perSecond;
        try {
            perSecond = ComputronicsServerConfig.SERVER.rfJammerEnergyPerSecond.get();
        } catch (Exception e) {
            perSecond = 20.0;
        }
        return perSecond / 20.0 * (power / 10.0);
    }

    @Override
    public void tickServer(Level level, BlockPos pos, BlockState state) {
        super.tickServer(level, pos, state);
        if (!enabled) {
            powered = false;
            return;
        }
        double cost = energyPerTick();
        Connector c = connector();
        if (cost <= 0) {
            powered = true;
        } else if (c != null && c.network() != null) {
            powered = c.tryChangeBuffer(-cost);
        } else {
            powered = false;
        }
    }

    public int getFrequency() {
        return frequency;
    }

    public double getPower() {
        return power;
    }

    /** Jamming only while switched on and powered. */
    @Override
    public boolean isEnabled() {
        return enabled && powered;
    }

    public double getEffectiveRadius() {
        return Math.min(RFJammerManager.maxRange(), Math.sqrt(power) * 6.0);
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
        this.power = Double.isNaN(val) ? 10.0 : Math.max(1.0, Math.min(val, 100.0));
        setChanged();
        return result(this.power);
    }

    @Callback(direct = true, doc = "function():boolean, boolean -- Returns whether the jammer is switched on, and whether it is actually jamming (powered).")
    public Object[] isEnabled(Context context, Arguments args) {
        return result(enabled, enabled && powered);
    }

    @Callback(doc = "function(state:boolean):boolean -- Enables or disables RF jamming.")
    public Object[] setEnabled(Context context, Arguments args) {
        this.enabled = args.checkBoolean(0);
        if (!enabled) powered = false;
        setChanged();
        return result(this.enabled);
    }

    @Callback(direct = true, doc = "function():number -- Gets the jamming radius in blocks.")
    public Object[] getEffectiveRadius(Context context, Arguments args) {
        return result(getEffectiveRadius());
    }

    @Callback(direct = true, doc = "function():number -- Gets the energy consumed per tick while enabled.")
    public Object[] getEnergyUsage(Context context, Arguments args) {
        return result(energyPerTick());
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
        this.power = tag.contains("power") ? Math.max(1.0, Math.min(tag.getDouble("power"), 100.0)) : 10.0;
        this.enabled = tag.getBoolean("enabled");
    }
}
