package pl.asie.computronics.item.rack;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

public class ItemRackLightBoard extends Item {
    public ItemRackLightBoard(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static int getMode(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd != null && cd.contains("mode")) {
            return Math.max(1, Math.min(5, cd.copyTag().getInt("mode")));
        }
        return 1;
    }

    public static void setMode(ItemStack stack, int mode) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("mode", Math.max(1, Math.min(5, mode))));
    }

    public static String getModeName(int mode) {
        return switch (mode) {
            case 1 -> "Default (4 LEDs)";
            case 2 -> "Regular (5 LEDs)";
            case 3 -> "Five (10 LEDs)";
            case 4 -> "Twelve (12 LEDs)";
            case 5 -> "Total (42 LEDs)";
            default -> "Default (4 LEDs)";
        };
    }

    public static int getLightCount(int mode) {
        return switch (mode) {
            case 1 -> 4;
            case 2 -> 5;
            case 3 -> 10;
            case 4 -> 12;
            case 5 -> 42;
            default -> 4;
        };
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            int current = getMode(stack);
            int next = (current % 5) + 1;
            setMode(stack, next);
            if (level.isClientSide()) {
                player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.8f, 1.2f);
            } else {
                player.displayClientMessage(Component.literal(String.format("§b[Light Board] §fMode: §e%s", getModeName(next))), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int mode = getMode(stack);
        tooltip.add(Component.literal("Mode: " + getModeName(mode)).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.computronics.rack_board_light.desc1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Shift + Right-Click in hand to cycle LED modes.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
