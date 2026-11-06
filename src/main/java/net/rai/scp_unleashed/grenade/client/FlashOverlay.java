package net.rai.scp_unleashed.grenade.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.grenade.FlashCurves;

import java.util.ArrayList;
import java.util.List;

public final class FlashOverlay {
    public static final ResourceLocation LayerId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "grenade_flash");
    public static final int WhiteColour = 0xFFFFFF;
    public static final int TailColour = 0xFFF2D8;
    public static final float ShownFloor = 0.004F;
    public static final int MaxFlashes = 4;

    private static final List<Flash> Flashes = new ArrayList<>();
    private static int Clock;

    private FlashOverlay() {
    }

    public static void flash(float White, float Ring) {
        if (White <= 0.0F && Ring <= 0.0F) {
            return;
        }
        if (Flashes.size() >= MaxFlashes) {
            Flashes.remove(0);
        }
        Flashes.add(new Flash(Math.max(0.0F, White), Math.max(0.0F, Ring), Clock));
        if (Ring > 0.0F) {
            FlashRinging.start();
        }
    }

    public static void tick() {
        if (Minecraft.getInstance().isPaused()) {
            return;
        }
        Clock++;
        Flashes.removeIf(Each -> Clock - Each.Start() > FlashCurves.lasts(Each.White(), Each.Ring()));
        FlashRinging.tick(ring());
    }

    public static void reset() {
        Flashes.clear();
        Clock = 0;
        FlashRinging.reset();
    }

    private static float age(Flash Each, float PartialTick) {
        return Clock - Each.Start() + PartialTick;
    }

    public static float white(float PartialTick) {
        float Level = 0.0F;
        for (Flash Each : Flashes) {
            Level = Math.max(Level, FlashCurves.white(Each.White(), age(Each, PartialTick)));
        }
        return Level;
    }

    public static float blur(float PartialTick) {
        float Level = 0.0F;
        for (Flash Each : Flashes) {
            Level = Math.max(Level, FlashCurves.blur(Each.White(), age(Each, PartialTick)));
        }
        return Level;
    }

    public static float ring() {
        float Level = 0.0F;
        for (Flash Each : Flashes) {
            Level = Math.max(Level, FlashCurves.ring(Each.Ring(), age(Each, 0.0F)));
        }
        return Level;
    }

    public static float muffle() {
        float Level = 0.0F;
        for (Flash Each : Flashes) {
            Level = Math.max(Level, FlashCurves.muffle(Each.Ring(), age(Each, 0.0F)));
        }
        return Level;
    }

    public static int colour(float Level) {
        float Tail = 1.0F - Mth.clamp(Level, 0.0F, 1.0F);
        int Red = Mth.floor(Mth.lerp(Tail, WhiteColour >> 16 & 255, TailColour >> 16 & 255));
        int Green = Mth.floor(Mth.lerp(Tail, WhiteColour >> 8 & 255, TailColour >> 8 & 255));
        int Blue = Mth.floor(Mth.lerp(Tail, WhiteColour & 255, TailColour & 255));
        int Alpha = Mth.clamp(Math.round(Level * 255.0F), 0, 255);
        return Alpha << 24 | Red << 16 | Green << 8 | Blue;
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null) {
            return;
        }
        float Level = white(Delta.getGameTimeDeltaPartialTick(false));
        if (Level < ShownFloor) {
            return;
        }
        Graphics.fill(0, 0, Graphics.guiWidth(), Graphics.guiHeight(), colour(Level));
    }

    private record Flash(float White, float Ring, int Start) {
    }
}
