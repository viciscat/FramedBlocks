package xfacthd.framedblocks.common.block.interactive;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeightedPressurePlateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.api.block.render.FramedBlockRenderProperties;
import xfacthd.framedblocks.api.model.wrapping.statemerger.StateMerger;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.data.BlockType;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

@SuppressWarnings("deprecation")
public class FramedWeightedPressurePlateBlock extends WeightedPressurePlateBlock implements IFramedBlock
{
    public static final StateMerger STATE_MERGER = new WeightedPressurePlateStateMerger();
    private static final Map<BlockType, BlockType> WATERLOGGING_SWITCH = Map.of(
            BlockType.FRAMED_GOLD_PRESSURE_PLATE, BlockType.FRAMED_WATERLOGGABLE_GOLD_PRESSURE_PLATE,
            BlockType.FRAMED_WATERLOGGABLE_GOLD_PRESSURE_PLATE, BlockType.FRAMED_GOLD_PRESSURE_PLATE,
            BlockType.FRAMED_IRON_PRESSURE_PLATE, BlockType.FRAMED_WATERLOGGABLE_IRON_PRESSURE_PLATE,
            BlockType.FRAMED_WATERLOGGABLE_IRON_PRESSURE_PLATE, BlockType.FRAMED_IRON_PRESSURE_PLATE
    );

    private final BlockType type;

    protected FramedWeightedPressurePlateBlock(BlockType type, int maxWeight, BlockSetType blockSet, Properties props)
    {
        super(maxWeight, blockSet, props);
        this.type = type;
        registerDefaultState(defaultBlockState()
                .setValue(FramedProperties.GLOWING, false)
                .setValue(FramedProperties.PROPAGATES_SKYLIGHT, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(FramedProperties.GLOWING, FramedProperties.PROPAGATES_SKYLIGHT);
    }

    @Override
    public final InteractionResult use(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit
    )
    {
        return handleUse(state, level, pos, player, hand, hit);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack)
    {
        tryApplyCamoImmediately(level, pos, placer, stack);
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos)
    {
        return getCamoShadeBrightness(state, level, pos, super.getShadeBrightness(state, level, pos));
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos)
    {
        return state.getValue(FramedProperties.PROPAGATES_SKYLIGHT);
    }

    @Override
    public boolean handleBlockLeftClick(BlockState state, Level level, BlockPos pos, Player player)
    {
        if (player.getMainHandItem().is(FBContent.ITEM_FRAMED_HAMMER.value()))
        {
            if (!level.isClientSide())
            {
                Utils.wrapInStateCopy(level, pos, player, ItemStack.EMPTY, false, false, () ->
                {
                    BlockState newState = getCounterpart().defaultBlockState();
                    level.setBlockAndUpdate(pos, newState);
                });
            }
            return true;
        }
        return false;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder)
    {
        return getCamoDrops(super.getDrops(state, builder), builder);
    }

    @Override
    public boolean doesBlockOccludeBeaconBeam(BlockState state, LevelReader level, BlockPos pos)
    {
        return true;
    }

    @Override
    public BlockType getBlockType()
    {
        return type;
    }

    @Override
    public void initializeClient(Consumer<IClientBlockExtensions> consumer)
    {
        consumer.accept(FramedBlockRenderProperties.INSTANCE);
    }

    protected final Block getCounterpart()
    {
        return FBContent.byType(WATERLOGGING_SWITCH.get(type));
    }



    public static FramedWeightedPressurePlateBlock gold()
    {
        return new FramedWeightedPressurePlateBlock(
                BlockType.FRAMED_GOLD_PRESSURE_PLATE,
                15,
                BlockSetType.GOLD,
                IFramedBlock.createProperties(BlockType.FRAMED_GOLD_PRESSURE_PLATE)
                        .noCollission()
                        .strength(0.5F)
        );
    }

    public static FramedWeightedPressurePlateBlock goldWaterloggable()
    {
        return new FramedWaterloggableWeightedPressurePlateBlock(
                BlockType.FRAMED_WATERLOGGABLE_GOLD_PRESSURE_PLATE,
                15,
                BlockSetType.GOLD,
                IFramedBlock.createProperties(BlockType.FRAMED_WATERLOGGABLE_GOLD_PRESSURE_PLATE)
                        .noCollission()
                        .strength(0.5F)
        );
    }

    public static FramedWeightedPressurePlateBlock iron()
    {
        return new FramedWeightedPressurePlateBlock(
                BlockType.FRAMED_IRON_PRESSURE_PLATE,
                150,
                BlockSetType.IRON,
                IFramedBlock.createProperties(BlockType.FRAMED_IRON_PRESSURE_PLATE)
                        .requiresCorrectToolForDrops()
                        .noCollission()
                        .strength(0.5F)
        );
    }

    public static FramedWeightedPressurePlateBlock ironWaterloggable()
    {
        return new FramedWaterloggableWeightedPressurePlateBlock(
                BlockType.FRAMED_WATERLOGGABLE_IRON_PRESSURE_PLATE,
                150,
                BlockSetType.IRON,
                IFramedBlock.createProperties(BlockType.FRAMED_WATERLOGGABLE_IRON_PRESSURE_PLATE)
                        .requiresCorrectToolForDrops()
                        .noCollission()
                        .strength(0.5F)
        );
    }



    private static final class WeightedPressurePlateStateMerger implements StateMerger
    {
        @Override
        public BlockState apply(BlockState state)
        {
            if (state.hasProperty(BlockStateProperties.WATERLOGGED))
            {
                state = state.setValue(BlockStateProperties.WATERLOGGED, false);
            }

            if (state.getValue(WeightedPressurePlateBlock.POWER) > 1)
            {
                return state.setValue(WeightedPressurePlateBlock.POWER, 1);
            }
            return state.setValue(FramedProperties.GLOWING, false).setValue(FramedProperties.PROPAGATES_SKYLIGHT, false);
        }

        @Override
        public Set<Property<?>> getHandledProperties(Holder<Block> block)
        {
            return Set.of(
                    BlockStateProperties.WATERLOGGED,
                    WeightedPressurePlateBlock.POWER,
                    FramedProperties.GLOWING,
                    FramedProperties.PROPAGATES_SKYLIGHT
            );
        }
    }
}
