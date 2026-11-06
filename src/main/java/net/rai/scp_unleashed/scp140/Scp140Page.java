package net.rai.scp_unleashed.scp140;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public record Scp140Page(int Page, @Nullable BlockPos Site, @Nullable ResourceKey<Level> SiteDimension, BlockPos Origin, ResourceKey<Level> OriginDimension,
                         long WrittenAt, long Day) {
    public boolean hasSite() {
        return Site != null && SiteDimension != null;
    }

    public boolean sameRealm() {
        return hasSite() && SiteDimension.equals(OriginDimension);
    }

    public int offsetX() {
        return hasSite() ? Site.getX() - Origin.getX() : 0;
    }

    public int offsetZ() {
        return hasSite() ? Site.getZ() - Origin.getZ() : 0;
    }

    public CompoundTag save() {
        CompoundTag Tag = new CompoundTag();
        Tag.putInt("Page", Page);
        if (hasSite()) {
            Tag.putLong("Site", Site.asLong());
            Tag.putString("SiteDimension", SiteDimension.location().toString());
        }
        Tag.putLong("Origin", Origin.asLong());
        Tag.putString("OriginDimension", OriginDimension.location().toString());
        Tag.putLong("WrittenAt", WrittenAt);
        Tag.putLong("Day", Day);
        return Tag;
    }

    @Nullable
    public static Scp140Page load(CompoundTag Tag) {
        ResourceKey<Level> OriginDimension = dimension(Tag.getString("OriginDimension"));
        if (OriginDimension == null) {
            OriginDimension = Level.OVERWORLD;
        }
        BlockPos Site = Tag.contains("Site") ? BlockPos.of(Tag.getLong("Site")) : null;
        ResourceKey<Level> SiteDimension = Tag.contains("SiteDimension") ? dimension(Tag.getString("SiteDimension")) : null;
        if (Site == null || SiteDimension == null) {
            Site = null;
            SiteDimension = null;
        }
        return new Scp140Page(Tag.getInt("Page"), Site, SiteDimension, BlockPos.of(Tag.getLong("Origin")), OriginDimension, Tag.getLong("WrittenAt"), Tag.getLong("Day"));
    }

    @Nullable
    private static ResourceKey<Level> dimension(String Name) {
        ResourceLocation Location = ResourceLocation.tryParse(Name);
        return Location == null ? null : ResourceKey.create(Registries.DIMENSION, Location);
    }
}
