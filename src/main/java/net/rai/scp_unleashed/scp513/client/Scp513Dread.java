package net.rai.scp_unleashed.scp513.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp513.Scp513Registry;
import net.rai.scp_unleashed.scp513.Scp513Stage;
import net.rai.scp_unleashed.shadercompat.client.VeilPost;

import java.util.Arrays;
import java.util.List;

public final class Scp513Dread {
    public static final ResourceLocation LayerId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp513_dread");
    public static final ResourceLocation MiasmaLayerId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp513_miasma");
    public static final ResourceLocation Pipeline = VeilPost.id("scp513_dread");
    public static final int TexelTarget = 110;
    public static final int VignetteColour = 0x08070A;
    public static final int MiasmaColour = 0x0B0A0D;
    public static final float[] RingAlphas = {0.62f, 0.46f, 0.34f, 0.24f, 0.15f, 0.08f};
    public static final int RingWidth = 2;
    public static final float CornerCut = 0.62f;
    public static final int CloseRings = 26;
    public static final float CloseAlpha = 0.97f;
    public static final int IntensitySteps = 16;
    public static final int FadeOutTicks = 30;
    public static final float PulseBoost = 0.12f;
    public static final int PulseTicks = 4;
    public static final int SlowBeat = 30;
    public static final int FastBeat = 13;
    public static final int RacingBeat = 9;
    public static final int DubDelay = 5;
    public static final float QuietBeat = 0.25f;
    public static final float LoudBeat = 0.65f;
    public static final int DroneMin = 160;
    public static final int DroneMax = 260;
    public static final float DroneQuiet = 0.25f;
    public static final float DroneLoud = 0.55f;
    public static final int BreathEvery = 46;
    public static final float BreathVolume = 0.4f;
    public static final int TendrilCount = 14;
    public static final int TendrilLength = 11;
    public static final float TendrilAlpha = 0.55f;
    private static int Age;
    private static int NextBeat;
    private static int Dub = -1;
    private static int Beat;
    private static int NextDrone;
    private static int Fade;
    private static boolean Running;
    private static long CachedKey = Long.MIN_VALUE;
    private static int[] Rects = new int[0];
    private static int RectCount;
    private static int[][] Tendrils = new int[0][];

    private Scp513Dread() {
    }

    public static void begin() {
        Running = true;
        Age = 0;
        NextBeat = 10;
        Dub = -1;
        Beat = 0;
        NextDrone = 40;
        Fade = FadeOutTicks;
        CachedKey = Long.MIN_VALUE;
        LocalPlayer User = Minecraft.getInstance().player;
        RandomSource Random = User != null ? User.getRandom() : RandomSource.create();
        Tendrils = new int[TendrilCount][];
        for (int Index = 0; Index < TendrilCount; Index++) {
            Tendrils[Index] = new int[]{Random.nextInt(4), Random.nextInt(1000), 4 + Random.nextInt(TendrilLength - 3), Random.nextInt(3) - 1, Random.nextInt(1000)};
        }
        play(Scp513Registry.DreadLayers, 0.8f, DroneQuiet);
    }

    public static void end() {
        Running = false;
        Fade = FadeOutTicks;
        Dub = -1;
    }

    public static void reset() {
        Running = false;
        Fade = 0;
        Age = 0;
        Dub = -1;
    }

    private static void play(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Pitch, float Volume) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            play(Layer, Pitch, Volume);
        }
    }

    private static void play(DeferredHolder<SoundEvent, SoundEvent> Layer, float Pitch, float Volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(Layer.get(), Pitch, Volume));
    }

    public static float strength(float Partial) {
        if (!Running) {
            return Fade <= 0 ? 0.0f : Fade / (float) FadeOutTicks * 0.6f;
        }
        float Elapsed = Scp513ClientState.elapsed() + Partial;
        Scp513Stage Stage = Scp513Stage.of((int) Elapsed);
        return switch (Stage) {
            case Dread -> 0.18f + 0.47f * Scp513Stage.dreadShare((int) Elapsed);
            case Frozen, Approach -> 0.66f + 0.14f * Mth.clamp((Elapsed - Scp513Stage.FreezeAt) / (Scp513Stage.GrabAt - Scp513Stage.FreezeAt), 0.0f, 1.0f);
            case Grab, Strangle -> 0.85f;
            default -> 0.0f;
        };
    }

    public static float closing(float Partial) {
        if (!Running) {
            return 0.0f;
        }
        float Elapsed = Scp513ClientState.elapsed() + Partial;
        return Mth.clamp((Elapsed - Scp513Stage.StrangleAt) / Scp513Stage.StrangleTicks, 0.0f, 1.0f);
    }

    public static void tick(Minecraft Client) {
        LocalPlayer User = Client.player;
        if (User == null) {
            reset();
            return;
        }
        if (!Running) {
            if (Fade > 0) {
                Fade--;
            }
            return;
        }
        if (Client.isPaused()) {
            return;
        }
        Age++;
        if (Beat > 0) {
            Beat--;
        }
        int Elapsed = Scp513ClientState.elapsed();
        Scp513Stage Stage = Scp513Stage.of(Elapsed);
        float Share = Scp513Stage.dreadShare(Elapsed);
        if (Age >= NextBeat) {
            float Volume = Stage == Scp513Stage.Dread ? Mth.lerp(Share, QuietBeat, LoudBeat) : LoudBeat;
            play(Scp513Registry.Heartbeat, 0.95f + User.getRandom().nextFloat() * 0.05f, Volume);
            Dub = DubDelay;
            Beat = PulseTicks;
            int Gap = switch (Stage) {
                case Dread -> Math.round(Mth.lerp(Share, SlowBeat, FastBeat));
                case Frozen, Approach -> FastBeat;
                default -> RacingBeat;
            };
            NextBeat = Age + Gap;
        }
        if (Dub >= 0 && Dub-- == 0) {
            float Volume = Stage == Scp513Stage.Dread ? Mth.lerp(Share, QuietBeat, LoudBeat) * 0.8f : LoudBeat * 0.8f;
            play(Scp513Registry.HeartbeatThump, 0.9f + User.getRandom().nextFloat() * 0.05f, Volume);
        }
        if (Age >= NextDrone && !Stage.seized()) {
            play(Scp513Registry.DreadLayers, 0.75f + User.getRandom().nextFloat() * 0.1f, Mth.lerp(Share, DroneQuiet, DroneLoud));
            NextDrone = Age + DroneMin + User.getRandom().nextInt(DroneMax - DroneMin);
        }
        if (Stage.frozen() && !Stage.seized() && Age % BreathEvery == 0) {
            play(Scp513Registry.Breath, 0.9f + User.getRandom().nextFloat() * 0.1f, BreathVolume);
        }
    }

    public static int texel(int Width, int Height) {
        return Math.max(2, Math.round(Math.min(Width, Height) / (float) TexelTarget));
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || Client.getCameraEntity() != Client.player) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        float Amount = strength(Partial) + (Beat > 0 ? PulseBoost * strength(Partial) : 0.0f);
        float Close = closing(Partial);
        if (Amount <= 0.0f && Close <= 0.0f) {
            return;
        }
        Graphics.flush();
        if (!VeilPost.run(Pipeline, Values -> Values.set("Dread", Mth.clamp(Amount, 0.0f, 1.0f)).set("Close", Close))) {
            vignette(Client, Graphics, Amount, Close);
        }
    }

    public static void renderMiasma(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || Client.options.hideGui || Client.getCameraEntity() != Client.player) {
            return;
        }
        float Amount = Mth.clamp(strength(Delta.getGameTimeDeltaPartialTick(false)) * 1.15f - 0.25f, 0.0f, 1.0f);
        if (Amount <= 0.0f) {
            return;
        }
        Window Display = Client.getWindow();
        int Width = Display.getWidth();
        int Height = Display.getHeight();
        int Texel = texel(Width, Height);
        int Columns = (Width + Texel - 1) / Texel;
        int Rows = (Height + Texel - 1) / Texel;
        int OffsetX = (Width - Columns * Texel) / 2;
        int OffsetY = (Height - Rows * Texel) / 2;
        float Scale = (float) Display.getGuiScale();
        PoseStack Pose = Graphics.pose();
        Pose.pushPose();
        Pose.scale(1.0f / Scale, 1.0f / Scale, 1.0f);
        int Sway = Age / 12;
        for (int[] Tendril : Tendrils) {
            int Side = Tendril[0];
            int Length = Math.round(Tendril[2] * Amount);
            boolean Vertical = Side < 2;
            int Span = Vertical ? Columns : Rows;
            int Along = Mth.clamp(Math.round(Tendril[1] / 1000.0f * Span), 2, Math.max(2, Span - 3));
            int Wobble = ((Sway + Tendril[4]) % 3) - 1;
            for (int Step = 0; Step < Length; Step++) {
                int Drift = Tendril[3] * (Step / 3) + (Step > Length / 2 ? Wobble : 0);
                int Column;
                int Row;
                if (Vertical) {
                    Column = Along + Drift;
                    Row = Side == 0 ? Step : Rows - 1 - Step;
                } else {
                    Row = Along + Drift;
                    Column = Side == 2 ? Step : Columns - 1 - Step;
                }
                float Taper = 1.0f - Step / (float) Math.max(1, Length) * 0.6f;
                int Colour = argb(TendrilAlpha * Amount * Taper, MiasmaColour);
                int Thick = Step < 3 ? 2 : 1;
                int X = OffsetX + Column * Texel;
                int Y = OffsetY + Row * Texel;
                Graphics.fill(X, Y, X + Texel * (Vertical ? Thick : 1), Y + Texel * (Vertical ? 1 : Thick), Colour);
            }
        }
        Pose.popPose();
    }

    private static void vignette(Minecraft Client, GuiGraphics Graphics, float Amount, float Close) {
        Window Display = Client.getWindow();
        int Width = Display.getWidth();
        int Height = Display.getHeight();
        int Texel = texel(Width, Height);
        int Columns = (Width + Texel - 1) / Texel;
        int Rows = (Height + Texel - 1) / Texel;
        int OffsetX = (Width - Columns * Texel) / 2;
        int OffsetY = (Height - Rows * Texel) / 2;
        float Scale = (float) Display.getGuiScale();
        int Step = Math.round(Mth.clamp(Amount, 0.0f, 1.0f) * IntensitySteps);
        int Shut = Math.round(Close * CloseRings);
        if (Step <= 0 && Shut <= 0) {
            return;
        }
        PoseStack Pose = Graphics.pose();
        Pose.pushPose();
        Pose.scale(1.0f / Scale, 1.0f / Scale, 1.0f);
        long Key = (((long) Width * 31L + Height) * 64L + Step) * 64L + Shut;
        if (Key != CachedKey) {
            build(Columns, Rows, OffsetX, OffsetY, Texel, Step / (float) IntensitySteps, Shut);
            CachedKey = Key;
        }
        for (int Index = 0; Index < RectCount; Index++) {
            int Base = Index * 5;
            Graphics.fill(Rects[Base], Rects[Base + 1], Rects[Base + 2], Rects[Base + 3], Rects[Base + 4]);
        }
        Pose.popPose();
        Graphics.flush();
    }

    private static void build(int Columns, int Rows, int OffsetX, int OffsetY, int Texel, float Amount, int Shut) {
        RectCount = 0;
        int Rings = RingAlphas.length;
        for (int Row = 0; Row < Rows; Row++) {
            int RunStart = 0;
            int RunColour = 0;
            for (int Column = 0; Column <= Columns; Column++) {
                int Colour = 0;
                if (Column < Columns) {
                    int Dx = Math.min(Column, Columns - 1 - Column);
                    int Dy = Math.min(Row, Rows - 1 - Row);
                    int Depth = (int) Math.min(Math.min(Dx, Dy), (Dx + Dy) * CornerCut);
                    if (Depth < Shut) {
                        Colour = argb(CloseAlpha, VignetteColour);
                    } else {
                        int Ring = (Depth - Shut) / RingWidth;
                        if (Ring < Rings && Amount > 0.0f) {
                            Colour = argb(RingAlphas[Ring] * Amount, VignetteColour);
                        }
                    }
                }
                if (Colour != RunColour) {
                    if (RunColour != 0) {
                        add(OffsetX + RunStart * Texel, OffsetY + Row * Texel, OffsetX + Column * Texel, OffsetY + (Row + 1) * Texel, RunColour);
                    }
                    RunStart = Column;
                    RunColour = Colour;
                }
            }
        }
    }

    private static int argb(float Alpha, int Colour) {
        int A = Math.max(1, Math.min(255, Math.round(Alpha * 255.0f)));
        return A << 24 | Colour & 0xFFFFFF;
    }

    private static void add(int X0, int Y0, int X1, int Y1, int Colour) {
        int Base = RectCount * 5;
        if (Base + 5 > Rects.length) {
            Rects = Arrays.copyOf(Rects, Math.max(64, Rects.length * 2));
        }
        Rects[Base] = X0;
        Rects[Base + 1] = Y0;
        Rects[Base + 2] = X1;
        Rects[Base + 3] = Y1;
        Rects[Base + 4] = Colour;
        RectCount++;
    }
}
