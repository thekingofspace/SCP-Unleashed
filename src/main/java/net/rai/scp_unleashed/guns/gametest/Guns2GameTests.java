package net.rai.scp_unleashed.guns.gametest;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunAmmo;
import net.rai.scp_unleashed.guns.GunCraftingRecipe;
import net.rai.scp_unleashed.guns.GunCraftingRecipes;
import net.rai.scp_unleashed.guns.GunFiring;
import net.rai.scp_unleashed.guns.GunHeat;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunReloading;
import net.rai.scp_unleashed.guns.GunStats;
import net.rai.scp_unleashed.guns.GunVoiceBudget;
import net.rai.scp_unleashed.guns.GunsRegistry;
import net.rai.scp_unleashed.guns.npc.NpcGunner;
import net.rai.scp_unleashed.guns.npc.gametest.NpcGunnerGameTests;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Guns2GameTests {
    public static final String Arena = NpcGunnerGameTests.Arena;
    public static final double Lane = NpcGunnerGameTests.Lane;
    public static final double ShooterX = NpcGunnerGameTests.ShooterX;
    public static final float Sturdy = 400.0F;
    public static final float Tolerance = 0.01F;
    public static final int VoicePeakLimit = GunVoiceBudget.ClientLimit * 2 / 3;

    private Guns2GameTests() {
    }

    public static final class Shooter extends GunPerfListener {
        public Shooter(ServerLevel World, String Name, Vec3 Spot) {
            super(World, Name, Spot);
        }

        @Override
        public boolean isSpectator() {
            return false;
        }
    }

    private static Shooter shooter(GameTestHelper Helper, String Name, ItemStack Gun) {
        Shooter Made = new Shooter(Helper.getLevel(), Name, Helper.absoluteVec(new Vec3(ShooterX, NpcGunnerGameTests.stand(Helper), Lane)));
        Made.setItemInHand(InteractionHand.MAIN_HAND, Gun);
        return Made;
    }

    private static Husk gunner(GameTestHelper Helper, ItemStack Gun) {
        Husk Made = Helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(ShooterX, NpcGunnerGameTests.stand(Helper), Lane));
        Made.setItemSlot(EquipmentSlot.MAINHAND, Gun);
        return Made;
    }

    private static Pig pig(GameTestHelper Helper, double X) {
        Pig Made = Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(X, NpcGunnerGameTests.stand(Helper), Lane));
        Made.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Sturdy);
        Made.setHealth(Sturdy);
        return Made;
    }

    private static void tickHeld(Shooter User) {
        ItemStack Held = User.getMainHandItem();
        Held.inventoryTick(User.level(), User, User.getInventory().selected, true);
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "guns2_stats")
    public static void statsAreBalanced(GameTestHelper Helper) {
        GunStats Combat = GunKind.CombatShotgun.getStats();
        GunStats Pump = GunKind.Shotgun.getStats();
        GunStats Breaker = GunKind.DoubleBarrel.getStats();
        GunStats Machine = GunKind.MachineGun.getStats();
        GunStats Rifle = GunKind.AssaultRifle.getStats();
        Helper.assertTrue(GunKind.CombatShotgun.getAmmo() == GunsRegistry.ShotgunShells.get(), "the combat shotgun does not take shotgun shells");
        Helper.assertTrue(GunKind.MachineGun.getAmmo() == GunsRegistry.RifleRounds.get(), "the machine gun does not take rifle rounds");
        Helper.assertTrue(!Combat.isAutomatic() && Combat.getCapacity() == 8 && Combat.getPellets() == 8, "the combat shotgun is not a semi-automatic 8 shell, 8 pellet gun");
        Helper.assertTrue(Combat.getInterval() < Pump.getInterval(), "the combat shotgun follows up no faster than the pump shotgun");
        Helper.assertTrue(Combat.getDamage() < Pump.getDamage(), "the combat shotgun pellets hit as hard as the pump shotgun's");
        Helper.assertTrue(Combat.getPelletSpread() < Breaker.getPelletSpread() && Combat.getPelletSpread() < Pump.getPelletSpread(), "the combat shotgun pattern is not the tightest");
        Helper.assertTrue(Machine.isAutomatic() && Machine.getCapacity() == 100, "the machine gun is not a 100 round automatic");
        Helper.assertTrue(Machine.getDamage() > Rifle.getDamage() && Machine.getInterval() <= Rifle.getInterval(), "the machine gun does not hit harder and fire at least as fast as the rifle");
        Helper.assertTrue(Machine.getReloadTicks() > Rifle.getReloadTicks(), "the machine gun reloads no slower than the rifle");
        Helper.assertTrue(Machine.getHipSpread() > Rifle.getHipSpread() && Machine.getBloomLimit() > Rifle.getBloomLimit(), "the machine gun is not less accurate from the hip");
        Helper.assertTrue(Machine.getBloomPerShot() > Machine.getBloomRecovery() * Machine.getInterval(), "machine gun bloom never builds up in sustained fire");
        Helper.assertTrue(Machine.getCrouchSpread() < Rifle.getCrouchSpread() && Machine.getCrouchRecoil() < 1.0F, "crouching does not brace the machine gun");
        Helper.assertTrue(Machine.getAimTicks() > Rifle.getAimTicks(), "the machine gun aims no slower than the rifle");
        Helper.assertTrue(Machine.usesHeat() && Machine.shotsToOverheat() > 30 && Machine.shotsToOverheat() < Machine.getCapacity(), "machine gun overheat out of range: "
                + Machine.shotsToOverheat());
        Helper.assertFalse(Rifle.usesHeat() || Combat.usesHeat(), "a gun other than the machine gun overheats");
        ItemAttributeModifiers Modifiers = new ItemStack(GunsRegistry.MachineGun.get()).getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        boolean Slows = Modifiers.modifiers().stream().anyMatch(Listed -> Listed.attribute().value() == Attributes.MOVEMENT_SPEED.value() && Listed.modifier().amount() < 0.0
                && Listed.slot() == EquipmentSlotGroup.MAINHAND);
        Helper.assertTrue(Slows, "holding the machine gun does not slow you down");
        Helper.assertTrue(new ItemStack(GunsRegistry.CombatShotgun.get()).getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers().isEmpty(),
                "the combat shotgun slows you down");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "guns2_fire_combat")
    public static void combatShotgunFiresItsPellets(GameTestHelper Helper) {
        GunStats Stats = GunKind.CombatShotgun.getStats();
        ItemStack Gun = GunItem.loaded(GunKind.CombatShotgun);
        Husk Gunner = gunner(Helper, Gun);
        Pig Target = pig(Helper, ShooterX + 3.0);
        float Expected = Stats.getDamage() * Stats.getPellets();
        Helper.startSequence()
                .thenExecute(() -> {
                    Helper.assertTrue(NpcGunner.fireAt(Gunner, Gun, Target, null, 1.0F), "the combat shotgun did not fire");
                    Helper.assertTrue(Math.abs(Sturdy - Target.getHealth() - Expected) < Tolerance, "one shell did " + (Sturdy - Target.getHealth()) + " damage, expected " + Expected);
                    Helper.assertTrue(NpcGunner.rounds(Gunner, Gun) == Stats.getCapacity() - 1, "one shot used " + (Stats.getCapacity() - NpcGunner.rounds(Gunner, Gun)) + " shells");
                    Helper.assertFalse(NpcGunner.fireAt(Gunner, Gun, Target, null, 1.0F), "fired again before the interval");
                })
                .thenExecuteAfter(Stats.getInterval() - 1, () -> Helper.assertFalse(NpcGunner.ready(Gunner, Gun), "ready a tick early"))
                .thenExecuteAfter(1, () -> Helper.assertTrue(NpcGunner.fireAt(Gunner, Gun, Target, null, 1.0F), "no follow-up shot after " + Stats.getInterval() + " ticks"))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "guns2_fire_machine")
    public static void machineGunFiresHeavyRounds(GameTestHelper Helper) {
        GunStats Stats = GunKind.MachineGun.getStats();
        ItemStack Gun = GunItem.loaded(GunKind.MachineGun);
        Husk Gunner = gunner(Helper, Gun);
        Pig Target = pig(Helper, ShooterX + 4.0);
        Helper.startSequence()
                .thenExecute(() -> {
                    Helper.assertTrue(NpcGunner.fireAt(Gunner, Gun, Target, null, 1.0F), "the machine gun did not fire");
                    Helper.assertTrue(Math.abs(Sturdy - Target.getHealth() - Stats.getDamage()) < Tolerance, "one round did " + (Sturdy - Target.getHealth()) + " damage");
                    Helper.assertFalse(NpcGunner.fireAt(Gunner, Gun, Target, null, 1.0F), "fired again before the interval");
                })
                .thenExecuteAfter(Stats.getInterval(), () -> Helper.assertTrue(NpcGunner.fireAt(Gunner, Gun, Target, null, 1.0F), "no second round after the interval"))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "guns2_magazine")
    public static void magazinesReloadFromInventory(GameTestHelper Helper) {
        ItemStack Combat = GunItem.loaded(GunKind.CombatShotgun);
        Shooter User = shooter(Helper, "guns2_mag", Combat);
        User.getInventory().add(new ItemStack(GunsRegistry.ShotgunShells.get(), 20));
        User.getInventory().add(new ItemStack(GunsRegistry.RifleRounds.get(), 30));
        GunStats CombatStats = GunKind.CombatShotgun.getStats();
        GunStats MachineStats = GunKind.MachineGun.getStats();
        int[] Fired = {0};
        Helper.onEachTick(() -> tickHeld(User));
        Helper.startSequence()
                .thenExecute(() -> {
                    Helper.assertTrue(GunItem.getAmmo(Combat) == CombatStats.getCapacity(), "the combat shotgun did not start full");
                    GunFiring.requestFire(User, GunKind.CombatShotgun.ordinal(), 1);
                    GunFiring.requestFire(User, GunKind.CombatShotgun.ordinal(), 2);
                    Helper.assertTrue(GunItem.getAmmo(Combat) == CombatStats.getCapacity() - 1, "a semi-automatic double tap fired twice in one tick");
                })
                .thenExecuteAfter(CombatStats.getInterval(), () -> GunFiring.requestFire(User, GunKind.CombatShotgun.ordinal(), 3))
                .thenExecuteAfter(CombatStats.getInterval(), () -> {
                    GunFiring.requestFire(User, GunKind.CombatShotgun.ordinal(), 4);
                    Helper.assertTrue(GunItem.getAmmo(Combat) == CombatStats.getCapacity() - 3, "three shots left " + GunItem.getAmmo(Combat) + " shells");
                    Helper.assertTrue(GunAmmo.count(User, GunKind.CombatShotgun) == 20, "firing took shells from the inventory");
                    GunReloading.start(User);
                    Helper.assertTrue(GunItem.getReload(Combat) != null, "the magazine swap did not start");
                })
                .thenExecuteAfter(CombatStats.getReloadTicks() / 2, () -> {
                    Helper.assertTrue(GunItem.getReload(Combat) != null, "the magazine swap finished early");
                    GunFiring.requestFire(User, GunKind.CombatShotgun.ordinal(), 5);
                    Helper.assertTrue(GunItem.getAmmo(Combat) == CombatStats.getCapacity() - 3, "fired during the magazine swap");
                })
                .thenWaitUntil(() -> Helper.assertTrue(GunItem.getReload(Combat) == null, "the magazine swap never finished"))
                .thenExecute(() -> {
                    Helper.assertTrue(GunItem.getAmmo(Combat) == CombatStats.getCapacity(), "the magazine swap left " + GunItem.getAmmo(Combat) + " shells");
                    Helper.assertTrue(GunAmmo.count(User, GunKind.CombatShotgun) == 17, "the swap took " + (20 - GunAmmo.count(User, GunKind.CombatShotgun)) + " shells, expected 3");
                    ItemStack Machine = GunItem.loaded(GunKind.MachineGun);
                    User.setItemInHand(InteractionHand.MAIN_HAND, Machine);
                })
                .thenExecuteFor(MachineStats.getInterval() * 10, () -> {
                    ItemStack Machine = User.getMainHandItem();
                    if (Helper.getTick() % MachineStats.getInterval() == 0 && Fired[0] < 10) {
                        int Before = GunItem.getAmmo(Machine);
                        GunFiring.requestFire(User, GunKind.MachineGun.ordinal(), 10 + Fired[0]);
                        if (GunItem.getAmmo(Machine) < Before) {
                            Fired[0]++;
                        }
                    }
                })
                .thenExecute(() -> {
                    ItemStack Machine = User.getMainHandItem();
                    Helper.assertTrue(Fired[0] == 10 && GunItem.getAmmo(Machine) == MachineStats.getCapacity() - 10, "full auto fired " + Fired[0] + " rounds, "
                            + GunItem.getAmmo(Machine) + " left");
                    GunReloading.start(User);
                    Helper.assertTrue(GunItem.getReload(Machine) != null, "the belt box swap did not start");
                })
                .thenWaitUntil(() -> Helper.assertTrue(GunItem.getReload(User.getMainHandItem()) == null, "the belt box swap never finished"))
                .thenExecute(() -> {
                    Helper.assertTrue(GunItem.getAmmo(User.getMainHandItem()) == MachineStats.getCapacity(), "the belt box swap did not refill the gun");
                    Helper.assertTrue(GunAmmo.count(User, GunKind.MachineGun) == 20, "the belt box swap took " + (30 - GunAmmo.count(User, GunKind.MachineGun)) + " rounds, expected 10");
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "guns2_overheat")
    public static void sustainedFireOverheats(GameTestHelper Helper) {
        GunStats Stats = GunKind.MachineGun.getStats();
        ItemStack Gun = GunItem.loaded(GunKind.MachineGun);
        Shooter User = shooter(Helper, "guns2_heat", Gun);
        int Expected = Stats.shotsToOverheat();
        int[] Fired = {0};
        long[] VentedAt = {-1L};
        int[] Spoiled = {0};
        Helper.onEachTick(() -> {
            tickHeld(User);
            long Now = Helper.getLevel().getGameTime();
            if (VentedAt[0] < 0 && Helper.getTick() % Stats.getInterval() == 0) {
                int Before = GunItem.getAmmo(Gun);
                GunFiring.requestFire(User, GunKind.MachineGun.ordinal(), Fired[0]);
                if (GunItem.getAmmo(Gun) < Before) {
                    Fired[0]++;
                }
                if (GunHeat.venting(Gun, Now)) {
                    VentedAt[0] = Now;
                }
            } else if (VentedAt[0] >= 0 && GunHeat.venting(Gun, Now)) {
                int Before = GunItem.getAmmo(Gun);
                GunFiring.requestFire(User, GunKind.MachineGun.ordinal(), 1000);
                if (GunItem.getAmmo(Gun) != Before) {
                    Spoiled[0]++;
                }
            }
        });
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(VentedAt[0] >= 0, "never overheated after " + Fired[0] + " rounds"))
                .thenExecute(() -> {
                    Helper.assertTrue(Fired[0] == Expected, "overheated after " + Fired[0] + " rounds, expected " + Expected);
                    Helper.assertTrue(GunItem.getAmmo(Gun) == Stats.getCapacity() - Expected, "sustained fire spent the wrong number of rounds");
                })
                .thenWaitUntil(() -> Helper.assertTrue(GunHeat.vent(Gun) == null, "the vent never cleared"))
                .thenExecute(() -> {
                    long Took = Helper.getLevel().getGameTime() - VentedAt[0];
                    Helper.assertTrue(Spoiled[0] == 0, "fired " + Spoiled[0] + " times while venting");
                    Helper.assertTrue(Math.abs(Took - Stats.getVentTicks()) <= 1, "the vent took " + Took + " ticks");
                    Helper.assertTrue(GunHeat.level(Gun, Stats, Helper.getLevel().getGameTime()) == 0.0F, "heat stayed after venting");
                    int Before = GunItem.getAmmo(Gun);
                    GunFiring.requestFire(User, GunKind.MachineGun.ordinal(), 2000);
                    Helper.assertTrue(GunItem.getAmmo(Gun) == Before - 1, "could not fire after the vent cleared");
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "guns2_heat")
    public static void heatCoolsBetweenBursts(GameTestHelper Helper) {
        GunStats Stats = GunKind.MachineGun.getStats();
        ItemStack Gun = new ItemStack(GunsRegistry.MachineGun.get());
        Helper.assertFalse(GunHeat.addShot(Gun, Stats, 100L), "one round overheated the gun");
        Helper.assertTrue(Math.abs(GunHeat.level(Gun, Stats, 100L) - Stats.getHeatPerShot()) < Tolerance, "a round added the wrong heat");
        Helper.assertTrue(Math.abs(GunHeat.level(Gun, Stats, 105L) - (Stats.getHeatPerShot() - 5 * Stats.getHeatCooling())) < Tolerance, "heat did not cool over time");
        Helper.assertTrue(GunHeat.level(Gun, Stats, 100000L) == 0.0F, "heat went below zero or never cooled");
        ItemStack Rifle = new ItemStack(GunsRegistry.AssaultRifle.get());
        Helper.assertFalse(GunHeat.addShot(Rifle, GunKind.AssaultRifle.getStats(), 100L) || Rifle.has(GunsRegistry.Heat.get()), "the rifle picked up heat");
        Helper.assertFalse(GunHeat.cooled(Gun, 100L), "a gun that never vented cooled");
        long Now = 200L;
        boolean Vented = false;
        for (int Shot = 0; Shot < Stats.shotsToOverheat() + 5 && !Vented; Shot++) {
            Vented = GunHeat.addShot(Gun, Stats, Now);
            Now += Stats.getInterval();
        }
        Helper.assertTrue(Vented, "rapid fire never vented");
        long Started = Now - Stats.getInterval();
        Helper.assertTrue(GunHeat.venting(Gun, Started + Stats.getVentTicks() - 1), "the vent ended early");
        Helper.assertFalse(GunHeat.cooled(Gun, Started + Stats.getVentTicks() - 1), "cooled while still venting");
        Helper.assertTrue(GunHeat.cooled(Gun, Started + Stats.getVentTicks()), "the vent never cleared");
        Helper.assertTrue(GunHeat.vent(Gun) == null, "the vent timer stayed on the gun");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "guns2_npc_sustain")
    public static void npcMachineGunSustainsAndVents(GameTestHelper Helper) {
        GunStats Stats = GunKind.MachineGun.getStats();
        ItemStack Gun = GunItem.loaded(GunKind.MachineGun);
        Husk Gunner = gunner(Helper, Gun);
        Pig Target = pig(Helper, ShooterX + 8.0);
        Target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100000.0);
        Target.setHealth(100000.0F);
        int[] Fired = {0};
        long[] VentedAt = {-1L};
        long[] FirstAt = {-1L};
        Helper.onEachTick(() -> {
            long Now = Helper.getLevel().getGameTime();
            if (NpcGunner.fireAt(Gunner, Gun, Target, null, 1.0F)) {
                Fired[0]++;
                if (FirstAt[0] < 0) {
                    FirstAt[0] = Now;
                }
            }
            if (VentedAt[0] < 0 && GunHeat.venting(Gun, Now)) {
                VentedAt[0] = Now;
            }
        });
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(VentedAt[0] >= 0, "an NPC never overheated after " + Fired[0] + " rounds"))
                .thenExecute(() -> {
                    long Span = VentedAt[0] - FirstAt[0];
                    Helper.assertTrue(Fired[0] == Stats.shotsToOverheat(), "an NPC overheated after " + Fired[0] + " rounds");
                    Helper.assertTrue(Span == (long) (Fired[0] - 1) * Stats.getInterval(), "full auto did not sustain its cadence: " + Fired[0] + " rounds in " + Span + " ticks");
                    Helper.assertFalse(NpcGunner.ready(Gunner, Gun), "an NPC is ready while venting");
                    Helper.assertFalse(NpcGunner.reloading(Gunner, Gun), "an NPC reloaded a part-full belt");
                })
                .thenWaitUntil(() -> Helper.assertTrue(Fired[0] > Stats.shotsToOverheat(), "an NPC never fired again after venting"))
                .thenExecute(() -> {
                    Helper.assertTrue(Helper.getLevel().getGameTime() - VentedAt[0] >= Stats.getVentTicks(), "an NPC fired before the vent cleared");
                    Helper.assertTrue(Target.getHealth() < 100000.0F, "the target was never hit");
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "guns2_voices")
    public static void sustainedFireRespectsVoiceBudget(GameTestHelper Helper) {
        ItemStack Gun = GunItem.loaded(GunKind.MachineGun);
        Husk Gunner = gunner(Helper, Gun);
        Pig Target = pig(Helper, ShooterX + 8.0);
        Target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100000.0);
        Target.setHealth(100000.0F);
        GunPerfListener Ear = new GunPerfListener(Helper.getLevel(), "guns2_ear", Helper.absoluteVec(new Vec3(ShooterX, NpcGunnerGameTests.stand(Helper), Lane + 2.0)));
        Ear.enlist(true);
        int[] Fired = {0};
        Helper.onEachTick(() -> {
            if (Fired[0] < 30 && NpcGunner.fireAt(Gunner, Gun, Target, null, 1.0F)) {
                Fired[0]++;
            }
            Ear.sample();
        });
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Fired[0] >= 30, "only " + Fired[0] + " rounds fired"))
                .thenIdle(20)
                .thenExecute(() -> {
                    int Peak = Ear.keptPeak();
                    int Heard = Ear.managed();
                    Ear.enlist(false);
                    Helper.assertTrue(Heard >= 30, "a nearby listener heard only " + Heard + " gun layers for 30 rounds");
                    Helper.assertTrue(Peak <= VoicePeakLimit, "30 rounds of sustained fire held " + Peak + " voices, the limit is " + VoicePeakLimit);
                    Helper.assertTrue(Ear.refused() == 0, "the client budget refused " + Ear.refused() + " layers from one machine gun");
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "guns2_bench")
    public static void benchRecipesBuildBothGuns(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        List<String> Seen = new ArrayList<>();
        for (RecipeHolder<GunCraftingRecipe> Listed : GunCraftingRecipes.all(World)) {
            Seen.add(BuiltInRegistries.ITEM.getKey(Listed.value().getResult().getItem()).getPath());
        }
        Helper.assertTrue(Seen.contains("combat_shotgun") && Seen.contains("machine_gun"), "the bench does not list both new guns: " + Seen);
        for (String Name : List.of("combat_shotgun", "machine_gun")) {
            Optional<RecipeHolder<?>> Found = World.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "guns/gun_crafting/" + Name));
            Helper.assertTrue(Found.isPresent() && Found.get().value() instanceof GunCraftingRecipe, "no gun bench recipe for " + Name);
            GunCraftingRecipe Formula = (GunCraftingRecipe) Found.get().value();
            Helper.assertTrue(BuiltInRegistries.ITEM.getKey(Formula.getResult().getItem()).getPath().equals(Name), Name + " recipe makes the wrong item");
            List<ItemStack> Supplies = new ArrayList<>();
            Formula.getInputs().forEach(Need -> Supplies.add(new ItemStack(Need.ingredient().getItems()[0].getItem(), Need.count())));
            Helper.assertTrue(Formula.plan(Supplies) != null, Name + " cannot be built from its own parts");
            Supplies.get(1).shrink(1);
            Helper.assertTrue(Formula.plan(Supplies) == null, Name + " can be built with a part missing");
        }
        Optional<RecipeHolder<?>> Barrel = World.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "guns/heavy_barrel"));
        Helper.assertTrue(Barrel.isPresent() && Barrel.get().value().getResultItem(World.registryAccess()).is(GunsRegistry.HeavyBarrel.get()), "no heavy barrel recipe");
        boolean UsesLongBarrel = Barrel.get().value().getIngredients().stream().anyMatch(Need -> Need.test(new ItemStack(GunsRegistry.LongBarrel.get())));
        Helper.assertTrue(UsesLongBarrel, "the heavy barrel does not build on a long barrel");
        Helper.succeed();
    }
}
