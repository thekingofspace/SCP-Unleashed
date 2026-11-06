package net.rai.scp_unleashed.scp427.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.control.BodyAttachments;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.scp427.Scp427Events;
import net.rai.scp_unleashed.scp427.Scp427Exposure;
import net.rai.scp_unleashed.scp427.Scp427Flesh;
import net.rai.scp_unleashed.scp427.Scp427Light;
import net.rai.scp_unleashed.scp427.Scp427LocketItem;
import net.rai.scp_unleashed.scp427.Scp427Registry;
import net.rai.scp_unleashed.scp427.Scp427State;
import net.rai.scp_unleashed.scp427.Scp427Transformation;
import net.rai.scp_unleashed.stasis.StasisPods;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import net.rai.scp_unleashed.storage.StorageRegistry;
import net.rai.scp_unleashed.stranger.StrangerEncounterCondition;
import net.rai.scp_unleashed.stranger.StrangerPlan;

import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp427GameTests {
    public static final String Arena = "scp427_arena";
    public static final TagKey<EntityType<?>> StasisImmune = TagKey.create(Registries.ENTITY_TYPE, Scp427Registry.location("stasis_immune"));

    private Scp427GameTests() {
    }

    private static Scp427TestPlayer player(GameTestHelper Helper, double X, double Z, String Name) {
        ServerLevel World = Helper.getLevel();
        Scp427TestPlayer User = new Scp427TestPlayer(World, Name);
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, 2.0, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(GameType.SURVIVAL);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, ServerPlayer User) {
        Scp427Light.release(User);
        if (!User.isRemoved()) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    private static ItemStack locket() {
        return new ItemStack(Scp427Registry.Locket.get());
    }

    private static void open(ServerPlayer User) {
        User.getInventory().selected = 0;
        User.getInventory().setItem(0, locket());
        InteractionResultHolder<ItemStack> Result = User.getMainHandItem().use(User.level(), User, InteractionHand.MAIN_HAND);
        if (!User.isUsingItem()) {
            throw new IllegalStateException("The locket did not open: " + Result.getResult());
        }
    }

    private static int monsters(GameTestHelper Helper) {
        AABB Area = new AABB(Helper.absolutePos(BlockPos.ZERO)).inflate(24.0);
        return Helper.getLevel().getEntitiesOfClass(Scp427Flesh.class, Area, Entity::isAlive).size();
    }

    private static int droppedLockets(GameTestHelper Helper) {
        AABB Area = new AABB(Helper.absolutePos(BlockPos.ZERO)).inflate(24.0);
        return Helper.getLevel().getEntitiesOfClass(ItemEntity.class, Area, Dropped -> Scp427LocketItem.is(Dropped.getItem())).size();
    }

    private static void clearMonsters(GameTestHelper Helper) {
        AABB Area = new AABB(Helper.absolutePos(BlockPos.ZERO)).inflate(24.0);
        for (Scp427Flesh Monster : Helper.getLevel().getEntitiesOfClass(Scp427Flesh.class, Area)) {
            Monster.discard();
        }
    }

    private static boolean litAt(ServerLevel World, BlockPos Pos) {
        return Scp427Light.isOurs(World.getBlockState(Pos));
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp427_openHealsAndCounts")
    public static void openHealsAndCounts(GameTestHelper Helper) {
        Scp427TestPlayer User = player(Helper, 8.5, 8.5, "scp427_healer");
        User.setHealth(8.0f);
        User.addEffect(new MobEffectInstance(MobEffects.POISON, 400, 0));
        open(User);
        Helper.runAfterDelay(42, () -> {
            Helper.assertTrue(User.getHealth() >= 9.5f, "Two seconds open healed only to " + User.getHealth());
            int Ticks = Scp427Exposure.exposure(User);
            Helper.assertTrue(Ticks >= 38 && Ticks <= 44, "Exposure after two seconds open is " + Ticks + " ticks");
            MobEffectInstance Poison = User.getEffect(MobEffects.POISON);
            Helper.assertTrue(Poison == null || Poison.getDuration() <= 400 - 2 * Scp427Exposure.CleanseTicks, "Poison was not shortened: " + (Poison == null ? 0 : Poison.getDuration()));
            Scp427Light.Lit Light = Scp427Light.litFor(User);
            Helper.assertTrue(Light != null && litAt(Helper.getLevel(), Light.Pos()), "No light block follows the open locket");
            Helper.assertTrue(Light.Pos().equals(BlockPos.containing(User.getEyePosition())), "The light is not at the holder's eyes");
            User.stopUsingItem();
        });
        Helper.runAfterDelay(44, () -> {
            Helper.assertTrue(Scp427Light.litFor(User) == null, "The light record stayed after closing");
            Helper.assertFalse(litAt(Helper.getLevel(), BlockPos.containing(User.getEyePosition())), "The light block stayed after closing");
            dismiss(Helper, User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp427_lightFollowsAndCleansUp")
    public static void lightFollowsAndCleansUp(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp427TestPlayer User = player(Helper, 4.5, 4.5, "scp427_mover");
        open(User);
        BlockPos[] First = new BlockPos[1];
        Helper.runAfterDelay(3, () -> {
            Scp427Light.Lit Light = Scp427Light.litFor(User);
            Helper.assertTrue(Light != null && litAt(World, Light.Pos()), "No light after opening");
            First[0] = Light.Pos();
            Vec3 Moved = User.position().add(4.0, 0.0, 3.0);
            User.teleportTo(Moved.x, Moved.y, Moved.z);
        });
        Helper.runAfterDelay(6, () -> {
            Scp427Light.Lit Light = Scp427Light.litFor(User);
            Helper.assertTrue(Light != null && !Light.Pos().equals(First[0]) && litAt(World, Light.Pos()), "The light did not follow the holder");
            Helper.assertFalse(litAt(World, First[0]), "The old light block was left behind");
            User.getInventory().selected = 1;
        });
        Helper.runAfterDelay(9, () -> {
            Helper.assertTrue(Scp427Light.litFor(User) == null, "Changing hotbar slot left the light on");
            Helper.assertFalse(litAt(World, BlockPos.containing(User.getEyePosition())), "Changing hotbar slot left a light block");
            Helper.assertTrue(Scp427Exposure.exposure(User) > 0, "No exposure was recorded");
            User.getInventory().selected = 0;
            User.stopUsingItem();
            open(User);
        });
        Helper.runAfterDelay(12, () -> {
            Helper.assertTrue(Scp427Light.litFor(User) != null, "Reopening did not light again");
            Scp427Events.loggedOut(new PlayerEvent.PlayerLoggedOutEvent(User));
            Helper.assertTrue(Scp427Light.litFor(User) == null, "Logging out left the light record");
            Helper.assertFalse(litAt(World, BlockPos.containing(User.getEyePosition())), "Logging out left a light block");
        });
        Helper.runAfterDelay(15, () -> Helper.assertTrue(Scp427Light.litFor(User) != null, "The holder still holds it open, so the light should return"));
        Helper.runAfterDelay(16, () -> User.hurt(User.damageSources().genericKill(), Float.MAX_VALUE));
        Helper.runAfterDelay(19, () -> {
            Helper.assertTrue(User.isDeadOrDying(), "The test player did not die");
            Helper.assertTrue(Scp427Light.litFor(User) == null, "Death left the light record");
            Helper.assertFalse(litAt(World, BlockPos.containing(User.getEyePosition())), "Death left a light block");
            dismiss(Helper, User);
        });
        Scp427TestPlayer Gone = player(Helper, 12.5, 12.5, "scp427_gone");
        Helper.runAfterDelay(22, () -> open(Gone));
        BlockPos[] Kept = new BlockPos[1];
        Helper.runAfterDelay(25, () -> {
            Scp427Light.Lit Light = Scp427Light.litFor(Gone);
            Helper.assertTrue(Light != null, "Second holder has no light");
            Kept[0] = Light.Pos();
            World.removePlayerImmediately(Gone, Entity.RemovalReason.DISCARDED);
        });
        Helper.runAfterDelay(31, () -> {
            Helper.assertTrue(Scp427Light.litFor(Gone) == null, "A holder that vanished kept its light record");
            Helper.assertFalse(litAt(World, Kept[0]), "A holder that vanished left its light block");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 140, batch = "scp427_exposureDecays")
    public static void exposureDecays(GameTestHelper Helper) {
        Scp427TestPlayer User = player(Helper, 8.5, 8.5, "scp427_rester");
        Scp427Exposure.setExposure(User, 200);
        Helper.runAfterDelay(95, () -> {
            int Ticks = Scp427Exposure.exposure(User);
            Helper.assertTrue(Ticks >= 196 && Ticks <= 198, "After 95 closed ticks exposure is " + Ticks + ", expected about 197");
            Scp427Exposure.setExposure(User, 1);
        });
        Helper.runAfterDelay(130, () -> {
            Helper.assertTrue(Scp427Exposure.state(User) == null, "A fully faded exposure kept its attachment");
            Helper.assertFalse(BodyAttachments.accountBound(Scp427Registry.Exposure.get()), "Exposure would survive death");
            dismiss(Helper, User);
            Helper.succeed();
        });
    }

    public static final class DeathWatch {
        private final UUID Watched;
        private final UUID Body;
        private int Deaths;
        private int Cancelled;
        private int BodyDeaths;

        public DeathWatch(UUID Watched, UUID Body) {
            this.Watched = Watched;
            this.Body = Body;
        }

        @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
        public void died(LivingDeathEvent Event) {
            UUID Dead = Event.getEntity().getUUID();
            if (Dead.equals(Watched) && Event.getEntity() instanceof ServerPlayer) {
                Deaths++;
                if (Event.isCanceled()) {
                    Cancelled++;
                }
            } else if (Body != null && Dead.equals(Body) && !Event.isCanceled()) {
                BodyDeaths++;
            }
        }
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp427_limitTransforms")
    public static void limitTransforms(GameTestHelper Helper) {
        clearMonsters(Helper);
        Scp427TestPlayer User = player(Helper, 8.5, 8.5, "scp427_victim");
        open(User);
        DeathWatch Watch = new DeathWatch(User.getUUID(), null);
        NeoForge.EVENT_BUS.register(Watch);
        int Before = Scp427Transformation.spawnedTotal();
        Helper.runAfterDelay(4, () -> {
            int Result;
            try {
                Result = User.server.getCommands().getDispatcher().execute("scp_unleashed scp427 exposure set 301", User.createCommandSourceStack().withPermission(4));
            } catch (CommandSyntaxException Problem) {
                throw new IllegalStateException("The exposure command did not parse", Problem);
            }
            Helper.assertTrue(Result == 1, "The exposure command did not apply");
        });
        Helper.runAfterDelay(7, () -> {
            Helper.assertTrue(Scp427Exposure.isTransforming(User), "Crossing the limit did not start the change");
            Helper.assertFalse(User.isUsingItem(), "The locket stayed open during the change");
            Helper.assertTrue(Scp427Light.litFor(User) == null, "The light stayed on during the change");
            Helper.assertTrue(User.isAlive(), "The holder died before the change finished");
        });
        Helper.runAfterDelay(7 + Scp427Transformation.Length + 6, () -> {
            NeoForge.EVENT_BUS.unregister(Watch);
            Helper.assertTrue(Watch.Deaths == 1 && Watch.Cancelled == 0, "The holder died " + Watch.Deaths + " times");
            Helper.assertTrue(User.isDeadOrDying(), "The holder survived the change");
            Helper.assertTrue(Scp427Transformation.spawnedTotal() == Before + 1, "SCP-427-1 spawned " + (Scp427Transformation.spawnedTotal() - Before) + " times");
            Helper.assertTrue(monsters(Helper) == 1, "Found " + monsters(Helper) + " SCP-427-1 near the arena");
            Helper.assertTrue(droppedLockets(Helper) == 1, "Found " + droppedLockets(Helper) + " dropped lockets");
            Helper.assertTrue(User.getLastDeathLocation().isPresent(), "No death location was recorded");
            clearMonsters(Helper);
            dismiss(Helper, User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp427_offHandDoesNothing")
    public static void offHandDoesNothing(GameTestHelper Helper) {
        Scp427TestPlayer User = player(Helper, 8.5, 8.5, "scp427_lefty");
        User.setItemSlot(EquipmentSlot.OFFHAND, locket());
        InteractionResultHolder<ItemStack> Result = User.getOffhandItem().use(User.level(), User, InteractionHand.OFF_HAND);
        Helper.assertFalse(User.isUsingItem(), "The off-hand locket opened: " + Result.getResult());
        User.startUsingItem(InteractionHand.OFF_HAND);
        Helper.runAfterDelay(30, () -> {
            Helper.assertTrue(Scp427Exposure.exposure(User) == 0, "Off-hand use built exposure");
            Helper.assertTrue(Scp427Light.litFor(User) == null, "Off-hand use made light");
            User.stopUsingItem();
            dismiss(Helper, User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp427_lockerIsInert")
    public static void lockerIsInert(GameTestHelper Helper) {
        BlockPos Spot = new BlockPos(8, 1, 8);
        Helper.setBlock(Spot, StorageRegistry.SmallContainer.get().defaultBlockState());
        if (!(Helper.getBlockEntity(Spot) instanceof StorageContainerBlockEntity Locker)) {
            throw new IllegalStateException("No storage container block entity");
        }
        Locker.setContents(locket());
        Scp427TestPlayer User = player(Helper, 9.5, 8.5, "scp427_keeper");
        int Lights = Scp427Light.activeCount();
        Helper.runAfterDelay(50, () -> {
            Helper.assertTrue(Scp427LocketItem.is(Locker.peekContents()), "The locket left the locker");
            Helper.assertTrue(Scp427Exposure.exposure(User) == 0, "A locked-away locket built exposure");
            Helper.assertTrue(Scp427Light.activeCount() == Lights, "A locked-away locket made light");
            dismiss(Helper, User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 240, batch = "scp427_controlledDeferredDeath")
    public static void controlledDeferredDeath(GameTestHelper Helper) {
        clearMonsters(Helper);
        ServerLevel World = Helper.getLevel();
        Scp427TestPlayer User = player(Helper, 8.5, 5.5, "scp427_controller");
        User.enlist(true);
        DClass Body = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new BlockPos(8, 2, 8));
        UUID BodyId = Body.getUUID();
        Helper.assertTrue(ControlManager.possess(User, Body), "The test player could not take control of the D-Class");
        DeathWatch Watch = new DeathWatch(User.getUUID(), BodyId);
        NeoForge.EVENT_BUS.register(Watch);
        int Before = Scp427Transformation.spawnedTotal();
        open(User);
        Helper.runAfterDelay(3, () -> {
            Helper.assertTrue(Scp427Exposure.exposure(User) > 0, "The controlled body built no exposure");
            Scp427Exposure.setExposure(User, Scp427Exposure.Limit + 1);
        });
        Helper.startSequence().thenIdle(5).thenWaitUntil(() -> {
            Helper.assertFalse(ControlSessions.isControlling(User), "The controlled body has not died yet");
            Helper.assertFalse(ControlManager.hasPendingDeath(User), "A deferred death is still pending");
        }).thenIdle(4).thenExecute(() -> {
            NeoForge.EVENT_BUS.unregister(Watch);
            User.enlist(false);
            Helper.assertTrue(Watch.Deaths == 1 && Watch.Cancelled == 1, "The controller saw " + Watch.Deaths + " deaths, " + Watch.Cancelled + " deferred");
            Helper.assertTrue(Watch.BodyDeaths == 1, "The D-Class body died " + Watch.BodyDeaths + " times");
            Helper.assertTrue(Scp427Transformation.spawnedTotal() == Before + 1, "SCP-427-1 spawned " + (Scp427Transformation.spawnedTotal() - Before) + " times");
            Helper.assertTrue(monsters(Helper) == 1, "Found " + monsters(Helper) + " SCP-427-1 near the arena");
            Helper.assertTrue(droppedLockets(Helper) == 1, "The body's locket did not drop: " + droppedLockets(Helper));
            Helper.assertTrue(User.isAlive() && User.getHealth() > 1.0f, "The controller did not return home alive");
            Helper.assertTrue(Scp427Exposure.state(User) == null, "SCP-427 exposure followed the controller home");
            Entity Left = World.getEntity(BodyId);
            Helper.assertTrue(Left == null || Left.isRemoved(), "The D-Class corpse was left lying in place of the monster");
            clearMonsters(Helper);
            dismiss(Helper, User);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp427_npcTransforms")
    public static void npcTransforms(GameTestHelper Helper) {
        clearMonsters(Helper);
        DClass Body = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new BlockPos(8, 2, 8));
        Body.setItemSlot(EquipmentSlot.MAINHAND, locket());
        Body.startUsingItem(InteractionHand.MAIN_HAND);
        int Before = Scp427Transformation.spawnedTotal();
        Helper.runAfterDelay(25, () -> {
            Helper.assertTrue(Scp427Exposure.isOpen(Body), "The D-Class did not hold the locket open");
            Helper.assertTrue(Scp427Exposure.exposure(Body) >= 20, "The D-Class built " + Scp427Exposure.exposure(Body) + " ticks of exposure");
            Helper.assertTrue(Scp427Light.litFor(Body) != null, "The D-Class's open locket made no light");
            Scp427Exposure.setExposure(Body, Scp427Exposure.Limit + 1);
        });
        Helper.runAfterDelay(30 + Scp427Transformation.Length + 6, () -> {
            Helper.assertTrue(Body.isRemoved() || Body.isDeadOrDying(), "The D-Class survived the change");
            Helper.assertTrue(Scp427Transformation.spawnedTotal() == Before + 1, "SCP-427-1 spawned " + (Scp427Transformation.spawnedTotal() - Before) + " times");
            Helper.assertTrue(monsters(Helper) == 1, "Found " + monsters(Helper) + " SCP-427-1");
            Helper.assertTrue(droppedLockets(Helper) == 1, "The D-Class's locket did not drop");
            Helper.assertTrue(Scp427Light.litFor(Body) == null, "The light outlived the D-Class");
            clearMonsters(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp427_cureHalvesExposure")
    public static void cureHalvesExposure(GameTestHelper Helper) {
        Scp427TestPlayer User = player(Helper, 8.5, 8.5, "scp427_cured");
        Scp427Exposure.setExposure(User, 4000);
        Helper.assertTrue(AnomalyCure.cure(User, null), "AnomalyCure reported nothing to cure");
        Helper.assertTrue(Scp427Exposure.exposure(User) == 2000, "AnomalyCure left " + Scp427Exposure.exposure(User) + " ticks");
        Scp427State State = User.getData(Scp427Registry.Exposure.get());
        State.Ticks = Scp427Exposure.Limit + 1;
        Scp427Transformation.begin(Helper.getLevel(), User, State);
        Helper.assertFalse(AnomalyCure.cure(User, null), "AnomalyCure claimed to stop a change in progress");
        Helper.assertTrue(Scp427Exposure.isTransforming(User), "AnomalyCure stopped a change in progress");
        dismiss(Helper, User);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp427_documentAndCapture")
    public static void documentAndCapture(GameTestHelper Helper) {
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp427Registry.DocumentId);
        Helper.assertTrue(Document != null, "The SCP-427 dossier did not load");
        Helper.assertTrue(Document.Classification() == DocumentClass.Safe, "SCP-427 is not filed as Safe");
        Helper.assertTrue(Document.Condition() instanceof StrangerEncounterCondition, "SCP-427 does not arrive with the stranger");
        StrangerPlan Plan = ((StrangerEncounterCondition) Document.Condition()).plan();
        Helper.assertTrue("foundation".equals(Plan.Variant()), "SCP-427 arrives with the wrong stranger: " + Plan.Variant());
        Helper.assertTrue(Plan.Gifts().size() == 1 && Scp427LocketItem.is(Plan.Gifts().get(0).toStack()), "The stranger's gift is not the locket");
        Scp427Flesh Monster = Helper.spawnWithNoFreeWill(Scp427Registry.Flesh.get(), new BlockPos(8, 2, 8));
        Helper.assertFalse(Monster.getType().is(StasisImmune), "SCP-427-1 is tagged stasis immune");
        Helper.assertFalse(StasisPods.isVetoed(Monster), "Stasis pods refuse SCP-427-1");
        Monster.discard();
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp427_lightsRecover")
    public static void lightsRecover(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Stale = Helper.absolutePos(new BlockPos(3, 3, 3));
        World.setBlock(Stale, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, Scp427Light.Brightness), Block.UPDATE_ALL);
        BlockPos Foreign = Helper.absolutePos(new BlockPos(5, 3, 5));
        World.setBlock(Foreign, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15), Block.UPDATE_ALL);
        Scp427Light.Ledger.of(World.getServer()).track(World.dimension(), Stale);
        Scp427Light.Ledger.of(World.getServer()).track(World.dimension(), Foreign);
        int Cleared = Scp427Light.recover(World.getServer());
        Helper.assertTrue(Cleared == 1, "Recovery cleared " + Cleared + " lights");
        Helper.assertTrue(World.getBlockState(Stale).isAir(), "A light left over from a crash survived recovery");
        Helper.assertTrue(World.getBlockState(Foreign).is(Blocks.LIGHT), "Recovery removed a light it did not place");
        Helper.assertTrue(Scp427Light.Ledger.of(World.getServer()).size() == 0, "The light ledger was not emptied");
        Helper.succeed();
    }
}
