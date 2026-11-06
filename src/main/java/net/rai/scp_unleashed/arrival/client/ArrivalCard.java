package net.rai.scp_unleashed.arrival.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.arrival.ArrivalCards;
import net.rai.scp_unleashed.config.ScpClientConfig;
import net.rai.scp_unleashed.intro.client.IntroClient;
import net.rai.scp_unleashed.mixin.arrival.ArrivalGuiAccess;
import net.rai.scp_unleashed.rocketry.rocket.DropPodEntity;
import net.rai.scp_unleashed.scp024.client.Scp024DialogueScreen;
import net.rai.scp_unleashed.scp093.client.Scp093ClientState;
import net.rai.scp_unleashed.scp2521.client.Scp2521ClientScene;
import net.rai.scp_unleashed.scp3008.client.Scp3008ClientState;
import net.rai.scp_unleashed.scp902.client.Scp902ClientScene;
import org.jetbrains.annotations.Nullable;

public final class ArrivalCard {
    public static final int TitleColour = 0xFFF2D27A;
    public static final int SubtitleColour = 0xFFB4BCC8;
    public static final int BrassColour = 0xFFC9A04A;
    public static final int SettleTicks = 20;
    public static final int PendingLimit = 2400;
    public static final int YieldTicks = 8;
    public static final float Clear = 0.01f;
    @Nullable
    private static ResourceKey<Level> Pending;
    private static int PendingAge;
    private static int Settle;
    @Nullable
    private static ResourceKey<Level> Showing;
    @Nullable
    private static Component Main;
    @Nullable
    private static Component Sub;
    private static int Ticks = -1;
    private static float Fade = 1.0f;
    private static float FadeO = 1.0f;
    private static boolean Yielding;

    private ArrivalCard() {
    }

    public static boolean showing() {
        return Ticks >= 0;
    }

    public static boolean pending() {
        return Pending != null;
    }

    public static void queue(ResourceLocation Dimension) {
        if (!ScpClientConfig.locationTitles()) {
            return;
        }
        ResourceKey<Level> Wanted = ResourceKey.create(Registries.DIMENSION, Dimension);
        if (!ArrivalCards.listed(Wanted)) {
            return;
        }
        if (Showing != null && !Showing.equals(Wanted)) {
            stop();
        }
        Pending = Wanted;
        PendingAge = 0;
        Settle = 0;
    }

    public static void reset() {
        Pending = null;
        PendingAge = 0;
        Settle = 0;
        stop();
    }

    private static void stop() {
        Showing = null;
        Main = null;
        Sub = null;
        Ticks = -1;
        Fade = 1.0f;
        FadeO = 1.0f;
        Yielding = false;
    }

    private static void start(ResourceKey<Level> Dimension) {
        Showing = Dimension;
        Main = Component.translatable(ArrivalCards.titleKey(Dimension));
        Sub = Component.translatable(ArrivalCards.subtitleKey(Dimension));
        Ticks = 0;
        Fade = 1.0f;
        FadeO = 1.0f;
        Yielding = false;
        Pending = null;
        PendingAge = 0;
        Settle = 0;
    }

    public static void tick() {
        Minecraft Client = Minecraft.getInstance();
        if (Client.level == null || Client.player == null) {
            if (Pending != null || Showing != null) {
                reset();
            }
            return;
        }
        if (Client.isPaused()) {
            FadeO = Fade;
            return;
        }
        ResourceKey<Level> Here = Client.level.dimension();
        if (Showing != null) {
            FadeO = Fade;
            if (!Here.equals(Showing)) {
                stop();
            } else {
                if (interrupted(Client)) {
                    Yielding = true;
                }
                if (Yielding) {
                    Fade = Math.max(0.0f, Fade - 1.0f / YieldTicks);
                }
                if (Fade <= 0.0f || ++Ticks > ArrivalCards.Length) {
                    stop();
                }
            }
        }
        if (Pending == null || Showing != null) {
            return;
        }
        PendingAge++;
        if (!ScpClientConfig.locationTitles() || PendingAge > PendingLimit || !Here.equals(Pending)) {
            Pending = null;
            return;
        }
        Settle = blocked(Client) ? 0 : Settle + 1;
        if (Settle >= SettleTicks) {
            start(Pending);
        }
    }

    public static boolean interrupted(Minecraft Client) {
        return IntroClient.active() || IntroClient.titleTicks() >= 0 || IntroClient.veil(1.0f) > Clear || Scp902ClientScene.cinematic() || Scp2521ClientScene.cinematic()
                || Scp093ClientState.flash(1.0f) > Clear || Scp3008ClientState.fadeLevel(1.0f) > Clear || titleShowing(Client) || Client.getCameraEntity() != Client.player;
    }

    public static boolean blocked(Minecraft Client) {
        LocalPlayer User = Client.player;
        if (User == null || Client.level == null || !User.isAlive() || Client.getOverlay() != null) {
            return true;
        }
        if (Client.screen != null && !(Client.screen instanceof Scp024DialogueScreen)) {
            return true;
        }
        if (Client.options.hideGui || User.hasEffect(MobEffects.BLINDNESS)) {
            return true;
        }
        if (User.getVehicle() instanceof DropPodEntity Pod && !Pod.isLanded()) {
            return true;
        }
        return interrupted(Client) || !sectionReady(Client, User);
    }

    public static boolean titleShowing(Minecraft Client) {
        ArrivalGuiAccess Shown = (ArrivalGuiAccess) Client.gui;
        return Shown.scpArrivalTitle() != null && Shown.scpArrivalTitleTime() > 0;
    }

    private static boolean sectionReady(Minecraft Client, LocalPlayer User) {
        BlockPos Feet = User.blockPosition();
        if (Client.level == null || !Client.level.getChunkSource().hasChunk(Feet.getX() >> 4, Feet.getZ() >> 4)) {
            return false;
        }
        return Client.level.isOutsideBuildHeight(Feet.getY()) || Client.levelRenderer.isSectionCompiled(Feet);
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Tracker) {
        Minecraft Client = Minecraft.getInstance();
        if (Ticks < 0 || Main == null || Sub == null || Client.options.hideGui || Client.getCameraEntity() != Client.player) {
            return;
        }
        float Partial = Client.isPaused() ? 0.0f : Tracker.getGameTimeDeltaPartialTick(true);
        draw(Graphics, Client.font, Graphics.guiWidth(), Graphics.guiHeight(), Ticks + Partial, ArrivalCards.Length, Main, Sub, Mth.lerp(Partial, FadeO, Fade));
    }

    public static void draw(GuiGraphics Graphics, Font Shape, int Width, int Height, float Time, int Span, Component Title, Component Subtitle, float Shade) {
        float Share = ArrivalCards.share(Time, Span) * Mth.clamp(Shade, 0.0f, 1.0f);
        int Alpha = Math.round(Share * 255.0f);
        if (Alpha < 8) {
            return;
        }
        int A = Alpha << 24;
        int Middle = Height / 3;
        Graphics.pose().pushPose();
        Graphics.pose().translate(Width / 2.0f, Middle, 0.0f);
        Graphics.pose().scale(ArrivalCards.TitleScale, ArrivalCards.TitleScale, 1.0f);
        Graphics.drawString(Shape, Title, -Shape.width(Title) / 2, ArrivalCards.TitleRise, A | (TitleColour & 0xFFFFFF), true);
        Graphics.pose().popPose();
        int Rule = Math.round(ArrivalCards.ruleShare(Time) * (Shape.width(Title) + ArrivalCards.RulePad));
        Graphics.fill(Width / 2 - Rule, Middle + ArrivalCards.RuleOffset, Width / 2 + Rule, Middle + ArrivalCards.RuleOffset + 1, A | (BrassColour & 0xFFFFFF));
        Graphics.drawString(Shape, Subtitle, (Width - Shape.width(Subtitle)) / 2, Middle + ArrivalCards.SubtitleOffset, A | (SubtitleColour & 0xFFFFFF), true);
    }
}
