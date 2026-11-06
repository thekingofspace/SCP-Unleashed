package net.rai.scp_unleashed.scp610;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;

public class Scp610Apostle extends Scp610Unit {
    public static final int EvolveTicks = 60;
    public static final int CheckTicks = 40;
    public static final int GiveUpTicks = 12000;
    public static final int RoamMin = 16;
    public static final int RoamMax = 28;
    public static final int Samples = 6;
    private int Searched;

    public Scp610Apostle(EntityType<? extends Scp610Apostle> Type, Level World) {
        super(Type, World, Scp610Form.Apostle);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(4, new Roam(this));
    }

    public boolean evolving() {
        return action() == ActionEvolve;
    }

    @Override
    protected void serverTick() {
        if (!(level() instanceof ServerLevel World) || isActing() || tickCount % CheckTicks != 0) {
            return;
        }
        Searched += CheckTicks;
        evaluate(World);
    }

    public boolean evaluate(ServerLevel World) {
        if (!canAct()) {
            return false;
        }
        BlockPos Feet = blockPosition();
        if (Scp610Founding.allowed(World, Feet) != Scp610Founding.Verdict.Ready) {
            return false;
        }
        boolean Fits = Scp610Founding.groundFits(World, Feet);
        if (Fits && (Scp610Founding.sheltered(World, Feet) || Searched >= GiveUpTicks)) {
            beginEvolve();
            return true;
        }
        return false;
    }

    public void beginEvolve() {
        setTarget(null);
        setAction(ActionEvolve, EvolveTicks);
        playLayers(Scp610Sounds.EvolveLayers, 1.4f, 0.8f);
    }

    @Override
    protected void finishAction(byte Kind) {
        super.finishAction(Kind);
        if (Kind != ActionEvolve || !(level() instanceof ServerLevel World) || !isAlive()) {
            return;
        }
        BlockPos Feet = blockPosition();
        if (Scp610Founding.allowed(World, Feet) != Scp610Founding.Verdict.Ready) {
            return;
        }
        Scp610Spewer Made = Scp610Founding.found(World, Feet, getYRot());
        if (Made != null) {
            discard();
        }
    }

    @Override
    protected void actionTick(byte Kind, int Elapsed) {
        if (Kind == ActionEvolve && level() instanceof ServerLevel World && Elapsed % 8 == 0) {
            Scp610Infection.cloud(World, position().add(0.0, 0.6, 0.0), 0.8, 6);
        }
        super.actionTick(Kind, Elapsed);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putInt("Searched", Searched);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        Searched = Tag.getInt("Searched");
    }

    public int searched() {
        return Searched;
    }

    public void setSearched(int Ticks) {
        Searched = Ticks;
    }

    public static final class Roam extends Goal {
        private final Scp610Apostle Walker;
        @Nullable
        private Vec3 Destination;

        public Roam(Scp610Apostle Walker) {
            this.Walker = Walker;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (Walker.getTarget() != null || Walker.locksMovement() || Walker.getRandom().nextInt(30) != 0 || !(Walker.level() instanceof ServerLevel World)) {
                return false;
            }
            Destination = pick(World);
            return Destination != null;
        }

        @Override
        public boolean canContinueToUse() {
            return Walker.getTarget() == null && !Walker.locksMovement() && !Walker.getNavigation().isDone();
        }

        @Override
        public void start() {
            if (Destination != null) {
                Walker.getNavigation().moveTo(Destination.x, Destination.y, Destination.z, 0.9);
            }
        }

        @Nullable
        private Vec3 pick(ServerLevel World) {
            Optional<BlockPos> Village = World.getPoiManager().findClosest(Holder -> Holder.is(PoiTypes.MEETING), Walker.blockPosition(), 96, PoiManager.Occupancy.ANY);
            if (Village.isPresent() && Village.get().distSqr(Walker.blockPosition()) > 32.0 * 32.0) {
                BlockPos Toward = Village.get();
                Vec3 Way = Vec3.atCenterOf(Toward).subtract(Walker.position()).normalize().scale(RoamMax);
                return Walker.position().add(Way);
            }
            Vec3 Best = null;
            int Darkest = Integer.MAX_VALUE;
            for (int Try = 0; Try < Samples; Try++) {
                double Angle = Walker.getRandom().nextDouble() * Math.PI * 2.0;
                double Reach = RoamMin + Walker.getRandom().nextDouble() * (RoamMax - RoamMin);
                int X = Walker.getBlockX() + (int) Math.round(Math.cos(Angle) * Reach);
                int Z = Walker.getBlockZ() + (int) Math.round(Math.sin(Angle) * Reach);
                BlockPos Ground = World.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(X, 0, Z));
                if (Math.abs(Ground.getY() - Walker.getBlockY()) > 12) {
                    Ground = new BlockPos(X, Walker.getBlockY(), Z);
                }
                int Light = World.getBrightness(LightLayer.BLOCK, Ground) + Math.max(0, World.getBrightness(LightLayer.SKY, Ground) - World.getSkyDarken());
                if (Light < Darkest) {
                    Darkest = Light;
                    Best = Vec3.atBottomCenterOf(Ground);
                }
            }
            return Best;
        }
    }
}
