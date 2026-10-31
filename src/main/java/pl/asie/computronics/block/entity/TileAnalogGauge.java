package pl.asie.computronics.block.entity;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import pl.asie.computronics.init.ModBlockEntities;

public class TileAnalogGauge extends TileEntityComputronics {
    private double value = 0.0;
    private String label = "";
    private int color = 0xFFFFFF;

    public TileAnalogGauge(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANALOG_GAUGE.get(), pos, state);
        node = Network.newNode(this, Visibility.Network)
                .withComponent("analog_gauge", Visibility.Network)
                .create();
    }

    @Callback(direct = true, doc = "function():number -- Gets the current dial gauge value (0.0 - 1.0).")
    public Object[] getValue(Context context, Arguments args) {
        return result(value);
    }

    @Callback(doc = "function(val:number):number -- Sets the dial gauge value (0.0 - 1.0).")
    public Object[] setValue(Context context, Arguments args) {
        double val = args.checkDouble(0);
        this.value = Math.max(0.0, Math.min(val, 1.0));
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return result(this.value);
    }

    @Callback(direct = true, doc = "function():string -- Gets the custom dial gauge label.")
    public Object[] getLabel(Context context, Arguments args) {
        return result(label);
    }

    @Callback(doc = "function(text:string):string -- Sets the custom dial gauge label.")
    public Object[] setLabel(Context context, Arguments args) {
        this.label = args.checkString(0);
        setChanged();
        return result(this.label);
    }

    @Callback(direct = true, doc = "function():number -- Gets the gauge color.")
    public Object[] getColor(Context context, Arguments args) {
        return result(color);
    }

    @Callback(doc = "function(color:number):number -- Sets the gauge color in RGB.")
    public Object[] setColor(Context context, Arguments args) {
        this.color = args.checkInteger(0);
        setChanged();
        return result(this.color);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("value", value);
        tag.putString("label", label);
        tag.putInt("color", color);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.value = tag.getDouble("value");
        this.label = tag.getString("label");
        this.color = tag.contains("color") ? tag.getInt("color") : 0xFFFFFF;
    }
}
