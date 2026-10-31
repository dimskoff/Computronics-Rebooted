package pl.asie.computronics.oc.driver.rack;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.ComponentConnectableRackMountableEnvironment;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public class EnvironmentRackLightBoard extends ComponentConnectableRackMountableEnvironment {
    private final EnvironmentHost host;
    private final int[] colors = new int[4];
    private int mode = 1;

    public EnvironmentRackLightBoard(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network)
                .withComponent("light_board", Visibility.Network)
                .create());
    }

    @Callback(doc = "function(index:number, color:number):boolean -- Sets RGB color of an indicator light (0-3).")
    public Object[] setLightColor(Context context, Arguments args) {
        int index = args.checkInteger(0);
        int color = args.checkInteger(1) & 0xFFFFFF;
        if (index >= 0 && index < colors.length) {
            colors[index] = color;
            if (host instanceof Rack rack) {
                rack.markChanged(rack.indexOfMountable(this));
            }
            return new Object[]{true};
        }
        return new Object[]{false, "index out of range"};
    }

    @Callback(direct = true, doc = "function(index:number):number -- Gets RGB color of an indicator light (0-3).")
    public Object[] getLightColor(Context context, Arguments args) {
        int index = args.checkInteger(0);
        if (index >= 0 && index < colors.length) {
            return new Object[]{colors[index]};
        }
        return new Object[]{null, "index out of range"};
    }

    @Callback(doc = "function(colors:table):boolean -- Sets colors of all lights from table.")
    public Object[] setColors(Context context, Arguments args) {
        Map<?, ?> map = args.checkTable(0);
        for (int i = 0; i < colors.length; i++) {
            Object val = map.get(i + 1);
            if (val instanceof Number n) {
                colors[i] = n.intValue() & 0xFFFFFF;
            }
        }
        if (host instanceof Rack rack) {
            rack.markChanged(rack.indexOfMountable(this));
        }
        return new Object[]{true};
    }

    @Callback(direct = true, doc = "function():number -- Gets total number of lights on the board (4).")
    public Object[] getLightCount(Context context, Arguments args) {
        return new Object[]{colors.length};
    }

    @Callback(doc = "function():boolean -- Clears all light colors.")
    public Object[] clear(Context context, Arguments args) {
        for (int i = 0; i < colors.length; i++) {
            colors[i] = 0;
        }
        if (host instanceof Rack rack) {
            rack.markChanged(rack.indexOfMountable(this));
        }
        return new Object[]{true};
    }

    @Callback(doc = "function(mode:number):number -- Sets operation display mode (1-5).")
    public Object[] setMode(Context context, Arguments args) {
        this.mode = Math.max(1, Math.min(args.checkInteger(0), 5));
        if (host instanceof Rack rack) {
            rack.markChanged(rack.indexOfMountable(this));
        }
        return new Object[]{mode};
    }

    @Callback(direct = true, doc = "function():number -- Gets current display mode.")
    public Object[] getMode(Context context, Arguments args) {
        return new Object[]{mode};
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
                DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Display,
                DeviceInfo.DeviceAttribute.Description, "Rack Light Board",
                DeviceInfo.DeviceAttribute.Vendor, "Computronics",
                DeviceInfo.DeviceAttribute.Product, "LightBoard-4"
        );
    }

    @Override
    public boolean onActivate(Player player, InteractionHand hand, ItemStack stack, float hitX, float hitY) {
        if (!player.level().isClientSide()) {
            this.mode = (mode % 5) + 1;
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5f, 1.2f);
            if (host instanceof Rack rack) {
                rack.markChanged(rack.indexOfMountable(this));
            }
        }
        return true;
    }

    @Override
    public void loadData(CompoundTag tag, HolderLookup.Provider registries) throws li.cil.oc.api.UnrecoverablePersistanceException {
        super.loadData(tag, registries);
        for (int i = 0; i < colors.length; i++) {
            if (tag.contains("color_" + i)) {
                colors[i] = tag.getInt("color_" + i);
            }
        }
        if (tag.contains("mode")) {
            mode = tag.getInt("mode");
        }
    }

    @Override
    public void saveData(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveData(tag, registries);
        for (int i = 0; i < colors.length; i++) {
            tag.putInt("color_" + i, colors[i]);
        }
        tag.putInt("mode", mode);
    }
}
