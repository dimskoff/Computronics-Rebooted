package pl.asie.computronics.block.entity;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import pl.asie.computronics.init.ModBlockEntities;

public class TileAnalogGauge extends TileEntityComputronics {
    public static final int MAX_LABEL_LENGTH = 32;

    private volatile double value = 0.0;
    private volatile String label = "";
    private volatile int color = 0xFFFFFF;

    public TileAnalogGauge(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANALOG_GAUGE.get(), pos, state);
        node = Network.newNode(this, Visibility.Network)
                .withComponent("analog_gauge", Visibility.Network)
                .create();
    }

    public double getGaugeValue() {
        return value;
    }

    public String getGaugeLabel() {
        return label;
    }

    public int getGaugeColor() {
        return color;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Callback(direct = true, doc = "function():number -- Gets the current dial gauge value (0.0 - 1.0).")
    public Object[] getValue(Context context, Arguments args) {
        return result(value);
    }

    @Callback(doc = "function(val:number):number -- Sets the dial gauge value (0.0 - 1.0).")
    public Object[] setValue(Context context, Arguments args) {
        double val = args.checkDouble(0);
        if (Double.isNaN(val)) val = 0.0;
        double clamped = Math.max(0.0, Math.min(val, 1.0));
        if (clamped != this.value) {
            this.value = clamped;
            sync();
        }
        return result(this.value);
    }

    @Callback(direct = true, doc = "function():string -- Gets the custom dial gauge label.")
    public Object[] getLabel(Context context, Arguments args) {
        return result(label);
    }

    @Callback(doc = "function(text:string):string -- Sets the custom dial gauge label (max 32 chars).")
    public Object[] setLabel(Context context, Arguments args) {
        String text = args.checkString(0);
        if (text.length() > MAX_LABEL_LENGTH) text = text.substring(0, MAX_LABEL_LENGTH);
        this.label = text;
        sync();
        return result(this.label);
    }

    @Callback(direct = true, doc = "function():number -- Gets the gauge color.")
    public Object[] getColor(Context context, Arguments args) {
        return result(color);
    }

    @Callback(doc = "function(color:number):number -- Sets the gauge color in RGB.")
    public Object[] setColor(Context context, Arguments args) {
        this.color = args.checkInteger(0) & 0xFFFFFF;
        sync();
        return result(this.color);
    }

    private void writeSyncData(CompoundTag tag) {
        tag.putDouble("value", value);
        tag.putString("label", label);
        tag.putInt("color", color);
    }

    private void readSyncData(CompoundTag tag) {
        this.value = Math.max(0.0, Math.min(tag.getDouble("value"), 1.0));
        this.label = tag.getString("label");
        this.color = tag.contains("color") ? tag.getInt("color") : 0xFFFFFF;
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeSyncData(tag);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readSyncData(tag);
    }

    // --- Client sync (only gauge state, never OC node data) ---

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeSyncData(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        readSyncData(tag);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (tag != null && !tag.isEmpty()) {
            readSyncData(tag);
        }
    }
}
