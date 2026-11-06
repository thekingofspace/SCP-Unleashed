package net.rai.scp_unleashed.scp008;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.labware.LabOrganism;

import java.util.function.Predicate;

public final class Scp008Organism extends LabOrganism {
    public static final ResourceLocation Id = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp008");
    public static final String NameKey = "labware.scp_unleashed.agent.scp008";
    public static final int Colour = 0x7ED37A;

    @Override
    public ResourceLocation getId() {
        return Id;
    }

    @Override
    public String getNameKey() {
        return NameKey;
    }

    @Override
    public int getColour() {
        return Colour;
    }

    @Override
    public boolean isPresentOn(LivingEntity Subject) {
        return Scp008Infection.isInfected(Subject);
    }

    @Override
    public boolean protects(LivingEntity Subject) {
        return Scp008Infection.isProtected(Subject);
    }

    @Override
    public void expose(ServerLevel World, Vec3 Source, Predicate<BlockPos> Ignored, double Radius) {
        Scp008Infection.exposeAround(World, Source, Ignored, Radius);
    }

    @Override
    public Object[] getInfoArguments() {
        return new Object[] {(int) getRadius(), Scp008Zombie.Lifespan / 1200, Scp008Events.ZombieCap};
    }
}
