package net.rai.scp_unleashed.scp001day.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp001day.Scp001DayRegistry;

public final class Scp001DayFusion {
    public static final double AreaRange = 48.0;
    public static final int SplitPieces = 4;

    private Scp001DayFusion() {
    }

    public static EntityType<Scp001DayFlesh> typeOf(Scp001DayForm Form) {
        return switch (Form) {
            case Crawler -> Scp001DayRegistry.CrawlerType.get();
            case Clinger -> Scp001DayRegistry.ClingerType.get();
            case Lurker -> Scp001DayRegistry.LurkerType.get();
            case Mass -> Scp001DayRegistry.MassType.get();
        };
    }

    public static int areaMass(ServerLevel World, Vec3 Centre) {
        int Total = 0;
        for (Scp001DayFlesh Each : World.getEntitiesOfClass(Scp001DayFlesh.class, new AABB(Centre, Centre).inflate(AreaRange), Scp001DayFlesh::isAlive)) {
            Total += Each.mass();
        }
        return Total;
    }

    @Nullable
    public static Scp001DayFlesh make(ServerLevel World, int Mass, Vec3 Spot, @Nullable UUID Carrying) {
        Scp001DayForm Form = Scp001DayForm.forMass(Mass);
        Scp001DayFlesh Made = typeOf(Form).create(World);
        if (Made == null) {
            return null;
        }
        Made.moveTo(Spot.x, Spot.y, Spot.z, World.random.nextFloat() * 360.0f, 0.0f);
        Made.setMass(Mass);
        Made.carry(Carrying);
        Made.finalizeSpawn(World, World.getCurrentDifficultyAt(Made.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        Made.setHealth(Made.getMaxHealth());
        World.addFreshEntity(Made);
        return Made;
    }

    public static boolean tryFuse(ServerLevel World, Scp001DayFlesh Seed) {
        if (Seed.holding() || Seed.stunned() || World.random.nextDouble() > Scp001DayFlesh.FuseOdds) {
            return false;
        }
        List<Scp001DayFlesh> Group = new ArrayList<>();
        int Total = 0;
        for (Scp001DayFlesh Each : Scp001DayFlesh.near(World, Seed.position(), Scp001DayFlesh.FuseRange)) {
            if (Each.holding() || Each.stunned()) {
                continue;
            }
            if (Total + Each.mass() > Scp001DayForm.MassCap) {
                continue;
            }
            Group.add(Each);
            Total += Each.mass();
        }
        if (Group.size() < 2 || Total < Scp001DayForm.FuseFloor) {
            return false;
        }
        return fuse(World, Group) != null;
    }

    @Nullable
    public static Scp001DayFlesh fuse(ServerLevel World, List<Scp001DayFlesh> Group) {
        int Total = 0;
        Vec3 Centre = Vec3.ZERO;
        UUID Carrying = null;
        for (Scp001DayFlesh Each : Group) {
            Total += Each.mass();
            Centre = Centre.add(Each.position());
            if (Each.carrying() != null) {
                Carrying = Each.carrying();
            }
        }
        if (Group.size() < 2 || Total > Scp001DayForm.MassCap) {
            return null;
        }
        Centre = Centre.scale(1.0 / Group.size());
        LivingEntity Target = Group.get(0).getTarget();
        for (Scp001DayFlesh Each : Group) {
            Each.discard();
        }
        Scp001DayFlesh Made = make(World, Total, Centre, Carrying);
        if (Made != null) {
            Made.setTarget(Target);
            Made.triggerAnim("action", "fuse");
            World.playSound(null, Centre.x, Centre.y + 1.0, Centre.z, Scp001DayRegistry.FleshFuse.get(), SoundSource.HOSTILE, 1.2f, 0.7f);
            World.sendParticles(ParticleTypes.DRIPPING_LAVA, Centre.x, Centre.y + 1.0, Centre.z, 20, 0.6, 0.6, 0.6, 0.02);
        }
        return Made;
    }

    public static List<Scp001DayFlesh> split(ServerLevel World, Scp001DayFlesh Parent, @Nullable LivingEntity Target) {
        List<Scp001DayFlesh> Made = new ArrayList<>();
        int Mass = Parent.mass();
        if (Mass < 2) {
            return Made;
        }
        int Pieces = Math.min(SplitPieces, Mass - 1);
        int Left = Mass - Pieces;
        Vec3 Centre = Parent.position();
        UUID Carrying = Parent.carrying();
        Parent.discard();
        Scp001DayFlesh Rest = make(World, Left, Centre, Carrying);
        if (Rest != null) {
            Rest.setTarget(Target);
            Made.add(Rest);
        }
        for (int Index = 0; Index < Pieces; Index++) {
            double Angle = Index * Math.PI * 2.0 / Pieces;
            Scp001DayFlesh Piece = make(World, 1, Centre.add(Math.cos(Angle) * 0.6, 0.1, Math.sin(Angle) * 0.6), Carrying);
            if (Piece != null) {
                Piece.setTarget(Target);
                Made.add(Piece);
            }
        }
        World.playSound(null, Centre.x, Centre.y + 1.0, Centre.z, Scp001DayRegistry.FleshSplit.get(), SoundSource.HOSTILE, 1.2f, 1.1f);
        return Made;
    }
}
