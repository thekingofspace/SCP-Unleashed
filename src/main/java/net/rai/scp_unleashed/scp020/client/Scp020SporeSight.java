package net.rai.scp_unleashed.scp020.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.cameras.client.CameraClientHooks;
import net.rai.scp_unleashed.scp020.Scp020Infection;
import net.rai.scp_unleashed.scp020.Scp020SporeMemory;
import net.rai.scp_unleashed.scp020.Scp020Spores;
import net.rai.scp_unleashed.scp020.network.Scp020SporesPayload;
import net.rai.scp_unleashed.senses.Senses;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Scp020SporeSight {
    public static final int MotesPerTick = 8;
    public static final float MoteChance = 0.08f;
    public static final double MoteReach = 32.0;
    public static final double FilmReach = 24.0;
    public static final int MaxFilms = 256;
    public static final double ViewCone = 0.35;
    public static final double MoteLift = 0.04;
    public static final double FilmLift = 0.012;
    public static final double MoteEdgeMargin = 0.22;
    public static final double MoteDrift = 0.01;
    public static final double MoteDriftRange = 0.012;
    public static final double MoteJitter = 0.003;
    public static final double MoteRise = 0.002;
    public static final int RevealTicks = 12;
    private static final Scp020SporeMemory Memory = new Scp020SporeMemory();
    private static final Map<Scp020SporeMemory.Face, Scp020SporeParticles.Film> Films = new HashMap<>();
    private static final RandomSource Random = RandomSource.create();
    private static boolean Shown;
    private static boolean CameraMode;
    private static int Revealed;
    @Nullable
    private static ClientLevel Seen;
    @Nullable
    private static SpriteSet MoteSprites;
    @Nullable
    private static SpriteSet FilmSprites;

    private Scp020SporeSight() {
    }

    public static void moteSprites(SpriteSet Sprites) {
        MoteSprites = Sprites;
    }

    public static void filmSprites(SpriteSet Sprites) {
        FilmSprites = Sprites;
    }

    public static void apply(Scp020SporesPayload Payload) {
        follow(Minecraft.getInstance());
        Memory.apply(Payload);
        if (!refresh()) {
            Films.clear();
            Revealed = 0;
        }
    }

    public static boolean shown() {
        return Shown && CameraMode == CameraClientHooks.isViewing();
    }

    public static boolean present(Scp020SporeMemory.Face Patch) {
        return Memory.holds(Patch);
    }

    public static float reveal() {
        return Math.min(1.0f, Revealed / (float) RevealTicks);
    }

    public static boolean keeps(Scp020SporeMemory.Face Patch, Scp020SporeParticles.Film Made) {
        return shown() && Films.get(Patch) == Made;
    }

    public static Scp020SporeMemory memory() {
        return Memory;
    }

    private static boolean refresh() {
        Minecraft Game = Minecraft.getInstance();
        LocalPlayer User = Game.player;
        boolean OnCamera = CameraClientHooks.isViewing();
        Shown = User != null && Game.level != null && User.isAlive() && !Senses.isBlindfolded(User) && Memory.shows(OnCamera, Scp020Spores.goggled(User));
        CameraMode = OnCamera;
        return Shown;
    }

    public static void reset() {
        Memory.clear();
        Films.clear();
        Shown = false;
        CameraMode = false;
        Revealed = 0;
        Seen = null;
    }

    private static double share(ParticleStatus Setting) {
        return switch (Setting) {
            case ALL -> 1.0;
            case DECREASED -> 0.5;
            case MINIMAL -> 0.25;
        };
    }

    private static void follow(Minecraft Game) {
        if (Game.level != Seen) {
            Memory.clear();
            Films.clear();
            Revealed = 0;
            Seen = Game.level;
        }
    }

    public static void tick(Minecraft Game) {
        follow(Game);
        if (!refresh()) {
            Films.clear();
            Revealed = 0;
            return;
        }
        if (Game.isPaused() || Game.level == null) {
            return;
        }
        Revealed = Math.min(RevealTicks, Revealed + 1);
        Camera View = Game.gameRenderer.getMainCamera();
        Vec3 Eye = View.getPosition();
        Vector3f Forward = View.getLookVector();
        Vec3 Look = new Vec3(Forward.x(), Forward.y(), Forward.z());
        films(Game.level, Eye);
        motes(Game.level, Eye, Look, share(Game.options.particles().get()));
    }

    private static void films(ClientLevel World, Vec3 Eye) {
        Map<Scp020SporeMemory.Face, Integer> Wanted = new HashMap<>();
        double Reach = FilmReach * FilmReach;
        for (Scp020SporeMemory.Face Patch : Memory.faces()) {
            if (Wanted.size() >= MaxFilms) {
                break;
            }
            if (Patch.centre(0.0).distanceToSqr(Eye) <= Reach && Scp020Infection.isOpen(World, Patch.Host(), Patch.Side())) {
                Wanted.put(Patch, Scp020SporeMemory.variant(Memory.connections(Patch)));
            }
        }
        Films.entrySet().removeIf(Entry -> !Entry.getValue().isAlive() || !Integer.valueOf(Entry.getValue().variant()).equals(Wanted.get(Entry.getKey())));
        if (FilmSprites == null) {
            return;
        }
        Minecraft Game = Minecraft.getInstance();
        for (Map.Entry<Scp020SporeMemory.Face, Integer> Entry : Wanted.entrySet()) {
            if (!Films.containsKey(Entry.getKey())) {
                Scp020SporeParticles.Film Made = new Scp020SporeParticles.Film(World, Entry.getKey(), Entry.getValue(), FilmLift, FilmSprites);
                Films.put(Entry.getKey(), Made);
                Game.particleEngine.add(Made);
            }
        }
    }

    private static void motes(ClientLevel World, Vec3 Eye, Vec3 Look, double Share) {
        List<Scp020SporeMemory.Face> All = Memory.faces();
        if (MoteSprites == null || All.isEmpty()) {
            return;
        }
        Minecraft Game = Minecraft.getInstance();
        int Budget = Math.max(1, (int) Math.round(MotesPerTick * Share));
        double Reach = MoteReach * MoteReach;
        int Count = All.size();
        int Start = Random.nextInt(Count);
        for (int Step = 0; Step < Count && Budget > 0; Step++) {
            if (Random.nextFloat() >= MoteChance * Share) {
                continue;
            }
            Scp020SporeMemory.Face Patch = All.get((Start + Step) % Count);
            Vec3 Centre = Patch.centre(MoteLift);
            Vec3 Toward = Centre.subtract(Eye);
            double Distance = Toward.lengthSqr();
            if (Distance > Reach || Distance < 1.0e-4 || Patch.normal().dot(Toward) >= 0.0 || Toward.dot(Look) / Math.sqrt(Distance) < ViewCone) {
                continue;
            }
            if (!Scp020Infection.isOpen(World, Patch.Host(), Patch.Side())) {
                continue;
            }
            Vec3 Spot = Centre.add(spread(Patch, Memory.connections(Patch)));
            Vec3 Push = Patch.normal().scale(MoteDrift + Random.nextDouble() * MoteDriftRange);
            Game.particleEngine.add(new Scp020SporeParticles.Mote(World, Spot.x, Spot.y, Spot.z, Push.x + jitter(), Push.y + jitter() + MoteRise, Push.z + jitter(), MoteSprites, Patch));
            Budget--;
        }
    }

    private static double jitter() {
        return (Random.nextDouble() - 0.5) * 2.0 * MoteJitter;
    }

    private static double across(boolean Low, boolean High) {
        double From = -0.5 + (Low ? 0.0 : MoteEdgeMargin);
        double To = 0.5 - (High ? 0.0 : MoteEdgeMargin);
        return From + Random.nextDouble() * (To - From);
    }

    private static Vec3 spread(Scp020SporeMemory.Face Patch, int Mask) {
        Direction Rightward = Scp020SporeMemory.right(Patch.Side());
        Direction Upward = Scp020SporeMemory.up(Patch.Side());
        double Along = across((Mask & Scp020SporeMemory.Left) != 0, (Mask & Scp020SporeMemory.Right) != 0);
        double Rise = across((Mask & Scp020SporeMemory.Bottom) != 0, (Mask & Scp020SporeMemory.Top) != 0);
        return new Vec3(Rightward.getStepX() * Along + Upward.getStepX() * Rise, Rightward.getStepY() * Along + Upward.getStepY() * Rise,
                Rightward.getStepZ() * Along + Upward.getStepZ() * Rise);
    }
}
