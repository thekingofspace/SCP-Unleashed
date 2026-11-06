package net.rai.scp_unleashed.scp178.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp178.Scp178GlassesItem;
import net.rai.scp_unleashed.scp178.Scp178Registry;
import net.rai.scp_unleashed.scp178.Scp178Steering;
import net.rai.scp_unleashed.scp178.Scp178Watcher;
import net.rai.scp_unleashed.scp178.network.Scp178ReportPayload;
import net.rai.scp_unleashed.scp178.network.Scp178WatcherPayload;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Scp178ClientWatchers {
    public static final int FirstEntityId = -178000000;
    public static final double NoticeCone = 0.985;
    public static final double NoticeWidth = 1.4;
    public static final double NoticeRange = 40.0;
    public static final int NoticeMin = 50;
    public static final int NoticeMax = 90;
    public static final float NoticeFade = 0.5f;
    public static final double StruckDistance = 2.0;
    public static final int OrphanTicks = 40;
    public static final int FadeGrace = 30;
    public static final int RetryTicks = 10;

    private static final Map<Integer, Visual> Visuals = new LinkedHashMap<>();
    private static final Map<Scp178Watcher, Integer> Fading = new LinkedHashMap<>();
    private static int NextEntityId = FirstEntityId;
    @Nullable
    private static ClientLevel LastLevel;
    private static boolean WasWearing;
    private static int Orphaned;
    private static int Clock;

    private Scp178ClientWatchers() {
    }

    private static final class Visual {
        private final int Id;
        private boolean Hostile;
        private double Target;
        private double Bearing;
        private int Noticed;
        private int NoticeLimit;
        private boolean Reported;
        private boolean Relocating;
        private int Retry;
        @Nullable
        private Scp178Steering Motion;
        @Nullable
        private Scp178Watcher Body;

        private Visual(int Id) {
            this.Id = Id;
        }
    }

    public static int alive() {
        return Visuals.size();
    }

    public static int lingering() {
        return Visuals.size() + Fading.size();
    }

    public static void handle(Scp178WatcherPayload Payload) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || Client.level == null) {
            return;
        }
        Visual Found = Visuals.get(Payload.Id());
        switch (Payload.Kind()) {
            case Scp178WatcherPayload.Spawn -> spawn(Client.level, Client.player, Payload.Id(), Payload.Value(), Payload.Angle());
            case Scp178WatcherPayload.Hostile -> {
                if (Found != null) {
                    Found.Hostile = true;
                    if (Found.Motion != null) {
                        Found.Motion.setHostile(true);
                    }
                    if (Found.Body != null) {
                        Found.Body.setHostile(true);
                        Scp178Voices.cue(Found.Body, Scp178Watcher.TurnCue);
                    }
                }
            }
            case Scp178WatcherPayload.Calm -> {
                if (Found != null) {
                    Found.Hostile = false;
                    Found.Target = Payload.Value();
                    Found.Bearing = Payload.Angle();
                    Found.Reported = false;
                    Found.Noticed = 0;
                    if (Found.Motion != null) {
                        Found.Motion.setHostile(false);
                        Found.Motion.setAnchor(Found.Bearing);
                        Found.Motion.hold(false);
                        Found.Motion.endLunge();
                    }
                    if (Found.Body != null) {
                        Found.Body.setHostile(false);
                    }
                }
            }
            case Scp178WatcherPayload.Distance -> {
                if (Found != null) {
                    Found.Target = Payload.Value();
                }
            }
            case Scp178WatcherPayload.Windup -> {
                if (Found != null && Found.Body != null && Found.Motion != null) {
                    Found.Motion.hold(true);
                    Found.Body.startAction(Scp178Watcher.ActionWindup);
                    Scp178Voices.cue(Found.Body, Scp178Watcher.ActionWindup);
                }
            }
            case Scp178WatcherPayload.Attack -> {
                if (Found != null) {
                    Found.Target = StruckDistance;
                    if (Found.Body != null && Found.Motion != null) {
                        Found.Motion.hold(false);
                        if (Found.Motion.lunge(Client.player.position(), Payload.Value(), Scp178Steering.groundOf(Client.level), Client.player.getRandom())) {
                            Vec3 Blink = Found.Motion.position();
                            Found.Body.moveTo(Blink.x, Blink.y, Blink.z, Found.Body.getYRot(), 0.0f);
                        }
                        Found.Body.startAction(Scp178Watcher.ActionLunge);
                        Scp178Voices.cue(Found.Body, Scp178Watcher.ActionLunge);
                        Scp178Voices.cue(Found.Body, Scp178Watcher.StrikeCue);
                    }
                }
            }
            case Scp178WatcherPayload.Killed -> {
                Visual Gone = Visuals.remove(Payload.Id());
                if (Gone != null && Gone.Body != null) {
                    Gone.Body.vanish();
                    Scp178Voices.cue(Gone.Body, Scp178Voices.FadeCue);
                    retire(Gone.Body);
                }
            }
            case Scp178WatcherPayload.Clear -> fadeAll();
            default -> {
            }
        }
    }

    private static void spawn(ClientLevel Level, Player Viewer, int Id, double Distance, double Bearing) {
        Visual Old = Visuals.remove(Id);
        if (Old != null && Old.Body != null) {
            Old.Body.leave();
            retire(Old.Body);
        }
        RandomSource Random = Viewer.getRandom();
        Visual Made = new Visual(Id);
        Made.Target = Distance;
        Made.Bearing = Bearing;
        Made.NoticeLimit = NoticeMin + Random.nextInt(NoticeMax - NoticeMin + 1);
        Visuals.put(Id, Made);
        arrive(Level, Viewer, Made, Scp178Steering.groundOf(Level));
    }

    private static boolean arrive(ClientLevel Level, Player Viewer, Visual Shape, Scp178Steering.Ground Floor) {
        Scp178Steering Motion = new Scp178Steering(Viewer.getX(), Viewer.getY(), Viewer.getZ());
        Motion.setAnchor(Shape.Bearing);
        Motion.setHostile(Shape.Hostile);
        if (!Motion.place(Viewer.position(), Shape.Target, Shape.Bearing, Floor, Viewer.getRandom())) {
            Shape.Retry = RetryTicks;
            return false;
        }
        Shape.Motion = Motion;
        Shape.Body = body(Level, Viewer, Shape);
        return true;
    }

    private static Scp178Watcher body(ClientLevel Level, Player Viewer, Visual Shape) {
        Scp178Watcher Made = new Scp178Watcher(Scp178Registry.Watcher.get(), Level);
        Made.setId(NextEntityId--);
        Made.bind(Shape.Id);
        Made.setHostile(Shape.Hostile);
        Vec3 At = Shape.Motion.position();
        float Yaw = yawToward(At, Viewer.position());
        Made.moveTo(At.x, At.y, At.z, Yaw, 0.0f);
        Made.setYBodyRot(Yaw);
        Made.setYHeadRot(Yaw);
        Made.face(Yaw, Yaw, 0.0f);
        Level.addEntity(Made);
        return Made;
    }

    private static float yawToward(Vec3 From, Vec3 To) {
        return (float) (Mth.atan2(To.z - From.z, To.x - From.x) * Mth.RAD_TO_DEG) - 90.0f;
    }

    public static void tick(Minecraft Client) {
        ClientLevel Level = Client.level;
        if (Level != LastLevel) {
            reset();
            LastLevel = Level;
        }
        if (Level == null || Client.player == null) {
            return;
        }
        boolean Wearing = Scp178GlassesItem.isWorn(Client.player);
        if (WasWearing && !Wearing) {
            fadeAll();
        }
        WasWearing = Wearing;
        Orphaned = Wearing || Visuals.isEmpty() ? 0 : Orphaned + 1;
        if (Orphaned > OrphanTicks) {
            fadeAll();
            Orphaned = 0;
        }
        if (!Client.isPaused()) {
            Scp178Steering.Ground Floor = Scp178Steering.groundOf(Level);
            for (Visual Shape : Visuals.values()) {
                update(Level, Client.player, Shape, Floor);
            }
        }
        Clock++;
        Fading.entrySet().removeIf(Entry -> {
            Scp178Watcher Body = Entry.getKey();
            if (Body.isRemoved()) {
                return true;
            }
            if (Body.fadeDone() || Clock >= Entry.getValue()) {
                Body.discard();
                return true;
            }
            return false;
        });
    }

    private static void update(ClientLevel Level, Player Viewer, Visual Shape, Scp178Steering.Ground Floor) {
        if (Shape.Motion == null) {
            if (--Shape.Retry <= 0) {
                arrive(Level, Viewer, Shape, Floor);
            }
            return;
        }
        if (Shape.Body == null || Shape.Body.isRemoved()) {
            Shape.Body = body(Level, Viewer, Shape);
        }
        Scp178Watcher Body = Shape.Body;
        Scp178Steering Motion = Shape.Motion;
        Vec3 Player = Viewer.position();
        if (Body.action() != Scp178Watcher.ActionLunge && Body.action() != Scp178Watcher.ActionRecover) {
            Motion.endLunge();
        }
        if (!Shape.Relocating && (Motion.needsRelocation(Player, Shape.Target) || Motion.lost(Player, Floor))) {
            Shape.Relocating = true;
            Shape.Retry = 0;
            Body.fadeOut();
        }
        if (Shape.Relocating) {
            if (Body.absent() && --Shape.Retry <= 0) {
                if (Motion.relocate(Player, Shape.Target, Floor, Viewer.getRandom())) {
                    Vec3 Moved = Motion.position();
                    Body.moveTo(Moved.x, Moved.y, Moved.z, Body.getYRot(), 0.0f);
                    Body.fadeIn();
                    Shape.Relocating = false;
                } else {
                    Shape.Retry = RetryTicks;
                }
            }
            return;
        }
        Motion.tick(Player, Shape.Target, Floor, Body.getRandom());
        Vec3 At = Motion.position();
        Body.setPos(At.x, At.y, At.z);
        Vec3 Eye = Viewer.getEyePosition();
        float Look = yawToward(At, Player);
        float Heading = Motion.moving() ? (float) Motion.headingYaw() : Look;
        double Flat = Math.hypot(Eye.x - At.x, Eye.z - At.z);
        float Pitch = (float) (-Mth.atan2(Eye.y - Body.getEyeY(), Flat) * Mth.RAD_TO_DEG);
        Body.face(Heading, Look, Pitch);
        if (!Shape.Hostile && !Shape.Reported && Scp178Vision.fade(0.0f) >= NoticeFade) {
            Shape.Noticed = lookedAt(Level, Viewer, Body) ? Shape.Noticed + 1 : Math.max(0, Shape.Noticed - 1);
            if (Shape.Noticed >= Shape.NoticeLimit) {
                Shape.Reported = true;
                PacketDistributor.sendToServer(new Scp178ReportPayload(Scp178ReportPayload.Stared, Shape.Id));
            }
        }
    }

    private static boolean lookedAt(ClientLevel Level, Player Viewer, Scp178Watcher Body) {
        Vec3 Eye = Viewer.getEyePosition();
        Vec3 Middle = Body.position().add(0.0, Body.getBbHeight() * 0.72, 0.0);
        Vec3 Toward = Middle.subtract(Eye);
        double Distance = Toward.length();
        if (Distance < 1.0e-3 || Distance > NoticeRange) {
            return Distance < 1.0e-3;
        }
        double Facing = Viewer.getViewVector(1.0f).dot(Toward.scale(1.0 / Distance));
        if (Facing <= Math.min(NoticeCone, Math.cos(Math.atan2(NoticeWidth, Distance)))) {
            return false;
        }
        BlockHitResult Hit = Level.clip(new ClipContext(Eye, Middle, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, Viewer));
        return Hit.getType() == HitResult.Type.MISS;
    }

    public static boolean struck(Scp178Watcher Body) {
        Visual Shape = Visuals.get(Body.phantomId());
        if (Shape == null || Shape.Body != Body || Body.isFading()) {
            return false;
        }
        PacketDistributor.sendToServer(new Scp178ReportPayload(Scp178ReportPayload.Struck, Shape.Id));
        Body.flinch();
        Scp178Voices.cue(Body, Scp178Watcher.HurtCue);
        return true;
    }

    private static void retire(Scp178Watcher Body) {
        Fading.putIfAbsent(Body, Clock + Math.max(Scp178Watcher.FadeTicks, Scp178Watcher.DeathTicks) + FadeGrace);
    }

    public static void fadeAll() {
        for (Visual Shape : Visuals.values()) {
            if (Shape.Body != null && !Shape.Body.isRemoved()) {
                Shape.Body.leave();
                retire(Shape.Body);
            }
        }
        Visuals.clear();
    }

    public static void reset() {
        for (Visual Shape : Visuals.values()) {
            if (Shape.Body != null) {
                Shape.Body.discard();
            }
        }
        for (Scp178Watcher Body : Fading.keySet()) {
            Body.discard();
        }
        Visuals.clear();
        Fading.clear();
        WasWearing = false;
        Orphaned = 0;
    }
}
