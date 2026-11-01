package pl.asie.computronics.item;

import li.cil.oc.api.driver.item.Chargeable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
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
import pl.asie.computronics.terminal.PocketTerminalManager;

import java.util.List;
import java.util.UUID;

public class ItemPocketTerminal extends Item implements Chargeable {
    public static final double MAX_CHARGE = 20000.0;
    public static final int SCREEN_WIDTH = 32;
    public static final int SCREEN_HEIGHT = 12;
    public static final int TOTAL_VRAM = SCREEN_WIDTH * SCREEN_HEIGHT;
    public static final int DEFAULT_PORT = 404;

    public ItemPocketTerminal(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean canCharge(ItemStack stack) {
        return true;
    }

    @Override
    public double charge(ItemStack stack, double amount, boolean simulate) {
        double current = getCharge(stack);
        double space = MAX_CHARGE - current;
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
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putDouble("energy", Math.max(0.0, Math.min(amount, MAX_CHARGE)));
        });
    }

    public static boolean isActive(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && customData.contains("active")) {
            return customData.copyTag().getBoolean("active");
        }
        return false;
    }

    public static void setActive(ItemStack stack, boolean active) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putBoolean("active", active);
        });
    }

    public static int getListeningPort(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && customData.contains("port")) {
            return customData.copyTag().getInt("port");
        }
        return DEFAULT_PORT;
    }

    public static void setListeningPort(ItemStack stack, int port) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt("port", Math.max(1, Math.min(65535, port)));
        });
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
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                tag.putString("term_id", newId);
                tag.putInt("port", DEFAULT_PORT);
                tag.putString("firmware", "Micro-Kernel v2.0");
                tag.putInt("fg_col", 0x00FF66);
                tag.putInt("bg_col", 0x08150A);
                tag.putString("title", "POCKET TERMINAL");
            });
            return newId;
        }
        return id;
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
        double charge = getCharge(stack);
        return Math.round(13.0f * (float) (charge / MAX_CHARGE));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return isActive(stack) ? 0x00FF66 : 0x00AACC;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        getOrCreateTerminalId(stack);

        // Flash firmware if holding EEPROM in offhand or shift-clicking
        ItemStack offhand = player.getOffhandItem();
        if (player.isShiftKeyDown() && !offhand.isEmpty() && offhand.getItem().toString().toLowerCase().contains("eeprom")) {
            String eepromName = offhand.getHoverName().getString();
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                tag.putString("firmware", eepromName);
            });
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.literal("§a[Wireless Monitor] §fFlashed firmware from EEPROM: §e" + eepromName), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8f, 1.4f);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        if (!level.isClientSide()) {
            boolean active = isActive(stack);
            double charge = getCharge(stack);

            if (!active && charge <= 0.0) {
                player.displayClientMessage(Component.literal("§c[Wireless Monitor: Battery Depleted - Recharge in Charger]").withStyle(ChatFormatting.RED), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.6f, 0.5f);
                return InteractionResultHolder.sidedSuccess(stack, false);
            }

            boolean newActive = !active;
            setActive(stack, newActive);

            String id = getTerminalId(stack);
            String shortId = id != null && id.length() >= 8 ? id.substring(0, 8) : "--------";
            int port = getListeningPort(stack);

            if (newActive) {
                int pct = (int) Math.round(charge / MAX_CHARGE * 100.0);
                player.displayClientMessage(Component.literal("§a[Wireless Monitor: ON | Port " + port + " | ID: " + shortId + " | " + pct + "%]").withStyle(ChatFormatting.GREEN), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.6f, 1.8f);
            } else {
                player.displayClientMessage(Component.literal("§7[Wireless Monitor: STANDBY / OFF]").withStyle(ChatFormatting.GRAY), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.6f, 0.8f);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            String termId = getOrCreateTerminalId(stack);
            boolean active = isActive(stack);
            double charge = getCharge(stack);

            if (active) {
                if (level.getGameTime() % 20 == 0) {
                    double newCharge = Math.max(0.0, charge - 1.0);
                    setCharge(stack, newCharge);
                    if (newCharge <= 0.0) {
                        setActive(stack, false);
                        active = false;
                        player.displayClientMessage(Component.literal("§c[Pocket Terminal: Battery Depleted]").withStyle(ChatFormatting.RED), true);
                    }
                }

                if (active) {
                    PocketTerminalManager.updateTerminal(player, termId, stack);
                } else {
                    PocketTerminalManager.removeTerminal(termId);
                }
            } else {
                PocketTerminalManager.removeTerminal(termId);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        boolean active = isActive(stack);
        double charge = getCharge(stack);
        String id = getTerminalId(stack);
        String shortId = id != null && id.length() >= 8 ? id.substring(0, 8) : "None";
        int port = getListeningPort(stack);
        String fw = getFirmware(stack);

        tooltip.add(Component.translatable("tooltip.computronics.pocket_terminal.desc1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.computronics.pocket_terminal.desc2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.computronics.pocket_terminal.desc3").withStyle(ChatFormatting.DARK_GRAY));

        tooltip.add(Component.literal(active ? "§a● State: ACTIVE / ON" : "§7○ State: STANDBY / OFF"));
        int pct = (int) Math.round(charge / MAX_CHARGE * 100.0);
        tooltip.add(Component.literal(String.format("§eBattery: %.0f / %.0f EU (%d%%)", charge, MAX_CHARGE, pct)));
        tooltip.add(Component.literal("§dFirmware: §f" + fw));
        tooltip.add(Component.literal("§6Listening Port: §f" + port + " §8(Wireless Only)"));
        tooltip.add(Component.literal("§bDevice ID: §f" + (flag.isAdvanced() && id != null ? id : shortId)));
    }
}
