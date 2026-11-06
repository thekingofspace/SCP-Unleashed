package net.rai.scp_unleashed.scp001scarlet.cult;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.raid.Raid;
import net.rai.scp_unleashed.raid.RaidContext;
import net.rai.scp_unleashed.raid.RaidDirector;
import net.rai.scp_unleashed.raid.RaidGround;
import net.rai.scp_unleashed.raid.RaidMark;
import net.rai.scp_unleashed.raid.RaidOutcome;
import net.rai.scp_unleashed.raid.RaidRules;
import net.rai.scp_unleashed.raid.RaidStyle;
import net.rai.scp_unleashed.scp001scarlet.Scp001ScarletRegistry;
import net.rai.scp_unleashed.scp001scarlet.ScarletConfig;
import net.rai.scp_unleashed.scp001scarlet.ScarletMeter;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletGlyphBlock;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ScarletRitualStyle implements RaidStyle {
    public static final ScarletRitualStyle Instance = new ScarletRitualStyle();
    public static final ResourceLocation Id = Scp001ScarletRegistry.location("scarlet_ritual");
    public static final String CentreKey = "Centre";
    public static final String ProgressKey = "Progress";
    public static final String CompletedKey = "Completed";
    public static final String BumpKey = "Bump";
    public static final int MinGroup = 3;
    public static final int MaxGroup = 5;
    public static final int ChantEvery = 80;
    public static final double ChantDistance = 18.0;
    public static final int[][] Candles = {{-2, -2}, {2, -2}, {-2, 2}, {2, 2}};

    private ScarletRitualStyle() {
    }

    @Override
    public ResourceLocation id() {
        return Id;
    }

    @Override
    public int warningTicks() {
        return 160;
    }

    @Override
    public long durationTicks() {
        return ScarletConfig.ritualTicks() + 3600L;
    }

    @Override
    public long retreatTicks() {
        return 40L;
    }

    @Override
    public long cooldownTicks() {
        return (long) ((ScarletConfig.cultistMinDays() + ScarletConfig.cultistMaxDays()) * 0.5 * 24000.0);
    }

    @Override
    public int maxAlive() {
        return MaxGroup;
    }

    @Override
    public boolean allowsDimension(ResourceKey<Level> Dimension) {
        return Dimension == Level.OVERWORLD;
    }

    @Override
    public String noticeKey(String Event) {
        return "raid.scp_unleashed.scarlet_cult." + Event;
    }

    @Override
    public void warn(RaidContext Context) {
        chantFrom(Context, 1.0f);
    }

    @Override
    public void tickWarning(RaidContext Context) {
        long Elapsed = Context.Now() - Context.Current().stageAt();
        if (Elapsed > 0 && Elapsed % 60 == 0) {
            chantFrom(Context, 0.8f);
        }
    }

    private static void chantFrom(RaidContext Context, float Volume) {
        double Heading = Context.Current().heading();
        RandomSource Random = Context.random();
        for (ServerPlayer User : Context.defenders(RaidRules.NoticeRange)) {
            double X = User.getX() + Math.cos(Heading) * ChantDistance;
            double Z = User.getZ() + Math.sin(Heading) * ChantDistance;
            User.connection.send(new ClientboundSoundPacket(Scp001ScarletRegistry.Chant, SoundSource.HOSTILE, X, User.getY() + 1.0, Z, Volume, 0.9f, Random.nextLong()));
            User.connection.send(new ClientboundSoundPacket(Scp001ScarletRegistry.ChantDrone, SoundSource.HOSTILE, X, User.getY() + 1.0, Z, Volume * 0.7f, 0.8f, Random.nextLong()));
        }
    }

    public static boolean clearSpot(ServerLevel World, BlockPos Centre) {
        if (!World.canSeeSky(Centre.above()) || !RaidGround.standable(World, Centre)) {
            return false;
        }
        for (int Index = 0; Index < MaxGroup; Index++) {
            BlockPos Slot = slot(Centre, angle(Index, MaxGroup));
            if (RaidGround.standNear(World, Slot.getX(), Slot.getZ(), Centre.getY(), 2) == null) {
                return false;
            }
        }
        return true;
    }

    public static float angle(int Index, int Count) {
        return Index * Mth.TWO_PI / Count;
    }

    public static BlockPos slot(BlockPos Centre, float Angle) {
        return BlockPos.containing(Centre.getX() + 0.5 + Math.sin(Angle) * ScarletCultist.SlotRadius, Centre.getY(), Centre.getZ() + 0.5 - Math.cos(Angle) * ScarletCultist.SlotRadius);
    }

    @Nullable
    public static BlockPos findCentre(ServerLevel World, Raid Current, RandomSource Random) {
        BlockPos Base = Current.estimate();
        double Near = Current.footprint() + 10.0;
        for (int Attempt = 0; Attempt < 16; Attempt++) {
            BlockPos Found = RaidGround.standAround(World, Base, Near, Near + 18.0, Base.getY(), 12, Random, 8, true);
            if (Found != null && clearSpot(World, Found)) {
                return Found;
            }
        }
        BlockPos Fallback = RaidGround.standNear(World, Current.target().getX(), Current.target().getZ(), Current.target().getY(), 8);
        return Fallback != null && RaidGround.standable(World, Fallback) ? Fallback : null;
    }

    public static int groupSize(ServerLevel World, RandomSource Random) {
        int Size = MinGroup + Random.nextInt(MaxGroup - MinGroup + 1);
        if (World.getDifficulty() == Difficulty.EASY) {
            Size = Math.max(MinGroup, Size - 1);
        }
        return Mth.clamp(Size, MinGroup, MaxGroup);
    }

    @Override
    public boolean begin(RaidContext Context) {
        if (!ScarletCultFaction.allowed(Context.Server())) {
            return false;
        }
        ServerLevel World = Context.World();
        Raid Current = Context.Current();
        BlockPos Centre = findCentre(World, Current, Context.random());
        if (Centre == null) {
            return false;
        }
        return gather(Context, Centre, groupSize(World, Context.random()));
    }

    public static boolean gather(RaidContext Context, BlockPos Centre, int Size) {
        ServerLevel World = Context.World();
        Raid Current = Context.Current();
        int Made = 0;
        for (int Index = 0; Index < Size && Context.room() > 0; Index++) {
            float Angle = angle(Index, Size);
            BlockPos Slot = slot(Centre, Angle);
            BlockPos Feet = RaidGround.standNear(World, Slot.getX(), Slot.getZ(), Centre.getY(), 3);
            if (Feet == null) {
                Feet = Centre;
            }
            ScarletCultist Member = Scp001ScarletRegistry.CultistType.get().create(World);
            if (Member == null) {
                continue;
            }
            Member.moveTo(Feet.getX() + 0.5, Feet.getY(), Feet.getZ() + 0.5, Angle * Mth.RAD_TO_DEG + 180.0f, 0.0f);
            Member.finalizeSpawn(World, World.getCurrentDifficultyAt(Feet), MobSpawnType.EVENT, null);
            Member.leader(Index == 0);
            Member.assign(Centre, Angle);
            Member.setPersistenceRequired();
            if (!World.addFreshEntity(Member)) {
                continue;
            }
            Context.enlist(Member, Index == 0 ? RaidMark.Caster : RaidMark.Raider);
            World.sendParticles(ParticleTypes.LARGE_SMOKE, Member.getX(), Member.getY() + 1.0, Member.getZ(), 12, 0.3, 0.6, 0.3, 0.02);
            Made++;
        }
        if (Made == 0) {
            return false;
        }
        CompoundTag Data = Current.extra();
        Data.putLong(CentreKey, Centre.asLong());
        Data.putInt(ProgressKey, 0);
        Current.changed();
        kindle(World, Centre);
        return true;
    }

    private static void kindle(ServerLevel World, BlockPos Centre) {
        if (World.getBlockState(Centre).canBeReplaced()) {
            World.setBlock(Centre, Scp001ScarletRegistry.Brazier.get().defaultBlockState().setValue(ScarletBrazierBlock.Lit, true), Block.UPDATE_ALL);
        }
        for (int[] Offset : Candles) {
            BlockPos At = RaidGround.standNear(World, Centre.getX() + Offset[0], Centre.getZ() + Offset[1], Centre.getY(), 2);
            if (At != null && World.getBlockState(At).isAir()) {
                World.setBlock(At, Blocks.RED_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 1 + World.random.nextInt(3)).setValue(CandleBlock.LIT, true),
                        Block.UPDATE_ALL);
            }
        }
        World.playSound(null, Centre, Scp001ScarletRegistry.Kindle.get(), SoundSource.HOSTILE, 1.0f, 0.8f);
    }

    @Nullable
    public static BlockPos centre(Raid Current) {
        CompoundTag Data = Current.extra();
        return Data.contains(CentreKey) ? BlockPos.of(Data.getLong(CentreKey)) : null;
    }

    public static int progress(Raid Current) {
        return Current.extra().getInt(ProgressKey);
    }

    public static void progress(Raid Current, int Value) {
        Current.extra().putInt(ProgressKey, Value);
        Current.changed();
    }

    public static boolean completed(Raid Current) {
        return Current.extra().getBoolean(CompletedKey);
    }

    public static float bump(Raid Current) {
        return Current.extra().getFloat(BumpKey);
    }

    public static int kneeling(RaidContext Context) {
        List<Mob> Loaded = Context.loadedRaiders();
        if (Loaded.isEmpty()) {
            return Context.Current().remaining();
        }
        int Count = 0;
        for (Mob Each : Loaded) {
            if (Each instanceof ScarletCultist Member && Member.kneeling()) {
                Count++;
            }
        }
        return Count;
    }

    @Override
    public void tick(RaidContext Context) {
        Raid Current = Context.Current();
        if (!ScarletCultFaction.allowed(Context.Server())) {
            RaidDirector.end(Context, RaidOutcome.Stopped);
            return;
        }
        Current.setLastPresence(Context.Now());
        if (Current.remaining() < 2) {
            RaidDirector.end(Context, RaidOutcome.Defeated);
            return;
        }
        BlockPos Centre = centre(Current);
        if (Centre == null) {
            RaidDirector.end(Context, RaidOutcome.Stopped);
            return;
        }
        if (kneeling(Context) >= 2) {
            progress(Current, progress(Current) + RaidRules.TickEvery);
        }
        ServerLevel World = Context.World();
        if (World.isLoaded(Centre)) {
            World.sendParticles(ScarletGlyphBlock.Ember, Centre.getX() + 0.5, Centre.getY() + 0.9, Centre.getZ() + 0.5, 6, 1.6, 0.3, 1.6, 0.0);
            if (progress(Current) % ChantEvery < RaidRules.TickEvery) {
                World.playSound(null, Centre, Scp001ScarletRegistry.Chant.get(), SoundSource.HOSTILE, 1.0f, 0.85f + World.random.nextFloat() * 0.1f);
                World.playSound(null, Centre, Scp001ScarletRegistry.ChantDrone.get(), SoundSource.HOSTILE, 0.7f, 0.8f);
            }
        }
        if (progress(Current) >= ScarletConfig.ritualTicks()) {
            complete(Context);
        }
    }

    public static void complete(RaidContext Context) {
        Raid Current = Context.Current();
        float Bump = (float) (ScarletConfig.ritualMin() + Context.random().nextDouble() * (ScarletConfig.ritualMax() - ScarletConfig.ritualMin()));
        ScarletMeter.add(Context.Server(), Bump);
        Current.extra().putBoolean(CompletedKey, true);
        Current.extra().putFloat(BumpKey, Bump);
        Current.changed();
        BlockPos Centre = centre(Current);
        if (Centre != null) {
            scorch(Context.World(), Centre);
        }
        Context.notice("completed");
        RaidDirector.end(Context, RaidOutcome.Retreated);
    }

    public static void scorch(ServerLevel World, BlockPos Centre) {
        if (World.getBlockState(Centre).is(Scp001ScarletRegistry.Brazier.get())) {
            World.setBlock(Centre, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        for (int Dx = -1; Dx <= 1; Dx++) {
            for (int Dz = -1; Dz <= 1; Dz++) {
                BlockPos At = Centre.offset(Dx, 0, Dz);
                BlockState Here = World.getBlockState(At);
                if ((Here.isAir() || Here.canBeReplaced() && Here.getFluidState().isEmpty()) && World.getBlockState(At.below()).isFaceSturdy(World, At.below(), Direction.UP)) {
                    World.setBlock(At, ScarletScorchBlock.part(Scp001ScarletRegistry.Scorch.get(), Dx, Dz), Block.UPDATE_ALL);
                }
            }
        }
        World.sendParticles(ParticleTypes.LARGE_SMOKE, Centre.getX() + 0.5, Centre.getY() + 0.5, Centre.getZ() + 0.5, 30, 1.2, 0.4, 1.2, 0.02);
    }

    @Override
    public boolean exhausted(RaidContext Context) {
        return true;
    }

    @Override
    public void dismiss(ServerLevel World, Mob Raider) {
        if (Raider instanceof ScarletCultist Member) {
            Member.vanish();
        } else {
            Raider.discard();
        }
    }

    @Override
    public void ended(RaidContext Context, RaidOutcome Outcome) {
        Raid Current = Context.Current();
        if (completed(Current)) {
            return;
        }
        BlockPos Centre = centre(Current);
        if (Centre == null) {
            return;
        }
        ServerLevel World = Context.World();
        BlockState Here = World.getBlockState(Centre);
        if (Here.is(Scp001ScarletRegistry.Brazier.get()) && Here.getValue(ScarletBrazierBlock.Lit)) {
            World.setBlock(Centre, Here.setValue(ScarletBrazierBlock.Lit, false), Block.UPDATE_ALL);
            World.sendParticles(ParticleTypes.SMOKE, Centre.getX() + 0.5, Centre.getY() + 0.7, Centre.getZ() + 0.5, 12, 0.2, 0.2, 0.2, 0.01);
        }
    }
}
