package net.rai.scp_unleashed.scp001gate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.gateway.GatewayTravel;
import org.jetbrains.annotations.Nullable;

public final class Scp001GateGarden {
    public static final ResourceKey<Level> Key = ResourceKey.create(Registries.DIMENSION, Scp001GateRegistry.location("scp001_garden"));
    public static final String ReturnKey = "scp_unleashed_scp001_gate_return";
    public static final String CooldownKey = "scp_unleashed_scp001_gate_cooldown";
    public static final int StandinBaseX = 3328000;
    public static final int StandinBaseZ = 3328000;
    public static final int Cooldown = 60;
    public static final int TreeAhead = 40;
    public static final int Outside = 58;
    public static final int Ahead = 5;
    public static final int StandinReach = 512;
    private static volatile ResourceKey<Level> Standin;

    private Scp001GateGarden() {
    }

    public static ResourceKey<Level> key() {
        ResourceKey<Level> Borrowed = Standin;
        return Borrowed != null ? Borrowed : Key;
    }

    public static void standIn(@Nullable ResourceKey<Level> Dimension) {
        Standin = Dimension;
    }

    public static boolean standingIn() {
        return Standin != null;
    }

    public static boolean inside(@Nullable Level World) {
        return World != null && World.dimension() == key();
    }

    public static boolean covers(@Nullable Level World, double X, double Z) {
        if (!inside(World)) {
            return false;
        }
        if (!standingIn()) {
            return true;
        }
        BlockPos Origin = origin();
        return Math.abs(X - Origin.getX()) <= StandinReach && Math.abs(Z - Origin.getZ()) <= StandinReach;
    }

    public static boolean barred(MobSpawnType Type) {
        return Type == MobSpawnType.NATURAL || Type == MobSpawnType.CHUNK_GENERATION || Type == MobSpawnType.PATROL || Type == MobSpawnType.EVENT
                || Type == MobSpawnType.STRUCTURE || Type == MobSpawnType.REINFORCEMENT || Type == MobSpawnType.JOCKEY;
    }

    public static BlockPos origin() {
        return standingIn() ? new BlockPos(StandinBaseX, 0, StandinBaseZ) : BlockPos.ZERO;
    }

    @Nullable
    public static ServerLevel level(MinecraftServer Server) {
        return Server.getLevel(key());
    }

    public static boolean cooling(Player User) {
        CompoundTag Persisted = User.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        return Persisted.contains(CooldownKey) && User.level().getGameTime() - Persisted.getLong(CooldownKey) < Cooldown;
    }

    private static void stamp(Player User, long When) {
        CompoundTag Root = User.getPersistentData();
        CompoundTag Persisted = Root.getCompound(Player.PERSISTED_NBT_TAG);
        Persisted.putLong(CooldownKey, When);
        Root.put(Player.PERSISTED_NBT_TAG, Persisted);
    }

    public static void passThrough(ServerPlayer User, ServerLevel From, BlockPos Pos) {
        if (cooling(User) || !(From.getBlockEntity(Pos) instanceof Scp001GatePortalBlockEntity Portal)) {
            return;
        }
        if (Portal.leadsHome()) {
            leave(User);
        } else {
            enter(User, Portal.centre(), Portal.front());
        }
    }

    public static boolean enter(ServerPlayer User, BlockPos Gate, Direction Front) {
        ServerLevel Target = level(User.server);
        if (Target == null || inside(User.level())) {
            return false;
        }
        CompoundTag Root = User.getPersistentData();
        CompoundTag Persisted = Root.getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag Back = new CompoundTag();
        Back.putString("Dimension", User.level().dimension().location().toString());
        Back.putDouble("X", Gate.getX() + 0.5 + Front.getStepX() * Ahead);
        Back.putDouble("Y", Gate.getY());
        Back.putDouble("Z", Gate.getZ() + 0.5 + Front.getStepZ() * Ahead);
        Back.putFloat("Yaw", Front.toYRot());
        Persisted.put(ReturnKey, Back);
        Root.put(Player.PERSISTED_NBT_TAG, Persisted);
        BlockPos Home = prepare(Target);
        Vec3 Spot = new Vec3(Home.getX() + 0.5, Home.getY(), Home.getZ() + 0.5 + Ahead);
        User.level().playSound(null, User.getX(), User.getY(), User.getZ(), Scp001GateRegistry.Pass.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        GatewayTravel.move(User, Target, GatewayTravel.safeSpot(Target, Spot, User.getType()), Direction.SOUTH.toYRot());
        stamp(User, Target.getGameTime());
        Target.playSound(null, Spot.x, Spot.y, Spot.z, Scp001GateRegistry.Pass.get(), SoundSource.PLAYERS, 1.0f, 1.2f);
        return true;
    }

    public static void leave(ServerPlayer User) {
        CompoundTag Persisted = User.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        ServerLevel Target = null;
        Vec3 Spot = null;
        float Yaw = 0.0f;
        if (Persisted.contains(ReturnKey)) {
            CompoundTag Back = Persisted.getCompound(ReturnKey);
            ResourceLocation Id = ResourceLocation.tryParse(Back.getString("Dimension"));
            Target = Id == null ? null : User.server.getLevel(ResourceKey.create(Registries.DIMENSION, Id));
            Spot = new Vec3(Back.getDouble("X"), Back.getDouble("Y"), Back.getDouble("Z"));
            Yaw = Back.getFloat("Yaw");
        }
        if (Target == null || Target.dimension() == key()) {
            Scp001GateSites Site = Scp001GateSites.get(User.server);
            Target = User.server.overworld();
            if (Site.planned()) {
                BlockPos Centre = Site.centre();
                Spot = new Vec3(Centre.getX() + 0.5 + Site.front().getStepX() * Outside, Centre.getY(), Centre.getZ() + 0.5 + Site.front().getStepZ() * Outside);
                Yaw = Site.front().toYRot();
            } else {
                Spot = Vec3.atBottomCenterOf(Target.getSharedSpawnPos());
            }
        }
        CompoundTag Root = User.getPersistentData();
        CompoundTag Kept = Root.getCompound(Player.PERSISTED_NBT_TAG);
        Kept.remove(ReturnKey);
        Root.put(Player.PERSISTED_NBT_TAG, Kept);
        User.level().playSound(null, User.getX(), User.getY(), User.getZ(), Scp001GateRegistry.Pass.get(), SoundSource.PLAYERS, 1.0f, 0.8f);
        GatewayTravel.move(User, Target, GatewayTravel.safeSpot(Target, Spot, User.getType()), Yaw);
        stamp(User, Target.getGameTime());
    }

    public static BlockPos prepare(ServerLevel Garden) {
        Built Data = Garden.getDataStorage().computeIfAbsent(Built.DataFactory, Built.DataName);
        BlockPos Origin = origin();
        Garden.getChunkAt(Origin);
        Garden.getChunkAt(Origin.offset(0, 0, TreeAhead));
        if (Data.Ready) {
            return new BlockPos(Origin.getX(), Data.Ground, Origin.getZ());
        }
        int Ground = Garden.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Origin.getX(), Origin.getZ());
        if (Ground <= Garden.getMinBuildHeight() + 2) {
            Ground = 72;
        }
        BlockPos Centre = new BlockPos(Origin.getX(), Ground, Origin.getZ());
        buildArch(Garden, Centre);
        int TreeGround = Garden.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Origin.getX(), Origin.getZ() + TreeAhead);
        buildTree(Garden, new BlockPos(Origin.getX(), Math.max(TreeGround, Garden.getMinBuildHeight() + 3), Origin.getZ() + TreeAhead));
        Data.Ready = true;
        Data.Ground = Ground;
        Data.setDirty();
        return Centre;
    }

    public static void buildArch(ServerLevel World, BlockPos Centre) {
        Direction Front = Direction.SOUTH;
        long Seed = 4242L;
        for (int X = Scp001GatePlan.MinX; X <= Scp001GatePlan.MaxX; X++) {
            for (int Z = Scp001GatePlan.MinZ; Z <= Scp001GatePlan.MaxZ; Z++) {
                for (int Y = Scp001GatePlan.MinY; Y <= Scp001GatePlan.MaxY; Y++) {
                    Scp001GatePlan.Kind Type = Scp001GatePlan.cell(X, Y, Z, Seed);
                    BlockPos At = Scp001GatePlan.toWorld(Centre, Front, X, Y, Z);
                    if (Type == null) {
                        if (Y >= 0 && Math.abs(X) <= Scp001GatePlan.FrameHalf + 1 && Math.abs(Z) <= Scp001GatePlan.Depth + 1) {
                            World.setBlock(At, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                        }
                        continue;
                    }
                    World.setBlock(At, Scp001GatePlan.state(Type, Front), Block.UPDATE_CLIENTS);
                    if (Type == Scp001GatePlan.Kind.Portal && World.getBlockEntity(At) instanceof Scp001GatePortalBlockEntity Portal) {
                        Portal.bind(Centre, Front);
                        Portal.home(true);
                    }
                    if (Y == -1) {
                        for (int Down = 2; Down <= 6; Down++) {
                            BlockPos Fill = At.below(Down - 1);
                            if (World.getBlockState(Fill).canBeReplaced()) {
                                World.setBlock(Fill, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
                            }
                        }
                    }
                }
            }
        }
    }

    public static void buildTree(ServerLevel World, BlockPos Base) {
        BlockState Log = Blocks.OAK_LOG.defaultBlockState();
        BlockState Leaf = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        int Trunk = 18;
        for (int Y = -2; Y < Trunk; Y++) {
            for (int Dx = -1; Dx <= 1; Dx++) {
                for (int Dz = -1; Dz <= 1; Dz++) {
                    if (Y > Trunk - 4 && Math.abs(Dx) + Math.abs(Dz) == 2) {
                        continue;
                    }
                    World.setBlock(Base.offset(Dx, Y, Dz), Log, Block.UPDATE_CLIENTS);
                }
            }
        }
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            for (int Step = 2; Step <= 4; Step++) {
                World.setBlock(Base.relative(Side, Step).below(Step == 4 ? 1 : 0), Log.setValue(RotatedPillarBlock.AXIS, Side.getAxis()), Block.UPDATE_CLIENTS);
            }
        }
        int[][] Arms = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}};
        for (int Index = 0; Index < Arms.length; Index++) {
            int Rise = 11 + Index % 3 * 2;
            int Length = Index < 4 ? 7 : 5;
            BlockPos End = Base.above(Rise);
            for (int Step = 1; Step <= Length; Step++) {
                End = Base.offset(Arms[Index][0] * Step, Rise + Step / 2, Arms[Index][1] * Step);
                World.setBlock(End, Log.setValue(RotatedPillarBlock.AXIS, Arms[Index][0] != 0 && Arms[Index][1] == 0 ? Direction.Axis.X : Direction.Axis.Z), Block.UPDATE_CLIENTS);
            }
            blob(World, End.above(1), 4 + Index % 2, 3, Leaf);
        }
        blob(World, Base.above(Trunk + 2), 8, 5, Leaf);
        blob(World, Base.above(Trunk - 2), 10, 4, Leaf);
    }

    private static void blob(ServerLevel World, BlockPos Centre, int Radius, int Tall, BlockState Leaf) {
        for (int Dx = -Radius; Dx <= Radius; Dx++) {
            for (int Dz = -Radius; Dz <= Radius; Dz++) {
                for (int Dy = -Tall; Dy <= Tall; Dy++) {
                    double Shape = (Dx * Dx + Dz * Dz) / (double) (Radius * Radius) + (Dy * Dy) / (double) (Tall * Tall);
                    if (Shape > 1.0 || Scp001GatePlan.mix(Centre.getX() + Dx, Centre.getY() + Dy, Centre.getZ() + Dz, 77L) < 0.06 * Shape) {
                        continue;
                    }
                    BlockPos At = Centre.offset(Dx, Dy, Dz);
                    if (World.getBlockState(At).canBeReplaced()) {
                        World.setBlock(At, Leaf, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    public static final class Built extends SavedData {
        public static final String DataName = "scp_unleashed_scp001_garden";
        public static final SavedData.Factory<Built> DataFactory = new SavedData.Factory<>(Built::new, Built::load);
        public boolean Ready;
        public int Ground;

        @Override
        public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
            Tag.putBoolean("Ready", Ready);
            Tag.putInt("Ground", Ground);
            return Tag;
        }

        public static Built load(CompoundTag Tag, HolderLookup.Provider Registries) {
            Built Data = new Built();
            Data.Ready = Tag.getBoolean("Ready");
            Data.Ground = Tag.getInt("Ground");
            return Data;
        }
    }
}
