package pl.asie.computronics.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import pl.asie.computronics.signal.ISignalNode;
import pl.asie.computronics.signal.Signals;

import java.util.List;

/**
 * Signal wire. Right-click a port, then another port of the opposite direction to connect them
 * (one wire is used per connection). Sneak + right-click a port to unplug it.
 * Sneak + right-click in the air cancels a pending connection.
 */
public class ItemSignalWire extends Item {
    private static final String PENDING = "pending";

    public ItemSignalWire(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        ISignalNode node = Signals.node(level, ctx.getClickedPos());
        if (node == null) return InteractionResult.PASS;
        ISignalNode.PortRef ref = node.pickPort(player, ctx.getClickLocation());
        if (ref == null) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.computronics.wire.no_port"), true);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide) return InteractionResult.SUCCESS;

        ItemStack stack = ctx.getItemInHand();
        BlockPos pos = ctx.getClickedPos();

        if (player.isShiftKeyDown()) {
            int removed = ref.output() ? Signals.disconnectOutput(level, pos, ref.port())
                    : (Signals.disconnectInput(level, pos, ref.port()) ? 1 : 0);
            if (removed > 0 && !player.getAbilities().instabuild) {
                ItemStack back = new ItemStack(this, removed);
                if (!player.getInventory().add(back)) player.drop(back, false);
            }
            player.displayClientMessage(Component.translatable("message.computronics.wire.unplugged", removed), true);
            clearPending(stack);
            return InteractionResult.CONSUME;
        }

        CompoundTag pending = pending(stack);
        if (pending == null) {
            CompoundTag t = new CompoundTag();
            t.putLong("pos", pos.asLong());
            t.putInt("port", ref.port());
            t.putBoolean("out", ref.output());
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(PENDING, t));
            player.displayClientMessage(Component.translatable("message.computronics.wire.start", node.describePort(ref)), true);
            return InteractionResult.CONSUME;
        }

        BlockPos firstPos = BlockPos.of(pending.getLong("pos"));
        ISignalNode.PortRef first = new ISignalNode.PortRef(pending.getInt("port"), pending.getBoolean("out"));
        Signals.Result r = Signals.connect(level, firstPos, first, pos, ref);
        switch (r) {
            case OK -> {
                stack.consume(1, player);
                if (!stack.isEmpty()) clearPending(stack);
                level.playSound(null, pos, SoundEvents.TRIPWIRE_ATTACH, SoundSource.BLOCKS, 0.6f, 1.4f);
                player.displayClientMessage(Component.translatable("message.computronics.wire.connected"), true);
            }
            case SAME_DIRECTION -> player.displayClientMessage(Component.translatable("message.computronics.wire.same_direction"), true);
            case TOO_FAR -> {
                player.displayClientMessage(Component.translatable("message.computronics.wire.too_far", (int) Signals.MAX_WIRE_LENGTH), true);
                clearPending(stack);
            }
            default -> {
                player.displayClientMessage(Component.translatable("message.computronics.wire.no_port"), true);
                clearPending(stack);
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && pending(stack) != null) {
            if (!level.isClientSide) {
                clearPending(stack);
                player.displayClientMessage(Component.translatable("message.computronics.wire.cancelled"), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    private static CompoundTag pending(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return null;
        CompoundTag t = cd.copyTag();
        return t.contains(PENDING) ? t.getCompound(PENDING) : null;
    }

    private static void clearPending(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return;
        CompoundTag t = cd.copyTag();
        t.remove(PENDING);
        if (t.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
        else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return pending(stack) != null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.computronics.signal_wire.desc1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.computronics.signal_wire.desc2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.computronics.signal_wire.desc3").withStyle(ChatFormatting.DARK_GRAY));
    }
}
