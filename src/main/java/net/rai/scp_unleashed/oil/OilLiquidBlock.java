package net.rai.scp_unleashed.oil;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

public class OilLiquidBlock extends LiquidBlock {
    public static final int StickyTicks = 60;
    public static final int RefreshBelow = 20;

    private final OilGrade Grade;

    public OilLiquidBlock(FlowingFluid Liquid, OilGrade Grade, Properties Settings) {
        super(Liquid, Settings);
        this.Grade = Grade;
    }

    public OilGrade getGrade() {
        return Grade;
    }

    @Override
    protected void entityInside(BlockState State, Level World, BlockPos Pos, Entity Target) {
        super.entityInside(State, World, Pos, Target);
        if (World.isClientSide || !Grade.isSticky() || !(Target instanceof LivingEntity Living) || !Living.isAlive()) {
            return;
        }
        if (Living instanceof Player User && (User.isCreative() || User.isSpectator())) {
            return;
        }
        MobEffectInstance Current = Living.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (Current == null || Current.getDuration() < RefreshBelow) {
            Living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, StickyTicks, 0, false, false, true));
        }
    }
}
