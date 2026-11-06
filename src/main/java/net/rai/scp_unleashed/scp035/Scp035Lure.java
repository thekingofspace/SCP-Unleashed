package net.rai.scp_unleashed.scp035;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.scp035.network.Scp035LurePayload;
import net.rai.scp_unleashed.scp148.Scp148Shield;
import net.rai.scp_unleashed.senses.Senses;
import net.rai.scp_unleashed.storage.StorageContainerBlock;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Scp035Lure {
    public static final double PullRange = 16.0;
    public static final double HoldRange = 17.0;
    public static final double EquipReach = 1.5;
    public static final double EquipHeight = 1.6;
    public static final int ScanInterval = 4;
    public static final int SightGrace = 12;
    public static final int NpcInterval = 10;
    public static final double NpcSpeed = 0.9;
    public static final float StrengthStep = 0.04f;
    public static final double PointStep = 0.02;
    private static final Map<UUID, Lured> States = new HashMap<>();

    private Scp035Lure() {
    }

    public static void tick(ServerPlayer User) {
        Lured State = States.computeIfAbsent(User.getUUID(), Key -> new Lured());
        UUID Body = ControlSessions.session(User).BodyId();
        if (!Body.equals(State.Body)) {
            State.Body = Body;
            State.release();
        }
        if (!(User.level() instanceof ServerLevel World) || !Scp035Sight.isDrawable(User) || Senses.isBlindfolded(User)) {
            State.release();
            sync(User, State);
            return;
        }
        if ((User.tickCount + User.getId()) % ScanInterval == 0) {
            Source Found = find(World, User, State);
            if (Found != null && Scp148Shield.blocks(User)) {
                State.release();
            } else if (Found != null) {
                State.take(Found);
            } else if (State.Active) {
                State.Blind += ScanInterval;
                if (State.Blind >= SightGrace) {
                    State.release();
                }
            }
        }
        if (State.Active) {
            if (!refresh(World, State)) {
                State.release();
            } else {
                State.Strength = strength(User.getEyePosition().distanceTo(State.Point));
                if (inReach(User.position(), State.Point) && seize(World, User, State)) {
                    State.release();
                }
            }
        }
        sync(User, State);
    }

    public static float strength(double Distance) {
        return (float) Mth.clamp(1.0 - (Distance - EquipReach) / (PullRange - EquipReach), 0.0, 1.0);
    }

    public static boolean inReach(Vec3 Feet, Vec3 Point) {
        double Dx = Feet.x - Point.x;
        double Dz = Feet.z - Point.z;
        return Dx * Dx + Dz * Dz <= EquipReach * EquipReach && Math.abs(Point.y - Feet.y) <= EquipHeight;
    }

    @Nullable
    private static Source find(ServerLevel World, ServerPlayer User, Lured State) {
        Vec3 Eye = User.getEyePosition();
        Source Best = null;
        for (Scp035Mask Mask : World.getEntitiesOfClass(Scp035Mask.class, User.getBoundingBox().inflate(HoldRange), Entity::isAlive)) {
            Vec3 Point = Mask.gazePoint();
            double Distance = Eye.distanceTo(Point);
            double Limit = Mask.getUUID().equals(State.MaskId) ? HoldRange : PullRange;
            if (Distance > Limit || Best != null && Distance >= Best.Distance()) {
                continue;
            }
            if (Scp035Sight.clear(World, Eye, Point)) {
                Best = new Source(Point, Mask.getUUID(), null, Distance);
            }
        }
        for (BlockPos Pos : Scp035Sources.near(World, Eye, HoldRange)) {
            StorageContainerBlockEntity Locker = StorageContainerBlock.containerAt(World, Pos);
            if (Locker == null || !Locker.isVisible() || !Scp035MaskItem.is(Locker.peekContents())) {
                continue;
            }
            Vec3 Window = Scp035Sight.lockerWindow(World, Eye, Locker, Pos.equals(State.LockerPos) ? HoldRange : PullRange);
            if (Window == null) {
                continue;
            }
            double Distance = Eye.distanceTo(Window);
            if (Best == null || Distance < Best.Distance()) {
                Best = new Source(Window, null, Pos.immutable(), Distance);
            }
        }
        return Best;
    }

    private static boolean refresh(ServerLevel World, Lured State) {
        if (State.MaskId != null) {
            if (World.getEntity(State.MaskId) instanceof Scp035Mask Mask && Mask.isAlive()) {
                State.Point = Mask.gazePoint();
                return true;
            }
            return false;
        }
        if (State.LockerPos != null) {
            StorageContainerBlockEntity Locker = StorageContainerBlock.containerAt(World, State.LockerPos);
            return Locker != null && Locker.isVisible() && Scp035MaskItem.is(Locker.peekContents());
        }
        return false;
    }

    private static boolean seize(ServerLevel World, ServerPlayer User, Lured State) {
        if (State.MaskId != null && World.getEntity(State.MaskId) instanceof Scp035Mask Mask && Mask.isAlive()) {
            ItemStack Stack = Mask.stack();
            Mask.discard();
            Scp035Possession.equip(User, Stack);
            return true;
        }
        if (State.LockerPos != null) {
            StorageContainerBlockEntity Locker = StorageContainerBlock.containerAt(World, State.LockerPos);
            if (Locker != null && Scp035MaskItem.is(Locker.peekContents())) {
                ItemStack Stack = Locker.release();
                Scp035Possession.equip(User, Stack);
                return true;
            }
        }
        return false;
    }

    public static void drawPeople(ServerLevel World, Vec3 Point, Scp035Mask Mask) {
        AABB Area = new AABB(Point, Point).inflate(PullRange);
        double Reach = PullRange * PullRange;
        for (Mob Person : World.getEntitiesOfClass(Mob.class, Area, Candidate -> Scp035Sight.isDrawable(Candidate))) {
            Vec3 Eye = Person.getEyePosition();
            if (Eye.distanceToSqr(Point) > Reach || !Scp035Sight.clear(World, Eye, Point)) {
                continue;
            }
            if (Scp148Shield.blocks(Person) || Senses.isBlindfolded(Person)) {
                continue;
            }
            if (inReach(Person.position(), Point)) {
                ItemStack Stack = Mask.stack();
                Mask.discard();
                Scp035Possession.seize(World, Person, Stack);
                return;
            }
            Person.getNavigation().moveTo(Point.x, Point.y, Point.z, NpcSpeed);
            Person.getLookControl().setLookAt(Point.x, Point.y, Point.z);
        }
    }

    public static void drawPeople(ServerLevel World, StorageContainerBlockEntity Locker) {
        Vec3 Front = Scp035Sight.lockerFront(Locker);
        AABB Area = new AABB(Front, Front).inflate(PullRange);
        for (Mob Person : World.getEntitiesOfClass(Mob.class, Area, Candidate -> Scp035Sight.isDrawable(Candidate))) {
            Vec3 Window = Scp035Sight.lockerWindow(World, Person.getEyePosition(), Locker, PullRange);
            if (Window == null) {
                continue;
            }
            if (Scp148Shield.blocks(Person) || Senses.isBlindfolded(Person)) {
                continue;
            }
            if (inReach(Person.position(), Window)) {
                ItemStack Stack = Locker.release();
                Scp035Possession.seize(World, Person, Stack);
                return;
            }
            Person.getNavigation().moveTo(Front.x, Front.y, Front.z, NpcSpeed);
            Person.getLookControl().setLookAt(Window.x, Window.y, Window.z);
        }
    }

    public static boolean isLured(Player User) {
        Lured State = States.get(User.getUUID());
        return State != null && State.Active;
    }

    public static void sync(ServerPlayer User) {
        Lured State = States.get(User.getUUID());
        if (State != null) {
            sync(User, State);
        }
    }

    private static void sync(ServerPlayer User, Lured State) {
        float Compel = Scp035Carry.compel(User);
        boolean Changed = !State.Sent || State.SentActive != State.Active || Math.abs(State.SentCompel - Compel) > StrengthStep * 0.5f
                || State.Active && (Math.abs(State.SentStrength - State.Strength) > StrengthStep || State.SentPoint.distanceToSqr(State.Point) > PointStep * PointStep);
        if (!Changed) {
            return;
        }
        State.Sent = true;
        State.SentActive = State.Active;
        State.SentStrength = State.Strength;
        State.SentPoint = State.Point;
        State.SentCompel = Compel;
        PacketDistributor.sendToPlayer(User, new Scp035LurePayload(State.Active, State.Point.x, State.Point.y, State.Point.z, State.Strength, Compel));
    }

    public static void resend(ServerPlayer User) {
        Lured State = States.computeIfAbsent(User.getUUID(), Key -> new Lured());
        State.release();
        State.Sent = false;
        sync(User, State);
    }

    public static void forget(Player User) {
        States.remove(User.getUUID());
    }

    public static void clear() {
        States.clear();
    }

    private record Source(Vec3 Point, @Nullable UUID MaskId, @Nullable BlockPos LockerPos, double Distance) {
    }

    private static final class Lured {
        private boolean Active;
        @Nullable
        private UUID MaskId;
        @Nullable
        private BlockPos LockerPos;
        private Vec3 Point = Vec3.ZERO;
        private float Strength;
        private int Blind;
        private boolean Sent;
        private boolean SentActive;
        private float SentStrength;
        private float SentCompel;
        private Vec3 SentPoint = Vec3.ZERO;
        private UUID Body = Util.NIL_UUID;

        private void take(Source Found) {
            Active = true;
            MaskId = Found.MaskId();
            LockerPos = Found.LockerPos();
            Point = Found.Point();
            Blind = 0;
        }

        private void release() {
            Active = false;
            MaskId = null;
            LockerPos = null;
            Strength = 0.0f;
            Blind = 0;
        }
    }
}
