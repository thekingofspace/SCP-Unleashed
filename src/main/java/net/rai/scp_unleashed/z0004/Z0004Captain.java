package net.rai.scp_unleashed.z0004;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class Z0004Captain extends Z0004Pirate {
    public static final float CaptainWidth = 0.7f;
    public static final float CaptainHeight = 2.15f;
    public static final float CaptainEyeHeight = 1.92f;
    public static final double CaptainHealth = 90.0;
    public static final double CaptainDamage = 7.0;
    public static final double CaptainArmor = 6.0;
    public static final double CaptainKnockback = 0.5;
    public static final double BarRange = 48.0;
    private final ServerBossEvent Bar = new ServerBossEvent(Component.translatable("entity.scp_unleashed.z0004_captain"), BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.PROGRESS);

    public Z0004Captain(EntityType<? extends Z0004Captain> Type, Level World) {
        super(Type, World);
        xpReward = 30;
    }

    public static AttributeSupplier.Builder createCaptainAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, CaptainHealth).add(Attributes.ATTACK_DAMAGE, CaptainDamage).add(Attributes.ARMOR, CaptainArmor)
                .add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.FOLLOW_RANGE, FollowRange).add(Attributes.KNOCKBACK_RESISTANCE, CaptainKnockback)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    public boolean captain() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel World && tickCount % 10 == 0) {
            Bar.setProgress(Mth.clamp(getHealth() / getMaxHealth(), 0.0f, 1.0f));
            for (ServerPlayer Viewer : World.players()) {
                boolean Near = isAlive() && Viewer.distanceToSqr(this) < BarRange * BarRange;
                if (Near && !Bar.getPlayers().contains(Viewer)) {
                    Bar.addPlayer(Viewer);
                } else if (!Near && Bar.getPlayers().contains(Viewer)) {
                    Bar.removePlayer(Viewer);
                }
            }
        }
    }

    @Override
    public void die(DamageSource Source) {
        super.die(Source);
        Bar.removeAllPlayers();
    }

    @Override
    public void remove(RemovalReason Reason) {
        Bar.removeAllPlayers();
        super.remove(Reason);
    }

    public ServerBossEvent bar() {
        return Bar;
    }
}
