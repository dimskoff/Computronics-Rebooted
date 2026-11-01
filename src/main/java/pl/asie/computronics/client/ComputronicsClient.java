package pl.asie.computronics.client;

import li.cil.oc.api.Manual;
import li.cil.oc.api.manual.PathProvider;
import li.cil.oc.api.prefab.ItemStackTabIconRenderer;
import li.cil.oc.api.prefab.ResourceContentProvider;
import net.minecraft.client.Minecraft;
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

    public static void onRegisterRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(pl.asie.computronics.init.ModBlockEntities.CONTROL_PANEL.get(), ControlPanelRenderer::new);
        event.registerBlockEntityRenderer(pl.asie.computronics.init.ModBlockEntities.TAPE_DRIVE.get(), TapeDriveRenderer::new);
    }

    public static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                NeoForge.EVENT_BUS.register(PocketTerminalRenderer.INSTANCE);
                NeoForge.EVENT_BUS.register(RackMountableRenderer.INSTANCE);
                NeoForge.EVENT_BUS.register(ComputronicsClient.class);

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
                        if (stack.getItem() instanceof pl.asie.computronics.item.rack.ItemRackLightBoard) {
                            return "%LANGUAGE%/computronics/item/rack_board_light.md";
                        }
                        if (stack.getItem() instanceof pl.asie.computronics.item.rack.ItemRackSwitchBoard) {
                            return "%LANGUAGE%/computronics/item/rack_board_switch.md";
                        }
                        if (stack.getItem() instanceof pl.asie.computronics.item.rack.ItemRackBoomBoard) {
                            return "%LANGUAGE%/computronics/item/rack_board_boom.md";
                        }
                        if (stack.getItem() instanceof pl.asie.computronics.item.rack.ItemRackCapacitorBoard) {
                            return "%LANGUAGE%/computronics/item/rack_board_capacitor.md";
                        }
                        if (stack.getItem() instanceof pl.asie.computronics.item.rack.ItemRackCoProcessor) {
                            return "%LANGUAGE%/computronics/item/rack_board_co_processor.md";
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

    @net.neoforged.bus.api.SubscribeEvent
    public static void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        ItemStack main = mc.player.getMainHandItem();
        ItemStack off = mc.player.getOffhandItem();
        boolean holdingScrewdriver = (main.getItem() instanceof ItemScrewdriver) || (off.getItem() instanceof ItemScrewdriver);
        if (!holdingScrewdriver) return;

        if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult bhr) {
            BlockPos pos = bhr.getBlockPos();
            BlockState state = mc.level.getBlockState(pos);
            if (state.getBlock() instanceof pl.asie.computronics.block.BlockControlPanel) {
                if (mc.level.getBlockEntity(pos) instanceof pl.asie.computronics.block.entity.TileControlPanel panel) {
                    int[] grid = pl.asie.computronics.block.BlockControlPanel.calculateGrid(state, pos, bhr.getLocation());
                    if (grid != null) {
                        int gx = grid[0];
                        int gy = grid[1];
                        pl.asie.computronics.block.entity.TileControlPanel.PanelElement elem = panel.findRootElement(gx, gy);
                        String msg;
                        if (elem != null && elem.type != pl.asie.computronics.block.entity.TileControlPanel.ElementType.NONE) {
                            String typeStr = elem.type.name();
                            int slot1 = elem.slot + 1;
                            String extra = "";
                            if (elem.type == pl.asie.computronics.block.entity.TileControlPanel.ElementType.BUTTON && !elem.label.isEmpty()) {
                                extra = " §7| Label: '§f" + elem.label + "§7'";
                            } else if (elem.type == pl.asie.computronics.block.entity.TileControlPanel.ElementType.SWITCH) {
                                extra = " §7| State: " + (elem.active ? "§a[ON]" : "§c[OFF]");
                            } else if (elem.type == pl.asie.computronics.block.entity.TileControlPanel.ElementType.SEVEN_SEGMENT) {
                                extra = " §7| Val: §b\"" + elem.displayValue + "\"";
                            }
                            msg = String.format("§b[Screwdriver] §fSlot: §e#%d §7(X:%d, Y:%d) §7| Type: §6%s §7| Size: §d%dx%d%s",
                                    slot1, elem.slot % 8, elem.slot / 8, typeStr, elem.sizeX, elem.sizeY, extra);
                        } else {
                            int slot1 = gy * 8 + gx + 1;
                            msg = String.format("§7[Screwdriver] Grid: (X:%d, Y:%d) -> Slot #%d [Empty]", gx, gy, slot1);
                        }
                        mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal(msg), true);
                    }
                }
            }
        }
    }
}
