package pl.asie.computronics.oc.environment;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class EnvironmentDroneWinch extends AbstractManagedEnvironment {
    private final EnvironmentHost host;
    public static final int MAX_LENGTH = 32;

    private int deployedLength = 0;
    private Entity heldEntity = null;

    public EnvironmentDroneWinch(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network)
                .withComponent("winch", Visibility.Neighbors)
                .create());
    }

    @Callback(doc = "function(blocks:number):number -- Deploys winch cable downwards.")
    public Object[] deploy(Context context, Arguments args) {
        int amt = args.checkInteger(0);
        this.deployedLength = Math.min(MAX_LENGTH, this.deployedLength + Math.max(0, amt));
        return new Object[]{deployedLength};
    }

    @Callback(doc = "function(blocks:number):number -- Retracts winch cable upwards.")
    public Object[] retract(Context context, Arguments args) {
        int amt = args.checkInteger(0);
        this.deployedLength = Math.max(0, this.deployedLength - Math.max(0, amt));
        if (this.deployedLength == 0 && heldEntity != null) {
            heldEntity = null;
        }
        return new Object[]{deployedLength};
    }

    @Callback(direct = true, doc = "function():number -- Gets the current deployed length of the cable.")
    public Object[] getDeployedLength(Context context, Arguments args) {
        return new Object[]{deployedLength};
    }

    @Callback(doc = "function():boolean -- Grabs an item or entity beneath the winch hook.")
    public Object[] grab(Context context, Arguments args) {
        Level level = host.getEnvironmentLevel();
        if (level == null) return new Object[]{false};

        double hookX = host.xPosition();
        double hookY = host.yPosition() - deployedLength;
        double hookZ = host.zPosition();

        AABB box = new AABB(hookX - 1.0, hookY - 1.0, hookZ - 1.0, hookX + 1.0, hookY + 1.0, hookZ + 1.0);
        List<Entity> list = level.getEntities((Entity) null, box, Entity::isAlive);
        if (!list.isEmpty()) {
            this.heldEntity = list.get(0);
            return new Object[]{true};
        }
        return new Object[]{false};
    }

    @Callback(doc = "function():boolean -- Releases the currently held entity/item.")
    public Object[] release(Context context, Arguments args) {
        boolean released = heldEntity != null;
        this.heldEntity = null;
        return new Object[]{released};
    }
}
