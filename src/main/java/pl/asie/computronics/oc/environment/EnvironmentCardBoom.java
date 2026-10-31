package pl.asie.computronics.oc.environment;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.world.level.Level;

public class EnvironmentCardBoom extends AbstractManagedEnvironment {
    private final EnvironmentHost host;
    private int countdown = -1;
    private float explosionPower = 3.0f;

    public EnvironmentCardBoom(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Neighbors)
                .withComponent("boom", Visibility.Neighbors)
                .create());
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void update() {
        super.update();
        if (countdown > 0) {
            countdown--;
            if (countdown == 0) {
                explode(explosionPower);
            }
        }
    }

    private void explode(float power) {
        Level world = host.getEnvironmentLevel();
        if (world != null && !world.isClientSide) {
            world.explode(null, host.xPosition(), host.yPosition(), host.zPosition(), power, Level.ExplosionInteraction.BLOCK);
        }
    }

    @Callback(doc = "function([power:number]):boolean -- Detonates the self-destruct card immediately.")
    public Object[] detonate(Context context, Arguments args) {
        float power = (float) Math.min(args.optDouble(0, 3.0), 8.0);
        explode(power);
        return new Object[]{true};
    }

    @Callback(doc = "function(ticks:number[, power:number]):boolean -- Sets self-destruct countdown timer in ticks.")
    public Object[] setTime(Context context, Arguments args) {
        this.countdown = Math.max(1, args.checkInteger(0));
        this.explosionPower = (float) Math.min(args.optDouble(1, 3.0), 8.0);
        return new Object[]{true};
    }
}
