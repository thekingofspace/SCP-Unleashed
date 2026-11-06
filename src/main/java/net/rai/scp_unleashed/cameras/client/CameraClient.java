package net.rai.scp_unleashed.cameras.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.cameras.CameraCooldown;
import net.rai.scp_unleashed.cameras.CameraMount;
import net.rai.scp_unleashed.cameras.CameraRegistry;
import net.rai.scp_unleashed.cameras.network.CameraExitPayload;
import net.rai.scp_unleashed.cameras.network.CameraFeedPayload;
import net.rai.scp_unleashed.cameras.network.CameraLookPayload;
import net.rai.scp_unleashed.cameras.network.CameraSwitchPayload;
import net.rai.scp_unleashed.senses.Senses;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public final class CameraClient {
    public static final float Fov = 70.0f;
    public static final float MouseShare = 0.55f;
    public static final float KeyRate = 50.0f;
    public static final int ConnectTicks = 10;
    public static final int LookGap = 2;
    public static final int ServoGap = 5;
    public static final int SwitchGap = 6;
    public static final float ServoThreshold = 0.4f;
    public static final double AnchorSlack = 1.0e-6;
    public static final double AnchorRelease = 1.0;
    private static boolean Active;
    private static BlockPos Pos = BlockPos.ZERO;
    private static CameraMount Mount = CameraMount.Fallback;
    private static float Pan;
    private static float Tilt;
    private static float SentPan;
    private static float SentTilt;
    private static String Name = "";
    private static int Index;
    private static int Count;
    @Nullable
    private static CameraEye Eye;
    @Nullable
    private static CameraType SavedType;
    @Nullable
    private static LocalPlayer Owner;
    private static float LockYaw;
    private static float LockPitch;
    private static int Ticks;
    private static int SwitchedAt;
    private static int LastLook;
    private static final CameraCooldown ServoPace = new CameraCooldown(ServoGap);
    private static final CameraCooldown SwitchPace = new CameraCooldown(SwitchGap);
    private static float TickMoved;
    private static boolean ShiftArmed;
    @Nullable
    private static Vec3 Anchor;
    private static boolean Anchored;

    private CameraClient() {
    }

    public static boolean isActive() {
        return Active;
    }

    public static String name() {
        return Name;
    }

    public static int index() {
        return Index;
    }

    public static int count() {
        return Count;
    }

    public static float pan() {
        return Pan;
    }

    public static float tilt() {
        return Tilt;
    }

    public static CameraMount mount() {
        return Mount;
    }

    public static BlockPos pos() {
        return Pos;
    }

    public static int ticks() {
        return Ticks;
    }

    public static int sinceSwitch() {
        return Ticks - SwitchedAt;
    }

    public static boolean connecting() {
        Minecraft Game = Minecraft.getInstance();
        if (!Active || Game.level == null) {
            return false;
        }
        return sinceSwitch() < ConnectTicks || !Game.level.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(Pos.getX()), SectionPos.blockToSectionCoord(Pos.getZ()));
    }

    public static boolean blind() {
        Minecraft Game = Minecraft.getInstance();
        return Game.player != null && Senses.isBlindfolded(Game.player);
    }

    public static void feed(CameraFeedPayload Payload) {
        if (!Payload.Active()) {
            if (Active) {
                exit();
            }
            return;
        }
        boolean Fresh = !Active;
        boolean Moved = Fresh || !Payload.Pos().equals(Pos);
        Pos = Payload.Pos();
        Mount = new CameraMount(Payload.Ceiling(), Payload.Facing());
        Name = Payload.Name();
        Index = Payload.Index();
        Count = Payload.Count();
        if (Moved) {
            Pan = CameraMount.clampPan(Payload.Pan());
            Tilt = Mount.clampTilt(Payload.Tilt());
            SentPan = Pan;
            SentTilt = Tilt;
            SwitchedAt = Ticks;
            play(CameraRegistry.SwitchClick.get(), 1.0f, 0.45f);
            play(CameraRegistry.SwitchStatic.get(), 1.0f, 0.35f);
            play(CameraRegistry.SwitchHiss.get(), 1.0f, 0.25f);
            play(CameraRegistry.RecordBeep.get(), 1.0f, 0.3f);
        }
        if (Fresh) {
            enter();
        } else {
            placeEye();
        }
    }

    private static void enter() {
        Minecraft Game = Minecraft.getInstance();
        LocalPlayer User = Game.player;
        if (User == null || Game.level == null) {
            return;
        }
        Owner = User;
        Active = true;
        Ticks = 0;
        SwitchedAt = 0;
        LastLook = 0;
        TickMoved = 0.0f;
        ServoPace.reset();
        SwitchPace.reset();
        Anchor = User.position();
        Anchored = !User.isPassenger() && steady(User);
        SavedType = Game.options.getCameraType();
        Game.options.setCameraType(CameraType.FIRST_PERSON);
        LockYaw = User.getYRot();
        LockPitch = User.getXRot();
        Eye = new CameraEye(Game.level);
        placeEye();
        Game.setCameraEntity(Eye);
        Game.gameRenderer.setRenderHand(false);
        drain(CameraClientEvents.LeaveKey);
        if (Game.screen instanceof CameraTabletScreen) {
            Game.setScreen(null);
        }
        ShiftArmed = !Game.options.keyShift.isDown();
    }

    public static void exit() {
        Minecraft Game = Minecraft.getInstance();
        boolean Was = Active;
        Active = false;
        Eye = null;
        if (Game.player != null) {
            if (Owner == Game.player) {
                Game.player.setYRot(LockYaw);
                Game.player.yRotO = LockYaw;
                Game.player.setXRot(LockPitch);
                Game.player.xRotO = LockPitch;
            }
            Game.setCameraEntity(Game.player);
        }
        Game.gameRenderer.setRenderHand(true);
        if (SavedType != null) {
            Game.options.setCameraType(SavedType);
        }
        SavedType = null;
        Owner = null;
        Anchor = null;
        Anchored = false;
        if (Was) {
            play(CameraRegistry.SwitchClick.get(), 0.8f, 0.4f);
        }
    }

    public static void reset() {
        if (Active) {
            Minecraft Game = Minecraft.getInstance();
            Active = false;
            Eye = null;
            Game.gameRenderer.setRenderHand(true);
            if (SavedType != null) {
                Game.options.setCameraType(SavedType);
            }
            SavedType = null;
            Owner = null;
            Anchor = null;
            Anchored = false;
        }
    }

    private static void play(SoundEvent Sound, float Pitch, float Volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(Sound, Pitch, Volume));
    }

    private static void placeEye() {
        if (Eye != null) {
            Vec3 Where = Mount.eye(Pos, Pan, Tilt);
            Eye.place(Where, Mount.yaw(Pan), Mount.pitch(Tilt));
        }
    }

    public static void requestExit() {
        if (!Active) {
            return;
        }
        PacketDistributor.sendToServer(CameraExitPayload.Instance);
        exit();
    }

    public static void requestSwitch(int Step) {
        if (!Active || !SwitchPace.use()) {
            return;
        }
        PacketDistributor.sendToServer(new CameraSwitchPayload(Step));
        play(CameraRegistry.SwitchClick.get(), 1.15f, 0.35f);
    }

    private static void drain(KeyMapping Key) {
        while (Key.consumeClick()) {
            Key.setDown(false);
        }
    }

    public static boolean escapePressed() {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_ESCAPE);
    }

    public static void tickStart() {
        if (!Active) {
            return;
        }
        Minecraft Game = Minecraft.getInstance();
        if (Game.player == null || Game.level == null || Game.player != Owner) {
            reset();
            return;
        }
        Ticks++;
        SwitchPace.tick();
        ServoPace.tick();
        Options Settings = Game.options;
        drain(Settings.keyDrop);
        drain(Settings.keyInventory);
        drain(Settings.keySwapOffhand);
        drain(Settings.keyTogglePerspective);
        drain(Settings.keyAdvancements);
        drain(Settings.keySaveHotbarActivator);
        drain(Settings.keyLoadHotbarActivator);
        for (KeyMapping Slot : Settings.keyHotbarSlots) {
            drain(Slot);
        }
        while (CameraClientEvents.NextKey.consumeClick()) {
            requestSwitch(1);
        }
        while (CameraClientEvents.PreviousKey.consumeClick()) {
            requestSwitch(-1);
        }
        boolean LeavePressed = false;
        while (CameraClientEvents.LeaveKey.consumeClick()) {
            LeavePressed = true;
        }
        if (LeavePressed && Game.screen == null) {
            requestExit();
            return;
        }
        boolean ShiftDown = Settings.keyShift.isDown();
        if (!ShiftDown) {
            ShiftArmed = true;
        } else if (ShiftArmed && Game.screen == null) {
            ShiftArmed = false;
            requestExit();
            return;
        }
        if (Settings.getCameraType() != CameraType.FIRST_PERSON) {
            Settings.setCameraType(CameraType.FIRST_PERSON);
        }
    }

    public static void tickEnd() {
        if (!Active) {
            return;
        }
        if ((Math.abs(Pan - SentPan) > 0.05f || Math.abs(Tilt - SentTilt) > 0.05f) && Ticks - LastLook >= LookGap) {
            PacketDistributor.sendToServer(new CameraLookPayload(Pan, Tilt));
            SentPan = Pan;
            SentTilt = Tilt;
            LastLook = Ticks;
        }
        if (TickMoved > ServoThreshold && ServoPace.use()) {
            play(CameraRegistry.Servo.get(), 1.0f, 0.18f);
            play(CameraRegistry.ServoRatchet.get(), 1.0f, 0.1f);
        }
        TickMoved = 0.0f;
    }

    private static boolean steady(Player Body) {
        return Body.onGround() || Body.isInLiquid() || Body.onClimbable() || Body.getAbilities().flying;
    }

    public static void holdBody(Player Body) {
        if (!Active || Body != Owner || Anchor == null) {
            return;
        }
        if (Body.isPassenger()) {
            Entity Ride = Body.getRootVehicle();
            if (Ride != Body && Ride.isControlledByLocalInstance()) {
                Ride.setDeltaMovement(Vec3.ZERO);
            }
            return;
        }
        if (!Anchored) {
            if (!steady(Body)) {
                return;
            }
            Anchored = true;
            Anchor = Body.position();
        }
        Body.setDeltaMovement(Vec3.ZERO);
        double Drift = Body.position().distanceToSqr(Anchor);
        if (Drift > AnchorRelease) {
            Anchor = Body.position();
        } else if (Drift > AnchorSlack) {
            Body.setPos(Anchor);
        }
    }

    public static void frame(DeltaTracker Tracker) {
        if (!Active) {
            return;
        }
        Minecraft Game = Minecraft.getInstance();
        Game.gameRenderer.setRenderHand(false);
        LocalPlayer User = Game.player;
        if (User == null || Game.level == null || User != Owner) {
            return;
        }
        float DeltaYaw = Mth.wrapDegrees(User.getYRot() - LockYaw);
        float DeltaPitch = User.getXRot() - LockPitch;
        User.setYRot(LockYaw);
        User.yRotO = LockYaw;
        User.setXRot(LockPitch);
        User.xRotO = LockPitch;
        float OldPan = Pan;
        float OldTilt = Tilt;
        if (Game.screen == null) {
            Options Settings = Game.options;
            float Seconds = Math.min(0.25f, Tracker.getRealtimeDeltaTicks() / 20.0f);
            float KeyYaw = (Settings.keyRight.isDown() ? 1.0f : 0.0f) - (Settings.keyLeft.isDown() ? 1.0f : 0.0f);
            float KeyPitch = (Settings.keyDown.isDown() ? 1.0f : 0.0f) - (Settings.keyUp.isDown() ? 1.0f : 0.0f);
            Pan += DeltaYaw * MouseShare + KeyYaw * KeyRate * Seconds;
            Tilt += DeltaPitch * MouseShare + KeyPitch * KeyRate * Seconds;
        }
        Pan = CameraMount.clampPan(Pan);
        Tilt = Mount.clampTilt(Tilt);
        TickMoved += Math.abs(Pan - OldPan) + Math.abs(Tilt - OldTilt);
        placeEye();
        if (Eye != null && Game.getCameraEntity() != Eye) {
            Game.setCameraEntity(Eye);
        }
    }
}
