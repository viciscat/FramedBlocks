package xfacthd.framedblocks.common.data;

import net.minecraft.core.Holder;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.api.blueprint.BlueprintCopyBehaviour;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.data.blueprint.*;
import xfacthd.framedblocks.common.item.FramedBlueprintItem;

public final class BlueprintBehaviours
{
    public static void register()
    {
        BlueprintCopyBehaviour doubleBlockBehaviour = new DoubleBlockCopyBehaviour();

        FBContent.getRegisteredBlocks()
                .stream()
                .map(Holder::value)
                .filter(IFramedBlock.class::isInstance)
                .filter(b -> ((IFramedBlock) b).getBlockType().isDoubleBlock())
                .forEach(block ->
                {
                    if (block == FBContent.BLOCK_FRAMED_DOUBLE_SLAB.value())
                    {
                        FramedBlueprintItem.registerBehaviour(new DoubleSlabCopyBehaviour(), block);
                    }
                    else if (block == FBContent.BLOCK_FRAMED_DOUBLE_PANEL.value())
                    {
                        FramedBlueprintItem.registerBehaviour(new DoublePanelCopyBehaviour(), block);
                    }
                    else
                    {
                        FramedBlueprintItem.registerBehaviour(doubleBlockBehaviour, block);
                    }
                });

        FramedBlueprintItem.registerBehaviour(
                new DoorCopyBehaviour(),
                FBContent.BLOCK_FRAMED_DOOR.value(),
                FBContent.BLOCK_FRAMED_IRON_DOOR.value()
        );

        FramedBlueprintItem.registerBehaviour(
                new CollapsibleBlockCopyBehaviour(),
                FBContent.BLOCK_FRAMED_COLLAPSIBLE_BLOCK.value()
        );
    }



    private BlueprintBehaviours() { }
}
