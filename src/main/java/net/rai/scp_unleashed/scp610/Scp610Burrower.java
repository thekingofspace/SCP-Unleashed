package net.rai.scp_unleashed.scp610;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class Scp610Burrower extends Scp610Utility {
    public static final int BurrowTicks = 24;
    public static final int SurfaceTicks = 24;
    public static final int Resurface = 8;

    public Scp610Burrower(EntityType<? extends Scp610Burrower> Type, Level World) {
        super(Type, World, Scp610Form.Burrower);
    }

    @Override
    protected void registerWork() {
    }

    public boolean buried() {
        byte Current = action();
        return Current == ActionBurrow || Current == ActionSurface;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        return buried() && !Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || super.isInvulnerableTo(Source);
    }

    @Override
    protected void threatened(LivingEntity Attacker) {
        super.threatened(Attacker);
        dive();
    }

    public boolean dive() {
        if (!canAct() || !(level() instanceof ServerLevel World)) {
            return false;
        }
        setAction(ActionBurrow, BurrowTicks);
        World.sendParticles(Gore, getX(), getY() + 0.2, getZ(), 16, 0.4, 0.1, 0.4, 0.0);
        playLayers(Scp610Sounds.WetLayers, 1.0f, 0.7f);
        return true;
    }

    @Override
    protected void finishAction(byte Kind) {
        if (Kind == ActionBurrow && level() instanceof ServerLevel World) {
            endAction();
            BlockPos Spot = surfaceSpot(World);
            if (Spot != null) {
                teleportTo(Spot.getX() + 0.5, Spot.getY(), Spot.getZ() + 0.5);
            }
            setAction(ActionSurface, SurfaceTicks);
            World.sendParticles(Gore, getX(), getY() + 0.2, getZ(), 16, 0.4, 0.1, 0.4, 0.0);
            return;
        }
        super.finishAction(Kind);
    }

    public BlockPos surfaceSpot(ServerLevel World) {
        BlockPos Here = blockPosition();
        for (int Try = 0; Try < 16; Try++) {
            int Dx = random.nextInt(Resurface * 2 + 1) - Resurface;
            int Dz = random.nextInt(Resurface * 2 + 1) - Resurface;
            if (Dx * Dx + Dz * Dz < 9) {
                continue;
            }
            for (int Dy = 3; Dy >= -3; Dy--) {
                BlockPos Feet = Here.offset(Dx, Dy, Dz);
                BlockState Below = World.getBlockState(Feet.below());
                if (!Below.getCollisionShape(World, Feet.below()).isEmpty() && World.noCollision(this, getType().getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(Feet)))
                        && World.getFluidState(Feet).isEmpty()) {
                    return Feet;
                }
            }
        }
        return null;
    }

    @Override
    public boolean isPushable() {
        return !buried() && super.isPushable();
    }
}
