package pl.asie.computronics.block.entity;

import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.BlockEntityEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class TileEntityComputronics extends BlockEntityEnvironment implements SidedEnvironment {

    public TileEntityComputronics(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public Node sidedNode(Direction side) {
        return node();
    }

    @Override
    public boolean canConnect(Direction side) {
        return true;
    }

    public void tickServer(Level level, BlockPos pos, BlockState state) {
        if (node != null && node.network() == null) {
            li.cil.oc.api.Network.joinOrCreateNetwork(this);
        }
    }

    protected Object[] result(Object... args) {
        if (args == null || args.length == 0) {
            return new Object[0];
        }
        return args;
    }
}
