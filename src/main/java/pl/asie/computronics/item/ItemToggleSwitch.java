package pl.asie.computronics.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

public class ItemToggleSwitch extends Item {

    public ItemToggleSwitch(Properties properties) {
        super(properties);
    }

    public static int getPlacementSize(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom != null && custom.contains("size")) {
            int s = custom.copyTag().getInt("size");
            if (s == 2) return 2;
        }
        return 1;
    }

    public static void cycleSize(ItemStack stack, Player player) {
        int cur = getPlacementSize(stack);
        int next = (cur == 1) ? 2 : 1;
        CustomData custom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = custom.copyTag();
        tag.putInt("size", next);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        player.displayClientMessage(Component.literal("§6[Toggle Switch] §fРазмер установки: §e" + next + "x" + next), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6f, 1.3f);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                cycleSize(stack, player);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int size = getPlacementSize(stack);
        tooltip.add(Component.translatable("tooltip.computronics.toggle_switch.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Размер при установке: §e" + size + "x" + size).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Shift + ПКМ: сменить размер (1x1 / 2x2)").withStyle(ChatFormatting.DARK_GRAY));
    }
}
