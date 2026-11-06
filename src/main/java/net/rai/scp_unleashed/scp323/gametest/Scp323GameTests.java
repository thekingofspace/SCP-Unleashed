package net.rai.scp_unleashed.scp323.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;
import net.rai.scp_unleashed.antimemetic.AntiMemeticRegistry;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.control.ControlRefusing;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.scp148.Scp148Registry;
import net.rai.scp_unleashed.scp148.Scp148Shield;
import net.rai.scp_unleashed.scp323.Scp323Beast;
import net.rai.scp_unleashed.scp323.Scp323Bond;
import net.rai.scp_unleashed.scp323.Scp323Carry;
import net.rai.scp_unleashed.scp323.Scp323Hosts;
import net.rai.scp_unleashed.scp323.Scp323Lure;
import net.rai.scp_unleashed.scp323.Scp323Night;
import net.rai.scp_unleashed.scp323.Scp323Registry;
import net.rai.scp_unleashed.scp323.Scp323Skull;
import net.rai.scp_unleashed.scp323.Scp323SkullItem;
import net.rai.scp_unleashed.scp323.Scp323Watch;
import net.rai.scp_unleashed.stasis.StasisPods;
import net.rai.scp_unleashed.stasis.StasisRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp323GameTests {
    public static final String Arena = "scp323_arena";
    public static final long Noon = 6000L;
    public static final long Midnight = 18000L;
    public static final int LureLift = 140;

    private Scp323GameTests() {
    }

    private static ServerPlayer player(GameTestHelper Helper, double X, double Z, String Name) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), Name));
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, 1.0, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(GameType.SURVIVAL);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, ServerPlayer User) {
        Scp323Bond.clear(User);
        Scp323Watch.stop(User);
        Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
    }

    private static AABB area(GameTestHelper Helper) {
        return new AABB(Helper.absoluteVec(new Vec3(-4.0, -2.0, -4.0)), Helper.absoluteVec(new Vec3(36.0, 10.0, 36.0)));
    }

    private static List<Scp323Beast> beasts(GameTestHelper Helper) {
        return Helper.getLevel().getEntitiesOfClass(Scp323Beast.class, area(Helper), Entity::isAlive);
    }

    private static List<Scp323Skull> skulls(GameTestHelper Helper) {
        return Helper.getLevel().getEntitiesOfClass(Scp323Skull.class, area(Helper), Entity::isAlive);
    }

    private static int droppedSkullItems(GameTestHelper Helper) {
        return Helper.getLevel().getEntitiesOfClass(ItemEntity.class, area(Helper), Dropped -> Scp323SkullItem.is(Dropped.getItem())).size();
    }

    private static void tidy(GameTestHelper Helper) {
        for (Scp323Beast Beast : beasts(Helper)) {
            Beast.vanish();
        }
        for (Scp323Skull Skull : skulls(Helper)) {
            Skull.discard();
        }
        for (Entity Other : Helper.getLevel().getEntitiesOfClass(LivingEntity.class, area(Helper), Found -> !(Found instanceof ServerPlayer))) {
            Other.discard();
        }
    }

    private static void setTime(GameTestHelper Helper, long Hour) {
        ServerLevel Over = Helper.getLevel().getServer().overworld();
        long Day = Math.floorDiv(Over.getDayTime(), 24000L);
        Over.setDayTime(Day * 24000L + Hour);
    }

    private static Scp323Bond horned(boolean FirstDone) {
        return new Scp323Bond(Scp323Bond.Horned, Util.NIL_UUID, -1L, false, 0, FirstDone, true);
    }

    private static void command(GameTestHelper Helper, String Line) {
        MinecraftServer Server = Helper.getLevel().getServer();
        Server.getCommands().performPrefixedCommand(Server.createCommandSourceStack().withSuppressedOutput(), Line);
    }

    private static String describe(LivingEntity Host) {
        Scp323Bond Bond = Scp323Bond.of(Host);
        return " [stage " + Bond.Stage() + ", first done " + Bond.FirstDone() + ", last night " + Bond.LastNight() + ", interrupted " + Bond.Interrupted() + ", armed " + Bond.Armed() + ", time " + Scp323Night.time(Host.level()) + "]";
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp323_wearTransformsSurvival")
    public static void wearTransformsSurvival(GameTestHelper Helper) {
        setTime(Helper, Midnight);
        ServerPlayer User = player(Helper, 8.5, 8.5, "scp323_survivor");
        User.setItemSlot(EquipmentSlot.HEAD, Scp323SkullItem.fresh());
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Scp323Bond.isTransformed(User), "a survival player wearing SCP-323 did not transform" + describe(User));
            Helper.assertTrue(User.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "the skull stayed on the head instead of fusing");
            Helper.assertTrue(beasts(Helper).size() == 1, "expected one SCP-323-1, found " + beasts(Helper).size());
            Helper.assertTrue(User.getUUID().equals(beasts(Helper).get(0).hostPlayer()), "SCP-323-1 is not bound to its wearer");
            Helper.assertTrue(Scp323Watch.isWatching(User), "the wearer is not watching through SCP-323-1");
            dismiss(Helper, User);
            tidy(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp323_creativeStaysAHat")
    public static void creativeStaysAHat(GameTestHelper Helper) {
        setTime(Helper, Midnight);
        ServerPlayer User = player(Helper, 8.5, 8.5, "scp323_builder");
        User.setGameMode(GameType.CREATIVE);
        User.setItemSlot(EquipmentSlot.HEAD, Scp323SkullItem.fresh());
        Helper.runAfterDelay(40, () -> {
            Helper.assertTrue(!Scp323Bond.isBound(User), "a creative player was bound to SCP-323" + describe(User));
            Helper.assertTrue(Scp323SkullItem.is(User.getItemBySlot(EquipmentSlot.HEAD)), "the creative player's skull hat disappeared");
            Helper.assertTrue(beasts(Helper).isEmpty(), "SCP-323-1 appeared for a creative player");
            dismiss(Helper, User);
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp323_beastDeathKillsHost")
    public static void beastDeathKillsHost(GameTestHelper Helper) {
        setTime(Helper, Noon);
        ServerPlayer User = player(Helper, 8.5, 8.5, "scp323_doomed");
        Helper.assertTrue(Scp323Hosts.fuse(User), "fusing the skull failed");
        Scp323Beast Beast = beasts(Helper).get(0);
        Helper.runAfterDelay(5, () -> {
            Beast.hurt(Beast.damageSources().genericKill(), 10000.0f);
            Helper.runAfterDelay(5, () -> {
                Helper.assertTrue(!Scp323Bond.isBound(User), "the host is still bound after SCP-323-1 died" + describe(User));
                Helper.assertTrue(User.getHealth() <= 0.0f, "the host survived the death of SCP-323-1 (health " + User.getHealth() + ")");
                Helper.assertTrue(!Scp323Watch.isWatching(User), "the host is still watching a dead body");
                Helper.assertTrue(skulls(Helper).size() == 1 && droppedSkullItems(Helper) == 0, "expected exactly one dropped skull, found " + skulls(Helper).size() + " skulls and "
                        + droppedSkullItems(Helper) + " skull items");
                Beast.die(Beast.damageSources().genericKill());
                Helper.runAfterDelay(2, () -> {
                    Helper.assertTrue(skulls(Helper).size() == 1, "a second death call duplicated the skull");
                    dismiss(Helper, User);
                    tidy(Helper);
                    Helper.succeed();
                });
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp323_targetsClosest")
    public static void targetsClosest(GameTestHelper Helper) {
        setTime(Helper, Midnight);
        Scp323Beast Beast = Helper.spawn(Scp323Registry.Beast.get(), new Vec3(4.5, 1.0, 16.5));
        Villager Near = Helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(9.5, 1.0, 16.5));
        Villager Far = Helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(20.5, 1.0, 16.5));
        Near.setInvulnerable(true);
        Far.setInvulnerable(true);
        Helper.runAfterDelay(30, () -> {
            Helper.assertTrue(Beast.getTarget() == Near, "SCP-323-1 did not pick the closest person: target " + (Beast.getTarget() == null ? "none" : Beast.getTarget().getName().getString()));
            Vec3 Away = Helper.absoluteVec(new Vec3(28.5, 1.0, 28.5));
            Near.teleportTo(Away.x, Away.y, Away.z);
            Vec3 Beside = Beast.position().add(1.5, 0.0, 0.0);
            Villager Closer = EntityType.VILLAGER.create(Helper.getLevel());
            Closer.moveTo(Beside.x, Beside.y, Beside.z, 0.0f, 0.0f);
            Closer.setNoAi(true);
            Closer.setInvulnerable(true);
            Helper.getLevel().addFreshEntity(Closer);
            Helper.runAfterDelay(25, () -> {
                Helper.assertTrue(Beast.getTarget() == Closer, "SCP-323-1 did not switch to a clearly closer person: target "
                        + (Beast.getTarget() == null ? "none" : Beast.getTarget().getName().getString()));
                tidy(Helper);
                setTime(Helper, Noon);
                Helper.succeed();
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 260, batch = "scp323_retractsAfterQuota")
    public static void retractsAfterQuota(GameTestHelper Helper) {
        setTime(Helper, Noon);
        ServerPlayer User = player(Helper, 8.5, 8.5, "scp323_hungry");
        Helper.assertTrue(Scp323Hosts.fuse(User), "fusing the skull failed");
        Scp323Beast Beast = beasts(Helper).get(0);
        Helper.runAfterDelay(45, () -> {
            for (int Index = 0; Index < Scp323Hosts.KillQuota; Index++) {
                Villager Victim = Helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(12.5 + Index, 1.0, 8.5));
                Victim.hurt(Beast.damageSources().source(Scp323Registry.Claw, Beast), 1000.0f);
            }
            Helper.assertTrue(Beast.kills() == Scp323Hosts.KillQuota, "kills were not counted: " + Beast.kills());
            Helper.succeedWhen(() -> {
                Helper.assertTrue(!Beast.isAlive() || Beast.isRemoved(), "SCP-323-1 did not retract after its first kills");
                Helper.assertTrue(Scp323Bond.isHorned(User), "the host is not left horned" + describe(User));
                Helper.assertTrue(Scp323Bond.of(User).FirstDone(), "the first episode was not marked done" + describe(User));
                Helper.assertTrue(!Scp323Watch.isWatching(User), "the host is still watching after the retract");
                Helper.assertTrue(skulls(Helper).isEmpty(), "a skull dropped although nobody died");
                dismiss(Helper, User);
                tidy(Helper);
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 320, batch = "scp323_nightCycle")
    public static void nightCycle(GameTestHelper Helper) {
        setTime(Helper, Noon);
        ServerPlayer User = player(Helper, 8.5, 8.5, "scp323_nightwalker");
        Scp323Bond.set(User, horned(true));
        Helper.runAfterDelay(30, () -> {
            Helper.assertTrue(Scp323Bond.isHorned(User), "the host changed during the day" + describe(User));
            setTime(Helper, 13200L);
            Helper.runAfterDelay(30, () -> {
                Helper.assertTrue(Scp323Bond.isTransformed(User), "nightfall did not regrow SCP-323-1" + describe(User));
                Helper.assertTrue(beasts(Helper).size() == 1, "expected one SCP-323-1 at night");
                setTime(Helper, 24000L + 1000L);
                Helper.succeedWhen(() -> {
                    Helper.assertTrue(beasts(Helper).isEmpty(), "dawn did not send SCP-323-1 back");
                    Helper.assertTrue(Scp323Bond.isHorned(User), "the host is not horned again after dawn" + describe(User));
                    dismiss(Helper, User);
                    tidy(Helper);
                    setTime(Helper, Noon);
                });
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 1200, batch = "scp323_commandTimeCycles")
    public static void commandTimeCycles(GameTestHelper Helper) {
        ServerPlayer User = player(Helper, 8.5, 8.5, "scp323_insomniac");
        List<UUID> Creatures = new ArrayList<>();
        GameTestSequence Steps = Helper.startSequence();
        Steps.thenExecute(() -> {
            command(Helper, "time set day");
            Scp323Bond.set(User, horned(true));
        });
        for (int Cycle = 1; Cycle <= 3; Cycle++) {
            int Round = Cycle;
            Steps.thenIdle(20);
            Steps.thenExecute(() -> {
                Helper.assertTrue(Scp323Bond.isHorned(User) && beasts(Helper).isEmpty(), "the host changed during the day in cycle " + Round + describe(User));
                command(Helper, "time set midnight");
            });
            Steps.thenWaitUntil(() -> {
                Helper.assertTrue(Scp323Bond.isTransformed(User), "midnight did not regrow SCP-323-1 in cycle " + Round + describe(User));
                Helper.assertTrue(beasts(Helper).size() == 1, "expected one SCP-323-1 in cycle " + Round);
            });
            Steps.thenExecute(() -> {
                UUID Creature = Scp323Bond.of(User).Creature();
                Helper.assertTrue(!Creatures.contains(Creature), "cycle " + Round + " reused an old SCP-323-1");
                Creatures.add(Creature);
                command(Helper, "time set day");
            });
            Steps.thenWaitUntil(() -> {
                Helper.assertTrue(beasts(Helper).isEmpty(), "day did not send SCP-323-1 back in cycle " + Round);
                Helper.assertTrue(Scp323Bond.isHorned(User), "the host is not horned again in cycle " + Round + describe(User));
            });
        }
        Steps.thenExecute(() -> {
            Helper.assertTrue(Creatures.size() == 3, "expected three nightly changes, saw " + Creatures.size());
            dismiss(Helper, User);
            tidy(Helper);
            setTime(Helper, Noon);
        });
        Steps.thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 900, batch = "scp323_skippedDayIsNewNight")
    public static void skippedDayIsNewNight(GameTestHelper Helper) {
        ServerPlayer User = player(Helper, 8.5, 8.5, "scp323_skipper");
        UUID[] First = new UUID[1];
        GameTestSequence Steps = Helper.startSequence();
        Steps.thenExecute(() -> {
            command(Helper, "time set midnight");
            long Phase = Scp323Night.nightId(Scp323Night.time(Helper.getLevel()));
            Scp323Bond.set(User, new Scp323Bond(Scp323Bond.Horned, Util.NIL_UUID, Phase, false, 0, true, false));
        });
        Steps.thenIdle(40);
        Steps.thenExecute(() -> {
            Helper.assertTrue(Scp323Bond.isHorned(User) && beasts(Helper).isEmpty(), "the host changed twice in one night" + describe(User));
            command(Helper, "time add 24000");
        });
        Steps.thenWaitUntil(() -> Helper.assertTrue(Scp323Bond.isTransformed(User) && beasts(Helper).size() == 1, "a full day skipped at night did not count as a new night" + describe(User)));
        Steps.thenExecute(() -> {
            First[0] = Scp323Bond.of(User).Creature();
            command(Helper, "time add 24000");
        });
        Steps.thenWaitUntil(() -> {
            Scp323Bond Bond = Scp323Bond.of(User);
            Helper.assertTrue(Bond.transformed() && !Bond.Creature().equals(First[0]), "a second skipped day did not bring a new night" + describe(User));
            Helper.assertTrue(beasts(Helper).size() == 1, "expected one SCP-323-1 after the second skip, saw " + beasts(Helper).size());
        });
        Steps.thenExecute(() -> {
            dismiss(Helper, User);
            tidy(Helper);
            setTime(Helper, Noon);
        });
        Steps.thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 900, batch = "scp323_longNightOnce")
    public static void longNightOnce(GameTestHelper Helper) {
        ServerPlayer User = player(Helper, 8.5, 8.5, "scp323_longnight");
        UUID[] Changed = new UUID[1];
        GameTestSequence Steps = Helper.startSequence();
        Steps.thenExecute(() -> {
            setTime(Helper, 13500L);
            Scp323Bond.set(User, horned(true));
        });
        Steps.thenWaitUntil(() -> Helper.assertTrue(Scp323Bond.isTransformed(User), "nightfall did not regrow SCP-323-1" + describe(User)));
        Steps.thenExecute(() -> Changed[0] = Scp323Bond.of(User).Creature());
        Steps.thenIdle(200);
        Steps.thenExecute(() -> {
            Helper.assertTrue(beasts(Helper).size() == 1 && Changed[0].equals(Scp323Bond.of(User).Creature()), "the night fired more than once while SCP-323-1 was out" + describe(User));
            beasts(Helper).get(0).beginRetract();
        });
        Steps.thenWaitUntil(() -> Helper.assertTrue(beasts(Helper).isEmpty() && Scp323Bond.isHorned(User), "SCP-323-1 did not retract" + describe(User)));
        for (int Step = 0; Step < 4; Step++) {
            long Hour = 15000L + Step * 2000L;
            Steps.thenExecute(() -> setTime(Helper, Hour));
            Steps.thenIdle(80);
            Steps.thenExecute(() -> Helper.assertTrue(beasts(Helper).isEmpty() && Scp323Bond.isHorned(User), "the same night fired again at " + Hour + describe(User)));
        }
        Steps.thenExecute(() -> {
            dismiss(Helper, User);
            tidy(Helper);
            setTime(Helper, Noon);
        });
        Steps.thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp323_stateSurvivesSave")
    public static void stateSurvivesSave(GameTestHelper Helper) {
        Scp323Bond Bond = new Scp323Bond(Scp323Bond.Transformed, UUID.randomUUID(), 42L, true, 1, false, false);
        Tag Encoded = Scp323Bond.BondCodec.encodeStart(NbtOps.INSTANCE, Bond).getOrThrow();
        Helper.assertTrue(Bond.equals(Scp323Bond.BondCodec.parse(NbtOps.INSTANCE, Encoded).getOrThrow()), "the bond did not survive a codec round trip");
        DClass Subject = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(8.5, 1.0, 8.5));
        Scp323Bond.set(Subject, horned(true));
        CompoundTag Saved = Subject.saveWithoutId(new CompoundTag());
        Saved.putString("id", "scp_unleashed:dclass");
        Subject.discard();
        Entity Loaded = EntityType.create(Saved, Helper.getLevel()).orElse(null);
        Helper.assertTrue(Loaded != null && Scp323Bond.isHorned(Loaded), "a horned D-Class lost its horns through save and load");
        Scp323Beast Beast = Helper.spawn(Scp323Registry.Beast.get(), new Vec3(16.5, 1.0, 16.5));
        UUID Host = UUID.randomUUID();
        Beast.prepare(false, 1, true, 42L);
        Beast.bindBody(Host, new CompoundTag(), "Test");
        CompoundTag BeastTag = Beast.saveWithoutId(new CompoundTag());
        Scp323Beast Copy = Scp323Registry.Beast.get().create(Helper.getLevel());
        Helper.assertTrue(Copy != null, "could not make a second SCP-323-1");
        Copy.load(BeastTag);
        Helper.assertTrue(Host.equals(Copy.hostBody()) && Copy.kills() == 1 && !Copy.firstEpisode(), "SCP-323-1 lost its host link through save and load");
        tidy(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp323_dclassTransformsAndDrops")
    public static void dclassTransformsAndDrops(GameTestHelper Helper) {
        setTime(Helper, Midnight);
        DClass Subject = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(8.5, 1.0, 8.5));
        UUID Id = Subject.getUUID();
        Subject.setItemSlot(EquipmentSlot.HEAD, Scp323SkullItem.fresh());
        Helper.runAfterDelay(20, () -> {
            Helper.assertTrue(beasts(Helper).size() == 1, "a D-Class wearing SCP-323 did not transform");
            Scp323Beast Beast = beasts(Helper).get(0);
            Helper.assertTrue(Id.equals(Beast.hostBody()) && Beast.storedBody() != null, "SCP-323-1 does not carry the D-Class body");
            Helper.assertTrue(Helper.getLevel().getEntity(Id) == null, "the D-Class body is still walking around");
            Beast.hurt(Beast.damageSources().genericKill(), 10000.0f);
            Helper.runAfterDelay(30, () -> {
                Helper.assertTrue(skulls(Helper).size() == 1 && droppedSkullItems(Helper) == 0, "expected exactly one skull after the D-Class died, found " + skulls(Helper).size());
                Entity Body = Helper.getLevel().getEntity(Id);
                Helper.assertTrue(Body == null || !Body.isAlive(), "the D-Class host survived the death of SCP-323-1");
                tidy(Helper);
                Helper.succeed();
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp323_controlRules")
    public static void controlRules(GameTestHelper Helper) {
        setTime(Helper, Noon);
        ServerPlayer User = player(Helper, 8.5, 8.5, "scp323_operator");
        Scp323Beast Beast = Helper.spawn(Scp323Registry.Beast.get(), new Vec3(10.5, 1.0, 8.5));
        Helper.assertTrue(Beast instanceof ControlRefusing Refusing && Refusing.controlRefusal() != null, "SCP-323-1 does not refuse control with a message");
        Helper.assertTrue(!ControlManager.possess(User, Beast), "control of a transformed body was allowed");
        DClass Subject = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(9.5, 1.0, 10.5));
        Scp323Bond.set(Subject, horned(true));
        UUID Id = Subject.getUUID();
        boolean Linked = ControlManager.possess(User, Subject);
        Helper.assertTrue(Linked && ControlSessions.isControlling(User), "control of a horned daytime D-Class was refused");
        Helper.assertTrue(Scp323Bond.isHorned(User), "the controller did not inherit the horned body");
        ControlManager.release(User);
        Helper.runAfterDelay(5, () -> {
            Helper.assertTrue(!ControlSessions.isControlling(User), "release failed");
            Entity Back = Helper.getLevel().getEntity(Id);
            Helper.assertTrue(Back != null && Scp323Bond.isHorned(Back), "the released D-Class lost its horns");
            ItemStack Pod = new ItemStack(StasisRegistry.Pods.values().iterator().next().get());
            Helper.assertTrue(StasisPods.refusal(User, Pod, Beast) != null, "a stasis pod accepted SCP-323-1");
            Helper.assertTrue(StasisPods.refusal(User, Pod, Back) != null, "a stasis pod accepted a horned D-Class");
            Helper.assertTrue(StasisPods.refusal(User, Pod, User) != null, "a stasis pod accepted a player");
            dismiss(Helper, User);
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp323_cureRules")
    public static void cureRules(GameTestHelper Helper) {
        setTime(Helper, Noon);
        ServerPlayer Marked = player(Helper, 6.5, 6.5, "scp323_marked");
        Scp323Bond.set(Marked, horned(true));
        Helper.assertTrue(AnomalyCure.cure(Marked, null), "the cure did nothing to a horned host by day");
        Helper.assertTrue(!Scp323Bond.isBound(Marked), "the horns survived the cure");
        Helper.assertTrue(skulls(Helper).size() == 1, "the cure did not eject exactly one skull");
        ServerPlayer Changed = player(Helper, 20.5, 20.5, "scp323_changed");
        Helper.assertTrue(Scp323Hosts.fuse(Changed), "fusing the skull failed");
        Helper.runAfterDelay(5, () -> {
            Helper.assertTrue(!AnomalyCure.cure(Changed, null), "the cure reported success on a transformed host");
            Helper.assertTrue(Scp323Bond.isTransformed(Changed), "the cure changed a transformed host");
            dismiss(Helper, Marked);
            dismiss(Helper, Changed);
            tidy(Helper);
            Helper.succeed();
        });
    }

    private static ServerPlayer playerAt(GameTestHelper Helper, Vec3 Spot, String Name) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), Name));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(GameType.SURVIVAL);
        return User;
    }

    private static DClass dclassAt(GameTestHelper Helper, Vec3 Spot) {
        DClass Made = DClassRegistry.DClassType.get().create(Helper.getLevel());
        Made.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        Helper.getLevel().addFreshEntity(Made);
        return Made;
    }

    private static BlockPos platform(GameTestHelper Helper) {
        BlockPos Base = Helper.absolutePos(new BlockPos(16, 1, 16)).above(LureLift);
        for (int Dx = -8; Dx <= 8; Dx++) {
            for (int Dz = -8; Dz <= 8; Dz++) {
                Helper.getLevel().setBlockAndUpdate(Base.offset(Dx, -1, Dz), Blocks.SMOOTH_STONE.defaultBlockState());
            }
        }
        return Base;
    }

    private static void unplatform(GameTestHelper Helper, BlockPos Base) {
        ServerLevel World = Helper.getLevel();
        for (Scp323Beast Beast : World.getEntitiesOfClass(Scp323Beast.class, new AABB(Base).inflate(14.0))) {
            Beast.vanish();
        }
        for (Scp323Skull Skull : World.getEntitiesOfClass(Scp323Skull.class, new AABB(Base).inflate(14.0))) {
            Skull.discard();
        }
        for (LivingEntity Other : World.getEntitiesOfClass(LivingEntity.class, new AABB(Base).inflate(14.0), Found -> !(Found instanceof ServerPlayer))) {
            Other.discard();
        }
        for (int Dx = -8; Dx <= 8; Dx++) {
            for (int Dz = -8; Dz <= 8; Dz++) {
                World.setBlockAndUpdate(Base.offset(Dx, -1, Dz), Blocks.AIR.defaultBlockState());
            }
        }
    }

    @GameTest(template = Arena, timeoutTicks = 260, batch = "scp323_dayInert")
    public static void dayInert(GameTestHelper Helper) {
        setTime(Helper, Noon);
        ServerLevel World = Helper.getLevel();
        BlockPos Base = platform(Helper);
        Vec3 Centre = Vec3.atBottomCenterOf(Base);
        Scp323Skull Skull = Scp323Skull.place(World, Centre, 0.0f, false);
        Helper.assertTrue(Skull != null, "could not place the skull");
        ServerPlayer Watcher = playerAt(Helper, Centre.add(0.6, 0.0, 0.6), "scp323_day_watcher");
        ServerPlayer Carrier = playerAt(Helper, Centre.add(-5.0, 0.0, 0.0), "scp323_day_carrier");
        Carrier.getInventory().add(Scp323SkullItem.fresh());
        ServerPlayer Wearer = playerAt(Helper, Centre.add(0.0, 0.0, -5.0), "scp323_day_wearer");
        Wearer.setItemSlot(EquipmentSlot.HEAD, Scp323SkullItem.fresh());
        DClass Subject = dclassAt(Helper, Centre.add(0.0, 0.0, 0.9));
        DClass Hatted = dclassAt(Helper, Centre.add(5.0, 0.0, 0.0));
        Hatted.setItemSlot(EquipmentSlot.HEAD, Scp323SkullItem.fresh());
        Helper.runAfterDelay(150, () -> {
            Helper.assertTrue(Skull.isAlive() && !Skull.isRestless(), "the skull stirred or was taken in daylight");
            Helper.assertTrue(!Scp323Lure.isLured(Watcher) && !Scp323Bond.isBound(Watcher), "a player in sight of the skull was drawn in by day");
            Helper.assertTrue(Scp323Carry.compel(Carrier) == 0.0f && !Scp323Bond.isBound(Carrier), "carrying the skull by day built up a compulsion");
            Helper.assertTrue(Carrier.getInventory().contains(Scp323SkullItem.fresh()), "the carried skull left the carrier's pack by day");
            Helper.assertTrue(!Scp323Bond.isBound(Wearer) && Scp323SkullItem.is(Wearer.getItemBySlot(EquipmentSlot.HEAD)), "wearing the skull by day did something");
            Helper.assertTrue(!EnchantmentHelper.has(Wearer.getItemBySlot(EquipmentSlot.HEAD), EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE), "the skull cannot be taken off");
            Helper.assertTrue(!Scp323Bond.isBound(Subject) && !Scp323Bond.isBound(Hatted), "a D-Class was taken by day");
            Helper.assertTrue(Subject.isAlive() && Hatted.isAlive() && Scp323SkullItem.is(Hatted.getItemBySlot(EquipmentSlot.HEAD)), "a D-Class lost its skull hat or vanished by day");
            Helper.assertTrue(World.getEntitiesOfClass(Scp323Beast.class, new AABB(Base).inflate(14.0)).isEmpty(), "SCP-323-1 appeared in daylight");
            dismiss(Helper, Watcher);
            dismiss(Helper, Carrier);
            dismiss(Helper, Wearer);
            unplatform(Helper, Base);
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp323_nightfallFusesWorn")
    public static void nightfallFusesWorn(GameTestHelper Helper) {
        setTime(Helper, Noon);
        ServerPlayer User = player(Helper, 8.5, 8.5, "scp323_dusk");
        User.setItemSlot(EquipmentSlot.HEAD, Scp323SkullItem.fresh());
        Helper.runAfterDelay(30, () -> {
            Helper.assertTrue(!Scp323Bond.isBound(User) && beasts(Helper).isEmpty(), "the skull fused before nightfall");
            setTime(Helper, 13100L);
            Helper.succeedWhen(() -> {
                Helper.assertTrue(Scp323Bond.isTransformed(User), "the worn skull did not fuse at nightfall" + describe(User));
                Helper.assertTrue(User.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "the skull stayed on the head after nightfall");
                Helper.assertTrue(beasts(Helper).size() == 1 && beasts(Helper).get(0).action() == Scp323Beast.ActionMorph, "SCP-323-1 did not grow out of the wearer");
                dismiss(Helper, User);
                tidy(Helper);
                setTime(Helper, Noon);
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp323_lureStopsAtDawn")
    public static void lureStopsAtDawn(GameTestHelper Helper) {
        setTime(Helper, Midnight);
        ServerLevel World = Helper.getLevel();
        BlockPos Base = platform(Helper);
        Vec3 Centre = Vec3.atBottomCenterOf(Base);
        Scp323Skull Skull = Scp323Skull.place(World, Centre, 0.0f, false);
        Helper.assertTrue(Skull != null, "could not place the skull");
        ServerPlayer User = playerAt(Helper, Centre.add(0.0, 0.0, 6.0), "scp323_night_watcher");
        Helper.assertTrue(!Scp148Shield.isWarded(User), "a telekill ward from another test reaches the lure test");
        Helper.runAfterDelay(30, () -> {
            Helper.assertTrue(Scp323Lure.isLured(User), "the skull did not draw in a player at night");
            Helper.assertTrue(Skull.isRestless(), "the skull is not restless at night");
            setTime(Helper, 24000L + 500L);
            Helper.runAfterDelay(25, () -> {
                Helper.assertTrue(!Scp323Lure.isLured(User), "the lure kept pulling after dawn");
                Helper.assertTrue(!Skull.isRestless() && Skull.isAlive(), "the skull did not settle at dawn");
                Helper.assertTrue(!Scp323Bond.isBound(User), "the player was taken");
                dismiss(Helper, User);
                unplatform(Helper, Base);
                tidy(Helper);
                setTime(Helper, Noon);
                Helper.succeed();
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 260, batch = "scp323_firstEpisodeEndsAtDawn")
    public static void firstEpisodeEndsAtDawn(GameTestHelper Helper) {
        setTime(Helper, Midnight);
        ServerPlayer User = player(Helper, 8.5, 8.5, "scp323_first_night");
        User.setItemSlot(EquipmentSlot.HEAD, Scp323SkullItem.fresh());
        Helper.runAfterDelay(80, () -> {
            Helper.assertTrue(Scp323Bond.isTransformed(User) && beasts(Helper).size() == 1, "the first episode did not start at night" + describe(User));
            Helper.assertTrue(beasts(Helper).get(0).firstEpisode() && beasts(Helper).get(0).kills() == 0, "expected a first episode with no kills");
            setTime(Helper, 24000L + 600L);
            Helper.succeedWhen(() -> {
                Helper.assertTrue(beasts(Helper).isEmpty(), "the first episode did not end at dawn");
                Helper.assertTrue(Scp323Bond.isHorned(User) && Scp323Bond.of(User).FirstDone(), "the host is not horned with the first episode done" + describe(User));
                dismiss(Helper, User);
                tidy(Helper);
                setTime(Helper, Noon);
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp323_lureProtection")
    public static void lureProtection(GameTestHelper Helper) {
        setTime(Helper, Midnight);
        ServerLevel World = Helper.getLevel();
        BlockPos Base = platform(Helper);
        Vec3 Centre = Vec3.atBottomCenterOf(Base);
        Scp323Skull Skull = Scp323Skull.place(World, Centre, 0.0f, false);
        Helper.assertTrue(Skull != null, "could not place the skull");
        DClass Shielded = dclassAt(Helper, Centre.add(0.0, 0.0, 0.8));
        Shielded.addEffect(new MobEffectInstance(AntiMemeticRegistry.Shielded, 2400));
        DClass Helmeted = dclassAt(Helper, Centre.add(0.8, 0.0, 0.0));
        Helmeted.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Scp148Registry.Helmet.get()));
        ServerPlayer Protected = playerAt(Helper, Centre.add(-0.8, 0.0, 0.0), "scp323_protected");
        Protected.addEffect(new MobEffectInstance(AntiMemeticRegistry.Shielded, 2400));
        ServerPlayer Telekill = playerAt(Helper, Centre.add(0.0, 0.0, -0.8), "scp323_telekill");
        Telekill.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Scp148Registry.Helmet.get()));
        Helper.runAfterDelay(50, () -> {
            Helper.assertTrue(Skull.isAlive(), "a protected person picked up the skull");
            Helper.assertTrue(!Scp323Bond.isBound(Shielded) && !Scp323Bond.isBound(Helmeted), "a protected D-Class was taken by SCP-323");
            Helper.assertTrue(!Scp323Bond.isBound(Protected) && !Scp323Bond.isBound(Telekill), "a protected player was taken by SCP-323");
            ServerPlayer Bare = playerAt(Helper, Centre.add(0.6, 0.0, 0.6), "scp323_bare");
            Helper.assertTrue(!Scp148Shield.isWarded(Bare), "a telekill ward from another test reaches the lure test");
            Helper.succeedWhen(() -> {
                Helper.assertTrue(!Skull.isAlive(), "the unprotected player was not drawn onto the skull");
                Helper.assertTrue(Scp323Bond.isBound(Bare), "the unprotected player was not taken" + describe(Bare));
                dismiss(Helper, Protected);
                dismiss(Helper, Telekill);
                dismiss(Helper, Bare);
                for (Scp323Beast Beast : World.getEntitiesOfClass(Scp323Beast.class, new AABB(Base).inflate(12.0))) {
                    Beast.vanish();
                }
                Shielded.discard();
                Helmeted.discard();
                unplatform(Helper, Base);
                tidy(Helper);
            });
        });
    }
}
