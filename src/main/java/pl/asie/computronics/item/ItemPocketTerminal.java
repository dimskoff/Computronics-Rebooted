package pl.asie.computronics.item;

import li.cil.oc.api.driver.item.Chargeable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import pl.asie.computronics.config.ComputronicsConfig;
import pl.asie.computronics.terminal.PocketTerminalManager;

import java.util.List;
import java.util.UUID;

public class ItemPocketTerminal extends Item implements Chargeable {
    /** Fallback values used when the config is not loaded yet. */
    public static final double MAX_CHARGE = 20000.0;
    public static final int SCREEN_WIDTH = 32;
    public static final int SCREEN_HEIGHT = 12;
    public static final int TOTAL_VRAM = SCREEN_WIDTH * SCREEN_HEIGHT;
    public static final int DEFAULT_PORT = 404;
    private static final ResourceLocation OC_EEPROM = ResourceLocation.fromNamespaceAndPath("opencomputers", "eeprom");

    public ItemPocketTerminal(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static double maxCharge() {
        try {
            return ComputronicsConfig.SERVER.wirelessMonitorMaxEnergy.get();
        } catch (Throwable t) {
            return MAX_CHARGE;
        }
    }

    public static double usagePerSecond() {
        try {
            return ComputronicsConfig.SERVER.wirelessMonitorEnergyUsagePerSecond.get();
        } catch (Throwable t) {
            return 1.0;
        }
    }

    public static int defaultPort() {
        try {
            return ComputronicsConfig.SERVER.wirelessMonitorDefaultPort.get();
        } catch (Throwable t) {
            return DEFAULT_PORT;
        }
    }

    private static boolean allowFlashing() {
        try {
            return ComputronicsConfig.SERVER.wirelessMonitorAllowEEPROMFlashing.get();
        } catch (Throwable t) {
            return true;
        }
    }

    @Override
    public boolean canCharge(ItemStack stack) {
        return true;
    }

    @Override
    public double charge(ItemStack stack, double amount, boolean simulate) {
        double current = getCharge(stack);
        double space = maxCharge() - current;
        double toAdd = Math.min(amount, Math.max(0.0, space));
        if (!simulate && toAdd > 0) {
            setCharge(stack, current + toAdd);
        }
        return amount - toAdd;
    }

    public static boolean canChargeStatic(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemPocketTerminal;
    }

    public static double chargeStatic(ItemStack stack, double amount, boolean simulate) {
        if (stack != null && stack.getItem() instanceof ItemPocketTerminal terminal) {
            return terminal.charge(stack, amount, simulate);
        }
        return amount;
    }

    public static double getCharge(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && customData.contains("energy")) {
            return customData.copyTag().getDouble("energy");
        }
        return 0.0;
    }

    public static void setCharge(ItemStack stack, double amount) {
        double max = maxCharge();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putDouble("energy", Math.max(0.0, Math.min(amount, max))));
    }

    public static boolean isActive(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null && customData.contains("active") && customData.copyTag().getBoolean("active");
    }

    public static void setActive(ItemStack stack, boolean active) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean("active", active));
    }

    public static int getListeningPort(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && customData.contains("port")) {
            return customData.copyTag().getInt("port");
        }
        return defaultPort();
    }

    public static void setListeningPort(ItemStack stack, int port) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("port", Math.max(1, Math.min(65535, port))));
    }

    public static String getFirmware(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && customData.contains("firmware")) {
            return customData.copyTag().getString("firmware");
        }
        return "Micro-Kernel v2.0";
    }

    public static String getTerminalId(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && customData.contains("term_id")) {
            return customData.copyTag().getString("term_id");
        }
        return null;
    }

    public static String getOrCreateTerminalId(ItemStack stack) {
        String id = getTerminalId(stack);
        if (id == null || id.isEmpty()) {
            final String newId = UUID.randomUUID().toString();
            final int port = defaultPort();
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                tag.putString("term_id", newId);
                tag.putInt("port", port);
                tag.putString("firmware", "Micro-Kernel v2.0");
                tag.putInt("fg_col", 0x00FF66);
                tag.putInt("bg_col", 0x08150A);
                tag.putString("title", "POCKET TERMINAL");
            });
            return newId;
        }
        return id;
    }

    private static boolean isEeprom(ItemStack stack) {
        return !stack.isEmpty() && OC_EEPROM.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0f * (float) Math.min(1.0, getCharge(stack) / maxCharge()));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return isActive(stack) ? 0x00FF66 : 0x00AACC;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        getOrCreateTerminalId(stack);

        ItemStack offhand = player.getOffhandItem();
        if (player.isShiftKeyDown() && hand == InteractionHand.MAIN_HAND && isEeprom(offhand)) {
            if (!allowFlashing()) {
                if (!level.isClientSide()) {
                    player.displayClientMessage(Component.translatable("message.computronics.pocket_terminal.flash_disabled").withStyle(ChatFormatting.RED), true);
                }
                return InteractionResultHolder.fail(stack);
            }
            String eepromName = offhand.getHoverName().getString();
            if (!level.isClientSide()) {
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString("firmware", eepromName));
                player.displayClientMessage(Component.translatable("message.computronics.pocket_terminal.flashed", eepromName).withStyle(ChatFormatting.GREEN), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8f, 1.4f);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        if (!level.isClientSide()) {
            boolean active = isActive(stack);
            double charge = getCharge(stack);

            if (!active && charge <= 0.0) {
                player.displayClientMessage(Component.translatable("message.computronics.pocket_terminal.depleted").withStyle(ChatFormatting.RED), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.6f, 0.5f);
                return InteractionResultHolder.fail(stack);
            }

            boolean newActive = !active;
            setActive(stack, newActive);
            String id = getTerminalId(stack);
            String shortId = id != null && id.length() >= 8 ? id.substring(0, 8) : "--------";

            if (newActive) {
                int pct = (int) Math.round(charge / maxCharge() * 100.0);
                player.displayClientMessage(Component.translatable("message.computronics.pocket_terminal.on",
                        getListeningPort(stack), shortId, pct).withStyle(ChatFormatting.GREEN), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.6f, 1.8f);
            } else {
                PocketTerminalManager.removeTerminal(id);
                player.displayClientMessage(Component.translatable("message.computronics.pocket_terminal.off").withStyle(ChatFormatting.GRAY), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.6f, 0.8f);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack stack, Player player) {
        if (!player.level().isClientSide()) {
            PocketTerminalManager.removeTerminal(getTerminalId(stack));
        }
        return super.onDroppedByPlayer(stack, player);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) return;
        if (!isActive(stack)) {
            String id = getTerminalId(stack);
            if (id != null) PocketTerminalManager.removeTerminal(id);
            return;
        }
        String termId = getOrCreateTerminalId(stack);
        if (level.getGameTime() % 20 == 0) {
            double newCharge = Math.max(0.0, getCharge(stack) - usagePerSecond());
            setCharge(stack, newCharge);
            if (newCharge <= 0.0) {
                setActive(stack, false);
                PocketTerminalManager.removeTerminal(termId);
                player.displayClientMessage(Component.translatable("message.computronics.pocket_terminal.depleted").withStyle(ChatFormatting.RED), true);
                return;
            }
        }
        PocketTerminalManager.updateTerminal(player, termId, stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        double charge = getCharge(stack);
        double max = maxCharge();
        String id = getTerminalId(stack);
        String shortId = id != null && id.length() >= 8 ? id.substring(0, 8) : "-";

        tooltip.add(Component.translatable("tooltip.computronics.pocket_terminal.desc1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.computronics.pocket_terminal.desc2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.computronics.pocket_terminal.desc3").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(isActive(stack)
                ? Component.translatable("tooltip.computronics.pocket_terminal.state_on").withStyle(ChatFormatting.GREEN)
                : Component.translatable("tooltip.computronics.pocket_terminal.state_off").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.computronics.pocket_terminal.battery",
                String.format("%.0f", charge), String.format("%.0f", max), (int) Math.round(charge / max * 100.0)).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.computronics.pocket_terminal.firmware", getFirmware(stack)).withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.computronics.pocket_terminal.port", getListeningPort(stack)).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.computronics.pocket_terminal.id", flag.isAdvanced() && id != null ? id : shortId).withStyle(ChatFormatting.AQUA));
    }
}
