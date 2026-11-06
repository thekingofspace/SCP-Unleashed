package net.rai.scp_unleashed.scp063;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

public final class Scp063Scrubbing {
    public static final int StrokeTicks = 10;
    public static final int SoundInterval = 5;
    public static final int BaseTicks = 20;
    public static final float TicksPerHardness = 12.0f;
    public static final int MaxTicks = 240;
    public static final int Stages = 10;
    private static final Map<Player, Progress> Working = new WeakHashMap<>();

    private Scp063Scrubbing() {
    }

    public static boolean isScrubbing(LivingEntity Holder) {
        return Holder.getExistingData(Scp063Registry.Scrubbing).orElse(Boolean.FALSE);
    }

    public static boolean scrubbingNow(LivingEntity Holder) {
        return Scp063ToothbrushItem.isBrushing(Holder) && isScrubbing(Holder);
    }

    public static void mark(LivingEntity Holder, boolean Scrubbing) {
        if (Scrubbing) {
            Holder.setData(Scp063Registry.Scrubbing, Boolean.TRUE);
        } else if (Holder.hasData(Scp063Registry.Scrubbing)) {
            Holder.removeData(Scp063Registry.Scrubbing);
        }
    }

    @Nullable
    public static BlockHitResult aim(Player User) {
        HitResult Hit = ProjectileUtil.getHitResultOnViewVector(User, Target -> !Target.isSpectator() && Target.isPickable(), User.blockInteractionRange());
        return Hit instanceof BlockHitResult Aimed && Hit.getType() == HitResult.Type.BLOCK ? Aimed : null;
    }

    public static void begin(Player User, InteractionHand Hand) {
        mark(User, true);
        User.startUsingItem(Hand);
    }

    public static int ticksFor(Level World, BlockPos Pos, BlockState State) {
        float Hardness = Math.max(0.0f, State.getDestroySpeed(World, Pos));
        return Mth.clamp(Math.round(BaseTicks + Hardness * TicksPerHardness), BaseTicks, MaxTicks);
    }

    public static void tick(Level World, Player User, int TicksUsing) {
        BlockHitResult Hit = aim(User);
        if (Hit == null) {
            User.releaseUsingItem();
            return;
        }
        BlockPos Pos = Hit.getBlockPos();
        BlockState State = World.getBlockState(Pos);
        if (TicksUsing % SoundInterval == 0) {
            stroke(World, User, Hit, State);
        }
        if (User instanceof ServerPlayer Server) {
            work(Server, Pos, State);
        }
    }

    private static void work(ServerPlayer User, BlockPos Pos, BlockState State) {
        ServerLevel World = User.serverLevel();
        Progress Current = Working.get(User);
        if (Current != null && (!Current.Pos().equals(Pos) || Current.State() != State)) {
            stop(User);
            Current = null;
        }
        if (!Scp063Erosion.erodible(World, Pos, State) || !mayTry(User, Pos)) {
            return;
        }
        int Needed = ticksFor(World, Pos, State);
        int Ticks = (Current == null ? 0 : Current.Ticks()) + 1;
        int Stage = Math.min(Stages - 1, Ticks * Stages / Needed);
        if (Current == null || Stage != Current.Stage()) {
            World.destroyBlockProgress(crackId(User), Pos, Stage);
        }
        if (Ticks < Needed) {
            Working.put(User, new Progress(Pos.immutable(), State, Ticks, Stage));
            return;
        }
        stop(User);
        if (permitted(User, Pos, State)) {
            Scp063Erosion.wipe(World, Pos, State, User);
            World.playSound(null, Pos, Scp063Registry.Scrubbed.get(), SoundSource.BLOCKS, 0.7f, 0.95f + World.getRandom().nextFloat() * 0.1f);
        }
    }

    public static boolean mayTry(ServerPlayer User, BlockPos Pos) {
        ServerLevel World = User.serverLevel();
        return World.mayInteract(User, Pos) && !User.blockActionRestricted(World, Pos, User.gameMode.getGameModeForPlayer());
    }

    public static boolean permitted(ServerPlayer User, BlockPos Pos, BlockState State) {
        if (!mayTry(User, Pos)) {
            return false;
        }
        GameType Mode = User.gameMode.getGameModeForPlayer();
        return !CommonHooks.fireBlockBreak(User.serverLevel(), Mode, User, Pos, State).isCanceled();
    }

    public static int crackId(Player User) {
        return ~User.getId();
    }

    public static void stop(Player User) {
        Progress Current = Working.remove(User);
        if (Current != null && User.level() instanceof ServerLevel World) {
            World.destroyBlockProgress(crackId(User), Current.Pos(), -1);
        }
    }

    public static boolean working(Player User) {
        return Working.containsKey(User);
    }

    public static int progress(Player User) {
        Progress Current = Working.get(User);
        return Current == null ? 0 : Current.Ticks();
    }

    private static void stroke(Level World, Player User, BlockHitResult Hit, BlockState State) {
        RandomSource Dice = World.getRandom();
        float Pitch = 0.9f + Dice.nextFloat() * 0.2f;
        World.playSound(User, Hit.getBlockPos(), Scp063Registry.Scrub.get(), SoundSource.BLOCKS, 0.55f, Pitch);
        if (!World.isClientSide) {
            return;
        }
        Vec3 Point = Hit.getLocation();
        Direction Side = Hit.getDirection();
        if (State.shouldSpawnTerrainParticles() && State.getRenderShape() != RenderShape.INVISIBLE) {
            HumanoidArm Arm = User.getUsedItemHand() == InteractionHand.MAIN_HAND ? User.getMainArm() : User.getMainArm().getOpposite();
            int Hand = Arm == HumanoidArm.RIGHT ? 1 : -1;
            Vec3 Look = User.getViewVector(0.0f);
            double SweepX = Side.getAxis() == Direction.Axis.Y ? Look.z : Side.getStepZ() != 0 ? -Side.getStepZ() : -0.1 * Side.getStepX();
            double SweepZ = Side.getAxis() == Direction.Axis.Y ? -Look.x : Side.getStepX() != 0 ? Side.getStepX() : 0.1 * Side.getStepZ();
            BlockParticleOption Dust = new BlockParticleOption(ParticleTypes.BLOCK, State);
            int Count = 5 + Dice.nextInt(4);
            for (int Index = 0; Index < Count; Index++) {
                double Push = Hand * 3.0 * Dice.nextDouble();
                World.addParticle(Dust, Point.x, Point.y, Point.z, SweepX * Push, 0.0, SweepZ * Push);
            }
        }
        for (int Index = 0; Index < 2; Index++) {
            World.addParticle(Scp063Registry.FoamSpeck.get(), Point.x + Side.getStepX() * 0.03, Point.y + Side.getStepY() * 0.03, Point.z + Side.getStepZ() * 0.03,
                    Dice.nextGaussian() * 0.02, 0.02, Dice.nextGaussian() * 0.02);
        }
    }

    private record Progress(BlockPos Pos, BlockState State, int Ticks, int Stage) {
    }
}
