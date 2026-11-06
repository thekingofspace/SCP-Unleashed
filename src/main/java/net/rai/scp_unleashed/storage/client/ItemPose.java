package net.rai.scp_unleashed.storage.client;

import net.minecraft.world.item.ItemDisplayContext;

public record ItemPose(Placement Flat, Placement Solid, ItemAnchor Anchor, ItemDisplayContext Display) {
    public static ItemPose resting(Placement Flat, Placement Solid) {
        return new ItemPose(Flat, Solid, ItemAnchor.Bottom, ItemDisplayContext.FIXED);
    }

    public static ItemPose resting(Placement Both) {
        return resting(Both, Both);
    }

    public static ItemPose hanging(Placement Flat, Placement Solid) {
        return new ItemPose(Flat, Solid, ItemAnchor.Top, ItemDisplayContext.FIXED);
    }

    public static ItemPose hanging(Placement Both) {
        return hanging(Both, Both);
    }

    public Placement choose(boolean BlockLike) {
        return BlockLike ? Solid : Flat;
    }

    public ItemPose withFlat(Placement Wanted) {
        return new ItemPose(Wanted, Solid, Anchor, Display);
    }

    public ItemPose withSolid(Placement Wanted) {
        return new ItemPose(Flat, Wanted, Anchor, Display);
    }

    public ItemPose withAnchor(ItemAnchor Wanted) {
        return new ItemPose(Flat, Solid, Wanted, Display);
    }

    public ItemPose withDisplay(ItemDisplayContext Wanted) {
        return new ItemPose(Flat, Solid, Anchor, Wanted);
    }
}
