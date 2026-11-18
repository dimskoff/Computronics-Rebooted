package pl.asie.computronics.oc.driver.rack;

import li.cil.oc.api.Network;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.ComponentConnectableRackMountableEnvironment;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import pl.asie.computronics.item.rack.ItemRackLightBoard;
import pl.asie.computronics.oc.OCData;

import java.util.Map;

/**
 * Rack light board, component "light_board".
 * Persisted / client-synced keys (CUSTOM_DATA, read by RackMountableRenderer):
 * {@code mode} (1..5), {@code c_N} (RGB int), {@code a_N} (boolean), N = 0..41.
 */
public class EnvironmentRackLightBoard extends ComponentConnectableRackMountableEnvironment {
    public static final int MAX_LIGHTS = 42;

    private final EnvironmentHost host;
    private final int[] colors = new int[MAX_LIGHTS];
    private final boolean[] active = new boolean[MAX_LIGHTS];
    private int mode;

    public EnvironmentRackLightBoard(EnvironmentHost host) {
        this(host, 1);
    }

    public EnvironmentRackLightBoard(EnvironmentHost host, int initialMode) {
        this.host = host;
        this.mode = clampMode(initialMode);
        setNode(Network.newNode(this, Visibility.Network)
                .withComponent("light_board", Visibility.Network)
                .withConnector()
                .create());
    }

    private static int clampMode(int mode) {
        return Math.max(1, Math.min(5, mode));
    }

    private int maxLights() {
        return ItemRackLightBoard.getLightCount(mode);
    }

    /** Lights are 1-based (original Computronics API). */
    private int checkIndex(Arguments args, int n) {
        int index = args.checkInteger(n);
        if (index < 1 || index > maxLights()) {
            throw new IllegalArgumentException("index out of range");
        }
        return index - 1;
    }

    @Callback(doc = "function(index:number, color:number):boolean -- Sets the RGB color of the specified light (1-N).")
    public Object[] setColor(Context context, Arguments args) {
        int idx = checkIndex(args, 0);
        int color = args.checkInteger(1) & 0xFFFFFF;
        boolean changed = colors[idx] != color;
        colors[idx] = color;
        if (changed) markRackDirty();
        return new Object[]{true};
    }

    @Callback(direct = true, doc = "function(index:number):number -- Returns the RGB color of the specified light.")
    public Object[] getColor(Context context, Arguments args) {
        return new Object[]{colors[checkIndex(args, 0)]};
    }

    @Callback(doc = "function(index:number, active:boolean):boolean -- Turns the specified light on or off.")
    public Object[] setActive(Context context, Arguments args) {
        int idx = checkIndex(args, 0);
        boolean state = args.checkBoolean(1);
        boolean changed = active[idx] != state;
        active[idx] = state;
        if (changed) markRackDirty();
        return new Object[]{true};
    }

    @Callback(direct = true, doc = "function(index:number):boolean -- Returns whether the specified light is on.")
    public Object[] isActive(Context context, Arguments args) {
        return new Object[]{active[checkIndex(args, 0)]};
    }

    @Callback(doc = "function(colors:table):boolean -- Sets the colors of all lights from a table {[index]=color}.")
    public Object[] setColors(Context context, Arguments args) {
        Map<?, ?> map = args.checkTable(0);
        int max = maxLights();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (e.getKey() instanceof Number k && e.getValue() instanceof Number v) {
                int i = k.intValue() - 1;
                if (i >= 0 && i < max) {
                    colors[i] = v.intValue() & 0xFFFFFF;
                    active[i] = true;
                }
            }
        }
        markRackDirty();
        return new Object[]{true};
    }

    @Callback(direct = true, doc = "function():number -- Returns the number of lights in the current mode.")
    public Object[] getLightCount(Context context, Arguments args) {
        return new Object[]{maxLights()};
    }

    @Callback(doc = "function():boolean -- Turns off all lights and resets their colors.")
    public Object[] clear(Context context, Arguments args) {
        for (int i = 0; i < MAX_LIGHTS; i++) {
            colors[i] = 0;
            active[i] = false;
        }
        markRackDirty();
        return new Object[]{true};
    }

    @Callback(doc = "function(mode:number):number -- Sets the display mode (1: 4, 2: 5, 3: 10, 4: 12, 5: 42 lights).")
    public Object[] setMode(Context context, Arguments args) {
        setModeInternal(args.checkInteger(0));
        return new Object[]{mode};
    }

    @Callback(direct = true, doc = "function():number -- Returns the current display mode.")
    public Object[] getMode(Context context, Arguments args) {
        return new Object[]{mode};
    }

    private void setModeInternal(int newMode) {
        newMode = clampMode(newMode);
        if (newMode == mode) return;
        mode = newMode;
        // Keep the item in sync so the mode survives removing the board from the rack.
        if (host instanceof Rack rack) {
            int index = rack.indexOfMountable(this);
            if (index >= 0) {
                ItemStack stack = rack.getItem(index);
                if (!stack.isEmpty()) ItemRackLightBoard.setMode(stack, mode);
                rack.setChanged();
            }
        }
        markRackDirty();
    }

    private void markRackDirty() {
        if (host instanceof Rack rack) {
            int index = rack.indexOfMountable(this);
            if (index >= 0) rack.markChanged(index);
        } else if (host != null) {
            host.markChanged();
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
                DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Display,
                DeviceInfo.DeviceAttribute.Description, "Rack Light Board",
                DeviceInfo.DeviceAttribute.Vendor, "Computronics",
                DeviceInfo.DeviceAttribute.Product, "LED-Board-" + maxLights()
        );
    }

    @Override
    public boolean onActivate(Player player, InteractionHand hand, ItemStack stack, float hitX, float hitY) {
        if (!player.level().isClientSide()) {
            setModeInternal((mode % 5) + 1);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5f, 1.2f);
            player.displayClientMessage(Component.translatable("message.computronics.light_board.mode", mode, maxLights()), true);
        }
        return true;
    }

    private void readState(CompoundTag tag) {
        if (tag.contains("mode")) {
            mode = clampMode(tag.getInt("mode"));
        }
        for (int i = 0; i < MAX_LIGHTS; i++) {
            if (tag.contains("c_" + i)) colors[i] = tag.getInt("c_" + i) & 0xFFFFFF;
            if (tag.contains("a_" + i)) active[i] = tag.getBoolean("a_" + i);
        }
    }

    private void writeState(CompoundTag tag) {
        tag.putInt("mode", mode);
        for (int i = 0; i < MAX_LIGHTS; i++) {
            tag.putInt("c_" + i, colors[i]);
            tag.putBoolean("a_" + i, active[i]);
        }
    }

    @Override
    public void loadData(DataComponentHolder holder) throws UnrecoverablePersistanceException {
        super.loadData(holder);
        readState(OCData.read(holder));
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        super.saveData(holder);
        OCData.write(holder, this::writeState);
    }

    @Override
    public void describeForClient(MutableDataComponentHolder holder) {
        // Only the visual state is needed on the client.
        OCData.write(holder, this::writeState);
    }
}
