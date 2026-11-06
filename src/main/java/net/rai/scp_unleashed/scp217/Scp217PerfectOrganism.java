package net.rai.scp_unleashed.scp217;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.labware.LabOrganism;
import net.rai.scp_unleashed.labware.LabPetriDishBlockEntity;
import net.rai.scp_unleashed.marksman.ability.ToolAbilityItem;

import java.util.function.Predicate;

public final class Scp217PerfectOrganism extends LabOrganism {
    public static final ResourceLocation Id = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp217_perfect");
    public static final String NameKey = "labware.scp_unleashed.agent.scp217_perfect";
    public static final int TextColour = 0xF2C14E;
    public static final int LiquidColour = 0xEDBE4A;
    public static final int DishColour = 0xFFB830;
    public static final int MistColour = 0xF6D88A;
    public static final int GlintChance = 2;
    public static final double GlintSpread = 0.36;

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
        return Scp217Clockwork.isPerfect(Subject);
    }

    @Override
    public boolean protects(LivingEntity Subject) {
        return Scp217Clockwork.isProtected(Subject);
    }

    @Override
    public void expose(ServerLevel World, Vec3 Source, Predicate<BlockPos> Ignored, double Radius) {
        Scp217Clockwork.exposeAround(World, Source, Ignored, Radius, true);
    }

    @Override
    public void animateCulture(Level World, BlockPos Pos, RandomSource Random) {
        if (Random.nextInt(GlintChance) != 0) {
            return;
        }
        double X = Pos.getX() + 0.5 + (Random.nextDouble() - 0.5) * GlintSpread;
        double Z = Pos.getZ() + 0.5 + (Random.nextDouble() - 0.5) * GlintSpread;
        World.addParticle(ParticleTypes.WAX_ON, X, Pos.getY() + LabPetriDishBlockEntity.SurfaceHeight + 0.02, Z, 0.0, 0.012, 0.0);
    }

    @Override
    public Object[] getInfoArguments() {
        return new Object[] {(int) getRadius(), Component.keybind(ToolAbilityItem.AbilityKey)};
    }
}
