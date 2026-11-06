package net.rai.scp_unleashed.scp163.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rai.scp_unleashed.scp163.Scp163CraftEntity;
import net.rai.scp_unleashed.scp163.Scp163Registry;

public final class Scp163ClientEffects {
    public static final double RumbleReach = 140.0;
    public static final int FlashColour = 0xE8F6FF;

    private static float Flash;
    private static float FlashOld;
    private static float Shake;
    private static float Rumble;
    private static long Ticks;

    private Scp163ClientEffects() {
    }

    public static void reset() {
        Flash = 0.0f;
        FlashOld = 0.0f;
        Shake = 0.0f;
        Rumble = 0.0f;
    }

    public static void craftTick(Scp163CraftEntity Craft) {
        Minecraft Client = Minecraft.getInstance();
        if (Craft.getPhase() != Scp163CraftEntity.PhaseFlight || !Craft.isLaunched()) {
            return;
        }
        int Left = Craft.ticksToImpact();
        SoundManager Sounds = Client.getSoundManager();
        if (Left > 3 && Craft.claimSounds()) {
            Sounds.play(new Scp163CraftSound(Craft, Scp163Registry.CraftRush.get(), 1.0f, 0.65f, 1.25f));
            Sounds.play(new Scp163CraftSound(Craft, Scp163Registry.CraftWhine.get(), 0.6f, 0.8f, 1.5f));
            Sounds.play(new Scp163CraftSound(Craft, Scp163Registry.CraftBurn.get(), 0.5f, 0.85f, 1.2f));
        }
        LocalPlayer Viewer = Client.player;
        if (Viewer != null && Left < 36) {
            double Distance = Math.sqrt(Viewer.distanceToSqr(Craft.position()));
            float Near = (float) Mth.clamp(1.0 - Distance / RumbleReach, 0.0, 1.0);
            Rumble = Math.max(Rumble, (1.0f - Left / 36.0f) * Near * 0.5f);
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
        Flash = Math.max(Flash, (float) Mth.clamp(1.1 - Distance / 210.0, 0.1, 0.9) * Sky);
        Shake = Math.max(Shake, (float) Mth.clamp(1.4 - Distance / 80.0, 0.0, 1.2));
        World.setSkyFlashTime(3);
        if (Distance < 220.0) {
            burst(World, At, Radius);
        }
    }

    private static void burst(ClientLevel World, Vec3 At, float Radius) {
        RandomSource Random = World.random;
        World.addParticle(ParticleTypes.EXPLOSION_EMITTER, true, At.x, At.y + 1.0, At.z, 0.0, 0.0, 0.0);
        for (int Index = 0; Index < 40; Index++) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Speed = 0.2 + Random.nextDouble() * 0.3;
            World.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, At.x + Math.cos(Angle) * 1.5, At.y + 0.5 + Random.nextDouble(), At.z + Math.sin(Angle) * 1.5,
                    Math.cos(Angle) * Speed, 0.02 + Random.nextDouble() * 0.05, Math.sin(Angle) * Speed);
        }
        BlockState[] Chunks = {Blocks.COARSE_DIRT.defaultBlockState(), Blocks.GRAVEL.defaultBlockState(), Blocks.DIRT.defaultBlockState(),
                Scp163Registry.Hull.get().defaultBlockState()};
        for (int Index = 0; Index < 120; Index++) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Speed = 0.2 + Random.nextDouble() * 0.6;
            World.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Chunks[Random.nextInt(Chunks.length)]), true, At.x, At.y + 1.0, At.z,
                    Math.cos(Angle) * Speed, 0.5 + Random.nextDouble() * 0.8, Math.sin(Angle) * Speed);
        }
        for (int Index = 0; Index < 50; Index++) {
            World.addParticle(ParticleTypes.END_ROD, true, At.x + Random.nextGaussian(), At.y + 1.0, At.z + Random.nextGaussian(),
                    Random.nextGaussian() * 0.18, 0.2 + Random.nextDouble() * 0.4, Random.nextGaussian() * 0.18);
        }
        for (int Index = 0; Index < 14; Index++) {
            World.addParticle(ParticleTypes.FLAME, true, At.x + Random.nextGaussian() * Radius * 0.2, At.y + 0.5, At.z + Random.nextGaussian() * Radius * 0.2, 0.0, 0.05, 0.0);
        }
    }

    public static void tick(Minecraft Client) {
        Ticks++;
        FlashOld = Flash;
        Flash *= 0.84f;
        Shake *= 0.9f;
        Rumble *= 0.82f;
    }

    public static void shake(ViewportEvent.ComputeCameraAngles Event) {
        float Amount = Shake + Rumble;
        if (Amount < 0.01f) {
            return;
        }
        double Time = Ticks + Event.getPartialTick();
        float Yaw = (float) (Math.sin(Time * 1.37) * Math.cos(Time * 0.61) + Math.sin(Time * 2.9) * 0.4) * Amount * 1.6f;
        float Pitch = (float) (Math.sin(Time * 1.71 + 1.3) * Math.cos(Time * 0.47) + Math.sin(Time * 3.3 + 0.2) * 0.4) * Amount * 1.3f;
        float Roll = (float) (Math.sin(Time * 1.13 + 0.4) + Math.sin(Time * 2.3 + 2.1) * 0.5) * Amount * 2.0f;
        Event.setYaw(Event.getYaw() + Yaw);
        Event.setPitch(Event.getPitch() + Pitch);
        Event.setRoll(Event.getRoll() + Roll);
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        float PartialTick = Delta.getGameTimeDeltaPartialTick(false);
        float Glare = Mth.lerp(PartialTick, FlashOld, Flash);
        if (Glare > 0.004f) {
            int Alpha = (int) (Math.min(0.9f, Glare) * 230.0f);
            Graphics.fill(0, 0, Graphics.guiWidth(), Graphics.guiHeight(), Alpha << 24 | FlashColour);
        }
    }
}
