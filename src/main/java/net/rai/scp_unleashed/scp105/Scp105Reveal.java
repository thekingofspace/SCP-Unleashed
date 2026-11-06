package net.rai.scp_unleashed.scp105;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.Optional;

public final class Scp105Reveal {
    public static final double PhotoReach = 0.55;
    public static final double ItemReach = 1.3;
    public static final double[] CreatureReach = {2.2, 1.7, 2.8, 3.4};
    public static final float[] CreatureTurns = {0.0f, 25.0f, -25.0f, 55.0f, -55.0f, 90.0f, -90.0f};
    public static final int[] Lifts = {0, 1, -1, 2, -2};
    public static final int ShredCount = 14;
    public static final int ScrapCount = 10;
    public static final int FlashCount = 1;
    public static final int PoofCount = 8;

    private Scp105Reveal() {
    }

    public static Vec3 ahead(Vec3 Origin, float Yaw, double Reach) {
        Vec3 Facing = Vec3.directionFromRotation(0.0f, Yaw);
        return Origin.add(Facing.x * Reach, 0.0, Facing.z * Reach);
    }

    public static void crumble(ServerLevel World, Vec3 Where, ItemStack Photo) {
        World.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Photo.copyWithCount(1)), Where.x, Where.y, Where.z, ShredCount, 0.1, 0.1, 0.1, 0.06);
        World.sendParticles(Scp105Registry.Scrap.get(), Where.x, Where.y, Where.z, ScrapCount, 0.14, 0.08, 0.14, 0.02);
        Scp105Sounds.play(World, Where, Scp105Registry.TearLayers, SoundSource.PLAYERS, 0.9f, 0.95f + World.getRandom().nextFloat() * 0.1f);
    }

    public static boolean appear(ServerLevel World, Vec3 Eye, Vec3 Feet, float Yaw, Scp105Photo Photo) {
        if (Photo.Subject() == Scp105Photo.Kind.Thing) {
            Item Thing = BuiltInRegistries.ITEM.get(Photo.Id());
            if (Thing == Items.AIR) {
                return true;
            }
            Vec3 Spot = clearAhead(World, Eye.subtract(0.0, 0.35, 0.0), Yaw, ItemReach);
            ItemEntity Drop = new ItemEntity(World, Spot.x, Spot.y, Spot.z, new ItemStack(Thing, Photo.Count()));
            Vec3 Back = Vec3.directionFromRotation(0.0f, Yaw).scale(-0.06);
            Drop.setDeltaMovement(Back.x, 0.12, Back.z);
            Drop.setPickUpDelay(10);
            World.addFreshEntity(Drop);
            arrived(World, Spot);
            return true;
        }
        Optional<EntityType<?>> Found = BuiltInRegistries.ENTITY_TYPE.getOptional(Photo.Id());
        if (Found.isEmpty() || !Scp105Subjects.allowedCreature(Found.get())) {
            return true;
        }
        EntityType<?> Type = Found.get();
        Optional<Vec3> Spot = findSpot(World, Eye, Feet, Yaw, Type);
        if (Spot.isEmpty()) {
            return false;
        }
        Entity Made = Type.create(World);
        if (!(Made instanceof Mob Body)) {
            if (Made != null) {
                Made.discard();
            }
            return true;
        }
        float Facing = Yaw + 180.0f;
        Vec3 At = Spot.get();
        Body.moveTo(At.x, At.y, At.z, Facing, 0.0f);
        Body.setYHeadRot(Facing);
        Body.setYBodyRot(Facing);
        Body.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(At)), MobSpawnType.MOB_SUMMONED, null);
        if (Photo.Young() && Body instanceof AgeableMob Growing) {
            Growing.setBaby(true);
        }
        if (Body instanceof Slime Blob) {
            Blob.setSize(1, true);
        }
        if (!World.tryAddFreshEntityWithPassengers(Body)) {
            return true;
        }
        arrived(World, At.add(0.0, Body.getBbHeight() * 0.5, 0.0));
        World.sendParticles(ParticleTypes.POOF, At.x, At.y + Body.getBbHeight() * 0.5, At.z, PoofCount, Body.getBbWidth() * 0.4, Body.getBbHeight() * 0.3, Body.getBbWidth() * 0.4, 0.01);
        return true;
    }

    private static void arrived(ServerLevel World, Vec3 Where) {
        World.sendParticles(Scp105Registry.Flash.get(), Where.x, Where.y, Where.z, FlashCount, 0.0, 0.0, 0.0, 0.0);
        Scp105Sounds.play(World, Where, Scp105Registry.AppearLayers, SoundSource.PLAYERS, 0.8f, 0.95f + World.getRandom().nextFloat() * 0.1f);
    }

    public static Vec3 clearAhead(ServerLevel World, Vec3 From, float Yaw, double Reach) {
        Vec3 Target = ahead(From, Yaw, Reach);
        HitResult Hit = World.clip(new ClipContext(From, Target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        if (Hit.getType() == HitResult.Type.MISS) {
            return Target;
        }
        Vec3 Toward = Target.subtract(From);
        double Length = Toward.length();
        double Keep = Math.max(0.0, From.distanceTo(Hit.getLocation()) - 0.3);
        return Length < 1.0e-4 ? From : From.add(Toward.scale(Keep / Length));
    }

    public static Optional<Vec3> findSpot(ServerLevel World, Vec3 Eye, Vec3 Feet, float Yaw, EntityType<?> Type) {
        for (double Reach : CreatureReach) {
            for (float Turn : CreatureTurns) {
                Vec3 Target = ahead(Feet, Yaw + Turn, Reach);
                Vec3 Sight = new Vec3(Target.x, Eye.y, Target.z);
                HitResult Hit = World.clip(new ClipContext(Eye, Sight, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
                if (Hit.getType() != HitResult.Type.MISS) {
                    continue;
                }
                for (int Lift : Lifts) {
                    Vec3 Base = new Vec3(Target.x, Mth.floor(Feet.y) + Lift, Target.z);
                    if (safe(World, Base, Type)) {
                        return Optional.of(Base);
                    }
                }
            }
        }
        return Optional.empty();
    }

    public static boolean safe(ServerLevel World, Vec3 Base, EntityType<?> Type) {
        BlockPos Pos = BlockPos.containing(Base);
        if (!World.isLoaded(Pos) || World.isOutsideBuildHeight(Pos)) {
            return false;
        }
        AABB Box = Type.getDimensions().makeBoundingBox(Base);
        if (!World.getWorldBorder().isWithinBounds(Box) || !World.noCollision(Box) || World.containsAnyLiquid(Box)) {
            return false;
        }
        BlockPos Below = Pos.below();
        if (!World.getBlockState(Below).isFaceSturdy(World, Below, Direction.UP)) {
            return false;
        }
        return World.getBlockStates(Box.inflate(0.4, 0.0, 0.4).expandTowards(0.0, -1.0, 0.0)).noneMatch(Scp105Reveal::hazard);
    }

    private static boolean hazard(BlockState State) {
        return State.is(BlockTags.FIRE) || State.is(BlockTags.CAMPFIRES) || State.is(Blocks.LAVA) || State.is(Blocks.MAGMA_BLOCK) || State.is(Blocks.CACTUS)
                || State.is(Blocks.SWEET_BERRY_BUSH) || State.is(Blocks.POWDER_SNOW) || State.is(Blocks.WITHER_ROSE) || State.is(Blocks.POINTED_DRIPSTONE);
    }
}
