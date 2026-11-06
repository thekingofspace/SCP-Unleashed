package net.rai.scp_unleashed.scp352.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.scp352.Scp352Crone;
import net.rai.scp_unleashed.scp352.Scp352Leap;
import net.rai.scp_unleashed.scp352.Scp352Phase;
import net.rai.scp_unleashed.scp352.Scp352Registry;
import net.rai.scp_unleashed.scp352.Scp352Rest;
import net.rai.scp_unleashed.stasis.StasisPodItem;
import net.rai.scp_unleashed.stasis.StasisPods;
import net.rai.scp_unleashed.stasis.StasisRegistry;

import org.slf4j.Logger;

import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp352GameTests {
    public static final String Arena = "scp352_arena";
    public static final double WalkingPlayer = 0.2158;
    public static final double SprintingPlayer = 0.2806;
    private static final Logger Log = LogUtils.getLogger();

    private Scp352GameTests() {
    }

    private static final class Mortal extends FakePlayer {
        private Mortal(ServerLevel World, GameProfile Profile) {
            super(World, Profile);
        }

        @Override
        public boolean isInvulnerableTo(DamageSource Source) {
            return false;
        }

        @Override
        public void die(DamageSource Source) {
            if (CommonHooks.onLivingDeath(this, Source)) {
                return;
            }
            dead = true;
        }
    }

    private static String state(Scp352Crone Creature) {
        LivingEntity Target = Creature.getTarget();
        return " [phase " + Creature.phase() + ", target " + (Target == null ? "none" : Target.getName().getString()) + ", kills " + Creature.kills() + ", height "
                + Creature.getBbHeight() + ", at " + Creature.blockPosition() + ", rest left " + Creature.restLeft() + "]";
    }

    private static Scp352Crone crone(GameTestHelper Helper, double X, double Z) {
        Scp352Crone Creature = Helper.spawn(Scp352Registry.Crone.get(), new Vec3(X, 2.0, Z));
        Creature.setYRot(0.0f);
        return Creature;
    }

    private static Mortal mortal(GameTestHelper Helper, double X, double Z) {
        ServerLevel World = Helper.getLevel();
        Mortal User = new Mortal(World, new GameProfile(UUID.randomUUID(), "scp352_test"));
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, 2.0, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        return User;
    }

    private static DClass prisoner(GameTestHelper Helper, double X, double Z) {
        return Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(X, 2.0, Z));
    }

    private static void dismiss(GameTestHelper Helper, Player User) {
        Helper.getLevel().removePlayerImmediately((Mortal) User, Entity.RemovalReason.DISCARDED);
    }

    private static void tidy(GameTestHelper Helper) {
        Helper.killAllEntitiesOfClass(Scp352Crone.class);
        Helper.killAllEntitiesOfClass(DClass.class);
    }

    private static double flat(Vec3 From, Vec3 To) {
        double Dx = To.x - From.x;
        double Dz = To.z - From.z;
        return Math.sqrt(Dx * Dx + Dz * Dz);
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp352_stageOneIsSlow")
    public static void stageOneIsSlow(GameTestHelper Helper) {
        Scp352Crone Creature = crone(Helper, 4.5, 24.5);
        prisoner(Helper, 31.5, 24.5);
        Vec3[] Start = new Vec3[1];
        Helper.runAfterDelay(30, () -> Start[0] = Creature.position());
        Helper.runAfterDelay(110, () -> {
            double Speed = flat(Start[0], Creature.position()) / 80.0;
            Log.info("SCP-352 first stage drift speed {} blocks per tick", Speed);
            Helper.assertTrue(Creature.phase() == Scp352Phase.Wander, "SCP-352 left the first stage while the prisoner was out of range" + state(Creature));
            Helper.assertTrue(Speed > 0.015, "SCP-352 did not drift toward the person she senses, speed " + Speed + state(Creature));
            Helper.assertTrue(Speed < WalkingPlayer * 0.4, "SCP-352 is not slow in her first stage, speed " + Speed + state(Creature));
            Helper.assertTrue(Creature.getBbHeight() < 2.0f, "SCP-352 is already tall" + state(Creature));
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 140, batch = "scp352_playerTriggersStageTwo")
    public static void playerTriggersStageTwo(GameTestHelper Helper) {
        Scp352Crone Creature = crone(Helper, 10.5, 10.5);
        Mortal User = mortal(Helper, 21.5, 10.5);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Creature.phase() == Scp352Phase.Transform, "SCP-352 did not start to rise for a player ten blocks away" + state(Creature)))
                .thenExecute(() -> {
                    float Health = Creature.getHealth();
                    Creature.hurt(Helper.getLevel().damageSources().playerAttack(User), 30.0f);
                    Helper.assertTrue(Creature.getHealth() >= Health, "SCP-352 took damage while rising" + state(Creature));
                })
                .thenWaitUntil(() -> Helper.assertTrue(Creature.phase() == Scp352Phase.Hunt && Creature.getTarget() == User && Creature.getBbHeight() > 2.5f,
                        "SCP-352 is not hunting the player on her legs" + state(Creature)))
                .thenExecute(() -> {
                    dismiss(Helper, User);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 420, batch = "scp352_chasesAndKills")
    public static void chasesAndKills(GameTestHelper Helper) {
        Scp352Crone Creature = crone(Helper, 6.5, 6.5);
        DClass Prisoner = prisoner(Helper, 17.5, 16.5);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Creature.phase() == Scp352Phase.Hunt, "SCP-352 never rose to hunt the prisoner" + state(Creature)))
                .thenWaitUntil(() -> Helper.assertTrue(!Prisoner.isAlive(), "SCP-352 has not killed the prisoner" + state(Creature) + " prisoner health " + Prisoner.getHealth()))
                .thenExecute(() -> {
                    Helper.assertTrue(Creature.kills() == 1, "the kill was counted " + Creature.kills() + " times" + state(Creature));
                    Helper.assertTrue(Creature.phase() == Scp352Phase.Seek || Creature.phase() == Scp352Phase.Settle, "SCP-352 did not go to rest after the kill" + state(Creature));
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 520, batch = "scp352_killStartsRest")
    public static void killStartsRest(GameTestHelper Helper) {
        RandomSource Random = RandomSource.create(352L);
        for (int Roll = 0; Roll < 4000; Roll++) {
            long Length = Scp352Rest.roll(Random);
            Helper.assertTrue(Length >= Scp352Rest.Shortest && Length <= Scp352Rest.Longest, "a rest roll fell outside two to eight days: " + Length);
        }
        Scp352Crone Creature = crone(Helper, 20.5, 20.5);
        DClass Prisoner = prisoner(Helper, 23.5, 20.5);
        Vec3[] Kill = new Vec3[1];
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(!Prisoner.isAlive(), "SCP-352 has not killed the prisoner" + state(Creature)))
                .thenExecute(() -> Kill[0] = Prisoner.position())
                .thenWaitUntil(() -> Helper.assertTrue(Creature.phase() == Scp352Phase.Rest, "SCP-352 has not settled to rest after the kill" + state(Creature)))
                .thenExecute(() -> {
                    long Length = Creature.rest().length();
                    Helper.assertTrue(Length >= Scp352Rest.Shortest && Length <= Scp352Rest.Longest, "the rest lasts " + Length + " ticks, outside two to eight days");
                    Helper.assertTrue(Creature.restLeft() <= 8L * Scp352Rest.Day, "more than eight days of rest are left");
                    Helper.assertTrue(flat(Kill[0], Creature.position()) < 8.0, "SCP-352 went to rest " + flat(Kill[0], Creature.position()) + " blocks from the kill");
                    Helper.assertTrue(Creature.getBbHeight() < 1.5f, "SCP-352 is not crouched in her rest" + state(Creature));
                    Mortal User = mortal(Helper, Creature.getX() - Helper.absoluteVec(Vec3.ZERO).x + 2.0, Creature.getZ() - Helper.absoluteVec(Vec3.ZERO).z);
                    Helper.runAfterDelay(30, () -> {
                        Helper.assertTrue(Creature.phase() == Scp352Phase.Rest, "a player walking past woke SCP-352" + state(Creature));
                        dismiss(Helper, User);
                        tidy(Helper);
                        Helper.succeed();
                    });
                });
    }

    private static Scp352Crone reload(GameTestHelper Helper, Scp352Crone Creature) {
        CompoundTag Saved = new CompoundTag();
        Creature.saveWithoutId(Saved);
        Creature.discard();
        Scp352Crone Loaded = Scp352Registry.Crone.get().create(Helper.getLevel());
        Loaded.load(Saved);
        Loaded.setUUID(UUID.randomUUID());
        return Loaded;
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp352_restSurvivesReload")
    public static void restSurvivesReload(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        long Morning = World.getDayTime();
        Scp352Crone Creature = crone(Helper, 12.5, 12.5);
        Creature.restNow(3L * Scp352Rest.Day);
        Helper.assertTrue(Creature.phase() == Scp352Phase.Rest, "the rest command did not put SCP-352 to sleep" + state(Creature));
        long Before = Creature.restLeft();
        Scp352Crone Saved = reload(Helper, Creature);
        Helper.assertTrue(Saved.phase() == Scp352Phase.Rest, "the rest was lost in a save and load" + state(Saved));
        Helper.assertTrue(Math.abs(Saved.restLeft() - Before) <= 5L, "the rest changed length in a save and load: " + Before + " became " + Saved.restLeft());
        World.addFreshEntity(Saved);
        Scp352Crone[] Current = {Saved};
        Helper.runAfterDelay(30, () -> {
            Helper.assertTrue(Current[0].phase() == Scp352Phase.Rest, "the reloaded SCP-352 woke up early" + state(Current[0]));
            long Left = Current[0].restLeft();
            CompoundTag Unloaded = new CompoundTag();
            Current[0].saveWithoutId(Unloaded);
            Current[0].discard();
            World.setDayTime(World.getDayTime() + Scp352Rest.Day);
            Scp352Crone Back = Scp352Registry.Crone.get().create(World);
            Back.load(Unloaded);
            Back.setUUID(UUID.randomUUID());
            World.addFreshEntity(Back);
            Helper.assertTrue(Back.phase() == Scp352Phase.Rest, "SCP-352 woke when her chunk came back after one day" + state(Back));
            Helper.assertTrue(Math.abs(Left - Scp352Rest.Day - Back.restLeft()) <= 5L, "a day spent unloaded did not count: " + Left + " left before, " + Back.restLeft() + " after");
            Current[0] = Back;
            CompoundTag Again = new CompoundTag();
            Back.saveWithoutId(Again);
            Back.discard();
            World.setDayTime(World.getDayTime() + 3L * Scp352Rest.Day);
            Scp352Crone Late = Scp352Registry.Crone.get().create(World);
            Late.load(Again);
            Late.setUUID(UUID.randomUUID());
            World.addFreshEntity(Late);
            Current[0] = Late;
            Helper.runAfterDelay(5, () -> {
                Helper.assertTrue(Late.phase() == Scp352Phase.Wake || Late.phase() == Scp352Phase.Wander, "SCP-352 did not catch up on time spent unloaded" + state(Late));
                World.setDayTime(Morning);
                tidy(Helper);
                Helper.succeed();
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp352_wakesOnSchedule")
    public static void wakesOnSchedule(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        long Morning = World.getDayTime();
        Scp352Crone Creature = crone(Helper, 12.5, 12.5);
        Creature.restNow(2L * Scp352Rest.Day);
        Helper.runAfterDelay(10, () -> {
            Helper.assertTrue(Creature.phase() == Scp352Phase.Rest, "SCP-352 is not asleep" + state(Creature));
            World.setDayTime(World.getDayTime() + 2L * Scp352Rest.Day - 40L);
        });
        Helper.runAfterDelay(20, () -> Helper.assertTrue(Creature.phase() == Scp352Phase.Rest, "SCP-352 woke before her time" + state(Creature)));
        Helper.runAfterDelay(70, () -> Helper.assertTrue(Creature.phase() == Scp352Phase.Wake || Creature.phase() == Scp352Phase.Wander, "SCP-352 slept past her time" + state(Creature)));
        Helper.runAfterDelay(120, () -> {
            Helper.assertTrue(Creature.phase() == Scp352Phase.Wander, "SCP-352 did not go back to wandering" + state(Creature));
            Helper.assertTrue(!Creature.rest().active(), "the rest is still counting after she woke");
            World.setDayTime(Morning);
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp352_commandsWork")
    public static void commandsWork(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp352Crone Creature = crone(Helper, 12.5, 12.5);
        CommandSourceStack Source = World.getServer().createCommandSourceStack().withPosition(Creature.position()).withLevel(World).withPermission(4).withSuppressedOutput();
        World.getServer().getCommands().performPrefixedCommand(Source, "scp_unleashed scp352 rest 5");
        Helper.assertTrue(Creature.phase() == Scp352Phase.Rest, "the rest command failed" + state(Creature));
        Helper.assertTrue(Math.abs(Creature.rest().length() - 5L * Scp352Rest.Day) <= 1L, "the rest command set " + Creature.rest().length() + " ticks");
        World.getServer().getCommands().performPrefixedCommand(Source, "scp_unleashed scp352 status");
        World.getServer().getCommands().performPrefixedCommand(Source, "scp_unleashed scp352 wake");
        Helper.assertTrue(Creature.phase() == Scp352Phase.Wake, "the wake command failed" + state(Creature));
        Helper.runAfterDelay(50, () -> {
            Helper.assertTrue(Creature.phase() == Scp352Phase.Wander, "SCP-352 did not finish waking" + state(Creature));
            World.getServer().getCommands().performPrefixedCommand(Source, "scp_unleashed scp352 hunt");
            Helper.assertTrue(Creature.phase() == Scp352Phase.Transform, "the hunt command did not make her rise" + state(Creature));
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp352_ignoresVillagers")
    public static void ignoresVillagers(GameTestHelper Helper) {
        Scp352Crone Creature = crone(Helper, 12.5, 12.5);
        Villager Local = Helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(15.5, 2.0, 12.5));
        Pig Swine = Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(12.5, 2.0, 15.5));
        float VillagerHealth = Local.getHealth();
        float PigHealth = Swine.getHealth();
        Helper.runAfterDelay(80, () -> {
            Helper.assertTrue(Creature.phase() == Scp352Phase.Wander, "SCP-352 rose for a villager or a pig" + state(Creature));
            Helper.assertTrue(Creature.getTarget() == null, "SCP-352 targeted a villager or a pig" + state(Creature));
            Creature.beginHunt(null);
        });
        Helper.runAfterDelay(180, () -> {
            Helper.assertTrue(Creature.phase() == Scp352Phase.Hunt, "SCP-352 did not rise when told to hunt" + state(Creature));
            Helper.assertTrue(Creature.getTarget() == null, "a hunting SCP-352 chose a villager or a pig" + state(Creature));
            Helper.assertTrue(Local.getHealth() >= VillagerHealth && Swine.getHealth() >= PigHealth, "SCP-352 hurt a villager or a pig");
            tidy(Helper);
            Local.discard();
            Swine.discard();
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp352_controlledKillCountsOnce")
    public static void controlledKillCountsOnce(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp352Crone Creature = crone(Helper, 30.5, 30.5);
        Mortal User = mortal(Helper, 10.5, 10.5);
        DClass Body = prisoner(Helper, 12.5, 10.5);
        Helper.assertTrue(ControlManager.possess(User, Body), "the test player could not take control of the D-Class");
        Helper.assertTrue(ControlSessions.isControlling(User), "the test player is not controlling the D-Class");
        DamageSource Talons = World.damageSources().source(Scp352Registry.Hunt, Creature);
        User.setHealth(0.0f);
        User.die(Talons);
        Helper.assertTrue(ControlManager.hasPendingDeath(User), "the controlled death was not deferred");
        Helper.assertTrue(!User.isDeadOrDying(), "the controlling player died instead of the body");
        Helper.assertTrue(Creature.kills() == 0, "the deferred death counted before the body fell" + state(Creature));
        LivingEntity[] Corpse = new LivingEntity[1];
        Helper.runAfterDelay(1, () -> {
            Corpse[0] = ControlManager.processDeath(User, Restored -> {
            });
            Helper.assertTrue(Corpse[0] != null && !Corpse[0].isAlive(), "the D-Class body did not fall");
            Helper.assertTrue(Creature.kills() == 1, "the controlled D-Class kill counted " + Creature.kills() + " times" + state(Creature));
            Helper.assertTrue(Creature.phase() == Scp352Phase.Seek, "SCP-352 did not head off to rest" + state(Creature));
            ControlManager.processDeath(User);
            Helper.assertTrue(!Creature.claimKill(Corpse[0]), "the same body could be claimed twice");
            Helper.assertTrue(Creature.kills() == 1, "the kill counted again" + state(Creature));
            dismiss(Helper, User);
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp352_stasisOnlyWhileResting")
    public static void stasisOnlyWhileResting(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp352Crone Creature = crone(Helper, 12.5, 12.5);
        Player User = Helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack Pod = new ItemStack(StasisRegistry.podFor(FragmentRarity.Faint));
        Helper.assertTrue(StasisPods.refusal(User, Pod, Creature) != null, "a pod would take SCP-352 while she wanders");
        Helper.assertTrue(!StasisPods.capture(World, User, Pod, Creature), "a pod captured SCP-352 while she wandered");
        Creature.restNow(4L * Scp352Rest.Day);
        long Left = Creature.restLeft();
        Helper.assertTrue(StasisPods.refusal(User, Pod, Creature) == null, "a pod refuses a sleeping SCP-352");
        Helper.assertTrue(StasisPods.capture(World, User, Pod, Creature), "the pod failed to take a sleeping SCP-352");
        Helper.assertTrue(Creature.isRemoved() && StasisPodItem.isOccupied(Pod), "SCP-352 is still loose after capture");
        StasisPods.breakOut(World, Pod, Helper.absoluteVec(new Vec3(20.5, 2.0, 20.5)), null);
        Scp352Crone Released = World.getEntitiesOfClass(Scp352Crone.class, Helper.getBounds()).stream().findFirst().orElseThrow();
        Helper.assertTrue(Released.phase() == Scp352Phase.Rest, "SCP-352 woke when she left the pod" + state(Released));
        Helper.assertTrue(Math.abs(Released.restLeft() - Left) <= 5L, "the pod changed her rest: " + Left + " became " + Released.restLeft());
        tidy(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp352_hitWhileRestingRouses")
    public static void hitWhileRestingRouses(GameTestHelper Helper) {
        Scp352Crone Creature = crone(Helper, 12.5, 12.5);
        Mortal User = mortal(Helper, 14.5, 12.5);
        Creature.restNow(6L * Scp352Rest.Day);
        Helper.runAfterDelay(5, () -> {
            Creature.hurt(Helper.getLevel().damageSources().playerAttack(User), 2.0f);
            Helper.assertTrue(Creature.phase() == Scp352Phase.Rouse, "a blow did not wake SCP-352 in a rage" + state(Creature));
            Helper.assertTrue(!Creature.rest().active(), "her rest kept counting after she was woken");
        });
        Helper.runAfterDelay(40, () -> {
            Helper.assertTrue(Creature.phase() == Scp352Phase.Hunt && Creature.getTarget() == User, "SCP-352 did not turn on the attacker" + state(Creature));
            dismiss(Helper, User);
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp352_outrunsSprinter")
    public static void outrunsSprinter(GameTestHelper Helper) {
        Scp352Crone Creature = crone(Helper, 3.5, 24.5);
        DClass Prisoner = prisoner(Helper, 45.5, 24.5);
        Creature.beginHunt(Prisoner);
        Vec3[] Start = new Vec3[1];
        Helper.runAfterDelay(62, () -> Start[0] = Creature.position());
        Helper.runAfterDelay(82, () -> {
            double Speed = flat(Start[0], Creature.position()) / 20.0;
            Log.info("SCP-352 hunting run speed {} blocks per tick", Speed);
            Helper.assertTrue(Creature.phase() == Scp352Phase.Hunt, "SCP-352 is not hunting" + state(Creature));
            Helper.assertTrue(Speed > SprintingPlayer, "SCP-352 runs " + Speed + " blocks a tick, no faster than a sprinting player" + state(Creature));
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 320, batch = "scp352_crossesFences")
    public static void crossesFences(GameTestHelper Helper) {
        for (int X = 0; X < 48; X++) {
            Helper.setBlock(new BlockPos(X, 2, 20), Blocks.OAK_FENCE);
        }
        Scp352Crone Creature = crone(Helper, 24.5, 10.5);
        DClass Prisoner = prisoner(Helper, 24.5, 32.5);
        Creature.beginHunt(Prisoner);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Helper.relativeVec(Creature.position()).z > 21.0, "SCP-352 has not crossed the fence" + state(Creature));
            tidy(Helper);
        });
    }

    private static Mortal standing(GameTestHelper Helper, double X, double Y, double Z) {
        ServerLevel World = Helper.getLevel();
        Mortal User = new Mortal(World, new GameProfile(UUID.randomUUID(), "scp352_test"));
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Y, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        return User;
    }

    private static void fill(GameTestHelper Helper, int X0, int Y0, int Z0, int X1, int Y1, int Z1, Block Kind) {
        for (int X = X0; X <= X1; X++) {
            for (int Y = Y0; Y <= Y1; Y++) {
                for (int Z = Z0; Z <= Z1; Z++) {
                    Helper.setBlock(new BlockPos(X, Y, Z), Kind);
                }
            }
        }
    }

    private static double gap(Entity First, Entity Second) {
        return flat(First.position(), Second.position());
    }

    @GameTest(template = Arena, timeoutTicks = 260, batch = "scp352_leapsAtFleeingTarget", skyAccess = true)
    public static void leapsAtFleeingTarget(GameTestHelper Helper) {
        Scp352Crone Creature = crone(Helper, 4.5, 24.5);
        Mortal User = mortal(Helper, 16.5, 24.5);
        Creature.beginHunt(User);
        double[] Gaps = {-1.0, -1.0};
        Helper.onEachTick(() -> {
            if (Creature.phase() == Scp352Phase.Hunt && Helper.relativeVec(User.position()).x < 44.0 && Creature.leaps() == 0) {
                User.moveTo(User.getX() + SprintingPlayer, User.getY(), User.getZ(), 0.0f, 0.0f);
            }
            if (Gaps[0] < 0.0 && Creature.action() == Scp352Crone.ActionLeap) {
                Gaps[0] = gap(Creature, User);
            }
            if (Gaps[0] >= 0.0 && Gaps[1] < 0.0 && Creature.action() == Scp352Crone.ActionLand) {
                Gaps[1] = gap(Creature, User);
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Creature.leaps() >= 1, "SCP-352 did not leap at a fleeing target" + state(Creature) + " gap " + gap(Creature, User));
            Helper.assertTrue(Gaps[1] >= 0.0, "SCP-352 has not landed" + state(Creature));
            Helper.assertTrue(Gaps[1] < Gaps[0] - 3.0, "the leap did not close the distance: " + Gaps[0] + " before, " + Gaps[1] + " after");
            Log.info("SCP-352 catch-up leap closed the gap from {} to {} blocks", Gaps[0], Gaps[1]);
            dismiss(Helper, User);
            tidy(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 360, batch = "scp352_leapsOntoLedge", skyAccess = true)
    public static void leapsOntoLedge(GameTestHelper Helper) {
        fill(Helper, 22, 2, 20, 27, 5, 28, Blocks.STONE);
        Scp352Crone Creature = crone(Helper, 14.5, 24.5);
        Mortal User = standing(Helper, 25.5, 6.0, 24.5);
        Creature.beginHunt(User);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Helper.relativeVec(Creature.position()).y > 5.9 && Creature.onGround(), "SCP-352 did not leap onto the ledge" + state(Creature));
            Helper.assertTrue(Creature.leaps() >= 1, "SCP-352 reached the ledge without leaping" + state(Creature));
            dismiss(Helper, User);
            tidy(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp352_neverLeapsAsCroneOrResting")
    public static void neverLeapsAsCroneOrResting(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp352Crone Wanderer = crone(Helper, 4.5, 10.5);
        Scp352Crone Sleeper = crone(Helper, 4.5, 38.5);
        Mortal Far = mortal(Helper, 26.5, 10.5);
        Mortal Near = mortal(Helper, 13.5, 38.5);
        Sleeper.restNow(4L * Scp352Rest.Day);
        Helper.assertTrue(!Wanderer.beginLeap(World, Far, false), "a crone in her first stage was able to leap");
        Helper.assertTrue(!Sleeper.beginLeap(World, Near, false), "a sleeping SCP-352 was able to leap");
        Helper.runAfterDelay(150, () -> {
            Helper.assertTrue(Wanderer.phase() == Scp352Phase.Wander && Wanderer.leaps() == 0, "the wandering crone leapt" + state(Wanderer));
            Helper.assertTrue(Sleeper.phase() == Scp352Phase.Rest && Sleeper.leaps() == 0, "the sleeping SCP-352 leapt" + state(Sleeper));
            Wanderer.beginHunt(Far);
            Helper.assertTrue(Wanderer.phase() == Scp352Phase.Transform && !Wanderer.beginLeap(World, Far, false), "SCP-352 leapt while rising");
            dismiss(Helper, Far);
            dismiss(Helper, Near);
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp352_noFallDamageFromLeaping", skyAccess = true)
    public static void noFallDamageFromLeaping(GameTestHelper Helper) {
        fill(Helper, 5, 2, 21, 9, 7, 27, Blocks.STONE);
        ServerLevel World = Helper.getLevel();
        Scp352Crone Creature = Helper.spawn(Scp352Registry.Crone.get(), new Vec3(7.5, 8.0, 24.5));
        Mortal User = mortal(Helper, 17.5, 24.5);
        Creature.beginHunt(User);
        float Health = Creature.getHealth();
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Creature.phase() == Scp352Phase.Hunt, "SCP-352 never rose" + state(Creature)))
                .thenExecute(() -> Helper.assertTrue(Creature.beginLeap(World, User, false), "SCP-352 found no way to leap down from the tower" + state(Creature)))
                .thenWaitUntil(() -> Helper.assertTrue(Creature.leaps() >= 1 && !Creature.flying() && Helper.relativeVec(Creature.position()).y < 2.5 && Creature.onGround(),
                        "SCP-352 has not leapt down to the ground" + state(Creature)))
                .thenExecute(() -> {
                    Helper.assertTrue(Creature.getHealth() >= Health, "SCP-352 hurt herself leaping: " + Creature.getHealth() + " of " + Health);
                    dismiss(Helper, User);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp352_noLeapIntoLavaOrLowCeiling")
    public static void noLeapIntoLavaOrLowCeiling(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        fill(Helper, 30, 2, 2, 38, 2, 10, Blocks.LAVA);
        Scp352Crone Creature = crone(Helper, 22.5, 6.5);
        Vec3 Pool = Helper.absoluteVec(new Vec3(34.5, 2.0, 6.5));
        Helper.assertTrue(!Scp352Leap.landable(World, Creature, BlockPos.containing(Pool)), "a lava pool counts as a landing spot");
        Helper.assertTrue(Scp352Leap.plan(World, Creature, Pool).isEmpty(), "SCP-352 planned a leap into lava");
        fill(Helper, 0, 5, 18, 47, 5, 30, Blocks.STONE);
        Scp352Crone Low = crone(Helper, 4.5, 24.5);
        Vec3 Ahead = Helper.absoluteVec(new Vec3(16.5, 2.0, 24.5));
        Helper.assertTrue(Scp352Leap.plan(World, Low, Ahead).isEmpty(), "SCP-352 planned a leap under a three block ceiling");
        Mortal User = mortal(Helper, 16.5, 24.5);
        Low.beginHunt(User);
        Helper.onEachTick(() -> {
            if (Low.phase() == Scp352Phase.Hunt && Helper.relativeVec(User.position()).x < 44.0) {
                User.moveTo(User.getX() + SprintingPlayer, User.getY(), User.getZ(), 0.0f, 0.0f);
            }
        });
        Helper.runAfterDelay(170, () -> {
            Helper.assertTrue(Low.phase() == Scp352Phase.Hunt, "SCP-352 is not hunting under the ceiling" + state(Low));
            Helper.assertTrue(Low.leaps() == 0, "SCP-352 leapt under a low ceiling" + state(Low));
            dismiss(Helper, User);
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp352_deathKeepsTheFile")
    public static void deathKeepsTheFile(GameTestHelper Helper) {
        Scp352Crone Creature = crone(Helper, 12.5, 12.5);
        Creature.hurt(Helper.getLevel().damageSources().magic(), 10000.0f);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Creature.isRemoved(), "SCP-352 did not fall and fade");
            AnomalyDocument Document = AnomalyDocuments.server().get(Scp352Registry.DocumentId);
            Helper.assertTrue(Document != null && Document.Classification() == DocumentClass.Keter, "the SCP-352 file is no longer Keter after a death");
            tidy(Helper);
        });
    }
}
