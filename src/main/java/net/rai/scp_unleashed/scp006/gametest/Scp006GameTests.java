package net.rai.scp_unleashed.scp006.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;
import net.rai.scp_unleashed.grenade.GrenadeDaze;
import net.rai.scp_unleashed.labware.LabContamination;
import net.rai.scp_unleashed.plumbing.EntityWashedEvent;
import net.rai.scp_unleashed.scp006.Scp006Mending;
import net.rai.scp_unleashed.scp006.Scp006Registry;
import net.rai.scp_unleashed.scp006.Scp006VesselItem;
import net.rai.scp_unleashed.scp008.Scp008Infection;
import net.rai.scp_unleashed.scp009.Scp009Cure;
import net.rai.scp_unleashed.scp009.Scp009Doom;
import net.rai.scp_unleashed.scp012.Scp012Composer;
import net.rai.scp_unleashed.scp016.Scp016Affliction;
import net.rai.scp_unleashed.scp020.Scp020Affliction;
import net.rai.scp_unleashed.scp020.Scp020Organism;
import net.rai.scp_unleashed.scp055.Scp055Episodes;
import net.rai.scp_unleashed.scp063.Scp063Registry;
import net.rai.scp_unleashed.scp063.Scp063Teeth;
import net.rai.scp_unleashed.scp075.Scp075Corrosion;
import net.rai.scp_unleashed.scp075.Scp075Registry;
import net.rai.scp_unleashed.scp079.Scp079Exposure;
import net.rai.scp_unleashed.scp093.Scp093Tears;
import net.rai.scp_unleashed.scp098.Scp098Hives;
import net.rai.scp_unleashed.scp098.Scp098Host;
import net.rai.scp_unleashed.scp129.Scp129Infection;
import net.rai.scp_unleashed.scp129.Scp129Strain;
import net.rai.scp_unleashed.scp129.Scp129Symptoms;
import net.rai.scp_unleashed.scp143.Scp143Registry;
import net.rai.scp_unleashed.scp149.Scp149Host;
import net.rai.scp_unleashed.scp151.Scp151Breath;
import net.rai.scp_unleashed.scp207.Scp207Registry;
import net.rai.scp_unleashed.scp207.Scp207Toll;
import net.rai.scp_unleashed.scp217.Scp217Clockwork;
import net.rai.scp_unleashed.scp224.Scp224Age;
import net.rai.scp_unleashed.scp224.Scp224Registry;
import net.rai.scp_unleashed.scp244.Scp244Chill;
import net.rai.scp_unleashed.scp253.Scp253Infection;
import net.rai.scp_unleashed.scp280.Scp280World;
import net.rai.scp_unleashed.scp294.Scp294Ascension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp006GameTests {
    public static final String Arena = "scp006_arena";
    public static final int Reach = 9;

    private Scp006GameTests() {
    }

    private static BlockState source() {
        return Scp006Registry.Liquid.get().defaultBlockState();
    }

    private static boolean isScp006Source(FluidState Liquid) {
        return Liquid.isSource() && Liquid.getType().isSame(Scp006Registry.Water.get());
    }

    private static ServerPlayer subject(GameTestHelper Helper, double X, double Y, double Z) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "scp006_test"));
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Y, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, ServerPlayer User) {
        Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
    }

    private static void basin(GameTestHelper Helper, int MinX, int MinZ, int Size) {
        for (int X = MinX - 1; X <= MinX + Size; X++) {
            for (int Z = MinZ - 1; Z <= MinZ + Size; Z++) {
                boolean Rim = X < MinX || Z < MinZ || X >= MinX + Size || Z >= MinZ + Size;
                Helper.setBlock(new BlockPos(X, 2, Z), Rim ? Blocks.STONE.defaultBlockState() : source());
            }
        }
    }

    private static void still(List<String> Left, boolean Afflicted, String Name) {
        if (Afflicted) {
            Left.add(Name);
        }
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp006_poolRefills")
    public static void poolRefills(GameTestHelper Helper) {
        basin(Helper, 4, 4, 2);
        BlockPos Taken = new BlockPos(4, 2, 4);
        Helper.startSequence().thenExecuteAfter(10, () -> {
            ServerLevel World = Helper.getLevel();
            BlockPos Absolute = Helper.absolutePos(Taken);
            BlockState State = World.getBlockState(Absolute);
            Helper.assertTrue(State.getBlock() instanceof BucketPickup, "SCP-006 cannot be picked up with a bucket");
            ItemStack Filled = ((BucketPickup) State.getBlock()).pickupBlock(null, World, Absolute, State);
            Helper.assertTrue(Filled.is(Scp006Registry.Bucket.get()), "An empty bucket scooped up " + Filled + " instead of an SCP-006 bucket");
            Helper.assertTrue(World.getFluidState(Absolute).isEmpty(), "The scooped spot still holds liquid right after the scoop");
        }).thenWaitUntil(() -> {
            Helper.assertTrue(isScp006Source(Helper.getLevel().getFluidState(Helper.absolutePos(Taken))), "The 2x2 pool has not refilled the scooped spot");
        }).thenExecute(() -> {
            for (int X = 4; X <= 5; X++) {
                for (int Z = 4; Z <= 5; Z++) {
                    Helper.assertTrue(isScp006Source(Helper.getLevel().getFluidState(Helper.absolutePos(new BlockPos(X, 2, Z)))), "Pool cell " + X + "," + Z + " is not a source");
                }
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp006_gapFills")
    public static void gapFills(GameTestHelper Helper) {
        for (int X = 3; X <= 7; X++) {
            for (int Z = 3; Z <= 5; Z++) {
                if (Z != 4 || X == 3 || X == 7) {
                    Helper.setBlock(new BlockPos(X, 2, Z), Blocks.STONE.defaultBlockState());
                }
            }
        }
        Helper.setBlock(new BlockPos(4, 2, 4), source());
        Helper.setBlock(new BlockPos(6, 2, 4), source());
        Helper.succeedWhen(() -> Helper.assertTrue(isScp006Source(Helper.getLevel().getFluidState(Helper.absolutePos(new BlockPos(5, 2, 4)))),
                "The gap between two SCP-006 sources over solid ground has not become a source"));
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp006_flowsLikeWater")
    public static void flowsLikeWater(GameTestHelper Helper) {
        BlockPos Ours = new BlockPos(12, 2, 12);
        BlockPos Theirs = new BlockPos(36, 2, 12);
        Helper.setBlock(Ours, Blocks.STONE.defaultBlockState());
        Helper.setBlock(Theirs, Blocks.STONE.defaultBlockState());
        Helper.setBlock(Ours.above(), source());
        Helper.setBlock(Theirs.above(), Blocks.WATER.defaultBlockState());
        Helper.startSequence().thenIdle(120).thenExecute(() -> {
            ServerLevel World = Helper.getLevel();
            int Wet = 0;
            for (int Dx = -Reach; Dx <= Reach; Dx++) {
                for (int Dz = -Reach; Dz <= Reach; Dz++) {
                    for (int Y = 2; Y <= 3; Y++) {
                        FluidState Mine = World.getFluidState(Helper.absolutePos(new BlockPos(Ours.getX() + Dx, Y, Ours.getZ() + Dz)));
                        FluidState Water = World.getFluidState(Helper.absolutePos(new BlockPos(Theirs.getX() + Dx, Y, Theirs.getZ() + Dz)));
                        String Where = "offset " + Dx + "," + Y + "," + Dz;
                        Helper.assertTrue(Mine.isEmpty() == Water.isEmpty(), "SCP-006 and water disagree on wetness at " + Where + ": " + Mine + " vs " + Water);
                        if (!Mine.isEmpty()) {
                            Wet++;
                            Helper.assertTrue(Mine.getAmount() == Water.getAmount(), "Level differs at " + Where + ": " + Mine.getAmount() + " vs " + Water.getAmount());
                            Helper.assertTrue(Mine.isSource() == Water.isSource(), "Source state differs at " + Where);
                            Helper.assertTrue(Mine.getValue(FlowingFluid.FALLING) == Water.getValue(FlowingFluid.FALLING), "Falling state differs at " + Where);
                        }
                    }
                }
            }
            Helper.assertTrue(Wet > 60, "SCP-006 barely spread: only " + Wet + " wet cells");
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp006_lavaContact")
    public static void lavaContact(GameTestHelper Helper) {
        for (int X = 3; X <= 20; X++) {
            for (int Z = 3; Z <= 7; Z++) {
                Helper.setBlock(new BlockPos(X, 2, Z), Blocks.STONE.defaultBlockState());
            }
        }
        Helper.setBlock(new BlockPos(5, 2, 5), Blocks.LAVA.defaultBlockState());
        Helper.setBlock(new BlockPos(4, 2, 5), source());
        Helper.setBlock(new BlockPos(10, 2, 5), Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, 2));
        Helper.setBlock(new BlockPos(9, 2, 5), source());
        Helper.setBlock(new BlockPos(15, 2, 5), source());
        Helper.setBlock(new BlockPos(15, 4, 5), Blocks.LAVA.defaultBlockState());
        Helper.succeedWhen(() -> {
            Helper.assertBlockPresent(Blocks.OBSIDIAN, new BlockPos(5, 2, 5));
            Helper.assertBlockPresent(Blocks.COBBLESTONE, new BlockPos(10, 2, 5));
            Helper.assertBlockPresent(Blocks.STONE, new BlockPos(15, 2, 5));
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp006_keepsApartFromWater")
    public static void keepsApartFromWater(GameTestHelper Helper) {
        for (int X = 29; X <= 35; X++) {
            for (int Y = 2; Y <= 3; Y++) {
                for (int Z = 19; Z <= 21; Z++) {
                    Helper.setBlock(new BlockPos(X, Y, Z), Blocks.STONE.defaultBlockState());
                }
            }
        }
        Helper.setBlock(new BlockPos(30, 2, 20), Blocks.WATER.defaultBlockState());
        Helper.setBlock(new BlockPos(30, 3, 20), source());
        Helper.setBlock(new BlockPos(34, 2, 20), source());
        Helper.setBlock(new BlockPos(34, 3, 20), Blocks.WATER.defaultBlockState());
        Helper.startSequence().thenIdle(60).thenExecute(() -> {
            ServerLevel World = Helper.getLevel();
            FluidState LowWater = World.getFluidState(Helper.absolutePos(new BlockPos(30, 2, 20)));
            FluidState HighOurs = World.getFluidState(Helper.absolutePos(new BlockPos(30, 3, 20)));
            FluidState LowOurs = World.getFluidState(Helper.absolutePos(new BlockPos(34, 2, 20)));
            FluidState HighWater = World.getFluidState(Helper.absolutePos(new BlockPos(34, 3, 20)));
            Helper.assertTrue(LowWater.isSource() && LowWater.getType().isSame(Fluids.WATER), "SCP-006 above replaced the water below it: " + LowWater);
            Helper.assertTrue(isScp006Source(HighOurs), "SCP-006 resting on water did not stay put: " + HighOurs);
            Helper.assertTrue(isScp006Source(LowOurs), "Water above replaced the SCP-006 below it: " + LowOurs);
            Helper.assertTrue(HighWater.isSource() && HighWater.getType().isSame(Fluids.WATER), "Water resting on SCP-006 did not stay put: " + HighWater);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp006_curesPlayer")
    public static void curesPlayer(GameTestHelper Helper) {
        BlockPos Pool = new BlockPos(8, 2, 8);
        Helper.setBlock(Pool, source());
        ServerPlayer User = subject(Helper, 8.5, 2.0, 8.5);
        User.addEffect(new MobEffectInstance(MobEffects.POISON, 400, 1));
        User.addEffect(new MobEffectInstance(MobEffects.WITHER, 400, 1));
        User.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 2));
        User.addEffect(new MobEffectInstance(Scp075Registry.Corrosion, 400, 0));
        User.addEffect(new MobEffectInstance(Scp143Registry.Bleeding, 400, 0));
        User.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 0));
        User.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0));
        User.setHealth(4.0f);
        User.setRemainingFireTicks(120);
        User.setTicksFrozen(80);
        ServerLevel World = Helper.getLevel();
        BlockPos Absolute = Helper.absolutePos(Pool);
        World.getBlockState(Absolute).entityInside(World, Absolute, User);
        Helper.assertFalse(User.hasEffect(MobEffects.POISON), "Poison was not cured");
        Helper.assertFalse(User.hasEffect(MobEffects.WITHER), "Wither was not cured");
        Helper.assertFalse(User.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Slowness was not cured");
        Helper.assertFalse(User.hasEffect(Scp075Registry.Corrosion), "SCP-075 corrosion was not cured");
        Helper.assertFalse(User.hasEffect(Scp143Registry.Bleeding), "SCP-143 bleeding was not cured");
        Helper.assertTrue(User.hasEffect(MobEffects.REGENERATION), "Regeneration, a beneficial effect, was removed");
        Helper.assertTrue(User.hasEffect(MobEffects.NIGHT_VISION), "Night vision, a beneficial effect, was removed");
        Helper.assertTrue(User.getHealth() >= User.getMaxHealth(), "Health was not restored: " + User.getHealth());
        Helper.assertTrue(User.getRemainingFireTicks() <= 0, "The player is still burning");
        Helper.assertTrue(User.getTicksFrozen() == 0, "The player is still frozen");
        Helper.assertTrue(Scp006Mending.lastChime(User) >= 0, "No cure chime played after a real cure");
        dismiss(Helper, User);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp006_curesHostile")
    public static void curesHostile(GameTestHelper Helper) {
        basin(Helper, 7, 7, 3);
        Husk Creature = Helper.spawn(EntityType.HUSK, new Vec3(8.5, 2.1, 8.5));
        Creature.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600, 1));
        Creature.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 1));
        Creature.setHealth(5.0f);
        Helper.succeedWhen(() -> {
            Helper.assertFalse(Creature.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "The husk wading in SCP-006 still has slowness");
            Helper.assertFalse(Creature.hasEffect(MobEffects.WEAKNESS), "The husk wading in SCP-006 still has weakness");
            Helper.assertTrue(Creature.getHealth() >= Creature.getMaxHealth(), "The husk wading in SCP-006 was not healed: " + Creature.getHealth());
            Creature.discard();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp006_playerAfflictions")
    public static void playerAfflictions(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ServerPlayer User = subject(Helper, 20.5, 2.0, 12.5);
        BlockPos Hive = Helper.absolutePos(new BlockPos(1, 2, 1));
        Helper.assertTrue(Scp009Doom.doom(User), "SCP-009 doom could not be applied");
        Helper.assertTrue(Scp012Composer.claim(User), "SCP-012 claim could not be applied");
        Helper.assertTrue(Scp016Affliction.afflict(User), "SCP-016 could not be applied");
        Helper.assertTrue(Scp055Episodes.begin(User), "SCP-055 episode could not begin");
        User.setData(Scp063Registry.Toothless, Boolean.TRUE);
        Scp098Hives.get(World).add(Hive);
        Helper.assertTrue(Scp098Host.infect(User, World, Hive), "SCP-098 host could not be applied");
        Scp098Host.setStage(User, 3);
        Helper.assertTrue(Scp149Host.infect(User), "SCP-149 host could not be applied");
        Scp149Host.beginBursting(User);
        Scp207Toll.set(User, new Scp207Toll.State(2, 0.3f, 0, 0, 0, 0, 0));
        Helper.assertTrue(Scp217Clockwork.infect(User), "SCP-217 could not be applied");
        Helper.assertTrue(Scp253Infection.infect(User), "SCP-253 could not be applied");
        Scp280World.get(World.getServer()).setMeter(User.getUUID(), 50.0f);
        Helper.assertTrue(AnomalyCure.cure(User, null), "AnomalyCure reported nothing cured on an afflicted player");
        List<String> Left = new ArrayList<>();
        still(Left, Scp009Doom.isDoomed(User) || Scp009Cure.isSlowed(User), "SCP-009");
        still(Left, Scp012Composer.isComposer(User), "SCP-012");
        still(Left, Scp016Affliction.isAfflicted(User), "SCP-016");
        still(Left, Scp055Episodes.isActive(User), "SCP-055");
        still(Left, Scp063Teeth.isToothless(User), "SCP-063");
        still(Left, Scp098Host.isHost(User), "SCP-098");
        still(Left, Scp149Host.isHost(User), "SCP-149");
        still(Left, User.getExistingDataOrNull(Scp207Registry.Toll.get()) != null, "SCP-207");
        still(Left, Scp217Clockwork.isInfected(User), "SCP-217");
        still(Left, Scp253Infection.isInfected(User), "SCP-253");
        still(Left, Scp280World.get(World.getServer()).meter(User.getUUID()) > 0.0f, "SCP-280");
        Helper.assertTrue(Left.isEmpty(), "Still afflicted after the cure: " + Left);
        Helper.assertFalse(AnomalyCure.cure(User, null), "AnomalyCure reported a cure on a player with nothing left to cure");
        dismiss(Helper, User);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp006_creatureAfflictions")
    public static void creatureAfflictions(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Villager Subject = Helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(10, 2, 10));
        Helper.assertTrue(Scp008Infection.infect(Subject), "SCP-008 could not be applied");
        Helper.assertTrue(Scp020Affliction.afflict(Subject), "SCP-020 could not be applied");
        Scp079Exposure.set(Subject, 50.0f);
        Scp093Tears.setDoses(Subject, 3.0f);
        Scp129Infection.treat(Subject);
        Scp129Infection.force(Subject, Scp129Strain.Four.bit() | Scp129Strain.Five.bit());
        Scp129Infection.setCourse(Subject, 2000);
        Scp129Symptoms.begin(Subject, World, Scp129Symptoms.Freeze, 2, false);
        Scp151Breath.gaze(Subject, World.getGameTime() - 10L);
        Subject.setAirSupply(0);
        Subject.setData(Scp224Registry.Age, new Scp224Age(200, GlobalPos.of(World.dimension(), Helper.absolutePos(BlockPos.ZERO)), 1L, false));
        Scp294Ascension.begin(Subject, 200);
        Helper.assertTrue(AnomalyCure.cure(Subject, null), "AnomalyCure reported nothing cured on an afflicted villager");
        List<String> Left = new ArrayList<>();
        still(Left, Scp008Infection.isInfected(Subject), "SCP-008");
        still(Left, Scp020Affliction.isAfflicted(Subject), "SCP-020");
        still(Left, Scp079Exposure.exposure(Subject) > 0.0f, "SCP-079");
        still(Left, Scp093Tears.read(Subject).Amount() > 0.0f, "SCP-093");
        still(Left, Scp129Infection.isInfected(Subject) || Scp129Symptoms.episodeOf(Subject).active(), "SCP-129");
        still(Left, Scp151Breath.isRecovering(Subject), "SCP-151");
        still(Left, Subject.hasData(Scp224Registry.Age), "SCP-224");
        still(Left, Scp294Ascension.isRising(Subject), "SCP-294");
        Helper.assertTrue(Left.isEmpty(), "Still afflicted after the cure: " + Left);
        Helper.assertTrue(Scp129Infection.stateOf(Subject).resisting(Scp129Strain.One) > 0, "The cure wiped SCP-129 immunity, which is beneficial");
        Helper.assertFalse(AnomalyCure.cure(Subject, null), "AnomalyCure reported a cure on a villager with nothing left to cure");
        Subject.discard();
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp006_mobAfflictions")
    public static void mobAfflictions(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Zombie Subject = Helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(14, 2, 10));
        LabContamination.add(Subject, Scp020Organism.Id);
        GrenadeDaze.daze(Subject, 100, null);
        for (int Step = 0; Step < 100; Step++) {
            Scp244Chill.expose(Subject, 1.0f, World.getGameTime());
            Scp244Chill.apply(World);
        }
        Helper.startSequence().thenExecuteAfter(5, () -> {
            Helper.assertTrue(Scp244Chill.exposure(Subject) > 0.0f, "SCP-244 chill did not stick to the zombie");
            Helper.assertTrue(AnomalyCure.cure(Subject, null), "AnomalyCure reported nothing cured on an afflicted zombie");
            List<String> Left = new ArrayList<>();
            still(Left, LabContamination.isContaminated(Subject), "lab contamination");
            still(Left, GrenadeDaze.isDazed(Subject), "flashbang daze");
            still(Left, Scp244Chill.exposure(Subject) > 0.0f, "SCP-244");
            Helper.assertTrue(Left.isEmpty(), "Still afflicted after the cure: " + Left);
            Helper.assertFalse(AnomalyCure.cure(Subject, null), "AnomalyCure reported a cure on a zombie with nothing left to cure");
            Subject.discard();
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp006_shower")
    public static void curesInShower(GameTestHelper Helper) {
        Villager Subject = Helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(20, 2, 20));
        Subject.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
        Subject.setHealth(8.0f);
        Helper.assertTrue(EntityWashedEvent.wash(Subject, Helper.getLevel(), Helper.absolutePos(new BlockPos(20, 5, 20)), new FluidStack(Scp006Registry.Water.get(), 50), 1),
                "The SCP-006 shower wash was cancelled");
        Helper.assertFalse(Subject.hasEffect(MobEffects.POISON), "An SCP-006 shower did not cure poison");
        Helper.assertTrue(Subject.getHealth() >= Subject.getMaxHealth(), "An SCP-006 shower did not heal");
        Subject.discard();
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp006_corrosionByWater")
    public static void corrosionByWater(GameTestHelper Helper) {
        Villager Subject = Helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(6, 2, 16));
        Scp075Corrosion.afflict(Subject, null);
        Helper.assertTrue(Subject.hasEffect(Scp075Registry.Corrosion), "SCP-075 corrosion did not apply");
        Helper.assertTrue(Scp006Mending.mend(Subject, null), "Mending reported nothing cured on a corroded villager");
        Helper.assertFalse(Subject.hasEffect(Scp075Registry.Corrosion), "SCP-075 corrosion survived the water");
        Subject.discard();
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp006_noChimeWhenWell")
    public static void noChimeWhenWell(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Villager Subject = Helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(16, 2, 16));
        Helper.assertFalse(Scp006Mending.mend(Subject, null), "Mending reported a cure on a healthy villager");
        Helper.assertTrue(Scp006Mending.lastChime(Subject) < 0, "A cure chime played although nothing was cured");
        Subject.setHealth(Subject.getMaxHealth() - 6.0f);
        Helper.assertTrue(Scp006Mending.touch(World, Subject), "Touching SCP-006 did not heal the hurt villager");
        Helper.assertTrue(Scp006Mending.lastChime(Subject) >= 0, "No chime after a real cure");
        Subject.setHealth(Subject.getMaxHealth() - 6.0f);
        Helper.assertFalse(Scp006Mending.touch(World, Subject), "A second touch in the same tick was not throttled");
        Helper.startSequence().thenExecuteAfter(Scp006Mending.Interval, () -> {
            Helper.assertTrue(Scp006Mending.touch(Helper.getLevel(), Subject), "The throttle never let the next touch through");
            Subject.discard();
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp006_vesselPours")
    public static void vesselPours(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ServerPlayer User = subject(Helper, 2.5, 2.0, 4.5);
        User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Scp006Registry.Vessel.get()));
        int Poured = 0;
        for (int Pour = 0; Pour < Scp006VesselItem.Pours; Pour++) {
            Vec3 Spot = Helper.absoluteVec(new Vec3(2.5 + Pour * 4, 2.0, 4.5 + (Pour % 2) * 8));
            User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 90.0f);
            ItemStack Held = User.getItemInHand(InteractionHand.MAIN_HAND);
            Helper.assertTrue(Held.is(Scp006Registry.Vessel.get()), "The vessel broke early, before pour " + (Pour + 1));
            InteractionResultHolder<ItemStack> Result = Held.use(World, User, InteractionHand.MAIN_HAND);
            Helper.assertTrue(Result.getResult().consumesAction(), "Pour " + (Pour + 1) + " failed: " + Result.getResult());
            User.setItemInHand(InteractionHand.MAIN_HAND, Result.getObject());
            Poured++;
            BlockPos Below = BlockPos.containing(Spot);
            Helper.assertTrue(isScp006Source(World.getFluidState(Below)), "Pour " + (Pour + 1) + " did not leave a source at " + Below);
            if (Pour == Scp006VesselItem.Pours - 2) {
                Helper.assertTrue(Scp006VesselItem.poursLeft(Result.getObject()) == 1, "After nine pours the vessel shows " + Scp006VesselItem.poursLeft(Result.getObject()) + " left");
            }
        }
        Helper.assertTrue(Poured == Scp006VesselItem.Pours, "Only " + Poured + " pours happened");
        Helper.assertTrue(User.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "The vessel did not break after its last pour");
        dismiss(Helper, User);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp006_creativePours")
    public static void creativePours(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ServerPlayer User = subject(Helper, 30.5, 2.0, 12.5);
        User.setGameMode(GameType.CREATIVE);
        User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Scp006Registry.Vessel.get()));
        for (int Pour = 0; Pour < 3; Pour++) {
            Vec3 Spot = Helper.absoluteVec(new Vec3(30.5 + Pour * 4, 2.0, 12.5));
            User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 90.0f);
            InteractionResultHolder<ItemStack> Result = User.getItemInHand(InteractionHand.MAIN_HAND).use(World, User, InteractionHand.MAIN_HAND);
            User.setItemInHand(InteractionHand.MAIN_HAND, Result.getObject());
        }
        ItemStack Held = User.getItemInHand(InteractionHand.MAIN_HAND);
        Helper.assertTrue(Held.is(Scp006Registry.Vessel.get()) && Scp006VesselItem.amount(Held) == Scp006VesselItem.Capacity, "Creative pours used up the vessel");
        dismiss(Helper, User);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp006_handlerDrain")
    public static void handlerDrain(GameTestHelper Helper) {
        ItemStack Stack = new ItemStack(Scp006Registry.Vessel.get());
        IFluidHandlerItem Handler = Stack.getCapability(Capabilities.FluidHandler.ITEM);
        Helper.assertTrue(Handler != null, "The vessel exposes no fluid handler");
        Helper.assertTrue(Handler.getFluidInTank(0).getAmount() == Scp006VesselItem.Capacity, "A fresh vessel does not hold its full amount");
        Helper.assertTrue(Handler.fill(new FluidStack(Scp006Registry.Water.get(), 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "The vessel accepted a refill");
        FluidStack Peek = Handler.drain(2500, IFluidHandler.FluidAction.SIMULATE);
        Helper.assertTrue(Peek.getAmount() == 2500 && Scp006VesselItem.amount(Handler.getContainer()) == Scp006VesselItem.Capacity, "A simulated drain changed the vessel");
        FluidStack First = Handler.drain(2500, IFluidHandler.FluidAction.EXECUTE);
        Helper.assertTrue(First.getAmount() == 2500 && First.getFluid().isSame(Scp006Registry.Water.get()), "The first drain returned " + First);
        Helper.assertFalse(Handler.getContainer().isEmpty(), "The vessel broke while it still held water");
        FluidTank Tank = new FluidTank(16000);
        FluidStack Moved = FluidUtil.tryFluidTransfer(Tank, Handler, Scp006VesselItem.Capacity, true);
        Helper.assertTrue(Moved.getAmount() == Scp006VesselItem.Capacity - 2500, "A tank drew " + Moved.getAmount() + " mB instead of the rest");
        Helper.assertTrue(Tank.getFluidAmount() == Scp006VesselItem.Capacity - 2500 && Tank.getFluid().getFluid().isSame(Scp006Registry.Water.get()), "The tank did not receive SCP-006");
        Helper.assertTrue(Handler.getContainer().isEmpty(), "The vessel survived being drained to nothing");
        ItemStack Bucket = new ItemStack(Scp006Registry.Bucket.get());
        Helper.assertTrue(FluidUtil.getFluidContained(Bucket).map(Liquid -> Liquid.getAmount() == 1000 && Liquid.getFluid().isSame(Scp006Registry.Water.get())).orElse(false),
                "The SCP-006 bucket exposes no fluid handler");
        Helper.succeed();
    }
}
