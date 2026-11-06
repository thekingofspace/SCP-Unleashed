package net.rai.scp_unleashed.scp058;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;
import net.rai.scp_unleashed.archive.investigation.StartCondition;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record Scp058CowCondition() implements StartCondition {
    public static final String TypeName = "scp058_cow";
    public static final MapCodec<Scp058CowCondition> ConditionCodec = MapCodec.unit(Scp058CowCondition::new);
    public static final String CowKey = "Scp058Cow";
    public static final int SearchRadius = 4;
    public static final int SearchVertical = 3;
    public static final int SearchAttempts = 6;

    @Override
    public String getTypeName() {
        return TypeName;
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        return List.of(
                Component.translatable("archive.scp_unleashed.notes.scp058.0"),
                Component.translatable("archive.scp_unleashed.notes.scp058.1"),
                Component.translatable("archive.scp_unleashed.notes.scp058.2"));
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        CompoundTag State = Case.getState();
        if (!State.contains(CowKey)) {
            return Optional.empty();
        }
        CompoundTag Cow = State.getCompound(CowKey);
        ServerLevel World = Reader.serverLevel();
        BlockPos Pos = BlockPos.containing(spot(Cow));
        if (!LeadPlanner.inDimension(Reader, Cow.getString("Dimension")) || !World.isLoaded(Pos)) {
            forget(Case);
            return Optional.empty();
        }
        Optional<BlockPos> Room = roomFor(World, Pos, Reader.getRandom());
        if (Room.isEmpty()) {
            forget(Case);
            return Optional.empty();
        }
        return Optional.of(new Finding(Room.get(), true));
    }

    public static boolean isMarked(Investigation Case) {
        return Case.getState().contains(CowKey);
    }

    public static void mark(Investigation Case, LivingEntity Carcass) {
        CompoundTag Cow = new CompoundTag();
        Cow.putDouble("X", Carcass.getX());
        Cow.putDouble("Y", Carcass.getY());
        Cow.putDouble("Z", Carcass.getZ());
        Cow.putFloat("Yaw", Carcass.getYRot());
        Cow.putString("Dimension", LeadPlanner.dimensionOf(Carcass.level()));
        Cow.putUUID("Id", Carcass.getUUID());
        Case.getState().put(CowKey, Cow);
        Case.markDirty();
    }

    public static void forget(Investigation Case) {
        if (Case.getState().contains(CowKey)) {
            Case.getState().remove(CowKey);
            Case.markDirty();
        }
    }

    public static Vec3 spot(CompoundTag Cow) {
        return new Vec3(Cow.getDouble("X"), Cow.getDouble("Y"), Cow.getDouble("Z"));
    }

    @Nullable
    public static UUID carcass(CompoundTag Cow) {
        return Cow.hasUUID("Id") ? Cow.getUUID("Id") : null;
    }

    public static boolean roomy(ServerLevel World, Vec3 Spot) {
        AABB Box = Scp058Registry.Heart.get().getDimensions().makeBoundingBox(Spot);
        return World.noCollision(Box) && !World.containsAnyLiquid(Box);
    }

    public static Optional<BlockPos> roomFor(ServerLevel World, BlockPos Pos, RandomSource Random) {
        if (roomy(World, Vec3.atBottomCenterOf(Pos))) {
            return Optional.of(Pos);
        }
        for (int Attempt = 0; Attempt < SearchAttempts; Attempt++) {
            Optional<BlockPos> Found = LeadPlanner.standableNear(World, Pos, 1, SearchRadius, SearchVertical, Random);
            if (Found.isPresent() && roomy(World, Vec3.atBottomCenterOf(Found.get()))) {
                return Found;
            }
        }
        return Optional.empty();
    }
}
