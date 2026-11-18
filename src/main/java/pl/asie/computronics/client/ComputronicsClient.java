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
        event.registerBlockEntityRenderer(pl.asie.computronics.init.ModBlockEntities.CONNECTOR.get(), ConnectorRenderer::new);
        event.registerBlockEntityRenderer(pl.asie.computronics.init.ModBlockEntities.ANALOG_GAUGE.get(), pl.asie.computronics.client.peripheral.AnalogGaugeRenderer::new);
    }

    public static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                NeoForge.EVENT_BUS.register(PocketTerminalRenderer.INSTANCE);
                NeoForge.EVENT_BUS.register(RackMountableRenderer.INSTANCE);
                NeoForge.EVENT_BUS.register(ComputronicsClient.class);
                NeoForge.EVENT_BUS.register(pl.asie.computronics.client.audio.ClientAudioManager.INSTANCE);
                pl.asie.computronics.util.SidedHooks.setPanelConfigOpener(pl.asie.computronics.client.gui.ControlPanelConfigScreen::open);
                pl.asie.computronics.util.SidedHooks.setPanelGeometryOpener(pl.asie.computronics.client.gui.PanelGeometryScreen::open);

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


    /** HUD hints: screwdriver shows the component under the cursor, the wire shows the port and its value. */
    @net.neoforged.bus.api.SubscribeEvent
    public static void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult bhr)) return;
        net.minecraft.world.item.Item held = mc.player.getMainHandItem().getItem();
        net.minecraft.world.level.block.entity.BlockEntity be = mc.level.getBlockEntity(bhr.getBlockPos());
        net.minecraft.network.chat.Component msg = null;

        if (held instanceof ItemScrewdriver && be instanceof pl.asie.computronics.block.entity.TileControlPanel panel) {
            int[] cell = pl.asie.computronics.block.BlockControlPanel.cellLookedAt(panel, mc.player);
            if (cell == null) return;
            pl.asie.computronics.panel.PanelComponent c = panel.layout().at(cell[0], cell[1]);
            msg = c == null
                    ? net.minecraft.network.chat.Component.translatable("message.computronics.panel.hud_empty", cell[0] + 1, cell[1] + 1)
                    : net.minecraft.network.chat.Component.translatable("message.computronics.panel.hud",
                            c.id(), net.minecraft.network.chat.Component.translatable(c.type.translationKey()), c.width, c.height);
        } else if (held instanceof pl.asie.computronics.item.ItemSignalWire && be instanceof pl.asie.computronics.signal.ISignalNode node) {
            pl.asie.computronics.signal.ISignalNode.PortRef ref = node.pickPort(mc.player, bhr.getLocation());
            if (ref == null) return;
            int value = ref.output() ? node.getOutput(ref.port())
                    : (be instanceof pl.asie.computronics.block.entity.TileConnector con ? con.input(ref.port())
                    : be instanceof pl.asie.computronics.block.entity.TileControlPanel p && p.layout().byId(ref.port()) != null
                    ? p.layout().byId(ref.port()).input : 0);
            msg = net.minecraft.network.chat.Component.translatable("message.computronics.wire.hud", node.describePort(ref), value)
                    .withStyle(ref.output() ? net.minecraft.ChatFormatting.RED : net.minecraft.ChatFormatting.GREEN);
        }
        if (msg != null) mc.player.displayClientMessage(msg, true);
    }
}