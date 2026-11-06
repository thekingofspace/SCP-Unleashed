package net.rai.scp_unleashed.scp2845.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rai.scp_unleashed.scp2845.Scp2845Comet;
import net.rai.scp_unleashed.scp2845.Scp2845Registry;

public final class Scp2845ClientEffects {
    public static final int FlashColour = 0xE8FFF0;
    private static float Flash;
    private static float FlashOld;
    private static float Shake;
    private static long Ticks;

    private Scp2845ClientEffects() {
    }

    public static void cometTick(Scp2845Comet Comet) {
        if (Comet.getPhase() != Scp2845Comet.PhaseFlight || !Comet.isLaunched() || Comet.ticksToImpact() <= 3 || !Comet.claimSounds()) {
            return;
        }
        Minecraft Client = Minecraft.getInstance();
        Client.getSoundManager().play(new Scp2845CometSound(Comet, Scp2845Registry.Descent.get(), 1.0f, 0.6f, 1.2f));
        Client.getSoundManager().play(new Scp2845CometSound(Comet, Scp2845Registry.DescentHum.get(), 0.8f, 0.8f, 1.1f));
        Client.getSoundManager().play(new Scp2845CometSound(Comet, Scp2845Registry.DescentChime.get(), 0.6f, 0.9f, 1.4f));
    }

    public static void impact(BlockPos Centre, float Radius) {
        Minecraft Client = Minecraft.getInstance();
        LocalPlayer Viewer = Client.player;
        ClientLevel World = Client.level;
        if (Viewer == null || World == null) {
            return;
        }
        double Distance = Math.sqrt(Viewer.distanceToSqr(Vec3.atCenterOf(Centre)));
        float Sky = World.canSeeSky(Viewer.blockPosition()) ? 1.0f : 0.35f;
        Flash = Math.max(Flash, (float) Mth.clamp(1.1 - Distance / 200.0, 0.1, 0.9) * Sky);
        Shake = Math.max(Shake, (float) Mth.clamp(1.3 - Distance / 70.0, 0.0, 1.0));
    }

    public static void tick() {
        Ticks++;
        FlashOld = Flash;
        Flash *= 0.85f;
        Shake *= 0.88f;
    }

    public static void shake(ViewportEvent.ComputeCameraAngles Event) {
        if (Shake < 0.01f) {
            return;
        }
        double Time = Ticks + Event.getPartialTick();
        Event.setYaw(Event.getYaw() + (float) (Math.sin(Time * 1.41) * Math.cos(Time * 0.57)) * Shake * 1.5f);
        Event.setPitch(Event.getPitch() + (float) (Math.sin(Time * 1.73 + 1.1) * Math.cos(Time * 0.43)) * Shake * 1.2f);
        Event.setRoll(Event.getRoll() + (float) Math.sin(Time * 1.19 + 0.4) * Shake * 1.6f);
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        float Glare = Mth.lerp(Delta.getGameTimeDeltaPartialTick(false), FlashOld, Flash);
        if (Glare > 0.004f) {
            Graphics.fill(0, 0, Graphics.guiWidth(), Graphics.guiHeight(), (int) (Math.min(0.9f, Glare) * 225.0f) << 24 | FlashColour);
        }
    }
}
