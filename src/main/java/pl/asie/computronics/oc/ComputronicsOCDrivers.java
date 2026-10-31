package pl.asie.computronics.oc;

import li.cil.oc.api.Driver;
import pl.asie.computronics.oc.driver.*;

public final class ComputronicsOCDrivers {
    private ComputronicsOCDrivers() {}

    public static void init() {
        Driver.add(new DriverItemNetworkCardTier3());
        Driver.add(new DriverItemPacketSniffer());
        Driver.add(new DriverItemCardBoom());
        Driver.add(new DriverItemChatUpgrade());
        Driver.add(new DriverItemRadarUpgrade());
        Driver.add(new DriverItemCameraUpgrade());
        Driver.add(new DriverItemWinchUpgrade());
        Driver.add(new DriverItemTape());
    }
}
