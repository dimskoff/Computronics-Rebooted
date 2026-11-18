package pl.asie.computronics.oc.driver.rack;

import li.cil.oc.api.Network;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.ComponentConnectableRackMountableEnvironment;
import net.minecraft.core.component.DataComponentHolder;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import pl.asie.computronics.config.ComputronicsConfig;
import pl.asie.computronics.oc.OCData;
import pl.asie.computronics.oc.SelfDestructFuse;

import java.util.Map;

/**
 * Rack detonator board, component "server_destruct" (original Computronics API: start/time).
 * Client data key read by RackMountableRenderer: {@code fuseTicks} (>= 0 while armed).
 */
public class EnvironmentRackBoomBoard extends ComponentConnectableRackMountableEnvironment {
    private final EnvironmentHost host;
    private final SelfDestructFuse fuse;

    public EnvironmentRackBoomBoard(EnvironmentHost host) {
        this.host = host;
        this.fuse = new SelfDestructFuse(host);
        setNode(Network.newNode(this, Visibility.Network)
                .withComponent("server_destruct", Visibility.Network)
                .create());
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
                DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
                DeviceInfo.DeviceAttribute.Description, "Rack Detonator Board",
                DeviceInfo.DeviceAttribute.Vendor, "Computronics",
                DeviceInfo.DeviceAttribute.Product, "BoomBoard-1"
        );
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void update() {
        super.update();
        if (fuse.tick()) {
            fuse.explode(ComputronicsConfig.SERVER.rackBoomExplosionPower.get().floatValue(),
                    ComputronicsConfig.SERVER.rackBoomAllowBlockDamage.get());
        }
    }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        boolean wasRunning = fuse.isRunning();
        fuse.onMessage(node(), message);
        if (wasRunning != fuse.isRunning()) markRackDirty();
    }

    @Callback(doc = "function([time:number=5]):number -- Starts the countdown; the server rack explodes after the given number of seconds.")
    public Object[] start(Context context, Arguments args) {
        Object[] result = fuse.start(args.optDouble(0, 5.0), 0);
        markRackDirty();
        return result;
    }

    @Callback(direct = true, doc = "function():number -- Returns the time in seconds left until the explosion.")
    public Object[] time(Context context, Arguments args) {
        return fuse.time();
    }

    private void markRackDirty() {
        if (host instanceof Rack rack) {
            int index = rack.indexOfMountable(this);
            if (index >= 0) rack.markChanged(index);
        }
    }

    @Override
    public void loadData(DataComponentHolder holder) throws UnrecoverablePersistanceException {
        super.loadData(holder);
        fuse.load(OCData.read(holder));
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        super.saveData(holder);
        OCData.write(holder, fuse::save);
    }

    @Override
    public void describeForClient(MutableDataComponentHolder holder) {
        OCData.write(holder, fuse::save);
    }
}
