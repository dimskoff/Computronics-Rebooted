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

import java.util.Map;

public class EnvironmentRackCapacitorBoard extends ComponentConnectableRackMountableEnvironment {
    public static final double CAPACITY = 50000.0;

    public EnvironmentRackCapacitorBoard(EnvironmentHost host) {
        setNode(Network.newNode(this, Visibility.Network)
                .withComponent("capacitor_board", Visibility.Network)
                .withConnector(CAPACITY)
                .create());
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
                DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Power,
                DeviceInfo.DeviceAttribute.Description, "Rack High-Density Capacitor",
                DeviceInfo.DeviceAttribute.Vendor, "Computronics",
                DeviceInfo.DeviceAttribute.Product, "CapacitorBoard-50k",
                DeviceInfo.DeviceAttribute.Capacity, "50000"
        );
    }

    private Connector connector() {
        return (Connector) node();
    }

    @Callback(direct = true, doc = "function():number -- Gets stored energy in rack capacitor.")
    public Object[] getEnergy(Context context, Arguments args) {
        return new Object[]{connector() != null ? connector().globalBuffer() : 0.0};
    }

    @Callback(direct = true, doc = "function():number -- Gets maximum capacitor buffer capacity (50000 EU).")
    public Object[] getMaxEnergy(Context context, Arguments args) {
        return new Object[]{CAPACITY};
    }
}
