package net.rai.scp_unleashed.scp4666.christmas;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.ArchiveRegistry;
import net.rai.scp_unleashed.archive.document.DocumentStamp;
import net.rai.scp_unleashed.archive.investigation.Investigations;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;
import net.rai.scp_unleashed.scp4666.Scp4666World;
import net.rai.scp_unleashed.seasonal.Season;
import net.rai.scp_unleashed.seasonal.Seasons;

import java.time.LocalDate;
import java.util.Optional;

public final class Scp4666Sightings {
    public static final int CheckInterval = 2400;
    public static final float Chance = 0.015f;
    public static final long CooldownTicks = 72000L;
    public static final long DuskStart = 12000L;
    public static final long DawnEnd = 23500L;
    public static final double NearDistance = 60.0;
    public static final double FarDistance = 90.0;
    public static final float ViewCone = 40.0f;
    public static final int Attempts = 24;
    public static final int MaxRise = 24;
    public static final double HeadHeight = 2.4;

    public enum Refusal {
        None, Season, Unread, Elsewhere, Indoors, Daytime, Cooldown, SameDay
    }

    private Scp4666Sightings() {
    }

    public static boolean dusk(Level World) {
        long Time = World.getDayTime() % 24000L;
        return Time >= DuskStart && Time <= DawnEnd;
    }

    public static boolean readFile(ServerPlayer User) {
        if (Scp4666World.inside(User)) {
            return true;
        }
        if (Investigations.get(User.server).find(User.getUUID(), Scp4666Registry.DocumentId).isPresent()) {
            return true;
        }
        Inventory Pockets = User.getInventory();
        for (int Slot = 0; Slot < Pockets.getContainerSize(); Slot++) {
            ItemStack Stack = Pockets.getItem(Slot);
            if (!Stack.is(ArchiveRegistry.OpenedDocument.get())) {
                continue;
            }
            DocumentStamp Stamp = Stack.get(ArchiveRegistry.StampComponent.get());
            if (Stamp != null && Stamp.Document().equals(Scp4666Registry.DocumentId)) {
                return true;
            }
        }
        return false;
    }

    public static void noticeRead(ServerPlayer User, Scp4666Visit Visit) {
        if (!Visit.Read && readFile(User)) {
            Visit.Read = true;
        }
    }

    public static Refusal refusal(ServerPlayer User, Scp4666Visit Visit, LocalDate Today) {
        boolean Present = User.level().dimension() == Level.OVERWORLD && User.isAlive() && !User.isSpectator() && !User.isSleeping();
        boolean Outdoors = User.level().canSeeSky(BlockPos.containing(User.getEyePosition()));
        return decide(Seasons.active(User.level(), Season.Christmas), Visit, Present, Outdoors, dusk(User.level()), User.level().getGameTime(), Today);
    }

    public static Refusal decide(boolean Christmas, Scp4666Visit Visit, boolean Present, boolean Outdoors, boolean Dusk, long Now, LocalDate Today) {
        if (!Christmas) {
            return Refusal.Season;
        }
        if (!Visit.Read) {
            return Refusal.Unread;
        }
        if (!Present) {
            return Refusal.Elsewhere;
        }
        if (!Outdoors) {
            return Refusal.Indoors;
        }
        if (!Dusk) {
            return Refusal.Daytime;
        }
        if (Visit.SightingTick >= 0L && Now - Visit.SightingTick < CooldownTicks) {
            return Refusal.Cooldown;
        }
        if (Visit.SightingDay == Today.toEpochDay()) {
            return Refusal.SameDay;
        }
        return Refusal.None;
    }

    public static boolean roll(ServerPlayer User, Scp4666Visit Visit) {
        if (refusal(User, Visit, Seasons.today()) != Refusal.None || User.getRandom().nextFloat() >= Chance) {
            return false;
        }
        return show(User, Visit).isPresent();
    }

    public static Optional<Scp4666Sighting> show(ServerPlayer User, Scp4666Visit Visit) {
        Optional<Vec3> Spot = spot(User.serverLevel(), User, User.getRandom());
        if (Spot.isEmpty()) {
            return Optional.empty();
        }
        Scp4666Sighting Figure = Scp4666Sighting.spawn(User.serverLevel(), Spot.get(), User);
        Visit.SightingTick = User.level().getGameTime();
        Visit.SightingDay = Seasons.today().toEpochDay();
        return Optional.of(Figure);
    }

    public static double farLimit(ServerPlayer User) {
        int Chunks = User.server.getPlayerList().getViewDistance();
        return Math.min(FarDistance, Math.max(NearDistance, Chunks * 16.0 - 16.0));
    }

    public static Optional<Vec3> spot(ServerLevel World, ServerPlayer User, RandomSource Random) {
        double Far = farLimit(User);
        Vec3 Eye = User.getEyePosition();
        for (int Attempt = 0; Attempt < Attempts; Attempt++) {
            Vec3 Aim = aim(User.position(), User.getYRot(), Random, Far);
            BlockPos Column = BlockPos.containing(Aim.x, User.getY(), Aim.z);
            if (!World.isLoaded(Column) || !World.isPositionEntityTicking(Column)) {
                continue;
            }
            BlockPos Top = World.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Column);
            if (Math.abs(Top.getY() - User.getY()) > MaxRise || !open(World, Top)) {
                continue;
            }
            Vec3 Feet = new Vec3(Top.getX() + 0.5, Top.getY(), Top.getZ() + 0.5);
            Vec3 Head = Feet.add(0.0, HeadHeight, 0.0);
            HitResult Line = World.clip(new ClipContext(Eye, Head, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, User));
            if (Line.getType() != HitResult.Type.MISS && Line.getLocation().distanceToSqr(Head) > 1.0) {
                continue;
            }
            return Optional.of(Feet);
        }
        return Optional.empty();
    }

    public static Vec3 aim(Vec3 Origin, float Facing, RandomSource Random, double Far) {
        float Yaw = Facing + (Random.nextFloat() * 2.0f - 1.0f) * ViewCone;
        double Distance = NearDistance + Random.nextDouble() * (Math.max(NearDistance, Far) - NearDistance);
        return new Vec3(Origin.x - Mth.sin(Yaw * Mth.DEG_TO_RAD) * Distance, Origin.y, Origin.z + Mth.cos(Yaw * Mth.DEG_TO_RAD) * Distance);
    }

    public static boolean open(ServerLevel World, BlockPos Top) {
        BlockState Ground = World.getBlockState(Top.below());
        if (!Ground.isFaceSturdy(World, Top.below(), Direction.UP) || !World.getFluidState(Top.below()).isEmpty()) {
            return false;
        }
        for (int Rise = 0; Rise < 3; Rise++) {
            BlockPos Above = Top.above(Rise);
            if (!World.getBlockState(Above).getCollisionShape(World, Above).isEmpty() || !World.getFluidState(Above).isEmpty()) {
                return false;
            }
        }
        return World.canSeeSky(Top);
    }
}
