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

public class EnvironmentRackKVM extends ComponentConnectableRackMountableEnvironment {
    private final EnvironmentHost host;
    private int selectedServer = 0;

    public EnvironmentRackKVM(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network)
                .withComponent("rack_kvm", Visibility.Network)
                .create());
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
                DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Bridge,
                DeviceInfo.DeviceAttribute.Description, "Rack KVM Switch Module",
                DeviceInfo.DeviceAttribute.Vendor, "Computronics",
                DeviceInfo.DeviceAttribute.Product, "RackKVM-4Port"
        );
    }

    @Callback(direct = true, doc = "function():number -- Gets index of currently routed server slot (0-3).")
    public Object[] getSelectedServer(Context context, Arguments args) {
        return new Object[]{selectedServer};
    }

    @Callback(doc = "function(slot:number):boolean -- Routes KVM focus to the server at slot (0-3).")
    public Object[] setSelectedServer(Context context, Arguments args) {
        int slot = args.checkInteger(0);
        if (slot >= 0 && slot < 4) {
            this.selectedServer = slot;
            if (host instanceof Rack rack) {
                rack.markChanged(rack.indexOfMountable(this));
            }
            if (node() != null) {
                node().sendToReachable("computer.signal", "rack_kvm_switch", selectedServer);
            }
            return new Object[]{true};
        }
        return new Object[]{false, "slot out of range (0-3)"};
    }

    @Callback(direct = true, doc = "function():number -- Gets total channel capacity (4).")
    public Object[] getChannelCount(Context context, Arguments args) {
        return new Object[]{4};
    }

    @Override
    public boolean onActivate(Player player, InteractionHand hand, ItemStack stack, float hitX, float hitY) {
        if (!player.level().isClientSide()) {
            selectedServer = (selectedServer + 1) % 4;
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.7f, 1.2f + (selectedServer * 0.1f));

            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    String.format("§b[Rack KVM] §fRouted peripheral focus to §aServer Channel #%d (Slot %d)", selectedServer + 1, selectedServer)), true);

            if (host instanceof Rack rack) {
                rack.markChanged(rack.indexOfMountable(this));
            }
            if (node() != null) {
                node().sendToReachable("computer.signal", "rack_kvm_switch", selectedServer);
            }
        }
        return true;
    }

    @Override
    public void loadData(CompoundTag tag, HolderLookup.Provider registries) throws li.cil.oc.api.UnrecoverablePersistanceException {
        super.loadData(tag, registries);
        if (tag.contains("selectedServer")) {
            selectedServer = tag.getInt("selectedServer");
        }
    }

    @Override
    public void saveData(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveData(tag, registries);
        tag.putInt("selectedServer", selectedServer);
    }
}
