package net.rai.scp_unleashed.scp3008.staff.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp3008.Scp3008Hours;
import net.rai.scp_unleashed.scp3008.staff.Scp3008Staff;
import net.rai.scp_unleashed.scp3008.staff.Scp3008StaffAnnouncer;
import net.rai.scp_unleashed.scp3008.staff.Scp3008StaffGoals;
import net.rai.scp_unleashed.scp3008.staff.Scp3008StaffRegistry;
import net.rai.scp_unleashed.scp3008.staff.Scp3008StaffSpawner;
import net.rai.scp_unleashed.scp3008.staff.Scp3008StaffVariant;
import net.rai.scp_unleashed.scp538.gametest.Scp538TestPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp3008StaffGameTests {
    public static final String Arena = "scp3008_staff_arena";
    public static final double Floor = 2.0;
    public static final double CapMin = 3.0;
    public static final double CapMax = 8.0;

    private Scp3008StaffGameTests() {
    }

    static Scp3008Staff staff(GameTestHelper Helper, double X, double Z, Scp3008StaffVariant Kind, @Nullable Boolean Night, boolean Still) {
        ServerLevel World = Helper.getLevel();
        Scp3008Staff Staff = Scp3008StaffRegistry.Staff.get().create(World);
        Vec3 Where = Helper.absoluteVec(new Vec3(X, Floor, Z));
        Staff.setVariant(Kind);
        Staff.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
        Staff.finalizeSpawn(World, World.getCurrentDifficultyAt(Staff.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        Staff.setPersistenceRequired();
        Staff.setNoAi(Still);
        Staff.setClockOverride(Night);
        World.addFreshEntity(Staff);
        return Staff;
    }

    static Scp538TestPlayer player(GameTestHelper Helper, double X, double Z, String Name, GameType Mode) {
        ServerLevel World = Helper.getLevel();
        Scp538TestPlayer User = new Scp538TestPlayer(World, Name);
        Vec3 Where = Helper.absoluteVec(new Vec3(X, Floor, Z));
        User.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(Mode);
        return User;
    }

    static void dismiss(GameTestHelper Helper, ServerPlayer... Users) {
        for (ServerPlayer User : Users) {
            if (!User.isRemoved()) {
                Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
            }
        }
    }

    static List<Scp3008Staff> present(GameTestHelper Helper) {
        AABB Box = new AABB(Helper.absolutePos(BlockPos.ZERO)).expandTowards(21.0, 6.0, 21.0).inflate(4.0);
        return Helper.getLevel().getEntitiesOfClass(Scp3008Staff.class, Box, Entity::isAlive);
    }

    static void sweep(GameTestHelper Helper) {
        for (Scp3008Staff Staff : present(Helper)) {
            Staff.discard();
        }
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_staff_variants")
    public static void everyVariantSpawns(GameTestHelper Helper) {
        Set<ResourceLocation> Models = new HashSet<>();
        Set<ResourceLocation> Textures = new HashSet<>();
        try {
            int Index = 0;
            for (Scp3008StaffVariant Kind : Scp3008StaffVariant.values()) {
                Scp3008Staff Staff = staff(Helper, 3.5 + Index * 2.5, 10.5, Kind, false, true);
                Index++;
                Helper.assertTrue(Staff.isAlive() && Staff.variant() == Kind, Kind.Name + " did not spawn as itself: " + Staff.variant());
                Helper.assertTrue(Math.abs(Staff.getBbHeight() - Kind.Height) < 0.01f && Math.abs(Staff.getBbWidth() - Kind.Width) < 0.01f,
                        Kind.Name + " has the wrong size " + Staff.getBbWidth() + " x " + Staff.getBbHeight());
                Helper.assertTrue(Staff.getMaxHealth() == (float) Kind.Health && Staff.getHealth() == Staff.getMaxHealth(), Kind.Name + " health " + Staff.getHealth() + "/" + Staff.getMaxHealth());
                Helper.assertTrue(Staff.getAttributeValue(Attributes.ATTACK_DAMAGE) == Kind.Damage, Kind.Name + " damage " + Staff.getAttributeValue(Attributes.ATTACK_DAMAGE));
                CompoundTag Saved = Staff.saveWithoutId(new CompoundTag());
                Scp3008Staff Copy = Scp3008StaffRegistry.Staff.get().create(Helper.getLevel());
                Copy.load(Saved);
                Helper.assertTrue(Copy.variant() == Kind && Math.abs(Copy.getBbHeight() - Kind.Height) < 0.01f, Kind.Name + " did not survive saving: " + Copy.variant());
                Models.add(Kind.model());
                Textures.add(Kind.texture());
            }
            Helper.assertTrue(Models.size() == Scp3008StaffVariant.values().length && Textures.size() == Scp3008StaffVariant.values().length,
                    "variants share geometry or textures");
        } finally {
            sweep(Helper);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp3008_staff_passive")
    public static void passiveByDay(GameTestHelper Helper) {
        Scp3008Staff Staff = staff(Helper, 10.5, 6.5, Scp3008StaffVariant.Lanky, false, false);
        Scp538TestPlayer User = player(Helper, 10.5, 11.5, "scp3008_day", GameType.SURVIVAL);
        Helper.startSequence().thenIdle(100).thenExecute(() -> {
            try {
                Helper.assertFalse(Staff.isHostile() || Staff.hostile(), "the staff turned hostile by day");
                Helper.assertTrue(Staff.getTarget() == null, "the staff targeted a customer by day: " + Staff.getTarget());
                Helper.assertTrue(User.getHealth() == User.getMaxHealth(), "the customer was hurt by day: " + User.getHealth());
            } finally {
                dismiss(Helper, User);
                sweep(Helper);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 260, batch = "scp3008_staff_hostile")
    public static void hostileAtNight(GameTestHelper Helper) {
        Scp3008Staff Staff = staff(Helper, 10.5, 5.5, Scp3008StaffVariant.Stout, true, false);
        Scp538TestPlayer User = player(Helper, 10.5, 12.5, "scp3008_night", GameType.SURVIVAL);
        Helper.startSequence().thenWaitUntil(() -> {
            Helper.assertTrue(Staff.isHostile(), "the staff did not turn hostile at night");
            Helper.assertTrue(Staff.getTarget() == User, "the staff is not hunting the customer: " + Staff.getTarget());
            Helper.assertTrue(User.getHealth() < User.getMaxHealth(), "the customer has not been hit yet: distance " + Staff.distanceTo(User) + ", in reach "
                    + Staff.isWithinMeleeAttackRange(User) + ", can swing " + Staff.canSwing() + ", sees " + Staff.getSensing().hasLineOfSight(User) + ", aggressive "
                    + Staff.isAggressive() + ", path done " + Staff.getNavigation().isDone() + ", at " + Staff.position() + " customer " + User.position());
        }).thenExecute(() -> {
            dismiss(Helper, User);
            sweep(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 260, batch = "scp3008_staff_dawn")
    public static void dawnCalmsThem(GameTestHelper Helper) {
        Scp3008Staff Staff = staff(Helper, 10.5, 5.5, Scp3008StaffVariant.Thorn, true, false);
        Scp538TestPlayer User = player(Helper, 10.5, 14.5, "scp3008_dawn", GameType.SURVIVAL);
        User.setInvulnerable(true);
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(Staff.getTarget() == User, "the staff never noticed the customer"))
                .thenExecute(() -> Staff.setClockOverride(false))
                .thenWaitUntil(() -> {
                    Helper.assertFalse(Staff.isHostile(), "the staff stayed hostile after dawn");
                    Helper.assertTrue(Staff.getTarget() == null, "the staff kept hunting after dawn: " + Staff.getTarget());
                })
                .thenExecute(() -> {
                    dismiss(Helper, User);
                    sweep(Helper);
                }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp3008_staff_creative")
    public static void creativeAndSpectatorsIgnored(GameTestHelper Helper) {
        Scp3008Staff Staff = staff(Helper, 10.5, 7.5, Scp3008StaffVariant.Lump, true, false);
        Scp538TestPlayer Builder = player(Helper, 8.5, 11.5, "scp3008_creative", GameType.CREATIVE);
        Scp538TestPlayer Ghost = player(Helper, 12.5, 11.5, "scp3008_spectator", GameType.SPECTATOR);
        Helper.assertFalse(Scp3008StaffGoals.isPrey(Builder) || Scp3008StaffGoals.isPrey(Ghost), "creative or spectator players count as prey");
        Helper.startSequence().thenIdle(120).thenExecute(() -> {
            try {
                Helper.assertTrue(Staff.isHostile(), "the staff was not hostile at night");
                Helper.assertTrue(Staff.getTarget() == null, "the staff targeted a creative or spectator player: " + Staff.getTarget());
                Helper.assertTrue(Builder.getHealth() == Builder.getMaxHealth(), "the creative player was hurt");
            } finally {
                dismiss(Helper, Builder, Ghost);
                sweep(Helper);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp3008_staff_announce")
    public static void announcementOncePerWave(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp3008StaffAnnouncer.forget(World);
        Scp3008Staff Speaker = staff(Helper, 10.5, 6.5, Scp3008StaffVariant.Stalk, true, true);
        Scp3008Staff Second = staff(Helper, 6.5, 8.5, Scp3008StaffVariant.Hunch, true, true);
        Scp538TestPlayer User = player(Helper, 10.5, 11.5, "scp3008_listener", GameType.SURVIVAL);
        try {
            long Time = World.getGameTime();
            int Before = Scp3008StaffAnnouncer.advance(World, false, Time);
            int First = Scp3008StaffAnnouncer.advance(World, true, Time + 1);
            Helper.assertTrue(First == Before + 1, "nightfall did not start a new wave: " + Before + " then " + First);
            Helper.assertTrue(Scp3008StaffAnnouncer.deliver(World, First, Time + 1) == 1, "the first wave did not reach the customer once");
            Helper.assertTrue(Scp3008StaffAnnouncer.heard(World, User, First), "the customer was not marked as having heard the wave");
            Helper.assertTrue(Speaker.announcing() || Second.announcing(), "no staff member made the announcement");
            for (int Repeat = 0; Repeat < 5; Repeat++) {
                Helper.assertTrue(Scp3008StaffAnnouncer.deliver(World, First, Time + 2 + Repeat) == 0, "the same wave was announced twice");
            }
            Helper.assertTrue(Scp3008StaffAnnouncer.advance(World, true, Time + 2) == First, "a wave started again before the interval passed");
            Helper.assertTrue(Scp3008StaffAnnouncer.advance(World, true, Time + 1 + Scp3008StaffAnnouncer.Interval / 2) == First, "a wave started at half the interval");
            int Next = Scp3008StaffAnnouncer.advance(World, true, Time + 1 + Scp3008StaffAnnouncer.Interval);
            Helper.assertTrue(Next == First + 1, "the next wave did not start after the interval: " + Next);
            Helper.assertTrue(Scp3008StaffAnnouncer.deliver(World, Next, Time + 2 + Scp3008StaffAnnouncer.Interval) == 1, "the next wave was not announced");
            Helper.assertTrue(Scp3008StaffAnnouncer.deliver(World, Next, Time + 3 + Scp3008StaffAnnouncer.Interval) == 0, "the next wave was announced twice");
            Helper.assertTrue(Scp3008StaffAnnouncer.advance(World, false, Time + 4 + Scp3008StaffAnnouncer.Interval) == Next, "dawn started a wave");
        } finally {
            Scp3008StaffAnnouncer.forget(World);
            dismiss(Helper, User);
            sweep(Helper);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp3008_staff_caps")
    public static void spawnCapsHold(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Helper.assertTrue(Scp3008StaffSpawner.allowedFor(false, 0, 0) == Scp3008StaffSpawner.DayPerPlayer, "the empty day cap");
        Helper.assertTrue(Scp3008StaffSpawner.allowedFor(true, 0, 0) == Scp3008StaffSpawner.NightPerPlayer, "the empty night cap");
        Helper.assertTrue(Scp3008StaffSpawner.NightPerPlayer > Scp3008StaffSpawner.DayPerPlayer && Scp3008StaffSpawner.NightLevelCap > Scp3008StaffSpawner.DayLevelCap,
                "night caps are not above day caps");
        Helper.assertTrue(Scp3008StaffSpawner.allowedFor(true, Scp3008StaffSpawner.NightPerPlayer, 0) == 0, "a full player cap still allowed a spawn");
        Helper.assertTrue(Scp3008StaffSpawner.allowedFor(false, 0, Scp3008StaffSpawner.DayLevelCap) == 0, "a full level cap still allowed a spawn");
        Helper.assertTrue(Scp3008StaffSpawner.allowedFor(true, 2, Scp3008StaffSpawner.NightLevelCap - 1) == 1, "the level cap did not limit the player cap");
        sweep(Helper);
        Scp538TestPlayer User = player(Helper, 10.5, 10.5, "scp3008_counter", GameType.SURVIVAL);
        try {
            Vec3 Centre = User.position();
            for (int Round = 0; Round < 20; Round++) {
                Scp3008StaffSpawner.populate(World, false, List.of(User), CapMin, CapMax);
            }
            int Day = Scp3008StaffSpawner.countNear(World, Centre, Scp3008StaffSpawner.CountRadius);
            Helper.assertTrue(Day > 0 && Day <= Scp3008StaffSpawner.DayPerPlayer, "day population " + Day + " broke the cap of " + Scp3008StaffSpawner.DayPerPlayer);
            for (Scp3008Staff Staff : present(Helper)) {
                Helper.assertTrue(Staff.storeBorn(), "a spawned staff member is not marked as store born");
                Helper.assertFalse(Staff.shouldBeSaved(), "store born staff would be saved with the chunk");
            }
            for (int Round = 0; Round < 30; Round++) {
                Scp3008StaffSpawner.populate(World, true, List.of(User), CapMin, CapMax);
            }
            int Night = Scp3008StaffSpawner.countNear(World, Centre, Scp3008StaffSpawner.CountRadius);
            Helper.assertTrue(Night > Day && Night <= Scp3008StaffSpawner.NightPerPlayer, "night population " + Night + " broke the cap of " + Scp3008StaffSpawner.NightPerPlayer);
        } finally {
            dismiss(Helper, User);
            sweep(Helper);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_staff_overworld")
    public static void neverSpawnsOutsideTheStore(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Helper.assertFalse(Scp3008Hours.inStore(World), "the test level counts as the store");
        Helper.assertFalse(Scp3008Hours.night(World), "the test level reports store night");
        sweep(Helper);
        Scp538TestPlayer User = player(Helper, 10.5, 10.5, "scp3008_outside", GameType.SURVIVAL);
        try {
            int Before = Scp3008StaffSpawner.countAll(World);
            for (int Round = 0; Round < 200; Round++) {
                Scp3008StaffSpawner.tick(World);
            }
            Helper.assertTrue(Scp3008StaffSpawner.countAll(World) == Before, "staff spawned outside the store");
        } finally {
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp3008_staff_shoved")
    public static void shovedAndStruckByDay(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp3008Staff Staff = staff(Helper, 10.5, 9.5, Scp3008StaffVariant.Hunch, false, false);
        Scp538TestPlayer User = player(Helper, 10.5, 10.6, "scp3008_shover", GameType.SURVIVAL);
        User.setInvulnerable(true);
        try {
            for (int Push = 0; Push < Scp3008Staff.ShoveLimit; Push++) {
                Staff.push(User);
            }
            Helper.assertTrue(Staff.stareFocus() == User, "the shoved staff member did not turn to stare");
            Helper.assertTrue(Staff.getTarget() == null, "a shove alone made the staff attack");
            Staff.hurt(World.damageSources().playerAttack(User), 1.0f);
            Helper.assertTrue(User.getUUID().equals(Staff.provoker()) && Staff.getTarget() == User, "striking the staff by day did not provoke it");
            Helper.assertTrue(Staff.engaged(), "the provoked staff member is not engaged");
        } finally {
            dismiss(Helper, User);
            sweep(Helper);
        }
        Helper.succeed();
    }
}
