package net.rai.scp_unleashed.scp548.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.EffectCures;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.cooler.CoolerBlock;
import net.rai.scp_unleashed.cooler.CoolerBlockEntity;
import net.rai.scp_unleashed.cooler.CoolerRegistry;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.scp500.Scp500Panacea;
import net.rai.scp_unleashed.scp538.gametest.Scp538TestPlayer;
import net.rai.scp_unleashed.scp548.Scp548Chill;
import net.rai.scp_unleashed.scp548.Scp548Registry;
import net.rai.scp_unleashed.scp548.Scp548Spider;
import net.rai.scp_unleashed.scp548.Scp548Swarm;
import net.rai.scp_unleashed.stranger.StrangerEncounterCondition;
import net.rai.scp_unleashed.stranger.StrangerGift;
import net.rai.scp_unleashed.stranger.StrangerPlan;
import net.rai.scp_unleashed.transferpod.TransferPodContent;
import net.rai.scp_unleashed.transferpod.TransferPods;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp548GameTests {
    public static final String Arena = "scp548_arena";
    public static final BlockPos Middle = new BlockPos(7, 2, 7);
    public static final BlockPos CoolerPos = new BlockPos(7, 2, 3);

    private Scp548GameTests() {
    }

    static Scp548Spider spider(GameTestHelper Helper, Vec3 Relative, boolean Still) {
        ServerLevel World = Helper.getLevel();
        Scp548Spider Spider = Scp548Registry.Spider.get().create(World);
        Vec3 Where = Helper.absoluteVec(Relative);
        Spider.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
        Spider.setPersistenceRequired();
        Spider.setNoAi(Still);
        World.addFreshEntity(Spider);
        return Spider;
    }

    static Scp538TestPlayer player(GameTestHelper Helper, Vec3 Relative, String Name, GameType Mode) {
        ServerLevel World = Helper.getLevel();
        Scp538TestPlayer User = new Scp538TestPlayer(World, Name);
        Vec3 Where = Helper.absoluteVec(Relative);
        User.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(Mode);
        return User;
    }

    static void dismiss(GameTestHelper Helper, ServerPlayer User) {
        if (!User.isRemoved()) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    static List<Scp548Spider> spiders(GameTestHelper Helper) {
        AABB Box = new AABB(Helper.absolutePos(BlockPos.ZERO)).expandTowards(16.0, 8.0, 16.0).inflate(2.0);
        return Helper.getLevel().getEntitiesOfClass(Scp548Spider.class, Box, Entity::isAlive);
    }

    static void sweep(GameTestHelper Helper) {
        for (Scp548Spider Spider : spiders(Helper)) {
            Spider.discard();
        }
    }

    static String describe(Scp548Spider Spider) {
        return "[mood " + Spider.mood() + ", chill " + Spider.chill() + ", target " + Spider.getTarget() + ", love " + Spider.isInLove() + ", age " + Spider.getAge() + "]";
    }

    static void cooler(GameTestHelper Helper, AtomicBoolean Fed) {
        Helper.setBlock(CoolerPos, CoolerRegistry.Cooler.get().defaultBlockState().setValue(CoolerBlock.Facing, Direction.SOUTH));
        Helper.onEachTick(() -> {
            if (Fed.get() && Helper.getLevel().getBlockEntity(Helper.absolutePos(CoolerPos)) instanceof CoolerBlockEntity Device) {
                Device.insertPower(Device.getPowerDemand());
            }
        });
    }

    static boolean coolerOn(GameTestHelper Helper) {
        BlockEntity Found = Helper.getLevel().getBlockEntity(Helper.absolutePos(CoolerPos));
        return Found instanceof CoolerBlockEntity Device && Device.isActive();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp548_auraRange")
    public static void auraRange(GameTestHelper Helper) {
        Scp548Spider Spider = spider(Helper, Vec3.atBottomCenterOf(Middle), true);
        Scp538TestPlayer Near = player(Helper, Vec3.atBottomCenterOf(Middle).add(1.0, 0.0, 0.0), "scp548_near", GameType.SURVIVAL);
        Scp538TestPlayer Far = player(Helper, Vec3.atBottomCenterOf(Middle).add(0.0, 0.0, Scp548Chill.AuraRadius + 1.5), "scp548_far", GameType.SURVIVAL);
        Helper.startSequence().thenIdle(40).thenExecute(() -> {
            try {
                Helper.assertTrue(Scp548Chill.inAura(Spider, Near), "The near player is not inside the aura");
                Helper.assertFalse(Scp548Chill.inAura(Spider, Far), "The far player is inside the aura");
                Helper.assertTrue(Near.getTicksFrozen() >= 60, "Standing right beside the spider did not freeze the player: " + Near.getTicksFrozen());
                Helper.assertTrue(Far.getTicksFrozen() == 0, "A player " + (Scp548Chill.AuraRadius + 1.5) + " blocks away froze: " + Far.getTicksFrozen());
            } finally {
                dismiss(Helper, Near);
                dismiss(Helper, Far);
                sweep(Helper);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 140, batch = "scp548_auraDamage")
    public static void auraDamage(GameTestHelper Helper) {
        Scp548Spider Spider = spider(Helper, Vec3.atBottomCenterOf(Middle), true);
        Scp538TestPlayer Near = player(Helper, Vec3.atBottomCenterOf(Middle).add(0.8, 0.0, 0.0), "scp548_frozen", GameType.SURVIVAL);
        Near.setTicksFrozen(Near.getTicksRequiredToFreeze());
        float Before = Near.getHealth();
        Helper.startSequence().thenIdle(60).thenExecute(() -> {
            try {
                Helper.assertTrue(Near.isFullyFrozen(), "The player thawed beside the spider");
                Helper.assertTrue(Near.getHealth() < Before, "Freezing beside the spider did no damage: " + Near.getHealth());
                Helper.assertTrue(Spider.isAlive() && Spider.getHealth() == Spider.getMaxHealth(), "The spider was hurt by its own cold: " + Spider.getHealth() + "/" + Spider.getMaxHealth()
                        + " last " + (Spider.getLastDamageSource() == null ? "none" : Spider.getLastDamageSource().getMsgId()));
            } finally {
                dismiss(Helper, Near);
                sweep(Helper);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp548_leatherWard")
    public static void leatherWard(GameTestHelper Helper) {
        spider(Helper, Vec3.atBottomCenterOf(Middle), true);
        Scp538TestPlayer Dressed = player(Helper, Vec3.atBottomCenterOf(Middle).add(0.8, 0.0, 0.0), "scp548_dressed", GameType.SURVIVAL);
        Dressed.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        Dressed.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        Dressed.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
        Dressed.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
        Scp538TestPlayer Partial = player(Helper, Vec3.atBottomCenterOf(Middle).add(-0.8, 0.0, 0.0), "scp548_partial", GameType.SURVIVAL);
        Partial.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
        Scp538TestPlayer Bare = player(Helper, Vec3.atBottomCenterOf(Middle).add(0.0, 0.0, 0.8), "scp548_bare", GameType.SURVIVAL);
        Helper.startSequence().thenIdle(40).thenExecute(() -> {
            try {
                Helper.assertTrue(Dressed.getTicksFrozen() == 0, "A full set of leather did not keep the cold out: " + Dressed.getTicksFrozen());
                Helper.assertTrue(Partial.getTicksFrozen() > 0 && Partial.getTicksFrozen() < Bare.getTicksFrozen(),
                        "Leather boots alone did not slow the cold: partial " + Partial.getTicksFrozen() + ", bare " + Bare.getTicksFrozen());
            } finally {
                dismiss(Helper, Dressed);
                dismiss(Helper, Partial);
                dismiss(Helper, Bare);
                sweep(Helper);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp548_biteFrostbite")
    public static void biteFrostbite(GameTestHelper Helper) {
        Scp548Spider Spider = spider(Helper, Vec3.atBottomCenterOf(Middle), true);
        DClass Body = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), Middle.east(3));
        float[] Health = new float[2];
        AtomicBoolean Bitten = new AtomicBoolean(false);
        AtomicBoolean Venom = new AtomicBoolean(false);
        Helper.onEachTick(() -> {
            if (Bitten.get() && Body.getLastDamageSource() != null && Body.getLastDamageSource().is(DamageTypeTags.IS_FREEZING)) {
                Venom.set(true);
            }
        });
        Helper.startSequence().thenIdle(2).thenExecute(() -> {
            Health[0] = Body.getHealth();
            Helper.assertTrue(Spider.doHurtTarget(Body), "The bite did not land on the D-Class");
            Helper.assertTrue(Scp548Chill.isFrostbitten(Body), "The bite did not frostbite the D-Class");
            Helper.assertTrue(Body.getHealth() < Health[0], "The bite did no damage");
            Helper.assertTrue(Spider.retreating() > 0, "The spider did not back off after biting");
            Health[1] = Body.getHealth();
            Bitten.set(true);
        }).thenIdle(90).thenExecute(() -> {
            try {
                Helper.assertTrue(Scp548Chill.isFrostbitten(Body), "The frostbite wore off too soon");
                Helper.assertTrue(Body.getTicksFrozen() >= Body.getTicksRequiredToFreeze() - 2, "Frostbite did not keep the D-Class frozen: " + Body.getTicksFrozen());
                Helper.assertTrue(Venom.get(), "Frostbite did no cold damage over time");
                Helper.assertTrue(Scp548Swarm.isPrey(Body), "A D-Class is not treated as prey");
            } finally {
                Body.discard();
                sweep(Helper);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp548_frostbiteCures")
    public static void frostbiteCures(GameTestHelper Helper) {
        Scp538TestPlayer First = player(Helper, Vec3.atBottomCenterOf(Middle), "scp548_patient", GameType.SURVIVAL);
        Scp538TestPlayer Second = player(Helper, Vec3.atBottomCenterOf(Middle.west(2)), "scp548_milk", GameType.SURVIVAL);
        Scp538TestPlayer Creative = player(Helper, Vec3.atBottomCenterOf(Middle.east(2)), "scp548_creative", GameType.CREATIVE);
        try {
            Helper.assertTrue(Scp548Chill.bite(First, null), "The player could not be frostbitten");
            Helper.assertTrue(Scp500Panacea.dose(First, null), "SCP-500 could not be given");
            Helper.assertFalse(Scp548Chill.isFrostbitten(First), "SCP-500 did not cure frostbite");
            Helper.assertTrue(First.getTicksFrozen() == 0, "SCP-500 left the player frozen");
            Helper.assertTrue(Scp548Chill.bite(Second, null), "The second player could not be frostbitten");
            Second.removeEffectsCuredBy(EffectCures.MILK);
            Helper.assertFalse(Scp548Chill.isFrostbitten(Second), "Milk did not cure frostbite");
            Helper.assertFalse(Scp548Chill.bite(Creative, null), "A creative player was frostbitten");
        } finally {
            dismiss(Helper, First);
            dismiss(Helper, Second);
            dismiss(Helper, Creative);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp548_snowballBreeding")
    public static void snowballBreeding(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp548Spider First = spider(Helper, Vec3.atBottomCenterOf(Middle), false);
        Scp548Spider Second = spider(Helper, Vec3.atBottomCenterOf(Middle).add(0.5, 0.0, 0.0), false);
        Scp538TestPlayer Keeper = player(Helper, Vec3.atBottomCenterOf(Middle.south(2)), "scp548_keeper", GameType.SURVIVAL);
        try {
            Keeper.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SNOWBALL, 4));
            Helper.assertTrue(First.mobInteract(Keeper, InteractionHand.MAIN_HAND).consumesAction(), "Feeding the first spider a snowball did nothing");
            Helper.assertTrue(Second.mobInteract(Keeper, InteractionHand.MAIN_HAND).consumesAction(), "Feeding the second spider a snowball did nothing");
            Helper.assertTrue(First.isInLove() && Second.isInLove(), "The fed spiders are not ready to breed " + describe(First) + describe(Second));
            Helper.assertTrue(Keeper.getMainHandItem().getCount() == 2, "Feeding did not use up the snowballs");
            Helper.assertFalse(First.isFood(new ItemStack(Items.WHEAT)), "The spiders take food other than snowballs");
            Helper.assertTrue(First.canMate(Second), "Two fed spiders cannot mate");
            int Before = spiders(Helper).size();
            First.spawnChildFromBreeding(World, Second);
            List<Scp548Spider> After = spiders(Helper);
            Helper.assertTrue(After.size() == Before + Scp548Spider.Young, "Breeding made " + (After.size() - Before) + " young instead of " + Scp548Spider.Young);
            Helper.assertTrue(After.stream().filter(Scp548Spider::isBaby).count() == Scp548Spider.Young, "The new spiders are not young");
            Helper.assertTrue(First.getAge() == Scp548Spider.BreedCooldown && Second.getAge() == Scp548Spider.BreedCooldown, "The parents have no breeding cooldown");
            Helper.assertFalse(First.isInLove() || Second.isInLove(), "The parents are still in love after breeding");
        } finally {
            dismiss(Helper, Keeper);
            sweep(Helper);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp548_breedingCap")
    public static void breedingCap(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Helper.assertTrue(Scp548Swarm.allowedFor(2, Scp548Swarm.AreaCap - 1, 0) == 1, "The area cap does not limit young");
        Helper.assertTrue(Scp548Swarm.allowedFor(2, 0, Scp548Swarm.LevelCap) == 0, "The world cap does not limit young");
        for (int Index = 0; Index < Scp548Swarm.AreaCap; Index++) {
            spider(Helper, Vec3.atBottomCenterOf(Middle).add((Index % 4) * 0.6 - 0.9, 0.0, (Index / 4) * 0.6 - 0.6), true);
        }
        List<Scp548Spider> Crowd = spiders(Helper);
        Scp538TestPlayer Keeper = player(Helper, Vec3.atBottomCenterOf(Middle.south(3)), "scp548_crowd", GameType.SURVIVAL);
        try {
            Helper.assertTrue(Crowd.size() == Scp548Swarm.AreaCap, "The crowd has " + Crowd.size() + " spiders");
            Helper.assertTrue(Scp548Swarm.crowded(World, Crowd.get(0).position()), "A full area does not count as crowded");
            Keeper.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SNOWBALL, 2));
            Crowd.get(0).mobInteract(Keeper, InteractionHand.MAIN_HAND);
            Helper.assertFalse(Crowd.get(0).isInLove(), "A spider in a full area still took a snowball");
            Helper.assertTrue(Keeper.getMainHandItem().getCount() == 2, "A snowball was used up in a full area");
            Crowd.get(0).spawnChildFromBreeding(World, Crowd.get(1));
            Helper.assertTrue(spiders(Helper).size() == Scp548Swarm.AreaCap, "Breeding went past the area cap");
        } finally {
            dismiss(Helper, Keeper);
            sweep(Helper);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp548_calmNearCooler")
    public static void calmNearCooler(GameTestHelper Helper) {
        AtomicBoolean Fed = new AtomicBoolean(true);
        cooler(Helper, Fed);
        Scp548Spider Spider = spider(Helper, Vec3.atBottomCenterOf(Middle), false);
        Scp538TestPlayer Visitor = player(Helper, Vec3.atBottomCenterOf(Middle.east(2)), "scp548_visitor", GameType.SURVIVAL);
        Helper.startSequence().thenIdle(20).thenExecute(() -> {
            Helper.assertTrue(coolerOn(Helper), "The fed cooler did not switch on");
            Spider.setChill(0);
            Spider.scan(Helper.getLevel());
        }).thenIdle(60).thenExecute(() -> {
            try {
                Helper.assertTrue(Spider.isContained(), "The spider is not calm beside a powered cooler " + describe(Spider));
                Helper.assertTrue(Spider.getTarget() == null, "A contained spider picked a target " + describe(Spider));
                Helper.assertTrue(Spider.chill() == Scp548Spider.ThawDelay, "The cooler did not keep the spider chilled " + describe(Spider));
                Spider.horizontalCollision = true;
                Helper.assertFalse(Spider.onClimbable(), "A contained spider climbs walls");
            } finally {
                dismiss(Helper, Visitor);
                sweep(Helper);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 340, batch = "scp548_powerLossThaws")
    public static void powerLossThaws(GameTestHelper Helper) {
        AtomicBoolean Fed = new AtomicBoolean(true);
        cooler(Helper, Fed);
        Scp548Spider Spider = spider(Helper, Vec3.atBottomCenterOf(Middle), false);
        Scp538TestPlayer Visitor = player(Helper, Vec3.atBottomCenterOf(Middle.east(3)), "scp548_thaw", GameType.SURVIVAL);
        Helper.startSequence().thenIdle(30).thenExecute(() -> {
            Helper.assertTrue(Spider.isContained(), "The spider is not contained while the cooler runs " + describe(Spider));
            Fed.set(false);
        }).thenIdle(30).thenExecute(() -> {
            Helper.assertFalse(coolerOn(Helper), "The cooler kept running without power");
            Helper.assertFalse(Spider.isHostile(), "The spider turned hostile with no delay " + describe(Spider));
            Helper.assertTrue(Spider.mood() == Scp548Spider.Thawing, "The spider is not thawing after the power cut " + describe(Spider));
        }).thenIdle(Scp548Spider.ThawDelay + 2 * Scp548Spider.ScanInterval).thenExecute(() -> {
            try {
                Helper.assertTrue(Spider.isHostile(), "The spider never turned hostile without power " + describe(Spider));
                Spider.horizontalCollision = true;
                Helper.assertTrue(Spider.onClimbable(), "A hostile spider does not climb");
            } finally {
                dismiss(Helper, Visitor);
                sweep(Helper);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp548_hostileHunts")
    public static void hostileHunts(GameTestHelper Helper) {
        Scp548Spider Spider = spider(Helper, Vec3.atBottomCenterOf(Middle), false);
        Scp538TestPlayer Prey = player(Helper, Vec3.atBottomCenterOf(Middle.east(3)), "scp548_prey", GameType.SURVIVAL);
        Scp538TestPlayer Watcher = player(Helper, Vec3.atBottomCenterOf(Middle.west(3)), "scp548_watcher", GameType.CREATIVE);
        Helper.startSequence().thenIdle(2).thenExecute(() -> {
            Spider.setChill(0);
            Spider.scan(Helper.getLevel());
            Helper.assertTrue(Spider.isHostile(), "A spider with no cooler and no chill left is not hostile " + describe(Spider));
        }).thenIdle(60).thenExecute(() -> {
            try {
                Helper.assertTrue(Scp548Swarm.nearestPrey(Helper.getLevel(), Spider, Scp548Spider.FollowRange) == Prey, "The survival player is not found as prey: "
                        + Scp548Swarm.nearestPrey(Helper.getLevel(), Spider, Scp548Spider.FollowRange) + " sight " + Spider.hasLineOfSight(Prey) + " players " + Helper.getLevel().players().size());
                Helper.assertTrue(Spider.getTarget() == Prey, "The hostile spider did not go after the survival player " + describe(Spider));
                Helper.assertFalse(Scp548Swarm.isPrey(Watcher), "A creative player counts as prey");
            } finally {
                dismiss(Helper, Prey);
                dismiss(Helper, Watcher);
                sweep(Helper);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp548_coolerPower")
    public static void coolerPower(GameTestHelper Helper) {
        AtomicBoolean Fed = new AtomicBoolean(false);
        cooler(Helper, Fed);
        Helper.startSequence().thenIdle(15).thenExecute(() -> {
            Helper.assertFalse(coolerOn(Helper), "An unpowered cooler switched on");
            Helper.assertFalse(Scp548Swarm.cooled(Helper.getLevel(), Helper.absoluteVec(Vec3.atBottomCenterOf(Middle))), "An unpowered cooler chills the area");
            Fed.set(true);
        }).thenIdle(10).thenExecute(() -> {
            Helper.assertTrue(coolerOn(Helper), "A fed cooler did not switch on");
            Helper.assertTrue(Scp548Swarm.cooled(Helper.getLevel(), Helper.absoluteVec(Vec3.atBottomCenterOf(Middle))), "A powered cooler does not chill the area");
            Helper.assertFalse(Scp548Swarm.cooled(Helper.getLevel(), Helper.absoluteVec(Vec3.atBottomCenterOf(CoolerPos)).add(CoolerBlockEntity.Radius + 2.0, 0.0, 0.0)),
                    "The cooler chills beyond its radius");
            Fed.set(false);
        }).thenIdle(CoolerBlockEntity.PowerGrace + 5).thenExecute(() -> {
            Helper.assertFalse(coolerOn(Helper), "The cooler kept running after its power was cut");
            Helper.assertFalse(Scp548Swarm.cooled(Helper.getLevel(), Helper.absoluteVec(Vec3.atBottomCenterOf(Middle))), "A cut-off cooler still chills the area");
            ResourceLocation Id = CoolerRegistry.location(CoolerRegistry.CoolerId);
            Optional<RecipeHolder<?>> Found = Helper.getLevel().getRecipeManager().byKey(Id);
            Helper.assertTrue(Found.isPresent(), "There is no recipe for the cooler");
            Helper.assertTrue(Found.get().value().getResultItem(Helper.getLevel().registryAccess()).is(CoolerRegistry.CoolerItem.get()), "The cooler recipe makes something else");
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp548_transferPodRelease")
    public static void transferPodRelease(GameTestHelper Helper) {
        TransferPodContent Held = TransferPods.content(Scp548Registry.PodContent).orElse(null);
        Helper.assertTrue(Held != null && Held.LabelKey().equals(Scp548Registry.PodLabel) && !Held.Listed(), "SCP-548 is a listed Transfer Pod content");
        Scp538TestPlayer User = player(Helper, Vec3.atBottomCenterOf(Middle.west(3)), "scp548_opener", GameType.SURVIVAL);
        try {
            User.setItemInHand(InteractionHand.MAIN_HAND, TransferPods.loaded(Scp548Registry.PodContent));
            BlockPos Floor = Helper.absolutePos(Middle.below());
            BlockHitResult Hit = new BlockHitResult(Vec3.atCenterOf(Floor).add(0.0, 0.5, 0.0), Direction.UP, Floor, false);
            InteractionResult Opened = User.getItemInHand(InteractionHand.MAIN_HAND).useOn(new UseOnContext(User, InteractionHand.MAIN_HAND, Hit));
            Helper.assertTrue(Opened.consumesAction(), "Opening the Transfer Pod failed: " + Opened);
            List<Scp548Spider> Freed = spiders(Helper);
            Helper.assertTrue(Freed.size() == Scp548Swarm.PodCount, "The pod released " + Freed.size() + " spiders");
            Helper.assertTrue(User.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "The opened pod was not used up");
            for (Scp548Spider Spider : Freed) {
                Helper.assertTrue(Spider.chill() >= Scp548Swarm.PodChill - Scp548Spider.ScanInterval && !Spider.isHostile(), "A released spider is not still chilled " + describe(Spider));
            }
        } finally {
            dismiss(Helper, User);
            sweep(Helper);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp548_strangerBringsPod")
    public static void strangerBringsPod(GameTestHelper Helper) {
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp548Registry.DocumentId);
        Helper.assertTrue(Document != null, "The SCP-548 dossier did not load");
        Helper.assertTrue(Document.Classification() == DocumentClass.Euclid, "SCP-548 is not filed as Euclid");
        Helper.assertTrue(Document.Condition() instanceof StrangerEncounterCondition, "SCP-548 does not arrive with the stranger");
        StrangerPlan Plan = ((StrangerEncounterCondition) Document.Condition()).plan();
        Helper.assertTrue(Plan.Gifts().size() == 1, "The stranger brings " + Plan.Gifts().size() + " gifts");
        StrangerGift Gift = Plan.Gifts().get(0);
        ItemStack Stack = Gift.toStack();
        Helper.assertTrue(TransferPods.isPod(Stack) && Scp548Registry.PodContent.equals(TransferPods.contentId(Stack)), "The stranger's gift is not a Transfer Pod holding SCP-548: " + Stack);
        Helper.assertTrue(AnomalySpawners.spawnerFor(Scp548Registry.DocumentId).isPresent(), "No spawner is registered for SCP-548");
        Helper.assertTrue(spiders(Helper).isEmpty(), "Spiders exist before the pod is opened");
        Helper.succeed();
    }
}
