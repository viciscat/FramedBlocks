package xfacthd.framedblocks.common.block.slopeslab;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.*;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.block.*;
import xfacthd.framedblocks.api.shapes.CommonShapes;
import xfacthd.framedblocks.api.shapes.ShapeProvider;
import xfacthd.framedblocks.api.util.*;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.block.AbstractFramedDoubleBlock;
import xfacthd.framedblocks.common.blockentity.doubled.slopeslab.FramedDoubleSlopeSlabBlockEntity;
import xfacthd.framedblocks.common.data.BlockType;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.doubleblock.CamoGetter;
import xfacthd.framedblocks.common.data.doubleblock.SolidityCheck;
import xfacthd.framedblocks.common.data.doubleblock.DoubleBlockTopInteractionMode;

public class FramedDoubleSlopeSlabBlock extends AbstractFramedDoubleBlock
{
    public FramedDoubleSlopeSlabBlock()
    {
        super(BlockType.FRAMED_DOUBLE_SLOPE_SLAB);
        registerDefaultState(defaultBlockState()
                .setValue(PropertyHolder.TOP_HALF, false)
                .setValue(FramedProperties.Y_SLOPE, true)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(
                FramedProperties.FACING_HOR, PropertyHolder.TOP_HALF, BlockStateProperties.WATERLOGGED,
                FramedProperties.Y_SLOPE
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx)
    {
        return PlacementStateBuilder.of(this, ctx)
                .withTargetOrHorizontalFacing()
                .withTop(PropertyHolder.TOP_HALF)
                .withWater()
                .build();
    }

    @Override
    public boolean handleBlockLeftClick(BlockState state, Level level, BlockPos pos, Player player)
    {
        return IFramedBlock.toggleYSlope(state, level, pos, player);
    }

    @Override
    public BlockState rotate(BlockState state, Direction face, Rotation rot)
    {
        if (Utils.isY(face))
        {
            Direction dir = state.getValue(FramedProperties.FACING_HOR);
            return state.setValue(FramedProperties.FACING_HOR, rot.rotate(dir));
        }
        else if (rot != Rotation.NONE)
        {
            return state.cycle(PropertyHolder.TOP_HALF);
        }
        return state;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState rotate(BlockState state, Rotation rot)
    {
        return rotate(state, Direction.UP, rot);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirror)
    {
        return Utils.mirrorFaceBlock(state, mirror);
    }

    @Override
    public Tuple<BlockState, BlockState> calculateBlockPair(BlockState state)
    {
        Direction facing = state.getValue(FramedProperties.FACING_HOR);
        boolean topHalf = state.getValue(PropertyHolder.TOP_HALF);
        boolean ySlope = state.getValue(FramedProperties.Y_SLOPE);

        BlockState defState = FBContent.BLOCK_FRAMED_SLOPE_SLAB.value().defaultBlockState();
        return new Tuple<>(
                defState.setValue(FramedProperties.FACING_HOR, facing)
                        .setValue(PropertyHolder.TOP_HALF, topHalf)
                        .setValue(FramedProperties.TOP, false)
                        .setValue(FramedProperties.Y_SLOPE, ySlope),
                defState.setValue(FramedProperties.FACING_HOR, facing.getOpposite())
                        .setValue(PropertyHolder.TOP_HALF, topHalf)
                        .setValue(FramedProperties.TOP, true)
                        .setValue(FramedProperties.Y_SLOPE, ySlope)
        );
    }

    @Override
    public DoubleBlockTopInteractionMode calculateTopInteractionMode(BlockState state)
    {
        return DoubleBlockTopInteractionMode.SECOND;
    }

    @Override
    public CamoGetter calculateCamoGetter(BlockState state, Direction side, @Nullable Direction edge)
    {
        Direction facing = state.getValue(FramedProperties.FACING_HOR);
        boolean top = state.getValue(PropertyHolder.TOP_HALF);
        Direction dirTwo = top ? Direction.UP : Direction.DOWN;

        if ((side == Direction.UP && top) || (side == facing.getOpposite() && edge == dirTwo))
        {
            return CamoGetter.SECOND;
        }
        else if ((side == Direction.DOWN && !top) || (side == facing && edge == dirTwo))
        {
            return CamoGetter.FIRST;
        }
        else if (side.getAxis() == facing.getClockWise().getAxis() && edge == dirTwo)
        {
            return top ? CamoGetter.SECOND : CamoGetter.FIRST;
        }

        return CamoGetter.NONE;
    }

    @Override
    public SolidityCheck calculateSolidityCheck(BlockState state, Direction side)
    {
        boolean topHalf = state.getValue(PropertyHolder.TOP_HALF);
        if (topHalf && side == Direction.UP)
        {
            return SolidityCheck.SECOND;
        }
        else if (!topHalf && side == Direction.DOWN)
        {
            return SolidityCheck.FIRST;
        }
        return SolidityCheck.NONE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new FramedDoubleSlopeSlabBlockEntity(pos, state);
    }

    @Override
    public BlockState getItemModelSource()
    {
        return defaultBlockState();
    }



    public static ShapeProvider generateShapes(ImmutableList<BlockState> states)
    {
        ImmutableMap.Builder<BlockState, VoxelShape> builder = ImmutableMap.builder();

        for (BlockState state : states)
        {
            builder.put(state, CommonShapes.SLAB.get(state.getValue(PropertyHolder.TOP_HALF)));
        }

        return ShapeProvider.of(builder.build());
    }
}
