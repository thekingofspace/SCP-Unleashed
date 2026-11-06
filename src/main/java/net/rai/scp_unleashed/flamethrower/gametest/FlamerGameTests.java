package net.rai.scp_unleashed.flamethrower.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.flamethrower.FlamerBurns;
import net.rai.scp_unleashed.flamethrower.FlamerConfig;
import net.rai.scp_unleashed.flamethrower.FlamerRegistry;
import net.rai.scp_unleashed.flamethrower.FlamerStream;
import net.rai.scp_unleashed.flamethrower.FlamerWorld;
import net.rai.scp_unleashed.flamethrower.FuelCanisterItem;
import net.rai.scp_unleashed.guns.GunAmmo;
import net.rai.scp_unleashed.guns.GunFiring;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunReloading;
import net.rai.scp_unleashed.guns.gametest.Guns2GameTests;
import net.rai.scp_unleashed.guns.npc.gametest.NpcGunnerGameTests;
import net.rai.scp_unleashed.scp001day.Scp001DayRegistry;
import net.rai.scp_unleashed.scp939.Scp939Registry;

import java.util.List;
import java.util.function.Predicate;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class FlamerGameTests {
    public static final String Arena = NpcGunnerGameTests.Arena;
    public static final double ShooterX = 2.5;
    public static final double Lane = 4.5;
    public static final float Sturdy = 400.0F;
    public static final float Tolerance = 0.01F;
    public static final List<String> Scp610Bodies = List.of("apostle", "spewer", "villager", "pig", "cow", "sheep", "chicken", "horse", "crawler", "shambler", "hulk",
            "amalgam", "colossus", "herald", "tendril_worm", "hive_lord", "tender", "weaver", "harvester", "burrower", "spore_bearer", "brood_sac", "siege_brute",
            "stalker", "spitter", "matriarch");

    private FlamerGameTests() {
    }

    private static Guns2GameTests.Shooter shooter(GameTestHelper Helper, String Name, double Z, float Pitch) {
        Guns2GameTests.Shooter Made = new Guns2GameTests.Shooter(Helper.getLevel(), Name, Helper.absoluteVec(new Vec3(ShooterX, NpcGunnerGameTests.stand(Helper), Z)));
        Made.setItemInHand(InteractionHand.MAIN_HAND, GunItem.loaded(GunKind.Flamethrower));
        aim(Made, -90.0F, Pitch);
        return Made;
    }

    private static void aim(LivingEntity User, float Yaw, float Pitch) {
        User.setYRot(Yaw);
        User.setYHeadRot(Yaw);
        User.setYBodyRot(Yaw);
        User.setXRot(Pitch);
        User.yRotO = Yaw;
        User.xRotO = Pitch;
    }

    private static <T extends Mob> T sturdy(GameTestHelper Helper, EntityType<T> Type, double X, double Z) {
        T Made = Helper.spawnWithNoFreeWill(Type, new Vec3(X, NpcGunnerGameTests.stand(Helper), Z));
        Made.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Sturdy);
        Made.setHealth(Sturdy);
        return Made;
    }

    private static void prime(LivingEntity User, long Now) {
        FlamerStream.streak(User, Now - 4);
        FlamerStream.streak(User, Now - 2);
    }

    private static FlamerStream.Pulse pulse(GameTestHelper Helper, LivingEntity User, long Now, FlamerConfig.FireRule Rule, Predicate<BlockState> Burnable) {
        return FlamerStream.pulse(Helper.getLevel(), User, Now, Rule, true, Burnable);
    }

    private static boolean struck(FlamerStream.Pulse Burst, LivingEntity Target) {
        return Burst.Hits().stream().anyMatch(Hit -> Hit.Target() == Target);
    }

    private static String describe(GameTestHelper Helper, FlamerStream.Pulse Burst, LivingEntity Target, LivingEntity User) {
        String Struck = Burst.Surface() == null ? "none" : Helper.relativePos(Burst.Surface().getBlockPos()) + " " + Helper.getLevel().getBlockState(Burst.Surface().getBlockPos());
        return "nozzle " + Helper.relativeVec(Burst.Nozzle()) + " end " + Helper.relativeVec(Burst.End()) + " reach " + Burst.Reach() + " surface " + Struck + " target "
                + Helper.relativeVec(Target.position()) + " look " + User.getLookAngle() + " hits " + Burst.Hits().size();
    }

    private static int lift(GameTestHelper Helper) {
        return NpcGunnerGameTests.stand(Helper) - 1;
    }

    private static void fill(GameTestHelper Helper, Block Placed, int X1, int Y1, int Z1, int X2, int Y2, int Z2) {
        int Lift = lift(Helper);
        Y1 += Lift;
        Y2 += Lift;
        for (int X = X1; X <= X2; X++) {
            for (int Y = Y1; Y <= Y2; Y++) {
                for (int Z = Z1; Z <= Z2; Z++) {
                    Helper.setBlock(new BlockPos(X, Y, Z), Placed);
                }
            }
        }
    }

    private static int count(GameTestHelper Helper, Block Wanted, int X1, int X2) {
        int Found = 0;
        int Lift = lift(Helper);
        for (int X = X1; X <= X2; X++) {
            for (int Y = 1 + Lift; Y <= 5 + Lift; Y++) {
                for (int Z = 0; Z <= 8; Z++) {
                    if (Helper.getBlockState(new BlockPos(X, Y, Z)).is(Wanted)) {
                        Found++;
                    }
                }
            }
        }
        return Found;
    }

    private static void tickHeld(Guns2GameTests.Shooter User) {
        ItemStack Held = User.getMainHandItem();
        Held.inventoryTick(User.level(), User, User.getInventory().selected, true);
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "flamer_cone")
    public static void coneHitsOnlyInRangeAndSight(GameTestHelper Helper) {
        Guns2GameTests.Shooter Open = shooter(Helper, "flamer_cone_open", 2.5, 0.0F);
        Guns2GameTests.Shooter Covered = shooter(Helper, "flamer_cone_cover", 5.5, 0.0F);
        Husk Front = sturdy(Helper, EntityType.HUSK, 6.5, 2.5);
        Husk Far = sturdy(Helper, EntityType.HUSK, 15.5, 2.5);
        Husk Wide = sturdy(Helper, EntityType.HUSK, 6.5, 0.5);
        Husk Flank = sturdy(Helper, EntityType.HUSK, 9.5, 4.34);
        Husk Screened = sturdy(Helper, EntityType.HUSK, 9.5, 7.0);
        Husk Walled = sturdy(Helper, EntityType.HUSK, 12.5, 5.5);
        fill(Helper, Blocks.GLASS_PANE, 4, 1, 6, 9, 4, 6);
        fill(Helper, Blocks.STONE, 11, 1, 4, 11, 4, 6);
        Helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    long Now = Helper.getLevel().getGameTime();
                    prime(Open, Now);
                    prime(Covered, Now);
                    FlamerStream.Pulse Clear = pulse(Helper, Open, Now, FlamerConfig.FireRule.Off, State -> false);
                    FlamerStream.Pulse Blocked = pulse(Helper, Covered, Now, FlamerConfig.FireRule.Off, State -> false);
                    Helper.assertTrue(struck(Clear, Front) && Front.getHealth() < Sturdy, "the husk in front was not burned");
                    Helper.assertTrue(struck(Blocked, Flank) && Flank.getHealth() < Sturdy, "the husk at the edge of the cone was not burned");
                    Helper.assertFalse(struck(Clear, Far) || Far.getHealth() < Sturdy, "the husk past the range was burned");
                    Helper.assertFalse(struck(Clear, Wide) || Wide.getHealth() < Sturdy, "the husk outside the cone was burned");
                    Helper.assertFalse(struck(Blocked, Screened) || Screened.getHealth() < Sturdy, "the flame went through the glass pane");
                    Helper.assertFalse(struck(Blocked, Walled) || Walled.getHealth() < Sturdy, "the flame went through the wall");
                    Helper.assertTrue(Blocked.Surface() != null && Blocked.Reach() < FlamerStream.stats().getRange(), "the wall did not stop the stream, reach " + Blocked.Reach());
                    Helper.assertTrue(Clear.Surface() == null, "the open stream struck a block " + describe(Helper, Clear, Front, Open));
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "flamer_vulnerable")
    public static void fleshTakesMoreAndBurnsLonger(GameTestHelper Helper) {
        Guns2GameTests.Shooter User = shooter(Helper, "flamer_flesh_mob", Lane, 20.0F);
        LivingEntity Crawler = sturdy(Helper, Scp001DayRegistry.CrawlerType.get(), 5.5, Lane);
        LivingEntity Neighbour = sturdy(Helper, Scp001DayRegistry.CrawlerType.get(), 5.5, 6.6);
        float[] After = {0.0F};
        Helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    long Now = Helper.getLevel().getGameTime();
                    prime(User, Now);
                    FlamerStream.Pulse Burst = pulse(Helper, User, Now, FlamerConfig.FireRule.Off, State -> false);
                    FlamerStream.Hit Found = Burst.Hits().stream().filter(Hit -> Hit.Target() == Crawler).findFirst().orElse(null);
                    Helper.assertTrue(Found != null, "the flame missed the crawler " + describe(Helper, Burst, Crawler, User));
                    Helper.assertTrue(Found.Vulnerable(), "the crawler is not flame vulnerable");
                    float Plain = FlamerStream.damageAt(Found.Distance(), false);
                    Helper.assertTrue(Math.abs(Found.Amount() - Plain * FlamerStream.VulnerableScale) < Tolerance, "flesh took " + Found.Amount() + ", plain is " + Plain);
                    Helper.assertTrue(Math.abs(Sturdy - Crawler.getHealth() - Found.Amount()) < Tolerance, "the crawler lost " + (Sturdy - Crawler.getHealth()));
                    Helper.assertTrue(FlamerBurns.remaining(Crawler) == FlamerStream.VulnerableBurnTicks, "flesh burn is " + FlamerBurns.remaining(Crawler));
                    Helper.assertTrue(FlamerStream.VulnerableBurnTicks > FlamerStream.BurnTicks * 2, "flesh does not burn longer");
                    Helper.assertFalse(struck(Burst, Neighbour), "the neighbour was hit directly");
                    After[0] = Crawler.getHealth();
                })
                .thenExecuteAfter(FlamerBurns.Second + 2, () -> {
                    Helper.assertTrue(Crawler.getHealth() <= After[0] - FlamerBurns.BurnDamage + Tolerance, "the burn did not hurt the crawler, health " + Crawler.getHealth());
                    Helper.assertTrue(FlamerBurns.remaining(Neighbour) > 0, "the burn did not spread to the neighbour");
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "flamer_plain")
    public static void plainMobTakesNormalDamage(GameTestHelper Helper) {
        Guns2GameTests.Shooter User = shooter(Helper, "flamer_plain", Lane, 20.0F);
        LivingEntity Pig = sturdy(Helper, EntityType.PIG, 5.5, Lane);
        Helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    long Now = Helper.getLevel().getGameTime();
                    prime(User, Now);
                    FlamerStream.Pulse Burst = pulse(Helper, User, Now, FlamerConfig.FireRule.Off, State -> false);
                    FlamerStream.Hit Found = Burst.Hits().stream().filter(Hit -> Hit.Target() == Pig).findFirst().orElse(null);
                    Helper.assertTrue(Found != null, "the flame missed the pig " + describe(Helper, Burst, Pig, User));
                    Helper.assertFalse(Found.Vulnerable(), "a pig counts as flame vulnerable");
                    float Plain = FlamerStream.damageAt(Found.Distance(), false);
                    Helper.assertTrue(Math.abs(Found.Amount() - Plain) < Tolerance, "the pig took " + Found.Amount() + ", plain is " + Plain);
                    Helper.assertTrue(Math.abs(Sturdy - Pig.getHealth() - Plain) < Tolerance, "the pig lost " + (Sturdy - Pig.getHealth()));
                    Helper.assertTrue(Pig.getRemainingFireTicks() >= FlamerStream.BurnTicks - 1, "the pig is not on fire, " + Pig.getRemainingFireTicks());
                    Helper.assertTrue(FlamerBurns.remaining(Pig) == 0, "the pig got the flesh burn");
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "flamer_fuel")
    public static void fuelBurnsAndRefills(GameTestHelper Helper) {
        Guns2GameTests.Shooter User = shooter(Helper, "flamer_fuel", Lane, -40.0F);
        ItemStack Gun = User.getMainHandItem();
        int Reload = GunKind.Flamethrower.getStats().getReloadTicks();
        int Capacity = GunKind.Flamethrower.getStats().getCapacity();
        Item Empty = Scp939Registry.EmptyCanister.get();
        int[] Sequence = {0};
        Helper.onEachTick(() -> tickHeld(User));
        Helper.startSequence()
                .thenIdle(1)
                .thenExecute(() -> GunFiring.requestFire(User, GunKind.Flamethrower.ordinal(), Sequence[0]++))
                .thenIdle(2)
                .thenExecute(() -> GunFiring.requestFire(User, GunKind.Flamethrower.ordinal(), Sequence[0]++))
                .thenIdle(2)
                .thenExecute(() -> GunFiring.requestFire(User, GunKind.Flamethrower.ordinal(), Sequence[0]++))
                .thenExecute(() -> {
                    Helper.assertTrue(GunItem.getAmmo(Gun) == Capacity - 3, "three pulses left " + GunItem.getAmmo(Gun) + " fuel");
                    GunItem.setAmmo(Gun, 30);
                    User.getInventory().add(new ItemStack(FlamerRegistry.FuelCanister.get()));
                    Helper.assertTrue(GunAmmo.count(User, GunKind.Flamethrower) == FuelCanisterItem.Capacity, "a full canister counts " + GunAmmo.count(User, GunKind.Flamethrower));
                    GunReloading.start(User);
                    Helper.assertTrue(GunItem.getReload(Gun) != null, "the refuel did not start");
                })
                .thenExecuteAfter(Reload + 2, () -> {
                    Helper.assertTrue(GunItem.getAmmo(Gun) == Capacity, "the tank holds " + GunItem.getAmmo(Gun) + " after refuelling");
                    Helper.assertTrue(GunAmmo.count(User, GunKind.Flamethrower) == 30, "the canister kept " + GunAmmo.count(User, GunKind.Flamethrower));
                    GunItem.setAmmo(Gun, 0);
                    GunReloading.start(User);
                })
                .thenExecuteAfter(Reload + 2, () -> {
                    Helper.assertTrue(GunItem.getAmmo(Gun) == 30, "the second refuel gave " + GunItem.getAmmo(Gun));
                    Helper.assertTrue(GunAmmo.count(User, GunKind.Flamethrower) == 0, "fuel left over " + GunAmmo.count(User, GunKind.Flamethrower));
                    Helper.assertTrue(User.getInventory().countItem(Empty) == 1, "the drained canister was not handed back empty");
                    GunItem.setAmmo(Gun, 80);
                    User.getInventory().add(new ItemStack(FlamerRegistry.FuelCanister.get(), 2));
                    GunReloading.start(User);
                })
                .thenExecuteAfter(Reload + 2, () -> {
                    Helper.assertTrue(GunItem.getAmmo(Gun) == Capacity, "the third refuel gave " + GunItem.getAmmo(Gun));
                    Helper.assertTrue(GunAmmo.count(User, GunKind.Flamethrower) == 2 * FuelCanisterItem.Capacity - 20, "spare fuel is " + GunAmmo.count(User, GunKind.Flamethrower));
                    Helper.assertTrue(User.getInventory().countItem(FlamerRegistry.FuelCanister.get()) == 2, "the split canister went missing");
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "flamer_water")
    public static void noFlameUnderwater(GameTestHelper Helper) {
        fill(Helper, Blocks.GLASS, 1, 1, 3, 4, 4, 5);
        fill(Helper, Blocks.WATER, 2, 1, 4, 3, 3, 4);
        fill(Helper, Blocks.GLASS, 5, 1, 6, 6, 4, 8);
        fill(Helper, Blocks.WATER, 5, 1, 7, 5, 3, 7);
        Guns2GameTests.Shooter Diver = shooter(Helper, "flamer_diver", Lane, 0.0F);
        Guns2GameTests.Shooter Bank = shooter(Helper, "flamer_bank", 7.5, 0.0F);
        Helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    ItemStack Gun = Diver.getMainHandItem();
                    int Before = GunItem.getAmmo(Gun);
                    Helper.assertTrue(FlamerStream.submerged(Diver), "the diver is not counted as underwater");
                    Helper.assertTrue(((GunItem) Gun.getItem()).blocksTrigger(Diver, Gun), "the trigger is not blocked underwater");
                    GunFiring.requestFire(Diver, GunKind.Flamethrower.ordinal(), 0);
                    Helper.assertTrue(GunItem.getAmmo(Gun) == Before, "fuel was spent underwater");
                    long Now = Helper.getLevel().getGameTime();
                    prime(Bank, Now);
                    Helper.assertFalse(FlamerStream.submerged(Bank), "the bank shooter counts as underwater");
                    FlamerStream.Pulse Burst = pulse(Helper, Bank, Now, FlamerConfig.FireRule.Off, State -> false);
                    Helper.assertTrue(Burst.Doused(), "the stream was not doused by the water ahead");
                    Helper.assertTrue(Burst.Reach() < 2.5, "the stream reached " + Burst.Reach() + " through water");
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "flamer_fire")
    public static void firePlacementFollowsRules(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        GameRules.BooleanValue Griefing = World.getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
        boolean Before = Griefing.get();
        fill(Helper, Blocks.OAK_PLANKS, 7, 1, 0, 7, 4, 8);
        Guns2GameTests.Shooter User = shooter(Helper, "flamer_fire", Lane, 0.0F);
        Husk Npc = Helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(ShooterX, NpcGunnerGameTests.stand(Helper), 1.5));
        aim(Npc, -90.0F, 0.0F);
        Helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    try {
                        Griefing.set(false, World.getServer());
                        Helper.assertTrue(FlamerWorld.allowed(World, User, FlamerConfig.FireRule.Flint), "flint mode refused a player without mobGriefing");
                        Helper.assertFalse(FlamerWorld.allowed(World, Npc, FlamerConfig.FireRule.Flint), "flint mode let a mob place fire without mobGriefing");
                        Helper.assertFalse(FlamerWorld.allowed(World, User, FlamerConfig.FireRule.MobGriefing), "mobGriefing mode ignored the game rule");
                        Helper.assertFalse(FlamerWorld.allowed(World, User, FlamerConfig.FireRule.Off), "off mode allowed fire");
                        long Now = World.getGameTime();
                        prime(User, Now);
                        pulse(Helper, User, Now, FlamerConfig.FireRule.Off, State -> false);
                        Helper.assertTrue(count(Helper, Blocks.FIRE, 0, 23) == 0, "off mode placed fire");
                        prime(Npc, Now);
                        pulse(Helper, Npc, Now, FlamerConfig.FireRule.Flint, State -> false);
                        Helper.assertTrue(count(Helper, Blocks.FIRE, 0, 23) == 0, "a mob placed fire without mobGriefing");
                        pulse(Helper, User, Now + 2, FlamerConfig.FireRule.Flint, State -> false);
                        Helper.assertTrue(count(Helper, Blocks.FIRE, 6, 6) > 0, "a player flamethrower did not set the planks alight");
                        Griefing.set(true, World.getServer());
                        Helper.assertTrue(FlamerWorld.allowed(World, Npc, FlamerConfig.FireRule.Flint), "flint mode refused a mob with mobGriefing");
                        Helper.assertTrue(FlamerWorld.allowed(World, User, FlamerConfig.FireRule.MobGriefing), "mobGriefing mode refused with the rule on");
                        Helper.assertFalse(FlamerWorld.allowed(World, Npc, FlamerConfig.FireRule.Off), "off mode allowed fire with mobGriefing");
                    } finally {
                        Griefing.set(Before, World.getServer());
                        fill(Helper, Blocks.AIR, 0, 1, 0, 6, 5, 8);
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "flamer_flesh")
    public static void fleshBlocksBurnAway(GameTestHelper Helper) {
        fill(Helper, Blocks.NETHER_WART_BLOCK, 6, 1, 2, 6, 4, 7);
        Guns2GameTests.Shooter User = shooter(Helper, "flamer_flesh", Lane, 0.0F);
        BlockPos Core = new BlockPos(6, 2 + lift(Helper), 4);
        Predicate<BlockState> Tagged = State -> State.is(FlamerRegistry.FlameBurnableFlesh);
        Predicate<BlockState> StandIn = State -> State.is(Blocks.NETHER_WART_BLOCK);
        Helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    long Now = Helper.getLevel().getGameTime();
                    prime(User, Now);
                    for (int Pulse = 0; Pulse < 4; Pulse++) {
                        pulse(Helper, User, Now + Pulse * 2L, FlamerConfig.FireRule.Flint, Tagged);
                    }
                    Helper.assertBlockPresent(Blocks.NETHER_WART_BLOCK, Core);
                    for (int Pulse = 4; Pulse < 8; Pulse++) {
                        pulse(Helper, User, Now + Pulse * 2L, FlamerConfig.FireRule.Off, StandIn);
                    }
                    Helper.assertBlockPresent(Blocks.NETHER_WART_BLOCK, Core);
                    for (int Pulse = 8; Pulse < 11; Pulse++) {
                        pulse(Helper, User, Now + Pulse * 2L, FlamerConfig.FireRule.Flint, StandIn);
                    }
                    Helper.assertFalse(Helper.getBlockState(Core).is(Blocks.NETHER_WART_BLOCK), "the flesh stand in did not burn away");
                    Block Flesh = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp610_flesh"));
                    if (Flesh != Blocks.AIR) {
                        Helper.setBlock(Core, Flesh);
                        for (int Pulse = 11; Pulse < 15; Pulse++) {
                            pulse(Helper, User, Now + Pulse * 2L, FlamerConfig.FireRule.Flint, Tagged);
                        }
                        Helper.assertFalse(Helper.getBlockState(Core).is(Flesh), "SCP-610 flesh did not burn away");
                    }
                    fill(Helper, Blocks.AIR, 0, 1, 0, 7, 5, 8);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "flamer_tags")
    public static void optionalTagsLoad(GameTestHelper Helper) {
        for (EntityType<?> Type : List.of(Scp001DayRegistry.CrawlerType.get(), Scp001DayRegistry.ClingerType.get(), Scp001DayRegistry.LurkerType.get(),
                Scp001DayRegistry.MassType.get())) {
            Helper.assertTrue(Type.is(FlamerRegistry.FlameVulnerable), BuiltInRegistries.ENTITY_TYPE.getKey(Type) + " is missing from flame_vulnerable");
        }
        Helper.assertFalse(EntityType.PIG.is(FlamerRegistry.FlameVulnerable), "pigs are flame vulnerable");
        for (String Body : Scp610Bodies) {
            ResourceLocation Id = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp610_" + Body);
            if (BuiltInRegistries.ENTITY_TYPE.containsKey(Id)) {
                Helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.get(Id).is(FlamerRegistry.FlameVulnerable), Id + " is missing from flame_vulnerable");
            }
        }
        Helper.assertTrue(BuiltInRegistries.BLOCK.getTag(FlamerRegistry.FlameBurnableFlesh).isPresent(), "flame_burnable_flesh did not load");
        Helper.assertFalse(Blocks.STONE.defaultBlockState().is(FlamerRegistry.FlameBurnableFlesh), "stone counts as burnable flesh");
        Helper.succeed();
    }
}
