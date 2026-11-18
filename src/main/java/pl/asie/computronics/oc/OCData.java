package pl.asie.computronics.oc;

import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

import java.util.function.Consumer;

/**
 * Helpers for OC 1.9 data-component persistence. Environments store their own state in
 * {@link DataComponents#CUSTOM_DATA} of the holder OC hands to {@code loadData/saveData}
 * (merged, so the node's own components and foreign keys are preserved). The legacy
 * CompoundTag bridge exposes the whole old tag as CUSTOM_DATA, so old top-level keys keep loading.
 */
public final class OCData {
    /** Key the OC NBT bridge uses for the encoded component patch; never copy it back into CUSTOM_DATA. */
    private static final String OC_COMPONENTS_TAG = "opencomputers:components";

    private OCData() {}

    public static CompoundTag read(DataComponentHolder holder) {
        if (holder == null) return new CompoundTag();
        CustomData data = holder.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    public static void write(MutableDataComponentHolder holder, Consumer<CompoundTag> writer) {
        if (holder == null) return;
        CompoundTag tag = read(holder);
        tag.remove(OC_COMPONENTS_TAG);
        writer.accept(tag);
        holder.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** Draws energy from the node's network if it is a connector; returns true when there is no connector. */
    public static boolean tryConsume(Node node, double amount) {
        if (amount <= 0) return true;
        if (node instanceof Connector connector) {
            return connector.tryChangeBuffer(-amount);
        }
        return true;
    }
}
