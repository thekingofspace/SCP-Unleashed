package net.rai.scp_unleashed.faction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record FactionMember(UUID Id, int Site, String Role, ResourceKey<Level> Dimension, BlockPos LastSeen) {
    public FactionMember seenAt(ResourceKey<Level> Where, BlockPos Pos) {
        return new FactionMember(Id, Site, Role, Where, Pos.immutable());
    }

    public CompoundTag save() {
        CompoundTag Saved = new CompoundTag();
        Saved.putUUID("Id", Id);
        Saved.putInt("Site", Site);
        Saved.putString("Role", Role);
        Saved.putString("Dimension", Dimension.location().toString());
        Saved.putLong("Seen", LastSeen.asLong());
        return Saved;
    }

    @Nullable
    public static FactionMember load(CompoundTag Saved) {
        ResourceLocation Place = ResourceLocation.tryParse(Saved.getString("Dimension"));
        if (Place == null || !Saved.hasUUID("Id")) {
            return null;
        }
        return new FactionMember(Saved.getUUID("Id"), Saved.getInt("Site"), Saved.getString("Role"), ResourceKey.create(Registries.DIMENSION, Place),
                BlockPos.of(Saved.getLong("Seen")));
    }
}
