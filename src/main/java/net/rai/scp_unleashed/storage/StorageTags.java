package net.rai.scp_unleashed.storage;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;

public final class StorageTags {
    public static final TagKey<Item> NoPlatform = tag("storage/no_platform");
    public static final TagKey<Item> Large = tag("storage/large");
    public static final TagKey<Item> Spin = tag("storage/spin");
    public static final TagKey<Item> Floating = tag("storage/float");
    public static final TagKey<Item> TallOnly = tag("storage/tall_only");
    public static final TagKey<Item> Storable = tag("storage/storable");

    private StorageTags() {
    }

    private static TagKey<Item> tag(String Path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path));
    }

    public static boolean isStorable(ItemStack Stack) {
        return !Stack.isEmpty() && Stack.is(Storable);
    }
}
