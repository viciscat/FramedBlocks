package xfacthd.framedblocks.common.blockentity.special;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.crafting.*;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.menu.FramingSawMenu;
import xfacthd.framedblocks.common.capability.EntityAwareEnergyStorage;
import xfacthd.framedblocks.common.capability.ExternalItemHandler;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class PoweredFramingSawBlockEntity extends BlockEntity
{
    public static final int ENERGY_CAPACITY = 5000;
    public static final int ENERGY_MAX_INSERT = 250;
    public static final int ENERGY_CONSUMPTION = 50;
    private static final boolean INSERT_ENERGY_DEBUG = true;
    private static final long ACTIVE_TIMEOUT = 40;
    public static final int MAX_PROGRESS = 30;

    private final ItemStackHandler itemHandler = new ItemStackHandler(FramingSawMenu.SLOT_RESULT + 1)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            PoweredFramingSawBlockEntity.this.onContentsChanged(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack)
        {
            return PoweredFramingSawBlockEntity.this.isValidItem(slot, stack);
        }
    };
    private final IItemHandler externalItemHandler = new ExternalItemHandler(itemHandler)
    {
        @Override
        protected boolean canExtract(int slot)
        {
            return slot == FramingSawMenu.SLOT_RESULT;
        }
    };
    private final RecipeWrapper container = new RecipeWrapper(itemHandler);
    private final EntityAwareEnergyStorage energyStorage = new EntityAwareEnergyStorage(
            ENERGY_CAPACITY, ENERGY_MAX_INSERT, 0, this
    );
    private FramingSawRecipeCache cache = null;
    private ResourceLocation selectedRecipeId = null;
    private RecipeHolder<FramingSawRecipe> selectedRecipe = null;
    private boolean active = false;
    private long lastActive = 0;
    private boolean recipeSatisfied = false;
    private FramingSawRecipeMatchResult matchResult = null;
    private FramingSawRecipeCalculation calculation = null;
    private int outputCount = 0;
    private int progress = 0;
    private boolean needSaving = false;
    private boolean inhibitUpdate = false;
    private boolean internalAccess = false;

    public PoweredFramingSawBlockEntity(BlockPos pPos, BlockState pBlockState)
    {
        super(FBContent.BE_TYPE_POWERED_FRAMING_SAW.value(), pPos, pBlockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PoweredFramingSawBlockEntity be)
    {
        if (!FMLEnvironment.production && INSERT_ENERGY_DEBUG)
        {
            be.energyStorage.receiveEnergy(ENERGY_MAX_INSERT, false);
        }

        if ((be.active || level.getGameTime() - be.lastActive > ACTIVE_TIMEOUT) && be.canRun())
        {
            if (!be.active)
            {
                be.active = true;
                level.setBlockAndUpdate(pos, state.setValue(PropertyHolder.ACTIVE, true));
            }

            be.energyStorage.extractEnergyInternal(ENERGY_CONSUMPTION);
            be.progress++;

            if (be.progress >= MAX_PROGRESS)
            {
                be.progress = 0;

                ItemStack result = be.selectedRecipe.value().getResult().copy();
                result.setCount(be.outputCount);
                be.internalAccess = true;
                be.inhibitUpdate = true;
                for (int i = 0; i < be.selectedRecipe.value().getAdditives().size(); i++)
                {
                    int slot = i + FramingSawMenu.SLOT_ADDITIVE_FIRST;
                    be.itemHandler.extractItem(slot, be.calculation.getAdditiveCount(i), false);
                }
                be.inhibitUpdate = false;
                be.itemHandler.extractItem(FramingSawMenu.SLOT_INPUT, be.calculation.getInputCount(), false);
                be.itemHandler.insertItem(FramingSawMenu.SLOT_RESULT, result, false);
                be.internalAccess = false;
            }
        }
        else if (be.active)
        {
            be.active = false;
            level.setBlockAndUpdate(pos, state.setValue(PropertyHolder.ACTIVE, false));
            be.lastActive = level.getGameTime();

            if (!be.recipeSatisfied)
            {
                be.progress = 0;
            }
        }

        if (be.needSaving)
        {
            be.setChanged();
            be.needSaving = false;
        }
    }

    private boolean canRun()
    {
        if (selectedRecipe == null || !recipeSatisfied) return false;
        if (energyStorage.getEnergyStored() < ENERGY_CONSUMPTION) return false;

        ItemStack output = itemHandler.getStackInSlot(FramingSawMenu.SLOT_RESULT);
        if (!output.isEmpty() && output.getItem() != selectedRecipe.value().getResult().getItem()) return false;
        if (output.getCount() + outputCount > output.getMaxStackSize()) return false;

        return true;
    }

    private void checkRecipeSatisfied()
    {
        if (selectedRecipe != null)
        {
            matchResult = selectedRecipe.value().matchWithResult(container, level);
            recipeSatisfied = matchResult.success();
        }
        else
        {
            matchResult = null;
            recipeSatisfied = false;
        }

        if (recipeSatisfied)
        {
            calculation = selectedRecipe.value().makeCraftingCalculation(container, false);
            outputCount = calculation.getOutputCount();
        }
        else
        {
            calculation = null;
            outputCount = 0;
            progress = 0;
        }
    }

    private void onContentsChanged(int slot)
    {
        needSaving = true;
        if (slot != FramingSawMenu.SLOT_RESULT && !inhibitUpdate)
        {
            checkRecipeSatisfied();
        }
    }

    private boolean isValidItem(int slot, ItemStack stack)
    {
        if (slot == FramingSawMenu.SLOT_INPUT)
        {
            return cache.getMaterialValue(stack.getItem()) > 0;
        }
        else if (slot < FramingSawMenu.SLOT_RESULT)
        {
            if (selectedRecipe != null)
            {
                int idx = slot - FramingSawMenu.SLOT_ADDITIVE_FIRST;
                List<FramingSawRecipeAdditive> additives = selectedRecipe.value().getAdditives();
                if (!additives.isEmpty() && idx < additives.size())
                {
                    return additives.get(idx).ingredient().test(stack);
                }
                return false;
            }
            return true;
        }
        else if (slot == FramingSawMenu.SLOT_RESULT)
        {
            return internalAccess;
        }
        throw new IllegalArgumentException("Invalid slot: " + slot);
    }

    public void selectRecipe(RecipeHolder<FramingSawRecipe> recipe)
    {
        ResourceLocation lastId = selectedRecipeId;
        selectedRecipe = recipe;
        selectedRecipeId = recipe == null ? null : recipe.id();
        checkRecipeSatisfied();
        if (!Objects.equals(lastId, selectedRecipeId))
        {
            needSaving = true;
        }
    }

    public RecipeHolder<FramingSawRecipe> getSelectedRecipe()
    {
        return selectedRecipe;
    }

    public FramingSawRecipeMatchResult getMatchResult()
    {
        return matchResult;
    }

    public int getProgress()
    {
        return progress;
    }

    public ItemStackHandler getItemHandler()
    {
        return itemHandler;
    }

    public IItemHandler getExternalItemHandler()
    {
        return externalItemHandler;
    }

    public IEnergyStorage getEnergyStorage()
    {
        return energyStorage;
    }

    public int getEnergy()
    {
        return energyStorage.getEnergyStored();
    }

    public void dropContents(Consumer<ItemStack> dropper)
    {
        inhibitUpdate = true;
        for (int i = 0; i < itemHandler.getSlots(); i++)
        {
            dropper.accept(itemHandler.getStackInSlot(i));
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
        inhibitUpdate = false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onLoad()
    {
        super.onLoad();
        //noinspection ConstantConditions
        cache = FramingSawRecipeCache.get(level.isClientSide());
        if (selectedRecipeId != null && !level.isClientSide())
        {
            RecipeHolder<FramingSawRecipe> recipe = (RecipeHolder<FramingSawRecipe>) level.getRecipeManager()
                    .byKey(selectedRecipeId)
                    .filter(h -> h.value() instanceof FramingSawRecipe)
                    .orElse(null);
            selectRecipe(recipe);
        }
    }

    public boolean isUsableByPlayer(Player player)
    {
        //noinspection ConstantConditions
        if (level.getBlockEntity(worldPosition) != this)
        {
            return false;
        }
        return !(player.distanceToSqr((double)worldPosition.getX() + 0.5D, (double)worldPosition.getY() + 0.5D, (double)worldPosition.getZ() + 0.5D) > 64.0D);
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        if (selectedRecipe != null)
        {
            tag.putString("recipe", selectedRecipe.id().toString());
        }
        tag.put("inventory", itemHandler.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        if (tag.contains("recipe"))
        {
            selectedRecipeId = new ResourceLocation(tag.getString("recipe"));
        }
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
    }
}
