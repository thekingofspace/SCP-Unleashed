package net.rai.scp_unleashed.z0004;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class Z0004ChestBlockEntity extends BlockEntity {
    public static final int Idle = 0;
    public static final int Active = 1;
    public static final int Won = 2;
    public static final int CheckInterval = 20;
    private int Stage = Idle;
    @Nullable
    private UUID FightId;
    @Nullable
    private UUID ShipId;
    private final Set<UUID> Crew = new LinkedHashSet<>();
    private final Set<UUID> Participants = new LinkedHashSet<>();
    private boolean Boarding;
    private long StartedAt;
    private long RestUntil;
    private int AwayTicks;
    private int Rewards;
    private double AnchorY = Double.NaN;

    public Z0004ChestBlockEntity(BlockPos Pos, BlockState State) {
        super(Z0004Registry.ChestEntity.get(), Pos, State);
    }

    public int stage() {
        return Stage;
    }

    @Nullable
    public UUID fightId() {
        return FightId;
    }

    @Nullable
    public UUID shipId() {
        return ShipId;
    }

    public Set<UUID> crew() {
        return Crew;
    }

    public Set<UUID> participants() {
        return Participants;
    }

    public boolean boarding() {
        return Boarding;
    }

    public long restUntil() {
        return RestUntil;
    }

    public int rewards() {
        return Rewards;
    }

    public boolean tracks(@Nullable UUID Fight) {
        return Stage == Active && Fight != null && Fight.equals(FightId);
    }

    public void open(ServerLevel World, Player User) {
        Vec3 Centre = Vec3.atCenterOf(worldPosition);
        Z0004Registry.play(World, Centre, Z0004Registry.OpenLayers, SoundSource.BLOCKS, 0.9f, 0.62f + World.random.nextFloat() * 0.08f);
        World.sendParticles(ParticleTypes.DRIPPING_WATER, Centre.x, Centre.y + 0.5, Centre.z, 8, 0.35, 0.1, 0.3, 0.0);
        World.sendParticles(ParticleTypes.FALLING_WATER, Centre.x, Centre.y + 0.6, Centre.z, 4, 0.3, 0.1, 0.25, 0.0);
        long Now = World.getGameTime();
        if (Stage == Won) {
            if (Participants.contains(User.getUUID())) {
                reward(World, User);
            }
            return;
        }
        if (Stage == Active || Now < RestUntil) {
            return;
        }
        if (!Z0004Fight.largeWater(World, worldPosition)) {
            return;
        }
        Optional<Z0004Fight.Spot> Found = Z0004Fight.findSpot(World, worldPosition);
        if (Found.isEmpty()) {
            return;
        }
        summon(World, User, Found.get());
    }

    public boolean summon(ServerLevel World, Player User, Z0004Fight.Spot Spot) {
        Z0004Ship Made = Z0004Registry.Ship.get().create(World);
        if (Made == null) {
            return false;
        }
        FightId = UUID.randomUUID();
        Made.prepare(Spot, worldPosition, FightId);
        World.addFreshEntity(Made);
        ShipId = Made.getUUID();
        Stage = Active;
        Boarding = false;
        Crew.clear();
        Participants.clear();
        Participants.add(User.getUUID());
        AnchorY = Z0004Fight.anchor(World, worldPosition).y;
        for (ServerPlayer Near : World.players()) {
            if (near(Near.position(), Z0004Fight.JoinRange) && !Near.isSpectator()) {
                Participants.add(Near.getUUID());
            }
        }
        StartedAt = World.getGameTime();
        AwayTicks = 0;
        Vec3 Centre = Vec3.atCenterOf(worldPosition);
        Z0004Registry.play(World, Centre, Z0004Registry.HornLayers, SoundSource.HOSTILE, 3.0f, 0.55f);
        Z0004Registry.play(World, Spot.Position(), Z0004Registry.BellLayers, SoundSource.HOSTILE, 3.0f, 0.5f);
        World.sendParticles(ParticleTypes.CLOUD, Spot.Position().x, Spot.Position().y + 1.0, Spot.Position().z, 80, 9.0, 1.5, 9.0, 0.01);
        setChanged();
        return true;
    }

    public void join(UUID Player) {
        if (Stage == Active && Participants.add(Player)) {
            setChanged();
        }
    }

    public void shipDestroyed(List<UUID> Spawned) {
        if (Stage != Active) {
            return;
        }
        Boarding = true;
        Crew.clear();
        Crew.addAll(Spawned);
        setChanged();
    }

    public void crewDown(UUID Member) {
        if (Stage == Active && Crew.remove(Member)) {
            setChanged();
        }
    }

    public Vec3 anchor() {
        return Double.isNaN(AnchorY) ? Vec3.atCenterOf(worldPosition) : new Vec3(worldPosition.getX() + 0.5, AnchorY, worldPosition.getZ() + 0.5);
    }

    public boolean near(Vec3 Where, double Range) {
        Vec3 Chest = Vec3.atCenterOf(worldPosition);
        if (Where.distanceToSqr(Chest) < Range * Range) {
            return true;
        }
        Vec3 Surface = anchor();
        return Z0004Fight.flatDistanceSqr(Where, Surface) < Range * Range && Where.y >= Chest.y - Range && Where.y <= Surface.y + Range;
    }

    private void reward(ServerLevel World, Player User) {
        Vec3 Top = Vec3.atCenterOf(worldPosition).add(0.0, 0.6, 0.0);
        List<ItemStack> Loot = new ArrayList<>();
        LootTable Table = World.getServer().reloadableRegistries().getLootTable(Z0004Registry.RewardTable);
        LootParams Params = new LootParams.Builder(World).withParameter(LootContextParams.ORIGIN, Top).withParameter(LootContextParams.THIS_ENTITY, User)
                .withLuck(User.getLuck()).create(LootContextParamSets.CHEST);
        Table.getRandomItems(Params, Loot::add);
        Loot.add(new ItemStack(Z0004Registry.Cutlass.get()));
        for (ItemStack Stack : Loot) {
            if (Stack.isEmpty()) {
                continue;
            }
            if (!User.getInventory().add(Stack) || !Stack.isEmpty()) {
                ItemEntity Dropped = new ItemEntity(World, User.getX(), User.getY() + 0.5, User.getZ(), Stack);
                Dropped.setPickUpDelay(0);
                World.addFreshEntity(Dropped);
            }
        }
        User.containerMenu.broadcastChanges();
        Z0004Registry.play(World, Top, Z0004Registry.RewardLayers, SoundSource.BLOCKS, 1.0f, 1.0f);
        World.sendParticles(ParticleTypes.GLOW, Top.x, Top.y + 0.2, Top.z, 16, 0.3, 0.3, 0.3, 0.02);
        Rewards++;
        Stage = Idle;
        Participants.clear();
        FightId = null;
        ShipId = null;
        RestUntil = World.getGameTime() + Z0004Fight.CooldownTicks;
        setChanged();
    }

    public void lose(ServerLevel World) {
        if (Stage != Active) {
            return;
        }
        if (ShipId != null && World.getEntity(ShipId) instanceof Z0004Ship Ship) {
            Ship.retreat();
        }
        for (UUID Member : Crew) {
            if (World.getEntity(Member) instanceof Z0004Pirate Pirate) {
                Pirate.retreat();
            }
        }
        Stage = Idle;
        Crew.clear();
        Participants.clear();
        FightId = null;
        ShipId = null;
        Boarding = false;
        RestUntil = World.getGameTime() + Z0004Fight.RetryTicks;
        Z0004Registry.play(World, Vec3.atCenterOf(worldPosition), Z0004Registry.BellLayers, SoundSource.HOSTILE, 1.2f, 0.45f);
        setChanged();
    }

    public void abandon(ServerLevel World) {
        lose(World);
    }

    private void win(ServerLevel World) {
        Stage = Won;
        Crew.clear();
        Boarding = false;
        ShipId = null;
        Z0004Registry.play(World, Vec3.atCenterOf(worldPosition), Z0004Registry.BellLayers, SoundSource.BLOCKS, 1.0f, 1.2f);
        setChanged();
    }

    private boolean present(ServerLevel World) {
        for (UUID Id : Participants) {
            Player User = World.getPlayerByUUID(Id);
            if (User != null && User.isAlive() && !User.isSpectator() && near(User.position(), Z0004Fight.LeaveRange)) {
                return true;
            }
        }
        return false;
    }

    public static void serverTick(net.minecraft.world.level.Level Level0, BlockPos Pos, BlockState State, Z0004ChestBlockEntity Chest) {
        if (!(Level0 instanceof ServerLevel World) || Chest.Stage != Active || World.getGameTime() % CheckInterval != 0) {
            return;
        }
        Chest.check(World);
    }

    public void check(ServerLevel World) {
        if (Stage != Active) {
            return;
        }
        long Now = World.getGameTime();
        if (Now - StartedAt > Z0004Fight.FightTicks) {
            lose(World);
            return;
        }
        if (present(World)) {
            AwayTicks = 0;
        } else {
            AwayTicks += CheckInterval;
            if (AwayTicks >= Z0004Fight.LeaveTicks) {
                lose(World);
                return;
            }
        }
        if (Boarding) {
            Crew.removeIf(Id -> {
                Entity Member = World.getEntity(Id);
                return Member != null && !Member.isAlive();
            });
            if (Crew.isEmpty()) {
                win(World);
            }
        }
    }

    public void forceStage(int Value, long Rest) {
        Stage = Value;
        RestUntil = Rest;
        setChanged();
    }

    public void startedAt(long Tick) {
        StartedAt = Tick;
    }

    @Override
    protected void saveAdditional(CompoundTag Tag0, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag0, Registries);
        Tag0.putInt("Stage", Stage);
        if (FightId != null) {
            Tag0.putUUID("Fight", FightId);
        }
        if (ShipId != null) {
            Tag0.putUUID("Ship", ShipId);
        }
        Tag0.put("Crew", ids(Crew));
        Tag0.put("Participants", ids(Participants));
        Tag0.putBoolean("Boarding", Boarding);
        Tag0.putLong("StartedAt", StartedAt);
        Tag0.putLong("RestUntil", RestUntil);
        Tag0.putInt("AwayTicks", AwayTicks);
        Tag0.putInt("Rewards", Rewards);
        if (!Double.isNaN(AnchorY)) {
            Tag0.putDouble("AnchorY", AnchorY);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Tag0, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag0, Registries);
        Stage = Tag0.getInt("Stage");
        FightId = Tag0.hasUUID("Fight") ? Tag0.getUUID("Fight") : null;
        ShipId = Tag0.hasUUID("Ship") ? Tag0.getUUID("Ship") : null;
        read(Tag0.getList("Crew", Tag.TAG_INT_ARRAY), Crew);
        read(Tag0.getList("Participants", Tag.TAG_INT_ARRAY), Participants);
        Boarding = Tag0.getBoolean("Boarding");
        StartedAt = Tag0.getLong("StartedAt");
        RestUntil = Tag0.getLong("RestUntil");
        AwayTicks = Tag0.getInt("AwayTicks");
        Rewards = Tag0.getInt("Rewards");
        AnchorY = Tag0.contains("AnchorY") ? Tag0.getDouble("AnchorY") : Double.NaN;
    }

    private static ListTag ids(Set<UUID> Values) {
        ListTag List0 = new ListTag();
        for (UUID Id : Values) {
            List0.add(NbtUtils.createUUID(Id));
        }
        return List0;
    }

    private static void read(ListTag List0, Set<UUID> Into) {
        Into.clear();
        for (Tag Entry : List0) {
            Into.add(NbtUtils.loadUUID(Entry));
        }
    }
}
