package net.rai.scp_unleashed.warhead.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rai.scp_unleashed.config.ScpClientConfig;
import net.rai.scp_unleashed.warhead.WarheadRegistry;

import java.util.ArrayList;
import java.util.List;

public final class WarheadEffects {
    public static final double SpeedOfSound = 343.0;
    public static final float BlastScale = 0.5f;
    public static final int FlashHold = 5;
    public static final int Lifetime = 240;
    public static final double ParticleReach = 420.0;
    public static final double Size = 2.0;

    private static float Flash;
    private static float FlashOld;
    private static int Hold;
    private static float Shake;
    private static long Ticks;
    private static final List<Shock> Shocks = new ArrayList<>();
    private static final List<Cloud> Clouds = new ArrayList<>();

    private record Shock(long At, float Amount) {
    }

    private static final class Cloud {
        private final Vec3 Centre;
        private final double Distance;
        private int Age;

        private Cloud(Vec3 Centre, double Distance) {
            this.Centre = Centre;
            this.Distance = Distance;
        }
    }

    private WarheadEffects() {
    }

    public static void reset() {
        Flash = 0.0f;
        FlashOld = 0.0f;
        Hold = 0;
        Shake = 0.0f;
        Shocks.clear();
        Clouds.clear();
    }

    public static boolean enabled() {
        return ScpClientConfig.WarheadEffects.get();
    }

    public static float flash(float PartialTick) {
        return Mth.lerp(PartialTick, FlashOld, Flash);
    }

    public static void blast(Vec3 Centre) {
        Minecraft Client = Minecraft.getInstance();
        LocalPlayer Viewer = Client.player;
        ClientLevel World = Client.level;
        if (Viewer == null || World == null || !enabled()) {
            return;
        }
        double Distance = Math.sqrt(Viewer.distanceToSqr(Centre));
        boolean Open = World.canSeeSky(Viewer.blockPosition());
        float Glare = (float) (Open ? Mth.clamp(1.25 - Distance / 700.0, 0.2, 1.0) : Mth.clamp(0.9 - Distance / 500.0, 0.12, 0.6));
        Flash = Math.max(Flash, Glare);
        Hold = FlashHold;
        World.setSkyFlashTime(8);
        int Arrive = (int) Math.round(Distance / SpeedOfSound * 20.0);
        Shocks.add(new Shock(Ticks + Arrive, (float) Mth.clamp(1.7 - Distance / 240.0, 0.0, 1.6)));
        sounds(Client, Centre, Distance, Arrive);
        Clouds.add(new Cloud(Centre, Distance));
    }

    private static void play(Minecraft Client, SoundEvent Event, Vec3 At, float Volume, int Delay) {
        if (Volume <= 0.0f) {
            return;
        }
        SimpleSoundInstance Sound = new SimpleSoundInstance(Event.getLocation(), SoundSource.BLOCKS, Math.min(1.0f, Volume), 1.0f, SoundInstance.createUnseededRandom(),
                false, 0, SoundInstance.Attenuation.NONE, At.x, At.y, At.z, false);
        if (Delay > 0) {
            Client.getSoundManager().playDelayed(Sound, Delay);
        } else {
            Client.getSoundManager().play(Sound);
        }
    }

    private static void sounds(Minecraft Client, Vec3 Centre, double Distance, int Arrive) {
        float Blast = BlastScale * (float) Mth.clamp(1.2 - Distance / 300.0, 0.15, 1.0);
        float Crack = BlastScale * (float) Mth.clamp(1.0 - Distance / 220.0, 0.0, 0.9);
        float Roar = BlastScale * (float) Mth.clamp(1.1 - Distance / 500.0, 0.3, 1.0);
        float Sub = BlastScale * (float) Mth.clamp(1.2 - Distance / 400.0, 0.25, 1.0);
        float Far = BlastScale * (float) Mth.clamp((Distance - 100.0) / 300.0, 0.0, 0.8);
        float Tail = BlastScale * 0.6f * (float) Mth.clamp(1.2 - Distance / 700.0, 0.2, 1.0);
        float Debris = BlastScale * (float) Mth.clamp(1.0 - Distance / 120.0, 0.0, 0.8);
        play(Client, WarheadRegistry.Blast.get(), Centre, Blast, Arrive);
        play(Client, WarheadRegistry.Blast.get(), Centre, Blast * 0.7f, Arrive + 1);
        play(Client, WarheadRegistry.BlastCrack.get(), Centre, Crack, Arrive);
        play(Client, WarheadRegistry.BlastRoar.get(), Centre, Roar, Arrive + 3);
        play(Client, WarheadRegistry.BlastSub.get(), Centre, Sub, Arrive);
        play(Client, WarheadRegistry.BlastFar.get(), Centre, Far, Arrive);
        play(Client, WarheadRegistry.BlastRoar.get(), Centre, Tail, Arrive + 36);
        int[] Falls = {44, 51, 63, 70, 86};
        for (int Index = 0; Index < Falls.length; Index++) {
            play(Client, WarheadRegistry.Debris.get(), Centre, Debris * (0.8f - 0.1f * Index), Arrive + Falls[Index]);
        }
    }

    public static void tick(Minecraft Client) {
        if (Client.isPaused()) {
            return;
        }
        Ticks++;
        if (!enabled() && (Flash > 0.0f || Shake > 0.0f || !Shocks.isEmpty() || !Clouds.isEmpty())) {
            reset();
        }
        FlashOld = Flash;
        if (Hold > 0) {
            Hold--;
        } else {
            Flash *= 0.9f;
            if (Flash < 0.003f) {
                Flash = 0.0f;
            }
        }
        Shake *= 0.93f;
        for (Shock Item : List.copyOf(Shocks)) {
            if (Item.At() <= Ticks) {
                Shocks.remove(Item);
                Shake = Math.max(Shake, Item.Amount());
            }
        }
        ClientLevel World = Client.level;
        if (World == null) {
            return;
        }
        for (Cloud Item : List.copyOf(Clouds)) {
            if (Item.Age++ > Lifetime) {
                Clouds.remove(Item);
                continue;
            }
            if (Item.Distance < ParticleReach) {
                emit(World, Item);
            }
        }
    }

    private static void emit(ClientLevel World, Cloud Item) {
        RandomSource Random = World.random;
        Vec3 At = Item.Centre;
        int Age = Item.Age;
        float Density = Item.Distance > 220.0 ? 0.5f : 1.0f;
        if (Age == 1) {
            for (int Index = 0; Index < 8; Index++) {
                World.addParticle(ParticleTypes.EXPLOSION_EMITTER, true, At.x + Random.nextGaussian() * 3.0 * Size, At.y + Random.nextDouble() * 4.0 * Size, At.z + Random.nextGaussian() * 3.0 * Size, 0.0, 0.0, 0.0);
            }
            BlockState[] Chunks = {Blocks.COARSE_DIRT.defaultBlockState(), Blocks.BLACKSTONE.defaultBlockState(), Blocks.GRAVEL.defaultBlockState(), Blocks.COBBLED_DEEPSLATE.defaultBlockState()};
            for (int Index = 0; Index < 200; Index++) {
                double Angle = Random.nextDouble() * Math.PI * 2.0;
                double Speed = (0.4 + Random.nextDouble() * 1.1) * Size * 0.75;
                World.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Chunks[Random.nextInt(Chunks.length)]), true, At.x, At.y + 1.0, At.z,
                        Math.cos(Angle) * Speed, 0.6 + Random.nextDouble() * 1.2, Math.sin(Angle) * Speed);
            }
        }
        if (Age < 30) {
            double Radius = (2.0 + Age * 0.45) * Size;
            for (int Index = 0; Index < 14 * Density; Index++) {
                Vec3 Spot = sphere(Random).scale(Radius);
                World.addParticle(ParticleTypes.EXPLOSION, true, At.x + Spot.x, At.y + Math.abs(Spot.y) + 1.0, At.z + Spot.z, 0.0, 0.0, 0.0);
                World.addParticle(ParticleTypes.FLAME, true, At.x + Spot.x * 0.8, At.y + Math.abs(Spot.y) * 0.8 + 1.0, At.z + Spot.z * 0.8,
                        Spot.x * 0.02, 0.08 + Random.nextDouble() * 0.1, Spot.z * 0.02);
            }
        }
        if (Age < 22) {
            double Ring = (3.0 + Age * 3.2) * Size * 0.7;
            for (int Index = 0; Index < 20 * Density; Index++) {
                double Angle = Random.nextDouble() * Math.PI * 2.0;
                World.addParticle(ParticleTypes.CLOUD, true, At.x + Math.cos(Angle) * Ring, At.y - 0.5, At.z + Math.sin(Angle) * Ring,
                        Math.cos(Angle) * 0.35, 0.02, Math.sin(Angle) * 0.35);
            }
        }
        if (Age > 3 && Age < 170) {
            double Height = Math.min(30.0, Age * 0.55) * Size;
            for (int Index = 0; Index < 9 * Density; Index++) {
                double Angle = Random.nextDouble() * Math.PI * 2.0;
                double Spread = Random.nextDouble() * 2.6 * Size;
                World.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, At.x + Math.cos(Angle) * Spread, At.y + Random.nextDouble() * Height * 0.85, At.z + Math.sin(Angle) * Spread,
                        Math.cos(Angle) * 0.01, 0.10 + Random.nextDouble() * 0.06, Math.sin(Angle) * 0.01);
            }
            if (Age < 90) {
                World.addParticle(ParticleTypes.FLAME, true, At.x + Random.nextGaussian(), At.y + Random.nextDouble() * Height * 0.5, At.z + Random.nextGaussian(),
                        0.0, 0.2 + Random.nextDouble() * 0.2, 0.0);
                World.addParticle(ParticleTypes.LAVA, true, At.x + Random.nextGaussian() * 2.0, At.y + 1.0, At.z + Random.nextGaussian() * 2.0, 0.0, 0.0, 0.0);
            }
        }
        if (Age > 24 && Age < 220) {
            double Top = Math.min(40.0, 14.0 + Age * 0.24) * Size;
            for (int Index = 0; Index < 10 * Density; Index++) {
                double Angle = Random.nextDouble() * Math.PI * 2.0;
                double Spread = (3.0 + Random.nextDouble() * 9.0) * Size;
                World.addParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, At.x + Math.cos(Angle) * Spread, At.y + Top + Random.nextGaussian() * 1.6, At.z + Math.sin(Angle) * Spread,
                        Math.cos(Angle) * 0.06, 0.03 + Random.nextDouble() * 0.03, Math.sin(Angle) * 0.06);
            }
            if (Age < 120) {
                World.addParticle(ParticleTypes.LARGE_SMOKE, true, At.x + Random.nextGaussian() * 4.0 * Size, At.y + Top - 2.0 + Random.nextGaussian() * Size, At.z + Random.nextGaussian() * 4.0 * Size,
                        0.0, 0.05, 0.0);
            }
        }
    }

    private static Vec3 sphere(RandomSource Random) {
        double Theta = Random.nextDouble() * Math.PI * 2.0;
        double Height = Random.nextDouble() * 2.0 - 1.0;
        double Ring = Math.sqrt(1.0 - Height * Height);
        return new Vec3(Math.cos(Theta) * Ring, Height, Math.sin(Theta) * Ring);
    }

    public static void shake(ViewportEvent.ComputeCameraAngles Event) {
        if (Shake < 0.01f) {
            return;
        }
        double Time = Ticks + Event.getPartialTick();
        float Yaw = (float) (Math.sin(Time * 1.37) * Math.cos(Time * 0.61) + Math.sin(Time * 2.9) * 0.4) * Shake * 1.8f;
        float Pitch = (float) (Math.sin(Time * 1.71 + 1.3) * Math.cos(Time * 0.47) + Math.sin(Time * 3.3 + 0.2) * 0.4) * Shake * 1.5f;
        float Roll = (float) (Math.sin(Time * 1.13 + 0.4) + Math.sin(Time * 2.3 + 2.1) * 0.5) * Shake * 2.2f;
        Event.setYaw(Event.getYaw() + Yaw);
        Event.setPitch(Event.getPitch() + Pitch);
        Event.setRoll(Event.getRoll() + Roll);
    }
}
