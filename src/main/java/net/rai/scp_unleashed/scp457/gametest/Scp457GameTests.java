package net.rai.scp_unleashed.scp457.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.plumbing.DrainBlockEntity;
import net.rai.scp_unleashed.plumbing.PlumbingRegistry;
import net.rai.scp_unleashed.scp457.Scp457;
import net.rai.scp_unleashed.scp457.Scp457Arrival;
import net.rai.scp_unleashed.scp457.Scp457Registry;
import net.rai.scp_unleashed.scp457.Scp457Spread;
import net.rai.scp_unleashed.scp457.Scp457Water;
import net.rai.scp_unleashed.stasis.StasisPodItem;
import net.rai.scp_unleashed.stasis.StasisPods;
import net.rai.scp_unleashed.stasis.StasisRegistry;

import java.util.List;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp457GameTests {
    public static final String Arena = "scp457_arena";

    private Scp457GameTests() {
    }

    static int stand(GameTestHelper Helper) {
        for (int Y = 0; Y < 5; Y++) {
            if (Helper.getBlockState(new BlockPos(1, Y, 1)).is(Blocks.SMOOTH_STONE) && Helper.getBlockState(new BlockPos(1, Y + 1, 1)).isAir()) {
                return Y + 1;
            }
        }
        return 1;
    }

    static void clearSkies(ServerLevel World) {
        World.setWeatherParameters(20000000, 0, false, false);
        World.setRainLevel(0.0f);
        World.setThunderLevel(0.0f);
    }

    static void begin(GameTestHelper Helper) {
        purge(Helper.getLevel());
        clearSkies(Helper.getLevel());
    }

    static Scp457 burning(GameTestHelper Helper, double X, double Z) {
        Scp457 Burning = Helper.spawn(Scp457Registry.Burning.get(), new Vec3(X, stand(Helper), Z));
        Burning.setYRot(0.0f);
        return Burning;
    }

    static Victim victim(GameTestHelper Helper, double X, double Z) {
        ServerLevel World = Helper.getLevel();
        Victim User = new Victim(World, new GameProfile(UUID.randomUUID(), "scp457_test"));
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, stand(Helper), Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        return User;
    }

    static FakePlayer watcher(GameTestHelper Helper, double X, double Z) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "scp457_watcher"));
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, stand(Helper), Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        return User;
    }

    static void dismiss(GameTestHelper Helper, ServerPlayer User) {
        Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
    }

    static void tidy(GameTestHelper Helper) {
        Helper.killAllEntitiesOfClass(DClass.class);
        Helper.killAllEntitiesOfClass(Villager.class);
        begin(Helper);
    }

    static void purge(ServerLevel World) {
        for (Scp457 Burning : List.copyOf(World.getEntities(Scp457Registry.Burning.get(), Found -> true))) {
            Burning.discard();
        }
    }

    static int count(GameTestHelper Helper) {
        return Scp457Spread.count(Helper.getLevel());
    }

    static String state(Scp457 Burning) {
        return " [target " + (Burning.getTarget() == null ? "none" : Burning.getTarget().getName().getString()) + ", health " + Burning.getHealth() + ", at "
                + Burning.position() + ", dead " + Burning.isDeadOrDying() + ", death kind " + Burning.deathKind() + ", rising " + Burning.isRising() + ", raining "
                + Burning.level().isRaining() + ", rained on " + Scp457Water.rainedOn(Burning) + ", in water " + Burning.isInWater() + ", touching " + Scp457Water.touching(Burning)
                + ", path done " + Burning.getNavigation().isDone() + ", ground " + Burning.onGround() + "]";
    }

    static void rule(ServerLevel World, GameRules.Key<GameRules.BooleanValue> Key, boolean Value) {
        World.getGameRules().getRule(Key).set(Value, World.getServer());
    }

    static void calm(ServerLevel World) {
        purge(World);
        rule(World, GameRules.RULE_DOFIRETICK, false);
        rule(World, GameRules.RULE_MOBGRIEFING, true);
        if (Scp457Registry.SpreadRule != null) {
            rule(World, Scp457Registry.SpreadRule, true);
        }
        World.setWeatherParameters(20000000, 0, false, false);
        World.setRainLevel(0.0f);
    }

    @AfterBatch(batch = "scp457_targetsClosest")
    public static void afterTargets(ServerLevel World) {
        calm(World);
    }

    @AfterBatch(batch = "scp457_ignitesOnHit")
    public static void afterIgnites(ServerLevel World) {
        calm(World);
    }

    @AfterBatch(batch = "scp457_leavesNoFloorFire")
    public static void afterNoFloorFire(ServerLevel World) {
        calm(World);
    }

    @AfterBatch(batch = "scp457_villagerKillSpreads")
    public static void afterSpread(ServerLevel World) {
        calm(World);
    }

    @AfterBatch(batch = "scp457_capHolds")
    public static void afterCap(ServerLevel World) {
        calm(World);
    }

    @AfterBatch(batch = "scp457_ruleHolds")
    public static void afterRule(ServerLevel World) {
        calm(World);
    }

    @AfterBatch(batch = "scp457_diesInWater")
    public static void afterWater(ServerLevel World) {
        calm(World);
    }

    @AfterBatch(batch = "scp457_diesToSplash")
    public static void afterSplash(ServerLevel World) {
        calm(World);
    }

    @AfterBatch(batch = "scp457_diesUnderDrain")
    public static void afterDrain(ServerLevel World) {
        calm(World);
    }

    @AfterBatch(batch = "scp457_diesInRain")
    public static void afterRain(ServerLevel World) {
        calm(World);
    }

    @AfterBatch(batch = "scp457_refusesMoat")
    public static void afterMoat(ServerLevel World) {
        calm(World);
    }

    @AfterBatch(batch = "scp457_fleesBucket")
    public static void afterFlee(ServerLevel World) {
        calm(World);
    }

    @AfterBatch(batch = "scp457_stasisRoundTrip")
    public static void afterStasis(ServerLevel World) {
        calm(World);
    }

    @AfterBatch(batch = "scp457_controlledKillOnce")
    public static void afterControlled(ServerLevel World) {
        calm(World);
    }

    @AfterBatch(batch = "scp457_shackShelters")
    public static void afterShack(ServerLevel World) {
        calm(World);
    }

    @GameTest(template = Arena, timeoutTicks = 260, batch = "scp457_targetsClosest")
    public static void targetsClosest(GameTestHelper Helper) {
        begin(Helper);
        Scp457 Burning = burning(Helper, 8.5, 16.5);
        FakePlayer Far = watcher(Helper, 8.5, 28.5);
        DClass Near = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(8.5, stand(Helper), 11.5));
        Villager[] Middle = new Villager[1];
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Burning.getTarget() == Near, "SCP-457 did not pick the nearer D-Class" + state(Burning)))
                .thenExecute(() -> {
                    Near.discard();
                    Middle[0] = Helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(15.5, stand(Helper), 16.5));
                })
                .thenWaitUntil(() -> Helper.assertTrue(Burning.getTarget() == Middle[0], "SCP-457 did not move to the nearest villager" + state(Burning)))
                .thenExecute(() -> Middle[0].discard())
                .thenWaitUntil(() -> Helper.assertTrue(Burning.getTarget() == Far, "SCP-457 did not fall back to the player" + state(Burning)))
                .thenExecute(() -> {
                    dismiss(Helper, Far);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp457_ignitesOnHit")
    public static void ignitesOnHit(GameTestHelper Helper) {
        begin(Helper);
        Scp457 Burning = burning(Helper, 8.5, 16.5);
        Victim User = victim(Helper, 11.5, 16.5);
        float Full = User.getHealth();
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(User.getHealth() < Full, "SCP-457 never struck the player" + state(Burning)))
                .thenExecute(() -> {
                    Helper.assertTrue(User.isOnFire(), "the struck player is not burning");
                    Helper.assertTrue(User.getRemainingFireTicks() >= 60, "the struck player burns for only " + User.getRemainingFireTicks() + " ticks");
                    dismiss(Helper, User);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    private static void plankLane(GameTestHelper Helper) {
        for (int X = 3; X <= 28; X++) {
            for (int Z = 15; Z <= 17; Z++) {
                Helper.setBlock(new BlockPos(X, stand(Helper) - 1, Z), Blocks.OAK_PLANKS);
            }
        }
    }

    static int fires(GameTestHelper Helper) {
        int Found = 0;
        int Base = stand(Helper);
        for (int X = 0; X < 32; X++) {
            for (int Z = 0; Z < 32; Z++) {
                for (int Y = Base - 1; Y <= Base + 2; Y++) {
                    if (Helper.getBlockState(new BlockPos(X, Y, Z)).getBlock() instanceof BaseFireBlock) {
                        Found++;
                    }
                }
            }
        }
        return Found;
    }

    @GameTest(template = Arena, timeoutTicks = 240, batch = "scp457_leavesNoFloorFire")
    public static void leavesNoFloorFire(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        begin(Helper);
        rule(World, GameRules.RULE_DOFIRETICK, true);
        rule(World, GameRules.RULE_MOBGRIEFING, true);
        plankLane(Helper);
        Scp457 Burning = burning(Helper, 4.5, 16.5);
        FakePlayer Bait = watcher(Helper, 29.5, 16.5);
        double Start = Burning.getX();
        Helper.onEachTick(() -> Helper.assertTrue(fires(Helper) == 0, "SCP-457 set the floor alight" + state(Burning)));
        Helper.runAfterDelay(200, () -> {
            Helper.assertTrue(Burning.getX() - Start > 8.0, "SCP-457 never walked down the plank lane" + state(Burning));
            Helper.assertTrue(fires(Helper) == 0, "fire was left on the plank lane");
            dismiss(Helper, Bait);
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp457_footstepCrossing")
    public static void footstepCrossing(GameTestHelper Helper) {
        Helper.assertTrue(Scp457.crossed(0.4, 0.52, 0.5), "a left plant inside one tick was missed");
        Helper.assertTrue(Scp457.crossed(0.96, 0.03, 0.0), "a right plant across the cycle wrap was missed");
        Helper.assertTrue(!Scp457.crossed(0.1, 0.3, 0.5), "a plant was heard before the foot came down");
        Helper.assertTrue(!Scp457.crossed(0.55, 0.9, 0.5), "a left plant was heard twice");
        Helper.assertTrue(!Scp457.crossed(0.02, 0.2, 0.0), "a right plant was heard twice");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 240, batch = "scp457_villagerKillSpreads")
    public static void villagerKillSpreads(GameTestHelper Helper) {
        begin(Helper);
        Scp457 Burning = burning(Helper, 8.5, 16.5);
        Villager Bystander = Helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(10.5, stand(Helper), 16.5));
        Bystander.setHealth(2.0f);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(count(Helper) == 2, "killing the villager did not raise a second SCP-457" + state(Burning)))
                .thenExecute(() -> {
                    Helper.assertTrue(!Bystander.isAlive(), "the villager is still alive");
                    List<? extends Scp457> Found = Helper.getLevel().getEntities(Scp457Registry.Burning.get(), Other -> Other != Burning && Other.isAlive());
                    Helper.assertTrue(Found.size() == 1 && Found.get(0).isRising(), "the new instance did not rise from the spot");
                    Helper.assertTrue(Found.get(0).distanceToSqr(Bystander) < 9.0, "the new instance rose far from the villager");
                })
                .thenExecuteAfter(40, () -> {
                    Helper.assertTrue(count(Helper) == 2, "the kill was counted more than once: " + count(Helper));
                    tidy(Helper);
                })
                .thenSucceed();
    }

    private static void killWith(GameTestHelper Helper, Scp457 Killer, Villager Bystander) {
        DamageSource Source = Helper.getLevel().damageSources().source(Scp457Registry.BurnDamage, Killer);
        Bystander.hurt(Source, 1000.0f);
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp457_capHolds")
    public static void capHolds(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        begin(Helper);
        Scp457 First = null;
        for (int Index = 0; Index < Scp457Spread.MaxPerDimension; Index++) {
            Scp457 Made = burning(Helper, 2.5 + (Index % 8) * 3.0, 3.5 + (Index / 8) * 3.0);
            Made.setNoAi(true);
            if (First == null) {
                First = Made;
            }
        }
        Scp457 Killer = First;
        Helper.assertTrue(count(Helper) == Scp457Spread.MaxPerDimension, "the cap fill spawned " + count(Helper));
        Villager Bystander = Helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(16.5, stand(Helper), 24.5));
        killWith(Helper, Killer, Bystander);
        Helper.assertTrue(Scp457Spread.rise(World, Helper.absoluteVec(new Vec3(20.5, stand(Helper), 24.5)), 0.0f) == null, "a new instance rose past the cap");
        Helper.runAfterDelay(10, () -> {
            Helper.assertTrue(!Bystander.isAlive(), "the villager survived");
            Helper.assertTrue(count(Helper) == Scp457Spread.MaxPerDimension, "the cap of " + Scp457Spread.MaxPerDimension + " did not hold: " + count(Helper));
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp457_ruleHolds")
    public static void ruleHolds(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        begin(Helper);
        Helper.assertTrue(Scp457Registry.SpreadRule != null, "the scp457Spread game rule is not registered");
        Scp457 Killer = burning(Helper, 4.5, 4.5);
        Killer.setNoAi(true);
        rule(World, Scp457Registry.SpreadRule, false);
        Villager First = Helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(16.5, stand(Helper), 16.5));
        killWith(Helper, Killer, First);
        Helper.startSequence()
                .thenExecuteAfter(10, () -> {
                    Helper.assertTrue(!First.isAlive(), "the first villager survived");
                    Helper.assertTrue(count(Helper) == 1, "a new instance rose with scp457Spread off: " + count(Helper));
                    rule(World, Scp457Registry.SpreadRule, true);
                    Villager Second = Helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(22.5, stand(Helper), 16.5));
                    killWith(Helper, Killer, Second);
                })
                .thenExecuteAfter(10, () -> {
                    Helper.assertTrue(count(Helper) == 2, "no new instance rose with scp457Spread back on: " + count(Helper));
                    tidy(Helper);
                })
                .thenSucceed();
    }

    private static void assertDoused(GameTestHelper Helper, Scp457 Burning, String What) {
        Helper.assertTrue(Burning.isDeadOrDying() || Burning.isRemoved(), "SCP-457 survived " + What + state(Burning));
        Helper.assertTrue(Burning.deathKind() == Scp457.DeathWater, "SCP-457 died to " + What + " without the steam death" + state(Burning));
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp457_diesInWater")
    public static void diesInWater(GameTestHelper Helper) {
        begin(Helper);
        Scp457 Burning = burning(Helper, 16.5, 16.5);
        Burning.setNoAi(true);
        Helper.runAfterDelay(5, () -> Helper.setBlock(new BlockPos(16, stand(Helper), 16), Blocks.WATER));
        Helper.startSequence()
                .thenWaitUntil(() -> assertDoused(Helper, Burning, "standing in water"))
                .thenExecuteAfter(Scp457.CollapseTicks + 4, () -> {
                    Helper.assertTrue(Burning.isRemoved(), "SCP-457 never finished collapsing");
                    Helper.assertTrue(count(Helper) == 0, "a doused SCP-457 is still counted");
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp457_diesToSplash")
    public static void diesToSplash(GameTestHelper Helper) {
        begin(Helper);
        Scp457 Burning = burning(Helper, 16.5, 16.5);
        Burning.setNoAi(true);
        Helper.runAfterDelay(5, () -> {
            Vec3 Above = Burning.position().add(0.0, 3.2, 0.0);
            ThrownPotion Thrown = new ThrownPotion(Helper.getLevel(), Above.x, Above.y, Above.z);
            Thrown.setItem(PotionContents.createItemStack(Items.SPLASH_POTION, Potions.WATER));
            Thrown.setDeltaMovement(0.0, -0.8, 0.0);
            Helper.getLevel().addFreshEntity(Thrown);
        });
        Helper.startSequence()
                .thenWaitUntil(() -> assertDoused(Helper, Burning, "a thrown splash bottle of water"))
                .thenExecute(() -> tidy(Helper))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp457_diesUnderDrain")
    public static void diesUnderDrain(GameTestHelper Helper) {
        begin(Helper);
        BlockPos Cell = new BlockPos(16, stand(Helper), 16);
        for (BlockPos Side : List.of(Cell.north(), Cell.south(), Cell.east(), Cell.west())) {
            for (int Y = 0; Y < 3; Y++) {
                Helper.setBlock(Side.above(Y), Blocks.STONE);
            }
        }
        BlockPos Spout = Cell.above(3);
        Helper.setBlock(Spout, PlumbingRegistry.Drain.get());
        Scp457 Burning = burning(Helper, 16.5, 16.5);
        Helper.onEachTick(() -> {
            if (Helper.getBlockEntity(Spout) instanceof DrainBlockEntity Drain) {
                Drain.getBuffer().fill(new FluidStack(Fluids.WATER, DrainBlockEntity.Capacity), IFluidHandler.FluidAction.EXECUTE);
            }
        });
        Helper.startSequence()
                .thenWaitUntil(() -> assertDoused(Helper, Burning, "a drain pouring water"))
                .thenExecute(() -> tidy(Helper))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 240, batch = "scp457_diesInRain", skyAccess = true)
    public static void diesInRain(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        begin(Helper);
        World.setWeatherParameters(0, 6000, true, false);
        World.setRainLevel(1.0f);
        Scp457 Burning = burning(Helper, 16.5, 16.5);
        float Full = Burning.getMaxHealth();
        Helper.startSequence()
                .thenExecuteAfter(25, () -> {
                    Helper.assertTrue(Burning.isAlive(), "rain killed SCP-457 at once instead of over a few seconds" + state(Burning));
                    Helper.assertTrue(Burning.getHealth() < Full, "rain did not hurt SCP-457" + state(Burning));
                    Helper.assertTrue(Burning.isSteaming(), "SCP-457 is not steaming in the rain" + state(Burning));
                })
                .thenWaitUntil(() -> assertDoused(Helper, Burning, "the rain"))
                .thenExecute(() -> tidy(Helper))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 320, batch = "scp457_refusesMoat")
    public static void refusesMoat(GameTestHelper Helper) {
        begin(Helper);
        int Base = stand(Helper);
        for (int X = 10; X <= 14; X++) {
            for (int Z = 14; Z <= 18; Z++) {
                if (X == 10 || X == 14 || Z == 14 || Z == 18) {
                    Helper.setBlock(new BlockPos(X, Base - 1, Z), Blocks.WATER);
                }
            }
        }
        Victim User = victim(Helper, 12.5, 16.5);
        float Full = User.getHealth();
        Scp457 Burning = burning(Helper, 4.5, 16.5);
        double[] Closest = {Double.MAX_VALUE};
        boolean[] Hunted = {false};
        Helper.onEachTick(() -> {
            if (Burning.isRemoved()) {
                return;
            }
            Helper.assertTrue(Burning.isAlive() && !Burning.isInWater(), "SCP-457 stepped into the moat" + state(Burning));
            Closest[0] = Math.min(Closest[0], Burning.distanceTo(User));
            Hunted[0] |= Burning.getTarget() == User;
        });
        Helper.runAfterDelay(280, () -> {
            Helper.assertTrue(Hunted[0], "SCP-457 never targeted the player on the island" + state(Burning));
            Helper.assertTrue(Closest[0] < 5.0, "SCP-457 never approached the moat, closest " + Closest[0] + state(Burning));
            Helper.assertTrue(User.getHealth() >= Full, "SCP-457 reached the player across the moat");
            dismiss(Helper, User);
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp457_fleesBucket")
    public static void fleesBucket(GameTestHelper Helper) {
        begin(Helper);
        Scp457 Burning = burning(Helper, 16.5, 16.5);
        FakePlayer Holder = watcher(Helper, 12.5, 16.5);
        Holder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        double Start = Burning.distanceTo(Holder);
        Helper.onEachTick(() -> Helper.assertTrue(Burning.isRemoved() || Burning.isAlive(), "SCP-457 died while fleeing" + state(Burning)));
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Burning.distanceTo(Holder) > Start + 3.0, "SCP-457 did not back away from the bucket, distance "
                        + Burning.distanceTo(Holder) + state(Burning)))
                .thenExecute(() -> {
                    dismiss(Helper, Holder);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp457_stasisRoundTrip")
    public static void stasisRoundTrip(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        begin(Helper);
        Scp457 Burning = burning(Helper, 8.5, 16.5);
        Burning.hurt(World.damageSources().generic(), 13.0f);
        float Health = Burning.getHealth();
        FakePlayer User = watcher(Helper, 24.5, 4.5);
        ItemStack Pod = new ItemStack(StasisRegistry.podFor(FragmentRarity.Faint));
        Helper.assertTrue(StasisPods.refusal(User, Pod, Burning) == null, "a stasis pod refuses SCP-457");
        Helper.assertTrue(StasisPods.capture(World, User, Pod, Burning), "the stasis pod failed to capture SCP-457");
        Helper.assertTrue(Burning.isRemoved() && StasisPodItem.isOccupied(Pod), "the pod does not hold SCP-457");
        Helper.assertTrue(count(Helper) == 0, "a captured SCP-457 still counts toward the cap");
        Helper.assertTrue(StasisPods.breakOut(World, Pod, Helper.absoluteVec(new Vec3(16.5, stand(Helper), 16.5)), null), "SCP-457 could not be released");
        List<? extends Scp457> Released = World.getEntities(Scp457Registry.Burning.get(), Scp457::isAlive);
        Helper.assertTrue(Released.size() == 1, "expected one released SCP-457, found " + Released.size());
        Scp457 Back = Released.get(0);
        Helper.assertTrue(Math.abs(Back.getHealth() - Health) < 0.01f, "health changed in stasis: " + Health + " became " + Back.getHealth());
        Helper.runAfterDelay(5, () -> {
            Helper.assertTrue(Back.isAlive() && !Back.isRemoved(), "the released SCP-457 did not stay in the world");
            dismiss(Helper, User);
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp457_controlledKillOnce")
    public static void controlledKillOnce(GameTestHelper Helper) {
        begin(Helper);
        Scp457 Burning = burning(Helper, 8.5, 16.5);
        DClass Body = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(11.5, stand(Helper), 16.5));
        Victim User = victim(Helper, 13.5, 17.5);
        Helper.assertTrue(ControlManager.possess(User, Body), "the test player could not take control of the D-Class");
        Helper.assertTrue(ControlSessions.isControlling(User), "no control session started");
        User.setHealth(2.0f);
        boolean[] Pending = new boolean[1];
        boolean[] Gone = new boolean[1];
        Helper.onEachTick(() -> {
            if (Gone[0]) {
                return;
            }
            if (ControlManager.hasPendingDeath(User)) {
                Pending[0] = true;
                Helper.assertTrue(count(Helper) == 1, "the controlled death was counted before the deferred tick" + state(Burning));
                ControlManager.processDeath(User);
            }
            Helper.assertTrue(count(Helper) <= 2, "the controlled D-Class death was counted twice" + state(Burning));
        });
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(count(Helper) == 2, "the controlled D-Class death was never counted" + state(Burning) + " [player health "
                        + User.getHealth() + ", pending " + Pending[0] + ", controlling " + ControlSessions.isControlling(User) + ", deaths " + User.Deaths + "]"))
                .thenExecute(() -> {
                    Gone[0] = true;
                    Helper.assertTrue(Pending[0], "the death did not go through the deferred control path");
                    Helper.assertTrue(!ControlSessions.isControlling(User), "the player is still controlling a dead body");
                    Helper.assertTrue(User.Deaths == 0, "the controller died for real");
                    dismiss(Helper, User);
                })
                .thenExecuteAfter(40, () -> {
                    Helper.assertTrue(count(Helper) == 2, "the count changed after the death: " + count(Helper));
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp457_shackShelters", skyAccess = true)
    public static void shackShelters(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        begin(Helper);
        BlockPos Surface = Helper.absolutePos(new BlockPos(12, stand(Helper), 12));
        String Problem = Scp457Arrival.shackProblem(World, Surface);
        Helper.assertTrue(Problem == null, "the burnt shack does not fit on flat ground: " + Problem);
        Scp457Arrival.buildShack(World, Surface, World.getRandom());
        Vec3 Inside = new Vec3(Surface.getX() + 3.5, Surface.getY(), Surface.getZ() + 1.6);
        Helper.assertTrue(Scp457Arrival.place(World, Inside, 0.0f), "SCP-457 did not fit inside the shack");
        BlockPos Spot = BlockPos.containing(Inside);
        Helper.assertTrue(World.getHeight(Heightmap.Types.MOTION_BLOCKING, Spot.getX(), Spot.getZ()) > Spot.getY() + 2, "the spot inside the shack is open to the sky");
        Helper.assertTrue(World.getBlockState(Spot.offset(2, 0, 0)).is(Blocks.FIRE), "the hearth fire is missing");
        Helper.assertTrue(count(Helper) == 1, "expected one SCP-457 in the shack, found " + count(Helper));
        Helper.runAfterDelay(5, () -> {
            tidy(Helper);
            Helper.succeed();
        });
    }

    public static final class Victim extends FakePlayer {
        public int Deaths;

        public Victim(ServerLevel World, GameProfile Profile) {
            super(World, Profile);
            ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class, this, 0, "spawnInvulnerableTime");
        }

        @Override
        public boolean isInvulnerableTo(DamageSource Source) {
            return isRemoved() || getAbilities().invulnerable && !Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        }

        @Override
        public void die(DamageSource Source) {
            if (CommonHooks.onLivingDeath(this, Source)) {
                return;
            }
            Deaths++;
        }
    }
}
