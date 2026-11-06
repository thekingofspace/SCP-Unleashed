package net.rai.scp_unleashed.scp087;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.scp268.Scp268;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class Scp087Encounters {
    public static final int FaceStart = 10;
    public static final int LaughStart = 20;
    public static final int HunterStart = 36;
    public static final int FaceFirstDelay = 200;
    public static final int AmbienceFirstDelay = 300;
    public static final int FaceCooldown = 240;
    public static final int FaceRetry = 300;
    public static final int HunterGrace = 200;
    public static final int HunterRetry = 40;
    public static final int HunterRespawn = 600;
    public static final double FaceLift = 1.45;
    public static final double ViewCone = 0.55;
    public static final float[] FaceChance = {0.0f, 0.3f, 0.55f, 0.8f};
    public static final double[] FaceNear = {0.0, 11.0, 7.0, 4.5};
    public static final double[] FaceFar = {0.0, 16.0, 12.0, 8.5};
    public static final float[] VanishRange = {0.0f, 8.5f, 6.0f, 3.5f};
    public static final float[] LaughChance = {0.0f, 0.0f, 0.5f, 0.85f};
    public static final int[] AmbienceMin = {0, 0, 220, 120};
    public static final int[] AmbienceSpread = {0, 0, 300, 180};
    public static final double HunterNear = 12.0;
    public static final double HunterFar = 22.0;

    private Scp087Encounters() {
    }

    public static int band(int Flights) {
        if (Flights >= HunterStart) {
            return 3;
        }
        if (Flights >= LaughStart) {
            return 2;
        }
        return Flights >= FaceStart ? 1 : 0;
    }

    public static void tickMember(ServerLevel World, Scp087Instance Instance, Scp087Visit Visit, ServerPlayer User, long Now) {
        int Band = band(Visit.Flights);
        RandomSource Random = User.getRandom();
        boolean Fresh = Visit.Flights > Visit.Deepest;
        if (Fresh) {
            Visit.Deepest = Visit.Flights;
        }
        if (Visit.Face != null && !(World.getEntity(Visit.Face) instanceof Scp087Face Live && Live.isAlive())) {
            Visit.Face = null;
        }
        if (Band >= 1 && Visit.Face == null && Now >= Visit.NextFace && !Scp268.isNeglected(User) && Targeting.canWitness(User)) {
            boolean Roll = Fresh ? Random.nextFloat() < FaceChance[Band] : Now >= Visit.NextFace + FaceRetry && Random.nextFloat() < FaceChance[Band] * 0.02f;
            if (Roll && face(World, Instance, Visit, User, Band)) {
                Visit.NextFace = Now + FaceCooldown;
            }
        }
        if (Band >= 2 && Now >= Visit.NextAmbience) {
            ambience(World, Instance, User, Band);
            Visit.NextAmbience = Now + AmbienceMin[Band] + Random.nextInt(AmbienceSpread[Band] + 1);
        }
    }

    public static void tickInstance(ServerLevel World, Scp087Instance Instance, List<ServerPlayer> Members, long Now) {
        if (Instance.Hunter != null) {
            Entity Found = World.getEntity(Instance.Hunter);
            if (!(Found instanceof Scp087Hunter Hunter) || Found.isRemoved()) {
                Instance.Hunter = null;
                Instance.HunterReady = Math.max(Instance.HunterReady, Now + HunterRespawn);
            } else if (!Hunter.isAlive()) {
                return;
            }
        }
        if (Instance.Hunter != null || Now < Instance.HunterReady || World.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        ServerPlayer Deepest = null;
        int Most = -1;
        for (ServerPlayer User : Members) {
            Scp087Visit Visit = Scp087Descent.visitOf(User);
            if (Visit != null && Visit.Flights > Most && !User.isCreative() && !User.isSpectator() && !Scp268.isNeglected(User) && Targeting.isTargetable(User)) {
                Most = Visit.Flights;
                Deepest = User;
            }
        }
        if (Deepest == null || Most < HunterStart) {
            return;
        }
        Scp087Hunter Spawned = hunter(World, Instance, Deepest);
        if (Spawned == null) {
            Instance.HunterReady = Now + HunterRetry;
            return;
        }
        Instance.Hunter = Spawned.getUUID();
        Scp087Sounds.laugh(World, Spawned.position().add(0.0, 2.0, 0.0), 1.4f);
    }

    private static boolean standable(ServerLevel World, BlockPos Cell, int Clearance, boolean Landings) {
        BlockState Below = World.getBlockState(Cell.below());
        boolean Stair = Below.getBlock() instanceof Scp087StairBlock;
        if (Stair && Landings || !Stair && !Below.is(Scp087Registry.Floor.get())) {
            return false;
        }
        for (int Up = 0; Up < Clearance; Up++) {
            BlockState Here = World.getBlockState(Cell.above(Up));
            if (!Here.isAir() && !(Here.getBlock() instanceof Scp087RailBlock)) {
                return false;
            }
        }
        return true;
    }

    private static double floorOf(ServerLevel World, BlockPos Cell) {
        return World.getBlockState(Cell.below()).getBlock() instanceof Scp087StairBlock ? Cell.getY() - 0.25 : Cell.getY();
    }

    private static boolean sees(ServerLevel World, Vec3 From, Vec3 To, Entity Viewer) {
        return World.clip(new ClipContext(From, To, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, Viewer)).getType() == HitResult.Type.MISS;
    }

    private static List<Vec3> cells(ServerLevel World, Scp087Instance Instance, ServerPlayer User, double Near, double Far, int Clearance, double Lift) {
        return cells(World, Instance, User, Near, Far, Clearance, Lift, false);
    }

    private static List<Vec3> cells(ServerLevel World, Scp087Instance Instance, ServerPlayer User, double Near, double Far, int Clearance, double Lift, boolean Landings) {
        List<Vec3> Found = new ArrayList<>();
        int OriginX = Instance.originX();
        int Feet = User.getBlockY();
        Vec3 Eye = User.getEyePosition();
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int X = 1; X <= 5; X++) {
            for (int Z = 1; Z < Scp087Layout.SouthWall; Z++) {
                for (int Y = Feet - Mth.ceil(Far) - 1; Y <= Feet; Y++) {
                    Cursor.set(OriginX + X, Y, Z);
                    if (!standable(World, Cursor, Clearance, Landings)) {
                        continue;
                    }
                    Vec3 Spot = new Vec3(OriginX + X + 0.5, floorOf(World, Cursor) + Lift, Z + 0.5);
                    double Gap = Spot.distanceTo(Eye);
                    if (Gap >= Near && Gap <= Far) {
                        Found.add(Spot);
                    }
                }
            }
        }
        return Found;
    }

    private static boolean face(ServerLevel World, Scp087Instance Instance, Scp087Visit Visit, ServerPlayer User, int Band) {
        Vec3 Eye = User.getEyePosition();
        Vec3 Look = User.getLookAngle();
        List<Vec3> Ahead = new ArrayList<>();
        List<Vec3> Aside = new ArrayList<>();
        for (Vec3 Spot : cells(World, Instance, User, FaceNear[Band], FaceFar[Band], 2, FaceLift)) {
            if (Spot.y > Eye.y - 1.0 || !sees(World, Eye, Spot, User)) {
                continue;
            }
            (Spot.subtract(Eye).normalize().dot(Look) > ViewCone ? Ahead : Aside).add(Spot);
        }
        List<Vec3> Pool = Ahead.isEmpty() ? Aside : Ahead;
        if (Pool.isEmpty()) {
            return false;
        }
        Vec3 Spot = Pool.get(User.getRandom().nextInt(Pool.size()));
        Scp087Face Face = Scp087Registry.Face.get().create(World);
        if (Face == null) {
            return false;
        }
        Face.moveTo(Spot.x, Spot.y - Scp087Face.Height / 2.0, Spot.z, yawToward(Spot, Eye), 0.0f);
        Face.setup(Instance.Token, User.getUUID(), VanishRange[Band], LaughChance[Band]);
        World.addFreshEntity(Face);
        Visit.Face = Face.getUUID();
        return true;
    }

    public static float yawToward(Vec3 From, Vec3 To) {
        return (float) (Mth.atan2(To.z - From.z, To.x - From.x) * Mth.RAD_TO_DEG) - 90.0f;
    }

    @Nullable
    private static Scp087Hunter hunter(ServerLevel World, Scp087Instance Instance, ServerPlayer Prey) {
        Vec3 Eye = Prey.getEyePosition();
        List<Vec3> Hidden = new ArrayList<>();
        for (Vec3 Spot : cells(World, Instance, Prey, HunterNear, HunterFar, 3, 0.0, true)) {
            if (Spot.y > Prey.getY() - 6.0) {
                continue;
            }
            if (!sees(World, Eye, Spot.add(0.0, 2.4, 0.0), Prey) && !sees(World, Eye, Spot.add(0.0, 1.0, 0.0), Prey)) {
                Hidden.add(Spot);
            }
        }
        if (Hidden.isEmpty()) {
            return null;
        }
        Vec3 Spot = Hidden.get(Prey.getRandom().nextInt(Hidden.size()));
        Scp087Hunter Hunter = Scp087Registry.Hunter.get().create(World);
        if (Hunter == null) {
            return null;
        }
        Hunter.moveTo(Spot.x, Spot.y, Spot.z, yawToward(Spot, Prey.position()), 0.0f);
        Hunter.bind(Instance.Token);
        Hunter.setTarget(Prey);
        World.addFreshEntity(Hunter);
        return Hunter;
    }

    @Nullable
    public static Scp087Face forceFace(ServerLevel World, ServerPlayer User) {
        Scp087Visit Visit = Scp087Descent.visitOf(User);
        if (Visit == null) {
            return null;
        }
        if (Visit.Face != null && World.getEntity(Visit.Face) instanceof Scp087Face Old) {
            Old.discard();
        }
        Visit.Face = null;
        int Band = Math.max(1, band(Visit.Flights));
        if (!face(World, Visit.Instance, Visit, User, Band)) {
            return null;
        }
        return Visit.Face != null && World.getEntity(Visit.Face) instanceof Scp087Face Made ? Made : null;
    }

    @Nullable
    public static Scp087Hunter forceHunter(ServerLevel World, ServerPlayer User) {
        Scp087Visit Visit = Scp087Descent.visitOf(User);
        if (Visit == null) {
            return null;
        }
        Scp087Instance Instance = Visit.Instance;
        if (Instance.Hunter != null && World.getEntity(Instance.Hunter) instanceof Scp087Hunter Old) {
            Old.discard();
        }
        Scp087Hunter Made = hunter(World, Instance, User);
        if (Made == null) {
            return null;
        }
        Instance.Hunter = Made.getUUID();
        return Made;
    }

    private static Vec3 below(ServerLevel World, Scp087Instance Instance, ServerPlayer User, double Near, double Far) {
        List<Vec3> Spots = cells(World, Instance, User, Near, Far, 2, 1.0);
        Spots.removeIf(Spot -> Spot.y > User.getY() - 3.0);
        if (Spots.isEmpty()) {
            return User.position().add(0.0, -Near, 0.0);
        }
        return Spots.get(User.getRandom().nextInt(Spots.size()));
    }

    private static void ambience(ServerLevel World, Scp087Instance Instance, ServerPlayer User, int Band) {
        RandomSource Random = User.getRandom();
        float Roll = Random.nextFloat();
        if (Roll < 0.5f) {
            Vec3 Start = below(World, Instance, User, 9.0, 16.0);
            int Steps = 3 + Random.nextInt(4);
            int Gap = 7 + Random.nextInt(4);
            float Pitch = 0.85f + Random.nextFloat() * 0.15f;
            for (int Step = 0; Step < Steps; Step++) {
                Vec3 Pos = Start.add((Random.nextDouble() - 0.5) * 0.6, -0.4 * Step, (Random.nextDouble() - 0.5) * 0.6);
                Scp087Sounds.later(World, Step * Gap, Pos, Scp087Registry.Footsteps, SoundSource.AMBIENT, 1.1f - Step * 0.08f, Pitch);
            }
        } else if (Roll < 0.75f) {
            Vec3 Behind = User.position().subtract(Vec3.directionFromRotation(0.0f, User.getYRot()).scale(2.5)).add(0.0, 1.4, 0.0);
            Scp087Sounds.play(World, Behind, Scp087Registry.Breath, SoundSource.AMBIENT, Band >= 3 ? 0.55f : 0.35f, 0.9f + Random.nextFloat() * 0.1f);
        } else {
            Scp087Sounds.laugh(World, below(World, Instance, User, 10.0, 18.0), Band >= 3 ? 1.3f : 1.0f);
        }
    }
}
