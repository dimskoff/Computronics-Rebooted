package pl.asie.computronics.client;

import li.cil.oc.api.Manual;
import li.cil.oc.api.manual.PathProvider;
import li.cil.oc.api.prefab.ItemStackTabIconRenderer;
import li.cil.oc.api.prefab.ResourceContentProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import pl.asie.computronics.Computronics;
import pl.asie.computronics.block.BlockComputronics;
import pl.asie.computronics.init.ModItems;
import pl.asie.computronics.item.*;

public class ComputronicsClient {

    public static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                NeoForge.EVENT_BUS.register(PocketTerminalRenderer.INSTANCE);

                Manual.addProvider(new ResourceContentProvider("computronics", "doc/"));
                Manual.addTab(
                        new ItemStackTabIconRenderer(new ItemStack(ModItems.TAPE_DRIVE.get())),
                        "tooltip.computronics.manual.tab",
                        "%LANGUAGE%/computronics/index.md"
                );
                Manual.addProvider(new PathProvider() {
                    @Override
                    public String pathFor(ItemStack stack) {
                        if (stack == null || stack.isEmpty()) return null;
                        if (stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof BlockComputronics bc) {
                            String name = BuiltInRegistries.BLOCK.getKey(bc).getPath();
                            return "%LANGUAGE%/computronics/block/" + name + ".md";
                        }
                        if (stack.getItem() instanceof ItemTape) {
                            return "%LANGUAGE%/computronics/item/tape.md";
                        }
                        if (stack.getItem() instanceof ItemNetworkCardTier3) {
                            return "%LANGUAGE%/computronics/item/network_card_3.md";
                        }
                        if (stack.getItem() instanceof ItemPacketSniffer) {
                            return "%LANGUAGE%/computronics/item/packet_sniffer.md";
                        }
                        if (stack.getItem() instanceof ItemCardBoom) {
                            return "%LANGUAGE%/computronics/item/card_boom.md";
                        }
                        if (stack.getItem() instanceof ItemRFJammerCard) {
                            return "%LANGUAGE%/computronics/item/rf_jammer_card.md";
                        }
                        if (stack.getItem() instanceof ItemCardBeep) {
                            return "%LANGUAGE%/computronics/item/card_beep.md";
                        }
                        if (stack.getItem() instanceof ItemCardFX) {
                            return "%LANGUAGE%/computronics/item/card_fx.md";
                        }
                        if (stack.getItem() instanceof ItemPocketTerminal) {
                            return "%LANGUAGE%/computronics/item/pocket_terminal.md";
                        }
                        if (stack.getItem() instanceof ItemMiniLamp) {
                            return "%LANGUAGE%/computronics/item/mini_lamp.md";
                        }
                        if (stack.getItem() instanceof ItemMiniButton) {
                            return "%LANGUAGE%/computronics/item/mini_button.md";
                        }
                        if (stack.getItem() instanceof ItemChatUpgrade) {
                            return "%LANGUAGE%/computronics/item/chat_upgrade.md";
                        }
                        if (stack.getItem() instanceof ItemRadarUpgrade) {
                            return "%LANGUAGE%/computronics/item/radar_upgrade.md";
                        }
                        if (stack.getItem() instanceof ItemCameraUpgrade) {
                            return "%LANGUAGE%/computronics/item/camera_upgrade.md";
                        }
                        if (stack.getItem() instanceof ItemWinchUpgrade) {
                            return "%LANGUAGE%/computronics/item/winch_upgrade.md";
                        }
                        if (stack.getItem() instanceof ItemSpeechUpgrade) {
                            return "%LANGUAGE%/computronics/item/speech_upgrade.md";
                        }
                        return null;
                    }

                    @Override
                    public String pathFor(Level world, BlockPos pos) {
                        if (world == null || pos == null) return null;
                        BlockState state = world.getBlockState(pos);
                        if (state.getBlock() instanceof BlockComputronics bc) {
                            String name = BuiltInRegistries.BLOCK.getKey(bc).getPath();
                            return "%LANGUAGE%/computronics/block/" + name + ".md";
                        }
                        return null;
                    }
                });
                Computronics.LOGGER.info("Computronics: OpenComputers Manual and Pocket Terminal Renderer initialized successfully.");
            } catch (Throwable t) {
                Computronics.LOGGER.warn("Computronics: Could not initialize client systems: {}", t.getMessage());
            }
        });
    }
}
