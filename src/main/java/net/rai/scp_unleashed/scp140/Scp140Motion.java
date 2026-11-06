package net.rai.scp_unleashed.scp140;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.control.ControlBodies;

public final class Scp140Motion {
    public static final float OpenStep = 0.085f;
    public static final float LeafStep = 16.0f;
    public static final float LeafEnd = 180.0f;
    public static final float FlipStart = 0.55f;
    public static final int FlipChance = 110;
    public static final double NearRadius = 3.5;
    public static final int ScanInterval = 4;

    private float Open;
    private float OpenBefore;
    private float Leaf;
    private float LeafBefore;
    private boolean Flipping;
    private boolean Greeted;
    private boolean Near;
    private int Ticks;

    public Event step(Level World, Vec3 Centre, boolean Allowed, RandomSource Random) {
        Ticks++;
        if (!Allowed) {
            Near = false;
        } else if (Ticks % ScanInterval == 0 || Near) {
            Near = someoneNear(World, Centre);
        }
        OpenBefore = Open;
        LeafBefore = Leaf;
        float Was = Open;
        Open = Mth.clamp(Open + (Near ? OpenStep : -OpenStep), 0.0f, 1.0f);
        Event Result = Event.None;
        if (Was <= 0.0f && Open > 0.0f) {
            Result = Event.Opened;
        } else if (Was > 0.0f && Open <= 0.0f) {
            Result = Event.Closed;
        }
        if (Flipping && Open < FlipStart) {
            Flipping = false;
            Leaf = 0.0f;
            LeafBefore = 0.0f;
        }
        if (Flipping) {
            Leaf += LeafStep;
            if (Leaf >= LeafEnd) {
                Flipping = false;
                Leaf = 0.0f;
                LeafBefore = 0.0f;
            }
        } else if (Near && Open >= FlipStart && (!Greeted || Open >= 1.0f && Random.nextInt(FlipChance) == 0)) {
            Flipping = true;
            Greeted = true;
            Leaf = 0.0f;
            LeafBefore = 0.0f;
            if (Result == Event.None) {
                Result = Event.Turned;
            }
        }
        if (Open <= 0.0f) {
            Greeted = false;
        }
        return Result;
    }

    public float open(float PartialTick) {
        return Mth.lerp(PartialTick, OpenBefore, Open);
    }

    public float leaf(float PartialTick) {
        return Flipping ? Mth.lerp(PartialTick, LeafBefore, Leaf) : 0.0f;
    }

    public boolean isOpen() {
        return Open > 0.0f;
    }

    public static boolean someoneNear(Level World, Vec3 Centre) {
        if (World.getNearestPlayer(Centre.x, Centre.y, Centre.z, NearRadius, false) != null) {
            return true;
        }
        AABB Reach = AABB.ofSize(Centre, NearRadius * 2.0, NearRadius * 2.0, NearRadius * 2.0);
        return !World.getEntitiesOfClass(LivingEntity.class, Reach, Target -> Target.isAlive() && (Target instanceof AbstractVillager || ControlBodies.isPersonLike(Target))
                && Target.distanceToSqr(Centre) <= NearRadius * NearRadius).isEmpty();
    }

    public enum Event {
        None,
        Opened,
        Closed,
        Turned
    }
}
