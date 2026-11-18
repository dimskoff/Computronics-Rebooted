package pl.asie.computronics.oc.driver.rack;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.ComponentConnectableRackMountableEnvironment;
import pl.asie.computronics.config.ComputronicsConfig;

import java.util.Map;

/** Rack capacitor board, component "rack_capacitor"; capacity from config rackCapacitorCapacity. */
public class EnvironmentRackCapacitorBoard extends ComponentConnectableRackMountableEnvironment {
    private final double capacity;

    public EnvironmentRackCapacitorBoard(EnvironmentHost host) {
        this.capacity = ComputronicsConfig.SERVER.rackCapacitorCapacity.get();
        setNode(Network.newNode(this, Visibility.Network)
                .withComponent("rack_capacitor", Visibility.Network)
                .withConnector(capacity)
                .create());
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
                DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Power,
                DeviceInfo.DeviceAttribute.Description, "Rack High-Density Capacitor",
                DeviceInfo.DeviceAttribute.Vendor, "Computronics",
                DeviceInfo.DeviceAttribute.Product, "CapacitorBoard",
                DeviceInfo.DeviceAttribute.Capacity, String.valueOf((long) capacity)
        );
    }

    @Callback(direct = true, doc = "function():number -- Returns the amount of energy stored in this capacitor.")
    public Object[] energy(Context context, Arguments args) {
        return new Object[]{node() instanceof Connector c ? c.localBuffer() : 0.0};
    }

    @Callback(direct = true, doc = "function():number -- Returns the maximum amount of energy this capacitor can store.")
    public Object[] maxEnergy(Context context, Arguments args) {
        return new Object[]{node() instanceof Connector c ? c.localBufferSize() : capacity};
    }
}
