package net.rai.scp_unleashed.scp407.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.control.ControlRegistry;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassCrime;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.dclass.DClassTrait;
import net.rai.scp_unleashed.dclass.theodore.Theodore;
import net.rai.scp_unleashed.dclass.theodore.TheodoreRegistry;
import net.rai.scp_unleashed.playerclone.PlayerClone;
import net.rai.scp_unleashed.playerclone.PlayerCloneRegistry;
import net.rai.scp_unleashed.scp407.Scp407Doom;
import net.rai.scp_unleashed.scp407.Scp407Ending;
import net.rai.scp_unleashed.scp407.Scp407Fusion;
import net.rai.scp_unleashed.scp407.Scp407Hearing;
import net.rai.scp_unleashed.scp407.Scp407PodBlock;
import net.rai.scp_unleashed.scp407.Scp407Registry;
import net.rai.scp_unleashed.scp407.Scp407Session;
import net.rai.scp_unleashed.scp407.Scp407Sessions;
import net.rai.scp_unleashed.scp407.Scp407Spawning;
import net.rai.scp_unleashed.scp407.Scp407Sprout;
import net.rai.scp_unleashed.scp407.Scp407Titan;
import net.rai.scp_unleashed.senses.Senses;
import net.rai.scp_unleashed.senses.SensesRegistry;
import net.rai.scp_unleashed.speaker.SoundMessages;
import net.rai.scp_unleashed.speaker.SpeakerBlockEntity;
import net.rai.scp_unleashed.stasis.StasisPodItem;
import net.rai.scp_unleashed.stasis.StasisPods;
import net.rai.scp_unleashed.stasis.StasisRegistry;
import net.rai.scp_unleashed.wire.WireMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp407PeopleGameTests {
    public static final String Arena = Scp407GameTests.Arena;
    public static final int SealLimit = 120;
    public static final int EndLimit = 300;
    public static final int DisturbRounds = 12;
    public static final String WitheredKey = "death.attack.scp407_withered";
    public static final String ImmuneKey = "message.scp_unleashed.stasis.immune";
    public static final String VetoedKey = "message.scp_unleashed.stasis.vetoed";

    private Scp407PeopleGameTests() {
    }

    private static DClass person(GameTestHelper Helper, double X, double Y, double Z, DClassTrait Trait, boolean Thinking) {
        DClass Body = DClassRegistry.DClassType.get().create(Helper.getLevel());
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Y, Z));
        Body.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        Body.identify(Trait, DClassCrime.Assault, 0);
        Body.setNoAi(!Thinking);
        Helper.getLevel().addFreshEntity(Body);
        return Body;
    }

    private static <T extends Mob> T place(GameTestHelper Helper, EntityType<T> Type, double X, double Y, double Z) {
        T Made = Type.create(Helper.getLevel());
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Y, Z));
        Made.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        Helper.getLevel().addFreshEntity(Made);
        return Made;
    }

    private static Boat seat(GameTestHelper Helper, LivingEntity Rider, double X, double Z) {
        Boat Seat = EntityType.BOAT.create(Helper.getLevel());
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, 2.0, Z));
        Seat.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        Helper.getLevel().addFreshEntity(Seat);
        Helper.assertTrue(Rider.startRiding(Seat, true), "Could not seat " + Rider.getName().getString());
        return Seat;
    }

    private static void bed(GameTestHelper Helper, LivingEntity Sleeper, BlockPos Head) {
        BlockState Bed = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH);
        Helper.setBlock(Head, Bed.setValue(BedBlock.PART, BedPart.HEAD));
        Helper.setBlock(Head.south(), Bed.setValue(BedBlock.PART, BedPart.FOOT));
        Sleeper.startSleeping(Helper.absolutePos(Head));
        Helper.assertTrue(Sleeper.isSleeping(), "Could not put " + Sleeper.getName().getString() + " to bed");
    }

    private static SpeakerBlockEntity roomSpeaker(GameTestHelper Helper) {
        Scp407GameTests.room(Helper, 3, 3);
        return Scp407GameTests.speaker(Helper, new BlockPos(2, 4, 6), Direction.EAST);
    }

    private static Scp407Session.Listener entry(Scp407Session Session, LivingEntity Target) {
        return Session.Listeners.get(Scp407Hearing.keyOf(Target));
    }

    private static boolean sealed(Scp407Session Session, UUID Key) {
        Scp407Session.Listener Entry = Session.Listeners.get(Key);
        return Entry != null && Entry.Fused && Entry.PodStage >= Scp407Fusion.Stages;
    }

    private static boolean soothed(LivingEntity Target) {
        return Target.hasEffect(MobEffects.REGENERATION) && Target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN);
    }

    private static boolean atAnchor(Scp407Session.Listener Entry, LivingEntity Target) {
        double Dx = Target.getX() - Entry.HoldX;
        double Dz = Target.getZ() - Entry.HoldZ;
        return Entry.Anchor != null && Target.blockPosition().equals(Entry.Anchor) && Dx * Dx + Dz * Dz <= 0.25;
    }

    private static boolean unharmed(LivingEntity Target, DamageSource Source) {
        float Before = Target.getHealth();
        boolean Hurt = Target.hurt(Source, 100.0f);
        return !Hurt && Target.isAlive() && Target.getHealth() >= Before;
    }

    private static boolean witheredAway(LivingEntity Target) {
        DamageSource Last = Target.getLastDamageSource();
        if (Target.isAlive() || Last == null || !Last.is(Scp407Registry.Withered)) {
            return false;
        }
        Component Line = Last.getLocalizedDeathMessage(Target);
        return Line.getContents() instanceof TranslatableContents Text && Text.getKey().equals(WitheredKey);
    }

    private static String keyOf(Component Line) {
        return Line != null && Line.getContents() instanceof TranslatableContents Text ? Text.getKey() : String.valueOf(Line);
    }

    private static int dropped(GameTestHelper Helper, Item Kind) {
        int Count = 0;
        for (ItemEntity Drop : Helper.getLevel().getEntitiesOfClass(ItemEntity.class, Helper.getBounds())) {
            if (Drop.getItem().is(Kind)) {
                Count += Drop.getItem().getCount();
            }
        }
        return Count;
    }

    private static void stop(SpeakerBlockEntity Speaker) {
        Speaker.receiveMessage(new WireMessage(Speaker.getChannel(), SoundMessages.stopPayload(), Speaker.getBlockPos()));
    }

    private static void disturb(GameTestHelper Helper, LivingEntity Target, int Round, Boat Lure) {
        switch (Round % 4) {
            case 0 -> Target.setDeltaMovement(0.9, 0.45, -0.9);
            case 1 -> {
                if (Target instanceof Mob Body) {
                    Body.getNavigation().moveTo(Target.getX() + 6.0, Target.getY(), Target.getZ() + 6.0, 1.6);
                }
                Target.setDeltaMovement(-0.8, 0.0, 0.8);
            }
            case 2 -> Target.teleportTo(Target.getX() + 2.5, Target.getY(), Target.getZ() - 1.5);
            default -> Target.startRiding(Lure, true);
        }
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp407_people_hearing")
    public static void dclassHearsInAnyState(GameTestHelper Helper) {
        SpeakerBlockEntity Speaker = Scp407GameTests.speaker(Helper, new BlockPos(4, 4, 10), Direction.EAST);
        for (int X = 11; X <= 13; X++) {
            for (int Z = 14; Z <= 16; Z++) {
                for (int Y = 2; Y <= 4; Y++) {
                    boolean Hollow = X == 12 && Z == 15 && Y <= 3;
                    Helper.setBlock(new BlockPos(X, Y, Z), Hollow ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState());
                }
            }
        }
        DClass Standing = person(Helper, 10.5, 2, 10.5, DClassTrait.Stoic, false);
        DClass Wandering = person(Helper, 9.5, 2, 8.5, DClassTrait.Restless, true);
        Wandering.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SHOVEL));
        Wandering.insert(new ItemStack(Items.COBBLESTONE, 12));
        DClass Sitting = person(Helper, 9.5, 2, 12.5, DClassTrait.Lazy, true);
        seat(Helper, Sitting, 9.5, 12.5);
        DClass Sleeping = person(Helper, 12.5, 2, 8.5, DClassTrait.Stoic, false);
        bed(Helper, Sleeping, new BlockPos(12, 2, 8));
        DClass Plugged = person(Helper, 8.5, 2, 6.5, DClassTrait.Curious, true);
        Plugged.setItemSlot(EquipmentSlot.HEAD, new ItemStack(SensesRegistry.Earplugs.get()));
        DClass Hidden = person(Helper, 12.5, 2, 15.5, DClassTrait.Restless, true);
        Scp407Session Session = Scp407GameTests.play(Helper, Speaker);
        Helper.startSequence().thenExecuteAfter(25, () -> {
            for (DClass Body : List.of(Standing, Wandering, Sitting, Sleeping)) {
                Helper.assertTrue(soothed(Body), Body.getName().getString() + " did not get Regeneration and Slowness");
                Helper.assertTrue(entry(Session, Body) != null, Body.getName().getString() + " was not recorded as a listener");
            }
            Helper.assertTrue(Sitting.isPassenger(), "Hearing the song pulled the seated D-Class out of its seat");
            Helper.assertTrue(Sleeping.isSleeping(), "Hearing the song woke the sleeping D-Class");
            Helper.assertTrue(Senses.isDeafened(Plugged), "The earplugs did not register on the D-Class");
            Helper.assertTrue(!Plugged.hasEffect(MobEffects.REGENERATION) && entry(Session, Plugged) == null, "An earplugged D-Class heard the song");
            Helper.assertTrue(!Hidden.hasEffect(MobEffects.REGENERATION) && entry(Session, Hidden) == null, "A walled off D-Class heard the song");
            Scp407GameTests.clean(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 800, batch = "scp407_people_sealed")
    public static void dclassSealedAndWithered(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        SpeakerBlockEntity Speaker = roomSpeaker(Helper);
        DClass Fighter = person(Helper, 6.5, 2, 6.5, DClassTrait.Combative, true);
        Fighter.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        Fighter.insert(new ItemStack(Items.COBBLESTONE, 7));
        Fighter.insert(new ItemStack(Items.COMPASS));
        DClass Sitter = person(Helper, 4.5, 2, 8.5, DClassTrait.Lazy, true);
        Boat Chair = seat(Helper, Sitter, 4.5, 8.5);
        DClass Sleeper = person(Helper, 8.5, 2, 4.5, DClassTrait.Stoic, false);
        bed(Helper, Sleeper, new BlockPos(8, 2, 3));
        List<DClass> Bodies = List.of(Fighter, Sitter, Sleeper);
        Scp407Session Session = Scp407GameTests.play(Helper, Speaker);
        int[] Round = new int[1];
        Map<UUID, Scp407Session.Listener> Entries = new HashMap<>();
        Helper.startSequence().thenExecuteAfter(12, () -> Scp407GameTests.skip(Helper, Session, Scp407Sessions.ActDelay)).thenExecuteAfter(4, () -> {
            Helper.assertTrue(Session.isGenesis(), "The room did not start the first act");
            for (DClass Body : Bodies) {
                Helper.assertTrue(entry(Session, Body) != null, Body.getName().getString() + " is not a listener");
            }
            Scp407GameTests.skip(Helper, Session, Session.ActTick + Scp407Fusion.FuseDelay + 5);
        }).thenWaitUntil(Scp407GameTests.within(Helper, SealLimit, "The D-Class were not sealed into pods", () -> {
            for (DClass Body : Bodies) {
                if (!sealed(Session, Body.getUUID())) {
                    return false;
                }
            }
            return true;
        })).thenExecute(() -> {
            for (DClass Body : Bodies) {
                Scp407Session.Listener Entry = entry(Session, Body);
                Entries.put(Body.getUUID(), Entry);
                Helper.assertTrue(!Body.isPassenger() && !Body.isSleeping(), Body.getName().getString() + " was sealed while seated or asleep");
                Helper.assertTrue(Scp407Fusion.isHeld(Body) && atAnchor(Entry, Body), Body.getName().getString() + " is not held inside its pod");
                Helper.assertTrue(Body.getAttributeValue(Attributes.MOVEMENT_SPEED) <= 0.0, Body.getName().getString() + " can still walk");
                Helper.assertTrue(unharmed(Body, World.damageSources().generic()), Body.getName().getString() + " took damage while sealed");
                Helper.assertTrue(unharmed(Body, World.damageSources().fall()), Body.getName().getString() + " took fall damage while sealed");
            }
            for (DClass Body : Bodies) {
                disturb(Helper, Body, 0, Chair);
            }
        }).thenWaitUntil(Scp407GameTests.within(Helper, DisturbRounds * 3 + 10, "The pod did not keep the D-Class in place", () -> {
            Round[0]++;
            if (Round[0] % 3 != 0) {
                return false;
            }
            for (DClass Body : Bodies) {
                Helper.assertTrue(!Body.isPassenger() && atAnchor(Entries.get(Body.getUUID()), Body), Body.getName().getString() + " left its pod on round " + Round[0] / 3);
            }
            if (Round[0] / 3 >= DisturbRounds) {
                return true;
            }
            for (DClass Body : Bodies) {
                disturb(Helper, Body, Round[0] / 3, Chair);
            }
            return false;
        })).thenExecute(() -> stop(Speaker)).thenWaitUntil(Scp407GameTests.within(Helper, EndLimit, "The sealed D-Class did not wither", () -> {
            for (DClass Body : Bodies) {
                if (Body.isAlive()) {
                    return false;
                }
            }
            return true;
        })).thenExecute(() -> {
            for (DClass Body : Bodies) {
                Helper.assertTrue(witheredAway(Body), Body.getName().getString() + " did not die of the song, last damage " + Body.getLastDamageSource());
                Helper.assertTrue(Entries.get(Body.getUUID()).Struck, Body.getName().getString() + " was not marked as struck");
                Helper.assertTrue(!Scp407Doom.get(World.getServer()).isPending(Body.getUUID()), Body.getName().getString() + " is still pending a strike");
            }
        }).thenExecuteAfter(60, () -> {
            Helper.assertTrue(dropped(Helper, Items.COBBLESTONE) == 7, "The D-Class dropped " + dropped(Helper, Items.COBBLESTONE) + " cobblestone instead of 7");
            Helper.assertTrue(dropped(Helper, Items.COMPASS) == 1, "The D-Class dropped " + dropped(Helper, Items.COMPASS) + " compasses instead of 1");
            Helper.assertTrue(dropped(Helper, Items.IRON_SWORD) == 1, "The D-Class dropped " + dropped(Helper, Items.IRON_SWORD) + " swords instead of 1");
            Helper.assertTrue(Scp407GameTests.growthIn(Helper, 3, 3) == 0, "Growth remained after the song ended");
            Scp407GameTests.clean(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 700, batch = "scp407_people_late")
    public static void dclassArrivingMidSongGetsItsOwnDelay(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        SpeakerBlockEntity Speaker = roomSpeaker(Helper);
        DClass First = person(Helper, 8.5, 2, 8.5, DClassTrait.Stoic, false);
        Scp407Session Session = Scp407GameTests.play(Helper, Speaker);
        DClass[] Late = new DClass[1];
        long[] Heard = new long[1];
        Helper.startSequence().thenExecuteAfter(12, () -> Scp407GameTests.skip(Helper, Session, Scp407Sessions.ActDelay)).thenExecuteAfter(4, () -> {
            Helper.assertTrue(Session.isGenesis(), "The room did not start the first act");
            Scp407GameTests.skip(Helper, Session, Session.ActTick + 1000);
        }).thenWaitUntil(Scp407GameTests.within(Helper, SealLimit, "The first listener was not sealed", () -> sealed(Session, First.getUUID()))).thenExecute(() -> {
            Late[0] = person(Helper, 5.5, 2, 5.5, DClassTrait.Curious, true);
        }).thenWaitUntil(Scp407GameTests.within(Helper, 40, "The arriving D-Class did not become a listener", () -> entry(Session, Late[0]) != null)).thenExecute(() -> {
            Scp407Session.Listener Entry = entry(Session, Late[0]);
            Heard[0] = Entry.FirstHeard;
            Helper.assertTrue(Heard[0] >= Session.ActTick + 1000, "The arriving D-Class was dated to " + Heard[0] + ", before it arrived");
            Helper.assertTrue(soothed(Late[0]), "The arriving D-Class did not get Regeneration and Slowness");
            Helper.assertTrue(Scp407Fusion.fuseAt(Session, Entry) == Heard[0] + Scp407Fusion.FuseDelay, "The arriving D-Class did not get its own delay");
            Scp407GameTests.skip(Helper, Session, Heard[0] + Scp407Fusion.FuseDelay - 40);
        }).thenExecuteAfter(20, () -> {
            Helper.assertTrue(!entry(Session, Late[0]).Fused && !Scp407Fusion.isHeld(Late[0]), "The arriving D-Class was sealed before its own delay ran out");
            Scp407GameTests.skip(Helper, Session, Heard[0] + Scp407Fusion.FuseDelay + 5);
        }).thenWaitUntil(Scp407GameTests.within(Helper, SealLimit, "The arriving D-Class was not sealed after its delay", () -> sealed(Session, Late[0].getUUID()))).thenExecute(() -> {
            Helper.assertTrue(Scp407Fusion.isHeld(Late[0]) && atAnchor(entry(Session, Late[0]), Late[0]), "The arriving D-Class is not held in its pod");
            Scp407GameTests.clean(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "scp407_people_wanderer")
    public static void dclassSurvivesTheSecondAct(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Helper.setBlock(new BlockPos(20, 2, 20), Blocks.STONE.defaultBlockState());
        SpeakerBlockEntity Speaker = Scp407GameTests.speaker(Helper, new BlockPos(20, 3, 20), Direction.UP);
        DClass Body = person(Helper, 26.5, 2, 20.5, DClassTrait.Stoic, false);
        DClass Sitter = person(Helper, 20.5, 2, 26.5, DClassTrait.Lazy, false);
        seat(Helper, Sitter, 20.5, 26.5);
        Scp407Session Session = Scp407GameTests.play(Helper, Speaker);
        UUID[] Titan = new UUID[1];
        Helper.startSequence().thenExecuteAfter(12, () -> Scp407GameTests.skip(Helper, Session, Scp407Sessions.ActDelay)).thenExecuteAfter(4, () -> {
            Helper.assertTrue(Session.Act == Scp407Session.ActWanderer, "The open field did not pick the second act");
            Helper.assertTrue(soothed(Body) && soothed(Sitter), "The D-Class in the open did not get Regeneration and Slowness");
            Scp407GameTests.skip(Helper, Session, Session.ActTick + Scp407Spawning.WandererDelay + 1);
        }).thenWaitUntil(Scp407GameTests.within(Helper, 160, "The titan did not appear", () -> Session.TitanSpawned)).thenExecute(() -> {
            Titan[0] = Session.Titan;
            Helper.assertTrue(Session.Grown.isEmpty() && !entry(Session, Body).Fused && !Scp407Fusion.isHeld(Body), "The second act grew or sealed something");
            Scp407Ending.begin(World, Session);
        }).thenWaitUntil(Scp407GameTests.within(Helper, 160, "The titan did not wither", () -> World.getEntity(Titan[0]) == null)).thenExecuteAfter(Scp407Ending.DoomTick + 20, () -> {
            for (DClass Each : List.of(Body, Sitter)) {
                Helper.assertTrue(Each.isAlive(), Each.getName().getString() + " died at the end of the second act");
                Helper.assertTrue(Each.getLastDamageSource() == null || !Each.getLastDamageSource().is(Scp407Registry.Withered), Each.getName().getString() + " was struck by the song");
                Helper.assertTrue(!Scp407Doom.get(World.getServer()).isPending(Each.getUUID()), Each.getName().getString() + " is pending a strike");
                Helper.assertTrue(!Scp407Fusion.isHeld(Each), Each.getName().getString() + " is still held");
            }
            Helper.assertTrue(Sitter.isPassenger(), "The second act pulled the seated D-Class out of its seat");
            Scp407GameTests.clean(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "scp407_people_hunted")
    public static void titanHuntsUnsealedDClass(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        SpeakerBlockEntity Speaker = roomSpeaker(Helper);
        DClass Fighter = person(Helper, 8.5, 2, 8.5, DClassTrait.Combative, true);
        Fighter.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
        DClass Coward = person(Helper, 8.5, 2, 4.5, DClassTrait.Cowardly, true);
        List<DClass> Bodies = List.of(Fighter, Coward);
        Scp407Session Session = Scp407GameTests.play(Helper, Speaker);
        boolean[] Targeted = new boolean[1];
        Helper.startSequence().thenExecuteAfter(12, () -> Scp407GameTests.skip(Helper, Session, Scp407Sessions.ActDelay)).thenExecuteAfter(4, () -> {
            Helper.assertTrue(Session.isGenesis(), "The room did not start the first act");
            Scp407Titan Titan = Scp407Spawning.raise(World, Session, Scp407GameTests.titanSpot(Helper, Session, 3, 3));
            Helper.assertTrue(Titan != null, "The titan could not be raised");
            for (DClass Body : Bodies) {
                Helper.assertTrue(Scp407Titan.isQuarry(Body), Body.getName().getString() + " is not prey for the titan");
            }
        }).thenWaitUntil(Scp407GameTests.within(Helper, 400, "The titan did not strike an unsealed D-Class", () -> {
            Entity Found = World.getEntity(Session.Titan);
            if (!(Found instanceof Scp407Titan Titan)) {
                return false;
            }
            if (Titan.getTarget() instanceof DClass) {
                Targeted[0] = true;
            }
            for (DClass Body : Bodies) {
                if (Body.getLastHurtByMob() == Titan || !Body.isAlive()) {
                    return true;
                }
            }
            return false;
        })).thenExecuteAfter(40, () -> {
            Helper.assertTrue(Targeted[0], "The titan never hunted a D-Class");
            Entity Found = World.getEntity(Session.Titan);
            Helper.assertTrue(Found instanceof Scp407Titan Titan && Titan.isAlive(), "The titan broke while fighting the D-Class");
            for (DClass Body : Bodies) {
                Scp407Session.Listener Entry = Session.Listeners.get(Body.getUUID());
                Helper.assertTrue(Entry != null && !Entry.Fused && !Scp407Fusion.isHeld(Body) || !Body.isAlive(), Body.getName().getString() + " was sealed before its time");
                Helper.assertTrue(Body.isAlive() || Entry != null && Entry.Struck, Body.getName().getString() + " died to the titan but was not marked, so it could die twice");
                Helper.assertTrue(Scp407GameTests.inside(Helper, Body), Body.getName().getString() + " left the test area");
            }
            Scp407GameTests.clean(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "scp407_people_pilot")
    public static void pilotedDClassIsSealedAndDiesOnce(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        SpeakerBlockEntity Speaker = roomSpeaker(Helper);
        Mob Body = Scp407GameTests.dclass(Helper, 6.5, 2, 6.5);
        FakePlayer User = Scp407GameTests.listener(Helper, 6.5, 2, 11.5, "scp407_pilot_sealed");
        UUID BodyId = Body.getUUID();
        Helper.assertTrue(ControlManager.possess(User, Body), "The test player could not take control of the D-Class");
        Scp407Session Session = Scp407GameTests.play(Helper, Speaker);
        Helper.startSequence().thenExecuteAfter(12, () -> Scp407GameTests.skip(Helper, Session, Scp407Sessions.ActDelay)).thenExecuteAfter(4, () -> {
            Helper.assertTrue(Session.isGenesis(), "The room did not start the first act");
            Helper.assertTrue(Session.Listeners.containsKey(BodyId) && !Session.Listeners.containsKey(User.getUUID()), "The piloted body was not the listener");
            Helper.assertTrue(soothed(User), "The pilot did not get Regeneration and Slowness");
            Scp407GameTests.skip(Helper, Session, Session.ActTick + Scp407Fusion.FuseDelay + 5);
        }).thenWaitUntil(Scp407GameTests.within(Helper, SealLimit, "The piloted body was not sealed", () -> sealed(Session, BodyId))).thenExecuteAfter(2, () -> {
            Scp407Session.Listener Entry = Session.Listeners.get(BodyId);
            Helper.assertTrue(Scp407Fusion.isHeld(User) && atAnchor(Entry, User), "The pilot is not held inside the pod");
            Helper.assertTrue(unharmed(User, World.damageSources().generic()), "The sealed pilot took damage");
            Helper.assertTrue(Scp407Fusion.refusesStasis(User), "A sealed pilot is not protected from stasis capture");
            Scp407Ending.begin(World, Session);
        }).thenWaitUntil(Scp407GameTests.within(Helper, 200, "The listeners were not struck", () -> Session.Struck)).thenExecute(() -> {
            if (ControlManager.hasPendingDeath(User)) {
                ControlManager.processDeath(User);
            }
        }).thenExecuteAfter(5, () -> {
            Helper.assertTrue(!ControlSessions.isControlling(User) && User.isAlive(), "The pilot did not come home alive");
            Helper.assertTrue(!Scp407Fusion.isHeld(User), "The pilot is still held after coming home");
            Entity Left = World.getEntity(BodyId);
            Helper.assertTrue(Left == null || !Left.isAlive(), "The piloted body survived");
            Helper.assertTrue(Session.Listeners.get(BodyId).Struck && !Scp407Doom.get(World.getServer()).isPending(BodyId), "The piloted body could be struck again");
            Scp407GameTests.clean(Helper);
            Scp407GameTests.dismiss(Helper, User);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 700, batch = "scp407_people_release")
    public static void pilotLeavingASealedBody(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        SpeakerBlockEntity Speaker = roomSpeaker(Helper);
        Mob Body = Scp407GameTests.dclass(Helper, 6.5, 2, 6.5);
        FakePlayer User = Scp407GameTests.listener(Helper, 6.5, 2, 11.5, "scp407_pilot_leaves");
        UUID BodyId = Body.getUUID();
        Helper.assertTrue(ControlManager.possess(User, Body), "The test player could not take control of the D-Class");
        Scp407Session Session = Scp407GameTests.play(Helper, Speaker);
        Helper.startSequence().thenExecuteAfter(12, () -> Scp407GameTests.skip(Helper, Session, Scp407Sessions.ActDelay)).thenExecuteAfter(4, () -> {
            Helper.assertTrue(Session.isGenesis(), "The room did not start the first act");
            Scp407GameTests.skip(Helper, Session, Session.ActTick + Scp407Fusion.FuseDelay + 5);
        }).thenWaitUntil(Scp407GameTests.within(Helper, SealLimit, "The piloted body was not sealed", () -> sealed(Session, BodyId))).thenExecuteAfter(2, () -> {
            Helper.assertTrue(Scp407Fusion.isHeld(User), "The pilot was not held");
            ControlManager.release(User);
            Helper.assertTrue(!ControlSessions.isControlling(User), "The pilot could not leave the body");
        }).thenExecuteAfter(3, () -> {
            Scp407Session.Listener Entry = Session.Listeners.get(BodyId);
            Helper.assertTrue(Entry != null && Entry.Fused, "The body stopped being a sealed listener when the pilot left");
            Helper.assertTrue(!Session.Listeners.containsKey(User.getUUID()), "The pilot became a listener at home");
            Entity Left = World.getEntity(BodyId);
            Helper.assertTrue(Left instanceof DClass, "The body was not left behind as a D-Class");
            DClass Npc = (DClass) Left;
            Helper.assertTrue(Npc.isAlive() && Scp407Fusion.isHeld(Npc) && atAnchor(Entry, Npc), "The left behind body is not held in its pod");
            Helper.assertTrue(unharmed(Npc, World.damageSources().generic()), "The left behind body took damage");
            Helper.assertTrue(!Scp407Fusion.isHeld(User) && User.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0.0, "The pilot is still held at home");
            Scp407Ending.begin(World, Session);
        }).thenWaitUntil(Scp407GameTests.within(Helper, EndLimit, "The left behind body did not wither", () -> {
            Entity Left = World.getEntity(BodyId);
            return Left == null || !Left.isAlive();
        })).thenExecute(() -> {
            Entity Left = World.getEntity(BodyId);
            Helper.assertTrue(!(Left instanceof LivingEntity Corpse) || witheredAway(Corpse), "The left behind body did not die of the song");
            Helper.assertTrue(User.isAlive() && !ControlSessions.isControlling(User), "The former pilot was harmed by the ending");
            Helper.assertTrue(!Scp407Doom.get(World.getServer()).isPending(BodyId) && !Scp407Doom.get(World.getServer()).isPending(User.getUUID()), "A strike was left pending");
            Scp407GameTests.clean(Helper);
            Scp407GameTests.dismiss(Helper, User);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 700, batch = "scp407_people_boarding")
    public static void pilotTakingASealedBody(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        SpeakerBlockEntity Speaker = roomSpeaker(Helper);
        DClass Body = person(Helper, 6.5, 2, 8.5, DClassTrait.Restless, false);
        FakePlayer User = Scp407GameTests.listener(Helper, 6.5, 2, 11.5, "scp407_pilot_boards");
        UUID BodyId = Body.getUUID();
        Scp407Session Session = Scp407GameTests.play(Helper, Speaker);
        Helper.startSequence().thenExecuteAfter(12, () -> Scp407GameTests.skip(Helper, Session, Scp407Sessions.ActDelay)).thenExecuteAfter(4, () -> {
            Helper.assertTrue(Session.isGenesis(), "The room did not start the first act");
            Helper.assertTrue(!Session.Listeners.containsKey(User.getUUID()), "The player outside the room heard the song");
            Scp407GameTests.skip(Helper, Session, Session.ActTick + Scp407Fusion.FuseDelay + 5);
        }).thenWaitUntil(Scp407GameTests.within(Helper, SealLimit, "The D-Class was not sealed", () -> sealed(Session, BodyId))).thenExecuteAfter(2, () -> {
            Helper.assertTrue(User.distanceToSqr(Body) <= ControlManager.Reach * ControlManager.Reach, "The player is too far from the pod");
            Helper.assertTrue(ControlManager.possess(User, Body), "The player could not take control of the sealed D-Class");
        }).thenExecuteAfter(2, () -> {
            Scp407Session.Listener Entry = Session.Listeners.get(BodyId);
            Helper.assertTrue(Scp407Hearing.keyOf(User).equals(BodyId), "The pilot is not keyed by the body");
            Helper.assertTrue(Scp407Fusion.isHeld(User) && atAnchor(Entry, User), "The pilot is not held in the pod");
            Helper.assertTrue(unharmed(User, World.damageSources().generic()), "The pilot of a sealed body took damage");
            int Walls = 0;
            for (Direction Side : Direction.Plane.HORIZONTAL) {
                BlockPos Wall = Entry.Anchor.relative(Side);
                BlockState State = World.getBlockState(Wall);
                if (State.getBlock() instanceof Scp407PodBlock) {
                    Walls++;
                    Helper.assertTrue(State.getDestroyProgress(User, World, Wall) <= 0.0f, "The pilot could break the pod at " + Wall);
                }
            }
            Helper.assertTrue(Walls >= 3, "The pod has only " + Walls + " walls");
            User.teleportTo(User.getX() + 3.0, User.getY(), User.getZ());
        }).thenExecuteAfter(2, () -> {
            Scp407Session.Listener Entry = Session.Listeners.get(BodyId);
            Helper.assertTrue(atAnchor(Entry, User), "The pilot slipped out of the pod");
            Scp407Ending.begin(World, Session);
        }).thenWaitUntil(Scp407GameTests.within(Helper, 200, "The listeners were not struck", () -> Session.Struck)).thenExecute(() -> {
            if (ControlManager.hasPendingDeath(User)) {
                ControlManager.processDeath(User);
            }
        }).thenExecuteAfter(5, () -> {
            Helper.assertTrue(!ControlSessions.isControlling(User) && User.isAlive() && !Scp407Fusion.isHeld(User), "The pilot did not come home free and alive");
            Entity Left = World.getEntity(BodyId);
            Helper.assertTrue(Left == null || !Left.isAlive(), "The sealed body survived");
            Helper.assertTrue(!Scp407Doom.get(World.getServer()).isPending(BodyId), "The sealed body is still pending a strike");
            Scp407GameTests.clean(Helper);
            Scp407GameTests.dismiss(Helper, User);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "scp407_people_reload")
    public static void sealedDClassSurvivesReload(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        SpeakerBlockEntity Speaker = roomSpeaker(Helper);
        DClass Body = person(Helper, 6.5, 2, 6.5, DClassTrait.Restless, true);
        UUID BodyId = Body.getUUID();
        Scp407Session Session = Scp407GameTests.play(Helper, Speaker);
        Scp407Session[] Restored = new Scp407Session[1];
        DClass[] Reloaded = new DClass[1];
        CompoundTag[] Shelf = new CompoundTag[1];
        Helper.startSequence().thenExecuteAfter(12, () -> Scp407GameTests.skip(Helper, Session, Scp407Sessions.ActDelay)).thenExecuteAfter(4, () -> {
            Helper.assertTrue(Session.isGenesis(), "The room did not start the first act");
            Scp407GameTests.skip(Helper, Session, Session.ActTick + Scp407Fusion.FuseDelay + 5);
        }).thenWaitUntil(Scp407GameTests.within(Helper, SealLimit, "The D-Class was not sealed", () -> sealed(Session, BodyId))).thenExecute(() -> {
            Scp407Session.Listener Before = Session.Listeners.get(BodyId);
            Scp407Sessions Data = Scp407Sessions.get(World);
            Scp407Sessions Loaded = Scp407Sessions.load(Data.save(new CompoundTag(), World.registryAccess()), World.registryAccess());
            Restored[0] = Loaded.byId(Session.Id);
            Helper.assertTrue(Restored[0] != null, "The playback did not survive a save and load");
            Scp407Session.Listener After = Restored[0].Listeners.get(BodyId);
            Helper.assertTrue(After != null && After.Fused && After.PodStage == Before.PodStage && Before.Anchor.equals(After.Anchor) && After.HoldX == Before.HoldX
                    && After.HoldY == Before.HoldY && After.HoldZ == Before.HoldZ && After.FirstHeard == Before.FirstHeard && After.Person == Before.Person,
                    "The sealed D-Class record changed across a save and load");
            Data.remove(Session);
            Data.adopt(Restored[0]);
            Shelf[0] = new CompoundTag();
            Helper.assertTrue(Body.save(Shelf[0]), "The D-Class could not be saved");
            Body.discard();
        }).thenExecuteAfter(1, () -> {
            Entity Made = EntityType.loadEntityRecursive(Shelf[0], World, Each -> Each);
            Helper.assertTrue(Made instanceof DClass, "The D-Class could not be loaded back");
            Reloaded[0] = (DClass) Made;
            Helper.assertTrue(Reloaded[0].getUUID().equals(BodyId) && World.addFreshEntity(Reloaded[0]), "The reloaded D-Class could not rejoin the world");
        }).thenExecuteAfter(3, () -> {
            Scp407Session.Listener Entry = Restored[0].Listeners.get(BodyId);
            Helper.assertTrue(Scp407Fusion.isHeld(Reloaded[0]) && atAnchor(Entry, Reloaded[0]), "The reloaded D-Class is not held in its pod");
            Helper.assertTrue(unharmed(Reloaded[0], World.damageSources().generic()), "The reloaded D-Class took damage while sealed");
            Helper.assertTrue(Scp407Fusion.refusesStasis(Reloaded[0]), "The reloaded D-Class could be put in stasis");
            Scp407Ending.begin(World, Restored[0]);
        }).thenWaitUntil(Scp407GameTests.within(Helper, EndLimit, "The reloaded D-Class did not wither", () -> !Reloaded[0].isAlive())).thenExecute(() -> {
            Helper.assertTrue(witheredAway(Reloaded[0]), "The reloaded D-Class did not die of the song");
            Scp407GameTests.clean(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 700, batch = "scp407_people_others")
    public static void otherPeopleShareTheSong(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        SpeakerBlockEntity Speaker = roomSpeaker(Helper);
        Theodore Theo = place(Helper, TheodoreRegistry.TheodoreType.get(), 4.5, 2, 8.5);
        PlayerClone Clone = place(Helper, PlayerCloneRegistry.CloneType.get(), 8.5, 2, 8.5);
        Entity Seat = ControlRegistry.VacantBodyType.get().create(World);
        Helper.assertTrue(Seat != null && !Scp407Hearing.counts(Seat), "An empty seat counts as a listener");
        List<LivingEntity> People = List.of(Theo, Clone);
        Scp407Session Session = Scp407GameTests.play(Helper, Speaker);
        Helper.startSequence().thenExecuteAfter(12, () -> Scp407GameTests.skip(Helper, Session, Scp407Sessions.ActDelay)).thenExecuteAfter(4, () -> {
            Helper.assertTrue(Session.isGenesis(), "The room did not start the first act");
            for (LivingEntity Each : People) {
                Helper.assertTrue(entry(Session, Each) != null && soothed(Each), Each.getName().getString() + " did not hear the song");
            }
            Scp407GameTests.skip(Helper, Session, Session.ActTick + Scp407Fusion.FuseDelay + 5);
        }).thenWaitUntil(Scp407GameTests.within(Helper, SealLimit, "Theodore and the clone were not sealed", () -> sealed(Session, Theo.getUUID()) && sealed(Session, Clone.getUUID())))
                .thenExecuteAfter(2, () -> {
                    for (LivingEntity Each : People) {
                        Helper.assertTrue(Scp407Fusion.isHeld(Each) && atAnchor(entry(Session, Each), Each), Each.getName().getString() + " is not held in its pod");
                        Helper.assertTrue(unharmed(Each, World.damageSources().generic()), Each.getName().getString() + " took damage while sealed");
                    }
                    Scp407Ending.begin(World, Session);
                }).thenWaitUntil(Scp407GameTests.within(Helper, EndLimit, "Theodore and the clone did not wither", () -> !Theo.isAlive() && !Clone.isAlive())).thenExecute(() -> {
                    for (LivingEntity Each : People) {
                        Helper.assertTrue(witheredAway(Each), Each.getName().getString() + " did not die of the song");
                    }
                    Scp407GameTests.clean(Helper);
                }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp407_people_stasis")
    public static void stasisRefusesSongCreatures(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = Scp407GameTests.listener(Helper, 10.5, 2, 10.5, "scp407_keeper");
        Scp407Sprout Sprout = place(Helper, Scp407Registry.Sprout.get(), 6.5, 2, 6.5);
        Scp407Titan Titan = place(Helper, Scp407Registry.Titan.get(), 14.5, 2, 6.5);
        for (Mob Each : List.<Mob>of(Sprout, Titan)) {
            ItemStack Pod = new ItemStack(StasisRegistry.podFor(FragmentRarity.Faint));
            Component Refusal = StasisPods.refusal(User, Pod, Each);
            Helper.assertTrue(Refusal != null && keyOf(Refusal).equals(ImmuneKey), "A stasis pod did not refuse " + Each.getName().getString() + ": " + keyOf(Refusal));
            Helper.assertTrue(!StasisPods.capture(World, User, Pod, Each), "A stasis pod captured " + Each.getName().getString());
            Helper.assertTrue(Each.isAlive() && !Each.isRemoved() && !StasisPodItem.isOccupied(Pod), "The pod took " + Each.getName().getString());
        }
        Sprout.discard();
        Titan.discard();
        Scp407GameTests.dismiss(Helper, User);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp407_people_stasis_sealed")
    public static void stasisRefusesSealedDClass(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        SpeakerBlockEntity Speaker = roomSpeaker(Helper);
        DClass Body = person(Helper, 6.5, 2, 6.5, DClassTrait.Restless, true);
        FakePlayer User = Scp407GameTests.listener(Helper, 6.5, 2, 11.5, "scp407_keeper_sealed");
        Scp407Session Session = Scp407GameTests.play(Helper, Speaker);
        Helper.startSequence().thenExecuteAfter(12, () -> Scp407GameTests.skip(Helper, Session, Scp407Sessions.ActDelay)).thenExecuteAfter(4, () -> {
            Helper.assertTrue(Session.isGenesis(), "The room did not start the first act");
            Scp407GameTests.skip(Helper, Session, Session.ActTick + Scp407Fusion.FuseDelay + 5);
        }).thenWaitUntil(Scp407GameTests.within(Helper, SealLimit, "The D-Class was not sealed", () -> sealed(Session, Body.getUUID()))).thenExecuteAfter(2, () -> {
            ItemStack Pod = new ItemStack(StasisRegistry.podFor(FragmentRarity.Faint));
            Component Refusal = StasisPods.refusal(User, Pod, Body);
            Helper.assertTrue(Refusal != null && keyOf(Refusal).equals(VetoedKey), "A stasis pod did not refuse the sealed D-Class: " + keyOf(Refusal));
            Helper.assertTrue(!StasisPods.capture(World, User, Pod, Body), "A stasis pod pulled the sealed D-Class out of its pod");
            Helper.assertTrue(Body.isAlive() && !Body.isRemoved() && Scp407Fusion.isHeld(Body) && !StasisPodItem.isOccupied(Pod),
                    "The sealed D-Class left its pod");
            Scp407GameTests.clean(Helper);
            Helper.assertTrue(StasisPods.refusal(User, Pod, Body) == null, "The released D-Class is still refused, so the veto lingers");
            Scp407GameTests.dismiss(Helper, User);
        }).thenSucceed();
    }
}
