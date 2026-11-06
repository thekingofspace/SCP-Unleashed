package net.rai.scp_unleashed.fission;

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

public class WasteWaterBlock extends LiquidBlock {
    public static final int PoisonTicks = 80;
    public static final int RefreshBelow = 30;

    public WasteWaterBlock(FlowingFluid Liquid, Properties Properties) {
        super(Liquid, Properties);
    }

    @Override
    protected void entityInside(BlockState State, Level World, BlockPos Pos, Entity Target) {
        super.entityInside(State, World, Pos, Target);
        if (World.isClientSide || !(Target instanceof LivingEntity Living) || !Living.isAlive()) {
            return;
        }
        if (Living instanceof Player User && (User.isCreative() || User.isSpectator())) {
            return;
        }
        MobEffectInstance Current = Living.getEffect(MobEffects.POISON);
        if (Current == null || Current.getDuration() < RefreshBelow) {
            Living.addEffect(new MobEffectInstance(MobEffects.POISON, PoisonTicks, 0));
        }
    }
}
