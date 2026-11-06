package net.rai.scp_unleashed.scp610;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class Scp610Weaver extends Scp610Utility {
    public static final int WeaveTicks = 32;
    public static final int MendTick = 24;
    public static final double Reach = 24.0;
    public static final double Close = 2.5;
    @Nullable
    private BlockPos Scar;

    public Scp610Weaver(EntityType<? extends Scp610Weaver> Type, Level World) {
        super(Type, World, Scp610Form.Weaver);
    }

    @Override
    protected void registerWork() {
        goalSelector.addGoal(3, new Weave(this));
    }

    @Nullable
    public BlockPos scar() {
        return Scar;
    }

    @Nullable
    public BlockPos findScar() {
        Scp610Hive Hive = hive();
        if (Hive == null) {
            return null;
        }
        BlockPos Best = null;
        double BestGap = Reach * Reach;
        for (long Each : Hive.Scars) {
            BlockPos Pos = BlockPos.of(Each);
            double Gap = Pos.distToCenterSqr(position());
            if (Gap < BestGap) {
                BestGap = Gap;
                Best = Pos;
            }
        }
        return Best;
    }

    public static boolean mendable(ServerLevel World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        return Scp610Growth.open(World, Pos) || Scp610Growth.convertible(World, Pos) || Scp610Growth.isFlesh(State);
    }

    public boolean mend(ServerLevel World, BlockPos Pos) {
        Scp610Hive Hive = hive();
        if (Hive == null) {
            return false;
        }
        Hive.healScar(Pos.asLong());
        BlockState State = World.getBlockState(Pos);
        if (Scp610Growth.isFlesh(State)) {
            if (Scp610Hives.ownerOf(World, Pos) == null) {
                Scp610Hives.own(World, Hive, Pos);
            }
            return false;
        }
        if (!mendable(World, Pos) || Scp610Growth.protectedBlock(World, Pos, State) || !Scp610Biomass.spend(Hive, Scp610Biomass.GrowthCost)) {
            return false;
        }
        Scp610Growth.place(World, Hive, Pos, Scp610Growth.wall(World.random), true);
        World.playSound(null, Pos, Scp610Sounds.Grow.get(), SoundSource.BLOCKS, 0.8f, 0.9f);
        World.sendParticles(Scp610Infection.Speck, Pos.getX() + 0.5, Pos.getY() + 0.8, Pos.getZ() + 0.5, 10, 0.3, 0.2, 0.3, 0.0);
        return true;
    }

    @Override
    protected void actionTick(byte Kind, int Elapsed) {
        if (Kind == ActionWeave && Elapsed == MendTick && Scar != null && level() instanceof ServerLevel World) {
            mend(World, Scar);
            Scar = null;
            return;
        }
        super.actionTick(Kind, Elapsed);
    }

    @Override
    protected void work() {
        if (Scar == null && tickCount % 20 == 0 && !fleeing()) {
            Scar = findScar();
        }
    }

    public static final class Weave extends Goal {
        private final Scp610Weaver Builder;

        public Weave(Scp610Weaver Builder) {
            this.Builder = Builder;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Builder.scar() != null && !Builder.fleeing() && !Builder.locksMovement();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            BlockPos Target = Builder.scar();
            if (Target == null) {
                return;
            }
            Vec3 Centre = Vec3.atCenterOf(Target);
            Builder.getLookControl().setLookAt(Centre.x, Centre.y, Centre.z);
            if (Builder.near(Centre, Close)) {
                Builder.getNavigation().stop();
                if (Builder.canAct()) {
                    Builder.setAction(ActionWeave, WeaveTicks);
                }
            } else {
                Builder.walkTo(Centre, 1.0);
            }
        }
    }
}
