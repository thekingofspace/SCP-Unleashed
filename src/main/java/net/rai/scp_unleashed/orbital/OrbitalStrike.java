package net.rai.scp_unleashed.orbital;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public final class OrbitalStrike {
    public static final float ProofResistance = 1200.0F;
    public static final double ColumnDepth = 2.5;
    public static final double DepthShare = 0.7;
    public static final float BeamFireSeconds = 8.0F;
    public static final float SplashFireSeconds = 3.0F;
    public static final double BeamPush = 1.1;
    public static final double SplashPush = 0.9;
    public static final double PushLift = 0.45;
    public static final float MagmaChance = 0.12F;
    public static final float BlackstoneChance = 0.42F;
    public static final float BasaltChance = 0.62F;
    public static final float FireChance = 0.14F;
    private static BiConsumer<Entity, Float> Observer = (Target, Amount) -> {
    };

    public record Settings(float Damage, double Radius, double EdgeShare, double Splash, float SplashDamage, boolean Crater, int CraterRadius) {
        public static Settings current() {
            return new Settings(OrbitalConfig.damage(), OrbitalConfig.beamRadius(), OrbitalConfig.edgeShare(), OrbitalConfig.splashRadius(), OrbitalConfig.splashDamage(),
                    OrbitalConfig.crater(), OrbitalConfig.craterRadius());
        }
    }

    private OrbitalStrike() {
    }

    public static void observe(@Nullable BiConsumer<Entity, Float> Watcher) {
        Observer = Watcher == null ? (Target, Amount) -> {
        } : Watcher;
    }

    public static float beamDamage(Settings Values, double Distance) {
        double Share = 1.0 - (1.0 - Values.EdgeShare()) * Mth.clamp(Distance / Values.Radius(), 0.0, 1.0);
        return (float) (Values.Damage() * Share);
    }

    public static float splashDamage(Settings Values, double Distance) {
        if (Distance >= Values.Splash() || Values.Splash() <= Values.Radius()) {
            return 0.0F;
        }
        double Share = Mth.clamp(1.0 - (Distance - Values.Radius()) / (Values.Splash() - Values.Radius()), 0.0, 1.0);
        return (float) (Values.SplashDamage() * Share);
    }

    public static DamageSource source(ServerLevel World, Entity Strike, @Nullable Entity Owner, Vec3 Centre) {
        return new DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(OrbitalRegistry.StrikeDamage), Strike, Owner, Centre);
    }

    public static int hurt(ServerLevel World, Entity Strike, @Nullable Entity Owner, Vec3 Centre, Settings Values) {
        double Reach = Math.max(Values.Radius(), Values.Splash()) + 2.0;
        AABB Area = new AABB(Centre.x - Reach, Centre.y - Reach, Centre.z - Reach, Centre.x + Reach, World.getMaxBuildHeight() + 16.0, Centre.z + Reach);
        DamageSource Source = source(World, Strike, Owner, Centre);
        Set<Entity> Bodies = new HashSet<>();
        int Hits = 0;
        for (Entity Candidate : World.getEntities((Entity) null, Area, Found -> Found.isAlive() && !Found.isSpectator() && (Found instanceof LivingEntity || Found instanceof PartEntity<?>))) {
            Entity Body = Candidate instanceof PartEntity<?> Part ? Part.getParent() : Candidate;
            if (!Bodies.add(Body)) {
                continue;
            }
            AABB Box = Candidate.getBoundingBox();
            double Dx = Math.max(Math.max(Box.minX - Centre.x, Centre.x - Box.maxX), 0.0);
            double Dz = Math.max(Math.max(Box.minZ - Centre.z, Centre.z - Box.maxZ), 0.0);
            double Dy = Math.max(Math.max(Box.minY - Centre.y, Centre.y - Box.maxY), 0.0);
            double Horizontal = Math.sqrt(Dx * Dx + Dz * Dz);
            boolean Beam = Horizontal <= Values.Radius() && Box.maxY >= Centre.y - ColumnDepth;
            float Amount = Beam ? beamDamage(Values, Horizontal) : splashDamage(Values, Math.sqrt(Horizontal * Horizontal + Dy * Dy));
            if (Amount <= 0.0F) {
                continue;
            }
            Candidate.invulnerableTime = 0;
            Body.invulnerableTime = 0;
            Observer.accept(Body, Amount);
            Candidate.hurt(Source, Amount);
            Hits++;
            if (Body.isAlive()) {
                Body.igniteForSeconds(Beam ? BeamFireSeconds : SplashFireSeconds);
                if (!(Candidate instanceof PartEntity<?>)) {
                    push(Body, Centre, Beam ? BeamPush : SplashPush * Amount / Math.max(1.0F, Values.SplashDamage()));
                }
            }
        }
        return Hits;
    }

    private static void push(Entity Target, Vec3 Centre, double Strength) {
        double Resist = Target instanceof LivingEntity Living ? Living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) : 0.0;
        double Scale = Strength * Math.max(0.0, 1.0 - Resist);
        if (Scale <= 0.0) {
            return;
        }
        Vec3 Away = new Vec3(Target.getX() - Centre.x, 0.0, Target.getZ() - Centre.z);
        Away = Away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : Away.normalize();
        Target.push(Away.x * Scale, PushLift * Math.max(0.0, 1.0 - Resist), Away.z * Scale);
        Target.hurtMarked = true;
    }

    public static boolean guarded(ServerLevel World, BlockPos Pos, BlockState State) {
        if (State.getDestroySpeed(World, Pos) < 0.0F || State.hasBlockEntity() || State.getBlock().getExplosionResistance() >= ProofResistance) {
            return true;
        }
        return State.is(BlockTags.WITHER_IMMUNE) || State.is(BlockTags.DRAGON_IMMUNE) || State.is(OrbitalRegistry.StrikeProof);
    }

    public static boolean natural(BlockState State) {
        return State.is(BlockTags.DIRT) || State.is(BlockTags.BASE_STONE_OVERWORLD) || State.is(BlockTags.SAND) || State.is(Tags.Blocks.GRAVELS)
                || State.is(BlockTags.TERRACOTTA) || State.is(Blocks.CLAY) || State.is(Blocks.SNOW_BLOCK) || State.is(Blocks.MUD);
    }

    public static boolean inside(int Dx, int Dy, int Dz, int Radius, double Jitter) {
        double Depth = Math.max(1.0, Radius * DepthShare);
        double Flat = Dx * Dx + Dz * Dz;
        double Shape = Dy <= 0 ? Flat / (Radius * Radius) + Dy * Dy / (Depth * Depth) : (Flat + Dy * Dy * 0.5) / (Radius * Radius);
        return Shape <= 1.0 + Jitter;
    }

    public static int carve(ServerLevel World, BlockPos Centre, int Radius, @Nullable Player Owner, RandomSource Random) {
        int Depth = (int) Math.ceil(Radius * DepthShare) + 1;
        int Count = 0;
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Dy = Radius; Dy >= -Depth; Dy--) {
            for (int Dx = -Radius; Dx <= Radius; Dx++) {
                for (int Dz = -Radius; Dz <= Radius; Dz++) {
                    double Jitter = (Random.nextDouble() - 0.5) * 0.24;
                    if (!inside(Dx, Dy, Dz, Radius, Jitter)) {
                        continue;
                    }
                    Cursor.set(Centre.getX() + Dx, Centre.getY() + Dy, Centre.getZ() + Dz);
                    if (!World.isInWorldBounds(Cursor)) {
                        continue;
                    }
                    BlockState State = World.getBlockState(Cursor);
                    if (State.isAir() || State.getBlock() instanceof LiquidBlock || guarded(World, Cursor, State)) {
                        continue;
                    }
                    if (Owner != null && !World.mayInteract(Owner, Cursor)) {
                        continue;
                    }
                    World.setBlock(Cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    Count++;
                }
            }
        }
        scorch(World, Centre, Radius, Depth, Owner, Random);
        return Count;
    }

    private static void scorch(ServerLevel World, BlockPos Centre, int Radius, int Depth, @Nullable Player Owner, RandomSource Random) {
        int Reach = Radius + 1;
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Dx = -Reach; Dx <= Reach; Dx++) {
            for (int Dz = -Reach; Dz <= Reach; Dz++) {
                if (Dx * Dx + Dz * Dz > Reach * Reach) {
                    continue;
                }
                for (int Dy = Radius; Dy >= -Depth - 1; Dy--) {
                    Cursor.set(Centre.getX() + Dx, Centre.getY() + Dy, Centre.getZ() + Dz);
                    BlockState State = World.getBlockState(Cursor);
                    if (State.isAir()) {
                        continue;
                    }
                    BlockPos Above = Cursor.above();
                    if (!World.getBlockState(Above).isAir() || !State.isFaceSturdy(World, Cursor, Direction.UP)) {
                        break;
                    }
                    if (natural(State) && !guarded(World, Cursor, State) && (Owner == null || World.mayInteract(Owner, Cursor))) {
                        float Roll = Random.nextFloat();
                        BlockState Burnt = Roll < MagmaChance ? Blocks.MAGMA_BLOCK.defaultBlockState() : Roll < BlackstoneChance ? Blocks.BLACKSTONE.defaultBlockState()
                                : Roll < BasaltChance ? Blocks.BASALT.defaultBlockState() : null;
                        if (Burnt != null) {
                            World.setBlock(Cursor, Burnt, Block.UPDATE_ALL);
                        }
                    }
                    if (Random.nextFloat() < FireChance && (Owner == null || World.mayInteract(Owner, Above))) {
                        BlockState Flame = BaseFireBlock.getState(World, Above);
                        if (Flame.canSurvive(World, Above)) {
                            World.setBlock(Above, Flame, Block.UPDATE_ALL);
                        }
                    }
                    break;
                }
            }
        }
    }
}
