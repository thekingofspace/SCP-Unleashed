package net.rai.scp_unleashed.scp002.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp002.MeteorEntity;
import net.rai.scp_unleashed.scp002.Scp002Registry;

public final class Scp002ClientEffects {
    public static final ResourceLocation Vignette = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/scp002/consume_vignette.png");
    public static final int VignetteSize = 256;
    public static final int SwoopLead = 100;
    public static final double RumbleReach = 140.0;

    private static float Flash;
    private static float FlashOld;
    private static float Shake;
    private static float Rumble;
    private static float Consume;
    private static float ConsumeTarget;
    private static int ConsumeHold;
    private static long Ticks;

    private Scp002ClientEffects() {
    }

    public static void reset() {
        Flash = 0.0f;
        FlashOld = 0.0f;
        Shake = 0.0f;
        Rumble = 0.0f;
        Consume = 0.0f;
        ConsumeTarget = 0.0f;
        ConsumeHold = 0;
    }

    public static void meteorTick(MeteorEntity Meteor) {
        Minecraft Client = Minecraft.getInstance();
        if (Meteor.getPhase() != MeteorEntity.PhaseFlight || !Meteor.isLaunched()) {
            return;
        }
        int Left = Meteor.ticksToImpact();
        SoundManager Sounds = Client.getSoundManager();
        if (Left > 3 && Meteor.claimSounds()) {
            Sounds.play(new MeteorSoundLayer(Meteor, Scp002Registry.MeteorRush.get(), 1.0f, 0.6f, 1.3f, true));
            Sounds.play(new MeteorSoundLayer(Meteor, Scp002Registry.MeteorRoar.get(), 0.9f, 1.0f, 1.4f, true));
            Sounds.play(new MeteorSoundLayer(Meteor, Scp002Registry.MeteorBurn.get(), 0.5f, 0.85f, 1.25f, true));
        }
        if (Left <= SwoopLead && Left > SwoopLead - 12 && Meteor.claimSwoop()) {
            Sounds.play(new MeteorSoundLayer(Meteor, Scp002Registry.MeteorSwoop.get(), 0.85f, 1.0f, 1.0f, false));
        }
        LocalPlayer Viewer = Client.player;
        if (Viewer != null && Left < 36) {
            double Distance = Math.sqrt(Viewer.distanceToSqr(Meteor.position()));
            float Near = (float) Mth.clamp(1.0 - Distance / RumbleReach, 0.0, 1.0);
            Rumble = Math.max(Rumble, (1.0f - Left / 36.0f) * Near * 0.55f);
        }
    }

    public static void impact(BlockPos Centre, float Radius) {
        Minecraft Client = Minecraft.getInstance();
        LocalPlayer Viewer = Client.player;
        ClientLevel World = Client.level;
        if (Viewer == null || World == null) {
            return;
        }
        Vec3 At = Vec3.atCenterOf(Centre);
        double Distance = Math.sqrt(Viewer.distanceToSqr(At));
        float Sky = World.canSeeSky(Viewer.blockPosition()) ? 1.0f : 0.35f;
        Flash = Math.max(Flash, (float) Mth.clamp(1.15 - Distance / 210.0, 0.12, 1.0) * Sky);
        Shake = Math.max(Shake, (float) Mth.clamp(1.5 - Distance / 80.0, 0.0, 1.3));
        World.setSkyFlashTime(4);
        if (Distance < 220.0) {
            burst(World, At, Radius);
        }
    }

    private static void burst(ClientLevel World, Vec3 At, float Radius) {
        RandomSource Random = World.random;
        World.addParticle(ParticleTypes.EXPLOSION_EMITTER, true, At.x, At.y + 1.0, At.z, 0.0, 0.0, 0.0);
        World.addParticle(ParticleTypes.EXPLOSION_EMITTER, true, At.x + Random.nextGaussian() * 2.0, At.y + 2.0, At.z + Random.nextGaussian() * 2.0, 0.0, 0.0, 0.0);
        for (int Index = 0; Index < 48; Index++) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Speed = 0.22 + Random.nextDouble() * 0.3;
            World.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, At.x + Math.cos(Angle) * 1.5, At.y + 0.5 + Random.nextDouble(), At.z + Math.sin(Angle) * 1.5,
                    Math.cos(Angle) * Speed, 0.02 + Random.nextDouble() * 0.05, Math.sin(Angle) * Speed);
        }
        BlockState[] Chunks = {Blocks.COARSE_DIRT.defaultBlockState(), Blocks.BLACKSTONE.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.COBBLED_DEEPSLATE.defaultBlockState()};
        for (int Index = 0; Index < 110; Index++) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Speed = 0.2 + Random.nextDouble() * 0.6;
            World.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Chunks[Random.nextInt(Chunks.length)]), true, At.x, At.y + 1.0, At.z,
                    Math.cos(Angle) * Speed, 0.5 + Random.nextDouble() * 0.8, Math.sin(Angle) * Speed);
        }
        for (int Index = 0; Index < 60; Index++) {
            World.addParticle(Scp002Registry.Ember.get(), true, At.x + Random.nextGaussian(), At.y + 1.0, At.z + Random.nextGaussian(),
                    Random.nextGaussian() * 0.15, 0.25 + Random.nextDouble() * 0.45, Random.nextGaussian() * 0.15);
        }
        for (int Index = 0; Index < 18; Index++) {
            World.addParticle(ParticleTypes.LAVA, true, At.x + Random.nextGaussian() * Radius * 0.3, At.y + 0.5, At.z + Random.nextGaussian() * Radius * 0.3, 0.0, 0.0, 0.0);
        }
    }

    public static void consume(float Intensity) {
        ConsumeTarget = Mth.clamp(Intensity, 0.0f, 1.0f);
        ConsumeHold = 40;
    }

    public static void tick(Minecraft Client) {
        Ticks++;
        FlashOld = Flash;
        Flash *= 0.84f;
        Shake *= 0.9f;
        Rumble *= 0.82f;
        if (ConsumeHold > 0) {
            ConsumeHold--;
        } else {
            ConsumeTarget = 0.0f;
        }
        if (Client.player == null || !Client.player.isAlive()) {
            ConsumeTarget = 0.0f;
        }
        Consume += (ConsumeTarget - Consume) * (ConsumeTarget > Consume ? 0.12f : 0.05f);
        if (Consume < 0.002f && ConsumeTarget <= 0.0f) {
            Consume = 0.0f;
        }
    }

    private static float heartPulse(float PartialTick) {
        float Period = Mth.lerp(Consume, 24.0f, 8.0f);
        float Phase = ((Ticks + PartialTick) % Period) / Period;
        float First = (float) Math.exp(-Phase * 9.0);
        float Second = (float) Math.exp(-Math.pow((Phase - 0.2f) * 12.0f, 2.0)) * 0.65f;
        return Mth.clamp(First + Second, 0.0f, 1.0f);
    }

    public static void shake(ViewportEvent.ComputeCameraAngles Event) {
        float Amount = Shake + Rumble;
        if (Amount < 0.01f) {
            return;
        }
        double Time = Ticks + Event.getPartialTick();
        float Yaw = (float) (Math.sin(Time * 1.37) * Math.cos(Time * 0.61) + Math.sin(Time * 2.9) * 0.4) * Amount * 1.8f;
        float Pitch = (float) (Math.sin(Time * 1.71 + 1.3) * Math.cos(Time * 0.47) + Math.sin(Time * 3.3 + 0.2) * 0.4) * Amount * 1.5f;
        float Roll = (float) (Math.sin(Time * 1.13 + 0.4) + Math.sin(Time * 2.3 + 2.1) * 0.5) * Amount * 2.2f;
        Event.setYaw(Event.getYaw() + Yaw);
        Event.setPitch(Event.getPitch() + Pitch);
        Event.setRoll(Event.getRoll() + Roll);
    }

    public static void fov(ViewportEvent.ComputeFov Event) {
        if (Consume < 0.01f) {
            return;
        }
        float Pulse = heartPulse((float) Event.getPartialTick());
        Event.setFOV(Event.getFOV() * (1.0 - 0.04 * Consume * Pulse));
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        float PartialTick = Delta.getGameTimeDeltaPartialTick(false);
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        float Glare = Mth.lerp(PartialTick, FlashOld, Flash);
        if (Glare > 0.004f) {
            int Alpha = (int) (Math.min(0.92f, Glare) * 235.0f);
            Graphics.fill(0, 0, Width, Height, Alpha << 24 | 0xFFEBD2);
        }
        if (Consume > 0.01f) {
            float Pulse = heartPulse(PartialTick);
            float Alpha = Mth.clamp(Consume * (0.5f + 0.5f * Pulse), 0.0f, 1.0f);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            Graphics.setColor(1.0f, 1.0f, 1.0f, Alpha);
            Graphics.blit(Vignette, 0, 0, Width, Height, 0.0f, 0.0f, VignetteSize, VignetteSize, VignetteSize, VignetteSize);
            Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        }
    }
}
