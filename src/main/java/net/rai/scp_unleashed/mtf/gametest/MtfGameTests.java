package net.rai.scp_unleashed.mtf.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.extraction.ExtractionRegistry;
import net.rai.scp_unleashed.guns.npc.NpcGunner;
import net.rai.scp_unleashed.mtf.MtfHelicopter;
import net.rai.scp_unleashed.mtf.MtfMission;
import net.rai.scp_unleashed.mtf.MtfMissions;
import net.rai.scp_unleashed.mtf.MtfOperative;
import net.rai.scp_unleashed.mtf.MtfRegistry;
import net.rai.scp_unleashed.mtf.MtfRole;
import net.rai.scp_unleashed.mtf.MtfSignalRadioItem;
import net.rai.scp_unleashed.mtf.MtfTier;
import net.rai.scp_unleashed.rifttech.RiftTechRegistry;
import net.rai.scp_unleashed.scp127.Scp127Item;
import net.rai.scp_unleashed.scp148.Scp148HelmetItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class MtfGameTests {
    public static final String Arena = "mtf_arena";
    public static final double Centre = 12.5;
    public static final int Floor = 2;
    public static final float Sturdy = 400.0f;
    public static final double Search = 320.0;

    private MtfGameTests() {
    }

    private static MtfTestPlayer caller(GameTestHelper Helper, double X, double Z, String Name) {
        ServerLevel World = Helper.getLevel();
        MtfTestPlayer User = new MtfTestPlayer(World, Name);
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Floor, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.enlist(true);
        User.setGameMode(GameType.SURVIVAL);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, MtfTestPlayer User) {
        MtfMissions.cancel(Helper.getLevel().getServer(), User.getUUID());
        User.enlist(false);
        if (!User.isRemoved()) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    private static List<MtfOperative> squad(GameTestHelper Helper, UUID Caller) {
        AABB Area = new AABB(Helper.absolutePos(net.minecraft.core.BlockPos.ZERO)).inflate(Search);
        return Helper.getLevel().getEntitiesOfClass(MtfOperative.class, Area, Member -> Caller.equals(Member.callerId()) && Member.isAlive());
    }

    private static List<MtfHelicopter> rides(GameTestHelper Helper, UUID Caller) {
        AABB Area = new AABB(Helper.absolutePos(net.minecraft.core.BlockPos.ZERO)).inflate(Search);
        return Helper.getLevel().getEntitiesOfClass(MtfHelicopter.class, Area, Ride -> Caller.equals(Ride.callerId()) && Ride.isAlive());
    }

    private static ItemStack fragment(FragmentRarity Grade, int Count) {
        return new ItemStack(ExtractionRegistry.fragmentFor(Grade), Count);
    }

    private static <T extends Mob> T dummy(GameTestHelper Helper, EntityType<T> Type, double X, double Z, float Health) {
        T Made = Helper.spawnWithNoFreeWill(Type, new Vec3(X, Floor, Z));
        if (Health > 0.0f) {
            Made.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Health);
            Made.setHealth(Health);
        }
        return Made;
    }

    private static Vec3 at(GameTestHelper Helper, double X, double Z) {
        return Helper.absoluteVec(new Vec3(X, Floor, Z));
    }

    private static void provoke(LivingEntity Target, MtfTestPlayer User) {
        Target.invulnerableTime = 0;
        Target.hurt(User.damageSources().playerAttack(User), 0.5f);
    }

    public static final class FriendlyWatch {
        private final Set<UUID> Friends = new HashSet<>();
        private int Attempts;

        public void guard(Entity Friend) {
            Friends.add(Friend.getUUID());
        }

        public int attempts() {
            return Attempts;
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
        public void incoming(LivingIncomingDamageEvent Event) {
            if (Event.getSource().getEntity() instanceof MtfOperative && Friends.contains(Event.getEntity().getUUID())) {
                Attempts++;
            }
        }
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "mtf_no_sky")
    public static void refusesWithoutSky(GameTestHelper Helper) {
        MtfTestPlayer User = caller(Helper, Centre, Centre, "mtf_roofed");
        User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MtfRegistry.SignalRadio.get()));
        User.setItemInHand(InteractionHand.OFF_HAND, fragment(FragmentRarity.Volatile, 3));
        MtfSignalRadioItem.signal(User, InteractionHand.MAIN_HAND);
        Helper.runAfterDelay(5, () -> {
            Helper.assertTrue(User.getOffhandItem().getCount() == 3, "a fragment was spent without sky: " + User.getOffhandItem().getCount());
            Helper.assertFalse(MtfMissions.busy(User.getUUID()), "a squad was called under a roof");
            Helper.assertTrue(rides(Helper, User.getUUID()).isEmpty(), "a helicopter spawned under a roof");
            dismiss(Helper, User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 80, skyAccess = true, batch = "mtf_consume")
    public static void consumesFragmentAndPlansTier(GameTestHelper Helper) {
        MtfTestPlayer User = caller(Helper, Centre, Centre, "mtf_caller");
        User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MtfRegistry.SignalRadio.get()));
        User.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(RiftTechRegistry.ApocalypticFragment.get()));
        MtfSignalRadioItem.signal(User, InteractionHand.MAIN_HAND);
        Helper.assertTrue(User.getOffhandItem().is(RiftTechRegistry.ApocalypticFragment.get()), "the apocalyptic fragment was spent");
        Helper.assertFalse(MtfMissions.busy(User.getUUID()), "the apocalyptic fragment called a squad");
        User.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        MtfSignalRadioItem.signal(User, InteractionHand.MAIN_HAND);
        Helper.assertFalse(MtfMissions.busy(User.getUUID()), "a squad came without a fragment");
        User.setItemInHand(InteractionHand.OFF_HAND, fragment(FragmentRarity.Volatile, 3));
        MtfSignalRadioItem.signal(User, InteractionHand.MAIN_HAND);
        Helper.assertTrue(User.getOffhandItem().getCount() == 2, "expected one fragment spent, have " + User.getOffhandItem().getCount());
        MtfMission Mission = MtfMissions.get(User.getUUID());
        Helper.assertTrue(Mission != null && Mission.tier() == MtfTier.Volatile, "the mission tier does not match the fragment");
        List<MtfHelicopter> Rides = rides(Helper, User.getUUID());
        Helper.assertTrue(Rides.size() == 1, "expected one helicopter, found " + Rides.size());
        Helper.assertTrue(Rides.get(0).queued() == MtfTier.Volatile.size(), "the helicopter carries " + Rides.get(0).queued());
        Helper.assertTrue(squad(Helper, User.getUUID()).isEmpty(), "operatives appeared before the helicopter");
        User.getCooldowns().removeCooldown(MtfRegistry.SignalRadio.get());
        MtfSignalRadioItem.signal(User, InteractionHand.MAIN_HAND);
        Helper.assertTrue(User.getOffhandItem().getCount() == 2, "a second fragment was spent while a squad was out");
        Helper.assertTrue(rides(Helper, User.getUUID()).size() == 1, "a second helicopter was called");
        int[] Sizes = {2, 3, 5, 7, 10};
        for (MtfTier Tier : MtfTier.values()) {
            Helper.assertTrue(Tier.size() == Sizes[Tier.ordinal()], Tier + " has " + Tier.size() + " operatives");
            for (MtfRole Role : Tier.roles()) {
                Helper.assertTrue(NpcGunner.isGun(Role.weapon()), Role + " carries no usable gun");
            }
            Helper.assertTrue(MtfTier.fromFragment(fragment(Tier.rarity(), 1)) == Tier, "the " + Tier + " fragment maps elsewhere");
        }
        Helper.assertTrue(MtfTier.Abyssal.roles().contains(MtfRole.Bearer) && Scp127Item.is(MtfRole.Bearer.weapon()), "the abyssal squad has no SCP-127 bearer");
        Helper.assertTrue(MtfTier.Abyssal.roles().contains(MtfRole.Leader) && MtfRole.Leader.helmet().getItem() instanceof Scp148HelmetItem, "the abyssal leader has no telekill helmet");
        Helper.assertTrue(MtfTier.Faint.health() < MtfTier.Abyssal.health() && MtfTier.Faint.accuracy() < MtfTier.Abyssal.accuracy(), "tiers do not improve");
        dismiss(Helper, User);
        Helper.assertTrue(rides(Helper, User.getUUID()).isEmpty(), "the helicopter outlived the caller");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 80, skyAccess = true, batch = "mtf_cooldown")
    public static void radioWaitsAfterSquad(GameTestHelper Helper) {
        MtfTestPlayer User = caller(Helper, Centre, Centre, "mtf_cooldown");
        User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MtfRegistry.SignalRadio.get()));
        User.setItemInHand(InteractionHand.OFF_HAND, fragment(FragmentRarity.Volatile, 3));
        MtfSignalRadioItem.signal(User, InteractionHand.MAIN_HAND);
        Helper.assertTrue(MtfMissions.busy(User.getUUID()), "the first call was refused");
        MtfMissions.cancel(Helper.getLevel().getServer(), User.getUUID());
        Helper.assertTrue(MtfMissions.cooldownLeft(User) > 0L, "no cooldown after the squad left");
        User.getCooldowns().removeCooldown(MtfRegistry.SignalRadio.get());
        MtfSignalRadioItem.signal(User, InteractionHand.MAIN_HAND);
        Helper.assertFalse(MtfMissions.busy(User.getUUID()), "a squad was called during the cooldown");
        Helper.assertTrue(User.getOffhandItem().getCount() == 2, "a fragment was spent during the cooldown: " + User.getOffhandItem().getCount());
        User.setData(MtfRegistry.NextCall, 0L);
        MtfSignalRadioItem.signal(User, InteractionHand.MAIN_HAND);
        Helper.assertTrue(MtfMissions.busy(User.getUUID()), "the call was refused after the cooldown");
        dismiss(Helper, User);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 2400, skyAccess = true, batch = "mtf_rope_down")
    public static void squadArrivesAfterRopeDown(GameTestHelper Helper) {
        MtfTestPlayer User = caller(Helper, Centre, Centre, "mtf_lz");
        Helper.assertTrue(MtfMissions.call(User, MtfTier.Volatile) == MtfMissions.Outcome.Called, "the call was refused under open sky");
        boolean[] SawRope = {false};
        Helper.onEachTick(() -> {
            List<MtfOperative> Members = squad(Helper, User.getUUID());
            List<MtfHelicopter> Rides = rides(Helper, User.getUUID());
            if (!Rides.isEmpty() && (Rides.get(0).phase() == MtfHelicopter.PhaseApproach) && !Members.isEmpty() && !SawRope[0]) {
                Helper.fail("operatives appeared before the helicopter arrived");
            }
            for (MtfOperative Member : Members) {
                if (Member.state() == MtfOperative.StateRoping) {
                    SawRope[0] = true;
                    Helper.assertTrue(!Rides.isEmpty() && Rides.get(0).ropesDown(), "an operative is on a rope that is not down");
                }
            }
        });
        Helper.succeedWhen(() -> {
            MtfMission Mission = MtfMissions.get(User.getUUID());
            Helper.assertTrue(Mission != null && Mission.stage() == MtfMission.Stage.Active, "not on station yet");
            List<MtfOperative> Members = squad(Helper, User.getUUID());
            Helper.assertTrue(Members.size() == MtfTier.Volatile.size(), "on the ground: " + Members.size());
            for (MtfOperative Member : Members) {
                Helper.assertTrue(Member.onDuty() && Member.onGround(), "an operative is still roping");
            }
            Helper.assertTrue(SawRope[0], "nobody used the rope");
            dismiss(Helper, User);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 400, skyAccess = true, batch = "mtf_target")
    public static void targetsCallersTargetNeverFriends(GameTestHelper Helper) {
        MtfTestPlayer User = caller(Helper, Centre, Centre, "mtf_leader");
        MtfMissions.deployNow(User, MtfTier.Volatile);
        Cow Quarry = dummy(Helper, EntityType.COW, Centre + 8.0, Centre, Sturdy);
        Wolf Pet = dummy(Helper, EntityType.WOLF, Centre - 4.0, Centre + 3.0, 0.0f);
        Pet.tame(User);
        DClass Guard = dummy(Helper, DClassRegistry.DClassType.get(), Centre - 4.0, Centre - 3.0, 0.0f);
        provoke(Quarry, User);
        int[] Ticks = {0};
        Helper.onEachTick(() -> {
            if (++Ticks[0] % 80 == 0 && Quarry.isAlive()) {
                provoke(Quarry, User);
            }
            List<MtfOperative> Members = squad(Helper, User.getUUID());
            for (MtfOperative Member : Members) {
                LivingEntity Target = Member.getTarget();
                if (Target == User || Target == Pet || Target == Guard || Target instanceof MtfOperative) {
                    Helper.fail("an operative targeted a friend: " + Target.getName().getString());
                }
            }
        });
        Helper.succeedWhen(() -> {
            List<MtfOperative> Members = squad(Helper, User.getUUID());
            int Locked = 0;
            for (MtfOperative Member : Members) {
                if (Member.getTarget() == Quarry) {
                    Locked++;
                }
            }
            Helper.assertTrue(Locked >= 3, "operatives on the caller's target: " + Locked);
            Helper.assertTrue(Pet.getHealth() >= Pet.getMaxHealth() && Guard.getHealth() >= Guard.getMaxHealth(), "a friend was hurt");
            dismiss(Helper, User);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 400, skyAccess = true, batch = "mtf_hostiles")
    public static void engagesHostilesWhenIdle(GameTestHelper Helper) {
        MtfTestPlayer User = caller(Helper, Centre, Centre, "mtf_idle");
        MtfMissions.deployNow(User, MtfTier.Wavering);
        Husk Threat = dummy(Helper, EntityType.HUSK, Centre + 9.0, Centre - 2.0, Sturdy);
        Wolf Pet = dummy(Helper, EntityType.WOLF, Centre - 3.0, Centre + 3.0, 0.0f);
        Pet.tame(User);
        Helper.onEachTick(() -> {
            for (MtfOperative Member : squad(Helper, User.getUUID())) {
                LivingEntity Target = Member.getTarget();
                if (Target == User || Target == Pet) {
                    Helper.fail("an operative targeted a friend while idle");
                }
            }
        });
        Helper.succeedWhen(() -> {
            int Locked = 0;
            for (MtfOperative Member : squad(Helper, User.getUUID())) {
                if (Member.getTarget() == Threat) {
                    Locked++;
                }
            }
            Helper.assertTrue(Locked >= 2, "operatives on the hostile: " + Locked);
            dismiss(Helper, User);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 600, skyAccess = true, batch = "mtf_fire")
    public static void firesOnCallersTarget(GameTestHelper Helper) {
        MtfTestPlayer User = caller(Helper, Centre - 4.0, Centre, "mtf_spotter");
        MtfMissions.deployNow(User, MtfTier.Volatile);
        Cow Quarry = dummy(Helper, EntityType.COW, Centre + 7.0, Centre, Sturdy);
        FriendlyWatch Watch = new FriendlyWatch();
        Watch.guard(User);
        NeoForge.EVENT_BUS.register(Watch);
        provoke(Quarry, User);
        float Start = Quarry.getHealth();
        int[] Ticks = {0};
        Helper.onEachTick(() -> {
            if (++Ticks[0] % 80 == 0 && Quarry.isAlive()) {
                provoke(Quarry, User);
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Quarry.getLastHurtByMob() instanceof MtfOperative, "no operative hit the target");
            Helper.assertTrue(Quarry.getHealth() < Start - 2.0f || !Quarry.isAlive(), "the target barely took damage: " + Quarry.getHealth());
            Helper.assertTrue(Watch.attempts() == 0, "operatives shot the caller " + Watch.attempts() + " times");
            Helper.assertTrue(User.getHealth() >= User.getMaxHealth(), "the caller was hurt");
            NeoForge.EVENT_BUS.unregister(Watch);
            dismiss(Helper, User);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 700, skyAccess = true, batch = "mtf_lanes")
    public static void holdsFireOnFriendlyInLane(GameTestHelper Helper) {
        MtfTestPlayer User = caller(Helper, 2.5, 2.5, "mtf_lane");
        MtfMissions.deployNow(User, MtfTier.Faint);
        List<MtfOperative> Members = new ArrayList<>(squad(Helper, User.getUUID()));
        Helper.assertTrue(Members.size() == 2, "expected two operatives, found " + Members.size());
        double TargetX = 16.5;
        double TargetZ = 11.0;
        double[][] Posts = {{4.5, 12.5}, {4.5, 9.5}};
        for (int Index = 0; Index < 2; Index++) {
            MtfOperative Member = Members.get(Index);
            Vec3 Post = at(Helper, Posts[Index][0], Posts[Index][1]);
            Member.moveTo(Post.x, Post.y, Post.z, -90.0f, 0.0f);
            Member.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0);
        }
        Cow Quarry = dummy(Helper, EntityType.COW, TargetX, TargetZ, Sturdy);
        double MidX = (Posts[0][0] + TargetX) * 0.5;
        Vec3 CallerSpot = at(Helper, MidX, (Posts[0][1] + TargetZ) * 0.5);
        User.teleportTo(CallerSpot.x, CallerSpot.y, CallerSpot.z);
        DClass Guard = dummy(Helper, DClassRegistry.DClassType.get(), MidX, (Posts[1][1] + TargetZ) * 0.5, 0.0f);
        FriendlyWatch Watch = new FriendlyWatch();
        Watch.guard(User);
        Watch.guard(Guard);
        NeoForge.EVENT_BUS.register(Watch);
        provoke(Quarry, User);
        float Start = Quarry.getHealth();
        float[] Blocked = {0.0f};
        int[] Ticks = {0};
        Helper.onEachTick(() -> {
            Ticks[0]++;
            if (Ticks[0] % 60 == 0 && Quarry.isAlive()) {
                provoke(Quarry, User);
            }
            if (Watch.attempts() > 0) {
                NeoForge.EVENT_BUS.unregister(Watch);
                Helper.fail("an operative fired into a friendly in the lane");
            }
        });
        Helper.startSequence()
                .thenIdle(120)
                .thenExecute(() -> {
                    Helper.assertTrue(Quarry.getHealth() >= Start - 0.51f * (1 + Ticks[0] / 60), "a shot got through the blocked lanes");
                    Blocked[0] = Quarry.getHealth();
                    Vec3 Aside = at(Helper, MidX, 20.5);
                    User.teleportTo(Aside.x, Aside.y, Aside.z);
                    Vec3 Away = at(Helper, MidX, 2.5);
                    Guard.teleportTo(Away.x, Away.y, Away.z);
                })
                .thenWaitUntil(() -> {
                    Helper.assertTrue(Quarry.getLastHurtByMob() instanceof MtfOperative && Quarry.getHealth() < Blocked[0] - 2.0f, "no shots once the lanes opened");
                })
                .thenExecute(() -> {
                    Helper.assertTrue(Watch.attempts() == 0, "friendly fire attempts: " + Watch.attempts());
                    Helper.assertTrue(Guard.getHealth() >= Guard.getMaxHealth(), "the guard was hurt");
                    NeoForge.EVENT_BUS.unregister(Watch);
                    dismiss(Helper, User);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 3000, skyAccess = true, batch = "mtf_extract")
    public static void extractionRemovesSquad(GameTestHelper Helper) {
        MtfTestPlayer User = caller(Helper, Centre, Centre, "mtf_exfil");
        MtfMissions.deployNow(User, MtfTier.Wavering);
        MtfMission Mission = MtfMissions.get(User.getUUID());
        Helper.assertTrue(Mission != null && Mission.alive() == MtfTier.Wavering.size(), "the squad did not deploy");
        Helper.assertTrue(MtfMissions.requestExtraction(User), "extraction was refused");
        Helper.runAfterDelay(3, () -> {
            Helper.assertTrue(Mission.stage() == MtfMission.Stage.Extracting, "extraction did not start");
            Helper.assertTrue(rides(Helper, User.getUUID()).size() == 1, "no pickup helicopter");
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(MtfMissions.get(User.getUUID()) == null, "the mission is still open");
            Helper.assertTrue(squad(Helper, User.getUUID()).isEmpty(), "operatives are still on the ground");
            Helper.assertTrue(rides(Helper, User.getUUID()).isEmpty(), "the helicopter is still here");
            Helper.assertTrue(Mission.boarded() == MtfTier.Wavering.size(), "boarded " + Mission.boarded());
            dismiss(Helper, User);
        });
    }
}
