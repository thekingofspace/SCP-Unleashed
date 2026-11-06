package net.rai.scp_unleashed.scp129;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.labware.LabOrganism;

import java.util.function.Predicate;

public final class Scp129Organism extends LabOrganism {
    public static final double Radius = 8.0;

    private final Scp129Strain Strain;

    public Scp129Organism(Scp129Strain Strain) {
        this.Strain = Strain;
    }

    public Scp129Strain strain() {
        return Strain;
    }

    @Override
    public ResourceLocation getId() {
        return Strain.id();
    }

    @Override
    public String getNameKey() {
        return Strain.nameKey();
    }

    @Override
    public int getColour() {
        return Strain.textColour();
    }

    @Override
    public int getVialColour() {
        return Strain.vialColour();
    }

    @Override
    public int getCultureColour() {
        return Strain.cultureColour();
    }

    @Override
    public int getMistColour() {
        return Strain.mistColour();
    }

    @Override
    public double getRadius() {
        return Radius;
    }

    @Override
    public boolean isPresentOn(LivingEntity Subject) {
        return Scp129Infection.carries(Subject, Strain);
    }

    @Override
    public boolean protects(LivingEntity Subject) {
        return Scp129Infection.isProtected(Subject);
    }

    @Override
    public void expose(ServerLevel World, Vec3 Source, Predicate<BlockPos> Ignored, double Reach) {
        Scp129Infection.exposeAround(World, Source, Ignored, Reach, Strain);
    }

    @Override
    public Object[] getInfoArguments() {
        return new Object[] {(int) getRadius()};
    }
}
