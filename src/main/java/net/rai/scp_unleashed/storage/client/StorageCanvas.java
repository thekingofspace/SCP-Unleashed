package net.rai.scp_unleashed.storage.client;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public interface StorageCanvas {
    boolean isBlockLike(StorageLayoutContext Context, ItemStack Shown);

    float halfHeight(StorageLayoutContext Context, ItemStack Shown, ItemDisplayContext Display);

    void drawModel(StorageLayoutContext Context, ModelResourceLocation Location, boolean Centred);

    void drawBlock(StorageLayoutContext Context, BlockState Shown);

    void drawItem(StorageLayoutContext Context, ItemStack Shown, ItemDisplayContext Display);
}
