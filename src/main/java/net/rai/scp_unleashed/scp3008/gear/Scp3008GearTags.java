package net.rai.scp_unleashed.scp3008.gear;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class Scp3008GearTags {
    public static final TagKey<Block> Liftable = block("scp3008_liftable");
    public static final TagKey<Block> Multiblock = block("scp3008_multiblock");
    public static final TagKey<Block> Unbreakable = block("scp3008_unbreakable");
    public static final TagKey<Block> TableLegs = block("scp3008_salvage/table_legs");
    public static final TagKey<Block> CabinetDoors = block("scp3008_salvage/cabinet_doors");
    public static final TagKey<Block> Warehouse = block("scp3008_furniture/warehouse");
    public static final TagKey<Block> FloorStanding = block("scp3008_floor_standing");
    public static final TagKey<Item> SpearTips = TagKey.create(Registries.ITEM, Scp3008GearRegistry.location("scp3008_gear/spear_tips"));

    private Scp3008GearTags() {
    }

    private static TagKey<Block> block(String Path) {
        return TagKey.create(Registries.BLOCK, Scp3008GearRegistry.location(Path));
    }
}
