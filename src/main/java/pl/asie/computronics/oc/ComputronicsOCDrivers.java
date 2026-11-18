package pl.asie.computronics.oc;

import li.cil.oc.api.Driver;
import li.cil.oc.api.FileSystem;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.internal.Drone;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import pl.asie.computronics.Computronics;
import pl.asie.computronics.init.ModItems;
import pl.asie.computronics.item.rack.ItemRackLightBoard;
import pl.asie.computronics.oc.driver.SimpleItemDriver;
import pl.asie.computronics.oc.driver.rack.EnvironmentRackBoomBoard;
import pl.asie.computronics.oc.driver.rack.EnvironmentRackCapacitorBoard;
import pl.asie.computronics.oc.driver.rack.EnvironmentRackCoProcessor;
import pl.asie.computronics.oc.driver.rack.EnvironmentRackLightBoard;
import pl.asie.computronics.oc.driver.rack.EnvironmentRackSwitchBoard;
import pl.asie.computronics.oc.environment.*;

import java.util.function.Predicate;
import java.util.function.Supplier;

import static pl.asie.computronics.oc.driver.SimpleItemDriver.TIER_2;
import static pl.asie.computronics.oc.driver.SimpleItemDriver.TIER_3;

public final class ComputronicsOCDrivers {
    private ComputronicsOCDrivers() {}

    private static Predicate<ItemStack> is(Supplier<? extends Item> item) {
        return stack -> stack.getItem() == item.get();
    }

    public static void init() {
        // Cards
        Driver.add(new SimpleItemDriver(is(ModItems.NETWORK_CARD_3), (s, h) -> new EnvironmentNetworkCardTier3(h), Slot.Card, TIER_3));
        Driver.add(new SimpleItemDriver(is(ModItems.PACKET_SNIFFER), (s, h) -> new EnvironmentPacketSniffer(h), Slot.Card, TIER_2));
        Driver.add(new SimpleItemDriver(is(ModItems.CARD_BOOM), (s, h) -> new EnvironmentCardBoom(h), Slot.Card, TIER_2));
        Driver.add(new SimpleItemDriver(is(ModItems.RF_JAMMER_CARD), (s, h) -> new EnvironmentRFJammerCard(h), Slot.Card, TIER_2));
        Driver.add(new SimpleItemDriver(is(ModItems.CARD_BEEP), (s, h) -> new EnvironmentCardBeep(h), Slot.Card, TIER_2));
        Driver.add(new SimpleItemDriver(is(ModItems.CARD_FX), (s, h) -> new EnvironmentCardFX(h), Slot.Card, TIER_2));

        // Upgrades
        Driver.add(new SimpleItemDriver(is(ModItems.CHAT_UPGRADE), (s, h) -> new EnvironmentChatUpgrade(h), Slot.Upgrade, TIER_2));
        Driver.add(new SimpleItemDriver(is(ModItems.SPEECH_UPGRADE), (s, h) -> new EnvironmentSpeechUpgrade(h), Slot.Upgrade, TIER_2));
        Driver.add(new SimpleItemDriver(is(ModItems.RADAR_UPGRADE), (s, h) -> new EnvironmentRadarUpgrade(h), Slot.Upgrade, TIER_2));
        Driver.add(new SimpleItemDriver(is(ModItems.CAMERA_UPGRADE), (s, h) -> new EnvironmentCameraUpgrade(h), Slot.Upgrade, TIER_2));
        Driver.add(new SimpleItemDriver(is(ModItems.WINCH_UPGRADE), (s, h) -> new EnvironmentDroneWinch(h), Slot.Upgrade, TIER_2,
                Drone.class::isAssignableFrom));

        // Server rack mountables
        Driver.add(new SimpleItemDriver(is(ModItems.RACK_BOARD_LIGHT),
                (s, h) -> new EnvironmentRackLightBoard(h, ItemRackLightBoard.getMode(s)), Slot.RackMountable, TIER_2));
        Driver.add(new SimpleItemDriver(is(ModItems.RACK_BOARD_SWITCH), (s, h) -> new EnvironmentRackSwitchBoard(h), Slot.RackMountable, TIER_2));
        Driver.add(new SimpleItemDriver(is(ModItems.RACK_BOARD_BOOM), (s, h) -> new EnvironmentRackBoomBoard(h), Slot.RackMountable, TIER_2));
        Driver.add(new SimpleItemDriver(is(ModItems.RACK_BOARD_CAPACITOR), (s, h) -> new EnvironmentRackCapacitorBoard(h), Slot.RackMountable, TIER_2));
        Driver.add(new SimpleItemDriver(is(ModItems.RACK_BOARD_CO_PROCESSOR), (s, h) -> new EnvironmentRackCoProcessor(h), Slot.RackMountable, TIER_2));

        // Tapes intentionally have no OC driver: they are only used inside the tape drive block.

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
            Items.registerFloppy(
                    "computronics",
                    ResourceLocation.fromNamespaceAndPath("computronics", "loot/computronics"),
                    DyeColor.CYAN,
                    () -> FileSystem.fromResource(ResourceLocation.fromNamespaceAndPath("computronics", "loot/computronics")),
                    true
            );
        } catch (Throwable t) {
            Computronics.LOGGER.warn("Failed to register Computronics loot floppies: {}", t.getMessage());
        }
    }
}
