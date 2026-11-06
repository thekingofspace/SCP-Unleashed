package net.rai.scp_unleashed.scp098;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class Scp098HiveBlockEntity extends BlockEntity {
    public static final int[] StageTicks = {1200, 1600, 2000};
    public static final int Capacity = 6;
    public static final int RipenTicks = 12000;
    public static final int RipenInterval = 20;
    public static final int CloudMin = 1200;
    public static final int CloudMax = 3000;
    public static final int EmergeMin = 600;
    public static final int EmergeMax = 1800;
    public static final double PreyRange = 16.0;
    public static final float WanderChance = 0.2f;
    public static final int MaxOut = 2;
    public static final double OutRange = 48.0;
    public static final int FullAt = 120;
    public static final int FullnessCap = 240;
    public static final int SpreadRange = 32;
    public static final int SpreadTries = 48;
    public static final int SpreadRetry = 1200;
    public static final int LocalCap = 6;
    public static final int GlobalCap = 40;
    public static final int SyncTicks = 10;
    public static final int EventCloud = 1;
    public static final String ResidentsKey = "Residents";
    public static final String CountKey = "OccupantCount";
    public static final String FullnessKey = "Fullness";

    public record Occupant(float Ripeness, float Health) {
    }

    private final List<Occupant> Occupants = new ArrayList<>();
    private int Growth;
    private int Fullness;
    private int CloudLeft;
    private int CloudSeed;
    private int CloudCooldown = -1;
    private int EmergeCooldown = -1;
    private int SpreadCooldown;
    private boolean Registered;
    private boolean NeedsSync;
    private long LastSync;
    private int ClientOccupants;
    private int ClientCloudLeft;
    private int ClientCloudSeed;
    @Nullable
    private UUID Breaker;

    public Scp098HiveBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp098Registry.HiveEntity.get(), Pos, State);
    }

    public int stage() {
        BlockState State = getBlockState();
        return State.hasProperty(Scp098HiveBlock.Stage) ? State.getValue(Scp098HiveBlock.Stage) : 0;
    }

    public int occupants() {
        return level != null && level.isClientSide ? ClientOccupants : Occupants.size();
    }

    public int fullness() {
        return Fullness;
    }

    public int fullnessPercent() {
        return Math.min(100, Fullness * 100 / FullAt);
    }

    public int growth() {
        return Growth;
    }

    public boolean hasRoom() {
        return Occupants.size() < Capacity;
    }

    public void blame(Player User) {
        Breaker = User.getUUID();
    }

    private void markDirty() {
        setChanged();
        NeedsSync = true;
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp098HiveBlockEntity Hive) {
        if (!(World instanceof ServerLevel Server)) {
            return;
        }
        if (!Hive.Registered) {
            Hive.Registered = true;
            Scp098Hives.get(Server).add(Pos);
        }
        RandomSource Random = Server.getRandom();
        int Stage = Hive.stage();
        if (Stage < Scp098HiveBlock.MaxStage) {
            Hive.Growth++;
            if (Hive.Growth >= StageTicks[Stage]) {
                Hive.Growth = 0;
                Hive.setStage(Server, Stage + 1);
                return;
            }
            if (Hive.Growth % 200 == 0) {
                Hive.setChanged();
            }
        }
        if (Server.getGameTime() % RipenInterval == 0 && !Hive.Occupants.isEmpty()) {
            Hive.ripen();
        }
        if (Stage >= Scp098HiveBlock.MaxStage) {
            Hive.clouds(Server, Pos, Random);
            Hive.spreadTick(Server, Random);
        }
        Hive.stir(Server, Random);
        if (Hive.NeedsSync && Server.getGameTime() - Hive.LastSync >= SyncTicks) {
            Hive.NeedsSync = false;
            Hive.LastSync = Server.getGameTime();
            Server.sendBlockUpdated(Pos, State, State, Block.UPDATE_CLIENTS);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, Scp098HiveBlockEntity Hive) {
        if (Hive.ClientCloudLeft <= 0) {
            return;
        }
        Scp098Spores.puff(World, Pos, Hive.ClientCloudSeed, Scp098Spores.CloudTicks - Hive.ClientCloudLeft, World.getRandom());
        Hive.ClientCloudLeft--;
    }

    @Override
    public boolean triggerEvent(int Id, int Param) {
        if (Id != EventCloud) {
            return super.triggerEvent(Id, Param);
        }
        if (level != null && level.isClientSide) {
            ClientCloudLeft = Scp098Spores.CloudTicks;
            ClientCloudSeed = Param;
        }
        return true;
    }

    public void setStage(ServerLevel World, int Stage) {
        int Clamped = Mth.clamp(Stage, 0, Scp098HiveBlock.MaxStage);
        BlockState State = getBlockState();
        if (!State.hasProperty(Scp098HiveBlock.Stage)) {
            return;
        }
        Growth = 0;
        World.setBlock(worldPosition, State.setValue(Scp098HiveBlock.Stage, Clamped), Block.UPDATE_ALL);
        Vec3 Top = Vec3.atCenterOf(worldPosition).add(0.0, 0.55, 0.0);
        World.playSound(null, Top.x, Top.y, Top.z, Scp098Registry.HiveGrow.get(), SoundSource.BLOCKS, 0.7f, 0.85f + World.getRandom().nextFloat() * 0.2f);
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()), Top.x, Top.y, Top.z, 12, 0.3, 0.05, 0.3, 0.02);
        markDirty();
    }

    private void ripen() {
        float Step = (float) RipenInterval / RipenTicks;
        for (int Index = 0; Index < Occupants.size(); Index++) {
            Occupant Current = Occupants.get(Index);
            if (Current.Ripeness() < 1.0f) {
                Occupants.set(Index, new Occupant(Math.min(1.0f, Current.Ripeness() + Step), Current.Health()));
            }
        }
        setChanged();
    }

    private void clouds(ServerLevel World, BlockPos Pos, RandomSource Random) {
        if (CloudLeft > 0) {
            int Elapsed = Scp098Spores.CloudTicks - CloudLeft;
            if (Elapsed % Scp098Spores.ExposeInterval == 0) {
                Scp098Spores.expose(World, Pos, CloudSeed, Elapsed);
            }
            CloudLeft--;
            return;
        }
        if (CloudCooldown < 0) {
            CloudCooldown = CloudMin + Random.nextInt(CloudMax - CloudMin + 1);
        }
        if (--CloudCooldown <= 0) {
            CloudCooldown = -1;
            startCloud(World);
        }
    }

    public void startCloud(ServerLevel World) {
        CloudLeft = Scp098Spores.CloudTicks;
        CloudSeed = World.getRandom().nextInt(1 << 20);
        World.blockEvent(worldPosition, getBlockState().getBlock(), EventCloud, CloudSeed);
        Vec3 Top = Vec3.atCenterOf(worldPosition).add(0.0, 0.6, 0.0);
        float Pitch = 0.9f + World.getRandom().nextFloat() * 0.15f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp098Registry.SporeLayers) {
            World.playSound(null, Top.x, Top.y, Top.z, Layer.get(), SoundSource.BLOCKS, 1.0f, Pitch);
        }
    }

    private void stir(ServerLevel World, RandomSource Random) {
        if (Occupants.isEmpty()) {
            EmergeCooldown = -1;
            return;
        }
        if (EmergeCooldown < 0) {
            EmergeCooldown = EmergeMin + Random.nextInt(EmergeMax - EmergeMin + 1);
        }
        if (--EmergeCooldown > 0) {
            return;
        }
        EmergeCooldown = EmergeMin + Random.nextInt(EmergeMax - EmergeMin + 1);
        if (outside(World) >= MaxOut) {
            return;
        }
        Vec3 Centre = Vec3.atCenterOf(worldPosition);
        LivingEntity Prey = World.getEntitiesOfClass(Animal.class, new AABB(worldPosition).inflate(PreyRange), Scp098Crab::isPrey).stream()
                .filter(Found -> Found.distanceToSqr(Centre) <= PreyRange * PreyRange)
                .min(Comparator.comparingDouble(Found -> Found.distanceToSqr(Centre))).orElse(null);
        if (Prey != null) {
            release(World, Prey, null);
        } else if (Random.nextFloat() < WanderChance) {
            release(World, null, null);
        }
    }

    public int outside(ServerLevel World) {
        return World.getEntitiesOfClass(Scp098Crab.class, new AABB(worldPosition).inflate(OutRange), Crab -> worldPosition.equals(Crab.home())).size();
    }

    public boolean release(ServerLevel World, @Nullable LivingEntity Prey, @Nullable LivingEntity Grudge) {
        if (Occupants.isEmpty()) {
            return false;
        }
        Occupant Ripest = Occupants.stream().max(Comparator.comparingDouble(Occupant::Ripeness)).orElseThrow();
        Occupants.remove(Ripest);
        markDirty();
        return Scp098Crab.surface(World, worldPosition, Ripest, Prey, Grudge, false) != null;
    }

    public boolean admit(float Ripeness, float Health) {
        if (!hasRoom()) {
            return false;
        }
        Occupants.add(new Occupant(Mth.clamp(Ripeness, 0.0f, 1.0f), Health));
        markDirty();
        return true;
    }

    public void feed(int Food) {
        if (Food <= 0) {
            return;
        }
        Fullness = Math.min(FullnessCap, Fullness + Food);
        markDirty();
    }

    public void setFullness(int Value) {
        Fullness = Mth.clamp(Value, 0, FullnessCap);
        markDirty();
    }

    public void collapse(ServerLevel World) {
        Player Culprit = Breaker == null ? null : World.getPlayerByUUID(Breaker);
        LivingEntity Grudge = Culprit != null && !Culprit.isCreative() && !Culprit.isSpectator() ? Culprit : null;
        Vec3 Centre = Vec3.atCenterOf(worldPosition);
        if (!Occupants.isEmpty()) {
            for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp098Registry.BurrowLayers) {
                World.playSound(null, Centre.x, Centre.y, Centre.z, Layer.get(), SoundSource.HOSTILE, 1.2f, 0.8f);
            }
        }
        for (Occupant Resident : List.copyOf(Occupants)) {
            Scp098Crab.surface(World, worldPosition, Resident, null, Grudge, true);
        }
        Occupants.clear();
        World.sendParticles(Scp098Registry.Spore.get(), Centre.x, Centre.y, Centre.z, 8, 0.4, 0.3, 0.4, 0.01);
    }

    private void spreadTick(ServerLevel World, RandomSource Random) {
        if (Fullness < FullAt) {
            return;
        }
        if (--SpreadCooldown > 0) {
            return;
        }
        SpreadCooldown = SpreadRetry;
        if (spread(World, Random)) {
            Fullness = 0;
            markDirty();
        }
    }

    public boolean spread(ServerLevel World, RandomSource Random) {
        if (!World.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return false;
        }
        Scp098Hives Hives = Scp098Hives.get(World);
        if (Hives.count() >= GlobalCap || Hives.countNear(worldPosition, SpreadRange) >= LocalCap) {
            return false;
        }
        for (int Attempt = 0; Attempt < SpreadTries; Attempt++) {
            BlockPos Target = worldPosition.offset(Random.nextInt(SpreadRange * 2 + 1) - SpreadRange, Random.nextInt(17) - 8, Random.nextInt(SpreadRange * 2 + 1) - SpreadRange);
            if (Target.distSqr(worldPosition) > (double) SpreadRange * SpreadRange || !World.isLoaded(Target) || !isSand(World.getBlockState(Target))) {
                continue;
            }
            BlockPos Above = Target.above();
            if (World.getBlockState(Above).isCollisionShapeFullBlock(World, Above)) {
                continue;
            }
            World.setBlock(Target, Scp098Registry.Hive.get().defaultBlockState(), Block.UPDATE_ALL);
            Vec3 Spot = Vec3.atCenterOf(Target).add(0.0, 0.55, 0.0);
            for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp098Registry.SpreadLayers) {
                World.playSound(null, Spot.x, Spot.y, Spot.z, Layer.get(), SoundSource.BLOCKS, 0.9f, 0.9f);
            }
            World.sendParticles(Scp098Registry.Spore.get(), Spot.x, Spot.y, Spot.z, 14, 0.4, 0.2, 0.4, 0.01);
            return true;
        }
        return false;
    }

    public static boolean isSand(BlockState State) {
        return State.is(Blocks.SAND) || State.is(Blocks.RED_SAND);
    }

    @Override
    protected void saveAdditional(CompoundTag Saved, HolderLookup.Provider Registries) {
        super.saveAdditional(Saved, Registries);
        Saved.putInt("Growth", Growth);
        Saved.putInt(FullnessKey, Fullness);
        Saved.putInt("CloudCooldown", CloudCooldown);
        Saved.putInt("EmergeCooldown", EmergeCooldown);
        Saved.putInt("SpreadCooldown", SpreadCooldown);
        ListTag Residents = new ListTag();
        for (Occupant Resident : Occupants) {
            CompoundTag Entry = new CompoundTag();
            Entry.putFloat("Ripeness", Resident.Ripeness());
            Entry.putFloat("Health", Resident.Health());
            Residents.add(Entry);
        }
        Saved.put(ResidentsKey, Residents);
    }

    @Override
    protected void loadAdditional(CompoundTag Saved, HolderLookup.Provider Registries) {
        super.loadAdditional(Saved, Registries);
        Fullness = Mth.clamp(Saved.getInt(FullnessKey), 0, FullnessCap);
        if (Saved.contains(CountKey)) {
            ClientOccupants = Saved.getInt(CountKey);
        }
        if (Saved.contains(ResidentsKey, Tag.TAG_LIST)) {
            Growth = Math.max(0, Saved.getInt("Growth"));
            CloudCooldown = Saved.contains("CloudCooldown") ? Saved.getInt("CloudCooldown") : -1;
            EmergeCooldown = Saved.contains("EmergeCooldown") ? Saved.getInt("EmergeCooldown") : -1;
            SpreadCooldown = Saved.getInt("SpreadCooldown");
            Occupants.clear();
            ListTag Residents = Saved.getList(ResidentsKey, Tag.TAG_COMPOUND);
            for (int Index = 0; Index < Residents.size() && Occupants.size() < Capacity; Index++) {
                CompoundTag Entry = Residents.getCompound(Index);
                Occupants.add(new Occupant(Mth.clamp(Entry.getFloat("Ripeness"), 0.0f, 1.0f), Math.max(1.0f, Entry.getFloat("Health"))));
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Update = new CompoundTag();
        Update.putInt(CountKey, Occupants.size());
        Update.putInt(FullnessKey, Fullness);
        return Update;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
