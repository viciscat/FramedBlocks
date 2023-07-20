package xfacthd.framedblocks.common.block;

import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.api.block.cache.StateCache;
import xfacthd.framedblocks.api.type.IBlockType;
import xfacthd.framedblocks.common.util.DoubleBlockTopInteractionMode;

import java.util.Objects;

public class DoubleBlockStateCache extends StateCache
{
    private final DoubleBlockTopInteractionMode topInteractionMode;
    private final Tuple<BlockState, BlockState> statePair;

    public DoubleBlockStateCache(BlockState state, IBlockType type)
    {
        super(state, type);
        IFramedDoubleBlock block = (IFramedDoubleBlock) state.getBlock();
        this.topInteractionMode = block.calculateTopInteractionMode(state);
        this.statePair = block.calculateBlockPair(state);
    }

    public final DoubleBlockTopInteractionMode getTopInteractionMode()
    {
        return topInteractionMode;
    }

    public final Tuple<BlockState, BlockState> getBlockPair()
    {
        return statePair;
    }

    @Override
    public boolean equals(Object other)
    {
        if (!super.equals(other))
        {
            return false;
        }
        DoubleBlockStateCache that = (DoubleBlockStateCache) other;
        return topInteractionMode == that.topInteractionMode &&
                statePair.getA() == that.statePair.getA() &&
                statePair.getB() == that.statePair.getB();
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), topInteractionMode, statePair.getA(), statePair.getB());
    }
}
