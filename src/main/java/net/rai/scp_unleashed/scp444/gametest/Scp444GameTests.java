package net.rai.scp_unleashed.scp444.gametest;

import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;
import net.rai.scp_unleashed.antimemetic.AntiMemeticRegistry;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.integration.voicechat.VoiceGate;
import net.rai.scp_unleashed.integration.voicechat.VoicechatProbe;
import net.rai.scp_unleashed.scp444.Scp444Affliction;
import net.rai.scp_unleashed.scp444.Scp444BookItem;
import net.rai.scp_unleashed.scp444.Scp444Reading;
import net.rai.scp_unleashed.scp444.Scp444Registry;
import net.rai.scp_unleashed.scp444.Scp444Silence;
import net.rai.scp_unleashed.scp444.Scp444Spread;
import net.rai.scp_unleashed.scp444.Scp444Sync;
import net.rai.scp_unleashed.senses.SensesRegistry;
import net.rai.scp_unleashed.storage.StorageContainerBlock;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import net.rai.scp_unleashed.storage.StorageRegistry;
import net.rai.scp_unleashed.storage.StorageTags;
import net.rai.scp_unleashed.stranger.StrangerEncounterCondition;
import net.rai.scp_unleashed.stranger.StrangerGift;
import net.rai.scp_unleashed.stranger.StrangerPlan;

import java.util.List;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp444GameTests {
    public static final String Arena = "scp444_arena";
    public static final List<String> ChatLines = List.of("msg @s hello", "tell @s hello", "w @s hello", "me waves", "say hello", "teammsg hello", "tm hello",
            "execute run say hello");
    public static final List<String> OtherLines = List.of("time query daytime", "effect give @s minecraft:glowing 5", "scp_unleashed scp444 status");

    private Scp444GameTests() {
    }

    private static Scp444TestPlayer player(GameTestHelper Helper, double X, double Z, String Name) {
        ServerLevel World = Helper.getLevel();
        Scp444TestPlayer User = new Scp444TestPlayer(World, Name);
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, 2.0, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(GameType.SURVIVAL);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, ServerPlayer User) {
        Scp444Reading.forget(User.getUUID());
        Scp444Sync.forget(User.getUUID());
        if (!User.isRemoved()) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    private static Scp444TestPlayer standing(GameTestHelper Helper, double X, double Z, float Yaw, String Name) {
        ServerLevel World = Helper.getLevel();
        Scp444TestPlayer User = new Scp444TestPlayer(World, Name);
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, 1.0, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        User.setYHeadRot(Yaw);
        World.addNewPlayer(User);
        User.setGameMode(GameType.SURVIVAL);
        return User;
    }

    private static Scp444TestPlayer speaker(GameTestHelper Helper, double X, double Z, float Yaw, String Name) {
        Scp444TestPlayer Speaker = standing(Helper, X, Z, Yaw, Name);
        Scp444Affliction.afflict(Speaker);
        Scp444Spread.forget(Speaker.getUUID());
        return Speaker;
    }

    private static void wall(GameTestHelper Helper, int FromX, int ToX, int FromZ, int ToZ, BlockState State) {
        for (int X = FromX; X <= ToX; X++) {
            for (int Z = FromZ; Z <= ToZ; Z++) {
                for (int Y = 1; Y <= 4; Y++) {
                    Helper.setBlock(new BlockPos(X, Y, Z), State);
                }
            }
        }
    }

    private static void paneRow(GameTestHelper Helper, int FromX, int ToX, int Z) {
        for (int X = FromX; X <= ToX; X++) {
            BlockState Pane = Blocks.GLASS_PANE.defaultBlockState().setValue(CrossCollisionBlock.EAST, X < ToX).setValue(CrossCollisionBlock.WEST, X > FromX);
            for (int Y = 1; Y <= 4; Y++) {
                Helper.setBlock(new BlockPos(X, Y, Z), Pane);
            }
        }
    }

    private static void dismissAll(GameTestHelper Helper, ServerPlayer... Users) {
        for (ServerPlayer User : Users) {
            Scp444Spread.forget(User.getUUID());
            dismiss(Helper, User);
        }
    }

    private static ItemStack book() {
        return new ItemStack(Scp444Registry.Book.get());
    }

    private static void use(ServerPlayer User) {
        User.gameMode.useItem(User, User.serverLevel(), User.getMainHandItem(), InteractionHand.MAIN_HAND);
    }

    private static boolean chats(ServerPlayer User) {
        return CommonHooks.getServerChatSubmittedDecorator().decorate(User, Component.literal("hello")) != null;
    }

    private static boolean blocked(ServerPlayer User, String Line) {
        CommandSourceStack Source = User.createCommandSourceStack().withPermission(4).withSuppressedOutput();
        ParseResults<CommandSourceStack> Parse = User.server.getCommands().getDispatcher().parse(Line, Source);
        CommandEvent Event = new CommandEvent(Parse);
        return NeoForge.EVENT_BUS.post(Event).isCanceled();
    }

    @GameTest(template = Arena, timeoutTicks = Scp444Reading.ReadTicks + 60, batch = "scp444_readingAfflicts")
    public static void readingAfflicts(GameTestHelper Helper) {
        Scp444TestPlayer Lingers = player(Helper, 4.5, 4.5, "scp444_lingers");
        Scp444TestPlayer Glances = player(Helper, 8.5, 4.5, "scp444_glances");
        Scp444TestPlayer Turns = player(Helper, 12.5, 4.5, "scp444_turns");
        for (ServerPlayer User : List.of(Lingers, Glances, Turns)) {
            User.setItemInHand(InteractionHand.MAIN_HAND, book());
            use(User);
            Helper.assertTrue(Scp444Reading.isReading(User), User.getGameProfile().getName() + " did not start reading after using the book");
            Helper.assertFalse(Scp444Affliction.isAfflicted(User), "Opening the book afflicted at once");
        }
        Helper.startSequence().thenIdle(5).thenExecute(() -> {
            Scp444Reading.turned(Turns, 1);
            Helper.assertTrue(Scp444Affliction.isAfflicted(Turns), "Turning past the first page did not afflict the reader");
            Helper.assertFalse(Scp444Affliction.isAfflicted(Lingers), "The lingering reader was afflicted too early");
        }).thenIdle(10).thenExecute(() -> {
            Scp444Reading.closed(Glances);
            Helper.assertFalse(Scp444Reading.isReading(Glances), "Closing the book left a reading session open");
        }).thenIdle(Scp444Reading.ReadTicks).thenExecute(() -> {
            Helper.assertTrue(Scp444Affliction.isAfflicted(Lingers), "Keeping the book open did not afflict the reader");
            Helper.assertFalse(Scp444Affliction.isAfflicted(Glances), "A glance shorter than the reading time afflicted the reader");
            Helper.assertTrue(Scp444Affliction.since(Lingers) != null, "The affliction has no start time");
            for (ServerPlayer User : List.of(Lingers, Glances, Turns)) {
                dismiss(Helper, User);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp444_chatAndCommandsBlocked")
    public static void chatAndCommandsBlocked(GameTestHelper Helper) {
        Scp444TestPlayer Mute = player(Helper, 5.5, 5.5, "scp444_mute");
        Scp444TestPlayer Free = player(Helper, 10.5, 10.5, "scp444_free");
        Helper.assertTrue(chats(Mute) && chats(Free), "Chat was blocked before anyone read the book");
        Helper.assertTrue(Scp444Affliction.afflict(Mute), "The test player could not be afflicted");
        Helper.assertFalse(chats(Mute), "An afflicted player's chat message went through");
        Helper.assertTrue(chats(Free), "An unafflicted player's chat message was blocked");
        for (String Line : ChatLines) {
            Helper.assertTrue(blocked(Mute, Line), "The afflicted player could still run /" + Line);
            Helper.assertFalse(blocked(Free, Line), "An unafflicted player could not run /" + Line);
        }
        for (String Line : OtherLines) {
            Helper.assertFalse(blocked(Mute, Line), "The afflicted player was refused an ordinary command: /" + Line);
        }
        CommandSourceStack Source = Mute.createCommandSourceStack().withPermission(4).withSuppressedOutput();
        Mute.server.getCommands().performPrefixedCommand(Source, "effect give @s minecraft:glowing 5");
        Helper.assertTrue(Mute.hasEffect(MobEffects.GLOWING), "An ordinary command did not run for the afflicted player");
        Mute.server.getCommands().performPrefixedCommand(Source, "scp_unleashed scp444 clear");
        Helper.assertFalse(Scp444Affliction.isAfflicted(Mute), "The clear command did not lift the affliction");
        Helper.assertTrue(chats(Mute), "Chat stayed blocked after the affliction was cleared");
        dismiss(Helper, Mute);
        dismiss(Helper, Free);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp444_persistsAndClearsOnDeath")
    public static void persistsAndClearsOnDeath(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp444TestPlayer User = player(Helper, 6.5, 6.5, "scp444_saved");
        Helper.assertTrue(Scp444Affliction.afflict(User), "The test player could not be afflicted");
        long Since = Scp444Affliction.since(User);
        CompoundTag Saved = new CompoundTag();
        User.saveWithoutId(Saved);
        dismiss(Helper, User);
        Scp444TestPlayer Returned = new Scp444TestPlayer(World, "scp444_returned");
        Returned.load(Saved);
        Helper.assertTrue(Scp444Affliction.isAfflicted(Returned), "The affliction did not survive a save and reload");
        Helper.assertTrue(Scp444Affliction.since(Returned) == Since, "The affliction's start time changed across a save and reload");
        Helper.assertFalse(chats(Returned), "The reloaded player could chat");
        Helper.assertFalse(AnomalyCure.cure(Returned, null), "SCP-006 or another anomaly cure lifted SCP-444");
        Helper.assertTrue(Scp444Affliction.isAfflicted(Returned), "An anomaly cure cleared SCP-444");
        Returned.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 4));
        Returned.removeAllEffects();
        Helper.assertTrue(Scp444Affliction.isAfflicted(Returned), "Clearing effects cleared SCP-444");
        Scp444TestPlayer Dying = player(Helper, 9.5, 9.5, "scp444_dying");
        Scp444Affliction.afflict(Dying);
        Dying.hurt(Dying.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
        Helper.startSequence().thenIdle(2).thenExecute(() -> {
            Helper.assertFalse(Scp444Affliction.isAfflicted(Dying), "Death did not clear SCP-444");
            Helper.assertTrue(chats(Dying), "The dead player's chat stayed blocked");
            dismiss(Helper, Dying);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = Scp444Reading.ReadTicks + 40, batch = "scp444_protection")
    public static void protection(GameTestHelper Helper) {
        Scp444TestPlayer Blind = player(Helper, 4.5, 8.5, "scp444_blind");
        Scp444TestPlayer Shielded = player(Helper, 11.5, 8.5, "scp444_shielded");
        Blind.setItemSlot(EquipmentSlot.HEAD, new ItemStack(SensesRegistry.Blindfold.get()));
        Shielded.addEffect(new MobEffectInstance(AntiMemeticRegistry.Shielded, 6000, 0));
        for (ServerPlayer User : List.of(Blind, Shielded)) {
            User.setItemInHand(InteractionHand.MAIN_HAND, book());
            use(User);
        }
        Helper.assertFalse(Scp444Reading.isReading(Blind), "A blindfolded player opened the book");
        Helper.assertTrue(Scp444Reading.isReading(Shielded), "A protected player could not open the book");
        Scp444Reading.turned(Shielded, 3);
        Helper.startSequence().thenIdle(Scp444Reading.ReadTicks + 5).thenExecute(() -> {
            Helper.assertFalse(Scp444Affliction.isAfflicted(Blind), "A blindfolded player was afflicted");
            Helper.assertFalse(Scp444Affliction.isAfflicted(Shielded), "Anti-memetic protection did not stop the script taking hold");
            Shielded.removeAllEffects();
            Helper.assertTrue(Scp444Affliction.afflict(Shielded), "Forcing the affliction failed once protection wore off");
            Shielded.addEffect(new MobEffectInstance(AntiMemeticRegistry.Shielded, 6000, 0));
            Helper.assertTrue(Scp444Affliction.isAfflicted(Shielded), "Taking a pill after reading lifted SCP-444");
            dismiss(Helper, Blind);
            dismiss(Helper, Shielded);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 240, batch = "scp444_controlledBody")
    public static void controlledBody(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp444TestPlayer User = player(Helper, 8.5, 6.5, "scp444_controller");
        User.enlist(true);
        DClass Body = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new BlockPos(8, 2, 8));
        UUID BodyId = Body.getUUID();
        Helper.assertTrue(ControlManager.possess(User, Body), "The test player could not take control of the D-Class");
        User.setItemInHand(InteractionHand.MAIN_HAND, book());
        use(User);
        Scp444Reading.turned(User, 1);
        Helper.assertTrue(Scp444Affliction.isAfflicted(User), "Reading in a controlled body did not afflict that body");
        Helper.assertFalse(chats(User), "A controller in an afflicted body could still chat");
        Scp444Sync.update(User);
        Helper.assertTrue(VoiceGate.isSilenced(User.getUUID()), "A controller in an afflicted body was not silenced for voice chat");
        ControlManager.release(User);
        Helper.assertFalse(ControlSessions.isControlling(User), "Release left the control session open");
        Helper.assertFalse(Scp444Affliction.isAfflicted(User), "The affliction followed the controller home");
        Helper.assertTrue(chats(User), "The controller could not chat in their own clean body");
        Scp444Sync.update(User);
        Helper.assertFalse(VoiceGate.isSilenced(User.getUUID()), "Voice stayed blocked in the controller's own clean body");
        Entity Returned = World.getEntity(BodyId);
        Helper.assertTrue(Returned instanceof DClass && Scp444Affliction.isAfflicted(Returned), "The released D-Class body lost its affliction");
        Helper.assertTrue(ControlManager.possess(User, (DClass) Returned), "Could not take the afflicted body back");
        Helper.assertFalse(chats(User), "Re-entering the afflicted body did not block chat again");
        User.hurt(User.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
        Helper.startSequence().thenWaitUntil(() -> {
            Helper.assertFalse(ControlSessions.isControlling(User), "The controlled body has not died yet");
            Helper.assertFalse(ControlManager.hasPendingDeath(User), "A deferred death is still pending");
        }).thenExecute(() -> {
            Helper.assertTrue(User.isAlive(), "The controller did not return home alive");
            Helper.assertFalse(Scp444Affliction.isAfflicted(User), "The dead body's affliction followed the controller home");
            Helper.assertTrue(chats(User), "The controller could not chat after the afflicted body died");
            Entity Left = World.getEntity(BodyId);
            Helper.assertTrue(Left == null || !Left.isAlive(), "The afflicted D-Class body is still alive");
            Scp444Affliction.afflict(User);
            DClass Clean = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new BlockPos(9, 2, 7));
            Helper.assertTrue(ControlManager.possess(User, Clean), "The afflicted controller could not take a clean body");
            Helper.assertTrue(chats(User), "A clean body was silenced because its controller is afflicted at home");
            ControlManager.release(User);
            Helper.assertTrue(Scp444Affliction.isAfflicted(User) && !chats(User), "The controller's own affliction was lost while away");
            User.enlist(false);
            dismiss(Helper, User);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp444_strangerBringsBook")
    public static void strangerBringsBook(GameTestHelper Helper) {
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp444Registry.DocumentId);
        Helper.assertTrue(Document != null, "The SCP-444 dossier did not load");
        Helper.assertTrue(Document.Classification() == DocumentClass.Euclid, "SCP-444 is not filed as Euclid");
        Helper.assertTrue(Document.Condition() instanceof StrangerEncounterCondition, "SCP-444 does not arrive with the stranger");
        StrangerPlan Plan = ((StrangerEncounterCondition) Document.Condition()).plan();
        Helper.assertTrue("foundation".equals(Plan.Variant()), "SCP-444 arrives with the wrong stranger: " + Plan.Variant());
        Helper.assertTrue(Plan.Gifts().size() == 1, "The stranger brings " + Plan.Gifts().size() + " gifts");
        StrangerGift Gift = Plan.Gifts().get(0);
        ItemStack Stack = Gift.toStack();
        Helper.assertTrue(Scp444BookItem.is(Stack) && Stack.getCount() == 1, "The stranger's gift is not the SCP-444 book: " + Stack);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = Scp444Reading.ReadTicks + 60, batch = "scp444_storageKeepsBookInert")
    public static void storageKeepsBookInert(GameTestHelper Helper) {
        BlockPos Spot = new BlockPos(8, 2, 8);
        BlockState State = StorageRegistry.SmallContainer.get().defaultBlockState().setValue(StorageContainerBlock.Facing, Direction.NORTH);
        Helper.setBlock(Spot, State);
        Helper.assertTrue(Helper.getBlockEntity(Spot) instanceof StorageContainerBlockEntity, "No storage container block entity");
        StorageContainerBlockEntity Locker = Helper.getBlockEntity(Spot);
        Scp444TestPlayer Keeper = player(Helper, 8.5, 6.5, "scp444_keeper");
        Scp444TestPlayer Nearby = player(Helper, 9.5, 7.0, "scp444_nearby");
        ItemStack Original = book();
        Helper.assertTrue(StorageTags.isStorable(Original), "The SCP-444 book is not tagged as storable");
        Helper.assertTrue(Locker.refusalFor(Original) == null, "The storage container refused the SCP-444 book");
        Keeper.setItemInHand(InteractionHand.MAIN_HAND, Original.copy());
        Locker.store(Keeper, Keeper.getMainHandItem());
        Helper.assertTrue(Keeper.getMainHandItem().isEmpty(), "Storing the book left it in the keeper's hand");
        Helper.assertTrue(Scp444BookItem.is(Locker.peekContents()), "The container does not hold the book");
        Helper.startSequence().thenIdle(Scp444Reading.ReadTicks + 20).thenExecute(() -> {
            for (ServerPlayer User : List.of(Keeper, Nearby)) {
                Scp444Sync.update(User);
                Helper.assertFalse(Scp444Affliction.isAfflicted(User), "A stored book afflicted " + User.getGameProfile().getName());
                Helper.assertFalse(Scp444Reading.isReading(User), "A stored book opened a reading session");
                Helper.assertFalse(Scp444Silence.isSilenced(User) || VoiceGate.isSilenced(User.getUUID()), "A stored book silenced " + User.getGameProfile().getName());
                Helper.assertTrue(chats(User), "A stored book blocked chat");
            }
            Locker.take(Keeper, InteractionHand.MAIN_HAND);
            Helper.assertFalse(Locker.hasContents(), "Taking the book left the container occupied");
            Helper.assertTrue(ItemStack.matches(Keeper.getMainHandItem(), Original), "The book came back changed: " + Keeper.getMainHandItem());
            use(Keeper);
            Helper.assertTrue(Scp444Reading.isReading(Keeper), "The book could not be read after it was taken out");
            Scp444Reading.turned(Keeper, 1);
            Helper.assertTrue(Scp444Affliction.isAfflicted(Keeper), "Reading the book after storage did not afflict the reader");
            dismiss(Helper, Keeper);
            dismiss(Helper, Nearby);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp444_voiceGate")
    public static void voiceGate(GameTestHelper Helper) {
        Scp444TestPlayer Mute = player(Helper, 5.5, 5.5, "scp444_voice_mute");
        Scp444TestPlayer Free = player(Helper, 10.5, 10.5, "scp444_voice_free");
        Scp444Affliction.afflict(Mute);
        Scp444Sync.update(Mute);
        Scp444Sync.update(Free);
        Helper.assertTrue(VoiceGate.isSilenced(Mute.getUUID()), "The afflicted player's voice is not gated");
        Helper.assertFalse(VoiceGate.isSilenced(Free.getUUID()), "A free player's voice is gated");
        if (VoiceGate.isLoaded()) {
            String Problem = VoicechatProbe.check(Mute, Free);
            Helper.assertTrue(Problem == null, "Simple Voice Chat: " + Problem);
        }
        Scp444Affliction.clear(Mute);
        Scp444Sync.update(Mute);
        Helper.assertFalse(VoiceGate.isSilenced(Mute.getUUID()), "Clearing the affliction left the voice gated");
        dismiss(Helper, Mute);
        dismiss(Helper, Free);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp444_spreadReachesNearby")
    public static void spreadReachesNearby(GameTestHelper Helper) {
        Scp444TestPlayer Speaker = speaker(Helper, 3.5, 8.5, -90.0f, "scp444_spread_speaker");
        Scp444TestPlayer Near = standing(Helper, 6.5, 8.5, 90.0f, "scp444_spread_near");
        Scp444TestPlayer Behind = standing(Helper, 1.5, 8.5, 0.0f, "scp444_spread_behind");
        Scp444TestPlayer Far = standing(Helper, 9.8, 8.5, 90.0f, "scp444_spread_far");
        Helper.assertTrue(Scp444Spread.ready(Speaker), "The speaker starts on cooldown");
        Helper.assertFalse(chats(Speaker), "The afflicted speaker's chat went through");
        Helper.assertTrue(Scp444Affliction.isAfflicted(Near), "A player three blocks away in plain sight did not catch it");
        Helper.assertTrue(Scp444Affliction.isAfflicted(Behind), "A player standing behind the speaker did not catch it");
        Helper.assertFalse(Scp444Affliction.isAfflicted(Far), "A player beyond the radius caught it");
        Helper.assertTrue(VoiceGate.isSilenced(Near.getUUID()), "The new instance's voice was not gated at once");
        Helper.assertTrue(chats(Far), "The player beyond the radius lost chat");
        Helper.assertTrue(Scp444Silence.isSilenced(Near), "The new instance could still chat");
        dismissAll(Helper, Speaker, Near, Behind, Far);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp444_spreadNeedsClearLine")
    public static void spreadNeedsClearLine(GameTestHelper Helper) {
        wall(Helper, 10, 10, 7, 10, Blocks.STONE.defaultBlockState());
        paneRow(Helper, 5, 11, 6);
        wall(Helper, 5, 11, 11, 11, Blocks.GLASS.defaultBlockState());
        Scp444TestPlayer Speaker = speaker(Helper, 8.5, 8.5, 90.0f, "scp444_line_speaker");
        Scp444TestPlayer Open = standing(Helper, 5.5, 8.5, -90.0f, "scp444_line_open");
        Scp444TestPlayer Stone = standing(Helper, 12.5, 8.5, 90.0f, "scp444_line_stone");
        Scp444TestPlayer Pane = standing(Helper, 8.5, 4.5, 0.0f, "scp444_line_pane");
        Scp444TestPlayer Glass = standing(Helper, 8.5, 12.5, 180.0f, "scp444_line_glass");
        Vec3 Origin = Scp444Spread.origin(Speaker);
        Helper.assertTrue(Scp444Spread.reaches(Helper.getLevel(), Origin, Open, Speaker), "The raycast to the open listener was blocked");
        for (ServerPlayer Hidden : List.of(Stone, Pane, Glass)) {
            Helper.assertFalse(Scp444Spread.reaches(Helper.getLevel(), Origin, Hidden, Speaker), "The raycast passed through to " + Hidden.getGameProfile().getName());
        }
        Helper.assertFalse(chats(Speaker), "The afflicted speaker's chat went through");
        Helper.assertTrue(Scp444Affliction.isAfflicted(Open), "The listener with a clear line did not catch it");
        Helper.assertFalse(Scp444Affliction.isAfflicted(Stone), "It passed through a stone wall");
        Helper.assertFalse(Scp444Affliction.isAfflicted(Pane), "It passed through glass panes");
        Helper.assertFalse(Scp444Affliction.isAfflicted(Glass), "It passed through glass blocks");
        dismissAll(Helper, Speaker, Open, Stone, Pane, Glass);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp444_spreadProtection")
    public static void spreadProtection(GameTestHelper Helper) {
        Scp444TestPlayer Speaker = speaker(Helper, 8.5, 8.5, 90.0f, "scp444_guard_speaker");
        Scp444TestPlayer Pill = standing(Helper, 5.5, 8.5, -90.0f, "scp444_guard_pill");
        Scp444TestPlayer Plugs = standing(Helper, 11.5, 8.5, 90.0f, "scp444_guard_plugs");
        Scp444TestPlayer Blind = standing(Helper, 8.5, 5.5, 0.0f, "scp444_guard_blind");
        Scp444TestPlayer Builder = standing(Helper, 8.5, 11.5, 180.0f, "scp444_guard_creative");
        Scp444TestPlayer Ghost = standing(Helper, 6.5, 6.5, 0.0f, "scp444_guard_ghost");
        Scp444TestPlayer Plain = standing(Helper, 10.5, 10.5, 0.0f, "scp444_guard_plain");
        Pill.addEffect(new MobEffectInstance(AntiMemeticRegistry.Shielded, 6000, 0));
        Plugs.setItemSlot(EquipmentSlot.HEAD, new ItemStack(SensesRegistry.Earplugs.get()));
        Blind.setItemSlot(EquipmentSlot.HEAD, new ItemStack(SensesRegistry.Blindfold.get()));
        Builder.setGameMode(GameType.CREATIVE);
        Ghost.setGameMode(GameType.SPECTATOR);
        Helper.assertTrue(blocked(Speaker, "me waves"), "The afflicted speaker could use /me");
        Helper.assertFalse(Scp444Affliction.isAfflicted(Pill), "Anti-memetic protection did not stop it");
        Helper.assertFalse(Scp444Affliction.isAfflicted(Plugs), "Earplugs did not stop it");
        Helper.assertTrue(Scp444Affliction.isAfflicted(Blind), "A blindfold alone stopped it");
        Helper.assertFalse(Scp444Affliction.isAfflicted(Builder), "A creative player caught it");
        Helper.assertFalse(Scp444Affliction.isAfflicted(Ghost), "A spectator caught it");
        Helper.assertTrue(Scp444Affliction.isAfflicted(Plain), "An unprotected listener did not catch it from a blocked command");
        dismissAll(Helper, Speaker, Pill, Plugs, Blind, Builder, Ghost, Plain);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = Scp444Spread.Cooldown + 40, batch = "scp444_spreadCooldown")
    public static void spreadCooldown(GameTestHelper Helper) {
        Scp444TestPlayer Speaker = speaker(Helper, 8.5, 8.5, 90.0f, "scp444_pace_speaker");
        Helper.assertFalse(chats(Speaker), "The afflicted speaker's chat went through");
        Helper.assertFalse(Scp444Spread.ready(Speaker), "The burst did not start a cooldown");
        Scp444TestPlayer Late = standing(Helper, 6.5, 8.5, -90.0f, "scp444_pace_late");
        Helper.startSequence().thenIdle(5).thenExecute(() -> {
            Helper.assertFalse(chats(Speaker), "A chat attempt during the cooldown went through");
            Helper.assertFalse(Scp444Affliction.isAfflicted(Late), "It spread again during the cooldown");
        }).thenIdle(Scp444Spread.Cooldown).thenExecute(() -> {
            Helper.assertTrue(Scp444Spread.ready(Speaker), "The cooldown did not run out");
            Helper.assertFalse(chats(Speaker), "The afflicted speaker's chat went through");
            Helper.assertTrue(Scp444Affliction.isAfflicted(Late), "It did not spread once the cooldown ran out");
            dismissAll(Helper, Speaker, Late);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = Scp444Spread.Cooldown + 40, batch = "scp444_spreadRuleOff")
    public static void spreadRuleOff(GameTestHelper Helper) {
        Scp444TestPlayer Speaker = speaker(Helper, 8.5, 8.5, 90.0f, "scp444_rule_speaker");
        Scp444TestPlayer Listener = standing(Helper, 6.5, 8.5, -90.0f, "scp444_rule_listener");
        Helper.getLevel().getGameRules().getRule(Scp444Registry.Spread).set(false, Helper.getLevel().getServer());
        try {
            Helper.assertFalse(chats(Speaker), "The afflicted speaker's chat went through with spreading off");
            Helper.assertFalse(Scp444Affliction.isAfflicted(Listener), "It spread with the game rule off");
        } finally {
            Helper.getLevel().getGameRules().getRule(Scp444Registry.Spread).set(true, Helper.getLevel().getServer());
        }
        Helper.startSequence().thenIdle(Scp444Spread.Cooldown + 1).thenExecute(() -> {
            Helper.assertFalse(chats(Speaker), "The afflicted speaker's chat went through");
            Helper.assertTrue(Scp444Affliction.isAfflicted(Listener), "It did not spread once the game rule was back on");
            dismissAll(Helper, Speaker, Listener);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp444_ambientNeverSpreads")
    public static void ambientNeverSpreads(GameTestHelper Helper) {
        Scp444TestPlayer Speaker = speaker(Helper, 8.5, 8.5, 90.0f, "scp444_quiet_speaker");
        Scp444TestPlayer Listener = standing(Helper, 7.0, 8.5, -90.0f, "scp444_quiet_listener");
        Speaker.enlist(true);
        Listener.enlist(true);
        for (int Index = 0; Index < 4000; Index++) {
            Scp444Affliction.ambience(Speaker);
        }
        Helper.assertFalse(Scp444Affliction.isAfflicted(Listener), "The ambient glyphs spread it");
        Helper.startSequence().thenIdle(120).thenExecute(() -> {
            Speaker.enlist(false);
            Listener.enlist(false);
            Helper.assertFalse(Scp444Affliction.isAfflicted(Listener), "Standing beside a silent instance spread it");
            Helper.assertTrue(Scp444Spread.ready(Speaker), "Ambient glyphs used up the speaker's cooldown");
            dismissAll(Helper, Speaker, Listener);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp444_spreadIntoControlledBody")
    public static void spreadIntoControlledBody(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp444TestPlayer Speaker = speaker(Helper, 8.5, 8.5, 90.0f, "scp444_body_speaker");
        Scp444TestPlayer Controller = standing(Helper, 6.5, 10.5, 0.0f, "scp444_body_controller");
        Controller.enlist(true);
        DClass Body = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new BlockPos(6, 1, 8));
        DClass Bystander = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new BlockPos(10, 1, 8));
        UUID BodyId = Body.getUUID();
        Helper.assertTrue(ControlManager.possess(Controller, Body), "The test player could not take control of the D-Class");
        Helper.assertFalse(chats(Speaker), "The afflicted speaker's chat went through");
        Helper.assertTrue(Scp444Affliction.isAfflicted(Controller), "The controlled D-Class body did not catch it");
        Helper.assertFalse(chats(Controller), "The controller could chat from the newly afflicted body");
        Helper.assertTrue(Scp444Affliction.isAfflicted(Bystander), "An idle D-Class in plain sight did not catch it");
        ControlManager.release(Controller);
        Helper.assertFalse(ControlSessions.isControlling(Controller), "Release left the control session open");
        Helper.assertFalse(Scp444Affliction.isAfflicted(Controller), "It leaked to the controller's own body");
        Helper.assertTrue(chats(Controller), "The controller lost chat in their own body");
        Entity Returned = World.getEntity(BodyId);
        Helper.assertTrue(Returned instanceof LivingEntity && Scp444Affliction.isAfflicted(Returned), "The released D-Class body lost the affliction");
        Controller.enlist(false);
        dismissAll(Helper, Speaker, Controller);
        Helper.succeed();
    }
}
