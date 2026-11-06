package net.rai.scp_unleashed.labware;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.gear.GearTags;

import java.util.function.Predicate;

public abstract class LabOrganism {
    public static final String InfoPrefix = "jei.scp_unleashed.info.vial.";

    public abstract ResourceLocation getId();

    public abstract String getNameKey();

    public abstract int getColour();

    public int getVialColour() {
        return getColour();
    }

    public int getCultureColour() {
        return getColour();
    }

    public int getMistColour() {
        return getColour();
    }

    public abstract boolean isPresentOn(LivingEntity Subject);

    public boolean protects(LivingEntity Subject) {
        return GearTags.wearsFullHazmatSuit(Subject);
    }

    public abstract void expose(ServerLevel World, Vec3 Source, Predicate<BlockPos> Ignored, double Radius);

    public double getRadius() {
        return LabExposure.Radius;
    }

    public void ventFrom(ServerLevel World, BlockPos Pos, RandomSource Random) {
    }

    public InteractionResult useVialOn(UseOnContext Context, ItemStack Vial) {
        return InteractionResult.PASS;
    }

    public ResourceLocation cultureIn(ServerLevel World, BlockPos Pos, RandomSource Random) {
        return getId();
    }

    public void animateCulture(Level World, BlockPos Pos, RandomSource Random) {
    }

    public Object[] getInfoArguments() {
        return new Object[] {(int) getRadius()};
    }

    public boolean infect(LivingEntity Subject) {
        return false;
    }

    public boolean spreadsByContact() {
        return false;
    }

    public float contactChance(LivingEntity Carrier, LabContact Route) {
        return 0.0f;
    }

    public boolean shields(LivingEntity Wearer) {
        return false;
    }

    public int stageOf(LivingEntity Subject) {
        return isPresentOn(Subject) ? 1 : 0;
    }

    public int stageCount() {
        return 1;
    }

    public final MutableComponent getName() {
        return Component.translatable(getNameKey());
    }

    public final MutableComponent getStyledName() {
        return getName().withColor(getColour() & 0xFFFFFF);
    }

    public final String getInfoKey(int Line) {
        return InfoPrefix + getId().getPath() + "." + Line;
    }

    public final ItemStack sealedVial() {
        return LabVials.sealedVial(getId());
    }

    public final ItemStack openVial() {
        return LabVials.openVial(getId());
    }
}
