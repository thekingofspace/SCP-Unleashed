package net.rai.scp_unleashed.scp4666.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp4666.Scp4666ClockModel;
import net.rai.scp_unleashed.scp4666.Scp4666World;
import net.rai.scp_unleashed.scp4666.network.Scp4666FxPayload;
import net.rai.scp_unleashed.scp4666.network.Scp4666SyncPayload;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public final class Scp4666ClientState {
    public static final int FlashTicks = 8;
    public static final int ShakeTicks = 10;
    public static final int WakeTicks = 60;
    public static final int SnowRate = 22;
    public static final double SnowRadius = 16.0;
    private static int Night = 1;
    private static final Scp4666ClockModel Model = new Scp4666ClockModel();
    private static long Written = -1L;
    private static int Overrides;
    private static long Drift;
    private static int Phase;
    private static int Tree = 4;
    private static boolean LightsDead;
    private static boolean Started;
    private static List<Integer> Tasks = List.of();
    private static int CentreX;
    private static int CentreZ;
    private static int[] Cabin = new int[6];
    private static boolean Mapped;
    private static int Ticks;
    private static int Flash;
    private static int Shake;
    private static int Wake;
    private static int Sack;
    private static int SackLength = 1;
    private static int Bury;
    private static int BuryLength = 1;
    private static boolean Held;
    private static double WindAngle = 0.4;
    private static final List<Emitter> Emitters = new ArrayList<>();

    private record Emitter(Vec3 Pos, int Kind, int Value, int[] Left) {
    }

    private Scp4666ClientState() {
    }

    public static boolean inside(Minecraft Client) {
        return Client.level != null && Client.level.dimension() == Scp4666World.key();
    }

    public static void sync(Scp4666SyncPayload Payload) {
        Night = Payload.Night();
        Phase = Payload.phase();
        Model.accept(Payload.Clock(), Payload.running(), Phase, Payload.Rate());
        Tree = Payload.tree();
        LightsDead = Payload.lightsDead();
        Started = Payload.started();
        Tasks = List.copyOf(Payload.Tasks());
        CentreX = Payload.CentreX();
        CentreZ = Payload.CentreZ();
        Mapped = Payload.Bounds().size() == 6;
        for (int Index = 0; Mapped && Index < 6; Index++) {
            Cabin[Index] = Payload.Bounds().get(Index);
        }
    }

    public static void effect(Scp4666FxPayload Payload) {
        switch (Payload.Kind()) {
            case Scp4666FxPayload.Thud -> {
                Flash = FlashTicks;
                Shake = ShakeTicks;
            }
            case Scp4666FxPayload.Sack -> {
                Sack = 1;
                SackLength = Math.max(1, Payload.Value());
            }
            case Scp4666FxPayload.Wake -> Wake = WakeTicks;
            case Scp4666FxPayload.Hold -> {
                Held = Payload.Value() > 0;
                if (!Held) {
                    Sack = 0;
                    Bury = 0;
                }
            }
            case Scp4666FxPayload.Probe -> probe();
            case Scp4666FxPayload.Bury -> {
                Bury = 1;
                BuryLength = Math.max(1, Payload.Value());
            }
            case Scp4666FxPayload.Breath -> Emitters.add(new Emitter(new Vec3(Payload.X(), Payload.Y(), Payload.Z()), Scp4666FxPayload.Breath, Payload.Value(), new int[]{40}));
            case Scp4666FxPayload.Snow -> Emitters.add(new Emitter(new Vec3(Payload.X(), Payload.Y(), Payload.Z()), Scp4666FxPayload.Snow, Payload.Value(), new int[]{Payload.Value()}));
            default -> {
            }
        }
    }

    private static void probe() {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || Client.level == null) {
            return;
        }
        Client.player.displayClientMessage(Component.translatable("commands.scp_unleashed.scp4666.clockcheck_client", Client.level.getDayTime(), Math.round(Model.target()),
                String.format(Locale.ROOT, "%.2f", Model.rate()), Model.running() ? "yes" : "no", Math.round(Model.worstError()), Model.snaps(), Overrides, Drift), false);
    }

    public static void reset() {
        Held = false;
        Sack = 0;
        Bury = 0;
        Flash = 0;
        Shake = 0;
        Wake = 0;
        Emitters.clear();
    }

    public static long clock() {
        return Model.time(0.0f);
    }

    public static Scp4666ClockModel model() {
        return Model;
    }

    public static int overrides() {
        return Overrides;
    }

    public static long drift() {
        return Drift;
    }

    public static void hold(ClientLevel World, float Partial) {
        long Found = World.getDayTime();
        if (Written >= 0L && Found != Written) {
            Overrides++;
            Drift = Math.max(Drift, Math.abs(Found - Written));
        }
        Written = Model.time(Partial);
        World.setDayTime(Written);
        World.setRainLevel(1.0f);
        World.setThunderLevel(0.0f);
    }

    public static void forget() {
        Model.reset();
        Written = -1L;
        Overrides = 0;
        Drift = 0L;
    }

    public static void tick(Minecraft Client) {
        if (!inside(Client)) {
            if (Held || Sack > 0) {
                reset();
            }
            if (Written >= 0L) {
                forget();
            }
            return;
        }
        Ticks++;
        Model.tick();
        if (Flash > 0) {
            Flash--;
        }
        if (Shake > 0) {
            Shake--;
        }
        if (Wake > 0) {
            Wake--;
        }
        if (Sack > 0) {
            Sack++;
        }
        if (Bury > 0 && Bury < BuryLength + 40) {
            Bury++;
        }
        ClientLevel World = Client.level;
        hold(World, 0.0f);
        WindAngle += Mth.sin(Ticks * 0.004f) * 0.003;
        if (!Client.isPaused()) {
            weather(Client, World);
            emitters(World);
        }
    }

    private static void weather(Minecraft Client, ClientLevel World) {
        Player User = Client.player;
        if (User == null) {
            return;
        }
        RandomSource Random = World.getRandom();
        double Gust = 0.18 + 0.12 * Math.max(0.0, Mth.sin(Ticks * 0.021f));
        double Wx = Math.cos(WindAngle) * Gust;
        double Wz = Math.sin(WindAngle) * Gust;
        int Count = switch (Client.options.particles().get()) {
            case MINIMAL -> SnowRate / 4;
            case DECREASED -> SnowRate / 2;
            default -> SnowRate;
        };
        for (int Index = 0; Index < Count; Index++) {
            double X = User.getX() + (Random.nextDouble() - 0.5) * SnowRadius * 2.0 - Wx * 20.0;
            double Y = User.getY() + Random.nextDouble() * 7.0 - 1.5;
            double Z = User.getZ() + (Random.nextDouble() - 0.5) * SnowRadius * 2.0 - Wz * 20.0;
            BlockPos Spot = BlockPos.containing(X, Y, Z);
            if (!World.canSeeSky(Spot) || !Scp4666World.openToSky(World, Spot)) {
                continue;
            }
            World.addParticle(ParticleTypes.SNOWFLAKE, X, Y, Z, Wx, -0.04 - Random.nextDouble() * 0.04, Wz);
        }
    }

    private static void emitters(ClientLevel World) {
        RandomSource Random = World.getRandom();
        Iterator<Emitter> Walk = Emitters.iterator();
        while (Walk.hasNext()) {
            Emitter Next = Walk.next();
            Next.Left()[0]--;
            if (Next.Left()[0] <= 0) {
                Walk.remove();
                continue;
            }
            if (Next.Kind() == Scp4666FxPayload.Breath) {
                Direction Out = Direction.from3DDataValue(Next.Value() & 7);
                Direction Along = Out.getClockWise();
                int Width = Math.max(1, Next.Value() >> 3);
                for (int Index = 0; Index < 2; Index++) {
                    double Spread = (Random.nextDouble() - 0.5) * Width;
                    World.addParticle(ParticleTypes.CLOUD, Next.Pos().x + Along.getStepX() * Spread, Next.Pos().y + (Random.nextDouble() - 0.4) * 1.2,
                            Next.Pos().z + Along.getStepZ() * Spread, -Out.getStepX() * 0.004, 0.006, -Out.getStepZ() * 0.004);
                }
            } else if (Next.Kind() == Scp4666FxPayload.Snow) {
                for (int Index = 0; Index < 2; Index++) {
                    World.addParticle(ParticleTypes.SNOWFLAKE, Next.Pos().x + (Random.nextDouble() - 0.5) * 1.6, Next.Pos().y + Random.nextDouble() * 0.6,
                            Next.Pos().z + (Random.nextDouble() - 0.5) * 1.6, 0.0, -0.03, 0.0);
                }
            }
        }
    }

    public static boolean started() {
        return Started;
    }

    public static int phase() {
        return Phase;
    }

    public static List<Integer> tasks() {
        return Tasks;
    }

    public static boolean held() {
        return Held;
    }

    public static int night() {
        return Night;
    }

    public static boolean lightsDead() {
        return LightsDead;
    }

    public static int tree() {
        return Tree;
    }

    public static boolean inCabin(Vec3 Point) {
        return Mapped && Point.x >= Cabin[0] && Point.x < Cabin[3] + 1 && Point.y >= Cabin[1] && Point.y < Cabin[4] + 1 && Point.z >= Cabin[2] && Point.z < Cabin[5] + 1;
    }

    public static double distanceFromCentre(Vec3 Point) {
        double Dx = Point.x - (CentreX + 0.5);
        double Dz = Point.z - (CentreZ + 0.5);
        return Math.sqrt(Dx * Dx + Dz * Dz);
    }

    public static float flash(float Partial) {
        return Flash <= 0 ? 0.0f : (Flash - Partial) / FlashTicks;
    }

    public static float shake(float Partial) {
        return Shake <= 0 ? 0.0f : (Shake - Partial) / ShakeTicks;
    }

    public static float wake(float Partial) {
        return Wake <= 0 ? 0.0f : (Wake - Partial) / WakeTicks;
    }

    public static float sack(float Partial) {
        return Sack <= 0 ? 0.0f : Math.min(1.0f, (Sack + Partial) / SackLength);
    }

    public static float bury(float Partial) {
        return Bury <= 0 ? 0.0f : Math.min(1.0f, (Bury + Partial) / BuryLength);
    }

    public static int ticks() {
        return Ticks;
    }
}
