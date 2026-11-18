package pl.asie.computronics.oc.environment;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import pl.asie.computronics.config.ComputronicsServerConfig;
import pl.asie.computronics.network.IRFJammer;
import pl.asie.computronics.network.RFJammerManager;

public class EnvironmentRFJammerCard extends AbstractManagedEnvironment implements IRFJammer {
    private final EnvironmentHost host;
    private volatile int frequency = 0;       // 0 = broad spectrum
    private volatile double power = 10.0;     // 1.0 to 100.0
    private volatile boolean enabled = false;
    private volatile boolean powered = false;
    // Position cached on the server thread in update(); read by the manager.
    private volatile BlockPos cachedPos = null;

    public EnvironmentRFJammerCard(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Neighbors)
                .withComponent("rf_jammer", Visibility.Neighbors)
                .withConnector(500)
                .create());
    }

    private Connector connector() {
        return node() instanceof Connector c ? c : null;
    }

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
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void update() {
        super.update();
        if (host != null) {
            cachedPos = BlockPos.containing(host.xPosition(), host.yPosition(), host.zPosition());
        }
        if (!enabled) {
            powered = false;
            return;
        }
        double cost = energyPerTick();
        Connector c = connector();
        if (cost <= 0) {
            powered = true;
        } else {
            powered = c != null && c.network() != null && c.tryChangeBuffer(-cost);
        }
    }

    @Override
    public void onConnect(Node node) {
        super.onConnect(node);
        if (node == this.node()) {
            RFJammerManager.INSTANCE.register(this);
        }
    }

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (node == this.node()) {
            enabled = false;
            powered = false;
            RFJammerManager.INSTANCE.unregister(this);
        }
    }

    @Override
    public Level getJammerLevel() {
        return host != null ? host.getEnvironmentLevel() : null;
    }

    @Override
    public BlockPos getJammerPos() {
        return cachedPos;
    }

    @Override
    public int getFrequency() {
        return frequency;
    }

    @Override
    public double getEffectiveRadius() {
        return Math.min(RFJammerManager.maxRange(), power * 0.75);
    }

    @Override
    public boolean isEnabled() {
        return enabled && powered;
    }

    @Callback(direct = true, doc = "function():number -- Gets currently jammed frequency (0 = broad spectrum).")
    public Object[] getFrequency(Context context, Arguments args) {
        return new Object[]{frequency};
    }

    @Callback(doc = "function(frequency:number):number -- Sets jammed frequency (0 = broad spectrum).")
    public Object[] setFrequency(Context context, Arguments args) {
        this.frequency = Math.max(0, args.checkInteger(0));
        return new Object[]{frequency};
    }

    @Callback(direct = true, doc = "function():number -- Gets jamming transmitter power.")
    public Object[] getPower(Context context, Arguments args) {
        return new Object[]{power};
    }

    @Callback(doc = "function(power:number):number -- Sets transmitter power (1.0 - 100.0).")
    public Object[] setPower(Context context, Arguments args) {
        double val = args.checkDouble(0);
        this.power = Double.isNaN(val) ? 10.0 : Math.max(1.0, Math.min(val, 100.0));
        return new Object[]{power};
    }

    @Callback(direct = true, doc = "function():number -- Gets effective jamming radius in blocks.")
    public Object[] getRadius(Context context, Arguments args) {
        return new Object[]{getEffectiveRadius()};
    }

    @Callback(direct = true, doc = "function():boolean, boolean -- Returns whether the card is switched on, and whether it is actually jamming (powered).")
    public Object[] isEnabled(Context context, Arguments args) {
        return new Object[]{enabled, enabled && powered};
    }

    @Callback(doc = "function(state:boolean):boolean -- Toggles jamming transmission.")
    public Object[] setEnabled(Context context, Arguments args) {
        this.enabled = args.checkBoolean(0);
        if (!enabled) powered = false;
        return new Object[]{this.enabled};
    }

    @Callback(doc = "function([frequency:number]):boolean -- Checks if this area is affected by any active RF jammer.")
    public Object[] isAreaJammed(Context context, Arguments args) {
        int checkFreq = args.optInteger(0, -1);
        Level world = getJammerLevel();
        BlockPos pos = host != null ? BlockPos.containing(host.xPosition(), host.yPosition(), host.zPosition()) : null;
        if (world == null || pos == null) {
            return new Object[]{false};
        }
        return new Object[]{RFJammerManager.INSTANCE.isAreaJammed(world, pos, checkFreq)};
    }
}
