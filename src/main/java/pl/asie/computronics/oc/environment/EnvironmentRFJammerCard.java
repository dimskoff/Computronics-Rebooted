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
import pl.asie.computronics.network.IRFJammer;
import pl.asie.computronics.network.RFJammerManager;

public class EnvironmentRFJammerCard extends AbstractManagedEnvironment implements IRFJammer {
    private final EnvironmentHost host;
    private int frequency = 0;       // 0 = broad spectrum
    private double power = 10.0;     // 1.0 to 100.0
    private boolean enabled = false;

    public EnvironmentRFJammerCard(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Neighbors)
                .withComponent("rf_jammer", Visibility.Neighbors)
                .withConnector(500)
                .create());
    }

    private Connector connector() {
        return (Connector) node();
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void update() {
        super.update();
        if (enabled) {
            double energyCost = Math.max(0.1, (power / 10.0) * 0.25);
            if (connector() != null && !connector().tryChangeBuffer(-energyCost)) {
                // Out of power, automatically disable
                enabled = false;
                RFJammerManager.INSTANCE.unregister(this);
            }
        }
    }

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (node == this.node()) {
            enabled = false;
            RFJammerManager.INSTANCE.unregister(this);
        }
    }

    @Override
    public Level getJammerLevel() {
        return host != null ? host.getEnvironmentLevel() : null;
    }

    @Override
    public BlockPos getJammerPos() {
        if (host == null) return null;
        return new BlockPos(
                (int) Math.floor(host.xPosition()),
                (int) Math.floor(host.yPosition()),
                (int) Math.floor(host.zPosition())
        );
    }

    @Override
    public int getFrequency() {
        return frequency;
    }

    @Override
    public double getEffectiveRadius() {
        return power * 0.75;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
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
        this.power = Math.max(1.0, Math.min(args.checkDouble(0), 100.0));
        return new Object[]{power};
    }

    @Callback(direct = true, doc = "function():number -- Gets effective jamming radius in blocks.")
    public Object[] getRadius(Context context, Arguments args) {
        return new Object[]{getEffectiveRadius()};
    }

    @Callback(direct = true, doc = "function():boolean -- Checks if the RF jammer card is actively transmitting.")
    public Object[] isEnabled(Context context, Arguments args) {
        return new Object[]{enabled};
    }

    @Callback(doc = "function(state:boolean):boolean -- Toggles jamming transmission.")
    public Object[] setEnabled(Context context, Arguments args) {
        boolean newState = args.checkBoolean(0);
        if (this.enabled != newState) {
            this.enabled = newState;
            if (this.enabled) {
                RFJammerManager.INSTANCE.register(this);
            } else {
                RFJammerManager.INSTANCE.unregister(this);
            }
        }
        return new Object[]{this.enabled};
    }

    @Callback(direct = true, doc = "function([frequency:number]):boolean -- Checks if this area is affected by any active RF jammer.")
    public Object[] isAreaJammed(Context context, Arguments args) {
        int checkFreq = args.optInteger(0, this.frequency);
        Level world = getJammerLevel();
        BlockPos pos = getJammerPos();
        if (world == null || pos == null) {
            return new Object[]{false};
        }
        return new Object[]{RFJammerManager.INSTANCE.isAreaJammed(world, pos, checkFreq)};
    }
}
