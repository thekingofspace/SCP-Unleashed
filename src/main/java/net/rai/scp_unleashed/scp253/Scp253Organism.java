package net.rai.scp_unleashed.scp253;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.labware.LabOrganism;

import java.util.function.Predicate;

public final class Scp253Organism extends LabOrganism {
    public static final ResourceLocation Id = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp253");
    public static final String NameKey = "labware.scp_unleashed.agent.scp253";
    public static final int TextColour = 0x6A9CF0;
    public static final int LiquidColour = 0x3A72E0;
    public static final int DishColour = 0x5F84C4;
    public static final int MistColour = 0x8DB3EC;

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
        return TextColour;
    }

    @Override
    public int getVialColour() {
        return LiquidColour;
    }

    @Override
    public int getCultureColour() {
        return DishColour;
    }

    @Override
    public int getMistColour() {
        return MistColour;
    }

    @Override
    public boolean isPresentOn(LivingEntity Subject) {
        return Scp253Infection.isInfected(Subject);
    }

    @Override
    public boolean protects(LivingEntity Subject) {
        return Scp253Infection.isProtected(Subject);
    }

    @Override
    public void expose(ServerLevel World, Vec3 Source, Predicate<BlockPos> Ignored, double Radius) {
        Scp253Infection.exposeAround(World, Source, Ignored, Radius);
    }

    @Override
    public Object[] getInfoArguments() {
        return new Object[] {(int) getRadius(), Scp253Infection.Duration / 1200};
    }
}
