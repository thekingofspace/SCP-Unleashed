package net.rai.scp_unleashed.scp354;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

public final class Scp354Emergence {
    public static final int FirstMin = 3600;
    public static final int FirstMax = 7200;
    public static final int IntervalMin = 7200;
    public static final int IntervalMax = 14400;
    public static final int ChurnTicks = 100;
    public static final int Cap = 4;
    public static final double CapRange = 96.0;
    public static final long CatchUpAfter = 12000L;
    public static final long CatchUpDouble = 36000L;
    public static final int SaveEvery = 100;
    public static final double GenericRise = 0.55;
    public static final double Sunk = 1.0;

    private Scp354Emergence() {
    }

    public static int firstTimer(RandomSource Dice) {
        return FirstMin + Dice.nextInt(FirstMax - FirstMin + 1);
    }

    public static int interval(RandomSource Dice) {
        return IntervalMin + Dice.nextInt(IntervalMax - IntervalMin + 1);
    }

    public static void tick(ServerLevel World) {
        Scp354Pools Data = Scp354Pools.get(World.getServer());
        if (Data.pools().isEmpty()) {
            return;
        }
        long Now = World.getGameTime();
        for (Scp354Pools.Pool Entry : Data.in(World.dimension())) {
            if (!World.isLoaded(Entry.Centre)) {
                continue;
            }
            tickPool(World, Data, Entry, Now);
        }
    }

    public static void watch(ServerLevel World, Scp354Pools.Pool Entry) {
        AABB Area = new AABB(Entry.Centre.getX() - Entry.Radius - 1.5, Entry.Centre.getY() - Entry.Depth - 1.0, Entry.Centre.getZ() - Entry.Radius - 1.5,
                Entry.Centre.getX() + Entry.Radius + 2.5, Entry.Centre.getY() + 1.0, Entry.Centre.getZ() + Entry.Radius + 2.5);
        for (LivingEntity Found : World.getEntitiesOfClass(LivingEntity.class, Area, Scp354Grab::isEligible)) {
            BlockPos Cell = BlockPos.containing(Found.getX(), Found.getY() + 0.05, Found.getZ());
            if (Scp354Fluid.isRed(World.getFluidState(Cell))) {
                Scp354Grab.touch(World, Found, Cell);
            }
        }
    }

    public static void tickPool(ServerLevel World, Scp354Pools Data, Scp354Pools.Pool Entry, long Now) {
        watch(World, Entry);
        if (Now - Entry.LastSeen >= CatchUpAfter) {
            catchUp(World, Entry, Now - Entry.LastSeen);
            Entry.Timer = interval(World.getRandom());
            Data.setDirty();
        }
        Entry.LastSeen = Now;
        if (Now % SaveEvery == 0) {
            Data.setDirty();
        }
        Entry.Timer--;
        if (Entry.Timer == ChurnTicks) {
            Entry.Tops = tops(World, Entry);
            Scp354Sounds.play(World, Vec3.atCenterOf(Entry.Centre), Scp354Registry.GurgleLayers, 1.6f, 0.6f);
        }
        if (Entry.Timer > 0 && Entry.Timer <= ChurnTicks) {
            churn(World, Entry, Entry.Timer);
        }
        if (Entry.Timer <= 0) {
            if (hasRoom(World, Entry)) {
                emerge(World, Entry, null);
            }
            Entry.Timer = interval(World.getRandom());
            Entry.Tops = List.of();
            Data.setDirty();
        }
    }

    public static int catchUp(ServerLevel World, Scp354Pools.Pool Entry, long Away) {
        int Wanted = Away >= CatchUpDouble ? 2 : 1;
        int Made = 0;
        for (int Index = 0; Index < Wanted; Index++) {
            if (!hasRoom(World, Entry) || emerge(World, Entry, null).isEmpty()) {
                break;
            }
            Made++;
        }
        return Made;
    }

    public static int alive(ServerLevel World, Scp354Pools.Pool Entry) {
        AABB Area = new AABB(Entry.Centre).inflate(CapRange);
        return World.getEntitiesOfClass(LivingEntity.class, Area, Found -> Found.isAlive() && Scp354Prey.isKin(Found)).size();
    }

    public static boolean hasRoom(ServerLevel World, Scp354Pools.Pool Entry) {
        return alive(World, Entry) < Cap;
    }

    public static List<BlockPos> tops(ServerLevel World, Scp354Pools.Pool Entry) {
        Set<BlockPos> Liquid = Scp354Basin.scan(World, Entry.Centre, Entry.Radius + 1, Entry.Depth + 2, 1);
        List<BlockPos> Found = new ArrayList<>();
        for (BlockPos Cell : Liquid) {
            if (!Liquid.contains(Cell.above())) {
                Found.add(Cell);
            }
        }
        return Found;
    }

    private static void churn(ServerLevel World, Scp354Pools.Pool Entry, int Left) {
        if (Entry.Tops.isEmpty()) {
            Entry.Tops = tops(World, Entry);
            if (Entry.Tops.isEmpty()) {
                return;
            }
        }
        RandomSource Dice = World.getRandom();
        double Strength = 1.0 - Left / (double) ChurnTicks;
        if (Left % 3 == 0) {
            for (int Index = 0; Index < 2 + (int) (Strength * 6); Index++) {
                BlockPos Cell = Entry.Tops.get(Dice.nextInt(Entry.Tops.size()));
                World.sendParticles(Scp354Registry.Bubble.get(), Cell.getX() + 0.5, Cell.getY() + 0.2, Cell.getZ() + 0.5, 2, 0.3, 0.25, 0.3, 0.0);
                World.sendParticles(Scp354Registry.Ripple.get(), Cell.getX() + 0.2 + Dice.nextDouble() * 0.6, Cell.getY() + Scp354Fluid.SurfaceHeight + 0.012,
                        Cell.getZ() + 0.2 + Dice.nextDouble() * 0.6, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        if (Left % 25 == 0) {
            Scp354Sounds.play(World, Vec3.atCenterOf(Entry.Centre), Scp354Registry.ChurnLayers, 1.0f + (float) Strength, 0.65f + Dice.nextFloat() * 0.15f);
        }
        if (Left == 40) {
            Scp354Sounds.play(World, Vec3.atCenterOf(Entry.Centre), Scp354Registry.BubbleLayers, 1.4f, 0.55f);
        }
    }

    @Nullable
    public static EntityType<?> pick(ServerLevel World, RandomSource Dice) {
        Optional<HolderSet.Named<EntityType<?>>> Tagged = World.registryAccess().registryOrThrow(Registries.ENTITY_TYPE).getTag(Scp354Registry.Spawns);
        if (Tagged.isEmpty() || Tagged.get().size() == 0) {
            return null;
        }
        List<Holder<EntityType<?>>> Choices = new ArrayList<>();
        Tagged.get().forEach(Choices::add);
        return Choices.get(Dice.nextInt(Choices.size())).value();
    }

    public static Vec3 spotFor(ServerLevel World, Scp354Pools.Pool Entry, EntityType<?> Type) {
        List<BlockPos> Tops = new ArrayList<>(tops(World, Entry));
        if (Tops.isEmpty()) {
            return Vec3.atBottomCenterOf(Entry.Centre.above());
        }
        Collections.shuffle(Tops, new Random(World.getRandom().nextLong()));
        BlockPos Best = null;
        int BestDepth = -1;
        for (BlockPos Top : Tops) {
            int Depth = 0;
            while (Scp354Fluid.isRed(World.getFluidState(Top.below(Depth + 1))) && Depth < 8) {
                Depth++;
            }
            Vec3 Spot = new Vec3(Top.getX() + 0.5, Top.getY() - Math.min(Sunk, Depth), Top.getZ() + 0.5);
            if (World.noCollision(Type.getDimensions().makeBoundingBox(Spot))) {
                return Spot;
            }
            if (Depth > BestDepth) {
                BestDepth = Depth;
                Best = Top;
            }
        }
        return new Vec3(Best.getX() + 0.5, Best.getY() - Math.min(Sunk, BestDepth), Best.getZ() + 0.5);
    }

    public static Optional<Entity> emerge(ServerLevel World, Scp354Pools.Pool Entry, @Nullable EntityType<?> Wanted) {
        EntityType<?> Type = Wanted != null ? Wanted : pick(World, World.getRandom());
        if (Type == null) {
            return Optional.empty();
        }
        Vec3 Spot = spotFor(World, Entry, Type);
        Entity Made = Type.create(World);
        if (Made == null) {
            return Optional.empty();
        }
        Made.moveTo(Spot.x, Spot.y, Spot.z, World.getRandom().nextFloat() * 360.0f, 0.0f);
        if (Made instanceof Mob Creature) {
            EventHooks.finalizeMobSpawn(Creature, World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), MobSpawnType.EVENT, null);
            Creature.setPersistenceRequired();
            Creature.setYHeadRot(Made.getYRot());
            Creature.setYBodyRot(Made.getYRot());
            if (Creature instanceof Scp354Swimmer Swimmer && Swimmer.swimsOut()) {
                faceExit(Creature, Swimmer);
            }
        }
        Made.setData(Scp354Registry.PoolBorn.get(), true);
        if (!World.addFreshEntity(Made)) {
            return Optional.empty();
        }
        if (Made instanceof Scp354Emergent Emergent) {
            Emergent.beginEmerge();
        } else {
            Made.setDeltaMovement(0.0, GenericRise, 0.0);
            Made.hurtMarked = true;
            Scp354Sounds.play(World, Spot, Scp354Registry.EmergeLayers, 1.6f, 0.8f + World.getRandom().nextFloat() * 0.15f);
            World.sendParticles(Scp354Registry.Splash.get(), Spot.x, Spot.y + Sunk + 0.2, Spot.z, 30, Made.getBbWidth() * 0.5, 0.15, Made.getBbWidth() * 0.5, 0.25);
        }
        Entry.Born++;
        Scp354Pools.get(World.getServer()).setDirty();
        return Optional.of(Made);
    }

    public static void faceExit(Mob Creature, Scp354Swimmer Swimmer) {
        Vec3 Exit = Scp354Swim.exit(Creature, Swimmer, null);
        if (Exit == null) {
            return;
        }
        float Yaw = Mth.wrapDegrees((float) (Math.atan2(Exit.z - Creature.getZ(), Exit.x - Creature.getX()) * 180.0 / Math.PI) - 90.0f);
        Creature.setYRot(Yaw);
        Creature.yRotO = Yaw;
        Creature.setYHeadRot(Yaw);
        Creature.setYBodyRot(Yaw);
        Swimmer.course().Exit = Exit;
    }

    public static boolean isPoolBorn(Entity Found) {
        return Boolean.TRUE.equals(Found.getExistingDataOrNull(Scp354Registry.PoolBorn.get()));
    }
}
