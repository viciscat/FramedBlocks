package xfacthd.framedblocks.common.data.skippreds.prism;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.api.predicate.SideSkipPredicate;
import xfacthd.framedblocks.common.block.AbstractFramedDoubleBlock;
import xfacthd.framedblocks.common.data.BlockType;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.property.CompoundDirection;
import xfacthd.framedblocks.common.data.property.DirectionAxis;
import xfacthd.framedblocks.common.data.skippreds.HalfDir;

public final class InnerSlopedPrismSkipPredicate implements SideSkipPredicate
{
    @Override
    public boolean test(BlockGetter level, BlockPos pos, BlockState state, BlockState adjState, Direction side)
    {
        CompoundDirection cmpDir = state.getValue(PropertyHolder.FACING_DIR);
        Direction orientation = cmpDir.orientation();
        if (side != orientation)
        {
            return SideSkipPredicate.CTM.test(level, pos, state, adjState, side);
        }

        if (orientation.getAxis() == cmpDir.direction().getAxis())
        {
            return false;
        }

        if (adjState.getBlock() instanceof IFramedBlock block && block.getBlockType() instanceof BlockType type)
        {
            return switch (type)
            {
                case FRAMED_INNER_SLOPED_PRISM -> testAgainstInnerSlopedPrism(
                        level, pos, state, cmpDir, adjState, side
                );
                case FRAMED_INNER_PRISM -> testAgainstInnerPrism(
                        level, pos, state, cmpDir, adjState, side
                );
                case FRAMED_DOUBLE_SLOPED_PRISM -> testAgainstDoubleSlopedPrism(
                        level, pos, state, cmpDir, adjState, side
                );
                case FRAMED_DOUBLE_PRISM -> testAgainstDoublePrism(
                        level, pos, state, cmpDir, adjState, side
                );
                default -> false;
            };
        }

        return false;
    }

    private static boolean testAgainstInnerSlopedPrism(
            BlockGetter level, BlockPos pos, BlockState state, CompoundDirection cmpDir, BlockState adjState, Direction side
    )
    {
        CompoundDirection adjCmpDir = adjState.getValue(PropertyHolder.FACING_DIR);

        if (getTriDir(cmpDir, side).isEqualTo(getTriDir(adjCmpDir, side.getOpposite())))
        {
            return SideSkipPredicate.compareState(level, pos, side, state, adjState);
        }
        return false;
    }

    private static boolean testAgainstInnerPrism(
            BlockGetter level, BlockPos pos, BlockState state, CompoundDirection cmpDir, BlockState adjState, Direction side
    )
    {
        DirectionAxis adjDirAxis = adjState.getValue(PropertyHolder.FACING_AXIS);

        if (getTriDir(cmpDir, side).isEqualTo(InnerPrismSkipPredicate.getTriDir(adjDirAxis, side.getOpposite())))
        {
            return SideSkipPredicate.compareState(level, pos, side, state, adjState);
        }
        return false;
    }

    private static boolean testAgainstDoubleSlopedPrism(
            BlockGetter level, BlockPos pos, BlockState state, CompoundDirection cmpDir, BlockState adjState, Direction side
    )
    {
        Tuple<BlockState, BlockState> states = AbstractFramedDoubleBlock.getStatePair(adjState);
        return testAgainstInnerSlopedPrism(level, pos, state, cmpDir, states.getA(), side);
    }

    private static boolean testAgainstDoublePrism(
            BlockGetter level, BlockPos pos, BlockState state, CompoundDirection cmpDir, BlockState adjState, Direction side
    )
    {
        Tuple<BlockState, BlockState> states = AbstractFramedDoubleBlock.getStatePair(adjState);
        return testAgainstInnerPrism(level, pos, state, cmpDir, states.getA(), side);
    }



    public static HalfDir getTriDir(CompoundDirection cmpDir, Direction side)
    {
        Direction dir = cmpDir.direction();
        Direction orientation = cmpDir.orientation();
        if (dir.getAxis() != orientation.getAxis() && side == orientation)
        {
            return HalfDir.fromDirections(side, dir);
        }
        return HalfDir.NULL;
    }
}
