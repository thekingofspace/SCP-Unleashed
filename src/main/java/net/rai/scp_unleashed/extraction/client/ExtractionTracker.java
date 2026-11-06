package net.rai.scp_unleashed.extraction.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.anomaly.Extractable;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.extraction.ExtractionRegistry;
import net.rai.scp_unleashed.extraction.ExtractionTargeting;
import net.rai.scp_unleashed.extraction.RiftExtractorItem;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class ExtractionTracker {
    public static final double TrackRange = 64.0;
    public static final int CompleteTicks = 26;
    public static final int LostTicks = 12;
    public static final int FadeTicks = 8;
    public static final int GraceTicks = 4;
    private static final float RaiseStep = 0.2F;
    private static final Map<Integer, Session> Sessions = new HashMap<>();
    private static final Map<Integer, ExtractionLoopSound> Loops = new HashMap<>();
    private static float Raise;
    private static float RaiseO;
    private static HumanoidArm RaisedArm = HumanoidArm.RIGHT;
    private static HudState State = HudState.Hidden;
    private static FragmentRarity HudGrade = FragmentRarity.Faint;
    private static int HudColour = FragmentRarity.Faint.getColour();
    private static String HudLabel = FragmentRarity.Faint.getSerializedName();
    private static float HudProgress;
    private static float HudProgressO;
    private static float HudAlpha;
    private static float HudAlphaO;
    private static int HudTimer;

    private ExtractionTracker() {
    }

    public enum HudState {
        Hidden, Active, Complete, Lost
    }

    public record HudView(FragmentRarity Grade, int Colour, String Label, float Progress, float Alpha, HudState State, int Timer) {
    }

    public static final class Session {
        private final int UserId;
        private int TargetId;
        private FragmentRarity Grade = FragmentRarity.Faint;
        private int Colour = FragmentRarity.Faint.getColour();
        private String Label = FragmentRarity.Faint.getSerializedName();
        private float Progress;
        private float ProgressO;
        private boolean Started;
        private int Misses;

        private Session(int UserId) {
            this.UserId = UserId;
        }

        private void update(int TargetId, FragmentRarity Grade, int Colour, String Label, float Progress) {
            boolean Fresh = !Started || this.TargetId != TargetId;
            this.TargetId = TargetId;
            this.Grade = Grade;
            this.Colour = Colour;
            this.Label = Label;
            this.ProgressO = Fresh ? Progress : this.Progress;
            this.Progress = Progress;
            this.Started = true;
            this.Misses = 0;
        }

        private int miss() {
            ProgressO = Progress;
            return ++Misses;
        }

        public int getUserId() {
            return UserId;
        }

        public int getTargetId() {
            return TargetId;
        }

        public FragmentRarity getGrade() {
            return Grade;
        }

        public int getColour() {
            return Colour;
        }

        public String getLabel() {
            return Label;
        }

        public float getProgress() {
            return Progress;
        }

        public float getProgress(float PartialTick) {
            return Mth.lerp(PartialTick, ProgressO, Progress);
        }

        public boolean isLocked() {
            return Misses == 0;
        }
    }

    public static boolean isExtracting(LivingEntity User) {
        return User.isUsingItem() && User.getUseItem().getItem() instanceof RiftExtractorItem;
    }

    public static HumanoidArm armOf(LivingEntity User, InteractionHand Hand) {
        return Hand == InteractionHand.MAIN_HAND ? User.getMainArm() : User.getMainArm().getOpposite();
    }

    public static Collection<Session> sessions() {
        return Collections.unmodifiableCollection(Sessions.values());
    }

    @Nullable
    public static Session sessionFor(int UserId) {
        return Sessions.get(UserId);
    }

    public static float raise(float PartialTick) {
        float Value = Mth.lerp(PartialTick, RaiseO, Raise);
        return Value * Value * (3.0F - 2.0F * Value);
    }

    public static HumanoidArm raisedArm() {
        return RaisedArm;
    }

    public static float tremor(float PartialTick, float Phase) {
        Minecraft Game = Minecraft.getInstance();
        if (Game.level == null || State != HudState.Active) {
            return 0.0F;
        }
        float Progress = Mth.lerp(PartialTick, HudProgressO, HudProgress);
        float Time = Game.level.getGameTime() + PartialTick;
        float Amplitude = 0.0015F + 0.009F * Progress * Progress;
        return Amplitude * Mth.sin(Time * 1.9F + Phase) * Mth.cos(Time * 0.77F + Phase * 2.3F);
    }

    @Nullable
    public static HudView hudView(float PartialTick) {
        if (State == HudState.Hidden) {
            return null;
        }
        return new HudView(HudGrade, HudColour, HudLabel, Mth.lerp(PartialTick, HudProgressO, HudProgress), Mth.lerp(PartialTick, HudAlphaO, HudAlpha), State, HudTimer);
    }

    public static void tick(Minecraft Game) {
        ClientLevel World = Game.level;
        LocalPlayer Local = Game.player;
        if (World == null || Local == null) {
            reset();
            return;
        }
        Set<Integer> Seen = new HashSet<>();
        for (AbstractClientPlayer User : World.players()) {
            if (!isExtracting(User) || User.distanceToSqr(Local) > TrackRange * TrackRange) {
                continue;
            }
            Entity Target = ExtractionTargeting.findTarget(User);
            Session Current = Sessions.get(User.getId());
            if (Target instanceof Extractable Rift) {
                Sessions.computeIfAbsent(User.getId(), Session::new).update(Target.getId(), Rift.getRarity(), Rift.getRiftColour(), Rift.getRiftLabel(), Rift.getExtraction());
                Seen.add(User.getId());
            } else if (Current != null && Current.miss() <= GraceTicks) {
                Seen.add(User.getId());
            }
        }
        Sessions.keySet().retainAll(Seen);
        tickLoops(Game, World);
        tickRaise(Local);
        tickHud(Local);
    }

    private static void tickLoops(Minecraft Game, ClientLevel World) {
        Iterator<Map.Entry<Integer, ExtractionLoopSound>> Entries = Loops.entrySet().iterator();
        while (Entries.hasNext()) {
            ExtractionLoopSound Loop = Entries.next().getValue();
            if (Loop.isStopped() || !Game.getSoundManager().isActive(Loop)) {
                Entries.remove();
            }
        }
        for (Session Current : Sessions.values()) {
            if (!Loops.containsKey(Current.getUserId()) && World.getEntity(Current.getUserId()) instanceof AbstractClientPlayer User) {
                ExtractionLoopSound Loop = new ExtractionLoopSound(User);
                Loops.put(Current.getUserId(), Loop);
                Game.getSoundManager().play(Loop);
            }
        }
    }

    private static void tickRaise(LocalPlayer Local) {
        RaiseO = Raise;
        boolean Using = isExtracting(Local);
        if (Using) {
            RaisedArm = armOf(Local, Local.getUsedItemHand());
        }
        Raise = Mth.clamp(Raise + (Using ? RaiseStep : -RaiseStep), 0.0F, 1.0F);
        if (!Using && !(Local.getItemInHand(handOf(Local, RaisedArm)).getItem() instanceof RiftExtractorItem)) {
            Raise = 0.0F;
            RaiseO = 0.0F;
        }
    }

    private static InteractionHand handOf(LivingEntity User, HumanoidArm Arm) {
        return User.getMainArm() == Arm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    private static void tickHud(LocalPlayer Local) {
        HudAlphaO = HudAlpha;
        HudProgressO = HudProgress;
        Session Mine = Sessions.get(Local.getId());
        if (Mine != null) {
            if (State != HudState.Active) {
                HudProgressO = Mine.getProgress();
            }
            State = HudState.Active;
            HudGrade = Mine.getGrade();
            HudColour = Mine.getColour();
            HudLabel = Mine.getLabel();
            HudProgress = Mine.getProgress();
            HudAlpha = Math.min(1.0F, HudAlpha + 0.34F);
            HudTimer = 0;
            return;
        }
        if (State == HudState.Active) {
            boolean Success = Local.getCooldowns().isOnCooldown(ExtractionRegistry.RiftExtractor.get());
            State = Success ? HudState.Complete : HudState.Lost;
            HudTimer = Success ? CompleteTicks : LostTicks;
            if (Success) {
                HudProgress = 1.0F;
            }
            return;
        }
        if (State == HudState.Hidden) {
            HudAlpha = 0.0F;
            return;
        }
        HudTimer--;
        if (State == HudState.Lost) {
            HudProgress = Math.max(0.0F, HudProgress - 0.08F);
        }
        if (HudTimer < FadeTicks) {
            HudAlpha = Math.max(0.0F, HudAlpha - 1.0F / FadeTicks);
        }
        if (HudTimer <= 0 || HudAlpha <= 0.0F) {
            State = HudState.Hidden;
            HudAlpha = 0.0F;
        }
    }

    private static void reset() {
        Sessions.clear();
        Loops.clear();
        Raise = 0.0F;
        RaiseO = 0.0F;
        State = HudState.Hidden;
        HudAlpha = 0.0F;
        HudAlphaO = 0.0F;
    }
}
