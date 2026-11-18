package pl.asie.computronics.oc.environment;

import li.cil.oc.api.Network;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.core.component.DataComponentHolder;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import pl.asie.computronics.config.ComputronicsConfig;
import pl.asie.computronics.oc.OCData;
import pl.asie.computronics.oc.SelfDestructFuse;

/** Self-destruct card, component "self_destruct" (original Computronics API). */
public class EnvironmentCardBoom extends AbstractManagedEnvironment {
    private final SelfDestructFuse fuse;

    public EnvironmentCardBoom(EnvironmentHost host) {
        this.fuse = new SelfDestructFuse(host);
        setNode(Network.newNode(this, Visibility.Neighbors)
                .withComponent("self_destruct", Visibility.Neighbors)
                .create());
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void update() {
        super.update();
        if (fuse.tick()) {
            fuse.explode(ComputronicsConfig.SERVER.cardBoomExplosionPower.get().floatValue(),
                    ComputronicsConfig.SERVER.cardBoomAllowBlockDamage.get());
        }
    }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        fuse.onMessage(node(), message);
    }

    @Callback(doc = "function([time:number=5]):number -- Starts the countdown; the card explodes after the given number of seconds.")
    public Object[] start(Context context, Arguments args) {
        return fuse.start(args.optDouble(0, 5.0), ComputronicsConfig.SERVER.cardBoomFuseTicks.get());
    }

    @Callback(direct = true, doc = "function():number -- Returns the time in seconds left until the explosion.")
    public Object[] time(Context context, Arguments args) {
        return fuse.time();
    }

    @Override
    public void loadData(DataComponentHolder holder) throws UnrecoverablePersistanceException {
        super.loadData(holder);
        fuse.load(OCData.read(holder));
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        super.saveData(holder);
        OCData.write(holder, fuse::save);
    }
}
