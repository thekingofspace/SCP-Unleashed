package net.rai.scp_unleashed.scp015;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

final class Scp015Front {
    static final int ShortestRun = 2;
    static final int RunSpread = 6;

    long Pos;
    @Nullable
    Direction Heading;
    int Run;
    int RunTarget;
    long NextTick;

    Scp015Front(long Pos, @Nullable Direction Heading, int RunTarget, long NextTick) {
        this.Pos = Pos;
        this.Heading = Heading;
        this.RunTarget = RunTarget;
        this.NextTick = NextTick;
    }

    static int rollRun(RandomSource Random) {
        return ShortestRun + Random.nextInt(RunSpread);
    }

    @Nullable
    Direction choose(ServerLevel World, BlockPos At) {
        RandomSource Random = World.random;
        Direction Best = null;
        double BestScore = Double.NEGATIVE_INFINITY;
        for (Direction Side : Direction.values()) {
            BlockPos Target = At.relative(Side);
            if (!Scp015Colony.growable(World, Target)) {
                continue;
            }
            BlockState TargetState = World.getBlockState(Target);
            if (!Scp015Growth.isFree(TargetState) || Scp015Colony.occupied(World, Target)) {
                continue;
            }
            double Score = Random.nextDouble() * 0.8;
            if (Heading != null) {
                if (Side == Heading) {
                    Score += Run < RunTarget ? 4.0 : 0.5;
                } else if (Side.getAxis() != Heading.getAxis()) {
                    Score += Run >= RunTarget ? 2.0 : 0.0;
                }
            }
            int Support = 0;
            int Crowd = 0;
            for (Direction Around : Direction.values()) {
                if (Around == Side.getOpposite()) {
                    continue;
                }
                BlockPos Near = Target.relative(Around);
                if (!World.isLoaded(Near)) {
                    continue;
                }
                BlockState NearState = World.getBlockState(Near);
                if (Scp015PipeBlock.isPipe(NearState)) {
                    Crowd++;
                } else if (!Scp015Growth.isFree(NearState)) {
                    Support++;
                }
            }
            Score += Support > 0 ? 0.8 : 0.0;
            Score -= Crowd * 0.6;
            if (Side.getAxis() == Direction.Axis.Y) {
                Score -= Support > 0 ? 0.3 : 1.2;
            }
            if (Score > BestScore) {
                BestScore = Score;
                Best = Side;
            }
        }
        return Best;
    }

    void advance(BlockPos Target, Direction Side, RandomSource Random) {
        if (Side == Heading) {
            Run++;
        } else {
            Heading = Side;
            Run = 1;
            RunTarget = rollRun(Random);
        }
        Pos = Target.asLong();
    }

    CompoundTag save() {
        CompoundTag Saved = new CompoundTag();
        Saved.putLong("Pos", Pos);
        Saved.putInt("Heading", Heading == null ? -1 : Heading.get3DDataValue());
        Saved.putInt("Run", Run);
        Saved.putInt("RunTarget", RunTarget);
        Saved.putLong("NextTick", NextTick);
        return Saved;
    }

    static Scp015Front load(CompoundTag Saved) {
        int HeadingId = Saved.getInt("Heading");
        Scp015Front Front = new Scp015Front(Saved.getLong("Pos"), HeadingId < 0 || HeadingId > 5 ? null : Direction.from3DDataValue(HeadingId),
                Saved.getInt("RunTarget"), Saved.getLong("NextTick"));
        Front.Run = Saved.getInt("Run");
        return Front;
    }
}
