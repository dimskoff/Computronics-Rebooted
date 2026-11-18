package pl.asie.computronics.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import pl.asie.computronics.block.entity.TileEntityComputronics;

import javax.annotation.Nullable;

/**
 * Base class of every Computronics machine block: renders as a regular model, ticks its
 * {@link TileEntityComputronics} on the server, and correctly rotates/mirrors any
 * {@code facing} / {@code horizontal_facing} property (structures, schematics, etc.).
 */
public abstract class BlockComputronics extends Block implements EntityBlock {

    public BlockComputronics(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof TileEntityComputronics compBe) {
                compBe.tickServer(lvl, pos, st);
            }
        };
    }

    @Nullable
    private static DirectionProperty facingProperty(BlockState state) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return BlockStateProperties.HORIZONTAL_FACING;
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return BlockStateProperties.FACING;
        }
        return null;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        DirectionProperty prop = facingProperty(state);
        return prop == null ? state : state.setValue(prop, rotation.rotate(state.getValue(prop)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        DirectionProperty prop = facingProperty(state);
        if (prop == null) {
            return state;
        }
        Direction facing = state.getValue(prop);
        return state.rotate(mirror.getRotation(facing));
    }
}
