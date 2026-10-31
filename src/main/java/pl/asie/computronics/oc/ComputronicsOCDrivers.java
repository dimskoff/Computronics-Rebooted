package pl.asie.computronics.oc;

import li.cil.oc.api.Driver;
import li.cil.oc.api.FileSystem;
import li.cil.oc.api.Items;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import pl.asie.computronics.Computronics;
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

        try {
            Items.registerFloppy(
                    "tape",
                    ResourceLocation.fromNamespaceAndPath("computronics", "loot/tape"),
                    DyeColor.YELLOW,
                    () -> FileSystem.fromResource(ResourceLocation.fromNamespaceAndPath("computronics", "loot/tape")),
                    true
            );
            Items.registerFloppy(
                    "explode",
                    ResourceLocation.fromNamespaceAndPath("computronics", "loot/explode"),
                    DyeColor.RED,
                    () -> FileSystem.fromResource(ResourceLocation.fromNamespaceAndPath("computronics", "loot/explode")),
                    true
            );
        } catch (Throwable t) {
            Computronics.LOGGER.warn("Failed to register Computronics loot floppies: {}", t.getMessage());
        }
    }
}
