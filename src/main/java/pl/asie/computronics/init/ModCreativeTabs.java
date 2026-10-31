package pl.asie.computronics.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import pl.asie.computronics.Computronics;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Computronics.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB_COMPUTRONICS =
            CREATIVE_MODE_TABS.register("computronics", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.computronics"))
                    .icon(() -> new ItemStack(ModItems.NETWORK_CARD_3.get()))
                    .displayItems((parameters, output) -> {
                        // Blocks
                        output.accept(ModBlocks.CHAT_BOX.get());
                        output.accept(ModBlocks.RADAR.get());
                        output.accept(ModBlocks.CAMERA.get());
                        output.accept(ModBlocks.TAPE_DRIVE.get());
                        output.accept(ModBlocks.ACOUSTIC_SENSOR.get());
                        output.accept(ModBlocks.RF_JAMMER.get());
                        output.accept(ModBlocks.WEATHER_STATION.get());
                        output.accept(ModBlocks.INDUCTIVE_CHARGER.get());
                        output.accept(ModBlocks.LASER_TRANSCEIVER.get());
                        output.accept(ModBlocks.ANALOG_GAUGE.get());
                        output.accept(ModBlocks.IRON_NOTEBLOCK.get());
                        output.accept(ModBlocks.CONTROL_PANEL.get());
                        output.accept(ModBlocks.SPEECH_BOX.get());
                        output.accept(ModBlocks.ENTITY_DETECTOR.get());

                        // Handhelds, Modular Elements & Cards
                        output.accept(ModItems.POCKET_TERMINAL.get());
                        output.accept(ModItems.MINI_LAMP.get());
                        output.accept(ModItems.MINI_BUTTON.get());
                        output.accept(ModItems.NETWORK_CARD_3.get());
                        output.accept(ModItems.PACKET_SNIFFER.get());
                        output.accept(ModItems.CARD_BOOM.get());
                        output.accept(ModItems.RF_JAMMER_CARD.get());
                        output.accept(ModItems.CARD_BEEP.get());
                        output.accept(ModItems.CARD_FX.get());
                        output.accept(ModItems.CHAT_UPGRADE.get());
                        output.accept(ModItems.SPEECH_UPGRADE.get());
                        output.accept(ModItems.RADAR_UPGRADE.get());
                        output.accept(ModItems.CAMERA_UPGRADE.get());
                        output.accept(ModItems.WINCH_UPGRADE.get());

                        // Server Rack Mountables
                        output.accept(ModItems.RACK_BOARD_LIGHT.get());
                        output.accept(ModItems.RACK_BOARD_SWITCH.get());
                        output.accept(ModItems.RACK_BOARD_BOOM.get());
                        output.accept(ModItems.RACK_BOARD_CAPACITOR.get());
                        output.accept(ModItems.RACK_BOARD_CO_PROCESSOR.get());
                        output.accept(ModItems.RACK_BOARD_KVM.get());

                        // Cassette Tapes
                        output.accept(ModItems.TAPE_IRON.get());
                        output.accept(ModItems.TAPE_GOLD.get());
                        output.accept(ModItems.TAPE_DIAMOND.get());
                        output.accept(ModItems.TAPE_NETHER_STAR.get());
                    })
                    .build());
}
