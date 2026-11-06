package net.rai.scp_unleashed.scp016;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.labware.LabOrganism;

import java.util.function.Predicate;

public final class Scp016Organism extends LabOrganism {
    public static final ResourceLocation Id = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp016");
    public static final String NameKey = "labware.scp_unleashed.agent.scp016";
    public static final int TextColour = 0xC21530;
    public static final int LiquidColour = 0xB8122A;
    public static final int DishColour = 0x9E1026;
    public static final int MistColour = 0xD8404E;

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
        return Scp016Affliction.isDetectable(Subject);
    }

    @Override
    public boolean protects(LivingEntity Subject) {
        return Scp016Affliction.isProtected(Subject);
    }

    @Override
    public void expose(ServerLevel World, Vec3 Source, Predicate<BlockPos> Ignored, double Radius) {
        Scp016Affliction.exposeAround(World, Source, Ignored, Radius);
    }

    @Override
    public Object[] getInfoArguments() {
        return new Object[] {(int) getRadius(), Scp016Affliction.Duration / 1200, Scp016Spawn.Lifespan / 1200, Scp016Events.SpawnCap};
    }
}
