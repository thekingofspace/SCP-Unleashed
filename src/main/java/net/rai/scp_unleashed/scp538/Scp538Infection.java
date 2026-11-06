package net.rai.scp_unleashed.scp538;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import net.rai.scp_unleashed.control.ControlManager;

public final class Scp538Infection {
    public static final int Duration = 6000;
    public static final int[] StageStarts = {0, 1500, 3600, 5400};
    public static final int Stages = StageStarts.length;
    public static final int[] FlakeOdds = {0, 160, 70, 30};
    public static final int[] RustleOdds = {0, 0, 400, 140};
    public static final float LethalFloor = 1000.0f;

    private Scp538Infection() {
    }

    public static boolean isInfected(LivingEntity Subject) {
        return Subject.getExistingDataOrNull(Scp538Registry.Infection.get()) != null;
    }

    public static int elapsed(LivingEntity Subject) {
        Integer Found = Subject.getExistingDataOrNull(Scp538Registry.Infection.get());
        return Found == null ? 0 : Found;
    }

    public static int stageOf(int Elapsed) {
        int Stage = 1;
        for (int Index = 1; Index < Stages; Index++) {
            if (Elapsed >= StageStarts[Index]) {
                Stage = Index + 1;
            }
        }
        return Stage;
    }

    public static int stage(LivingEntity Subject) {
        return isInfected(Subject) ? stageOf(elapsed(Subject)) : 0;
    }

    public static int progress(LivingEntity Subject) {
        return isInfected(Subject) ? Mth.clamp(elapsed(Subject) * 100 / Duration, 0, 100) : 0;
    }

    public static boolean isPaused(LivingEntity Subject) {
        return Subject instanceof Player User && (User.isCreative() || User.isSpectator());
    }

    public static boolean isExempt(LivingEntity Subject) {
        return InfectionImmunity.isImmune(Subject, Scp538Registry.Immune);
    }

    public static boolean infect(LivingEntity Subject) {
        if (!(Subject.level() instanceof ServerLevel World) || !Subject.isAlive() || isInfected(Subject) || isExempt(Subject) || !Scp538Swarm.isPerson(Subject)) {
            return false;
        }
        Subject.setData(Scp538Registry.Infection.get(), 0);
        Subject.getData(Scp538Registry.Trace.get()).LastStage = 1;
        Scp538Swarm.playLayers(World, Subject.getEyePosition(), Scp538Registry.OnsetLayers, 0.6f, 1.0f);
        World.sendParticles(Scp538Swarm.Shade, Subject.getX(), Subject.getY() + 0.1, Subject.getZ(), 8, 0.25, 0.05, 0.25, 0.0);
        tell(Subject, 1);
        Scp538Sync.update(Subject);
        return true;
    }

    public static boolean cure(LivingEntity Subject) {
        boolean Was = isInfected(Subject);
        Subject.removeData(Scp538Registry.Infection.get());
        Scp538Sync.update(Subject);
        return Was;
    }

    public static void setElapsed(LivingEntity Subject, int Elapsed) {
        Subject.setData(Scp538Registry.Infection.get(), Math.max(0, Elapsed));
        Subject.getData(Scp538Registry.Trace.get()).LastStage = stageOf(Elapsed);
        Scp538Sync.update(Subject);
    }

    public static void tick(LivingEntity Subject) {
        if (!(Subject.level() instanceof ServerLevel World) || !Subject.isAlive()) {
            return;
        }
        if (isPaused(Subject)) {
            Scp538Sync.update(Subject);
            return;
        }
        if (isExempt(Subject)) {
            cure(Subject);
            return;
        }
        int Now = elapsed(Subject) + 1;
        Subject.setData(Scp538Registry.Infection.get(), Now);
        Scp538Trace Trace = Subject.getData(Scp538Registry.Trace.get());
        int Stage = stageOf(Now);
        if (Stage != Trace.LastStage) {
            boolean Grew = Stage > Trace.LastStage;
            Trace.LastStage = Stage;
            if (Grew) {
                tell(Subject, Stage);
                Scp538Swarm.playLayers(World, Subject.getEyePosition(), Scp538Registry.CrawlLayers, 0.35f + 0.1f * Stage, 0.95f);
            }
        }
        ambience(World, Subject, Stage);
        Scp538Sync.update(Subject);
        if (Now >= Duration) {
            consume(World, Subject);
        }
    }

    public static void consume(ServerLevel World, LivingEntity Subject) {
        DamageSource Source = Subject.damageSources().source(Scp538Registry.ShadowDamage);
        Subject.hurt(Source, Math.max(LethalFloor, Subject.getMaxHealth() * 4.0f));
        if (Subject instanceof ServerPlayer User && ControlManager.hasPendingDeath(User)) {
            return;
        }
        if (Subject.isAlive() && isInfected(Subject)) {
            Scp538Swarm.burst(World, Subject);
            cure(Subject);
        }
    }

    public static void died(ServerLevel World, LivingEntity Dead) {
        if (!isInfected(Dead)) {
            return;
        }
        Scp538Swarm.burst(World, Dead);
        Dead.removeData(Scp538Registry.Infection.get());
        Scp538Sync.update(Dead);
    }

    private static void ambience(ServerLevel World, LivingEntity Subject, int Stage) {
        int Index = Mth.clamp(Stage, 1, Stages) - 1;
        if (FlakeOdds[Index] > 0 && Subject.getRandom().nextInt(FlakeOdds[Index]) == 0) {
            World.sendParticles(Scp538Swarm.Shade, Subject.getX(), Subject.getY() + 0.05, Subject.getZ(), 2 + Stage, Subject.getBbWidth() * 0.6, 0.02,
                    Subject.getBbWidth() * 0.6, 0.0);
        }
        if (RustleOdds[Index] > 0 && Subject.getRandom().nextInt(RustleOdds[Index]) == 0) {
            Scp538Swarm.playLayers(World, Subject.position(), Scp538Registry.SkitterLayers, 0.25f, 1.1f + Subject.getRandom().nextFloat() * 0.2f);
        }
    }

    private static void tell(LivingEntity Subject, int Stage) {
        if (Subject instanceof ServerPlayer Victim) {
            Victim.displayClientMessage(Component.translatable("message.scp_unleashed.scp538.stage." + Stage).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), true);
        }
    }
}
