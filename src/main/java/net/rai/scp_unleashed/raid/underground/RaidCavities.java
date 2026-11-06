package net.rai.scp_unleashed.raid.underground;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.raid.RaidGround;
import net.rai.scp_unleashed.raid.objective.RaidAccess;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RaidCavities {
    public static final int TierInside = 1;
    public static final int TierSeen = 2;
    public static final int TierOpen = 3;
    public static final int TierSurface = 4;

    public record Spot(BlockPos Feet, int Tier) {
    }

    private record Candidate(BlockPos Feet, int Tier, double Score) {
    }

    private RaidCavities() {
    }

    public static boolean hazard(BlockState State) {
        return State.is(BlockTags.FIRE) || State.is(Blocks.MAGMA_BLOCK) || State.is(Blocks.POWDER_SNOW) || State.is(Blocks.COBWEB) || State.is(Blocks.SWEET_BERRY_BUSH)
                || State.is(Blocks.WITHER_ROSE) || State.is(Blocks.CACTUS) || State.is(Blocks.POINTED_DRIPSTONE) || State.getFluidState().is(FluidTags.LAVA)
                || State.getBlock() instanceof CampfireBlock && CampfireBlock.isLitCampfire(State);
    }

    public static boolean clear(Level World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        return State.getFluidState().isEmpty() && State.getCollisionShape(World, Pos).isEmpty() && !hazard(State);
    }

    public static boolean footing(Level World, BlockPos Under) {
        BlockState Floor = World.getBlockState(Under);
        return Floor.isFaceSturdy(World, Under, Direction.UP) && Floor.getFluidState().isEmpty() && !hazard(Floor);
    }

    public static boolean roomy(Level World, BlockPos Feet) {
        int Half = UndergroundRules.CavityHalf;
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Dx = -Half; Dx <= Half; Dx++) {
            for (int Dz = -Half; Dz <= Half; Dz++) {
                Cursor.set(Feet.getX() + Dx, Feet.getY() - 1, Feet.getZ() + Dz);
                if (!footing(World, Cursor)) {
                    return false;
                }
                for (int Dy = 0; Dy < UndergroundRules.CavityHeight; Dy++) {
                    Cursor.set(Feet.getX() + Dx, Feet.getY() + Dy, Feet.getZ() + Dz);
                    if (!clear(World, Cursor)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean dry(Level World, BlockPos Feet, int Half, int Height, int Reach, boolean LavaOnly) {
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Dx = -Half - Reach; Dx <= Half + Reach; Dx++) {
            for (int Dz = -Half - Reach; Dz <= Half + Reach; Dz++) {
                for (int Dy = -1 - Reach; Dy <= Height + Reach - 1; Dy++) {
                    Cursor.set(Feet.getX() + Dx, Feet.getY() + Dy, Feet.getZ() + Dz);
                    BlockState State = World.getBlockState(Cursor);
                    if (LavaOnly ? State.getFluidState().is(FluidTags.LAVA) || State.is(BlockTags.FIRE) : !State.getFluidState().isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean safe(Level World, BlockPos Feet) {
        int Half = UndergroundRules.CavityHalf;
        int Height = UndergroundRules.CavityHeight;
        return roomy(World, Feet) && dry(World, Feet, Half, Height, UndergroundRules.WetReach, false) && dry(World, Feet, Half, Height, UndergroundRules.LavaReach, true);
    }

    public static boolean safeStand(ServerLevel World, BlockPos Feet) {
        return RaidGround.standable(World, Feet) && clear(World, Feet) && clear(World, Feet.above()) && clear(World, Feet.above(2)) && footing(World, Feet.below())
                && dry(World, Feet, 0, UndergroundRules.CavityHeight, UndergroundRules.WetReach, false)
                && dry(World, Feet, 0, UndergroundRules.CavityHeight, UndergroundRules.LavaReach, true);
    }

    public static int view(Level World, BlockPos Feet, List<? extends Player> Viewers) {
        Vec3 Spot = Vec3.atBottomCenterOf(Feet).add(0.0, 1.0, 0.0);
        int Worst = 0;
        double Square = UndergroundRules.ViewReach * UndergroundRules.ViewReach;
        for (Player Viewer : Viewers) {
            Vec3 Eye = Viewer.getEyePosition();
            if (Viewer.level() != World || Eye.distanceToSqr(Spot) > Square) {
                continue;
            }
            if (World.clip(new ClipContext(Eye, Spot, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Viewer)).getType() != HitResult.Type.MISS) {
                continue;
            }
            Vec3 Toward = Spot.subtract(Eye).normalize();
            Worst = Math.max(Worst, Viewer.getViewVector(1.0f).dot(Toward) >= UndergroundRules.ViewCone ? 2 : 1);
        }
        return Worst;
    }

    public static double crowd(BlockPos Feet, List<? extends Player> Viewers) {
        Vec3 Spot = Vec3.atBottomCenterOf(Feet);
        for (Player Viewer : Viewers) {
            if (Viewer.position().distanceToSqr(Spot) < UndergroundRules.CrowdReach * UndergroundRules.CrowdReach) {
                return UndergroundRules.CrowdPenalty;
            }
        }
        return 0.0;
    }

    public static boolean walkable(Mob Probe, BlockPos From, BlockPos To) {
        if (From.equals(To)) {
            return true;
        }
        Probe.moveTo(From.getX() + 0.5, From.getY(), From.getZ() + 0.5, 0.0f, 0.0f);
        Probe.setOnGround(true);
        return RaidAccess.route(Probe, Set.of(To), 2, UndergroundRules.PathRange, UndergroundRules.PathNodes) != null;
    }

    @Nullable
    public static Spot find(ServerLevel World, RaidSurvey Survey, List<? extends Player> Viewers, Mob Probe) {
        RaidFlood Flood = Survey.Flood();
        if (Flood.empty()) {
            return null;
        }
        List<Candidate> Found = new ArrayList<>();
        for (Map.Entry<Long, Integer> Entry : Flood.steps().entrySet()) {
            BlockPos Feet = BlockPos.of(Entry.getKey());
            if (!World.isPositionEntityTicking(Feet) || !roomy(World, Feet)) {
                continue;
            }
            boolean Covered = RaidSurvey.covered(World, Feet);
            int Seen = view(World, Feet, Viewers);
            int Tier = !Covered ? TierOpen : Seen >= 2 ? TierSeen : TierInside;
            double Score = Entry.getValue() + crowd(Feet, Viewers) + (Seen == 1 ? UndergroundRules.SightPenalty : Seen >= 2 ? UndergroundRules.SeenPenalty : 0.0);
            if (clear(World, Feet.above(UndergroundRules.CavityHeight))) {
                Score -= UndergroundRules.TallBonus;
            }
            Found.add(new Candidate(Feet, Tier, Score));
        }
        Found.sort(Comparator.comparingInt(Candidate::Tier).thenComparingDouble(Candidate::Score));
        int Tier = -1;
        int Checked = 0;
        for (Candidate One : Found) {
            if (One.Tier() != Tier) {
                Tier = One.Tier();
                Checked = 0;
            }
            if (Checked >= UndergroundRules.PathChecks || !safe(World, One.Feet())) {
                continue;
            }
            Checked++;
            if (walkable(Probe, One.Feet(), Survey.Anchor())) {
                return new Spot(One.Feet(), One.Tier());
            }
        }
        return null;
    }
}
