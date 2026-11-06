package net.rai.scp_unleashed.scp409.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.EffectCures;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.scp409.Scp409CaseBlock;
import net.rai.scp_unleashed.scp409.Scp409CaseItem;
import net.rai.scp_unleashed.scp409.Scp409Events;
import net.rai.scp_unleashed.scp409.Scp409Exposure;
import net.rai.scp_unleashed.scp409.Scp409Infection;
import net.rai.scp_unleashed.scp409.Scp409Registry;
import net.rai.scp_unleashed.scp409.Scp409Shatter;
import net.rai.scp_unleashed.stranger.StrangerEncounterCondition;
import net.rai.scp_unleashed.stranger.StrangerGift;
import net.rai.scp_unleashed.stranger.StrangerPlan;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp409GameTests {
    public static final String Arena = "scp409_arena";
    public static final BlockPos CaseSpot = new BlockPos(8, 2, 8);
    public static final List<String> Hazmat = List.of("hazmat_hood", "hazmat_suit", "hazmat_trousers", "hazmat_boots");
    public static final List<EquipmentSlot> HazmatSlots = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private Scp409GameTests() {
    }

    private static Scp409TestPlayer player(GameTestHelper Helper, double X, double Z, String Name) {
        ServerLevel World = Helper.getLevel();
        Scp409TestPlayer User = new Scp409TestPlayer(World, Name);
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, 2.0, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(GameType.SURVIVAL);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, ServerPlayer User) {
        if (!User.isRemoved()) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    private static void suit(LivingEntity Wearer) {
        for (int Index = 0; Index < Hazmat.size(); Index++) {
            Wearer.setItemSlot(HazmatSlots.get(Index), new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Hazmat.get(Index)))));
        }
    }

    private static BlockState placeCase(GameTestHelper Helper, boolean Open) {
        BlockState State = Scp409Registry.Case.get().defaultBlockState().setValue(Scp409CaseBlock.Facing, Direction.NORTH).setValue(Scp409CaseBlock.Open, Open);
        Helper.setBlock(CaseSpot, State);
        return State;
    }

    private static Vec3 crystalPoint(GameTestHelper Helper) {
        return Helper.absoluteVec(new Vec3(CaseSpot.getX() + 0.5, CaseSpot.getY() + 6.0 / 16.0, CaseSpot.getZ() + 0.5));
    }

    private static void clickCrystal(GameTestHelper Helper, ServerPlayer User) {
        BlockPos Absolute = Helper.absolutePos(CaseSpot);
        BlockHitResult Hit = new BlockHitResult(crystalPoint(Helper), Direction.UP, Absolute, false);
        User.gameMode.useItemOn(User, User.serverLevel(), User.getMainHandItem(), InteractionHand.MAIN_HAND, Hit);
    }

    private static String deathKey(DamageSource Source, LivingEntity Victim) {
        Component Message = Source.getLocalizedDeathMessage(Victim);
        return Message.getContents() instanceof TranslatableContents Translated ? Translated.getKey() : Message.getString();
    }

    private static List<BlockState> snapshot(GameTestHelper Helper) {
        List<BlockState> States = new ArrayList<>();
        for (int X = 0; X < 16; X++) {
            for (int Y = 1; Y < 7; Y++) {
                for (int Z = 0; Z < 16; Z++) {
                    States.add(Helper.getLevel().getBlockState(Helper.absolutePos(new BlockPos(X, Y, Z))));
                }
            }
        }
        return States;
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp409_closedCaseNeverInfects")
    public static void closedCaseNeverInfects(GameTestHelper Helper) {
        placeCase(Helper, false);
        BlockPos Absolute = Helper.absolutePos(CaseSpot);
        ServerLevel World = Helper.getLevel();
        Scp409TestPlayer Close = player(Helper, 8.5, 9.4, "scp409_closed_a");
        Scp409TestPlayer Hitter = player(Helper, 8.5, 6.6, "scp409_closed_b");
        Pig Inside = Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(8.5, 2.7, 8.5));
        Helper.assertFalse(Scp409CaseBlock.isOpen(World.getBlockState(Absolute)), "A freshly placed case is open");
        Helper.startSequence().thenExecuteFor(60, () -> {
            BlockState Now = World.getBlockState(Absolute);
            Now.entityInside(World, Absolute, Inside);
            Now.getBlock().stepOn(World, Absolute, Now, Close);
            Now.attack(World, Absolute, Hitter);
        }).thenExecute(() -> {
            Helper.assertFalse(Scp409Infection.isInfected(Close), "A player pressed against the closed case caught SCP-409");
            Helper.assertFalse(Scp409Infection.isInfected(Hitter), "A player striking the closed case caught SCP-409");
            Helper.assertFalse(Scp409Infection.isInfected(Inside), "A pig inside the closed case caught SCP-409");
            List<ItemStack> Drops = Block.getDrops(World.getBlockState(Absolute), World, Absolute, World.getBlockEntity(Absolute));
            Helper.assertTrue(Drops.size() == 1 && Scp409CaseItem.is(Drops.get(0)), "The case does not drop itself when mined: " + Drops);
            dismiss(Helper, Close);
            dismiss(Helper, Hitter);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp409_openCaseInfectsToucher")
    public static void openCaseInfectsToucher(GameTestHelper Helper) {
        placeCase(Helper, true);
        Scp409TestPlayer Toucher = player(Helper, 8.5, 9.45, "scp409_toucher");
        Scp409TestPlayer Reacher = player(Helper, 8.5, 13.5, "scp409_reacher");
        Scp409TestPlayer Distant = player(Helper, 2.5, 2.5, "scp409_distant");
        clickCrystal(Helper, Reacher);
        Helper.assertTrue(Scp409Infection.isInfected(Reacher), "Reaching into the open case did not infect the player");
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Scp409Infection.isInfected(Toucher), "A player pressed against the open case was not infected");
            Helper.assertFalse(Scp409Infection.isInfected(Distant), "A player across the room caught SCP-409");
            Helper.assertTrue(Scp409Infection.stageOf(Toucher) == 1, "A fresh infection is not at stage 1");
            dismiss(Helper, Toucher);
            dismiss(Helper, Reacher);
            dismiss(Helper, Distant);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp409_hazmatProtects")
    public static void hazmatProtects(GameTestHelper Helper) {
        placeCase(Helper, true);
        BlockPos Absolute = Helper.absolutePos(CaseSpot);
        ServerLevel World = Helper.getLevel();
        Scp409TestPlayer Suited = player(Helper, 8.5, 9.45, "scp409_suited");
        suit(Suited);
        Helper.assertTrue(Scp409Exposure.isProtected(Suited), "The full hazmat suit is not recognised");
        Pig Carrier = Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(3.5, 2.0, 3.5));
        Helper.startSequence().thenExecuteFor(60, () -> {
            clickCrystal(Helper, Suited);
            World.getBlockState(Absolute).attack(World, Absolute, Suited);
            if (!Scp409Infection.isInfected(Carrier)) {
                Scp409Infection.infect(Carrier);
            }
            Carrier.moveTo(Suited.getX(), Suited.getY(), Suited.getZ());
        }).thenExecute(() -> {
            Helper.assertFalse(Scp409Infection.isInfected(Suited), "A player in a full hazmat suit caught SCP-409");
            Helper.assertTrue(Scp409Infection.isInfected(Carrier), "The carrier pig lost its infection");
            dismiss(Helper, Suited);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp409_playerSpreadsToPlayer")
    public static void playerSpreadsToPlayer(GameTestHelper Helper) {
        Scp409TestPlayer Sick = player(Helper, 6.5, 6.5, "scp409_sick");
        Scp409TestPlayer Healthy = player(Helper, 6.8, 6.5, "scp409_healthy");
        Scp409TestPlayer Apart = player(Helper, 12.5, 12.5, "scp409_apart");
        Helper.assertTrue(Scp409Infection.infect(Sick), "The first player could not be infected");
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Scp409Infection.isInfected(Healthy), "Touching an infected player did not pass SCP-409 on");
            Helper.assertFalse(Scp409Infection.isInfected(Apart), "A player standing apart caught SCP-409");
            dismiss(Helper, Sick);
            dismiss(Helper, Healthy);
            dismiss(Helper, Apart);
        });
    }

    private static void lean(LivingEntity Mover, LivingEntity Anchor) {
        Mover.moveTo(Anchor.getX() + 0.55, Anchor.getY(), Anchor.getZ());
        Mover.setDeltaMovement(Vec3.ZERO);
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp409_cowSpreadsToPig")
    public static void cowSpreadsToPig(GameTestHelper Helper) {
        Helper.getLevel().getGameRules().getRule(Scp409Registry.MobSpread).set(true, Helper.getLevel().getServer());
        Cow Source = Helper.spawnWithNoFreeWill(EntityType.COW, new Vec3(6.5, 2.0, 6.5));
        Pig Target = Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(7.0, 2.0, 6.5));
        Pig Apart = Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(12.5, 2.0, 12.5));
        Helper.assertTrue(Scp409Infection.infect(Source), "The cow could not be infected");
        Helper.onEachTick(() -> lean(Target, Source));
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Scp409Infection.isInfected(Target), "The pig touching an infected cow did not catch SCP-409");
            Helper.assertFalse(Scp409Infection.isInfected(Apart), "A pig across the room caught SCP-409");
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp409_mobSpreadRuleOff")
    public static void mobSpreadRuleOff(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        World.getGameRules().getRule(Scp409Registry.MobSpread).set(false, World.getServer());
        Cow Source = Helper.spawnWithNoFreeWill(EntityType.COW, new Vec3(6.5, 2.0, 6.5));
        Pig Target = Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(7.0, 2.0, 6.5));
        Helper.assertTrue(Scp409Infection.infect(Source), "The cow could not be infected");
        Helper.startSequence().thenExecuteFor(40, () -> lean(Target, Source)).thenExecute(() -> {
            boolean Caught = Scp409Infection.isInfected(Target);
            World.getGameRules().getRule(Scp409Registry.MobSpread).set(true, World.getServer());
            Helper.assertFalse(Caught, "The pig caught SCP-409 from the cow with scp409MobSpread off");
            Scp409TestPlayer Farmer = player(Helper, 6.8, 6.5, "scp409_farmer");
            Farmer.moveTo(Source.getX(), Source.getY(), Source.getZ());
            Helper.runAfterDelay(20, () -> {
                boolean Person = Scp409Infection.isInfected(Farmer);
                dismiss(Helper, Farmer);
                Helper.assertTrue(Person, "A person touching the cow did not catch SCP-409 with scp409MobSpread off");
                Helper.succeed();
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp409_stagesAdvanceAndSlow")
    public static void stagesAdvanceAndSlow(GameTestHelper Helper) {
        Scp409TestPlayer User = player(Helper, 4.5, 4.5, "scp409_stages");
        double Base = User.getAttributeValue(Attributes.MOVEMENT_SPEED);
        double BaseAttack = User.getAttributeValue(Attributes.ATTACK_SPEED);
        double BaseMining = User.getAttributeValue(Attributes.BLOCK_BREAK_SPEED);
        Helper.assertTrue(Scp409Infection.infect(User), "The player could not be infected");
        double Previous = Base;
        for (int Stage = 1; Stage <= Scp409Infection.Stages; Stage++) {
            Scp409Infection.setStage(User, Stage);
            Helper.assertTrue(Scp409Infection.stageOf(User) == Stage, "Expected stage " + Stage + " but got " + Scp409Infection.stageOf(User));
            double Speed = User.getAttributeValue(Attributes.MOVEMENT_SPEED);
            Helper.assertTrue(Speed < Previous - 1.0e-6, "Movement did not slow further at stage " + Stage + ": " + Speed + " after " + Previous);
            Previous = Speed;
        }
        Helper.assertTrue(User.getAttributeValue(Attributes.ATTACK_SPEED) < BaseAttack - 1.0e-6, "Attack speed was not slowed at the final stage");
        Helper.assertTrue(User.getAttributeValue(Attributes.BLOCK_BREAK_SPEED) < BaseMining - 1.0e-6, "Mining speed was not slowed at the final stage");
        Scp409Infection.setElapsed(User, Scp409Infection.stageStart(User, 2) - 3);
        Helper.assertTrue(Scp409Infection.stageOf(User) == 1, "The infection was not wound back to stage 1");
        Helper.startSequence().thenIdle(8).thenExecute(() -> {
            Helper.assertTrue(Scp409Infection.stageOf(User) == 2, "The infection did not advance to stage 2 on its own");
            Helper.assertTrue(User.getAttributeValue(Attributes.MOVEMENT_SPEED) < Base, "Movement is not slowed at stage 2");
            dismiss(Helper, User);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp409_finalStageKillsAndShatters")
    public static void finalStageKillsAndShatters(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Pig Victim = Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(5.5, 2.0, 5.5));
        Helper.assertTrue(Scp409Infection.infect(Victim), "The pig could not be infected");
        Victim.setHealth(0.6f);
        Scp409Infection.setElapsed(Victim, Scp409Infection.finalStart(Victim) - 1);
        List<BlockState> Before = snapshot(Helper);
        int Shattered = Scp409Shatter.count();
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Victim.isDeadOrDying(), "The final stage has not killed the pig yet");
            DamageSource Last = Victim.getLastDamageSource();
            Helper.assertTrue(Last != null && Last.is(Scp409Registry.OvergrownDamage), "The pig died to the wrong damage: " + Last);
            Helper.assertTrue("death.attack.scp_unleashed.scp409_overgrown".equals(deathKey(Last, Victim)), "Wrong death message: " + deathKey(Last, Victim));
            Helper.assertTrue(Scp409Shatter.count() == Shattered + 1, "The pig did not shatter exactly once");
            Helper.assertFalse(Scp409Infection.isInfected(Victim), "The shattered body still carries SCP-409");
            Helper.assertTrue(Before.equals(snapshot(Helper)), "The shatter changed blocks in the arena");
            boolean Loot = !World.getEntitiesOfClass(ItemEntity.class, Victim.getBoundingBox().inflate(3.0), Drop -> Drop.getItem().is(Items.PORKCHOP)).isEmpty();
            Helper.assertTrue(Loot, "The pig dropped none of its normal loot");
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp409_anomalyCureClears")
    public static void anomalyCureClears(GameTestHelper Helper) {
        Zombie Patient = Helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(5.5, 2.0, 5.5));
        double Base = Patient.getAttributeValue(Attributes.MOVEMENT_SPEED);
        Helper.assertTrue(Scp409Infection.setStage(Patient, 3), "The zombie could not be set to stage 3");
        Patient.removeEffectsCuredBy(EffectCures.MILK);
        Helper.assertTrue(Scp409Infection.isInfected(Patient), "Milk cured SCP-409");
        Helper.assertTrue(AnomalyCure.cure(Patient, null), "AnomalyCure reported nothing cured");
        Helper.assertFalse(Scp409Infection.isInfected(Patient), "AnomalyCure left the infection in place");
        Helper.assertFalse(Scp409Infection.hasModifiers(Patient), "AnomalyCure left the slowdown in place");
        Helper.assertTrue(Math.abs(Patient.getAttributeValue(Attributes.MOVEMENT_SPEED) - Base) < 1.0e-6, "Movement speed did not recover");
        Helper.assertFalse(AnomalyCure.cure(Patient, null), "Curing a healthy zombie reported a cure");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp409_survivesSaveAndReload")
    public static void survivesSaveAndReload(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Pig Saved = Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(5.5, 2.0, 5.5));
        Scp409Infection.infect(Saved);
        Scp409Infection.setElapsed(Saved, 9123);
        CompoundTag Tag = new CompoundTag();
        Saved.saveWithoutId(Tag);
        Pig Loaded = EntityType.PIG.create(World);
        Helper.assertTrue(Loaded != null, "Could not make a fresh pig");
        Loaded.load(Tag);
        Helper.assertTrue(Scp409Infection.isInfected(Loaded), "The reloaded pig lost its infection");
        Helper.assertTrue(Scp409Infection.elapsed(Loaded) == 9123, "The reloaded pig lost its progress: " + Scp409Infection.elapsed(Loaded));
        Scp409TestPlayer User = player(Helper, 9.5, 9.5, "scp409_saved");
        Scp409Infection.setStage(User, 4);
        int Elapsed = Scp409Infection.elapsed(User);
        CompoundTag PlayerTag = new CompoundTag();
        User.saveWithoutId(PlayerTag);
        dismiss(Helper, User);
        Scp409TestPlayer Returned = new Scp409TestPlayer(World, "scp409_returned");
        Returned.load(PlayerTag);
        Helper.assertTrue(Scp409Infection.isInfected(Returned) && Scp409Infection.elapsed(Returned) == Elapsed, "The player's infection did not survive a save and reload");
        Helper.assertTrue(Scp409Infection.stageOf(Returned) == 4, "The reloaded player is not at stage 4");
        Helper.succeed();
    }

    public static final class DeathWatch {
        private final UUID Controller;
        private final UUID Body;
        private int ControllerDeaths;
        private int ControllerCancelled;
        private int BodyDeaths;

        public DeathWatch(UUID Controller, UUID Body) {
            this.Controller = Controller;
            this.Body = Body;
        }

        @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
        public void died(LivingDeathEvent Event) {
            UUID Dead = Event.getEntity().getUUID();
            if (Dead.equals(Controller) && Event.getEntity() instanceof ServerPlayer) {
                ControllerDeaths++;
                if (Event.isCanceled()) {
                    ControllerCancelled++;
                }
            } else if (Dead.equals(Body) && !Event.isCanceled()) {
                BodyDeaths++;
            }
        }
    }

    @GameTest(template = Arena, timeoutTicks = 240, batch = "scp409_controlledDeathCountsOnce")
    public static void controlledDeathCountsOnce(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp409TestPlayer User = player(Helper, 8.5, 6.5, "scp409_controller");
        User.enlist(true);
        DClass Body = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new BlockPos(8, 2, 8));
        UUID BodyId = Body.getUUID();
        Helper.assertTrue(ControlManager.possess(User, Body), "The test player could not take control of the D-Class");
        Helper.assertTrue(ControlSessions.isControlling(User), "No control session after possessing");
        DeathWatch Watch = new DeathWatch(User.getUUID(), BodyId);
        NeoForge.EVENT_BUS.register(Watch);
        int Shattered = Scp409Shatter.count();
        Helper.assertTrue(Scp409Infection.infect(User), "The controlled body could not be infected");
        User.setHealth(0.4f);
        Scp409Infection.setElapsed(User, Scp409Infection.finalStart(User) - 1);
        Helper.startSequence().thenWaitUntil(() -> {
            Helper.assertFalse(ControlSessions.isControlling(User), "The controlled body has not died yet");
            Helper.assertFalse(ControlManager.hasPendingDeath(User), "A deferred death is still pending");
        }).thenIdle(Scp409Events.CleanupTicks + 5).thenExecute(() -> {
            NeoForge.EVENT_BUS.unregister(Watch);
            User.enlist(false);
            Helper.assertTrue(Watch.ControllerDeaths == 1 && Watch.ControllerCancelled == 1, "The controller saw " + Watch.ControllerDeaths + " deaths, " + Watch.ControllerCancelled + " deferred");
            Helper.assertTrue(Watch.BodyDeaths == 1, "The D-Class body died " + Watch.BodyDeaths + " times");
            Helper.assertTrue(Scp409Shatter.count() == Shattered + 1, "The body shattered " + (Scp409Shatter.count() - Shattered) + " times");
            Helper.assertTrue(User.isAlive() && User.getHealth() > 1.0f, "The controller did not return home alive");
            Helper.assertFalse(Scp409Infection.isInfected(User), "SCP-409 followed the controller home");
            Helper.assertFalse(Scp409Infection.hasModifiers(User), "The crystal slowdown followed the controller home");
            Entity Left = World.getEntity(BodyId);
            Helper.assertTrue(Left == null || !Left.isAlive(), "The infected D-Class body is still alive");
            dismiss(Helper, User);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp409_strangerBringsClosedCase")
    public static void strangerBringsClosedCase(GameTestHelper Helper) {
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp409Registry.DocumentId);
        Helper.assertTrue(Document != null, "The SCP-409 dossier did not load");
        Helper.assertTrue(Document.Classification() == DocumentClass.Keter, "SCP-409 is not filed as Keter");
        Helper.assertTrue(Document.Condition() instanceof StrangerEncounterCondition, "SCP-409 does not arrive with the stranger");
        StrangerPlan Plan = ((StrangerEncounterCondition) Document.Condition()).plan();
        Helper.assertTrue("foundation".equals(Plan.Variant()), "SCP-409 arrives with the wrong stranger: " + Plan.Variant());
        Helper.assertTrue(Plan.Gifts().size() == 1, "The stranger brings " + Plan.Gifts().size() + " gifts");
        StrangerGift Gift = Plan.Gifts().get(0);
        ItemStack Stack = Gift.toStack();
        Helper.assertTrue(Scp409CaseItem.is(Stack) && Stack.getCount() == 1, "The stranger's gift is not the SCP-409 case: " + Stack);
        BlockItemStateProperties StateProps = Stack.get(DataComponents.BLOCK_STATE);
        Helper.assertTrue(StateProps == null || !Boolean.TRUE.equals(StateProps.get(Scp409CaseBlock.Open)), "The gifted case would be placed open");
        BlockState Placed = Scp409Registry.Case.get().defaultBlockState();
        Helper.assertFalse(Scp409CaseBlock.isOpen(Placed), "The case does not default to closed");
        Helper.succeed();
    }
}
