package net.rai.scp_unleashed.scp4666;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp4666.block.Scp4666FootprintsBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666WindowBlock;
import net.rai.scp_unleashed.scp4666.network.Scp4666FxPayload;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class Scp4666Happenings {
    public static final double ChimeDistance = 38.0;
    public static final double WhistleDistance = 34.0;
    public static final int RoofStepTicks = 16;
    public static final int StairTicks = 22;
    public static final double WindowReach = 10.0;
    public static final double AtticReach = 6.0;
    public static final double StareBack = 1.1;
    public static final double StareStep = 0.25;
    public static final double StareFarthest = 2.85;

    private Scp4666Happenings() {
    }

    @Nullable
    private static ServerPlayer pick(ServerLevel World, RandomSource Random, boolean Indoors, Scp4666Map Layout) {
        List<ServerPlayer> Found = new ArrayList<>();
        for (ServerPlayer User : Scp4666Director.awake(World)) {
            if (!Indoors || Layout.insideCabin(User.position()) && !Layout.inBasement(User.position())) {
                Found.add(User);
            }
        }
        return Found.isEmpty() ? null : Found.get(Random.nextInt(Found.size()));
    }

    private static Vec3 around(Vec3 Centre, double Distance, RandomSource Random) {
        double Angle = Random.nextDouble() * Math.PI * 2.0;
        return Centre.add(Math.cos(Angle) * Distance, 3.0, Math.sin(Angle) * Distance);
    }

    public static boolean chimes(ServerLevel World, Scp4666Map Layout, RandomSource Random) {
        ServerPlayer User = pick(World, Random, false, Layout);
        if (User == null) {
            return false;
        }
        Vec3 Spot = around(User.position(), ChimeDistance, Random);
        int Count = 3 + Random.nextInt(3);
        for (int Index = 0; Index < Count; Index++) {
            float Pitch = 1.4f + Random.nextFloat() * 0.4f - Index * 0.05f;
            Scp4666Cues.after(World, Index * 7, Level -> Scp4666Cues.sound(Level, Spot, List.of(Scp4666Registry.Chime, Scp4666Registry.ChimeBell), 3.0f, Pitch));
        }
        return true;
    }

    public static boolean whistle(ServerLevel World, Scp4666Map Layout, RandomSource Random) {
        ServerPlayer User = pick(World, Random, false, Layout);
        if (User == null) {
            return false;
        }
        Vec3 Spot = around(User.position(), WhistleDistance, Random);
        World.playSound(null, Spot.x, Spot.y, Spot.z, Scp4666Registry.WhistlePhrases.get(Random.nextInt(Scp4666Registry.WhistlePhrases.size())).get(), SoundSource.AMBIENT, 2.8f, 0.98f + Random.nextFloat() * 0.04f);
        return true;
    }

    public static boolean watcher(ServerLevel World, Scp4666Map Layout, RandomSource Random) {
        if (Scp4666YuleMan.present(World)) {
            return false;
        }
        ServerPlayer User = pick(World, Random, false, Layout);
        if (User == null) {
            return false;
        }
        double Facing = Math.toRadians(User.getYRot() + 90.0) + (Random.nextDouble() - 0.5) * Math.toRadians(100.0);
        Vec3 Toward = User.position().add(Math.cos(Facing) * 40.0, 0.0, Math.sin(Facing) * 40.0);
        double Angle = Math.atan2(Toward.z - (Layout.Centre.getZ() + 0.5), Toward.x - (Layout.Centre.getX() + 0.5));
        Vec3 Spot = Scp4666Director.ringPoint(Layout, Angle, Scp4666World.StalkRadius);
        Scp4666YuleMan Man = spawn(World, Spot, 0.0f);
        if (Man == null) {
            return false;
        }
        Man.stalk(Angle, Random.nextBoolean());
        return true;
    }

    @Nullable
    public static Scp4666YuleMan spawn(ServerLevel World, Vec3 Spot, float Yaw) {
        Scp4666YuleMan Man = Scp4666Registry.YuleMan.get().create(World);
        if (Man == null) {
            return null;
        }
        Man.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Man.setYHeadRot(Yaw);
        Man.setYBodyRot(Yaw);
        Man.finalizeSpawn(World, World.getCurrentDifficultyAt(Man.blockPosition()), MobSpawnType.EVENT, null);
        World.addFreshEntity(Man);
        return Man;
    }

    public static boolean roof(ServerLevel World, Scp4666Map Layout, RandomSource Random) {
        ServerPlayer User = pick(World, Random, true, Layout);
        if (User == null || Layout.RoofPath.isEmpty()) {
            return false;
        }
        int Start = nearest(Layout.RoofPath, User.position());
        int Direction = Random.nextBoolean() ? 1 : -1;
        int Steps = 8 + Random.nextInt(6);
        int Delay = 0;
        for (int Index = 0; Index < Steps; Index++) {
            int Spot = Math.floorMod(Start + Direction * Index, Layout.RoofPath.size());
            Vec3 Point = Layout.RoofPath.get(Spot);
            float Pitch = 0.62f + Random.nextFloat() * 0.12f;
            Scp4666Cues.after(World, Delay, Level -> Scp4666Cues.sound(Level, Point, List.of(Scp4666Registry.RoofStep, Scp4666Registry.RoofSnow), 1.4f, Pitch));
            Delay += RoofStepTicks + Random.nextInt(8) + (Index == Steps / 2 ? 50 : 0);
        }
        Vec3 Last = Layout.RoofPath.get(Math.floorMod(Start + Direction * Steps, Layout.RoofPath.size()));
        Scp4666Cues.after(World, Delay + 20, Level -> Scp4666Cues.sound(Level, Last, List.of(Scp4666Registry.Creak, Scp4666Registry.CreakLow), 1.2f, 0.7f));
        return true;
    }

    private static int nearest(List<Vec3> Points, Vec3 Target) {
        int Best = 0;
        double Distance = Double.MAX_VALUE;
        for (int Index = 0; Index < Points.size(); Index++) {
            double Gap = Points.get(Index).distanceToSqr(Target.x, Points.get(Index).y, Target.z);
            if (Gap < Distance) {
                Distance = Gap;
                Best = Index;
            }
        }
        return Best;
    }

    public static boolean gramophone(ServerLevel World, Scp4666Map Layout, RandomSource Random) {
        if (pick(World, Random, true, Layout) == null) {
            return false;
        }
        Scp4666Director.gramophone(World, Layout, true);
        return true;
    }

    @Nullable
    private static Scp4666Map.Window windowNear(Scp4666Map Layout, ServerPlayer User) {
        Scp4666State State = Scp4666State.get(User.server);
        Scp4666Map.Window Best = null;
        double BestDistance = WindowReach * WindowReach;
        for (int Index = 0; Index < Layout.Windows.size(); Index++) {
            Scp4666Map.Window Entry = Layout.Windows.get(Index);
            double Distance = Entry.glass().distanceToSqr(User.getEyePosition());
            if (Scp4666Chores.open(State, Index) && Distance < BestDistance) {
                BestDistance = Distance;
                Best = Entry;
            }
        }
        return Best;
    }

    public static boolean breathOnGlass(ServerLevel World, Scp4666Map Layout, RandomSource Random) {
        ServerPlayer User = pick(World, Random, true, Layout);
        if (User == null) {
            return false;
        }
        Scp4666Map.Window Glass = windowNear(Layout, User);
        if (Glass == null) {
            return false;
        }
        Vec3 Out = Glass.glass().add(Glass.Out().getStepX() * 0.6, 0.0, Glass.Out().getStepZ() * 0.6);
        Scp4666Cues.sound(World, Out, List.of(Scp4666Registry.GlassBreath, Scp4666Registry.BreathLow), 0.9f, 0.8f);
        for (ServerPlayer Near : World.players()) {
            if (Near.distanceToSqr(Out) < 24.0 * 24.0) {
                PacketDistributor.sendToPlayer(Near, new Scp4666FxPayload(Scp4666FxPayload.Breath, Out.x, Out.y, Out.z, Glass.Out().get3DDataValue() + Glass.Width() * 8));
            }
        }
        return true;
    }

    public static boolean attic(ServerLevel World, Scp4666Map Layout, RandomSource Random) {
        ServerPlayer Below = null;
        for (ServerPlayer User : Scp4666Director.awake(World)) {
            Vec3 Hatch = Vec3.atCenterOf(Layout.AtticHatch);
            double Dx = User.getX() - Hatch.x;
            double Dz = User.getZ() - Hatch.z;
            if (Dx * Dx + Dz * Dz < AtticReach * AtticReach && User.getY() < Hatch.y && User.getY() > Layout.Floor - 0.5) {
                Below = User;
            }
        }
        if (Below == null || Scp4666YuleMan.present(World)) {
            return false;
        }
        Scp4666Director.setHatch(World, Layout.AtticHatch, true);
        Vec3 Spot = Vec3.atBottomCenterOf(Layout.AtticHatch).add(0.0, 1.0, 0.0);
        float Yaw = (float) (Mth.atan2(Below.getZ() - Spot.z, Below.getX() - Spot.x) * Mth.RAD_TO_DEG) - 90.0f;
        Scp4666YuleMan Man = spawn(World, Spot, Yaw);
        if (Man == null) {
            return false;
        }
        Man.peek(Below);
        World.playSound(null, Spot.x, Spot.y, Spot.z, Scp4666Registry.CreakLow.get(), SoundSource.HOSTILE, 0.6f, 0.8f);
        Scp4666Cues.after(World, Scp4666YuleMan.PeekTicks + 4, Level -> {
            Scp4666Director.setHatch(Level, Layout.AtticHatch, false);
            Level.playSound(null, Spot.x, Spot.y, Spot.z, Scp4666Registry.DoorCreak.get(), SoundSource.BLOCKS, 0.9f, 1.3f);
        });
        return true;
    }

    public static boolean stairs(ServerLevel World, Scp4666Map Layout, RandomSource Random) {
        if (pick(World, Random, true, Layout) == null || Layout.BasementStairs.isEmpty()) {
            return false;
        }
        int Delay = 0;
        for (int Index = 0; Index < Layout.BasementStairs.size(); Index++) {
            Vec3 Step = Vec3.atCenterOf(Layout.BasementStairs.get(Index));
            float Pitch = 0.55f + Index * 0.03f;
            Scp4666Cues.after(World, Delay, Level -> Scp4666Cues.sound(Level, Step, List.of(Scp4666Registry.CreakLow, Scp4666Registry.Creak), 0.9f, Pitch));
            Delay += StairTicks + Random.nextInt(10);
        }
        return true;
    }

    public static boolean snowInside(ServerLevel World, Scp4666Map Layout, RandomSource Random) {
        ServerPlayer User = pick(World, Random, true, Layout);
        if (User == null) {
            return false;
        }
        Scp4666Map.Window Glass = windowNear(Layout, User);
        if (Glass == null) {
            return false;
        }
        Vec3 In = Glass.glass().add(-Glass.Out().getStepX() * 0.8, 0.6, -Glass.Out().getStepZ() * 0.8);
        World.playSound(null, In.x, In.y, In.z, Scp4666Registry.WindGust.get(), SoundSource.AMBIENT, 0.8f, 1.3f);
        Scp4666Chores.crack(World, Layout, Scp4666State.get(World.getServer()), Layout.Windows.indexOf(Glass));
        for (ServerPlayer Near : World.players()) {
            if (Near.distanceToSqr(In) < 24.0 * 24.0) {
                PacketDistributor.sendToPlayer(Near, new Scp4666FxPayload(Scp4666FxPayload.Snow, In.x, In.y, In.z, 200));
            }
        }
        return true;
    }

    public static boolean hum(ServerLevel World, Scp4666Map Layout, RandomSource Random) {
        Vec3 Fire = Vec3.atCenterOf(Layout.Fireplace).add(0.0, 1.0, 0.0);
        boolean Near = false;
        for (ServerPlayer User : Scp4666Director.awake(World)) {
            if (User.distanceToSqr(Fire) < 16.0 * 16.0) {
                Near = true;
            }
        }
        if (!Near) {
            return false;
        }
        World.playSound(null, Fire.x, Fire.y, Fire.z, Scp4666Registry.HumPhrases.get(Random.nextInt(Scp4666Registry.HumPhrases.size())).get(), SoundSource.AMBIENT, 0.45f, 1.0f);
        return true;
    }

    public static boolean knock(ServerLevel World, Scp4666Map Layout, RandomSource Random) {
        if (pick(World, Random, true, Layout) == null) {
            return false;
        }
        BlockPos Door = Random.nextBoolean() ? Layout.FrontDoor : Layout.SideDoor;
        Vec3 Spot = Vec3.atCenterOf(Door).add(0.0, 0.5, 0.0);
        int Rounds = 1 + Random.nextInt(2);
        int Delay = 0;
        for (int Round = 0; Round < Rounds; Round++) {
            for (int Index = 0; Index < 3; Index++) {
                float Pitch = 0.8f + Random.nextFloat() * 0.1f;
                Scp4666Cues.after(World, Delay, Level -> Scp4666Cues.sound(Level, Spot, List.of(Scp4666Registry.Knock, Scp4666Registry.KnockHeavy), 1.6f, Pitch));
                Delay += 9 + Random.nextInt(3);
            }
            Delay += 60 + Random.nextInt(60);
        }
        return true;
    }

    public static boolean basementBreath(ServerLevel World, Scp4666Map Layout, RandomSource Random) {
        for (ServerPlayer User : Scp4666World.living(World)) {
            if (Layout.inBasement(User.position())) {
                return false;
            }
        }
        if (pick(World, Random, true, Layout) == null) {
            return false;
        }
        BlockState Cellar = World.getBlockState(Layout.CellarDoor);
        boolean Shut = Cellar.getBlock() instanceof DoorBlock && !Cellar.getValue(DoorBlock.OPEN);
        Vec3 Spot = Vec3.atCenterOf(Shut ? Layout.CellarBehind : Layout.BasementCentre);
        for (int Index = 0; Index < 6; Index++) {
            float Pitch = 0.62f + Random.nextFloat() * 0.06f;
            Scp4666Cues.after(World, Index * 52, Level -> {
                for (ServerPlayer User : Scp4666World.living(Level)) {
                    if (Layout.inBasement(User.position())) {
                        return;
                    }
                }
                Scp4666Cues.sound(Level, Spot, List.of(Scp4666Registry.Breath, Scp4666Registry.BreathLow), 1.1f, Pitch);
            });
        }
        return true;
    }

    public static boolean treeOut(ServerLevel World, Scp4666Map Layout, RandomSource Random) {
        Scp4666State State = Scp4666State.get(World.getServer());
        if (State.tree() <= 0) {
            return false;
        }
        int Lights = State.tree();
        for (int Index = 1; Index <= Lights; Index++) {
            int Left = Lights - Index;
            Scp4666Cues.after(World, Index * 30, Level -> {
                Scp4666State Now = Scp4666State.get(Level.getServer());
                if (Now.tree() > Left) {
                    Scp4666Director.setTree(Level, Layout, Now, Left);
                    Vec3 Tree = Vec3.atCenterOf(Layout.Tree).add(0.0, 1.0, 0.0);
                    Level.playSound(null, Tree.x, Tree.y, Tree.z, Scp4666Registry.Twinkle.get(), SoundSource.BLOCKS, 0.7f, 0.6f + Left * 0.1f);
                }
            });
        }
        return true;
    }

    public static void windowVisit(ServerLevel World, Scp4666Map Layout, RandomSource Random) {
        if (Scp4666YuleMan.present(World) || Layout.Windows.isEmpty()) {
            return;
        }
        ServerPlayer User = pick(World, Random, true, Layout);
        Scp4666Map.Window Glass = User == null ? null : windowNear(Layout, User);
        Scp4666State State = Scp4666State.get(World.getServer());
        if (Glass == null) {
            List<Scp4666Map.Window> Open = new ArrayList<>();
            for (int Index = 0; Index < Layout.Windows.size(); Index++) {
                if (Scp4666Chores.open(State, Index)) {
                    Open.add(Layout.Windows.get(Index));
                }
            }
            if (Open.isEmpty()) {
                return;
            }
            Glass = Open.get(Random.nextInt(Open.size()));
        }
        Vec3 Spot = windowSpot(World, Layout, Glass);
        float Yaw = Glass.Out().getOpposite().toYRot();
        Scp4666YuleMan Man = spawn(World, Spot, Yaw);
        if (Man == null) {
            return;
        }
        Man.stare(Glass);
        Scp4666Cues.sound(World, Glass.glass(), List.of(Scp4666Registry.Scratch, Scp4666Registry.GlassBreath), 0.7f, 0.9f);
        if (Random.nextFloat() < Scp4666Chores.VisitCrack) {
            int Index = Layout.Windows.indexOf(Glass);
            Scp4666Cues.after(World, 60, Level -> Scp4666Chores.crack(Level, Layout, Scp4666State.get(Level.getServer()), Index));
        }
        Vec3 Pane = Glass.glass();
        Scp4666Cues.after(World, 25, Level -> Level.playSound(null, Pane.x, Pane.y, Pane.z, Scp4666Registry.Whisper.get(), SoundSource.HOSTILE, 0.6f, 0.95f));
    }

    public static boolean sniff(ServerLevel World, Scp4666Map Layout, RandomSource Random) {
        if (pick(World, Random, true, Layout) == null) {
            return false;
        }
        BlockPos Door = Random.nextBoolean() ? Layout.FrontDoor : Layout.SideDoor;
        Vec3 Spot = Vec3.atCenterOf(Door).add(0.0, 0.6, 0.0);
        int Count = 2 + Random.nextInt(2);
        for (int Index = 0; Index < Count; Index++) {
            float Pitch = 0.88f + Random.nextFloat() * 0.1f;
            Scp4666Cues.after(World, Index * 70, Level -> Level.playSound(null, Spot.x, Spot.y, Spot.z, Scp4666Registry.Sniff.get(), SoundSource.HOSTILE, 0.8f, Pitch));
        }
        return true;
    }

    public static double footing(ServerLevel World, double X, double Z, int Top, int Bottom) {
        for (int Y = Top; Y >= Bottom; Y--) {
            BlockPos Pos = BlockPos.containing(X, Y, Z);
            BlockState Found = World.getBlockState(Pos);
            if (fixture(Found)) {
                continue;
            }
            VoxelShape Shape = Found.getCollisionShape(World, Pos);
            if (!Shape.isEmpty()) {
                return Y + Shape.max(Direction.Axis.Y);
            }
        }
        return Bottom + 1.0;
    }

    public static boolean fixture(BlockState Found) {
        Block Kind = Found.getBlock();
        return Kind instanceof TrapDoorBlock || Kind instanceof IronBarsBlock || Kind instanceof Scp4666WindowBlock || Kind instanceof FenceBlock
                || Kind instanceof WallBlock || Kind instanceof DoorBlock;
    }

    public static Vec3 windowSpot(ServerLevel World, Scp4666Map Layout, Scp4666Map.Window Glass) {
        Vec3 Fallback = null;
        for (double Distance = StareBack; Distance <= StareFarthest + 1.0E-6; Distance += StareStep) {
            Vec3 Spot = Glass.outside(Distance);
            Vec3 Feet = new Vec3(Spot.x, footing(World, Spot.x, Spot.z, Glass.Pos().getY(), Layout.Ground), Spot.z);
            if (Fallback == null) {
                Fallback = Feet;
            }
            if (clear(World, Feet)) {
                return Feet;
            }
        }
        return Fallback;
    }

    public static boolean clear(ServerLevel World, Vec3 Feet) {
        AABB Body = Scp4666Registry.YuleMan.get().getDimensions().makeBoundingBox(Feet).deflate(1.0E-3);
        for (BlockPos Pos : BlockPos.betweenClosed(BlockPos.containing(Body.minX, Body.minY, Body.minZ), BlockPos.containing(Body.maxX, Body.maxY, Body.maxZ))) {
            BlockState Found = World.getBlockState(Pos);
            if (Found.isAir()) {
                continue;
            }
            for (AABB Box : Found.getCollisionShape(World, Pos).toAabbs()) {
                if (Box.move(Pos).intersects(Body)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void prints(ServerLevel World, Scp4666Map Layout, Scp4666State State, RandomSource Random) {
        if (Layout.Windows.isEmpty()) {
            return;
        }
        Scp4666Map.Window Glass = Layout.Windows.get(Random.nextInt(Layout.Windows.size()));
        Vec3 End = Glass.outside(1.2);
        Vec3 Away = new Vec3(Glass.Out().getStepX(), 0.0, Glass.Out().getStepZ());
        double Swing = (Random.nextDouble() - 0.5) * 1.2;
        Vec3 Side = new Vec3(-Away.z, 0.0, Away.x);
        Vec3 Start = End.add(Away.scale(18.0)).add(Side.scale(Swing * 10.0));
        int Count = 22;
        Direction Heading = Direction.getNearest(End.x - Start.x, 0.0, End.z - Start.z);
        for (int Index = 0; Index <= Count; Index++) {
            Vec3 Point = Start.lerp(End, (double) Index / Count).add(Side.scale(Index % 2 == 0 ? 0.35 : -0.35));
            BlockPos Pos = BlockPos.containing(Point.x, Layout.Ground + 1, Point.z);
            BlockState Found = World.getBlockState(Pos);
            if (Found.is(Blocks.SNOW) && Found.getValue(SnowLayerBlock.LAYERS) <= 3) {
                World.setBlock(Pos, Scp4666Registry.Footprints.get().defaultBlockState().setValue(Scp4666FootprintsBlock.Facing, Heading), Block.UPDATE_CLIENTS);
                State.addPrint(Pos);
            }
        }
    }

    public static void cellarAjar(ServerLevel World, Scp4666Map Layout) {
        BlockState Found = World.getBlockState(Layout.CellarDoor);
        if (Found.getBlock() instanceof DoorBlock && !Found.getValue(DoorBlock.OPEN)) {
            World.setBlock(Layout.CellarDoor, Found.setValue(DoorBlock.OPEN, true), Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
        }
    }

    public static void doorAjar(ServerLevel World, Scp4666Map Layout) {
        BlockState Found = World.getBlockState(Layout.FrontDoor);
        if (Found.getBlock() instanceof DoorBlock Door && !Found.getValue(DoorBlock.OPEN)) {
            Door.setOpen(null, World, Found, Layout.FrontDoor, true);
            World.playSound(null, Layout.FrontDoor, Scp4666Registry.DoorCreak.get(), SoundSource.BLOCKS, 0.6f, 0.7f);
        }
    }
}
