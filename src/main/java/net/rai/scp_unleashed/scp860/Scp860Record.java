package net.rai.scp_unleashed.scp860;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class Scp860Record implements INBTSerializable<CompoundTag> {
    public boolean Active;
    @Nullable
    public UUID Session;
    public ListTag Items = new ListTag();
    public ListTag Curios = new ListTag();
    @Nullable
    public ResourceKey<Level> ReturnDimension;
    public Vec3 ReturnPos = Vec3.ZERO;
    public float ReturnYaw;
    @Nullable
    public ResourceKey<Level> DoorDimension;
    @Nullable
    public BlockPos DoorPos;
    public boolean Died;
    public int DeathLevels;
    public float DeathProgress;
    public int DeathTotal;
    public long Entered;
    @Nullable
    public ResourceKey<Level> SafeDimension;
    public Vec3 SafePos = Vec3.ZERO;
    public int Restores;

    public boolean hasReturn() {
        return ReturnDimension != null;
    }

    public boolean hasSafe() {
        return SafeDimension != null;
    }

    public int itemCount() {
        return Items.size() + Curios.size();
    }

    public void clearTrip() {
        Active = false;
        Session = null;
        Items = new ListTag();
        Curios = new ListTag();
        Died = false;
        DeathLevels = 0;
        DeathProgress = 0.0f;
        DeathTotal = 0;
    }

    private static void putSpot(CompoundTag Saved, String Prefix, @Nullable ResourceKey<Level> Dimension, Vec3 Pos) {
        if (Dimension == null) {
            return;
        }
        Saved.putString(Prefix + "Dimension", Dimension.location().toString());
        Saved.putDouble(Prefix + "X", Pos.x);
        Saved.putDouble(Prefix + "Y", Pos.y);
        Saved.putDouble(Prefix + "Z", Pos.z);
    }

    @Nullable
    private static ResourceKey<Level> dimension(CompoundTag Saved, String Prefix) {
        if (!Saved.contains(Prefix + "Dimension")) {
            return null;
        }
        ResourceLocation Where = ResourceLocation.tryParse(Saved.getString(Prefix + "Dimension"));
        return Where == null ? null : ResourceKey.create(Registries.DIMENSION, Where);
    }

    private static Vec3 pos(CompoundTag Saved, String Prefix) {
        return new Vec3(Saved.getDouble(Prefix + "X"), Saved.getDouble(Prefix + "Y"), Saved.getDouble(Prefix + "Z"));
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider Lookup) {
        CompoundTag Saved = new CompoundTag();
        Saved.putBoolean("Active", Active);
        if (Session != null) {
            Saved.putUUID("Session", Session);
        }
        Saved.put("Items", Items.copy());
        Saved.put("Curios", Curios.copy());
        putSpot(Saved, "Return", ReturnDimension, ReturnPos);
        Saved.putFloat("ReturnYaw", ReturnYaw);
        if (DoorDimension != null && DoorPos != null) {
            Saved.putString("DoorDimension", DoorDimension.location().toString());
            Saved.put("DoorPos", NbtUtils.writeBlockPos(DoorPos));
        }
        Saved.putBoolean("Died", Died);
        Saved.putInt("DeathLevels", DeathLevels);
        Saved.putFloat("DeathProgress", DeathProgress);
        Saved.putInt("DeathTotal", DeathTotal);
        Saved.putLong("Entered", Entered);
        putSpot(Saved, "Safe", SafeDimension, SafePos);
        Saved.putInt("Restores", Restores);
        return Saved;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider Lookup, CompoundTag Saved) {
        Active = Saved.getBoolean("Active");
        Session = Saved.hasUUID("Session") ? Saved.getUUID("Session") : null;
        Items = Saved.getList("Items", Tag.TAG_COMPOUND);
        Curios = Saved.getList("Curios", Tag.TAG_COMPOUND);
        ReturnDimension = dimension(Saved, "Return");
        ReturnPos = pos(Saved, "Return");
        ReturnYaw = Saved.getFloat("ReturnYaw");
        DoorDimension = dimension(Saved, "Door");
        DoorPos = NbtUtils.readBlockPos(Saved, "DoorPos").orElse(null);
        Died = Saved.getBoolean("Died");
        DeathLevels = Saved.getInt("DeathLevels");
        DeathProgress = Saved.getFloat("DeathProgress");
        DeathTotal = Saved.getInt("DeathTotal");
        Entered = Saved.getLong("Entered");
        SafeDimension = dimension(Saved, "Safe");
        SafePos = pos(Saved, "Safe");
        Restores = Saved.getInt("Restores");
    }
}
