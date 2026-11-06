package net.rai.scp_unleashed.scp513.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.scp513.Scp513Affliction;
import net.rai.scp_unleashed.scp513.Scp513Pose;
import net.rai.scp_unleashed.scp513.Scp513Registry;
import net.rai.scp_unleashed.scp513.Scp513Stage;
import net.rai.scp_unleashed.scp513.Scp513Stalker;
import org.jetbrains.annotations.Nullable;

public final class Scp513Visions {
    public static final int[] GlimpseAt = {Scp513Stage.FreezeAt * 2 / 5, Scp513Stage.FreezeAt * 3 / 4};
    public static final int GlimpseLife = 34;
    public static final double GlimpseDistance = 17.0;
    public static final float GlimpseAngle = 68.0f;
    public static final float ApproachSpread = 50.0f;
    public static final int StepCycle = 32;
    public static final int RightStep = 18;
    public static final float StepVolume = 0.8f;
    public static final int HissEvery = 70;
    public static final float HissVolume = 0.5f;
    public static final int FirstId = -5_130_000;
    public static final int LastId = -2_000_000_000;
    @Nullable
    private static Scp513Stalker Glimpse;
    @Nullable
    private static Scp513Stalker Approacher;
    private static Vec3 Direction = new Vec3(0.0, 0.0, 1.0);
    private static int LastElapsed = -1;
    private static int NextId = FirstId;
    private static ClientLevel Current;

    private Scp513Visions() {
    }

    @Nullable
    public static Scp513Stalker approacher() {
        return Approacher;
    }

    public static void tick(Minecraft Client) {
        LocalPlayer User = Client.player;
        ClientLevel World = Client.level;
        if (User == null || World == null || !Scp513ClientState.isActive()) {
            reset();
            return;
        }
        if (World != Current) {
            reset();
            Current = World;
        }
        if (Client.isPaused()) {
            return;
        }
        int Elapsed = Scp513ClientState.elapsed();
        for (int At : GlimpseAt) {
            if (LastElapsed >= 0 && LastElapsed < At && Elapsed >= At && Elapsed < Scp513Stage.FreezeAt) {
                glimpse(World, User, At);
            }
        }
        LastElapsed = Elapsed;
        if (Glimpse != null && Glimpse.isRemoved()) {
            Glimpse = null;
        }
        if (Glimpse != null) {
            Glimpse.place(Glimpse.position(), yawToward(Glimpse.position(), User.position()), false);
        }
        Scp513Stage Stage = Scp513Stage.of(Elapsed);
        if (Stage.ordinal() < Scp513Stage.Approach.ordinal()) {
            dropApproacher();
            return;
        }
        if (Approacher == null || Approacher.isRemoved()) {
            Direction = chooseDirection(User, Scp513ClientState.seed());
            Approacher = spawn(World);
            if (Approacher == null) {
                return;
            }
            Approacher.place(target(User, Elapsed), yawToward(target(User, Elapsed), User.position()), true);
        } else {
            Vec3 Where = target(User, Elapsed);
            Approacher.place(Where, yawToward(Where, User.position()), false);
        }
        Scp513Pose Gesture = Elapsed < Scp513Stage.GrabAt ? Scp513Pose.Walk : Elapsed < Scp513Stage.StrangleAt ? Scp513Pose.Reach : Scp513Pose.Strangle;
        Approacher.setShown(Gesture);
        if (Gesture == Scp513Pose.Walk) {
            int Phase = (Elapsed - Scp513Stage.ArriveAt) % StepCycle;
            if (Phase == 0 || Phase == RightStep) {
                sound(World, Approacher.position(), Scp513Registry.Step, StepVolume, 0.7f + User.getRandom().nextFloat() * 0.08f);
                if (Phase != 0) {
                    sound(World, Approacher.position(), Scp513Registry.StepDrag, StepVolume * 0.7f, 0.8f);
                }
            }
            if ((Elapsed - Scp513Stage.ArriveAt) % HissEvery == 10) {
                sound(World, Approacher.hands(), Scp513Registry.Miasma, HissVolume, 0.8f);
            }
        }
    }

    private static Vec3 target(LocalPlayer User, int Elapsed) {
        return User.position().add(Direction.scale(Scp513Affliction.approachDistance(Elapsed)));
    }

    private static Vec3 chooseDirection(LocalPlayer User, long Seed) {
        float Offset = ((Seed >>> 32 & 0xFFFF) / 65535.0f * 2.0f - 1.0f) * ApproachSpread;
        float Yaw = (User.getYRot() + Offset) * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(Yaw), 0.0, Mth.cos(Yaw));
    }

    public static float yawToward(Vec3 From, Vec3 To) {
        double Dx = To.x - From.x;
        double Dz = To.z - From.z;
        return (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
    }

    private static void glimpse(ClientLevel World, LocalPlayer User, int At) {
        if (Glimpse != null && !Glimpse.isRemoved()) {
            Glimpse.discard();
        }
        float Side = (At & 1) == 0 ? 1.0f : -1.0f;
        float Yaw = (User.getYRot() + Side * GlimpseAngle) * Mth.DEG_TO_RAD;
        Vec3 Where = User.position().add(-Mth.sin(Yaw) * GlimpseDistance, 0.0, Mth.cos(Yaw) * GlimpseDistance);
        Scp513Stalker Spawned = spawn(World);
        if (Spawned == null) {
            return;
        }
        Spawned.setLife(GlimpseLife);
        Spawned.setShown(Scp513Pose.Idle);
        Spawned.place(Where, yawToward(Where, User.position()), true);
        Glimpse = Spawned;
    }

    @Nullable
    private static Scp513Stalker spawn(ClientLevel World) {
        Scp513Stalker Spawned = new Scp513Stalker(Scp513Registry.Stalker.get(), World);
        Spawned.setId(nextId());
        World.addEntity(Spawned);
        if (Spawned.isRemoved() || World.getEntity(Spawned.getId()) != Spawned) {
            return null;
        }
        return Spawned;
    }

    private static int nextId() {
        int Id = NextId;
        NextId = NextId <= LastId ? FirstId : NextId - 1;
        return Id;
    }

    private static void sound(ClientLevel World, Vec3 Where, DeferredHolder<SoundEvent, SoundEvent> Layer, float Volume, float Pitch) {
        World.playLocalSound(Where.x, Where.y, Where.z, Layer.get(), SoundSource.HOSTILE, Volume, Pitch, false);
    }

    private static void dropApproacher() {
        if (Approacher != null && !Approacher.isRemoved()) {
            Approacher.discard();
        }
        Approacher = null;
    }

    public static void reset() {
        if (Glimpse != null && !Glimpse.isRemoved()) {
            Glimpse.discard();
        }
        Glimpse = null;
        dropApproacher();
        LastElapsed = -1;
        Current = null;
    }
}
