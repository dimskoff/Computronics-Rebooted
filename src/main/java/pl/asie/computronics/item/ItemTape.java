package pl.asie.computronics.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.UUID;

public class ItemTape extends Item {
    public static final int CAPACITY_IRON = 1024 * 1024;          // 1 MB
    public static final int CAPACITY_GOLD = 2 * 1024 * 1024;      // 2 MB
    public static final int CAPACITY_DIAMOND = 4 * 1024 * 1024;   // 4 MB
    public static final int CAPACITY_NETHER_STAR = 16 * 1024 * 1024; // 16 MB

    private final int capacity;

    public ItemTape(Properties properties, int capacity) {
        super(properties.stacksTo(1));
        this.capacity = capacity;
    }

    public int getCapacity() {
        return capacity;
    }

    public static String getStorageId(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && customData.contains("storageId")) {
            return customData.copyTag().getString("storageId");
        }
        return null;
    }

    public static String getOrCreateStorageId(ItemStack stack) {
        String id = getStorageId(stack);
        if (id == null || id.isEmpty()) {
            final String newId = UUID.randomUUID().toString();
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString("storageId", newId));
            return newId;
        }
        return id;
    }

    public static String getLabel(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && customData.contains("tapeLabel")) {
            return customData.copyTag().getString("tapeLabel");
        }
        return null;
    }

    public static void setLabel(ItemStack stack, String label) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (label == null || label.isEmpty()) {
                tag.remove("tapeLabel");
            } else {
                tag.putString("tapeLabel", label);
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltip, tooltipFlag);
        String label = getLabel(stack);
        if (label != null && !label.isEmpty()) {
            tooltip.add(Component.literal("\"" + label + "\"").withStyle(ChatFormatting.YELLOW));
        }
        tooltip.add(Component.translatable("tooltip.computronics.tape.capacity", formatSize(capacity)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.computronics.tape.length", formatDuration(capacity)).withStyle(ChatFormatting.DARK_GRAY));
    }

    /** DFPWM at the default 32768 Hz sample rate stores 1 bit per sample = 4096 bytes per second. */
    public static final int BYTES_PER_SECOND = 4096;

    public static String formatSize(int bytes) {
        int kb = bytes / 1024;
        return kb >= 1024 ? (kb / 1024) + " MB" : kb + " KB";
    }

    /** Playback time of {@code bytes} of DFPWM audio at {@link #BYTES_PER_SECOND}, formatted as m:ss. */
    public static String formatDuration(int bytes) {
        int seconds = bytes / BYTES_PER_SECOND;
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}
