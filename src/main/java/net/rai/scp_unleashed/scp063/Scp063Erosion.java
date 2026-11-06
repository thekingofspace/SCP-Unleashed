package net.rai.scp_unleashed.scp063;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

public final class Scp063Erosion {
    public static final int ErodeTicks = 15;
    public static final int GritInterval = 5;
    public static final int FallTicks = 2;
    public static final int RetryTicks = 200;
    public static final int[][] Layer = {{0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, 0}};
    public static final UUID AgentId = UUID.fromString("5c3a0630-0063-4063-8063-000000000063");
    public static final String AgentName = "[SCP-063]";

    private Scp063Erosion() {
    }

    public static boolean enabled(Level World) {
        return Scp063Registry.erosion(World.getGameRules());
    }

    public static boolean isOrganic(BlockState State) {
        return State.is(Scp063Registry.Organic);
    }

    public static boolean isUnbreakable(Level World, BlockPos Pos, BlockState State) {
        return State.is(Scp063Registry.Immune) || State.getDestroySpeed(World, Pos) < 0.0f;
    }

    public static boolean erodible(Level World, BlockPos Pos, BlockState State) {
        if (World.isOutsideBuildHeight(Pos) || !World.isLoaded(Pos)) {
            return false;
        }
        if (State.isAir() || !State.getFluidState().isEmpty() || State.hasBlockEntity()) {
            return false;
        }
        return !isOrganic(State) && !isUnbreakable(World, Pos, State);
    }

    public static boolean erodible(Level World, BlockPos Pos) {
        return erodible(World, Pos, World.getBlockState(Pos));
    }

    public static boolean canFallInto(Level World, BlockPos Pos) {
        return !World.isOutsideBuildHeight(Pos) && World.isLoaded(Pos) && World.getBlockState(Pos).isAir();
    }

    @Nullable
    public static BlockPos nextTarget(Level World, BlockPos Origin, Set<BlockPos> Refused) {
        BlockPos Below = Origin.below();
        if (!erodible(World, Below) || Refused.contains(Below)) {
            return null;
        }
        for (int[] Offset : Layer) {
            BlockPos Candidate = Below.offset(Offset[0], 0, Offset[1]);
            if (!Refused.contains(Candidate) && erodible(World, Candidate)) {
                return Candidate;
            }
        }
        return null;
    }

    public static FakePlayer agent(ServerLevel World, @Nullable UUID Owner, String OwnerName, BlockPos Where) {
        GameProfile Profile = Owner != null ? new GameProfile(Owner, OwnerName.isEmpty() ? AgentName : OwnerName) : new GameProfile(AgentId, AgentName);
        FakePlayer Agent = FakePlayerFactory.get(World, Profile);
        Agent.setPos(Where.getX() + 0.5, Where.getY(), Where.getZ() + 0.5);
        return Agent;
    }

    public static boolean permitted(ServerLevel World, FakePlayer Agent, BlockPos Pos, BlockState State) {
        if (!World.mayInteract(Agent, Pos)) {
            return false;
        }
        return !NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(World, Pos, State, Agent)).isCanceled();
    }

    public static void wipe(ServerLevel World, BlockPos Pos, BlockState State, @Nullable Entity Source) {
        World.removeBlock(Pos, false);
        World.gameEvent(GameEvent.BLOCK_DESTROY, Pos, GameEvent.Context.of(Source, State));
        crumble(World, Pos, State);
    }

    public static void crumble(ServerLevel World, BlockPos Pos, BlockState State) {
        Vec3 Centre = Vec3.atCenterOf(Pos);
        if (State.getRenderShape() != RenderShape.INVISIBLE) {
            World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, State), Centre.x, Centre.y, Centre.z, 14, 0.28, 0.28, 0.28, 0.02);
            World.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, State), Centre.x, Centre.y + 0.2, Centre.z, 8, 0.3, 0.2, 0.3, 0.0);
        }
        float Pitch = 0.9f + World.getRandom().nextFloat() * 0.2f;
        World.playSound(null, Centre.x, Centre.y, Centre.z, Scp063Registry.Scrub.get(), SoundSource.BLOCKS, 0.45f, Pitch);
        World.playSound(null, Centre.x, Centre.y, Centre.z, Scp063Registry.Grit.get(), SoundSource.BLOCKS, 0.35f, Pitch);
    }

    public static void grit(ServerLevel World, BlockPos Pos, BlockState State) {
        if (State.getRenderShape() == RenderShape.INVISIBLE) {
            return;
        }
        double Top = top(World, Pos, State);
        World.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, State), Pos.getX() + 0.5, Top + 0.05, Pos.getZ() + 0.5, 3, 0.25, 0.02, 0.25, 0.0);
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, State), Pos.getX() + 0.5, Top + 0.05, Pos.getZ() + 0.5, 2, 0.2, 0.02, 0.2, 0.01);
    }

    public static double top(Level World, BlockPos Pos, BlockState State) {
        VoxelShape Shape = State.getShape(World, Pos);
        return Pos.getY() + (Shape.isEmpty() ? 0.0 : Shape.max(Direction.Axis.Y));
    }

    public static boolean settle(ServerLevel World, Scp063RestingBlockEntity Old) {
        BlockPos From = Old.getBlockPos();
        BlockPos To = From.below();
        BlockState State = World.getBlockState(From);
        if (!(State.getBlock() instanceof Scp063RestingBlock) || !canFallInto(World, To)) {
            return false;
        }
        CompoundTag Saved = Old.saveCustomOnly(World.registryAccess());
        if (!World.setBlock(To, State, Block.UPDATE_ALL)) {
            return false;
        }
        if (!(World.getBlockEntity(To) instanceof Scp063RestingBlockEntity Fresh)) {
            World.setBlock(To, World.getFluidState(To).createLegacyBlock(), Block.UPDATE_ALL);
            return false;
        }
        Fresh.loadCustomOnly(Saved, World.registryAccess());
        Fresh.inherit(Old);
        Old.release();
        World.setBlock(From, World.getFluidState(From).createLegacyBlock(), Block.UPDATE_ALL);
        Fresh.setChanged();
        return true;
    }

    public static void landed(ServerLevel World, BlockPos Pos) {
        World.playSound(null, Pos.getX() + 0.5, Pos.getY() + 0.1, Pos.getZ() + 0.5, Scp063Registry.Settle.get(), SoundSource.BLOCKS, 0.7f,
                0.95f + World.getRandom().nextFloat() * 0.1f);
    }
}
