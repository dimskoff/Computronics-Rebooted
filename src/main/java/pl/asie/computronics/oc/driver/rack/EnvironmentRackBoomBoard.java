package pl.asie.computronics.oc.driver.rack;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.ComponentConnectableRackMountableEnvironment;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

import java.util.Map;

public class EnvironmentRackBoomBoard extends ComponentConnectableRackMountableEnvironment {
    private final EnvironmentHost host;
    private int countdown = -1;

    public EnvironmentRackBoomBoard(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network)
                .withComponent("boom_board", Visibility.Network)
                .create());
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
                DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
                DeviceInfo.DeviceAttribute.Description, "Rack Detonator Board",
                DeviceInfo.DeviceAttribute.Vendor, "Computronics",
                DeviceInfo.DeviceAttribute.Product, "BoomBoard-1"
        );
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
                explode();
            }
        }
    }

    private void explode() {
        Level level = host.getEnvironmentLevel();
        if (level != null && !level.isClientSide()) {
            boolean blockDamage = pl.asie.computronics.config.ComputronicsConfig.SERVER.rackBoomAllowBlockDamage.get();
            float confPower = pl.asie.computronics.config.ComputronicsConfig.SERVER.rackBoomExplosionPower.get().floatValue();
            Level.ExplosionInteraction interaction = blockDamage ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.MOB;
            level.explode(null, host.xPosition(), host.yPosition(), host.zPosition(), confPower, interaction);
        }
    }

    @Callback(doc = "function():boolean -- Immediately detonates the rack boom board.")
    public Object[] detonate(Context context, Arguments args) {
        explode();
        return new Object[]{true};
    }

    @Callback(doc = "function(seconds:number):boolean -- Arms detonation countdown in seconds (1-60).")
    public Object[] setCountdown(Context context, Arguments args) {
        int sec = Math.max(1, Math.min(args.checkInteger(0), 60));
        this.countdown = sec * 20;
        return new Object[]{true, sec};
    }

    @Callback(direct = true, doc = "function():number -- Gets remaining countdown seconds, or -1 if disarmed.")
    public Object[] getCountdown(Context context, Arguments args) {
        return new Object[]{countdown > 0 ? (countdown / 20) : -1};
    }

    @Callback(doc = "function():boolean -- Disarms active detonation countdown.")
    public Object[] disarm(Context context, Arguments args) {
        this.countdown = -1;
        return new Object[]{true};
    }

    @Override
    public void loadData(CompoundTag tag, HolderLookup.Provider registries) throws li.cil.oc.api.UnrecoverablePersistanceException {
        super.loadData(tag, registries);
        if (tag.contains("countdown")) {
            countdown = tag.getInt("countdown");
        }
    }

    @Override
    public void saveData(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveData(tag, registries);
        tag.putInt("countdown", countdown);
    }
}
