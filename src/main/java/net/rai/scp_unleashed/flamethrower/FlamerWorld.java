package net.rai.scp_unleashed.flamethrower;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

public final class FlamerWorld {
    public static final int CoreHeat = 2;
    public static final int RimHeat = 1;
    public static final int BurnHeat = 6;
    public static final int CoolTicks = 30;
    public static final int MaxFiresPerPulse = 2;
    private static final Map<ServerLevel, Map<BlockPos, Heat>> Heats = new WeakHashMap<>();

    private FlamerWorld() {
    }

    private record Heat(int Level, long Tick) {
    }

    public static boolean allowed(ServerLevel World, @Nullable Entity User, FlamerConfig.FireRule Rule) {
        boolean Griefing = World.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        return switch (Rule) {
            case Off -> false;
            case MobGriefing -> Griefing;
            case Flint -> User instanceof Player || Griefing;
        };
    }

    public static boolean mayEdit(ServerLevel World, @Nullable Entity User, BlockPos Pos, Direction Face) {
        if (!World.isLoaded(Pos) || !World.getWorldBorder().isWithinBounds(Pos)) {
            return false;
        }
        if (User instanceof Player Person) {
            return Person.mayBuild() && World.mayInteract(Person, Pos) && Person.mayUseItemAt(Pos, Face, Person.getMainHandItem());
        }
        return true;
    }

    public static boolean placeFire(ServerLevel World, @Nullable Entity User, BlockPos Target, Direction Face) {
        if (!World.getBlockState(Target).isAir() || !mayEdit(World, User, Target, Face) || !BaseFireBlock.canBePlacedAt(World, Target, Face)) {
            return false;
        }
        World.setBlock(Target, BaseFireBlock.getState(World, Target), 11);
        World.gameEvent(User, GameEvent.BLOCK_PLACE, Target);
        return true;
    }

    public static boolean light(ServerLevel World, @Nullable Entity User, BlockPos Pos, Direction Face) {
        BlockState State = World.getBlockState(Pos);
        if (!CampfireBlock.canLight(State) && !CandleBlock.canLight(State) && !CandleCakeBlock.canLight(State)) {
            return false;
        }
        if (!mayEdit(World, User, Pos, Face)) {
            return false;
        }
        World.setBlock(Pos, State.setValue(BlockStateProperties.LIT, true), 11);
        World.gameEvent(User, GameEvent.BLOCK_CHANGE, Pos);
        return true;
    }

    public static int scorch(ServerLevel World, @Nullable Entity User, BlockHitResult Surface, long Now, FlamerConfig.FireRule Rule, boolean Flesh,
                             Predicate<BlockState> Burnable) {
        if (!allowed(World, User, Rule)) {
            return 0;
        }
        BlockPos Struck = Surface.getBlockPos();
        Direction Face = Surface.getDirection();
        int Burned = 0;
        if (Flesh) {
            Burned += heat(World, User, Struck, Face, CoreHeat, Now, Burnable);
            for (Direction Side : Direction.values()) {
                Burned += heat(World, User, Struck.relative(Side), Side, RimHeat, Now, Burnable);
            }
        }
        if (Burned == 0 && !Burnable.test(World.getBlockState(Struck)) && !light(World, User, Struck, Face)) {
            placeFire(World, User, Struck.relative(Face), Face);
        }
        return Burned;
    }

    public static boolean drip(ServerLevel World, @Nullable Entity User, Vec3 Point, FlamerConfig.FireRule Rule) {
        if (!allowed(World, User, Rule)) {
            return false;
        }
        BlockPos Pos = BlockPos.containing(Point);
        for (int Drop = 0; Drop < 3; Drop++) {
            BlockPos Below = Pos.below();
            BlockState Floor = World.getBlockState(Below);
            if (!Floor.isAir()) {
                return World.getBlockState(Pos).isAir() && placeFire(World, User, Pos, Direction.UP);
            }
            Pos = Below;
        }
        return false;
    }

    private static int heat(ServerLevel World, @Nullable Entity User, BlockPos Pos, Direction Face, int Amount, long Now, Predicate<BlockState> Burnable) {
        BlockState State = World.getBlockState(Pos);
        if (!Burnable.test(State) || !mayEdit(World, User, Pos, Face)) {
            return 0;
        }
        Map<BlockPos, Heat> Hot = Heats.computeIfAbsent(World, Key -> new HashMap<>());
        Heat Old = Hot.get(Pos);
        int Level = (Old == null || Now - Old.Tick() > CoolTicks ? 0 : Old.Level()) + Amount;
        if (Level < BurnHeat) {
            Hot.put(Pos.immutable(), new Heat(Level, Now));
            World.destroyBlockProgress(progressId(Pos), Pos, Math.min(9, Level * 10 / BurnHeat));
            return 0;
        }
        Hot.remove(Pos);
        burn(World, User, Pos);
        return 1;
    }

    public static void burn(ServerLevel World, @Nullable Entity User, BlockPos Pos) {
        World.destroyBlockProgress(progressId(Pos), Pos, -1);
        World.setBlock(Pos, Blocks.AIR.defaultBlockState(), 3);
        World.gameEvent(User, GameEvent.BLOCK_DESTROY, Pos);
        Vec3 Centre = Vec3.atCenterOf(Pos);
        World.sendParticles(ParticleTypes.LARGE_SMOKE, Centre.x, Centre.y, Centre.z, 6, 0.3, 0.3, 0.3, 0.02);
        World.sendParticles(ParticleTypes.FLAME, Centre.x, Centre.y, Centre.z, 8, 0.35, 0.35, 0.35, 0.03);
        World.playSound(null, Centre.x, Centre.y, Centre.z, FlamerRegistry.Sizzle.get(), SoundSource.BLOCKS, 0.8F, 0.9F + World.random.nextFloat() * 0.2F);
        BlockState Floor = World.getBlockState(Pos.below());
        if (!Floor.isAir() && BaseFireBlock.canBePlacedAt(World, Pos, Direction.UP)) {
            World.setBlock(Pos, BaseFireBlock.getState(World, Pos), 11);
        }
    }

    public static int progressId(BlockPos Pos) {
        long Packed = Pos.asLong();
        return 0x5F1A0000 ^ (int) (Packed ^ (Packed >>> 32));
    }

    public static void tick(ServerLevel World, long Now) {
        Map<BlockPos, Heat> Hot = Heats.get(World);
        if (Hot == null || Hot.isEmpty() || Now % 10 != 0) {
            return;
        }
        Iterator<Map.Entry<BlockPos, Heat>> Entries = Hot.entrySet().iterator();
        while (Entries.hasNext()) {
            Map.Entry<BlockPos, Heat> Entry = Entries.next();
            if (Now - Entry.getValue().Tick() > CoolTicks) {
                World.destroyBlockProgress(progressId(Entry.getKey()), Entry.getKey(), -1);
                Entries.remove();
            }
        }
    }

    public static void clear() {
        Heats.clear();
    }
}
