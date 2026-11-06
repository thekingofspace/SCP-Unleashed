package net.rai.scp_unleashed.scp217;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.labware.LabOrganism;

import java.util.function.Predicate;

public final class Scp217Organism extends LabOrganism {
    public static final ResourceLocation Id = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp217");
    public static final String NameKey = "labware.scp_unleashed.agent.scp217";
    public static final int TextColour = 0xE8862A;
    public static final int LiquidColour = 0xE07B1C;
    public static final int DishColour = 0xC86A18;
    public static final int MistColour = 0xF2A65A;

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
        return Scp217Clockwork.isInfected(Subject) && !Scp217Clockwork.isPerfect(Subject);
    }

    @Override
    public boolean protects(LivingEntity Subject) {
        return Scp217Clockwork.isProtected(Subject);
    }

    @Override
    public void expose(ServerLevel World, Vec3 Source, Predicate<BlockPos> Ignored, double Radius) {
        Scp217Clockwork.exposeAround(World, Source, Ignored, Radius, false);
    }

    @Override
    public ResourceLocation cultureIn(ServerLevel World, BlockPos Pos, RandomSource Random) {
        if (!Scp217Strain.roll(Random)) {
            return Id;
        }
        Scp217Strain.announce(World, Pos);
        return Scp217PerfectOrganism.Id;
    }
}
