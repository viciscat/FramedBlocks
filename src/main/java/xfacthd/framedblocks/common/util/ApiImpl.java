package xfacthd.framedblocks.common.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.IForgeRegistry;
import xfacthd.framedblocks.FramedBlocks;
import xfacthd.framedblocks.api.FramedBlocksAPI;
import xfacthd.framedblocks.api.block.FramedBlockEntity;
import xfacthd.framedblocks.api.data.CamoContainer;
import xfacthd.framedblocks.client.util.ClientConfig;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.blockentity.FramedDoubleBlockEntity;
import xfacthd.framedblocks.common.data.camo.CamoFactories;

@SuppressWarnings("unused")
public class ApiImpl implements FramedBlocksAPI
{
    @Override
    public String modid() { return FramedBlocks.MODID; }

    @Override
    public BlockEntityType<FramedBlockEntity> defaultBlockEntity() { return FBContent.blockEntityTypeFramedBlock.get(); }

    @Override
    public BlockState defaultModelState() { return FBContent.blockFramedCube.get().defaultBlockState(); }

    @Override
    public CreativeModeTab defaultCreativeTab() { return FramedBlocks.FRAMED_TAB; }

    @Override
    public boolean isFramedHammer(ItemStack stack) { return stack.getItem() == FBContent.itemFramedHammer.get(); }

    @Override
    public boolean isFramedDoubleBlockEntity(FramedBlockEntity be) { return be instanceof FramedDoubleBlockEntity; }

    @Override
    public boolean areBlocksFireproof() { return CommonConfig.fireproofBlocks; }

    @Override
    public boolean detailedCullingEnabled() { return ClientConfig.detailedCulling; }

    @Override
    public boolean allowBlockEntities() { return ServerConfig.allowBlockEntities; }

    @Override
    public ResourceLocation getCamoContainerFactoryRegistryName() { return FBContent.CAMO_CONTAINER_FACTORY_REG_NAME; }

    @Override
    public IForgeRegistry<CamoContainer.Factory> getCamoContainerFactoryRegistry()
    {
        return FBContent.CAMO_CONTAINER_FACTORY_REGISTRY.get();
    }

    @Override
    public void registerCamoContainerFactory(Item item, CamoContainer.Factory factory)
    {
        CamoFactories.registerCamoFactory(item, factory);
    }

    @Override
    public CamoContainer.Factory getCamoContainerFactory(ItemStack stack) { return CamoFactories.getFactory(stack); }
}