package pl.asie.computronics.item;

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
import pl.asie.computronics.panel.PanelComponentType;

import java.util.List;

/**
 * Base item for everything that mounts on a control panel. Sneak + right-click in the air cycles
 * the placement size; right-clicking a panel mounts it (handled by BlockControlPanel).
 */
public class ItemPanelComponent extends Item {
    private final PanelComponentType type;

    public ItemPanelComponent(Properties properties, PanelComponentType type) {
        super(properties);
        this.type = type;
    }

    public PanelComponentType type() {
        return type;
    }

    public static int getPlacementSize(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        return cd != null ? Math.max(1, Math.min(4, cd.copyTag().getInt("size"))) : 1;
    }

    public static void setPlacementSize(ItemStack stack, int size) {
        int s = Math.max(1, Math.min(4, size));
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("size", s));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        int next = getPlacementSize(stack) % 4 + 1;
        setPlacementSize(stack, next);
        if (level.isClientSide) {
            player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.4f);
        } else {
            int[] fp = type.footprint(next);
            player.displayClientMessage(Component.translatable("message.computronics.panel.size", fp[0], fp[1]), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.computronics." + type.id() + ".desc").withStyle(ChatFormatting.GRAY));
        int[] fp = type.footprint(getPlacementSize(stack));
        tooltip.add(Component.translatable("tooltip.computronics.panel.size", fp[0], fp[1]).withStyle(ChatFormatting.GOLD));
    }
}
