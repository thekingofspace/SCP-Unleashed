package net.rai.scp_unleashed.scp538.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.rai.scp_unleashed.scp538.Scp538Registry;
import net.rai.scp_unleashed.senses.Senses;

import java.util.ArrayList;
import java.util.List;

public final class Scp538Crawlers {
    public static final ResourceLocation LayerId = Scp538Registry.location("scp538_crawlers");
    public static final ResourceLocation Sheet = Scp538Registry.location("textures/gui/scp538/crawlers.png");
    public static final int Cell = 16;
    public static final int Frames = 4;
    public static final int Headings = 8;
    public static final int SheetWidth = Cell * Frames;
    public static final int SheetHeight = Cell * Headings;
    public static final int UnitTarget = 180;
    public static final float StridePixels = 2.5f;
    public static final int[] GapMin = {0, 280, 160, 70, 24};
    public static final int[] GapMax = {0, 460, 260, 130, 50};
    public static final int[] Bunch = {0, 1, 1, 2, 3};
    public static final int FirstDelay = 80;
    public static final int MaxShown = 6;
    public static final int FadeIn = 6;
    public static final int FadeOut = 10;
    public static final float MaxAlpha = 0.82f;
    public static final int TurnOdds = 34;
    private static final List<Crawler> Shown = new ArrayList<>();
    private static int Countdown = -1;
    private static int LastStage;

    private Scp538Crawlers() {
    }

    public static final class Crawler {
        public float X;
        public float Y;
        public float LastX;
        public float LastY;
        public int Heading;
        public final float Speed;
        public final int Size;
        public int Age;
        public final int Life;
        public float Travelled;
        public int Burst;
        public int Pause;

        public Crawler(float X, float Y, int Heading, float Speed, int Size, int Life) {
            this.X = X;
            this.Y = Y;
            this.LastX = X;
            this.LastY = Y;
            this.Heading = Heading;
            this.Speed = Speed;
            this.Size = Size;
            this.Life = Life;
        }

        public int frame() {
            return Math.floorMod((int) (Travelled / StridePixels), Frames);
        }

        public float alpha(float Partial) {
            float Time = Age + Partial;
            float In = Mth.clamp(Time / FadeIn, 0.0f, 1.0f);
            float Out = Mth.clamp((Life - Time) / FadeOut, 0.0f, 1.0f);
            return MaxAlpha * Math.min(In, Out);
        }
    }

    public static float stepX(int Heading) {
        return Mth.sin(Heading * Mth.PI / 4.0f);
    }

    public static float stepY(int Heading) {
        return -Mth.cos(Heading * Mth.PI / 4.0f);
    }

    public static int unit(int Width, int Height) {
        return Math.max(1, Math.round(Math.min(Width, Height) / (float) UnitTarget));
    }

    public static List<Crawler> shown() {
        return Shown;
    }

    public static void reset() {
        Shown.clear();
        Countdown = -1;
        LastStage = 0;
    }

    public static void tick(Minecraft Client) {
        int Stage = Math.min(Scp538ClientState.stage(), GapMin.length - 1);
        if (Client.player == null || Client.level == null || Stage <= 0 || Client.isPaused()) {
            if (Stage <= 0) {
                reset();
            }
            return;
        }
        if (LastStage == 0) {
            Countdown = FirstDelay;
        }
        LastStage = Stage;
        int Width = Client.getWindow().getGuiScaledWidth();
        int Height = Client.getWindow().getGuiScaledHeight();
        RandomSource Random = Client.level.getRandom();
        for (Crawler Each : List.copyOf(Shown)) {
            advance(Each, Random, Width, Height);
        }
        Shown.removeIf(Each -> Each.Age >= Each.Life || outside(Each, Width, Height));
        if (--Countdown <= 0) {
            int Count = 1 + Random.nextInt(Bunch[Stage]);
            for (int Index = 0; Index < Count && Shown.size() < MaxShown; Index++) {
                Shown.add(spawn(Random, Width, Height));
            }
            Countdown = GapMin[Stage] + Random.nextInt(Math.max(1, GapMax[Stage] - GapMin[Stage]));
            if (!Senses.isDeafened(Client.player)) {
                Client.getSoundManager().play(SimpleSoundInstance.forUI(Scp538Registry.Crawl.get(), 0.9f + Random.nextFloat() * 0.2f, 0.22f + 0.04f * Stage));
            }
        }
    }

    public static Crawler spawn(RandomSource Random, int Width, int Height) {
        int Size = unit(Width, Height) + (Random.nextInt(3) == 0 ? 1 : 0);
        int Span = Cell * Size;
        int Edge = Random.nextInt(4);
        float X;
        float Y;
        int Inward;
        switch (Edge) {
            case 0 -> {
                X = Random.nextFloat() * Width;
                Y = -Span * 0.5f;
                Inward = 4;
            }
            case 1 -> {
                X = Width + Span * 0.5f;
                Y = Random.nextFloat() * Height;
                Inward = 6;
            }
            case 2 -> {
                X = Random.nextFloat() * Width;
                Y = Height + Span * 0.5f;
                Inward = 0;
            }
            default -> {
                X = -Span * 0.5f;
                Y = Random.nextFloat() * Height;
                Inward = 2;
            }
        }
        int Heading = Math.floorMod(Inward + Random.nextInt(3) - 1, Headings);
        float Speed = (1.6f + Random.nextFloat() * 1.4f) * Size;
        int Life = 60 + Random.nextInt(70);
        return new Crawler(X, Y, Heading, Speed, Size, Life);
    }

    public static void advance(Crawler Each, RandomSource Random, int Width, int Height) {
        Each.LastX = Each.X;
        Each.LastY = Each.Y;
        Each.Age++;
        if (Each.Pause > 0) {
            Each.Pause--;
            return;
        }
        if (Each.Burst <= 0) {
            Each.Burst = 6 + Random.nextInt(10);
            if (Random.nextInt(3) == 0) {
                Each.Pause = 2 + Random.nextInt(6);
                return;
            }
        }
        Each.Burst--;
        if (Random.nextInt(TurnOdds) == 0) {
            Each.Heading = Math.floorMod(Each.Heading + (Random.nextBoolean() ? 1 : -1), Headings);
        }
        Each.X += stepX(Each.Heading) * Each.Speed;
        Each.Y += stepY(Each.Heading) * Each.Speed;
        Each.Travelled += Each.Speed / Each.Size;
    }

    public static boolean outside(Crawler Each, int Width, int Height) {
        float Margin = Cell * Each.Size;
        return Each.Age > FadeIn && (Each.X < -Margin || Each.Y < -Margin || Each.X > Width + Margin || Each.Y > Height + Margin);
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || Shown.isEmpty() || Senses.isBlindfolded(Client.player)) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(true);
        PoseStack Pose = Graphics.pose();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (Crawler Each : Shown) {
            float Alpha = Each.alpha(Partial);
            if (Alpha <= 0.01f) {
                continue;
            }
            float X = Mth.lerp(Partial, Each.LastX, Each.X);
            float Y = Mth.lerp(Partial, Each.LastY, Each.Y);
            int Left = Math.round(X - Cell * Each.Size * 0.5f);
            int Top = Math.round(Y - Cell * Each.Size * 0.5f);
            Graphics.setColor(1.0f, 1.0f, 1.0f, Alpha);
            Pose.pushPose();
            Pose.translate(Left, Top, 0.0f);
            Pose.scale(Each.Size, Each.Size, 1.0f);
            Graphics.blit(Sheet, 0, 0, Each.frame() * Cell, Each.Heading * Cell, Cell, Cell, SheetWidth, SheetHeight);
            Pose.popPose();
        }
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}
