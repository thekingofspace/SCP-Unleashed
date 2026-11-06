package net.rai.scp_unleashed.scp217.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.client.ParticleStatus;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.scp217.Scp217Pulse;
import net.rai.scp_unleashed.scp217.Scp217Registry;
import net.rai.scp_unleashed.scp217.network.Scp217PulsePayload;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Scp217PulseClient {
    public static final int Colour = 0xE8B84A;
    public static final float RingSpacing = 1.4f;
    public static final int MinPoints = 12;
    public static final int MaxPoints = 220;
    public static final int RingLife = 4;
    public static final int GlintEvery = 5;
    public static final float RingLift = 0.9f;
    public static final int RatchetEvery = 3;
    public static final float RatchetVolume = 0.4f;
    public static final float RatchetLow = 0.85f;
    public static final float RatchetRise = 0.35f;
    public static final DustParticleOptions RingDust = new DustParticleOptions(new Vector3f(0.91f, 0.72f, 0.29f), 1.3f);
    private static final Int2ObjectMap<Mark> Marks = new Int2ObjectOpenHashMap<>();
    private static final List<Wave> Waves = new ArrayList<>();
    private static long Clock;

    private Scp217PulseClient() {
    }

    private record Mark(long Start, long End) {
    }

    private record Wave(Vec3 Origin, float Radius, long Start) {
        private int span() {
            return Mth.ceil(Radius / Scp217Pulse.WaveSpeed);
        }
    }

    public static void receive(Scp217PulsePayload Payload) {
        ClientLevel World = Minecraft.getInstance().level;
        if (World == null) {
            return;
        }
        Vec3 Origin = Payload.origin();
        Waves.add(new Wave(Origin, Payload.Radius(), Clock));
        for (int Id : Payload.Targets()) {
            Entity Found = World.getEntity(Id);
            double Distance = Found == null ? Payload.Radius() : Found.position().distanceTo(Origin);
            long Start = Clock + Mth.ceil(Distance / Scp217Pulse.WaveSpeed);
            Marks.put(Id, new Mark(Start, Start + Scp217Pulse.GlowTicks));
        }
    }

    public static boolean reveals(Entity Target) {
        if (Marks.isEmpty() || !Target.level().isClientSide || !Target.isAlive()) {
            return false;
        }
        Mark Found = Marks.get(Target.getId());
        return Found != null && Clock >= Found.Start() && Clock < Found.End();
    }

    public static void tick() {
        Clock++;
        Minecraft Game = Minecraft.getInstance();
        ClientLevel World = Game.level;
        if (World == null) {
            reset();
            return;
        }
        if (!Marks.isEmpty()) {
            Marks.int2ObjectEntrySet().removeIf(Entry -> Entry.getValue().End() <= Clock);
        }
        Iterator<Wave> Each = Waves.iterator();
        while (Each.hasNext()) {
            Wave Current = Each.next();
            long Age = Clock - Current.Start();
            if (Age > Current.span()) {
                Each.remove();
                continue;
            }
            ring(Game, Current, Age);
            ratchet(Game, World, Current, Age);
        }
    }

    private static void ring(Minecraft Game, Wave Current, long Age) {
        ParticleStatus Status = Game.options.particles().get();
        if (Status == ParticleStatus.MINIMAL) {
            return;
        }
        float Radius = Math.min(Current.Radius(), Scp217Pulse.WaveSpeed * Age + 0.5f);
        int Points = Mth.clamp(Mth.ceil(Mth.TWO_PI * Radius / RingSpacing), MinPoints, MaxPoints);
        if (Status == ParticleStatus.DECREASED) {
            Points = Math.max(MinPoints, Points / 2);
        }
        float Offset = Age * 0.37f;
        Vec3 Origin = Current.Origin();
        for (int Index = 0; Index < Points; Index++) {
            float Angle = Offset + Index * Mth.TWO_PI / Points;
            ParticleOptions Look = Index % GlintEvery == 0 ? ParticleTypes.WAX_ON : RingDust;
            Particle Made = Game.particleEngine.createParticle(Look, Origin.x + Mth.cos(Angle) * Radius, Origin.y + RingLift, Origin.z + Mth.sin(Angle) * Radius, 0.0, 0.0, 0.0);
            if (Made != null) {
                Made.setLifetime(RingLife);
            }
        }
    }

    private static void ratchet(Minecraft Game, ClientLevel World, Wave Current, long Age) {
        LocalPlayer User = Game.player;
        if (User == null || Age % RatchetEvery != 0) {
            return;
        }
        float Pitch = RatchetLow + RatchetRise * Mth.clamp(Age / (float) Math.max(1, Current.span()), 0.0f, 1.0f);
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp217Registry.TickLayers) {
            World.playLocalSound(User.getX(), User.getEyeY(), User.getZ(), Layer.get(), SoundSource.PLAYERS, RatchetVolume, Pitch, false);
        }
    }

    public static void forget(int EntityId) {
        Marks.remove(EntityId);
    }

    public static void reset() {
        Marks.clear();
        Waves.clear();
    }
}
