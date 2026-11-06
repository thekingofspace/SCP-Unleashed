package net.rai.scp_unleashed.scp420j.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.scp420j.Scp420jChatter;
import net.rai.scp_unleashed.scp420j.Scp420jFloat;
import net.rai.scp_unleashed.scp420j.Scp420jHempBlock;
import net.rai.scp_unleashed.scp420j.Scp420jRegistry;
import net.rai.scp_unleashed.stranger.StrangerEncounterCondition;
import net.rai.scp_unleashed.stranger.StrangerGift;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp420jGameTests {
    public static final String Arena = "scp420j_arena";
    public static final BlockPos Spot = new BlockPos(5, 1, 5);
    public static final int Harvests = 40;

    private Scp420jGameTests() {
    }

    public static final class Subject extends ServerPlayer {
        public Subject(ServerLevel World, GameProfile Profile) {
            super(World.getServer(), World, Profile, ClientInformation.createDefault());
            new Quiet(World.getServer(), this);
        }
    }

    private static final class Quiet extends ServerGamePacketListenerImpl {
        private Quiet(MinecraftServer Server, ServerPlayer User) {
            super(Server, new Connection(PacketFlow.SERVERBOUND), User, CommonListenerCookie.createInitial(User.getGameProfile(), false));
        }

        @Override
        public void send(Packet<?> Sent, @Nullable PacketSendListener Listener) {
        }
    }

    private static Subject subject(GameTestHelper Helper, double X, double Y, double Z) {
        ServerLevel World = Helper.getLevel();
        Subject User = new Subject(World, new GameProfile(UUID.randomUUID(), "scp420j_test"));
        Vec3 At = Helper.absoluteVec(new Vec3(X, Y, Z));
        User.moveTo(At.x, At.y, At.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(GameType.SURVIVAL);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, ServerPlayer User) {
        if (!User.isRemoved()) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    private static void floor(GameTestHelper Helper, int Level) {
        for (int X = 0; X < 12; X++) {
            for (int Z = 0; Z < 12; Z++) {
                Helper.setBlock(new BlockPos(X, Level, Z), Blocks.STONE.defaultBlockState());
            }
        }
    }

    private static void drive(GameTestHelper Helper, ServerPlayer User) {
        Helper.onEachTick(() -> {
            if (!User.isRemoved()) {
                Scp420jFloat.tick(User);
            }
        });
    }

    private static ItemStack joints(ServerPlayer User, int Count) {
        ItemStack Stack = new ItemStack(Scp420jRegistry.Joint.get(), Count);
        User.setItemInHand(InteractionHand.MAIN_HAND, Stack);
        return Stack;
    }

    private static void light(ServerPlayer User) {
        User.getCooldowns().removeCooldown(Scp420jRegistry.Joint.get());
        Scp420jRegistry.Joint.get().use(User.level(), User, InteractionHand.MAIN_HAND);
    }

    private static boolean lowGravity(ServerPlayer User) {
        AttributeInstance Gravity = User.getAttribute(Attributes.GRAVITY);
        return Gravity != null && Gravity.hasModifier(Scp420jFloat.GravityId);
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp420j_crop")
    public static void seedsGrowThroughEightStages(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Subject User = subject(Helper, 2.5, 2.0, 2.5);
        BlockPos Soil = new BlockPos(5, 1, 5);
        Helper.setBlock(Soil, Blocks.FARMLAND.defaultBlockState());
        ItemStack Seeds = new ItemStack(Scp420jRegistry.Seeds.get(), 4);
        User.setItemInHand(InteractionHand.MAIN_HAND, Seeds);
        BlockPos Absolute = Helper.absolutePos(Soil);
        BlockHitResult Hit = new BlockHitResult(Vec3.atCenterOf(Absolute).add(0.0, 0.5, 0.0), Direction.UP, Absolute, false);
        Seeds.useOn(new UseOnContext(User, InteractionHand.MAIN_HAND, Hit));
        BlockState Planted = World.getBlockState(Absolute.above());
        Helper.assertTrue(Planted.is(Scp420jRegistry.Hemp.get()), "Seeds used on farmland placed " + Planted + " instead of hemp");
        Helper.assertTrue(Planted.getValue(CropBlock.AGE) == 0, "Freshly planted hemp is not at stage 0");
        Helper.assertTrue(User.getMainHandItem().getCount() == 3, "Planting did not use up one seed");
        Helper.setBlock(new BlockPos(7, 1, 5), Blocks.STONE.defaultBlockState());
        ItemStack Refused = new ItemStack(Scp420jRegistry.Seeds.get(), 1);
        BlockPos Stone = Helper.absolutePos(new BlockPos(7, 1, 5));
        Refused.useOn(new UseOnContext(User, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(Stone).add(0.0, 0.5, 0.0), Direction.UP, Stone, false)));
        Helper.assertTrue(World.getBlockState(Stone.above()).isAir(), "Hemp seeds were planted on bare stone");
        Scp420jHempBlock Hemp = Scp420jRegistry.Hemp.get();
        Helper.assertTrue(Hemp.getMaxAge() == 7 && CropBlock.AGE.getPossibleValues().size() == 8, "Hemp does not have eight growth stages");
        double Previous = -1.0;
        for (int Stage = 0; Stage <= 7; Stage++) {
            BlockState State = Hemp.getStateForAge(Stage);
            double Top = State.getShape(World, Absolute.above()).max(Direction.Axis.Y);
            Helper.assertTrue(Top > Previous, "Stage " + Stage + " is not taller than the stage before it");
            Previous = Top;
        }
        List<Integer> Seen = new ArrayList<>();
        Seen.add(0);
        int Meals = 0;
        while (World.getBlockState(Absolute.above()).getValue(CropBlock.AGE) < 7 && Meals < 20) {
            Helper.assertTrue(BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), World, Absolute.above()), "Bone meal did nothing to growing hemp");
            Seen.add(World.getBlockState(Absolute.above()).getValue(CropBlock.AGE));
            Meals++;
        }
        for (int Index = 1; Index < Seen.size(); Index++) {
            Helper.assertTrue(Seen.get(Index) > Seen.get(Index - 1), "Bone meal did not advance the crop: " + Seen);
        }
        Helper.assertTrue(Seen.get(Seen.size() - 1) == 7, "Bone meal never brought hemp to stage 7: " + Seen);
        Helper.assertTrue(!BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), World, Absolute.above()), "Bone meal still worked on mature hemp");
        dismiss(Helper, User);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp420j_harvest")
    public static void harvestDropsLeavesAndSeeds(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Absolute = Helper.absolutePos(Spot.above());
        Scp420jHempBlock Hemp = Scp420jRegistry.Hemp.get();
        int LeastLeaves = 99;
        int MostLeaves = 0;
        int LeastSeeds = 99;
        int MostSeeds = 0;
        for (int Round = 0; Round < Harvests; Round++) {
            List<ItemStack> Drops = Block.getDrops(Hemp.getStateForAge(7), World, Absolute, null);
            int Leaves = count(Drops, Scp420jRegistry.Leaf.get().getDefaultInstance());
            int Seeds = count(Drops, Scp420jRegistry.Seeds.get().getDefaultInstance());
            Helper.assertTrue(Leaves + Seeds == Drops.stream().mapToInt(ItemStack::getCount).sum(), "Mature hemp dropped something else: " + Drops);
            LeastLeaves = Math.min(LeastLeaves, Leaves);
            MostLeaves = Math.max(MostLeaves, Leaves);
            LeastSeeds = Math.min(LeastSeeds, Seeds);
            MostSeeds = Math.max(MostSeeds, Seeds);
        }
        Helper.assertTrue(LeastLeaves >= 1 && MostLeaves <= 3, "Mature hemp leaves ranged " + LeastLeaves + " to " + MostLeaves + ", wanted 1 to 3");
        Helper.assertTrue(LeastSeeds >= 1 && MostSeeds <= 2, "Mature hemp seeds ranged " + LeastSeeds + " to " + MostSeeds + ", wanted 1 to 2");
        Helper.assertTrue(MostLeaves > LeastLeaves && MostSeeds > LeastSeeds, "Harvest counts never varied over " + Harvests + " harvests");
        for (int Stage = 0; Stage < 7; Stage++) {
            List<ItemStack> Drops = Block.getDrops(Hemp.getStateForAge(Stage), World, Absolute, null);
            Helper.assertTrue(Drops.size() == 1 && Drops.get(0).is(Scp420jRegistry.Seeds.get()) && Drops.get(0).getCount() == 1,
                    "Immature hemp at stage " + Stage + " dropped " + Drops + " instead of one seed");
        }
        Helper.assertTrue(ComposterBlock.getValue(new ItemStack(Scp420jRegistry.Seeds.get())) > 0.0f, "Hemp seeds cannot be composted");
        Helper.assertTrue(ComposterBlock.getValue(new ItemStack(Scp420jRegistry.Leaf.get())) > 0.0f, "Hemp leaves cannot be composted");
        Helper.succeed();
    }

    private static int count(List<ItemStack> Drops, ItemStack Kind) {
        return Drops.stream().filter(Stack -> ItemStack.isSameItem(Stack, Kind)).mapToInt(ItemStack::getCount).sum();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp420j_craft")
    public static void leavesAndPaperMakeAJoint(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ItemStack Leaf = new ItemStack(Scp420jRegistry.Leaf.get());
        List<ItemStack> Grid = new ArrayList<>(List.of(ItemStack.EMPTY, Leaf.copy(), ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.PAPER), ItemStack.EMPTY, Leaf.copy(),
                ItemStack.EMPTY, ItemStack.EMPTY));
        Optional<RecipeHolder<CraftingRecipe>> Found = World.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, Grid), World);
        Helper.assertTrue(Found.isPresent(), "Two hemp leaves and a paper match no recipe");
        ItemStack Made = Found.get().value().assemble(CraftingInput.of(3, 3, Grid), World.registryAccess());
        Helper.assertTrue(Made.is(Scp420jRegistry.Joint.get()) && Made.getCount() == 1, "Two hemp leaves and a paper made " + Made);
        List<ItemStack> Short = new ArrayList<>(List.of(Leaf.copy(), new ItemStack(Items.PAPER), ItemStack.EMPTY, ItemStack.EMPTY));
        Optional<RecipeHolder<CraftingRecipe>> Missing = World.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(2, 2, Short), World);
        Helper.assertTrue(Missing.isEmpty() || !Missing.get().value().getResultItem(World.registryAccess()).is(Scp420jRegistry.Joint.get()), "One leaf and a paper already make a joint");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp420j_arrival")
    public static void strangerBringsSeeds(GameTestHelper Helper) {
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp420jRegistry.DocumentId);
        Helper.assertTrue(Document != null, "The SCP-420-J dossier did not load");
        Helper.assertTrue(Document.designation().getString().equals("SCP-420-J"), "The dossier designation is " + Document.designation().getString());
        Helper.assertTrue(Document.Condition() instanceof StrangerEncounterCondition, "SCP-420-J does not arrive by stranger");
        List<StrangerGift> Gifts = ((StrangerEncounterCondition) Document.Condition()).plan().Gifts();
        Helper.assertTrue(!Gifts.isEmpty(), "The stranger brings nothing");
        int Seeds = 0;
        for (StrangerGift Gift : Gifts) {
            ItemStack Stack = Gift.toStack();
            Helper.assertTrue(Stack.is(Scp420jRegistry.Seeds.get()), "The stranger gift includes " + Stack);
            Seeds += Stack.getCount();
        }
        Helper.assertTrue(Seeds == 4, "The stranger brings " + Seeds + " seeds instead of 4");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 760, batch = "scp420j_float")
    public static void jointFloatsAndLandsSafely(GameTestHelper Helper) {
        floor(Helper, 1);
        Subject User = subject(Helper, 5.5, 2.0, 5.5);
        joints(User, 2);
        double Start = User.getY();
        light(User);
        Helper.assertTrue(User.getMainHandItem().getCount() == 1, "The joint was not used up");
        Scp420jFloat.State Begun = Scp420jFloat.stateOf(User);
        Helper.assertTrue(Begun != null && !Begun.Descending() && Math.abs(Begun.BaseY() - Start) < 1.0e-6, "Lighting the joint did not start a float");
        Helper.assertTrue(lowGravity(User), "The float did not lift gravity");
        MobEffectInstance Pun = User.getEffect(MobEffects.SATURATION);
        Helper.assertTrue(Pun != null && Pun.getDuration() == Scp420jFloat.TrackTicks, "The Saturation pun effect is missing or has the wrong length");
        drive(Helper, User);
        float Health = User.getHealth();
        AtomicReference<Double> Peak = new AtomicReference<>(Start);
        Helper.onEachTick(() -> Peak.set(Math.max(Peak.get(), User.getY())));
        Helper.startSequence().thenExecuteAfter(Scp420jFloat.RiseTicks + 20, () -> {
            double Lift = User.getY() - Start;
            Helper.assertTrue(Lift >= 6.0 && Lift <= 8.0, "The floater rose " + Lift + " blocks instead of about 6 to 8");
            Helper.assertTrue(Scp420jFloat.isFloating(User), "The float ended early");
            User.hurt(User.damageSources().generic(), 2.0f);
            Helper.assertTrue(Scp420jFloat.isFloating(User), "Taking damage interrupted the float");
        }).thenExecuteAfter(Scp420jFloat.TrackTicks - Scp420jFloat.RiseTicks - 17, () -> {
            Scp420jFloat.State Late = Scp420jFloat.stateOf(User);
            Helper.assertTrue(Late != null && Late.Descending(), "The float did not end with the track (" + Scp420jFloat.TrackTicks + " ticks)");
            Helper.assertTrue(!lowGravity(User), "Gravity did not come back when the song ended");
            Helper.assertTrue(User.getY() - Start > 4.0, "The floater dropped before the song ended");
        }).thenWaitUntil(() -> Helper.assertTrue(Scp420jFloat.stateOf(User) == null, "The floater has not landed yet")).thenExecute(() -> {
            Helper.assertTrue(User.onGround() && Math.abs(User.getY() - Start) < 0.01, "The floater did not land back on the floor: " + User.getY() + " vs " + Start);
            Helper.assertTrue(User.getHealth() >= Health - 2.0f - 1.0e-4f, "The landing hurt: health " + User.getHealth() + " from " + Health);
            Helper.assertTrue(Peak.get() - Start <= 8.0, "The float peaked " + (Peak.get() - Start) + " blocks up");
            Helper.assertTrue(User.causeFallDamage(12.0f, 1.0f, User.damageSources().fall()), "The test subject cannot take fall damage at all");
            dismiss(Helper, User);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp420j_cushion")
    public static void floatersTakeNoFallDamage(GameTestHelper Helper) {
        floor(Helper, 1);
        Subject User = subject(Helper, 5.5, 2.0, 5.5);
        joints(User, 1);
        light(User);
        float Health = User.getHealth();
        Helper.assertTrue(!User.causeFallDamage(20.0f, 1.0f, User.damageSources().fall()), "A floating player took fall damage");
        Helper.assertTrue(User.getHealth() == Health, "A floating player lost health to a fall");
        Scp420jFloat.forget(User);
        drive(Helper, User);
        Helper.startSequence().thenExecuteAfter(2, () -> {
            Scp420jFloat.State Resumed = Scp420jFloat.stateOf(User);
            Helper.assertTrue(Resumed != null && Resumed.Descending(), "A float the client never heard about (a login) did not switch to a safe descent");
            Helper.assertTrue(!lowGravity(User), "The safe descent kept low gravity");
            Helper.assertTrue(!User.causeFallDamage(20.0f, 1.0f, User.damageSources().fall()), "A descending player took fall damage");
        }).thenWaitUntil(() -> Helper.assertTrue(Scp420jFloat.stateOf(User) == null, "The descent did not finish")).thenExecute(() -> {
            Helper.assertTrue(User.getHealth() == Health, "The safe descent hurt the player");
            dismiss(Helper, User);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 700, batch = "scp420j_restack")
    public static void secondJointRestartsWithoutStacking(GameTestHelper Helper) {
        floor(Helper, 1);
        Subject User = subject(Helper, 5.5, 2.0, 5.5);
        joints(User, 3);
        double Start = User.getY();
        light(User);
        drive(Helper, User);
        AtomicReference<Scp420jFloat.State> First = new AtomicReference<>();
        AtomicReference<Double> Peak = new AtomicReference<>(Start);
        Helper.onEachTick(() -> Peak.set(Math.max(Peak.get(), User.getY())));
        Helper.startSequence().thenExecuteAfter(150, () -> {
            Scp420jFloat.State Before = Scp420jFloat.stateOf(User);
            Helper.assertTrue(Before != null && !Before.Spoken().isEmpty(), "No chat line was said in the first 150 ticks");
            First.set(Before);
            light(User);
            Scp420jFloat.State After = Scp420jFloat.stateOf(User);
            Helper.assertTrue(User.getMainHandItem().getCount() == 1, "The second joint was not used up");
            Helper.assertTrue(After != null && After.Session() != Before.Session() && After.Elapsed() == 0, "The second joint did not restart the timer");
            Helper.assertTrue(After.BaseY() == Before.BaseY(), "The second joint moved the float base, so heights would stack");
            Helper.assertTrue(After.Spoken().isEmpty(), "The second joint did not reset the chat schedule");
            MobEffectInstance Pun = User.getEffect(MobEffects.SATURATION);
            Helper.assertTrue(Pun != null && Pun.getDuration() == Scp420jFloat.TrackTicks, "The Saturation effect was not refreshed cleanly");
        }).thenExecuteAfter(Scp420jFloat.TrackTicks - 5, () -> {
            Scp420jFloat.State Late = Scp420jFloat.stateOf(User);
            Helper.assertTrue(Late != null && !Late.Descending(), "The restarted float ended early, so the old timer leaked through");
            Helper.assertTrue(Peak.get() - Start <= 8.0, "The second joint stacked the height to " + (Peak.get() - Start));
            List<Integer> Planned = Scp420jChatter.plan(Late.Session()).Lines();
            Helper.assertTrue(Late.Spoken().equals(Planned), "The restarted float said " + Late.Spoken() + " instead of its own plan " + Planned);
        }).thenExecuteAfter(10, () -> {
            Scp420jFloat.State Done = Scp420jFloat.stateOf(User);
            Helper.assertTrue(Done != null && Done.Descending(), "The restarted float did not end on its own timer");
            Scp420jFloat.land(User);
            dismiss(Helper, User);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 520, batch = "scp420j_chatter")
    public static void floatSaysTwoToFourDifferentLines(GameTestHelper Helper) {
        for (int Session = 1; Session <= 4000; Session++) {
            Scp420jChatter.Plan Plan = Scp420jChatter.plan(Session * 7919);
            int Count = Plan.Lines().size();
            Helper.assertTrue(Count >= 2 && Count <= 4, "A chat plan has " + Count + " lines");
            Helper.assertTrue(new HashSet<>(Plan.Lines()).size() == Count, "A chat plan repeats a line: " + Plan.Lines());
            Helper.assertTrue(Plan.Lines().stream().allMatch(Line -> Line >= 0 && Line < Scp420jChatter.LineCount), "A chat plan uses a missing line: " + Plan.Lines());
            Helper.assertTrue(Plan.Times().get(0) >= Scp420jChatter.FirstEarliest && Plan.Times().get(0) <= Scp420jChatter.FirstLatest, "The first line comes at " + Plan.Times().get(0));
            for (int Index = 1; Index < Count; Index++) {
                Helper.assertTrue(Plan.Times().get(Index) - Plan.Times().get(Index - 1) >= Scp420jChatter.MinGap, "Two lines come too close: " + Plan.Times());
            }
            Helper.assertTrue(Plan.Times().get(Count - 1) < Scp420jFloat.TrackTicks, "A line comes after the song: " + Plan.Times());
        }
        floor(Helper, 1);
        Subject User = subject(Helper, 5.5, 2.0, 5.5);
        joints(User, 1);
        light(User);
        drive(Helper, User);
        Helper.startSequence().thenExecuteAfter(Scp420jFloat.TrackTicks + 2, () -> {
            Scp420jFloat.State Late = Scp420jFloat.stateOf(User);
            Helper.assertTrue(Late != null && Late.Descending(), "The float did not end with the song");
            List<Integer> Spoken = Late.Spoken();
            Set<Integer> Distinct = new HashSet<>(Spoken);
            Helper.assertTrue(Spoken.size() >= 2 && Spoken.size() <= 4, "The float said " + Spoken.size() + " lines");
            Helper.assertTrue(Distinct.size() == Spoken.size(), "The float repeated a line: " + Spoken);
            Helper.assertTrue(Spoken.equals(Scp420jChatter.plan(Late.Session()).Lines()), "The float did not follow its chat plan");
            String Said = Scp420jChatter.line(User, Spoken.get(0)).getString();
            Helper.assertTrue(Said.contains(User.getGameProfile().getName()), "The chat line does not carry the floater's name: " + Said);
            Scp420jFloat.land(User);
            dismiss(Helper, User);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp420j_ceiling")
    public static void ceilingStopsTheRise(GameTestHelper Helper) {
        floor(Helper, 1);
        for (int X = 3; X <= 8; X++) {
            for (int Z = 3; Z <= 8; Z++) {
                Helper.setBlock(new BlockPos(X, 6, Z), Blocks.STONE.defaultBlockState());
            }
        }
        Subject User = subject(Helper, 5.5, 2.0, 5.5);
        joints(User, 1);
        double Start = User.getY();
        double Ceiling = Helper.absoluteVec(new Vec3(0.0, 6.0, 0.0)).y;
        light(User);
        drive(Helper, User);
        Helper.startSequence().thenExecuteAfter(Scp420jFloat.RiseTicks + 30, () -> {
            Helper.assertTrue(User.getY() > Start + 2.0, "The floater did not rise under the ceiling");
            Helper.assertTrue(User.getY() + User.getBbHeight() <= Ceiling + 1.0e-4, "The floater passed through the ceiling");
            Helper.assertTrue(Scp420jFloat.isFloating(User), "Hitting the ceiling ended the float");
            Scp420jFloat.land(User);
            dismiss(Helper, User);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp420j_dclass")
    public static void controlledDClassSpeaksUnderItsOwnName(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        floor(Helper, 1);
        Subject User = subject(Helper, 2.5, 2.0, 2.5);
        DClass Body = DClassRegistry.DClassType.get().create(World);
        Vec3 BodySpot = Helper.absoluteVec(new Vec3(5.5, 2.0, 5.4));
        Body.moveTo(BodySpot.x, BodySpot.y, BodySpot.z, 180.0f, 0.0f);
        World.addFreshEntity(Body);
        Helper.assertTrue(ControlManager.possess(User, Body), "The test controller could not possess the D-Class");
        String BodyName = ControlSessions.session(User).Identity().getString("ControlName");
        Helper.assertTrue(!BodyName.isBlank() && Scp420jChatter.speakerName(User).getString().equals(BodyName), "Chat lines would not use the D-Class name");
        joints(User, 1);
        light(User);
        Helper.assertTrue(Scp420jFloat.isFloating(User), "A controlled D-Class could not use the joint");
        Helper.assertTrue(Scp420jChatter.line(User, 0).getString().contains(BodyName), "The chat line does not use the D-Class name");
        Scp420jFloat.land(User);
        ControlManager.release(User);
        dismiss(Helper, User);
        Helper.succeed();
    }
}
