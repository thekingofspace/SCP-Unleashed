package net.rai.scp_unleashed.scp999;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.control.ControlBodies;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

public final class Scp999Goals {
    public static final double CandyReach = 10.0;
    public static final int BowlReach = 12;
    public static final int BowlVertical = 3;
    public static final int SearchInterval = 20;
    public static final int GiveUp = 400;
    public static final double BegStop = 1.8;
    public static final double BegLeave = 10.0;
    public static final double ComfortRange = 16.0;
    public static final double ComfortStop = 1.6;
    public static final float HurtShare = 0.6f;
    public static final float HealedShare = 0.95f;
    public static final int NuzzleMin = 60;
    public static final int NuzzleMax = 120;
    public static final double SeekRange = 48.0;
    public static final double SeekStop = 2.5;
    public static final double CuriousRange = 12.0;
    public static final double CuriousStop = 2.2;
    public static final double PlayRange = 10.0;
    public static final double ToyReach = 1.3;
    public static final double FriendReach = 1.9;
    public static final int PlayRounds = 3;
    public static final int PlayRest = 400;
    public static final float PlayChance = 0.05f;
    public static final double FollowRange = 24.0;
    public static final double FollowStart = 5.0;
    public static final double FollowStop = 2.4;
    public static final double NudgeSpeed = 0.18;
    public static final double NudgeLift = 0.22;

    private Scp999Goals() {
    }

    public static boolean holdsCandy(Player User) {
        return User.getItemInHand(InteractionHand.MAIN_HAND).is(Scp999Registry.Candy.get()) || User.getItemInHand(InteractionHand.OFF_HAND).is(Scp999Registry.Candy.get());
    }

    @Nullable
    public static Player candyHolder(Scp999Blob Blob) {
        return Blob.level().getNearestPlayer(Blob.getX(), Blob.getY(), Blob.getZ(), Scp999Blob.CandySight,
                Candidate -> Candidate instanceof Player User && User.isAlive() && !User.isSpectator() && holdsCandy(User));
    }

    public static boolean hurt(LivingEntity Who) {
        return Who.isAlive() && Who.getHealth() < Who.getMaxHealth() * HurtShare && !(Who instanceof Player User && (User.isSpectator() || User.isCreative()));
    }

    @Nullable
    public static LivingEntity hurtPerson(Scp999Blob Blob) {
        LivingEntity Best = null;
        double BestDistance = ComfortRange * ComfortRange;
        for (LivingEntity Other : Blob.level().getEntitiesOfClass(LivingEntity.class, Blob.getBoundingBox().inflate(ComfortRange),
                Candidate -> (Candidate instanceof Player || ControlBodies.isPersonLike(Candidate)) && hurt(Candidate))) {
            double Distance = Other.distanceToSqr(Blob);
            if (Distance < BestDistance) {
                Best = Other;
                BestDistance = Distance;
            }
        }
        return Best;
    }

    @Nullable
    public static ItemEntity groundCandy(Scp999Blob Blob) {
        ItemEntity Best = null;
        double BestDistance = CandyReach * CandyReach;
        for (ItemEntity Item : Blob.level().getEntitiesOfClass(ItemEntity.class, Blob.getBoundingBox().inflate(CandyReach),
                Candidate -> Candidate.isAlive() && Candidate.getItem().is(Scp999Registry.Candy.get()))) {
            double Distance = Item.distanceToSqr(Blob);
            if (Distance < BestDistance) {
                Best = Item;
                BestDistance = Distance;
            }
        }
        return Best;
    }

    @Nullable
    public static BlockPos fullBowl(Scp999Blob Blob) {
        BlockPos Centre = Blob.blockPosition();
        BlockPos Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (BlockPos Pos : BlockPos.betweenClosed(Centre.offset(-BowlReach, -BowlVertical, -BowlReach), Centre.offset(BowlReach, BowlVertical, BowlReach))) {
            if (!Blob.level().isLoaded(Pos)) {
                continue;
            }
            if (PinkDogBowlBlock.candy(Blob.level().getBlockState(Pos)) <= 0) {
                continue;
            }
            double Distance = Pos.distSqr(Centre);
            if (Distance < BestDistance) {
                Best = Pos.immutable();
                BestDistance = Distance;
            }
        }
        return Best;
    }

    public static double flatDistance(Entity Owner, Vec3 Target) {
        double Dx = Target.x - Owner.getX();
        double Dz = Target.z - Owner.getZ();
        return Math.sqrt(Dx * Dx + Dz * Dz);
    }

    public static boolean friendly(Entity Other) {
        if (Other instanceof Enemy || Other instanceof NeutralMob Neutral && Neutral.isAngry()) {
            return false;
        }
        return Other instanceof Animal || Other instanceof AbstractVillager || Other instanceof Scp999Blob;
    }

    public static class SeekTreat extends Goal {
        private final Scp999Blob Blob;
        @Nullable
        private BlockPos Bowl;
        @Nullable
        private ItemEntity Candy;
        private int Wait;
        private int Spent;

        public SeekTreat(Scp999Blob Blob) {
            this.Blob = Blob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!Blob.free() || Blob.treatCooldown() > 0) {
                return false;
            }
            if (--Wait > 0) {
                return false;
            }
            Wait = SearchInterval;
            Candy = groundCandy(Blob);
            Bowl = Candy == null && Blob.craving() ? fullBowl(Blob) : null;
            return Candy != null || Bowl != null;
        }

        @Override
        public boolean canContinueToUse() {
            if (!Blob.free() || Spent > GiveUp || Candy == null && Bowl == null) {
                return false;
            }
            if (Candy != null) {
                return Candy.isAlive();
            }
            return Bowl != null && PinkDogBowlBlock.candy(Blob.level().getBlockState(Bowl)) > 0;
        }

        @Override
        public void start() {
            Spent = 0;
            move();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        private Vec3 target() {
            return Candy != null ? Candy.position() : Vec3.atBottomCenterOf(Bowl);
        }

        private void move() {
            Vec3 Spot = target();
            Blob.getNavigation().moveTo(Spot.x, Spot.y, Spot.z, Scp999Blob.RushModifier);
        }

        @Override
        public void tick() {
            Spent++;
            if (Candy == null && Bowl == null) {
                return;
            }
            Vec3 Spot = target();
            Blob.getLookControl().setLookAt(Spot.x, Spot.y + 0.2, Spot.z);
            boolean Close = flatDistance(Blob, Spot) <= Scp999Blob.EatReach && Math.abs(Spot.y - Blob.getY()) < 1.5;
            if (Close && Blob.level() instanceof ServerLevel World) {
                Blob.getNavigation().stop();
                if (Candy != null) {
                    Blob.eatFromGround(World, Candy);
                } else if (Bowl != null) {
                    Blob.eatFromBowl(World, Bowl);
                }
                Candy = null;
                Bowl = null;
                return;
            }
            if (Spent % 10 == 0 || Blob.getNavigation().isDone()) {
                move();
            }
        }

        @Override
        public void stop() {
            Candy = null;
            Bowl = null;
            if (!Blob.isActing()) {
                Blob.getNavigation().stop();
            }
        }
    }

    public static class Beg extends Goal {
        private final Scp999Blob Blob;
        @Nullable
        private Player Holder;
        private int Replan;

        public Beg(Scp999Blob Blob) {
            this.Blob = Blob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!Blob.free()) {
                return false;
            }
            Holder = candyHolder(Blob);
            return Holder != null;
        }

        @Override
        public boolean canContinueToUse() {
            return Blob.free() && Holder != null && Holder.isAlive() && !Holder.isSpectator() && holdsCandy(Holder) && Holder.distanceTo(Blob) < BegLeave;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (Holder == null) {
                return;
            }
            Blob.getLookControl().setLookAt(Holder, 30.0f, 30.0f);
            if (Holder.distanceTo(Blob) > BegStop) {
                if (--Replan <= 0) {
                    Replan = 10;
                    Blob.getNavigation().moveTo(Holder, Scp999Blob.BegModifier);
                }
            } else {
                Blob.getNavigation().stop();
            }
        }

        @Override
        public void stop() {
            Holder = null;
            Blob.getNavigation().stop();
        }
    }

    public static class Comfort extends Goal {
        private final Scp999Blob Blob;
        @Nullable
        private LivingEntity Patient;
        private int Replan;
        private int Nuzzle;

        public Comfort(Scp999Blob Blob) {
            this.Blob = Blob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!Blob.free()) {
                return false;
            }
            Patient = hurtPerson(Blob);
            return Patient != null;
        }

        @Override
        public boolean canContinueToUse() {
            return Blob.free() && Patient != null && Patient.isAlive() && Patient.getHealth() < Patient.getMaxHealth() * HealedShare && Patient.distanceTo(Blob) < ComfortRange + 4.0
                    && !(Patient instanceof Player User && User.isSpectator());
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            Replan = 0;
            Nuzzle = NuzzleMin / 2;
            if (Blob.level() instanceof ServerLevel World) {
                Scp999Effects.layers(World, Blob, Scp999Registry.CooLayers, Scp999Blob.VoiceVolume * 0.8f, Blob.voicePitch() * 1.05f);
            }
        }

        @Override
        public void tick() {
            if (Patient == null) {
                return;
            }
            Blob.getLookControl().setLookAt(Patient, 30.0f, 30.0f);
            if (Patient.distanceTo(Blob) > ComfortStop) {
                if (--Replan <= 0) {
                    Replan = 8;
                    Blob.getNavigation().moveTo(Patient, Scp999Blob.RushModifier);
                }
                return;
            }
            Blob.getNavigation().stop();
            if (--Nuzzle <= 0 && Blob.level() instanceof ServerLevel World) {
                Nuzzle = NuzzleMin + Blob.getRandom().nextInt(NuzzleMax - NuzzleMin + 1);
                Blob.nuzzle(World, Patient);
            }
        }

        @Override
        public void stop() {
            Patient = null;
            if (!Blob.isActing()) {
                Blob.getNavigation().stop();
            }
        }
    }

    public static class Seek extends Goal {
        private final Scp999Blob Blob;
        @Nullable
        private Player Target;
        private int Replan;

        public Seek(Scp999Blob Blob) {
            this.Blob = Blob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Nullable
        private Player find() {
            UUID Who = Blob.sought();
            if (Who == null) {
                return null;
            }
            Player Found = Blob.level().getPlayerByUUID(Who);
            if (Found == null || !Found.isAlive() || Found.isSpectator() || Found.distanceTo(Blob) > SeekRange) {
                return null;
            }
            return Found;
        }

        @Override
        public boolean canUse() {
            if (!Blob.free()) {
                return false;
            }
            Target = find();
            return Target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return Blob.free() && Target != null && Target == find();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (Target == null) {
                return;
            }
            Blob.getLookControl().setLookAt(Target, 30.0f, 30.0f);
            if (Target.distanceTo(Blob) <= SeekStop) {
                Blob.getNavigation().stop();
                if (Blob.level() instanceof ServerLevel World) {
                    Blob.arrive(World, Target);
                }
                Target = null;
                return;
            }
            if (--Replan <= 0) {
                Replan = 10;
                Blob.getNavigation().moveTo(Target, Scp999Blob.FollowModifier);
            }
        }

        @Override
        public void stop() {
            Target = null;
            if (!Blob.isActing()) {
                Blob.getNavigation().stop();
            }
        }
    }

    public static class Curious extends Goal {
        private final Scp999Blob Blob;
        @Nullable
        private Entity Subject;
        private int Replan;

        public Curious(Scp999Blob Blob) {
            this.Blob = Blob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Nullable
        private Entity find() {
            if (Blob.curiosity() <= 0) {
                return null;
            }
            UUID Who = Blob.curiousAbout();
            if (Who != null) {
                Player Found = Blob.level().getPlayerByUUID(Who);
                if (Found != null && Found.isAlive() && !Found.isSpectator() && Found.distanceTo(Blob) < CuriousRange) {
                    return Found;
                }
            }
            Entity Best = null;
            double BestDistance = CuriousRange * CuriousRange;
            for (Entity Other : Blob.level().getEntities(Blob, Blob.getBoundingBox().inflate(CuriousRange), Candidate -> Candidate instanceof LivingEntity && Candidate.isAlive()
                    && (friendly(Candidate) || Candidate instanceof Player User && !User.isSpectator()))) {
                double Distance = Other.distanceToSqr(Blob);
                if (Distance < BestDistance) {
                    Best = Other;
                    BestDistance = Distance;
                }
            }
            return Best;
        }

        @Override
        public boolean canUse() {
            if (!Blob.free()) {
                return false;
            }
            Subject = find();
            if (Subject == null && Blob.curiosity() > 0) {
                Blob.satisfyCuriosity();
            }
            return Subject != null;
        }

        @Override
        public boolean canContinueToUse() {
            return Blob.free() && Subject != null && Subject.isAlive() && Blob.curiosity() > 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (Subject == null) {
                return;
            }
            Blob.getLookControl().setLookAt(Subject, 30.0f, 30.0f);
            if (Subject.distanceTo(Blob) <= CuriousStop) {
                Blob.getNavigation().stop();
                if (Blob.level() instanceof ServerLevel World) {
                    Scp999Effects.layers(World, Blob, Scp999Registry.ChirpLayers, Scp999Blob.VoiceVolume, Blob.voicePitch() * 1.12f);
                    Scp999Effects.curious(World, Blob);
                }
                Blob.satisfyCuriosity();
                return;
            }
            if (--Replan <= 0) {
                Replan = 10;
                Blob.getNavigation().moveTo(Subject, Scp999Blob.FollowModifier * 0.85);
            }
        }

        @Override
        public void stop() {
            Subject = null;
            if (!Blob.isActing()) {
                Blob.getNavigation().stop();
            }
        }
    }

    public static class Play extends Goal {
        private final Scp999Blob Blob;
        @Nullable
        private Entity Target;
        private int Rounds;
        private int Replan;
        private int Spent;

        public Play(Scp999Blob Blob) {
            this.Blob = Blob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Nullable
        private Entity find() {
            Entity Best = null;
            double BestDistance = PlayRange * PlayRange;
            for (Entity Other : Blob.level().getEntities(Blob, Blob.getBoundingBox().inflate(PlayRange), Candidate -> Candidate.isAlive()
                    && (Candidate instanceof ItemEntity Item && !Item.getItem().is(Scp999Registry.Candy.get()) || friendly(Candidate)))) {
                double Distance = Other.distanceToSqr(Blob);
                if (Distance < BestDistance) {
                    Best = Other;
                    BestDistance = Distance;
                }
            }
            return Best;
        }

        @Override
        public boolean canUse() {
            if (!Blob.free() || Blob.playRest() > 0 || Blob.getRandom().nextFloat() >= Blob.playChance()) {
                return false;
            }
            Target = find();
            return Target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return Target != null && Target.isAlive() && Rounds < PlayRounds && Spent < GiveUp && (Blob.free() || Blob.action() == Scp999Blob.ActionPlay);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            Rounds = 0;
            Spent = 0;
            Replan = 0;
        }

        @Override
        public void tick() {
            Spent++;
            if (Target == null || !Blob.free()) {
                return;
            }
            Blob.getLookControl().setLookAt(Target, 30.0f, 30.0f);
            double Reach = Target instanceof ItemEntity ? ToyReach : FriendReach;
            if (flatDistance(Blob, Target.position()) <= Reach + Target.getBbWidth() * 0.5 && Blob.level() instanceof ServerLevel World) {
                Blob.getNavigation().stop();
                Rounds++;
                Blob.playWith(World, Target, Target instanceof ItemEntity Item ? Item : null);
                return;
            }
            if (--Replan <= 0) {
                Replan = 10;
                Blob.getNavigation().moveTo(Target, Scp999Blob.PlayModifier);
            }
        }

        @Override
        public void stop() {
            Target = null;
            Blob.restFromPlay(PlayRest);
            if (!Blob.isActing()) {
                Blob.getNavigation().stop();
            }
        }

        public static void poke(ServerLevel World, Scp999Blob Blob, @Nullable ItemEntity Toy) {
            if (Toy != null && Toy.isAlive()) {
                Vec3 Away = Toy.position().subtract(Blob.position());
                Vec3 Flat = new Vec3(Away.x, 0.0, Away.z);
                Vec3 Push = Flat.lengthSqr() < 1e-4 ? new Vec3(0.0, 0.0, NudgeSpeed) : Flat.normalize().scale(NudgeSpeed);
                Toy.setDeltaMovement(Push.x, NudgeLift, Push.z);
                Toy.hasImpulse = true;
            }
            Scp999Effects.layers(World, Blob, Scp999Registry.SquishLayers, Scp999Blob.VoiceVolume * 0.6f, 1.15f);
            Scp999Effects.hearts(World, Blob, 1);
        }
    }

    public static class Cuddle extends Goal {
        private final Scp999Blob Blob;
        @Nullable
        private Player Friend;
        private int Replan;
        private int Wait;
        private int Spent;

        public Cuddle(Scp999Blob Blob) {
            this.Blob = Blob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Nullable
        private Player find() {
            for (Player User : Blob.level().getEntitiesOfClass(Player.class, Blob.getBoundingBox().inflate(Scp999Blob.HugSeek),
                    Candidate -> Candidate.isAlive() && !Candidate.isSpectator() && Blob.likes(Candidate) && Blob.stillFor(Candidate) >= Scp999Blob.HugStill)) {
                if (User.distanceTo(Blob) <= Scp999Blob.HugSeek) {
                    return User;
                }
            }
            return null;
        }

        @Override
        public boolean canUse() {
            if (!Blob.free() || !Blob.readyToHug()) {
                return false;
            }
            if (--Wait > 0) {
                return false;
            }
            Wait = SearchInterval;
            if (Blob.getRandom().nextFloat() >= Scp999Blob.HugChance) {
                return false;
            }
            Friend = find();
            return Friend != null;
        }

        @Override
        public boolean canContinueToUse() {
            return Blob.free() && Friend != null && Friend.isAlive() && Spent < GiveUp / 2 && Friend.distanceTo(Blob) < Scp999Blob.HugSeek + 2.0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            Spent = 0;
            Replan = 0;
        }

        @Override
        public void tick() {
            Spent++;
            if (Friend == null) {
                return;
            }
            Blob.getLookControl().setLookAt(Friend, 30.0f, 30.0f);
            if (Friend.distanceTo(Blob) <= Scp999Blob.HugRange && Blob.level() instanceof ServerLevel World) {
                Blob.getNavigation().stop();
                Blob.hug(World, Friend);
                Friend = null;
                return;
            }
            if (--Replan <= 0) {
                Replan = 8;
                Blob.getNavigation().moveTo(Friend, Scp999Blob.FollowModifier);
            }
        }

        @Override
        public void stop() {
            Friend = null;
            if (!Blob.isActing()) {
                Blob.getNavigation().stop();
            }
        }
    }

    public static class FollowFriend extends Goal {
        private final Scp999Blob Blob;
        @Nullable
        private Player Friend;
        private int Replan;

        public FollowFriend(Scp999Blob Blob) {
            this.Blob = Blob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Nullable
        private Player find() {
            Player Best = null;
            double BestDistance = FollowRange * FollowRange;
            for (Player User : Blob.level().players()) {
                if (!User.isAlive() || User.isSpectator() || !Blob.likes(User)) {
                    continue;
                }
                double Distance = User.distanceToSqr(Blob);
                if (Distance < BestDistance) {
                    Best = User;
                    BestDistance = Distance;
                }
            }
            return Best;
        }

        @Override
        public boolean canUse() {
            if (!Blob.free()) {
                return false;
            }
            Friend = find();
            return Friend != null && Friend.distanceTo(Blob) > FollowStart;
        }

        @Override
        public boolean canContinueToUse() {
            return Blob.free() && Friend != null && Friend.isAlive() && !Friend.isSpectator() && Friend.distanceTo(Blob) > FollowStop && Friend.distanceTo(Blob) < FollowRange + 8.0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (Friend == null) {
                return;
            }
            Blob.getLookControl().setLookAt(Friend, 30.0f, 30.0f);
            if (--Replan <= 0) {
                Replan = 10;
                Blob.getNavigation().moveTo(Friend, Scp999Blob.FollowModifier);
            }
        }

        @Override
        public void stop() {
            Friend = null;
            Blob.getNavigation().stop();
        }
    }

    public static boolean aggressive(Mob Other) {
        LivingEntity Prey = Other.getTarget();
        return Prey != null && Prey.isAlive() && (Prey instanceof Player || Prey instanceof Scp999Blob || ControlBodies.isPersonLike(Prey) || Prey instanceof AbstractVillager);
    }
}
