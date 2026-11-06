package net.rai.scp_unleashed.scp173;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class Scp173Clones extends SavedData {
    public static final String DataName = "scp_unleashed_scp173";
    public static final int SpotTries = 24;
    private static final SavedData.Factory<Scp173Clones> DataFactory = new SavedData.Factory<>(Scp173Clones::new, Scp173Clones::load);
    private final Set<UUID> Clones = new HashSet<>();

    public static Scp173Clones get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public int count() {
        return Clones.size();
    }

    public void add(UUID Id) {
        if (Clones.add(Id)) {
            setDirty();
        }
    }

    public void remove(UUID Id) {
        if (Clones.remove(Id)) {
            setDirty();
        }
    }

    public void clear() {
        Clones.clear();
        setDirty();
    }

    @Nullable
    public static Scp173Statue spawnFrom(ServerLevel World, Scp173Statue Parent, boolean Forced) {
        if (!Forced && !Scp173Registry.cloning(World.getGameRules())) {
            return null;
        }
        List<BlockPos> Spots = Scp173Residue.find(World, Parent.blockPosition(), Scp173Hunt.ResidueRadius, Scp173Hunt.ResidueHeight);
        if (Spots.isEmpty() && !Forced) {
            return null;
        }
        LongOpenHashSet Room = Scp173Room.floor(World, Parent, Scp173Hunt.ResidueRadius, Scp173Hunt.ResidueHeight);
        Spots.removeIf(Spot -> !Room.contains(Spot.asLong()));
        Collections.shuffle(Spots, new Random(Parent.getRandom().nextLong()));
        int Tried = 0;
        for (BlockPos Spot : Spots) {
            if (Tried++ >= SpotTries) {
                break;
            }
            AABB Space = Scp173Registry.Statue.get().getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(Spot));
            if (!World.noCollision(Space) || !Forced && Scp173Sight.anyoneCouldSee(World, Space)) {
                continue;
            }
            Scp173Statue Made = emerge(World, Spot, Parent);
            if (Made != null) {
                return Made;
            }
        }
        if (!Forced) {
            return null;
        }
        for (int Attempt = 0; Attempt < SpotTries; Attempt++) {
            BlockPos Spot = Parent.blockPosition().offset(Parent.getRandom().nextInt(7) - 3, 0, Parent.getRandom().nextInt(7) - 3);
            AABB Space = Scp173Registry.Statue.get().getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(Spot));
            if (Room.contains(Spot.asLong()) && World.noCollision(Space) && !World.getBlockState(Spot.below()).isAir()) {
                Scp173Statue Made = emerge(World, Spot, Parent);
                if (Made != null) {
                    return Made;
                }
            }
        }
        return null;
    }

    @Nullable
    private static Scp173Statue emerge(ServerLevel World, BlockPos Spot, Scp173Statue Parent) {
        Scp173Statue Made = Scp173Registry.Statue.get().create(World);
        if (Made == null) {
            return null;
        }
        if (Scp173Residue.isResidue(World.getBlockState(Spot))) {
            World.removeBlock(Spot, false);
        }
        Made.moveTo(Spot.getX() + 0.5, Spot.getY(), Spot.getZ() + 0.5, World.random.nextFloat() * 360.0f, 0.0f);
        Made.setClone(true);
        Made.setPoseIndex(World.random.nextInt(Scp173Statue.StandPoses));
        Made.finalizeSpawn(World, World.getCurrentDifficultyAt(Spot), MobSpawnType.EVENT, null);
        LivingEntity Nearest = Scp173Hunt.nearestPerson(World, Made.position(), 48.0);
        if (Nearest != null) {
            Made.faceTowards(Nearest.position());
        }
        if (!World.addFreshEntity(Made)) {
            return null;
        }
        get(World.getServer()).add(Made.getUUID());
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SANDSTONE.defaultBlockState()), Made.getX(), Made.getY() + 0.3, Made.getZ(), 16, 0.3, 0.2, 0.3, 0.03);
        Made.play(Scp173Registry.CloneLayers, Made.position().add(0.0, 1.0, 0.0), 1.0f, 0.85f + World.random.nextFloat() * 0.2f);
        return Made;
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        ListTag Saved = new ListTag();
        for (UUID Id : Clones) {
            Saved.add(NbtUtils.createUUID(Id));
        }
        Tag.put("Clones", Saved);
        return Tag;
    }

    public static Scp173Clones load(CompoundTag Tag, HolderLookup.Provider Registries) {
        Scp173Clones Data = new Scp173Clones();
        ListTag Saved = Tag.getList("Clones", net.minecraft.nbt.Tag.TAG_INT_ARRAY);
        for (int Index = 0; Index < Saved.size(); Index++) {
            Data.Clones.add(NbtUtils.loadUUID(Saved.get(Index)));
        }
        return Data;
    }
}
