package net.rai.scp_unleashed.scp610;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.labware.LabContact;
import net.rai.scp_unleashed.labware.LabOrganism;

import java.util.function.Predicate;

public final class Scp610Organism extends LabOrganism {
    public static final ResourceLocation Id = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp610");
    public static final String NameKey = "labware.scp_unleashed.agent.scp610";
    public static final int TextColour = 0x8C4A4C;
    public static final int LiquidColour = 0x5A2626;
    public static final int DishColour = 0x4A1E22;
    public static final int MistColour = 0x6A2A2E;

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
        return Subject instanceof Scp610Creature || Scp610Infection.isInfected(Subject);
    }

    @Override
    public void expose(ServerLevel World, Vec3 Source, Predicate<BlockPos> Ignored, double Radius) {
        Scp610Infection.exposeAround(World, Source, Ignored, Radius);
    }

    @Override
    public boolean infect(LivingEntity Subject) {
        return Scp610Infection.infect(Subject);
    }

    @Override
    public boolean spreadsByContact() {
        return true;
    }

    @Override
    public float contactChance(LivingEntity Carrier, LabContact Route) {
        return switch (Route) {
            case Strike -> Scp610Infection.strikeChance(Carrier);
            case Touch -> Scp610Infection.touchChance(Carrier);
            case Cough -> Scp610Infection.CoughChance;
        };
    }

    @Override
    public boolean shields(LivingEntity Wearer) {
        return Scp610Infection.isSuited(Wearer);
    }

    @Override
    public int stageOf(LivingEntity Subject) {
        if (Subject instanceof Scp610Creature) {
            return stageCount();
        }
        return Scp610Infection.stageOf(Subject);
    }

    @Override
    public int stageCount() {
        return Scp610Infection.Stages;
    }

    @Override
    public Object[] getInfoArguments() {
        return new Object[] {(int) getRadius()};
    }
}
