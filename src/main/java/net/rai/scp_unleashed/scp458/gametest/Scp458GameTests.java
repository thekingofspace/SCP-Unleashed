package net.rai.scp_unleashed.scp458.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.scp458.Scp458Box;
import net.rai.scp_unleashed.scp458.Scp458BoxBlock;
import net.rai.scp_unleashed.scp458.Scp458BoxBlockEntity;
import net.rai.scp_unleashed.scp458.Scp458BoxItem;
import net.rai.scp_unleashed.scp458.Scp458Contents;
import net.rai.scp_unleashed.scp458.Scp458Favourites;
import net.rai.scp_unleashed.scp458.Scp458Lid;
import net.rai.scp_unleashed.scp458.Scp458Pizza;
import net.rai.scp_unleashed.scp458.Scp458Registry;
import net.rai.scp_unleashed.scp458.Scp458SliceItem;
import net.rai.scp_unleashed.stranger.StrangerEncounterCondition;
import net.rai.scp_unleashed.stranger.StrangerGift;
import net.rai.scp_unleashed.stranger.StrangerPlan;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp458GameTests {
    public static final String Arena = "scp458_arena";
    public static final BlockPos Spot = new BlockPos(8, 2, 8);
    public static final int Rested = Scp458Box.LidRest + 2;
    public static final int FrontSlice = 4;

    private Scp458GameTests() {
    }

    private static Scp458TestPlayer player(GameTestHelper Helper, double X, double Z, String Name) {
        ServerLevel World = Helper.getLevel();
        Scp458TestPlayer User = new Scp458TestPlayer(World, Name);
        Vec3 Where = Helper.absoluteVec(new Vec3(X, 2.0, Z));
        User.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(GameType.SURVIVAL);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, ServerPlayer User) {
        if (!User.isRemoved()) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    private static Scp458BoxBlockEntity place(GameTestHelper Helper, BlockPos Relative, Direction Facing) {
        Helper.setBlock(Relative, Scp458Registry.Box.get().defaultBlockState().setValue(Scp458BoxBlock.Facing, Facing));
        if (Helper.getLevel().getBlockEntity(Helper.absolutePos(Relative)) instanceof Scp458BoxBlockEntity Carton) {
            return Carton;
        }
        throw new IllegalStateException("The box at " + Relative + " has no block entity");
    }

    private static Scp458BoxBlockEntity place(GameTestHelper Helper) {
        return place(Helper, Spot, Direction.NORTH);
    }

    private static Vec3 pizzaPoint(BlockPos Absolute) {
        return Vec3.atLowerCornerOf(Absolute).add(0.5, 2.5 / 16.0, 0.45);
    }

    private static InteractionResult click(ServerPlayer User, BlockPos Absolute, Vec3 Hit, boolean Sneaking) {
        User.setShiftKeyDown(Sneaking);
        InteractionResult Result = User.gameMode.useItemOn(User, User.serverLevel(), User.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Hit, Direction.UP, Absolute, false));
        User.setShiftKeyDown(false);
        return Result;
    }

    private static InteractionResult click(ServerPlayer User, BlockPos Absolute) {
        return click(User, Absolute, pizzaPoint(Absolute), false);
    }

    private static InteractionResult shut(ServerPlayer User, BlockPos Absolute) {
        return click(User, Absolute, pizzaPoint(Absolute), true);
    }

    private static boolean open(GameTestHelper Helper, BlockPos Relative) {
        return Scp458BoxBlock.isOpen(Helper.getBlockState(Relative));
    }

    private static int slices(Player User, Scp458Pizza Wanted) {
        int Count = 0;
        for (int Slot = 0; Slot < User.getInventory().getContainerSize(); Slot++) {
            ItemStack Stack = User.getInventory().getItem(Slot);
            if (Stack.getItem() instanceof Scp458SliceItem && (Wanted == null || Scp458SliceItem.pizza(Stack) == Wanted)) {
                Count += Stack.getCount();
            }
        }
        return Count;
    }

    private static int slices(Player User) {
        return slices(User, null);
    }

    private static boolean exists(String Path) {
        try (InputStream Stream = Scp458GameTests.class.getResourceAsStream(Path)) {
            return Stream != null;
        } catch (Exception Problem) {
            return false;
        }
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp458_lidOpensAndCloses")
    public static void lidOpensAndCloses(GameTestHelper Helper) {
        Scp458BoxBlockEntity Carton = place(Helper);
        Scp458TestPlayer User = player(Helper, 8.5, 5.5, "scp458_lid");
        BlockPos At = Helper.absolutePos(Spot);
        try {
            Helper.assertFalse(open(Helper, Spot), "A new box starts with its lid up");
            Helper.assertTrue(click(User, At).consumesAction(), "Opening the box did not consume the click");
            Helper.assertTrue(open(Helper, Spot), "The click did not raise the lid");
            Helper.assertTrue(Carton.contents().Open() && Carton.contents().left() == Scp458Contents.Slices, "The opened box holds " + Carton.contents());
            Helper.assertTrue(shut(User, At).consumesAction(), "Closing the box did not consume the click");
            Helper.assertFalse(open(Helper, Spot), "Sneak-clicking did not lower the lid");
            Helper.assertFalse(Carton.contents().Open(), "The contents still think the lid is up");
            Helper.assertTrue(Scp458Lid.OpenAngle >= 100.0f && Scp458Lid.OpenAngle <= 110.0f, "The lid rests at " + Scp458Lid.OpenAngle + " degrees");
            Helper.assertTrue(Math.abs(Scp458Lid.angle(true, 0.0f, 0.0f)) < 0.001f, "The lid does not start its swing from shut");
            Helper.assertTrue(Math.abs(Scp458Lid.angle(true, 0.0f, Scp458Lid.OpenTicks) - Scp458Lid.OpenAngle) < 0.01f, "The lid does not settle at its open angle");
            float Peak = 0.0f;
            float Previous = 0.0f;
            float Jump = 0.0f;
            for (float Tick = 0.0f; Tick <= Scp458Lid.OpenTicks; Tick += 0.25f) {
                float Now = Scp458Lid.angle(true, 0.0f, Tick);
                Peak = Math.max(Peak, Now);
                Jump = Math.max(Jump, Math.abs(Now - Previous));
                Previous = Now;
            }
            Helper.assertTrue(Peak > Scp458Lid.OpenAngle + 2.0f && Peak < Scp458Lid.OpenAngle + 15.0f, "The swing peaks at " + Peak + " instead of a small overshoot");
            Helper.assertTrue(Jump < 12.0f, "The opening swing jumps " + Jump + " degrees in a quarter tick");
            float Lowest = Float.MAX_VALUE;
            Previous = Scp458Lid.OpenAngle;
            Jump = 0.0f;
            for (float Tick = 0.0f; Tick <= Scp458Lid.CloseTicks; Tick += 0.25f) {
                float Now = Scp458Lid.angle(false, Scp458Lid.OpenAngle, Tick);
                Lowest = Math.min(Lowest, Now);
                Jump = Math.max(Jump, Math.abs(Now - Previous));
                Previous = Now;
            }
            Helper.assertTrue(Lowest > -0.001f, "The closing lid dips into the box to " + Lowest);
            Helper.assertTrue(Jump < 12.0f, "The closing swing jumps " + Jump + " degrees in a quarter tick");
            Helper.assertTrue(Math.abs(Scp458Lid.angle(false, Scp458Lid.OpenAngle, Scp458Lid.CloseTicks)) < 0.01f, "The lid does not end shut");
        } finally {
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp458_firstOpenAssignsFavourite")
    public static void firstOpenAssignsFavourite(GameTestHelper Helper) {
        Scp458BoxBlockEntity Carton = place(Helper);
        Scp458TestPlayer User = player(Helper, 8.5, 5.5, "scp458_first");
        BlockPos At = Helper.absolutePos(Spot);
        Helper.assertTrue(Scp458Favourites.of(User) == null, "A new player already has a pizza on record");
        click(User, At);
        Scp458Pizza First = Scp458Favourites.of(User);
        Helper.assertTrue(First != null, "The first opening did not record a pizza");
        Helper.assertTrue(Carton.contents().Pizza().equals(Optional.of(First)), "The box holds " + Carton.contents().Pizza() + " instead of " + First);
        shut(User, At);
        Runnable Again = () -> {
            Helper.assertTrue(click(User, At).consumesAction() && open(Helper, Spot), "The box would not open again");
            Helper.assertTrue(Carton.contents().Pizza().equals(Optional.of(First)), "A later opening held " + Carton.contents().Pizza() + " instead of " + First);
            Helper.assertTrue(Scp458Favourites.of(User) == First, "The recorded pizza changed to " + Scp458Favourites.of(User));
            shut(User, At);
        };
        Helper.startSequence().thenIdle(Rested).thenExecute(Again).thenIdle(Rested).thenExecute(Again).thenIdle(Rested).thenExecute(Again)
                .thenExecute(() -> dismiss(Helper, User)).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp458_sixSlicesThenEmpty")
    public static void sixSlicesThenEmpty(GameTestHelper Helper) {
        Scp458BoxBlockEntity Carton = place(Helper);
        Scp458TestPlayer User = player(Helper, 8.5, 5.5, "scp458_hungry");
        BlockPos At = Helper.absolutePos(Spot);
        try {
            Scp458Favourites.set(User, Scp458Pizza.Hawaiian);
            click(User, At);
            for (int Take = 1; Take <= Scp458Contents.Slices; Take++) {
                Helper.assertTrue(click(User, At).consumesAction(), "Slice " + Take + " did not consume the click");
                Helper.assertTrue(slices(User) == Take, "After " + Take + " takes the player holds " + slices(User) + " slices");
                Helper.assertTrue(Carton.contents().left() == Scp458Contents.Slices - Take, "After " + Take + " takes the box has " + Carton.contents().left() + " left");
            }
            Helper.assertTrue(slices(User, Scp458Pizza.Hawaiian) == Scp458Contents.Slices, "Not every slice was Hawaiian");
            Helper.assertTrue(Carton.contents().empty() && Carton.contents().Taken() == Scp458Contents.Whole, "The box is not empty: " + Carton.contents());
            Helper.assertTrue(open(Helper, Spot), "Taking the last slice closed the lid");
            click(User, At);
            Helper.assertTrue(slices(User) == Scp458Contents.Slices, "An empty box gave a seventh slice");
            Helper.assertFalse(open(Helper, Spot), "Clicking the empty box did not close it");
            ItemStack Held = ItemStack.EMPTY;
            for (int Slot = 0; Slot < User.getInventory().getContainerSize(); Slot++) {
                if (User.getInventory().getItem(Slot).getItem() instanceof Scp458SliceItem) {
                    Held = User.getInventory().getItem(Slot);
                }
            }
            Helper.assertTrue(Held.getHoverName().getString().equals("Slice of Hawaiian Pizza"), "The slice is called " + Held.getHoverName().getString());
            Helper.assertTrue(Held.getFoodProperties(User) != null && Held.getFoodProperties(User).nutrition() == Scp458Registry.SliceNutrition, "The slice is not food");
        } finally {
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp458_guestGetsOpenersPizza")
    public static void guestGetsOpenersPizza(GameTestHelper Helper) {
        Scp458BoxBlockEntity Carton = place(Helper);
        Scp458TestPlayer Opener = player(Helper, 8.5, 5.5, "scp458_opener");
        Scp458TestPlayer Guest = player(Helper, 10.5, 9.5, "scp458_guest");
        BlockPos At = Helper.absolutePos(Spot);
        try {
            Scp458Favourites.set(Opener, Scp458Pizza.Hawaiian);
            Scp458Favourites.set(Guest, Scp458Pizza.Pepperoni);
            click(Opener, At);
            Helper.assertTrue(click(Guest, At).consumesAction(), "The guest could not take a slice");
            Helper.assertTrue(slices(Guest, Scp458Pizza.Hawaiian) == 1, "The guest did not get a slice of the opener's Hawaiian");
            Helper.assertTrue(slices(Guest, Scp458Pizza.Pepperoni) == 0, "The guest got their own pepperoni from someone else's box");
            Helper.assertTrue(Scp458Favourites.of(Guest) == Scp458Pizza.Pepperoni, "Taking a slice changed the guest's own pizza");
            Helper.assertTrue(Carton.contents().left() == Scp458Contents.Slices - 1, "The guest's slice did not leave the box");
            click(Opener, At);
            Helper.assertTrue(slices(Opener, Scp458Pizza.Hawaiian) == 1 && Carton.contents().left() == Scp458Contents.Slices - 2, "The opener could not share the same pizza");
        } finally {
            dismiss(Helper, Opener);
            dismiss(Helper, Guest);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp458_refillsAfterReopen")
    public static void refillsAfterReopen(GameTestHelper Helper) {
        Scp458BoxBlockEntity Carton = place(Helper);
        Scp458TestPlayer User = player(Helper, 8.5, 5.5, "scp458_refill");
        BlockPos At = Helper.absolutePos(Spot);
        click(User, At);
        for (int Take = 0; Take < 4; Take++) {
            click(User, At);
        }
        Helper.assertTrue(Carton.contents().left() == 2, "Four takes left " + Carton.contents().left());
        shut(User, At);
        Helper.assertFalse(open(Helper, Spot), "The lid did not close");
        Helper.startSequence().thenIdle(Rested).thenExecute(() -> {
            click(User, At);
            Helper.assertTrue(open(Helper, Spot), "The lid would not open again");
            Helper.assertTrue(Carton.contents().left() == Scp458Contents.Slices && Carton.contents().Taken() == 0, "The pizza was not whole again: " + Carton.contents());
            for (int Take = 0; Take < Scp458Contents.Slices; Take++) {
                click(User, At);
            }
            Helper.assertTrue(slices(User) == 4 + Scp458Contents.Slices, "The refilled pizza gave " + (slices(User) - 4) + " slices");
            dismiss(Helper, User);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp458_favouriteSurvivesDeath")
    public static void favouriteSurvivesDeath(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp458BoxBlockEntity Carton = place(Helper);
        Scp458TestPlayer Before = player(Helper, 8.5, 5.5, "scp458_before");
        BlockPos At = Helper.absolutePos(Spot);
        try {
            click(Before, At);
            Scp458Pizza Chosen = Scp458Favourites.of(Before);
            Helper.assertTrue(Chosen != null && Carton.contents().Pizza().equals(Optional.of(Chosen)), "The opening did not record a pizza");
            Scp458TestPlayer Respawned = new Scp458TestPlayer(World, "scp458_respawned");
            Respawned.restoreFrom(Before, false);
            Helper.assertTrue(Scp458Favourites.of(Respawned) == Chosen, "The pizza did not survive death: " + Scp458Favourites.of(Respawned));
            CompoundTag Saved = new CompoundTag();
            Before.saveWithoutId(Saved);
            Scp458TestPlayer Returned = new Scp458TestPlayer(World, "scp458_returned");
            Returned.load(Saved);
            Helper.assertTrue(Scp458Favourites.of(Returned) == Chosen, "The pizza did not survive a save and reload");
            Before.setData(Scp458Registry.BodyFavourite.get(), Chosen == Scp458Pizza.Cheese ? Scp458Pizza.Pesto : Scp458Pizza.Cheese);
            Scp458TestPlayer Again = new Scp458TestPlayer(World, "scp458_again");
            Again.restoreFrom(Before, false);
            Helper.assertFalse(Again.hasData(Scp458Registry.BodyFavourite.get()), "A body's pizza was carried through the player's death");
        } finally {
            dismiss(Helper, Before);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp458_playersStableAndVaried")
    public static void playersStableAndVaried(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Set<Scp458Pizza> Seen = EnumSet.noneOf(Scp458Pizza.class);
        List<Scp458TestPlayer> Crowd = new ArrayList<>();
        for (int Index = 0; Index < 16; Index++) {
            Scp458TestPlayer Person = new Scp458TestPlayer(World, "scp458_crowd_" + Index);
            Crowd.add(Person);
            Scp458Box.Step First = Scp458Box.act(World, Person, Scp458Contents.Sealed, false, Scp458Box.FrontAngle);
            Helper.assertTrue(First.Result() == Scp458Box.Outcome.Opened && First.After().Pizza().isPresent(), "Person " + Index + " could not open the box");
            Scp458Pizza Pick = First.After().Pizza().get();
            for (int Repeat = 0; Repeat < 3; Repeat++) {
                Scp458Box.Step Later = Scp458Box.act(World, Person, Scp458Contents.Sealed, false, Scp458Box.FrontAngle);
                Helper.assertTrue(Later.After().Pizza().equals(Optional.of(Pick)), "Person " + Index + " got " + Later.After().Pizza() + " after " + Pick);
            }
            Seen.add(Pick);
        }
        Helper.assertTrue(Seen.size() >= 2, "Sixteen people all got the same pizza: " + Seen);
        Set<UUID> Ids = new HashSet<>();
        for (Scp458TestPlayer Person : Crowd) {
            Ids.add(Person.getUUID());
        }
        Helper.assertTrue(Ids.size() == Crowd.size(), "The crowd shares an identity");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp458_variantsComplete")
    public static void variantsComplete(GameTestHelper Helper) {
        Scp458Pizza[] All = Scp458Pizza.values();
        Helper.assertTrue(All.length == 16, "There are " + All.length + " pizzas instead of 16");
        Helper.assertTrue(Scp458Pizza.byId("hawaiian") == Scp458Pizza.Hawaiian, "Hawaiian is missing");
        Language Words = Language.getInstance();
        Set<String> Ids = new HashSet<>();
        Set<String> Names = new HashSet<>();
        for (Scp458Pizza Each : All) {
            Helper.assertTrue(Ids.add(Each.id()), "The id " + Each.id() + " is used twice");
            Helper.assertTrue(Scp458Pizza.byId(Each.id()) == Each, "The id " + Each.id() + " does not find its pizza");
            Tag Encoded = Scp458Pizza.PizzaCodec.encodeStart(NbtOps.INSTANCE, Each).getOrThrow();
            Helper.assertTrue(Scp458Pizza.PizzaCodec.parse(NbtOps.INSTANCE, Encoded).getOrThrow() == Each, Each.id() + " does not survive saving");
            Helper.assertTrue(Words.has(Each.nameKey()), "No name for " + Each.id());
            String Name = Words.getOrDefault(Each.nameKey());
            Helper.assertTrue(Names.add(Name), "The name " + Name + " is used twice");
            ItemStack Slice = Scp458Box.slice(Each);
            Helper.assertTrue(Scp458SliceItem.pizza(Slice) == Each, "The slice forgot it was " + Each.id());
            Helper.assertTrue(Slice.getHoverName().getString().equals("Slice of " + Name), "The " + Each.id() + " slice is called " + Slice.getHoverName().getString());
            Helper.assertTrue(exists("/assets/scp_unleashed/textures/item/scp458/slice_" + Each.id() + ".png"), "No slice icon for " + Each.id());
            Helper.assertTrue(exists("/assets/scp_unleashed/models/item/scp458/slice_" + Each.id() + ".json"), "No slice item model for " + Each.id());
            Helper.assertTrue(exists("/assets/scp_unleashed/textures/block/scp458/pizza_" + Each.id() + ".png"), "No pizza texture for " + Each.id());
            for (int Piece = 0; Piece < Scp458Contents.Slices; Piece++) {
                Helper.assertTrue(exists("/assets/scp_unleashed/models/block/scp458/pizza/" + Each.id() + "_" + Piece + ".json"), "No model for slice " + Piece + " of " + Each.id());
            }
        }
        Helper.assertTrue(Words.getOrDefault("item.scp_unleashed.scp458_slice.named").contains("%s"), "The slice name has no place for the pizza");
        try (InputStream Stream = Scp458GameTests.class.getResourceAsStream("/assets/scp_unleashed/models/item/scp458_slice.json")) {
            Helper.assertTrue(Stream != null, "The slice item model is missing");
            JsonObject Parsed = JsonParser.parseReader(new InputStreamReader(Stream, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray Overrides = Parsed.getAsJsonArray("overrides");
            Helper.assertTrue(Overrides != null && Overrides.size() == All.length - 1, "The slice model has " + (Overrides == null ? 0 : Overrides.size()) + " overrides");
            Set<String> Targets = new HashSet<>();
            for (JsonElement Entry : Overrides) {
                JsonObject Rule = Entry.getAsJsonObject();
                float Value = Rule.getAsJsonObject("predicate").get("scp_unleashed:scp458_pizza").getAsFloat();
                String Target = Rule.get("model").getAsString();
                Scp458Pizza Expected = All[Math.round(Value)];
                Helper.assertTrue(Target.equals("scp_unleashed:item/scp458/slice_" + Expected.id()), "Override " + Value + " points at " + Target);
                Targets.add(Target);
            }
            Helper.assertTrue(Targets.size() == All.length - 1, "Overrides repeat a model");
        } catch (Exception Problem) {
            throw new IllegalStateException("Could not read the slice item model", Problem);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp458_npcGetsOwnPizza")
    public static void npcGetsOwnPizza(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp458BoxBlockEntity Carton = place(Helper);
        BlockPos At = Helper.absolutePos(Spot);
        DClass Body = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new BlockPos(8, 2, 5));
        Scp458Box.Step Done = Scp458BoxBlock.use(World, At, Body, false);
        Helper.assertTrue(Done.Result() == Scp458Box.Outcome.Opened && open(Helper, Spot), "The D-Class could not open the box: " + Done.Result());
        Scp458Pizza Own = Body.getExistingDataOrNull(Scp458Registry.BodyFavourite.get());
        Helper.assertTrue(Own != null && Carton.contents().Pizza().equals(Optional.of(Own)), "The D-Class has no pizza of its own on record");
        Helper.assertFalse(Body.hasData(Scp458Registry.Favourite.get()), "The D-Class pizza went into the player slot");
        Scp458Box.Step Took = Scp458BoxBlock.use(World, At, Body, false);
        Helper.assertTrue(Took.Result() == Scp458Box.Outcome.Took, "The D-Class could not take a slice");
        boolean Dropped = false;
        for (ItemEntity Found : World.getEntitiesOfClass(ItemEntity.class, Body.getBoundingBox().inflate(3.0))) {
            if (Found.getItem().getItem() instanceof Scp458SliceItem && Scp458SliceItem.pizza(Found.getItem()) == Own) {
                Dropped = true;
                Found.discard();
            }
        }
        Helper.assertTrue(Dropped, "The D-Class slice did not appear");
        Scp458BoxBlock.use(World, At, Body, true);
        Helper.startSequence().thenIdle(Rested).thenExecute(() -> {
            Scp458BoxBlock.use(World, At, Body, false);
            Helper.assertTrue(Carton.contents().Pizza().equals(Optional.of(Own)), "The D-Class got " + Carton.contents().Pizza() + " the second time");
            Body.discard();
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp458_controlledBodyKeepsItsOwn")
    public static void controlledBodyKeepsItsOwn(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp458BoxBlockEntity Carton = place(Helper);
        BlockPos At = Helper.absolutePos(Spot);
        Scp458TestPlayer User = player(Helper, 8.5, 5.5, "scp458_controller");
        User.enlist(true);
        try {
            Scp458Favourites.set(User, Scp458Pizza.Hawaiian);
            DClass Body = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new BlockPos(8, 2, 5));
            UUID BodyId = Body.getUUID();
            Body.setData(Scp458Registry.BodyFavourite.get(), Scp458Pizza.Mushroom);
            Helper.assertTrue(ControlManager.possess(User, Body), "The test player could not take control of the D-Class");
            Helper.assertTrue(ControlSessions.isControlling(User), "No control session after possessing");
            click(User, At);
            Helper.assertTrue(Carton.contents().Pizza().equals(Optional.of(Scp458Pizza.Mushroom)), "The controlled body got " + Carton.contents().Pizza() + " instead of its own mushroom");
            Helper.assertTrue(User.getExistingDataOrNull(Scp458Registry.Favourite.get()) == Scp458Pizza.Hawaiian, "Control changed the player's own pizza");
            ControlManager.release(User);
            Helper.assertFalse(ControlSessions.isControlling(User), "Release did not end the session");
            Helper.assertTrue(Scp458Favourites.of(User) == Scp458Pizza.Hawaiian, "The player came home with " + Scp458Favourites.of(User));
            Entity Found = World.getEntity(BodyId);
            Helper.assertTrue(Found instanceof LivingEntity Released && Released.getExistingDataOrNull(Scp458Registry.BodyFavourite.get()) == Scp458Pizza.Mushroom,
                    "The released body lost its own pizza");
            if (Found != null) {
                Found.discard();
            }
        } finally {
            User.enlist(false);
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp458_heldBoxOpensInHand")
    public static void heldBoxOpensInHand(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp458TestPlayer User = player(Helper, 8.5, 8.5, "scp458_carrier");
        Scp458Favourites.set(User, Scp458Pizza.Margherita);
        User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Scp458Registry.BoxItem.get()));
        User.getMainHandItem().use(World, User, InteractionHand.MAIN_HAND);
        Scp458Contents Held = Scp458BoxItem.contents(User.getMainHandItem());
        Helper.assertTrue(Held.Open() && Held.Pizza().equals(Optional.of(Scp458Pizza.Margherita)) && Held.left() == Scp458Contents.Slices, "The held box did not open: " + Held);
        User.getMainHandItem().use(World, User, InteractionHand.MAIN_HAND);
        Helper.assertTrue(slices(User, Scp458Pizza.Margherita) == 1, "The held box did not give a slice");
        Helper.assertTrue(Scp458BoxItem.contents(User.getMainHandItem()).left() == Scp458Contents.Slices - 1, "The held box did not lose a slice");
        User.setShiftKeyDown(true);
        User.getMainHandItem().use(World, User, InteractionHand.MAIN_HAND);
        User.setShiftKeyDown(false);
        Helper.assertFalse(Scp458BoxItem.contents(User.getMainHandItem()).Open(), "Sneak-using the held box did not close it");
        Helper.startSequence().thenIdle(Rested).thenExecute(() -> {
            User.getMainHandItem().use(World, User, InteractionHand.MAIN_HAND);
            User.getMainHandItem().use(World, User, InteractionHand.MAIN_HAND);
            Scp458Contents Carried = Scp458BoxItem.contents(User.getMainHandItem());
            Helper.assertTrue(Carried.Open() && Carried.left() == Scp458Contents.Slices - 1, "The held box did not refill and give a slice: " + Carried);
            BlockPos Floor = Helper.absolutePos(new BlockPos(11, 1, 8));
            InteractionResult Placed = User.gameMode.useItemOn(User, World, User.getMainHandItem(), InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(Floor).add(0.0, 0.5, 0.0), Direction.UP, Floor, false));
            BlockPos Landed = Floor.above();
            BlockState State = World.getBlockState(Landed);
            Helper.assertTrue(Placed.consumesAction() && State.is(Scp458Registry.Box.get()), "The open box could not be set down: " + Placed + ", " + State);
            Helper.assertTrue(Scp458BoxBlock.isOpen(State), "The box was set down shut");
            Helper.assertTrue(World.getBlockEntity(Landed) instanceof Scp458BoxBlockEntity Down && Down.contents().equals(Carried), "The placed box lost what was in it");
            dismiss(Helper, User);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp458_pickupKeepsContents")
    public static void pickupKeepsContents(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        place(Helper);
        Scp458TestPlayer User = player(Helper, 8.5, 5.5, "scp458_mover");
        BlockPos At = Helper.absolutePos(Spot);
        try {
            Scp458Favourites.set(User, Scp458Pizza.Supreme);
            click(User, At);
            click(User, At);
            click(User, At);
            User.getInventory().clearContent();
            Helper.assertTrue(User.gameMode.destroyBlock(At), "The box could not be picked up");
            Helper.assertTrue(World.getBlockState(At).isAir(), "The box is still there");
            ItemEntity Dropped = null;
            for (ItemEntity Found : World.getEntitiesOfClass(ItemEntity.class, new AABB(At).inflate(3.0))) {
                if (Scp458BoxItem.is(Found.getItem())) {
                    Dropped = Found;
                }
            }
            Helper.assertTrue(Dropped != null, "Breaking the box dropped nothing");
            ItemStack Carried = Dropped.getItem().copy();
            Dropped.discard();
            Scp458Contents Kept = Scp458BoxItem.contents(Carried);
            Helper.assertTrue(Kept.Open() && Kept.Pizza().equals(Optional.of(Scp458Pizza.Supreme)) && Kept.left() == Scp458Contents.Slices - 2, "The dropped box forgot its pizza: " + Kept);
        } finally {
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp458_lidRestsAfterClosing")
    public static void lidRestsAfterClosing(GameTestHelper Helper) {
        place(Helper);
        Scp458TestPlayer User = player(Helper, 8.5, 5.5, "scp458_rest");
        BlockPos At = Helper.absolutePos(Spot);
        click(User, At);
        shut(User, At);
        click(User, At);
        Helper.assertFalse(open(Helper, Spot), "The lid opened again before it settled");
        Helper.startSequence().thenIdle(Rested).thenExecute(() -> {
            click(User, At);
            Helper.assertTrue(open(Helper, Spot), "The lid stayed shut after resting");
            dismiss(Helper, User);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp458_frontSliceAndLidClick")
    public static void frontSliceAndLidClick(GameTestHelper Helper) {
        BlockPos EastSpot = new BlockPos(4, 2, 8);
        Scp458BoxBlockEntity North = place(Helper);
        Scp458BoxBlockEntity East = place(Helper, EastSpot, Direction.EAST);
        Scp458TestPlayer Front = player(Helper, 8.5, 4.5, "scp458_front");
        Scp458TestPlayer Side = player(Helper, 7.5, 8.5, "scp458_side");
        BlockPos At = Helper.absolutePos(Spot);
        BlockPos EastAt = Helper.absolutePos(EastSpot);
        try {
            click(Front, At);
            click(Front, At);
            Helper.assertTrue(North.contents().Taken() == 1 << FrontSlice, "The first slice was not the one facing the taker: " + Integer.toBinaryString(North.contents().Taken()));
            Vec3 Lid = Vec3.atLowerCornerOf(At).add(0.5, 10.0 / 16.0, 13.5 / 16.0);
            click(Front, At, Lid, false);
            Helper.assertFalse(open(Helper, Spot), "Clicking the raised lid did not close it");
            click(Side, EastAt);
            click(Side, EastAt);
            Helper.assertTrue(East.contents().Taken() == 1 << FrontSlice, "A turned box gave a slice from the wrong side: " + Integer.toBinaryString(East.contents().Taken()));
        } finally {
            dismiss(Helper, Front);
            dismiss(Helper, Side);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp458_automationRefused")
    public static void automationRefused(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp458BoxBlockEntity Carton = place(Helper);
        BlockPos At = Helper.absolutePos(Spot);
        FakePlayer Bot = FakePlayerFactory.get(World, new GameProfile(UUID.randomUUID(), "scp458_bot"));
        Vec3 Stand = Helper.absoluteVec(new Vec3(8.5, 2.0, 5.5));
        Bot.moveTo(Stand.x, Stand.y, Stand.z, 0.0f, 0.0f);
        InteractionResult Result = Bot.gameMode.useItemOn(Bot, World, ItemStack.EMPTY, InteractionHand.MAIN_HAND, new BlockHitResult(pizzaPoint(At), Direction.UP, At, false));
        Helper.assertFalse(Result.consumesAction(), "A fake player was allowed to use the box");
        Helper.assertFalse(open(Helper, Spot), "A fake player opened the box");
        Helper.assertTrue(Carton.contents().Pizza().isEmpty() && Scp458Favourites.of(Bot) == null, "A fake player was served");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp458_strangerBringsClosedBox")
    public static void strangerBringsClosedBox(GameTestHelper Helper) {
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp458Registry.DocumentId);
        Helper.assertTrue(Document != null, "The SCP-458 dossier did not load");
        Helper.assertTrue(Document.Classification() == DocumentClass.Safe, "SCP-458 is not filed as Safe");
        Helper.assertTrue(Document.Condition() instanceof StrangerEncounterCondition, "SCP-458 does not arrive with the stranger");
        StrangerPlan Plan = ((StrangerEncounterCondition) Document.Condition()).plan();
        Helper.assertTrue(Plan.Gifts().size() == 1, "The stranger brings " + Plan.Gifts().size() + " gifts");
        StrangerGift Gift = Plan.Gifts().get(0);
        ItemStack Stack = Gift.toStack();
        Helper.assertTrue(Scp458BoxItem.is(Stack) && Stack.getCount() == 1, "The stranger's gift is not the box: " + Stack);
        Helper.assertFalse(Scp458BoxItem.contents(Stack).Open(), "The gifted box would arrive open");
        Helper.assertFalse(Scp458BoxBlock.isOpen(Scp458Registry.Box.get().defaultBlockState()), "The box does not default to closed");
        Helper.succeed();
    }
}
