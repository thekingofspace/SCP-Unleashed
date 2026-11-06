package net.rai.scp_unleashed.scp217.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.labware.AssayRecord;
import net.rai.scp_unleashed.labware.LabAnalyzerBlockEntity;
import net.rai.scp_unleashed.labware.LabOrganism;
import net.rai.scp_unleashed.labware.LabOrganisms;
import net.rai.scp_unleashed.labware.LabPetriDishBlockEntity;
import net.rai.scp_unleashed.labware.LabSwabItem;
import net.rai.scp_unleashed.labware.LabSwabRecord;
import net.rai.scp_unleashed.labware.LabVialRack;
import net.rai.scp_unleashed.labware.LabVials;
import net.rai.scp_unleashed.labware.LabwareRegistry;
import net.rai.scp_unleashed.scp173.Scp173Registry;
import net.rai.scp_unleashed.scp217.Scp217Clockwork;
import net.rai.scp_unleashed.scp217.Scp217Organism;
import net.rai.scp_unleashed.scp217.Scp217PerfectOrganism;
import net.rai.scp_unleashed.scp217.Scp217Pulse;
import net.rai.scp_unleashed.scp217.Scp217Registry;
import net.rai.scp_unleashed.scp217.Scp217Strain;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp217PerfectGameTests {
    public static final String Arena = "scp217_arena";
    public static final String Batch = "scp217_perfect";
    public static final String PulseBatch = "scp217_pulse";
    public static final double Floor = 2.0;
    public static final int Trials = 200000;
    public static final int Tolerance = 250;
    public static final int Settle = 60;
    public static final List<Holder<MobEffect>> Harmful = List.of(MobEffects.MOVEMENT_SLOWDOWN, MobEffects.DIG_SLOWDOWN, MobEffects.WEAKNESS, MobEffects.HUNGER,
            MobEffects.CONFUSION, MobEffects.POISON, MobEffects.WITHER, MobEffects.BLINDNESS, MobEffects.DARKNESS);

    private Scp217PerfectGameTests() {
    }

    private static DClass prisoner(GameTestHelper Helper, double X, double Z) {
        ServerLevel World = Helper.getLevel();
        DClass Body = DClassRegistry.DClassType.get().create(World);
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Floor, Z));
        Body.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        Body.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), MobSpawnType.COMMAND, null);
        Body.setNoAi(true);
        for (EquipmentSlot Slot : EquipmentSlot.values()) {
            Body.setItemSlot(Slot, ItemStack.EMPTY);
        }
        World.addFreshEntity(Body);
        return Body;
    }

    private static ServerPlayer subject(GameTestHelper Helper, double X, double Z) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "scp217_test"));
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Floor, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, ServerPlayer User) {
        Scp217Clockwork.cure(User);
        if (!User.isRemoved()) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    private static void release(GameTestHelper Helper, List<? extends Entity> Spawned) {
        for (Entity Each : Spawned) {
            if (Each instanceof ServerPlayer User) {
                dismiss(Helper, User);
            } else {
                tidy(List.of(Each));
            }
        }
    }

    private static void guarded(GameTestHelper Helper, List<? extends Entity> Spawned, Runnable Body) {
        try {
            Body.run();
        } catch (RuntimeException Problem) {
            release(Helper, Spawned);
            throw Problem;
        }
    }

    private static void tidy(List<? extends Entity> Spawned) {
        for (Entity Each : Spawned) {
            if (Each instanceof LivingEntity Living) {
                Scp217Clockwork.cure(Living);
            }
            if (!Each.isRemoved()) {
                Each.discard();
            }
        }
    }

    private static long seedFor(boolean Perfect) {
        for (long Seed = 0L; ; Seed++) {
            if (Scp217Strain.perfects(RandomSource.create(Seed).nextFloat()) == Perfect) {
                return Seed;
            }
        }
    }

    private static LabPetriDishBlockEntity dish(GameTestHelper Helper, BlockPos Relative) {
        Helper.setBlock(Relative, LabwareRegistry.PetriDish.get().defaultBlockState());
        return Helper.getBlockEntity(Relative);
    }

    private static void use(GameTestHelper Helper, ServerPlayer User, BlockPos Relative, ItemStack Held, long Seed) {
        ServerLevel World = Helper.getLevel();
        BlockPos Absolute = Helper.absolutePos(Relative);
        User.setItemInHand(InteractionHand.MAIN_HAND, Held);
        BlockState State = World.getBlockState(Absolute);
        World.getRandom().setSeed(Seed);
        State.useItemOn(User.getItemInHand(InteractionHand.MAIN_HAND), World, User, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(Absolute), Direction.UP, Absolute, false));
    }

    private static void clear(GameTestHelper Helper, BlockPos... Spots) {
        for (BlockPos Spot : Spots) {
            Helper.setBlock(Spot, Blocks.AIR.defaultBlockState());
        }
    }

    private static int amplifier(LivingEntity Host, Holder<MobEffect> Effect) {
        MobEffectInstance Found = Host.getEffect(Effect);
        return Found == null || !Scp217Clockwork.isOurs(Found) ? -1 : Found.getAmplifier();
    }

    private static void settle(LivingEntity Host, int Target) {
        Scp217Clockwork.hasten(Host, Target - Scp217Clockwork.elapsed(Host));
        for (int Step = 0; Step < Settle; Step++) {
            Scp217Clockwork.tick(Host);
        }
    }

    private static void calm(GameTestHelper Helper, LivingEntity Host, String When) {
        for (Holder<MobEffect> Effect : Harmful) {
            Helper.assertFalse(Host.hasEffect(Effect), "a perfect host " + When + " has " + Effect.getRegisteredName());
        }
        Helper.assertFalse(Scp217Clockwork.hasDrag(Host), "a perfect host " + When + " is dragged down");
        Helper.assertTrue(Host.isAlive() && !Host.isDeadOrDying(), "a perfect host " + When + " is not alive");
        Helper.assertTrue(Host.getHealth() >= Host.getMaxHealth(), "a perfect host " + When + " lost health");
        Helper.assertFalse(Scp217Clockwork.isWinding(Scp217Clockwork.elapsed(Host)), "a perfect host " + When + " is winding down");
    }

    private static String keyOf(Component Name) {
        return Name.getContents() instanceof TranslatableContents Contents ? Contents.getKey() : Name.getString();
    }

    @Nullable
    private static LabSwabRecord recordIn(ServerPlayer User) {
        return LabVials.recordOf(User.getItemInHand(InteractionHand.MAIN_HAND));
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = Batch)
    public static void perfectRollOdds(GameTestHelper Helper) {
        Helper.assertTrue(Scp217Strain.perfects(0.0f), "a roll of zero did not perfect the strain");
        Helper.assertTrue(Scp217Strain.perfects(0.0099f), "a roll just under one percent did not perfect the strain");
        Helper.assertFalse(Scp217Strain.perfects(0.01f), "a roll of exactly one percent perfected the strain");
        Helper.assertFalse(Scp217Strain.perfects(0.5f), "a middling roll perfected the strain");
        Helper.assertFalse(Scp217Strain.perfects(0.9999f), "a high roll perfected the strain");
        int Expected = Math.round(Trials * Scp217Strain.Chance);
        for (long Seed : new long[] {217L, 8191L, 424242L}) {
            RandomSource Random = RandomSource.create(Seed);
            int Count = 0;
            for (int Trial = 0; Trial < Trials; Trial++) {
                if (Scp217Strain.roll(Random)) {
                    Count++;
                }
            }
            Helper.assertTrue(Math.abs(Count - Expected) <= Tolerance, "seed " + Seed + " perfected " + Count + " of " + Trials + " rolls, expected about " + Expected);
        }
        ServerLevel World = Helper.getLevel();
        BlockPos Spot = Helper.absolutePos(new BlockPos(4, 2, 4));
        LabOrganism Standard = LabOrganisms.find(Scp217Organism.Id).orElseThrow();
        LabOrganism Perfect = LabOrganisms.find(Scp217PerfectOrganism.Id).orElseThrow();
        RandomSource Random = RandomSource.create(99L);
        int Grown = 0;
        int Cultures = 20000;
        for (int Trial = 0; Trial < Cultures; Trial++) {
            ResourceLocation Result = Standard.cultureIn(World, Spot, Random);
            Helper.assertTrue(Result.equals(Scp217Organism.Id) || Result.equals(Scp217PerfectOrganism.Id), "a standard culture grew into " + Result);
            if (Result.equals(Scp217PerfectOrganism.Id)) {
                Grown++;
            }
        }
        Helper.assertTrue(Grown >= 140 && Grown <= 260, "the dish hook perfected " + Grown + " of " + Cultures + " standard cultures");
        for (int Trial = 0; Trial < 100; Trial++) {
            Helper.assertTrue(Perfect.cultureIn(World, Spot, Random).equals(Scp217PerfectOrganism.Id), "a perfect culture reverted in a dish");
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = Batch)
    public static void dishRollsOncePerPlacement(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        long Winning = seedFor(true);
        long Losing = seedFor(false);
        ServerPlayer User = subject(Helper, 10.5, 30.5);
        BlockPos First = new BlockPos(4, 2, 4);
        BlockPos Second = new BlockPos(8, 2, 4);
        BlockPos Third = new BlockPos(12, 2, 4);
        BlockPos Fourth = new BlockPos(16, 2, 4);
        LabPetriDishBlockEntity FirstDish = dish(Helper, First);
        LabPetriDishBlockEntity SecondDish = dish(Helper, Second);
        LabPetriDishBlockEntity ThirdDish = dish(Helper, Third);
        LabPetriDishBlockEntity FourthDish = dish(Helper, Fourth);
        guarded(Helper, List.of(User), () -> placements(Helper, User, Winning, Losing, FirstDish, SecondDish, ThirdDish, FourthDish));
        dismiss(Helper, User);
        BlockPos Absolute = Helper.absolutePos(First);
        CompoundTag Saved = FirstDish.saveWithFullMetadata(World.registryAccess());
        BlockEntity Loaded = BlockEntity.loadStatic(Absolute, World.getBlockState(Absolute), Saved, World.registryAccess());
        Helper.assertTrue(Loaded instanceof LabPetriDishBlockEntity Reloaded && Scp217Organism.Id.equals(Reloaded.getSample()), "reloading a standard dish changed its culture");
        World.getRandom().setSeed(Winning);
        World.neighborChanged(Absolute, Blocks.STONE, Absolute.below());
        World.sendBlockUpdated(Absolute, World.getBlockState(Absolute), World.getBlockState(Absolute), 3);
        Helper.onEachTick(() -> World.getRandom().setSeed(Winning));
        Helper.runAfterDelay(120, () -> {
            Helper.assertTrue(Scp217Organism.Id.equals(FirstDish.getSample()), "a standard dish turned perfect while it sat: " + FirstDish.getSample());
            Helper.assertTrue(Scp217PerfectOrganism.Id.equals(ThirdDish.getSample()), "a perfect dish changed while it sat");
            clear(Helper, First, Second, Third, Fourth);
            Helper.succeed();
        });
    }

    private static void placements(GameTestHelper Helper, ServerPlayer User, long Winning, long Losing, LabPetriDishBlockEntity FirstDish, LabPetriDishBlockEntity SecondDish,
                                   LabPetriDishBlockEntity ThirdDish, LabPetriDishBlockEntity FourthDish) {
        BlockPos First = new BlockPos(4, 2, 4);
        BlockPos Second = new BlockPos(8, 2, 4);
        BlockPos Third = new BlockPos(12, 2, 4);
        BlockPos Fourth = new BlockPos(16, 2, 4);
        use(Helper, User, First, LabVials.openVial(Scp217Organism.Id), Winning);
        Helper.assertTrue(Scp217PerfectOrganism.Id.equals(FirstDish.getSample()), "a standard culture poured on a winning roll did not turn perfect: " + FirstDish.getSample());
        Helper.assertTrue(LabVials.isEmptyVial(User.getMainHandItem()), "pouring did not empty the vial");
        use(Helper, User, First, LabVials.openVial(Scp217Organism.Id), Losing);
        Helper.assertTrue(Scp217PerfectOrganism.Id.equals(FirstDish.getSample()), "pouring into a full dish changed its culture");
        Helper.assertTrue(LabVials.sampleOf(User.getMainHandItem()) != null, "pouring into a full dish still used up the vial");

        FirstDish.setSample(null);
        use(Helper, User, First, LabVials.openVial(Scp217Organism.Id), Losing);
        Helper.assertTrue(Scp217Organism.Id.equals(FirstDish.getSample()), "a standard culture poured on a losing roll did not stay standard: " + FirstDish.getSample());

        use(Helper, User, Second, LabVials.openVial(Scp217PerfectOrganism.Id), Losing);
        Helper.assertTrue(Scp217PerfectOrganism.Id.equals(SecondDish.getSample()), "a perfect culture reverted when poured on a losing roll");

        use(Helper, User, First, new ItemStack(LabwareRegistry.Swab.get()), Winning);
        LabSwabRecord Copied = recordIn(User);
        Helper.assertTrue(Copied != null && Copied.Findings().equals(List.of(Scp217Organism.Id)), "a copy swab from a standard dish did not carry the standard strain");
        Helper.assertTrue(Scp217Organism.Id.equals(FirstDish.getSample()), "copying a culture out rolled the dish again");
        use(Helper, User, Third, User.getMainHandItem().copy(), Winning);
        Helper.assertTrue(Scp217PerfectOrganism.Id.equals(ThirdDish.getSample()), "a standard copy streaked on a winning roll did not turn perfect");
        Helper.assertTrue(User.getMainHandItem().isEmpty(), "streaking did not use up the swab");

        use(Helper, User, Second, new ItemStack(LabwareRegistry.Swab.get()), Losing);
        use(Helper, User, Fourth, User.getMainHandItem().copy(), Losing);
        Helper.assertTrue(Scp217PerfectOrganism.Id.equals(FourthDish.getSample()), "a perfect copy reverted when streaked on a losing roll");
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = Batch)
    public static void perfectSurvivesVialAndAnalysis(GameTestHelper Helper) {
        long Losing = seedFor(false);
        ServerPlayer User = subject(Helper, 6.5, 8.5);
        BlockPos Spot = new BlockPos(4, 2, 4);
        LabPetriDishBlockEntity Dish = dish(Helper, Spot);
        Dish.setSample(Scp217PerfectOrganism.Id);
        DClass Host = prisoner(Helper, 12.5, 12.5);
        DClass Plain = prisoner(Helper, 16.5, 12.5);
        ItemStack[] Taken = {ItemStack.EMPTY};
        guarded(Helper, List.of(User, Host, Plain), () -> Taken[0] = carry(Helper, User, Spot, Losing, Host, Plain));
        dismiss(Helper, User);
        BlockPos MachineSpot = new BlockPos(20, 2, 20);
        Helper.setBlock(MachineSpot, LabwareRegistry.Analyzer.get().defaultBlockState());
        LabAnalyzerBlockEntity Machine = Helper.getBlockEntity(MachineSpot);
        Machine.getStorage().setStackInSlot(LabAnalyzerBlockEntity.InputSlot, Taken[0]);
        Helper.onEachTick(() -> Machine.insertPower(LabAnalyzerBlockEntity.WorkDraw));
        Helper.succeedWhen(() -> {
            ItemStack Slip = Machine.getStorage().getStackInSlot(LabAnalyzerBlockEntity.OutputSlot);
            Helper.assertFalse(Slip.isEmpty(), "the analyzer has not printed a slip yet");
            AssayRecord Result = Slip.get(LabwareRegistry.Assay.get());
            Helper.assertTrue(Result != null && Result.Findings().contains(Scp217PerfectOrganism.Id) && !Result.Findings().contains(Scp217Organism.Id),
                    "the analyzer read a perfect host as " + (Result == null ? "nothing" : Result.Findings()));
            Helper.assertTrue(keyOf(LabOrganisms.nameOf(Result.Findings().get(0))).equals(Scp217PerfectOrganism.NameKey), "the assay does not name the perfect strain");
            clear(Helper, Spot, MachineSpot);
            tidy(List.of(Host, Plain));
        });
    }

    private static ItemStack carry(GameTestHelper Helper, ServerPlayer User, BlockPos Spot, long Losing, DClass Host, DClass Plain) {
        use(Helper, User, Spot, new ItemStack(LabwareRegistry.Swab.get()), Losing);
        ItemStack Swab = User.getMainHandItem();
        Helper.assertTrue(Scp217PerfectOrganism.Id.equals(LabVials.carriedSample(Swab)), "a swab copied from a perfect dish does not carry the perfect strain");
        User.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(LabwareRegistry.Vial.get()));
        Helper.assertTrue(LabSwabItem.transferToHeldVial(User, InteractionHand.MAIN_HAND), "the swab would not fill the empty vial");
        ItemStack Vial = User.getOffhandItem();
        Helper.assertTrue(Scp217PerfectOrganism.Id.equals(LabVials.sampleOf(Vial)), "the vial filled from the swab holds " + LabVials.sampleOf(Vial));
        Helper.assertTrue(LabVials.carriesCulture(Vial), "the perfect vial is not counted as a culture");
        Helper.assertTrue(keyOf(LabOrganisms.nameOf(LabVials.sampleOf(Vial))).equals(Scp217PerfectOrganism.NameKey), "the perfect vial is not named as the perfect strain");
        Helper.assertTrue(LabOrganisms.vialColourOf(Scp217PerfectOrganism.Id) != LabOrganisms.vialColourOf(Scp217Organism.Id), "the perfect vial looks the same as the standard one");
        Helper.assertTrue(LabOrganisms.cultureColourOf(Scp217PerfectOrganism.Id) != LabOrganisms.cultureColourOf(Scp217Organism.Id), "the perfect dish looks the same as the standard one");
        LabVialRack Rack = new LabVialRack();
        Helper.assertTrue(Rack.insertItem(0, Vial.copy(), false).isEmpty() && Rack.countCultures() == 1, "viral storage would not hold the perfect vial");
        Helper.assertTrue(Scp217PerfectOrganism.Id.equals(LabVials.sampleOf(Rack.getStackInSlot(0))), "the stored vial lost its strain");
        Helper.assertTrue(Scp217Clockwork.infect(Host, true), "a D-Class could not carry the perfect strain");
        Helper.assertTrue(Scp217Clockwork.infect(Plain), "a D-Class could not carry the standard strain");
        List<ResourceLocation> HostFound = LabOrganisms.detect(Host);
        List<ResourceLocation> PlainFound = LabOrganisms.detect(Plain);
        Helper.assertTrue(HostFound.contains(Scp217PerfectOrganism.Id) && !HostFound.contains(Scp217Organism.Id), "a perfect host tests as " + HostFound);
        Helper.assertTrue(PlainFound.contains(Scp217Organism.Id) && !PlainFound.contains(Scp217PerfectOrganism.Id), "a standard host tests as " + PlainFound);
        User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(LabwareRegistry.Swab.get()));
        LabSwabItem.swab(User, InteractionHand.MAIN_HAND, Host);
        ItemStack Taken = User.getMainHandItem().copy();
        LabSwabRecord Record = LabVials.recordOf(Taken);
        Helper.assertTrue(Record != null && Record.Findings().contains(Scp217PerfectOrganism.Id), "a swab of a perfect host missed the strain");
        return Taken;
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = Batch)
    public static void perfectHostNeverSuffers(GameTestHelper Helper) {
        DClass Body = prisoner(Helper, 6.5, 6.5);
        ServerPlayer User = subject(Helper, 14.5, 6.5);
        int Food = User.getFoodData().getFoodLevel();
        guarded(Helper, List.of(Body, User), () -> suffering(Helper, Body, User, Food));
        tidy(List.of(Body));
        dismiss(Helper, User);
        Helper.succeed();
    }

    private static void suffering(GameTestHelper Helper, DClass Body, ServerPlayer User, int Food) {
        int[] Targets = {0, Scp217Clockwork.StageStarts[1] + 5, Scp217Clockwork.StageStarts[2] + 5, Scp217Clockwork.StageStarts[3] + 5, Scp217Clockwork.PerfectCap};
        for (LivingEntity Host : List.of(Body, User)) {
            Helper.assertTrue(Scp217Clockwork.infect(Host, true), "the perfect strain could not be given to " + Host.getName().getString());
            Helper.assertTrue(Scp217Clockwork.isPerfect(Host), "the perfect strain did not take on " + Host.getName().getString());
            for (int Target : Targets) {
                settle(Host, Target);
                calm(Helper, Host, "at stage " + Scp217Clockwork.stageOf(Host));
            }
            Scp217Clockwork.hasten(Host, Scp217Clockwork.Duration);
            for (int Step = 0; Step < Scp217Clockwork.RetryTicks * 3; Step++) {
                Scp217Clockwork.tick(Host);
            }
            calm(Helper, Host, "past the full course");
            Helper.assertTrue(Scp217Clockwork.elapsed(Host) == Scp217Clockwork.PerfectCap, "a perfect host ran past its settled point: " + Scp217Clockwork.elapsed(Host));
            Helper.assertTrue(Scp217Clockwork.stageOf(Host) == Scp217Clockwork.Stages && Scp217Clockwork.isPerfect(Host), "a perfect host did not settle in the final stage");
        }
        Helper.assertTrue(User.getFoodData().getFoodLevel() >= Food, "a perfect player got hungry");
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = Batch)
    public static void perfectBuffsScaleWithStage(GameTestHelper Helper) {
        DClass Body = prisoner(Helper, 6.5, 6.5);
        Helper.assertTrue(Scp217Clockwork.infect(Body, true), "the perfect strain could not be given to a D-Class");
        int Previous = -1;
        int First = 0;
        for (int Stage = 1; Stage <= Scp217Clockwork.Stages; Stage++) {
            settle(Body, Scp217Clockwork.StageStarts[Stage - 1] + 5);
            int Speed = amplifier(Body, MobEffects.MOVEMENT_SPEED);
            int Strength = amplifier(Body, MobEffects.DAMAGE_BOOST);
            Helper.assertTrue(Speed == Scp217Clockwork.PerfectSpeedLevels[Stage - 1], "stage " + Stage + " speed is " + Speed);
            Helper.assertTrue(Strength == Scp217Clockwork.PerfectStrengthLevels[Stage - 1], "stage " + Stage + " strength is " + Strength);
            int Total = Speed + Strength;
            Helper.assertTrue(Total >= Previous, "the buffs shrank at stage " + Stage);
            if (Stage == 1) {
                First = Total;
            }
            Previous = Total;
        }
        Helper.assertTrue(Previous > First, "the buffs never grew past the first stage");
        Helper.assertTrue(amplifier(Body, MobEffects.MOVEMENT_SPEED) == 1 && amplifier(Body, MobEffects.DAMAGE_BOOST) == 1, "the final stage is not Speed II and Strength II");
        tidy(List.of(Body));
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = PulseBatch)
    public static void pulseFindsMobsInRange(GameTestHelper Helper) {
        Helper.assertTrue(Scp217Pulse.radiusOf(1) == 0.0 && Scp217Pulse.radiusOf(2) == 0.0, "the pulse reaches before the late stages");
        Helper.assertTrue(Scp217Pulse.radiusOf(3) == 32.0 && Scp217Pulse.radiusOf(4) == 48.0, "the pulse radii changed");
        for (int Z = 0; Z < 40; Z++) {
            for (int Y = 2; Y <= 4; Y++) {
                Helper.setBlock(new BlockPos(8, Y, Z), Blocks.STONE.defaultBlockState());
            }
        }
        ServerPlayer User = subject(Helper, 1.5, 1.5);
        ServerPlayer Standard = subject(Helper, 4.5, 30.5);
        ServerPlayer Clean = subject(Helper, 6.5, 30.5);
        Mob Near = Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(21.5, Floor, 21.5));
        Mob Middle = Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(24.5, Floor, 24.5));
        Mob Far = Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(37.5, Floor, 37.5));
        List<Entity> Spawned = List.of(User, Standard, Clean, Near, Middle, Far);
        guarded(Helper, Spawned, () -> pulses(Helper, User, Standard, Clean, Near, Middle, Far));
        Helper.runAfterDelay(20, () -> guarded(Helper, Spawned, () -> {
            int Left = Scp217Pulse.cooldown(User);
            Helper.assertTrue(Left >= Scp217Pulse.CooldownTicks - 21 && Left <= Scp217Pulse.CooldownTicks - 19, "the cooldown did not count down with time: " + Left);
            Helper.assertTrue(Scp217Pulse.request(User).Kind() == Scp217Pulse.Outcome.Cooling, "a pulse went out a second after the last one");
            release(Helper, Spawned);
            Helper.succeed();
        }));
    }

    private static void pulses(GameTestHelper Helper, ServerPlayer User, ServerPlayer Standard, ServerPlayer Clean, Mob Near, Mob Middle, Mob Far) {
        ServerLevel World = Helper.getLevel();
        Helper.assertTrue(Scp217Clockwork.infect(User, true), "the perfect strain could not be given to a player");
        Scp217Clockwork.hasten(User, Scp217Clockwork.StageStarts[1] + 5);
        Helper.assertTrue(Scp217Pulse.request(User).Kind() == Scp217Pulse.Outcome.Unable, "a stage 2 perfect host could pulse");
        Scp217Clockwork.hasten(User, Scp217Clockwork.StageStarts[2] + 5 - Scp217Clockwork.elapsed(User));
        Scp217Pulse.Result First = Scp217Pulse.request(User);
        Helper.assertTrue(First.sent(), "a stage 3 perfect host could not pulse: " + First.Kind());
        Helper.assertTrue(First.Marked().contains(Near), "the stage 3 pulse missed a pig behind a wall at 28 blocks");
        Helper.assertFalse(First.Marked().contains(Middle), "the stage 3 pulse found a pig past 32 blocks");
        Helper.assertFalse(First.Marked().contains(Far), "the stage 3 pulse found a pig past 48 blocks");
        Helper.assertFalse(Near.hasEffect(MobEffects.GLOWING) || Near.hasGlowingTag(), "the pulse lit a pig up for everyone");
        Helper.assertTrue(Scp217Pulse.cooldown(User) == Scp217Pulse.CooldownTicks, "the pulse cooldown did not start");
        Scp217Pulse.Result Again = Scp217Pulse.request(User);
        Helper.assertTrue(Again.Kind() == Scp217Pulse.Outcome.Cooling && Again.Marked().isEmpty(), "a second pulse went out during the cooldown");
        User.setData(Scp217Registry.PulseReady.get(), World.getGameTime());
        Scp217Pulse.tick(World, User);
        Helper.assertTrue(Scp217Pulse.cooldown(User) == 0, "the cooldown did not run out");
        Scp217Clockwork.hasten(User, Scp217Clockwork.StageStarts[3] + 5 - Scp217Clockwork.elapsed(User));
        Scp217Pulse.Result Second = Scp217Pulse.request(User);
        Helper.assertTrue(Second.sent(), "a stage 4 perfect host could not pulse after the cooldown");
        Helper.assertTrue(Second.Marked().contains(Near) && Second.Marked().contains(Middle), "the stage 4 pulse missed a pig inside 48 blocks");
        Helper.assertFalse(Second.Marked().contains(Far), "the stage 4 pulse found a pig past 48 blocks");

        Helper.assertTrue(Scp217Clockwork.infect(Standard), "the standard strain could not be given to a player");
        Scp217Clockwork.hasten(Standard, Scp217Clockwork.StageStarts[3] + 5);
        Helper.assertTrue(Scp217Pulse.request(Standard).Kind() == Scp217Pulse.Outcome.Unable, "a standard host could pulse");
        Helper.assertTrue(Scp217Pulse.request(Clean).Kind() == Scp217Pulse.Outcome.Unable, "an uninfected player could pulse");
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = Batch)
    public static void standardClockworkUnchanged(GameTestHelper Helper) {
        DClass Body = prisoner(Helper, 6.5, 6.5);
        Helper.assertTrue(Scp217Clockwork.infect(Body), "the standard strain could not be given to a D-Class");
        Helper.assertFalse(Scp217Clockwork.isPerfect(Body), "the standard strain came out perfect");
        settle(Body, 5);
        Helper.assertTrue(amplifier(Body, MobEffects.MOVEMENT_SPEED) == 0 && amplifier(Body, MobEffects.DAMAGE_BOOST) == 0, "stage 1 no longer gives Speed I and Strength I");
        settle(Body, Scp217Clockwork.StageStarts[1] + 5);
        Helper.assertTrue(Scp217Clockwork.hasDrag(Body), "stage 2 no longer drags");
        settle(Body, Scp217Clockwork.StageStarts[2] + 5);
        Helper.assertTrue(amplifier(Body, MobEffects.MOVEMENT_SPEED) == -1 && amplifier(Body, MobEffects.DAMAGE_BOOST) == -1, "stage 3 kept its buffs");
        Helper.assertTrue(amplifier(Body, MobEffects.MOVEMENT_SLOWDOWN) == 0 && amplifier(Body, MobEffects.DIG_SLOWDOWN) == 0, "stage 3 no longer stiffens");
        settle(Body, Scp217Clockwork.StageStarts[3] + 5);
        Helper.assertTrue(amplifier(Body, MobEffects.MOVEMENT_SLOWDOWN) == 2 && amplifier(Body, MobEffects.WEAKNESS) == 0, "stage 4 changed");
        settle(Body, Scp217Clockwork.Duration - Scp217Clockwork.WindingTicks + 5);
        Helper.assertTrue(Scp217Clockwork.isWinding(Scp217Clockwork.elapsed(Body)) && amplifier(Body, MobEffects.MOVEMENT_SLOWDOWN) == Scp217Clockwork.WindingSlowness,
                "the standard strain no longer winds down");
        List<ResourceLocation> Found = LabOrganisms.detect(Body);
        Helper.assertTrue(Found.contains(Scp217Organism.Id) && !Found.contains(Scp217PerfectOrganism.Id), "a standard host tests as " + Found);
        LabOrganism Standard = LabOrganisms.find(Scp217Organism.Id).orElseThrow();
        Helper.assertTrue(Standard.cultureIn(Helper.getLevel(), Body.blockPosition(), RandomSource.create(seedFor(false))).equals(Scp217Organism.Id),
                "a losing roll changed the standard culture");
        Scp217Clockwork.hasten(Body, Scp217Clockwork.Duration);
        Scp217Clockwork.tick(Body);
        Helper.assertTrue(Body.isDeadOrDying(), "the standard strain no longer winds its host down");
        Helper.runAfterDelay(2, () -> {
            tidy(List.of(Body));
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = Batch)
    public static void perfectRespectsImmunityAndCure(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        DClass Prisoner = prisoner(Helper, 4.5, 4.5);
        DClass Shielded = prisoner(Helper, 12.5, 4.5);
        Shielded.setInvulnerable(true);
        Mob Statue = Helper.spawnWithNoFreeWill(Scp173Registry.Statue.get(), new Vec3(8.5, Floor, 12.5));
        Helper.assertFalse(Scp217Clockwork.infect(Shielded, true), "an invulnerable D-Class caught the perfect strain");
        Helper.assertFalse(Scp217Clockwork.infect(Statue, true), "SCP-173 caught the perfect strain");
        Scp217Clockwork.exposeAround(World, Helper.absoluteVec(new Vec3(8.5, 3.0, 8.5)), Pos -> false, 10.0, true);
        Helper.assertTrue(Scp217Clockwork.isPerfect(Prisoner), "a D-Class beside an open perfect sample did not catch the perfect strain");
        Helper.assertFalse(Scp217Clockwork.isInfected(Shielded) || Scp217Clockwork.isInfected(Statue), "an immune body caught the perfect strain from a sample");
        settle(Prisoner, Scp217Clockwork.StageStarts[3] + 5);
        Helper.assertTrue(AnomalyCure.cure(Prisoner, null), "a cure did not clear the perfect strain");
        Helper.assertFalse(Scp217Clockwork.isInfected(Prisoner) || Scp217Clockwork.isPerfect(Prisoner), "the perfect strain stayed after a cure");
        Helper.assertTrue(amplifier(Prisoner, MobEffects.MOVEMENT_SPEED) == -1 && amplifier(Prisoner, MobEffects.DAMAGE_BOOST) == -1, "the perfect buffs stayed after a cure");
        Helper.assertTrue(Prisoner.getExistingDataOrNull(Scp217Registry.Perfect.get()) == null, "the perfect marker stayed after a cure");
        Helper.assertTrue(Scp217Clockwork.infect(Prisoner), "a cured D-Class could not catch the standard strain again");
        Helper.assertFalse(Scp217Clockwork.isPerfect(Prisoner), "a cured perfect host caught the standard strain as perfect");
        tidy(List.of(Prisoner, Shielded, Statue));
        Helper.succeed();
    }
}
