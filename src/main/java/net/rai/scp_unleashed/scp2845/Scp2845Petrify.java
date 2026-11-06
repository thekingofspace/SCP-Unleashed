package net.rai.scp_unleashed.scp2845;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class Scp2845Petrify {
    public static final int MaxHeight = 4;

    private Scp2845Petrify() {
    }

    public static int heightOf(LivingEntity Victim) {
        return Mth.clamp(Mth.ceil(Victim.getBbHeight() - 0.05f), 1, MaxHeight);
    }

    public static boolean petrify(ServerLevel World, LivingEntity Victim, @Nullable Entity Source) {
        if (Victim instanceof Player User && (User.isCreative() || User.isSpectator())) {
            return false;
        }
        BlockPos Base = Victim.blockPosition();
        int Height = heightOf(Victim);
        Scp2845Sounds.play(World, Victim.position(), Scp2845Registry.PetrifyLayers, 1.2f, 0.8f);
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Scp2845Registry.Pillar.get().defaultBlockState()), Victim.getX(), Victim.getY() + Victim.getBbHeight() * 0.5,
                Victim.getZ(), 40, 0.4, Victim.getBbHeight() * 0.4, 0.4, 0.1);
        if (Victim instanceof Player) {
            Victim.hurt(World.damageSources().source(Scp2845Registry.Petrified, Source), Float.MAX_VALUE);
            if (Victim.isAlive()) {
                return false;
            }
            raise(World, Base, Height);
            return true;
        }
        raise(World, Base, Height);
        Victim.discard();
        return true;
    }

    public static boolean fits(ServerLevel World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        return State.isAir() || State.canBeReplaced() && (State.getFluidState().isEmpty() || State.getFluidState().is(Fluids.WATER));
    }

    public static List<BlockPos> raise(ServerLevel World, BlockPos Base, int Height) {
        List<BlockPos> Placed = new ArrayList<>();
        BlockPos Start = Base;
        if (!fits(World, Start) && fits(World, Start.above())) {
            Start = Start.above();
        }
        for (int Index = 0; Index < Height; Index++) {
            BlockPos Pos = Start.above(Index);
            if (!fits(World, Pos)) {
                break;
            }
            World.setBlock(Pos, Scp2845Registry.Pillar.get().defaultBlockState(), Block.UPDATE_ALL);
            Placed.add(Pos.immutable());
        }
        for (BlockPos Pos : Placed) {
            BlockState State = World.getBlockState(Pos);
            boolean Below = World.getBlockState(Pos.below()).is(Scp2845Registry.Pillar.get());
            boolean Above = World.getBlockState(Pos.above()).is(Scp2845Registry.Pillar.get());
            World.setBlock(Pos, State.setValue(Scp2845PillarBlock.Part, Scp2845PillarBlock.segmentFor(Below, Above)), Block.UPDATE_CLIENTS);
        }
        return Placed;
    }
}
