package net.rai.scp_unleashed.scp335.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp335.Scp335Episodes;
import net.rai.scp_unleashed.scp335.Scp335Phase;
import net.rai.scp_unleashed.scp335.Scp335Registry;
import net.rai.scp_unleashed.scp335.network.Scp335StatePayload;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Scp335ClientState {
    public static final int HideAfter = Scp335Episodes.HideAfter;
    public static final float CrawlShare = 0.3f;
    public static final float TurnRate = 9.0f;
    public static final float CrawlTurnRate = 6.0f;
    public static final double CrawlLeash = 1.4;
    public static final double WalkStop = 0.45;
    private static final Map<Integer, Shown> States = new HashMap<>();
    private static final RandomSource Random = RandomSource.create();
    private static int Lurch;
    private static int Rest;
    private static float Push;
    private static float Strafe;
    private static float Heading;
    private static float Droop;
    private static int Clock;
    private static Vec3 Anchor = Vec3.ZERO;

    private Scp335ClientState() {
    }

    public static void apply(Scp335StatePayload Payload) {
        Minecraft Game = Minecraft.getInstance();
        if (Game.level == null) {
            return;
        }
        Entity Found = Game.level.getEntity(Payload.EntityId());
        if (Payload.Phase() == Scp335Phase.None) {
            States.remove(Payload.EntityId());
            if (Found instanceof Player User) {
                User.setForcedPose(null);
            }
            return;
        }
        Shown Before = States.get(Payload.EntityId());
        Shown After = new Shown(Payload.Phase(), Payload.Tick(), Payload.Approach(), Payload.Screen(), Payload.Assisted(), Payload.Lit());
        States.put(Payload.EntityId(), After);
        if (Found instanceof Player User) {
            User.setForcedPose(Payload.Phase() == Scp335Phase.Crawl ? Pose.SWIMMING : null);
        }
        if (Found == Game.player && (Before == null || Before.Phase != After.Phase)) {
            entered(Game, Game.player, After);
        }
    }

    private static void entered(Minecraft Game, LocalPlayer User, Shown State) {
        if (State.Phase != Scp335Phase.Glimpse && Game.screen != null && !(Game.screen instanceof PauseScreen) && !(Game.screen instanceof ChatScreen)) {
            Game.setScreen(null);
        }
        Anchor = User.position();
        if (State.Phase == Scp335Phase.Crawl) {
            Heading = User.getYRot() + 180.0f + (Random.nextFloat() - 0.5f) * 60.0f;
            Droop = 28.0f;
            Lurch = 0;
            Rest = 6;
        }
    }

    @Nullable
    public static Shown of(@Nullable Entity Target) {
        return Target == null ? null : States.get(Target.getId());
    }

    @Nullable
    public static Shown local() {
        Minecraft Game = Minecraft.getInstance();
        return of(Game.player);
    }

    public static boolean isLocked() {
        Shown State = local();
        return State != null && State.Phase.isLocked();
    }

    public static boolean isHidden(Entity Target) {
        Shown State = of(Target);
        return State != null && (State.Phase == Scp335Phase.Gone || State.Phase == Scp335Phase.Dissolve && State.Tick >= HideAfter);
    }

    public static void clear() {
        States.clear();
        Scp335Episodes.ClientLocked.clear();
    }

    public static void clearLocal() {
        Minecraft Game = Minecraft.getInstance();
        if (Game.player != null) {
            States.remove(Game.player.getId());
            Game.player.setForcedPose(null);
        }
        Scp335Episodes.ClientLocked.clear();
    }

    public static void consumeKeys(Minecraft Game) {
        Options Settings = Game.options;
        Set<KeyMapping> Allowed = Set.of(Settings.keyChat, Settings.keyCommand, Settings.keyPlayerList, Settings.keyScreenshot, Settings.keyFullscreen,
                Settings.keyTogglePerspective);
        for (KeyMapping Mapping : Settings.keyMappings) {
            if (Allowed.contains(Mapping)) {
                continue;
            }
            boolean Pressed = Mapping.consumeClick();
            while (Pressed) {
                Pressed = Mapping.consumeClick();
            }
            Mapping.setDown(false);
        }
    }

    public static void tick(Minecraft Game) {
        ClientLevel World = Game.level;
        if (World == null) {
            States.clear();
            Scp335Episodes.ClientLocked.clear();
            return;
        }
        Clock++;
        Iterator<Map.Entry<Integer, Shown>> Each = States.entrySet().iterator();
        while (Each.hasNext()) {
            Map.Entry<Integer, Shown> Entry = Each.next();
            Entity Found = World.getEntity(Entry.getKey());
            Shown State = Entry.getValue();
            if (Found == null) {
                if (Entry.getKey() != (Game.player == null ? -1 : Game.player.getId())) {
                    Each.remove();
                }
                continue;
            }
            if (!Game.isPaused()) {
                State.Tick++;
            }
            if (Found instanceof Player User) {
                User.setForcedPose(State.Phase == Scp335Phase.Crawl ? Pose.SWIMMING : null);
            }
            if (!Game.isPaused()) {
                particles(World, Found, State);
            }
        }
        LocalPlayer User = Game.player;
        Shown Mine = local();
        if (User == null || Mine == null) {
            Scp335Episodes.ClientLocked.clear();
            return;
        }
        Scp335Episodes.ClientLocked.clear();
        Scp335Episodes.ClientLocked.add(User.getUUID());
        if (!Game.isPaused()) {
            pilot(User, Mine);
        }
    }

    private static void pilot(LocalPlayer User, Shown State) {
        Vec3 Eye = User.getEyePosition();
        switch (State.Phase) {
            case Crawl -> {
                if (State.Tick == 8) {
                    Anchor = User.position();
                }
                if (Lurch > 0) {
                    Lurch--;
                } else if (Rest > 0) {
                    Rest--;
                } else {
                    Lurch = 4 + Random.nextInt(8);
                    Rest = 4 + Random.nextInt(14);
                    Push = 0.55f + Random.nextFloat() * 0.45f;
                    Strafe = (Random.nextFloat() - 0.5f) * 0.9f;
                    Heading += (Random.nextFloat() - 0.5f) * 80.0f;
                    Droop = 18.0f + Random.nextFloat() * 30.0f;
                }
                if (Scp335Episodes.flat(User.position(), Anchor) > CrawlLeash) {
                    Heading = Scp335Episodes.yawTowards(User.position(), Anchor);
                }
                float Twitch = Lurch > 0 ? Mth.sin(Clock * 0.9f) * 4.0f : Mth.sin(Clock * 0.21f) * 2.0f;
                turn(User, Heading + Twitch, Droop + Mth.sin(Clock * 0.37f) * 3.0f, CrawlTurnRate);
            }
            case Rise -> turn(User, Scp335Episodes.yawTowards(Eye, State.Screen), pitchTowards(Eye, State.Screen) * Math.min(1.0f, State.Tick / 18.0f), TurnRate);
            case Walk -> {
                double Distance = Scp335Episodes.flat(User.position(), State.Approach);
                Vec3 Aim = Distance > 1.6 && !State.Assisted ? State.Approach.add(0.0, User.getEyeHeight(), 0.0) : State.Screen;
                turn(User, Scp335Episodes.yawTowards(Eye, Aim), Distance > 1.6 ? 4.0f : pitchTowards(Eye, State.Screen), TurnRate);
            }
            case Dissolve, Gone -> {
                if (State.Lit) {
                    turn(User, Scp335Episodes.yawTowards(Eye, State.Screen), pitchTowards(Eye, State.Screen), TurnRate);
                } else {
                    turn(User, User.getYRot(), -35.0f, 2.0f);
                }
            }
            default -> {
            }
        }
    }

    private static float pitchTowards(Vec3 From, Vec3 To) {
        double Flat = Scp335Episodes.flat(From, To);
        return (float) -(Mth.atan2(To.y - From.y, Math.max(1.0E-3, Flat)) * Mth.RAD_TO_DEG);
    }

    private static void turn(LocalPlayer User, float Yaw, float Pitch, float Rate) {
        float YawStep = Mth.clamp(Mth.wrapDegrees(Yaw - User.getYRot()), -Rate, Rate);
        float PitchStep = Mth.clamp(Pitch - User.getXRot(), -Rate, Rate);
        User.setYRot(User.getYRot() + YawStep);
        User.setXRot(Mth.clamp(User.getXRot() + PitchStep, -90.0f, 90.0f));
        User.setYHeadRot(User.getYRot());
    }

    public static void steer(Player Mover, Input Controls) {
        if (!(Mover instanceof LocalPlayer User)) {
            return;
        }
        Shown State = of(User);
        if (State == null) {
            return;
        }
        Controls.leftImpulse = 0.0f;
        Controls.forwardImpulse = 0.0f;
        Controls.up = false;
        Controls.down = false;
        Controls.left = false;
        Controls.right = false;
        Controls.jumping = false;
        Controls.shiftKeyDown = false;
        User.setSprinting(false);
        if (State.Phase == Scp335Phase.Crawl && Lurch > 0) {
            Controls.forwardImpulse = CrawlShare * Push;
            Controls.leftImpulse = CrawlShare * Strafe;
            Controls.up = true;
        } else if (State.Phase == Scp335Phase.Walk && !State.Assisted) {
            double Distance = Scp335Episodes.flat(User.position(), State.Approach);
            float Facing = Math.abs(Mth.wrapDegrees(Scp335Episodes.yawTowards(User.position(), State.Approach) - User.getYRot()));
            if (Distance > WalkStop && Facing < 60.0f) {
                Controls.forwardImpulse = Distance > 1.2 ? 1.0f : 0.45f;
                Controls.up = true;
                Controls.jumping = User.horizontalCollision && User.onGround();
            }
        }
    }

    public static float overlay(float Partial) {
        Shown State = local();
        if (State == null) {
            return 0.0f;
        }
        float Tick = State.Tick + Partial;
        return switch (State.Phase) {
            case Glimpse -> Mth.clamp(Tick / Scp335Episodes.GlimpseTicks, 0.0f, 1.0f) * 0.18f;
            case Crawl -> 0.18f + Mth.clamp(Tick / 240.0f, 0.0f, 1.0f) * 0.14f + Mth.sin(Tick * 0.5f) * 0.02f;
            case Rise, Walk -> 0.22f + Mth.sin(Tick * 0.33f) * 0.04f;
            case Dissolve -> 0.26f + Mth.clamp(Tick / Scp335Episodes.DissolveTicks, 0.0f, 1.0f) * 0.54f;
            case Gone -> 0.85f;
            default -> 0.0f;
        };
    }

    private static void particles(ClientLevel World, Entity Found, Shown State) {
        AABB Box = Found.getBoundingBox();
        if (State.Phase == Scp335Phase.Dissolve) {
            int Count = State.Tick < 12 ? 12 : State.Tick < 36 ? 7 : 3;
            for (int Index = 0; Index < Count; Index++) {
                double X = Mth.lerp(Random.nextDouble(), Box.minX, Box.maxX);
                double Y = Mth.lerp(Random.nextDouble(), Box.minY, Box.maxY);
                double Z = Mth.lerp(Random.nextDouble(), Box.minZ, Box.maxZ);
                double Life = Scp335PixelParticle.Life;
                if (State.Lit) {
                    Vec3 Goal = State.Screen.add((Random.nextDouble() - 0.5) * 0.3, (Random.nextDouble() - 0.5) * 0.25, (Random.nextDouble() - 0.5) * 0.3);
                    World.addParticle(Scp335Registry.Pixel.get(), X, Y, Z, (Goal.x - X) / Life, (Goal.y - Y) / Life, (Goal.z - Z) / Life);
                } else {
                    World.addParticle(Scp335Registry.Pixel.get(), X, Y, Z, (Random.nextDouble() - 0.5) * 0.04, 0.05 + Random.nextDouble() * 0.08,
                            (Random.nextDouble() - 0.5) * 0.04);
                }
            }
            return;
        }
        if ((State.Phase == Scp335Phase.Crawl || State.Phase == Scp335Phase.Walk || State.Phase == Scp335Phase.Rise) && Random.nextInt(5) == 0) {
            Vec3 Head = Found.getEyePosition();
            World.addParticle(Scp335Registry.Pixel.get(), Head.x + (Random.nextDouble() - 0.5) * 0.5, Head.y + (Random.nextDouble() - 0.3) * 0.4,
                    Head.z + (Random.nextDouble() - 0.5) * 0.5, (Random.nextDouble() - 0.5) * 0.02, 0.02 + Random.nextDouble() * 0.03, (Random.nextDouble() - 0.5) * 0.02);
        }
    }

    public static List<Integer> tracked() {
        return List.copyOf(States.keySet());
    }

    public static final class Shown {
        public final Scp335Phase Phase;
        public final Vec3 Approach;
        public final Vec3 Screen;
        public final boolean Assisted;
        public final boolean Lit;
        public int Tick;

        public Shown(Scp335Phase Phase, int Tick, Vec3 Approach, Vec3 Screen, boolean Assisted, boolean Lit) {
            this.Phase = Phase;
            this.Tick = Tick;
            this.Approach = Approach;
            this.Screen = Screen;
            this.Assisted = Assisted;
            this.Lit = Lit;
        }
    }
}
