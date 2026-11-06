package net.rai.scp_unleashed.scp372.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp372.Scp372Glimpse;
import net.rai.scp_unleashed.scp372.Scp372Registry;
import net.rai.scp_unleashed.scp372.network.Scp372GlimpsePayload;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class Scp372ClientGlimpses {
    public static final int Attempts = 40;
    public static final int TryEvery = 2;
    public static final int FirstId = -3_720_000;
    public static final int LastId = -2_000_000_000;
    private static final RandomSource Random = RandomSource.create();
    @Nullable
    private static Scp372Glimpse Active;
    @Nullable
    private static ClientLevel Current;
    private static int Pending;
    private static boolean Dim;
    private static int NextId = FirstId;

    private Scp372ClientGlimpses() {
    }

    public static void receive(Scp372GlimpsePayload Payload) {
        if (Active != null && !Active.isRemoved()) {
            return;
        }
        Random.setSeed(Payload.Seed());
        Dim = Payload.Dim();
        Pending = Attempts;
    }

    public static void hush() {
        Pending = 0;
        if (Active != null && !Active.isRemoved()) {
            Active.discard();
        }
        Active = null;
    }

    public static void reset() {
        hush();
        Current = null;
    }

    public static void tick(Minecraft Game) {
        ClientLevel World = Game.level;
        LocalPlayer User = Game.player;
        if (World == null || User == null) {
            reset();
            return;
        }
        if (World != Current) {
            Active = null;
            Pending = 0;
            Current = World;
        }
        if (Game.isPaused()) {
            return;
        }
        if (Active != null && Active.isRemoved()) {
            Active = null;
        }
        if (Pending <= 0 || Active != null) {
            return;
        }
        Pending--;
        if (Pending % TryEvery == 0 && place(Game, World, User)) {
            Pending = 0;
        }
    }

    private static boolean place(Minecraft Game, ClientLevel World, LocalPlayer User) {
        if (!User.isAlive() || User.isSpectator()) {
            return false;
        }
        double Fov = Game.options.fov().get() * User.getFieldOfViewModifier();
        double Aspect = Game.getWindow().getWidth() / (double) Math.max(1, Game.getWindow().getHeight());
        Optional<Scp372Spots.Spot> Found = Scp372Spots.find(World, User, Random, Fov, Aspect);
        if (Found.isEmpty()) {
            return false;
        }
        Scp372Spots.Spot Spot = Found.get();
        Scp372Glimpse Glimpse = new Scp372Glimpse(Scp372Registry.Glimpse.get(), World);
        Glimpse.setId(nextId());
        Vec3 Point = Spot.Point();
        Glimpse.moveTo(Point.x, Point.y, Point.z, 0.0f, 0.0f);
        int Life = Scp372Glimpse.LifeMin + Random.nextInt(Scp372Glimpse.LifeMax - Scp372Glimpse.LifeMin + 1);
        Glimpse.setup(Spot.Normal(), Spot.Heading(), Dim ? Life + 1 : Life);
        World.addEntity(Glimpse);
        if (Glimpse.isRemoved() || World.getEntity(Glimpse.getId()) != Glimpse) {
            return false;
        }
        Active = Glimpse;
        return true;
    }

    private static int nextId() {
        int Id = NextId--;
        if (NextId < LastId) {
            NextId = FirstId;
        }
        return Id;
    }
}
