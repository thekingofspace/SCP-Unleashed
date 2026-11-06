package net.rai.scp_unleashed.scp2317;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.gateway.GatewayStash;
import net.rai.scp_unleashed.gateway.GatewayTravel;
import org.jetbrains.annotations.Nullable;

public final class Scp2317Travel {
    public static final String ReturnKey = "scp_unleashed_scp2317_return";
    public static final String PassedKey = "scp_unleashed_scp2317_passed";
    public static final int Rest = 40;
    public static final double Ahead = 2.0;
    public static final float ArrivalYaw = 180.0f;

    private Scp2317Travel() {
    }

    private static CompoundTag persisted(Player User) {
        return User.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
    }

    private static void store(Player User, CompoundTag Persisted) {
        User.getPersistentData().put(Player.PERSISTED_NBT_TAG, Persisted);
    }

    public static boolean resting(ServerPlayer User) {
        CompoundTag Persisted = persisted(User);
        return Persisted.contains(PassedKey) && User.server.overworld().getGameTime() - Persisted.getLong(PassedKey) < Rest;
    }

    private static void stamp(ServerPlayer User) {
        CompoundTag Persisted = persisted(User);
        Persisted.putLong(PassedKey, User.server.overworld().getGameTime());
        store(User, Persisted);
    }

    public static boolean enter(ServerPlayer User, Scp2317DoorBlockEntity Door, Vec3 From) {
        Direction Side = Door.sideOf(From);
        Vec3 Back = Door.front(Side, Ahead);
        return enter(User, User.serverLevel().dimension(), Back, Side.toYRot());
    }

    public static boolean enter(ServerPlayer User, ResourceKey<Level> ReturnDimension, Vec3 Back, float Yaw) {
        ServerLevel Desert = Scp2317World.level(User.server);
        if (Desert == null || Scp2317World.inside(User) || resting(User) || !User.isAlive() || User.isSpectator()) {
            return false;
        }
        CompoundTag Persisted = persisted(User);
        CompoundTag Spot = new CompoundTag();
        Spot.putString("Dimension", ReturnDimension.location().toString());
        Spot.putDouble("X", Back.x);
        Spot.putDouble("Y", Back.y);
        Spot.putDouble("Z", Back.z);
        Spot.putFloat("Yaw", Yaw);
        Persisted.put(ReturnKey, Spot);
        store(User, Persisted);
        Scp2317Sounds.play(User.serverLevel(), User.position(), Scp2317Registry.PassLayers, SoundSource.PLAYERS, 0.9f, 1.0f);
        if (Scp2317World.standingIn()) {
            Scp2317Builder.ensure(Desert);
        }
        Vec3 Arrival = Scp2317World.arrival(Scp2317World.players(User.server).size());
        GatewayTravel.move(User, Desert, GatewayTravel.safeSpot(Desert, Arrival, User.getType()), ArrivalYaw);
        stamp(User);
        Scp2317Sounds.play(Desert, Arrival, Scp2317Registry.PassLayers, SoundSource.PLAYERS, 0.9f, 0.8f);
        return true;
    }

    @Nullable
    public static Vec3 storedReturn(Player User) {
        CompoundTag Persisted = persisted(User);
        if (!Persisted.contains(ReturnKey)) {
            return null;
        }
        CompoundTag Spot = Persisted.getCompound(ReturnKey);
        return new Vec3(Spot.getDouble("X"), Spot.getDouble("Y"), Spot.getDouble("Z"));
    }

    public static boolean leave(ServerPlayer User) {
        if (!Scp2317World.inside(User) || resting(User)) {
            return false;
        }
        CompoundTag Persisted = persisted(User);
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
        if (Target == null || Scp2317World.inside(Target)) {
            GatewayStash.Spot Home = GatewayTravel.respawnSpot(User);
            Target = User.server.getLevel(Home.Dimension());
            if (Target == null) {
                Target = User.server.overworld();
            }
            Spot = Home.Pos();
            Yaw = Home.Yaw();
        }
        Persisted.remove(ReturnKey);
        store(User, Persisted);
        Scp2317Sounds.play(User.serverLevel(), User.position(), Scp2317Registry.PassLayers, SoundSource.PLAYERS, 0.9f, 0.8f);
        Vec3 Safe = GatewayTravel.safeSpot(Target, Spot, User.getType());
        GatewayTravel.move(User, Target, Safe, Yaw);
        stamp(User);
        Scp2317Sounds.play(Target, Safe, Scp2317Registry.PassLayers, SoundSource.PLAYERS, 0.9f, 1.0f);
        return true;
    }

    public static BlockPos doorCentre(BlockPos Master, Direction Front) {
        return BlockPos.containing(Scp2317DoorBlock.world(Master, Front, 1.0, 0.0, 0.5));
    }
}
