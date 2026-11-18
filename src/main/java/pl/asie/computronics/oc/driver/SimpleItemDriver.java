package pl.asie.computronics.oc.driver;

import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Generic OC item driver: one instance per component item, configured with a stack matcher,
 * an environment factory, the slot type, the OC tier (0-based: 0 = Tier 1, 1 = Tier 2, 2 = Tier 3)
 * and an optional host-class filter (e.g. drones only).
 */
public final class SimpleItemDriver implements HostAware {
    /** OC tier constants (li.cil.oc.common.Tier: None=-1, One=0, Two=1, Three=2). */
    public static final int TIER_1 = 0;
    public static final int TIER_2 = 1;
    public static final int TIER_3 = 2;

    private final Predicate<ItemStack> matcher;
    private final BiFunction<ItemStack, EnvironmentHost, ManagedEnvironment> factory;
    private final String slot;
    private final int tier;
    private final Predicate<Class<? extends EnvironmentHost>> hostFilter;

    public SimpleItemDriver(Predicate<ItemStack> matcher,
                            BiFunction<ItemStack, EnvironmentHost, ManagedEnvironment> factory,
                            String slot, int tier) {
        this(matcher, factory, slot, tier, host -> true);
    }

    public SimpleItemDriver(Predicate<ItemStack> matcher,
                            BiFunction<ItemStack, EnvironmentHost, ManagedEnvironment> factory,
                            String slot, int tier,
                            Predicate<Class<? extends EnvironmentHost>> hostFilter) {
        this.matcher = matcher;
        this.factory = factory;
        this.slot = slot;
        this.tier = tier;
        this.hostFilter = hostFilter;
    }

    @Override
    public boolean worksWith(ItemStack stack) {
        return stack != null && !stack.isEmpty() && matcher.test(stack);
    }

    @Override
    public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return worksWith(stack) && (host == null || hostFilter.test(host));
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host != null && !hostFilter.test(host.getClass())) return null;
        return factory.apply(stack, host);
    }

    @Override
    public String slot(ItemStack stack) {
        return slot;
    }

    @Override
    public int tier(ItemStack stack) {
        return tier;
    }
}
