package net.rai.scp_unleashed.scp024;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp024.plan.Scp024Layouts;
import net.rai.scp_unleashed.scp024.plan.Scp024Plan;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class Scp024Stage {
    public static final ResourceKey<Level> Key = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "show_stage"));
    public static final int FieldSpacing = 8192;
    public static final int RoomSpacing = 160;
    public static final int HoldRadius = 2;
    public static final int HoldMargin = 8;
    public static final TicketType<UUID> HoldTicket = TicketType.create("scp024_field", UUID::compareTo);
    public static final int StageY = 96;
    public static final String OwnedTag = "scp024_owned";
    public static final String OwnerKey = "Scp024Owner";
    private static final Set<UUID> Moving = ConcurrentHashMap.newKeySet();
    private static Scp024Plan Lobby;

    private Scp024Stage() {
    }

    @Nullable
    public static ServerLevel level(MinecraftServer Server) {
        return Server.getLevel(Key);
    }

    public static boolean inStage(@Nullable Level World) {
        return World != null && World.dimension() == Key;
    }

    public static boolean inStage(Entity Target) {
        return inStage(Target.level());
    }

    public static Scp024Plan lobby() {
        if (Lobby == null) {
            Lobby = Scp024Layouts.lobby();
        }
        return Lobby;
    }

    public static int[] cell(int Field) {
        if (Field <= 0) {
            return new int[]{0, 0};
        }
        int Ring = (int) Math.ceil((Math.sqrt(Field + 1.0) - 1.0) / 2.0);
        int Side = Ring * 2;
        int Step = Field - (Side - 1) * (Side - 1);
        int Along = Step % Side;
        return switch (Step / Side) {
            case 0 -> new int[]{Ring, -Ring + 1 + Along};
            case 1 -> new int[]{Ring - 1 - Along, Ring};
            case 2 -> new int[]{-Ring, Ring - 1 - Along};
            default -> new int[]{-Ring + 1 + Along, -Ring};
        };
    }

    public static BlockPos fieldBase(int Field) {
        int[] Cell = cell(Field);
        return new BlockPos(Cell[0] * FieldSpacing, StageY, Cell[1] * FieldSpacing);
    }

    public static boolean inField(double X, double Z, int Field) {
        int[] Cell = cell(Field);
        return Math.floorDiv(Mth.floor(X) + FieldSpacing / 2, FieldSpacing) == Cell[0] && Math.floorDiv(Mth.floor(Z) + FieldSpacing / 4, FieldSpacing) == Cell[1];
    }

    public static boolean inField(Entity Target, int Field) {
        return inStage(Target) && inField(Target.getX(), Target.getZ(), Field);
    }

    public static AABB footprint(int Field) {
        BlockPos Base = fieldBase(Field);
        AABB Lobby = lobbyBox(Field);
        return new AABB(Base.getX() - RoomSpacing, Lobby.minY, Lobby.minZ, Base.getX() + RoomSpacing, Lobby.maxY,
                Base.getZ() + RoomSpacing * (Scp024Games.ids().size() + 1.0)).inflate(HoldMargin);
    }

    public static BlockPos roomOrigin(int Field, int GameIndex) {
        return fieldBase(Field).offset(0, 0, RoomSpacing * (GameIndex + 1));
    }

    public static AABB lobbyBox(int Field) {
        return box(fieldBase(Field), lobby());
    }

    public static void hold(ServerLevel World, AABB Area, UUID Key) {
        eachChunk(Area, Pos -> World.getChunkSource().addRegionTicket(HoldTicket, Pos, HoldRadius, Key));
    }

    public static void release(ServerLevel World, AABB Area, UUID Key) {
        eachChunk(Area, Pos -> World.getChunkSource().removeRegionTicket(HoldTicket, Pos, HoldRadius, Key));
    }

    private static void eachChunk(AABB Area, Consumer<ChunkPos> Action) {
        int MinX = SectionPos.blockToSectionCoord(Mth.floor(Area.minX) - HoldMargin);
        int MaxX = SectionPos.blockToSectionCoord(Mth.floor(Area.maxX) + HoldMargin);
        int MinZ = SectionPos.blockToSectionCoord(Mth.floor(Area.minZ) - HoldMargin);
        int MaxZ = SectionPos.blockToSectionCoord(Mth.floor(Area.maxZ) + HoldMargin);
        for (int X = MinX; X <= MaxX; X++) {
            for (int Z = MinZ; Z <= MaxZ; Z++) {
                Action.accept(new ChunkPos(X, Z));
            }
        }
    }

    public static AABB box(BlockPos Origin, Scp024Plan Plan) {
        int[] Low = Plan.low();
        int[] High = Plan.high();
        return new AABB(Origin.getX() + Low[0], Origin.getY() + Low[1], Origin.getZ() + Low[2],
                Origin.getX() + High[0] + 1, Origin.getY() + High[1] + 1, Origin.getZ() + High[2] + 1);
    }

    public static Vec3 centre(BlockPos Origin, int[] Mark) {
        return new Vec3(Origin.getX() + Mark[0] + 0.5, Origin.getY() + Mark[1], Origin.getZ() + Mark[2] + 0.5);
    }

    public static float yaw(int Facing) {
        return Scp024Palette.side(Facing).toYRot();
    }

    public static void own(Entity Target, UUID Token) {
        Target.addTag(OwnedTag);
        Target.getPersistentData().putUUID(OwnerKey, Token);
    }

    @Nullable
    public static UUID owner(Entity Target) {
        if (!Target.getTags().contains(OwnedTag) || !Target.getPersistentData().hasUUID(OwnerKey)) {
            return null;
        }
        return Target.getPersistentData().getUUID(OwnerKey);
    }

    public static boolean isOwned(Entity Target) {
        return Target.getTags().contains(OwnedTag);
    }

    public static boolean isMoving(Player User) {
        return Moving.contains(User.getUUID());
    }

    public static void teleport(ServerPlayer User, ServerLevel World, Vec3 Spot, float Yaw, float Pitch) {
        Moving.add(User.getUUID());
        try {
            User.stopRiding();
            if (User.isFallFlying()) {
                User.stopFallFlying();
            }
            User.teleportTo(World, Spot.x, Spot.y, Spot.z, Yaw, Pitch);
            User.setDeltaMovement(Vec3.ZERO);
            User.fallDistance = 0.0f;
            User.resetFallDistance();
        } finally {
            Moving.remove(User.getUUID());
        }
    }

    public static void discardOwned(ServerLevel World, AABB Area, @Nullable UUID Token) {
        for (Entity Found : World.getEntities((Entity) null, Area.inflate(2.0), Candidate -> !(Candidate instanceof Player))) {
            if (Scp024Relocation.belongsOutside(Found)) {
                Scp024Relocation.moveOut(World.getServer(), Found);
                continue;
            }
            UUID Owner = owner(Found);
            if (Token == null || Owner == null || Owner.equals(Token)) {
                Found.discard();
            }
        }
    }
}
