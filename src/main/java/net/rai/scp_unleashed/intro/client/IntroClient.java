package net.rai.scp_unleashed.intro.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.config.ScpClientConfig;
import net.rai.scp_unleashed.intro.IntroDirector;
import net.rai.scp_unleashed.intro.IntroRegistry;
import net.rai.scp_unleashed.intro.IntroWorld;
import net.rai.scp_unleashed.intro.network.IntroActionPayload;
import net.rai.scp_unleashed.intro.network.IntroReadyPayload;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class IntroClient {
    public static final int PhaseNone = 0;
    public static final int PhaseEntering = 1;
    public static final int PhaseOffice = 2;
    public static final int PhaseGate = 3;
    public static final int PhaseReturning = 4;
    public static final int SettleTicks = 20;
    public static final int ForceReadyAfter = 60;
    public static final int BlackLimit = 120;
    public static final int FadeIn = 40;
    public static final int FadeOut = 30;
    public static final int EnterFade = 10;
    public static final int LookUpAt = 96;
    public static final int LookUpTicks = 34;
    public static final float GazeRate = 0.12f;
    public static final float BreathYaw = 0.45f;
    public static final float BreathPitch = 0.6f;
    public static final int DialogueAt = 124;
    public static final int OfficePages = 9;
    public static final int IntercomAt = 30;
    public static final int WalkAt = 110;
    public static final float GatePitchLimit = 12.0f;
    public static final int SkipHold = 30;
    public static final int SkipHintAfter = 40;
    public static final int StallLimit = 240;
    public static final int TitleLength = 120;
    public static final int ArrivalFade = 50;
    public static final float VoiceVolume = 0.35f;
    public static final float IntercomVolume = 0.3f;
    public static final int[] TapPages = {1, 4};
    public static final int[] LeanPages = {2, 6};
    public static final int[] SteeplePages = {3, 7};
    private static final RandomSource Random = RandomSource.create();
    private static int Phase = PhaseNone;
    private static int AwaitToken = -1;
    private static int AwaitStage;
    private static int AwaitAge;
    private static int Settle;
    private static int Token = -1;
    private static int EntityId = -1;
    private static BlockPos Origin = BlockPos.ZERO;
    private static int SceneTicks;
    private static float Veil;
    private static float VeilO;
    private static float VeilTarget;
    private static float VeilStep = 0.05f;
    private static boolean VeilWhite;
    private static int Black;
    private static int Stall;
    @Nullable
    private static IntroDialogue Dialogue;
    private static int LastPage = -1;
    private static int PageStart;
    private static int DialogueEnd = -1;
    private static boolean StandSent;
    private static boolean DoneSent;
    private static int SkipHeld;
    private static int SkipHeldO;
    private static boolean SkipSent;
    private static int TitleTicks = -1;
    private static boolean Crossed;
    private static boolean RisePlayed;
    private static boolean JumpWas;
    private static int NextArc = 60;
    @Nullable
    private static Vec3 Gaze;
    @Nullable
    private static CameraType SavedCamera;

    private IntroClient() {
    }

    public static int phase() {
        return Phase;
    }

    public static boolean active() {
        return Phase != PhaseNone;
    }

    public static boolean playing() {
        return Phase == PhaseOffice || Phase == PhaseGate;
    }

    public static boolean handHidden() {
        return Phase != PhaseNone || Veil > 0.01f;
    }

    public static boolean cameraHeld() {
        return Phase != PhaseNone;
    }

    public static boolean walking() {
        return Phase == PhaseGate && SceneTicks >= WalkAt && !Crossed && !SkipSent;
    }

    public static int sceneTicks() {
        return SceneTicks;
    }

    public static BlockPos origin() {
        return Origin;
    }

    @Nullable
    public static IntroDialogue dialogue() {
        return Dialogue;
    }

    public static float veil(float Partial) {
        return Mth.lerp(Partial, VeilO, Veil);
    }

    public static boolean veilWhite() {
        return VeilWhite;
    }

    public static float skipShare(float Partial) {
        return Mth.clamp(Mth.lerp(Partial, SkipHeldO, SkipHeld) / SkipHold, 0.0f, 1.0f);
    }

    public static boolean skipHintShown() {
        return playing() && !SkipSent && !Crossed && SceneTicks >= SkipHintAfter;
    }

    public static int titleTicks() {
        return TitleTicks;
    }

    public static boolean isScientist(Entity Target) {
        return Phase == PhaseOffice && Target.getId() == EntityId;
    }

    public static int page() {
        return Dialogue == null ? -1 : Dialogue.page();
    }

    public static float pageAge(float Partial) {
        return SceneTicks - PageStart + Partial;
    }

    public static boolean speaking() {
        return Dialogue != null && Dialogue.speaking();
    }

    public static boolean mouthOpen() {
        return Phase == PhaseOffice && Dialogue != null && Dialogue.mouthOpen();
    }

    public static boolean contains(int[] Pages, int Value) {
        for (int Each : Pages) {
            if (Each == Value) {
                return true;
            }
        }
        return false;
    }

    public static void reset() {
        clearScene();
        Phase = PhaseNone;
        AwaitToken = -1;
        Settle = 0;
        Token = -1;
        Veil = 0.0f;
        VeilO = 0.0f;
        VeilTarget = 0.0f;
        VeilWhite = false;
        TitleTicks = -1;
        Stall = 0;
        Black = 0;
        SavedCamera = null;
    }

    private static void clearScene() {
        Dialogue = null;
        LastPage = -1;
        DialogueEnd = -1;
        StandSent = false;
        DoneSent = false;
        SkipHeld = 0;
        SkipHeldO = 0;
        SkipSent = false;
        Crossed = false;
        RisePlayed = false;
        SceneTicks = 0;
        EntityId = -1;
        Gaze = null;
    }

    private static void veil(float Target, int Ticks, boolean White) {
        VeilTarget = Target;
        VeilStep = Ticks <= 0 ? 1.0f : 1.0f / Ticks;
        VeilWhite = White;
        if (Ticks <= 0) {
            Veil = Target;
            VeilO = Target;
        }
    }

    private static void holdCamera() {
        Minecraft Client = Minecraft.getInstance();
        if (SavedCamera == null) {
            SavedCamera = Client.options.getCameraType();
        }
        if (Client.options.getCameraType() != CameraType.FIRST_PERSON) {
            Client.options.setCameraType(CameraType.FIRST_PERSON);
        }
    }

    private static void releaseCamera() {
        Minecraft Client = Minecraft.getInstance();
        if (SavedCamera != null) {
            Client.options.setCameraType(SavedCamera);
            SavedCamera = null;
        }
    }

    public static void await(int NewToken, int Stage) {
        AwaitToken = NewToken;
        AwaitStage = Stage;
        AwaitAge = 0;
        Settle = 0;
        Stall = 0;
        switch (Stage) {
            case IntroDirector.AwaitOffice, IntroDirector.AwaitGate -> {
                clearScene();
                Phase = PhaseEntering;
                holdCamera();
                veil(1.0f, 0, false);
            }
            case IntroDirector.AwaitReturn -> {
                Phase = PhaseReturning;
                Dialogue = null;
                if (Veil < 1.0f) {
                    veil(1.0f, 0, VeilWhite);
                }
            }
            default -> {
                if (Phase != PhaseNone) {
                    clearScene();
                    Phase = PhaseNone;
                    releaseCamera();
                    veil(0.0f, 20, VeilWhite);
                }
            }
        }
    }

    public static void scene(int NewToken, int Scene, int NewEntity, BlockPos NewOrigin) {
        clearScene();
        Token = NewToken;
        EntityId = NewEntity;
        Origin = NewOrigin;
        Stall = 0;
        AwaitToken = -1;
        Veil = 1.0f;
        VeilO = 1.0f;
        veil(0.0f, FadeIn, false);
        holdCamera();
        LocalPlayer User = Minecraft.getInstance().player;
        String Name = User == null ? "?" : User.getGameProfile().getName();
        if (Scene == 1) {
            Phase = PhaseOffice;
            List<Component> Pages = new ArrayList<>();
            for (int Index = 1; Index <= OfficePages; Index++) {
                Pages.add(Component.translatable("intro.scp_unleashed.office." + Index, Name));
            }
            Dialogue = new IntroDialogue(Component.translatable("intro.scp_unleashed.office.plate"), Pages,
                    new IntroVoice(IntroRegistry.Voice.get(), null, null, VoiceVolume, 0.0f, 1.0f));
        } else {
            Phase = PhaseGate;
            Dialogue = new IntroDialogue(Component.translatable("intro.scp_unleashed.gate.plate"), List.of(Component.translatable("intro.scp_unleashed.gate.1", Name)),
                    new IntroVoice(IntroRegistry.Intercom.get(), null, IntroRegistry.IntercomClick.get(), IntercomVolume, 0.0f, 1.0f));
        }
    }

    public static void crossing() {
        if (Crossed || Phase != PhaseGate) {
            return;
        }
        Crossed = true;
        Dialogue = null;
        veil(1.0f, 4, true);
        ui(IntroRegistry.Crossing.get(), 1.0f, 1.0f);
        ui(IntroRegistry.CrossingLayer.get(), 1.0f, 0.8f);
    }

    public static void leaving() {
        Dialogue = null;
        if (!VeilWhite || Veil < 0.5f) {
            veil(1.0f, 6, false);
        } else {
            veil(1.0f, 4, true);
        }
        Phase = PhaseReturning;
        Stall = 0;
    }

    public static void arrived() {
        Phase = PhaseNone;
        AwaitToken = -1;
        releaseCamera();
        veil(0.0f, ArrivalFade, VeilWhite);
        TitleTicks = 0;
        ui(IntroRegistry.Arrival.get(), 1.0f, 0.9f);
    }

    public static void abort() {
        clearScene();
        Phase = PhaseNone;
        AwaitToken = -1;
        releaseCamera();
        veil(0.0f, 12, VeilWhite);
    }

    public static void tick() {
        Minecraft Client = Minecraft.getInstance();
        VeilO = Veil;
        Veil = Veil < VeilTarget ? Math.min(VeilTarget, Veil + VeilStep) : Math.max(VeilTarget, Veil - VeilStep);
        SkipHeldO = SkipHeld;
        if (TitleTicks >= 0 && ++TitleTicks > TitleLength) {
            TitleTicks = -1;
        }
        if (Client.level == null || Client.player == null) {
            return;
        }
        readiness(Client);
        watchdog();
        switch (Phase) {
            case PhaseEntering, PhaseReturning -> {
                if (AwaitToken < 0 && ++Stall > StallLimit) {
                    abort();
                }
            }
            case PhaseOffice -> office(Client);
            case PhaseGate -> gate(Client);
            default -> {
            }
        }
        if (cameraHeld() && Client.options.getCameraType() != CameraType.FIRST_PERSON) {
            Client.options.setCameraType(CameraType.FIRST_PERSON);
        }
        skipping(Client);
        jumping(Client);
    }

    private static void watchdog() {
        Black = Veil >= 0.99f && VeilTarget >= 0.99f ? Black + 1 : 0;
        if (Black > BlackLimit) {
            Black = 0;
            if (Phase == PhaseOffice || Phase == PhaseGate || Phase == PhaseNone) {
                veil(0.0f, 20, VeilWhite);
            }
        }
    }

    private static boolean canSee(Minecraft Client) {
        LocalPlayer User = Client.player;
        if (User == null || Client.level == null || Client.screen != null || Client.getOverlay() != null || !User.isAlive()) {
            return false;
        }
        BlockPos Feet = User.blockPosition();
        if (!Client.level.getChunkSource().hasChunk(Feet.getX() >> 4, Feet.getZ() >> 4)) {
            return false;
        }
        return Client.level.isOutsideBuildHeight(Feet.getY()) || Client.levelRenderer.isSectionCompiled(Feet);
    }

    private static void readiness(Minecraft Client) {
        if (AwaitToken < 0) {
            return;
        }
        AwaitAge++;
        Settle = canSee(Client) ? Settle + 1 : 0;
        boolean Forced = AwaitStage != IntroDirector.AwaitStart && AwaitAge >= ForceReadyAfter && Client.screen == null;
        if (Settle < SettleTicks && !Forced) {
            return;
        }
        boolean Skip = AwaitStage == IntroDirector.AwaitStart && ScpClientConfig.skipIntroCutscene();
        PacketDistributor.sendToServer(new IntroReadyPayload(AwaitToken, Skip));
        if (AwaitStage == IntroDirector.AwaitStart && !Skip) {
            Phase = PhaseEntering;
            holdCamera();
            veil(1.0f, EnterFade, false);
        }
        AwaitToken = -1;
        Settle = 0;
        Stall = 0;
    }

    private static void office(Minecraft Client) {
        SceneTicks++;
        if (SceneTicks == 1 || SceneTicks % 160 == 0) {
            at(IntroRegistry.OfficeHum.get(), Origin.offset(7, 4, 8), 0.5f, 1.0f);
        }
        if (SceneTicks % 20 == 0) {
            at(IntroRegistry.ClockTick.get(), Origin.offset(11, 3, 7), 0.35f, (SceneTicks / 20) % 2 == 0 ? 1.0f : 0.86f);
        }
        if (SceneTicks == 70 || SceneTicks == 560 || SceneTicks == 1180) {
            at(IntroRegistry.DistantDoor.get(), Origin.offset(1 + Random.nextInt(12), 2, 2), 0.6f, 0.9f + Random.nextFloat() * 0.15f);
        }
        if (Dialogue == null) {
            return;
        }
        if (SceneTicks == DialogueAt) {
            Dialogue.begin();
            at(IntroRegistry.Paper.get(), Origin.offset(IntroWorld.FolderSpot), 0.7f, 1.0f);
        }
        Dialogue.tick();
        int Current = Dialogue.page();
        if (Current != LastPage && Current >= 0) {
            LastPage = Current;
            PageStart = SceneTicks;
            if (contains(TapPages, Current)) {
                at(IntroRegistry.FolderTap.get(), Origin.offset(IntroWorld.FolderSpot), 0.8f, 1.0f);
            } else if (Current > 0) {
                at(IntroRegistry.Paper.get(), Origin.offset(IntroWorld.FolderSpot), 0.45f, 1.1f);
            }
            if (Dialogue.lastPage() && !StandSent) {
                StandSent = true;
                send(IntroActionPayload.Stand);
                at(IntroRegistry.Chair.get(), Origin.offset(IntroWorld.ScientistChair), 0.8f, 1.0f);
            }
        }
        if (contains(TapPages, Current) && (SceneTicks - PageStart) % 14 == 7 && SceneTicks - PageStart < 40) {
            at(IntroRegistry.FolderTap.get(), Origin.offset(IntroWorld.FolderSpot), 0.35f, 1.2f);
        }
        if (Dialogue.finished() && DialogueEnd < 0) {
            DialogueEnd = SceneTicks;
            veil(1.0f, FadeOut, false);
        }
        if (DialogueEnd >= 0 && SceneTicks >= DialogueEnd + FadeOut + 6 && !DoneSent) {
            DoneSent = true;
            send(IntroActionPayload.DialogueDone);
        }
    }

    private static Vec3 gateCentre() {
        return IntroWorld.GateField.move(Vec3.atLowerCornerOf(Origin)).getCenter();
    }

    private static void gate(Minecraft Client) {
        SceneTicks++;
        LocalPlayer User = Client.player;
        if (User == null) {
            return;
        }
        Vec3 Room = Vec3.atLowerCornerOf(Origin);
        Vec3 Centre = gateCentre();
        if (SceneTicks == 1 || SceneTicks % 100 == 0) {
            at(IntroRegistry.MachineDrone.get(), BlockPos.containing(Room.add(12.5, 4.0, 18.0)), 1.0f, 1.0f);
        }
        if (SceneTicks == 1 || SceneTicks % 70 == 0) {
            at(IntroRegistry.GateHum.get(), BlockPos.containing(Centre), 1.0f, 1.0f);
        }
        if (SceneTicks > 20 && Random.nextInt(90) == 0) {
            at(Random.nextBoolean() ? IntroRegistry.MachineClank.get() : IntroRegistry.MachineHiss.get(),
                    BlockPos.containing(Room.add(Random.nextBoolean() ? 3.0 : 21.0, 2.0, 12.0 + Random.nextInt(16))), 0.7f, 0.85f + Random.nextFloat() * 0.3f);
        }
        if (Dialogue != null) {
            if (SceneTicks == IntercomAt) {
                Dialogue.begin();
            }
            Dialogue.tick();
            if (Dialogue.finished()) {
                Dialogue = null;
            }
        }
        double Distance = User.position().distanceTo(new Vec3(Centre.x, User.getY(), Centre.z));
        if (!RisePlayed && Distance < 5.0) {
            RisePlayed = true;
            at(IntroRegistry.GateRise.get(), BlockPos.containing(Centre), 1.0f, 1.0f);
        }
        sparks(Client, Room, Centre);
        AABB Field = IntroWorld.GateField.move(Room);
        if (User.getBoundingBox().intersects(Field)) {
            crossing();
        }
    }

    private static void sparks(Minecraft Client, Vec3 Room, Vec3 Centre) {
        if (Client.level == null) {
            return;
        }
        AABB Field = IntroWorld.GateField.move(Room);
        for (int Index = 0; Index < 2; Index++) {
            double X = Mth.lerp(Random.nextDouble(), Field.minX, Field.maxX);
            double Y = Mth.lerp(Random.nextDouble(), Field.minY, Field.maxY);
            Client.level.addParticle(ParticleTypes.PORTAL, X, Y, Centre.z + (Random.nextDouble() - 0.5) * 0.6, (Centre.x - X) * 0.4, (Centre.y - Y) * 0.4, 0.0);
        }
        if (Random.nextInt(6) == 0) {
            double X = Mth.lerp(Random.nextDouble(), Field.minX, Field.maxX);
            Client.level.addParticle(ParticleTypes.END_ROD, X, Field.minY + 0.1, Centre.z + 0.4, 0.0, 0.03 + Random.nextDouble() * 0.03, 0.0);
        }
        if (--NextArc > 0) {
            return;
        }
        NextArc = 30 + Random.nextInt(70);
        boolean Left = Random.nextBoolean();
        double Side = Left ? Field.minX - 0.4 : Field.maxX + 0.4;
        double Start = Field.minY + 1.0 + Random.nextDouble() * (Field.maxY - Field.minY - 2.0);
        Vec3 From = new Vec3(Side, Start, Centre.z);
        Vec3 To = new Vec3(Mth.lerp(0.3 + Random.nextDouble() * 0.4, Field.minX, Field.maxX), Start + (Random.nextDouble() - 0.5) * 2.0, Centre.z);
        int Steps = 10;
        for (int Step = 0; Step <= Steps; Step++) {
            double Share = Step / (double) Steps;
            Vec3 Point = From.lerp(To, Share).add(0.0, (Random.nextDouble() - 0.5) * 0.35, (Random.nextDouble() - 0.5) * 0.2);
            Client.level.addParticle(ParticleTypes.ELECTRIC_SPARK, Point.x, Point.y, Point.z, 0.0, 0.0, 0.0);
        }
        at(IntroRegistry.Spark.get(), BlockPos.containing(From), 0.6f, 0.9f + Random.nextFloat() * 0.3f);
    }

    private static void skipping(Minecraft Client) {
        if (!playing() || SkipSent || Crossed) {
            SkipHeld = 0;
            return;
        }
        if (IntroClientEvents.SkipKey.isDown()) {
            SkipHeld++;
            if (SkipHeld >= SkipHold) {
                SkipSent = true;
                send(IntroActionPayload.Skip);
                ui(IntroRegistry.SkipFill.get(), 1.0f, 0.7f);
                Dialogue = null;
                veil(1.0f, 8, false);
            }
        } else {
            SkipHeld = Math.max(0, SkipHeld - 3);
        }
    }

    private static void jumping(Minecraft Client) {
        boolean Down = Client.options.keyJump.isDown();
        if (Down && !JumpWas) {
            advance();
        }
        JumpWas = Down;
    }

    public static boolean advance() {
        if (!playing() || Dialogue == null || Dialogue.page() < 0) {
            return false;
        }
        Dialogue.advance();
        return true;
    }

    public static void frame(float Delta, float Partial) {
        Minecraft Client = Minecraft.getInstance();
        LocalPlayer User = Client.player;
        if (!cameraHeld() || User == null || Client.level == null) {
            return;
        }
        float Time = SceneTicks + Partial;
        float Yaw;
        float Pitch;
        if (Phase == PhaseOffice || Phase == PhaseEntering && Gaze == null && AwaitStage == IntroDirector.AwaitOffice) {
            Vec3 Eye = User.getEyePosition(Partial);
            Vec3 Want = lookTarget(Client, Partial);
            if (Gaze == null) {
                Gaze = Want;
            } else {
                float Ease = 1.0f - (float) Math.exp(-GazeRate * Math.max(0.0f, Delta));
                Gaze = Gaze.lerp(Want, Ease);
            }
            float FaceYaw = yawTo(Eye, Gaze);
            float FacePitch = pitchTo(Eye, Gaze);
            float Share = Mth.clamp((Time - LookUpAt) / LookUpTicks, 0.0f, 1.0f);
            Share = Share * Share * (3.0f - 2.0f * Share);
            float TableYaw = IntroWorld.officeYaw();
            Yaw = TableYaw + Mth.wrapDegrees(FaceYaw - TableYaw) * Share;
            Pitch = Mth.lerp(Share, IntroWorld.OfficePitch, FacePitch);
        } else if (Phase == PhaseGate) {
            Vec3 Eye = User.getEyePosition(Partial);
            Vec3 Centre = gateCentre();
            Yaw = yawTo(Eye, Centre);
            Pitch = Mth.clamp(pitchTo(Eye, Centre), -GatePitchLimit, GatePitchLimit);
        } else {
            return;
        }
        Yaw += Mth.sin(Time * 0.031f) * BreathYaw;
        Pitch += Mth.sin(Time * 0.052f + 1.3f) * BreathPitch;
        pose(User, Yaw, Pitch);
    }

    private static void pose(LocalPlayer User, float Yaw, float Pitch) {
        float Clamped = Mth.clamp(Pitch, -90.0f, 90.0f);
        User.setYRot(Yaw);
        User.yRotO = Yaw;
        User.setXRot(Clamped);
        User.xRotO = Clamped;
        User.setYHeadRot(Yaw);
        User.yHeadRotO = Yaw;
        if (!User.isPassenger()) {
            User.setYBodyRot(Yaw);
            User.yBodyRotO = Yaw;
        }
    }

    private static float yawTo(Vec3 From, Vec3 To) {
        return (float) (Mth.atan2(To.z - From.z, To.x - From.x) * Mth.RAD_TO_DEG) - 90.0f;
    }

    private static float pitchTo(Vec3 From, Vec3 To) {
        double Dx = To.x - From.x;
        double Dz = To.z - From.z;
        return (float) (-(Mth.atan2(To.y - From.y, Math.sqrt(Dx * Dx + Dz * Dz)) * Mth.RAD_TO_DEG));
    }

    public static Vec3 lookTarget(Minecraft Client, float Partial) {
        Entity Found = Client.level == null || EntityId < 0 ? null : Client.level.getEntity(EntityId);
        if (Found != null) {
            return Found.getEyePosition(Partial).add(0.0, -0.05, 0.0);
        }
        BlockPos Chair = Origin.offset(IntroWorld.ScientistChair);
        return new Vec3(Chair.getX() + 0.5, Chair.getY() + 1.45, Chair.getZ() + 0.5);
    }

    private static void send(int Action) {
        if (Token >= 0) {
            PacketDistributor.sendToServer(new IntroActionPayload(Token, Action));
        }
    }

    public static void ui(SoundEvent Sound, float Pitch, float Volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(Sound, Pitch, Volume));
    }

    public static void at(SoundEvent Sound, BlockPos Pos, float Volume, float Pitch) {
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(Sound, SoundSource.AMBIENT, Volume, Pitch, Random, Pos.getX() + 0.5, Pos.getY() + 0.5,
                Pos.getZ() + 0.5));
    }
}
