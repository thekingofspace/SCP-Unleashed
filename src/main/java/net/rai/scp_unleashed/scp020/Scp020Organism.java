package net.rai.scp_unleashed.scp020;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.labware.LabOrganism;
import net.rai.scp_unleashed.labware.LabOrganisms;

import java.util.function.Predicate;

public final class Scp020Organism extends LabOrganism {
    public static final ResourceLocation Id = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp020");
    public static final String NameKey = "labware.scp_unleashed.agent.scp020";
    public static final int TextColour = 0x7A8F45;
    public static final int LiquidColour = 0x55652C;
    public static final int DishColour = 0x5E6E33;
    public static final int MistColour = 0x8C9A5E;
    public static final double Radius = 8.0;

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
    public double getRadius() {
        return Radius;
    }

    @Override
    public boolean isPresentOn(LivingEntity Subject) {
        return Scp020Affliction.isAfflicted(Subject);
    }

    @Override
    public boolean protects(LivingEntity Subject) {
        return Scp020Affliction.isProtected(Subject);
    }

    @Override
    public void expose(ServerLevel World, Vec3 Source, Predicate<BlockPos> Ignored, double Reach) {
        Scp020Affliction.exposeAround(World, Source, Ignored, Reach);
    }

    @Override
    public InteractionResult useVialOn(UseOnContext Context, ItemStack Vial) {
        return Scp020Mold.plant(Context, Vial);
    }

    @Override
    public void ventFrom(ServerLevel World, BlockPos Pos, RandomSource Random) {
        Scp020Mold.seedCeiling(World, Pos, Random);
    }

    @Override
    public Object[] getInfoArguments() {
        return new Object[] {(int) getRadius(), (int) Scp020Mold.Reach, (Scp020Affliction.MaxIntensity * Scp020Affliction.DecayTicks + Scp020Affliction.Grace + 600) / 1200};
    }

    public static Scp020Organism get() {
        return (Scp020Organism) LabOrganisms.find(Id).orElseThrow();
    }
}
