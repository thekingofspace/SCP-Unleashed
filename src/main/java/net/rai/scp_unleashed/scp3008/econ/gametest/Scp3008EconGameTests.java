package net.rai.scp_unleashed.scp3008.econ.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp3008.econ.Scp3008EconRegistry;
import net.rai.scp_unleashed.scp3008.econ.block.Scp3008LanternBlock;
import net.rai.scp_unleashed.scp3008.econ.block.Scp3008WorkbenchMenu;
import net.rai.scp_unleashed.scp3008.econ.item.Scp3008HammerItem;
import net.rai.scp_unleashed.scp3008.econ.item.Scp3008LanternItem;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp3008EconGameTests {
    public static final String Arena = "scp3008_econ_arena";
    public static final int MinimumTables = 90;
    private static final BlockPos Spot = new BlockPos(4, 2, 4);

    private Scp3008EconGameTests() {
    }

    private static FakePlayer user(GameTestHelper Helper, String Name) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), Name));
        Vec3 Where = Helper.absoluteVec(new Vec3(4.5, 2.0, 2.5));
        User.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
        User.setGameMode(GameType.SURVIVAL);
        return User;
    }

    private static Map<ResourceLocation, Resource> storeTables(GameTestHelper Helper) {
        return Helper.getLevel().getServer().getResourceManager().listResources("loot_table",
                Location -> Location.getNamespace().equals(Scp_unleashed.ModId) && Location.getPath().contains("scp3008") && Location.getPath().endsWith(".json"));
    }

    private static void collect(JsonElement Node, List<String> Items, List<String> Tags) {
        if (Node.isJsonArray()) {
            for (JsonElement Child : Node.getAsJsonArray()) {
                collect(Child, Items, Tags);
            }
            return;
        }
        if (!Node.isJsonObject()) {
            return;
        }
        JsonObject Fields = Node.getAsJsonObject();
        if (Fields.has("type") && Fields.has("name") && Fields.get("type").isJsonPrimitive()) {
            String Kind = Fields.get("type").getAsString();
            if (Kind.equals("minecraft:item")) {
                Items.add(Fields.get("name").getAsString());
            } else if (Kind.equals("minecraft:tag")) {
                Tags.add(Fields.get("name").getAsString());
            }
        }
        for (Map.Entry<String, JsonElement> Entry : Fields.entrySet()) {
            collect(Entry.getValue(), Items, Tags);
        }
    }

    private static List<String> tagMembers(String Tag) {
        List<String> Found = new ArrayList<>();
        Optional<HolderSet.Named<Item>> Listed = BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, ResourceLocation.parse(Tag)));
        Listed.ifPresent(Members -> Members.forEach(Member -> Found.add(BuiltInRegistries.ITEM.getKey(Member.value()).toString())));
        return Found;
    }

    private static Set<String> lootItems(GameTestHelper Helper, List<String> Problems) {
        Set<String> Found = new HashSet<>();
        for (Map.Entry<ResourceLocation, Resource> Entry : storeTables(Helper).entrySet()) {
            List<String> Items = new ArrayList<>();
            List<String> Tags = new ArrayList<>();
            try (Reader Text = Entry.getValue().openAsReader()) {
                collect(JsonParser.parseReader(Text), Items, Tags);
            } catch (Exception Failure) {
                Problems.add(Entry.getKey() + " could not be read: " + Failure.getMessage());
                continue;
            }
            for (String Tag : Tags) {
                if (Tag.startsWith("minecraft:")) {
                    Problems.add(Entry.getKey() + " uses the vanilla tag " + Tag);
                }
                Items.addAll(tagMembers(Tag));
            }
            for (String Name : Items) {
                if (Name.startsWith("minecraft:") && !Name.equals("minecraft:air")) {
                    Problems.add(Entry.getKey() + " drops " + Name);
                }
                Found.add(Name);
            }
        }
        return Found;
    }

    private static boolean storeRecipe(RecipeHolder<?> Known) {
        ResourceLocation Id = Known.id();
        return Id.getNamespace().equals(Scp_unleashed.ModId) && (Id.getPath().startsWith("scp3008_") || Id.getPath().equals("crowbar"));
    }

    private static List<DeferredItem<? extends Item>> storeItems() {
        List<DeferredItem<? extends Item>> All = new ArrayList<>();
        All.addAll(Scp3008EconRegistry.Materials);
        All.addAll(Scp3008EconRegistry.Survival);
        All.addAll(Scp3008EconRegistry.Foods);
        All.addAll(Scp3008EconRegistry.Placeables);
        return All;
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_econ_loot")
    public static void storeLootHoldsNoVanillaItems(GameTestHelper Helper) {
        int Tables = storeTables(Helper).size();
        Helper.assertTrue(Tables >= MinimumTables, "only " + Tables + " store loot tables were found");
        List<String> Problems = new ArrayList<>();
        Set<String> Found = lootItems(Helper, Problems);
        Helper.assertTrue(Problems.isEmpty(), "vanilla items in store loot: " + String.join("; ", Problems.subList(0, Math.min(8, Problems.size()))));
        Helper.assertTrue(Found.contains("scp_unleashed:scp3008_particleboard") && Found.contains("scp_unleashed:scp3008_battery"), "store loot is missing materials");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_econ_recipes")
    public static void storeRecipesUseNoVanillaItems(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        List<String> Problems = new ArrayList<>();
        int Count = 0;
        for (RecipeHolder<?> Known : World.getRecipeManager().getRecipes()) {
            if (!storeRecipe(Known)) {
                continue;
            }
            Count++;
            NonNullList<Ingredient> Parts = Known.value().getIngredients();
            for (Ingredient Part : Parts) {
                for (ItemStack Choice : Part.getItems()) {
                    ResourceLocation Id = BuiltInRegistries.ITEM.getKey(Choice.getItem());
                    if (Id.getNamespace().equals("minecraft")) {
                        Problems.add(Known.id() + " takes " + Id);
                    }
                }
            }
            ResourceLocation Made = BuiltInRegistries.ITEM.getKey(Known.value().getResultItem(World.registryAccess()).getItem());
            if (Made.getNamespace().equals("minecraft")) {
                Problems.add(Known.id() + " makes " + Made);
            }
        }
        Helper.assertTrue(Count >= 15, "only " + Count + " store recipes were found");
        Helper.assertTrue(Problems.isEmpty(), "vanilla items in store recipes: " + String.join("; ", Problems));
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_econ_recipes")
    public static void everyStoreItemCanBeObtained(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Set<String> Found = lootItems(Helper, new ArrayList<>());
        for (RecipeHolder<?> Known : World.getRecipeManager().getRecipes()) {
            Found.add(BuiltInRegistries.ITEM.getKey(Known.value().getResultItem(World.registryAccess()).getItem()).toString());
        }
        List<String> Missing = new ArrayList<>();
        for (DeferredItem<? extends Item> Entry : storeItems()) {
            String Id = BuiltInRegistries.ITEM.getKey(Entry.get()).toString();
            if (!Found.contains(Id)) {
                Missing.add(Id);
            }
        }
        Helper.assertTrue(Missing.isEmpty(), "no recipe or store loot gives " + String.join(", ", Missing));
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_econ_recipes")
    public static void allenKeyIsKeptAfterAssembly(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        List<ItemStack> Grid = List.of(new ItemStack(Scp3008EconRegistry.Particleboard.get()), new ItemStack(Scp3008EconRegistry.Particleboard.get()),
                new ItemStack(Scp3008EconRegistry.HexFittings.get()), new ItemStack(Scp3008EconRegistry.AllenKey.get()));
        CraftingInput Input = CraftingInput.of(2, 2, Grid);
        Optional<RecipeHolder<CraftingRecipe>> Match = World.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, Input, World);
        Helper.assertTrue(Match.isPresent(), "the flat-pack workbench cannot be assembled in the inventory grid");
        ItemStack Made = Match.get().value().assemble(Input, World.registryAccess());
        Helper.assertTrue(Made.is(Scp3008EconRegistry.WorkbenchItem.get()), "the assembly made " + Made);
        NonNullList<ItemStack> Left = Match.get().value().getRemainingItems(Input);
        Helper.assertTrue(Left.stream().anyMatch(Stack -> Stack.is(Scp3008EconRegistry.AllenKey.get())), "the allen key was used up");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_econ_workbench")
    public static void workbenchMenuStaysOpenOnlyAtTheBench(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Pos = Helper.absolutePos(Spot);
        World.setBlockAndUpdate(Pos, Scp3008EconRegistry.Workbench.get().defaultBlockState());
        FakePlayer User = user(Helper, "econ_bench");
        Scp3008WorkbenchMenu Menu = new Scp3008WorkbenchMenu(1, User.getInventory(), ContainerLevelAccess.create(World, Pos), Scp3008EconRegistry.Workbench.get());
        Helper.assertTrue(Menu.stillValid(User), "the workbench grid closed while standing at the bench");
        Helper.assertTrue(Menu.getSize() == 10, "the workbench grid is not three by three");
        World.setBlockAndUpdate(Pos, Blocks.AIR.defaultBlockState());
        Helper.assertFalse(Menu.stillValid(User), "the workbench grid stayed open after the bench was removed");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_econ_lantern")
    public static void lanternDimsDrainsAndRecharges(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Pos = Helper.absolutePos(Spot);
        Scp3008LanternBlock Lamp = Scp3008EconRegistry.Lantern.get();
        World.setBlockAndUpdate(Pos, Lamp.defaultBlockState().setValue(Scp3008LanternBlock.Lit, true));
        Helper.assertTrue(World.getBlockState(Pos).getLightEmission(World, Pos) == Scp3008LanternBlock.BrightLight, "a fresh lit lantern is not bright");
        Helper.assertTrue(World.getBlockTicks().hasScheduledTick(Pos, Lamp), "a lit lantern is not draining");
        for (int Step = 0; Step < Scp3008LanternBlock.MaxCharge - Scp3008LanternBlock.LowCharge; Step++) {
            Lamp.drain(World.getBlockState(Pos), World, Pos);
        }
        Helper.assertTrue(World.getBlockState(Pos).getLightEmission(World, Pos) == Scp3008LanternBlock.DimLight, "a low lantern did not dim");
        for (int Step = 0; Step < Scp3008LanternBlock.LowCharge; Step++) {
            Lamp.drain(World.getBlockState(Pos), World, Pos);
        }
        BlockState Flat = World.getBlockState(Pos);
        Helper.assertTrue(Flat.getValue(Scp3008LanternBlock.Charge) == 0 && !Flat.getValue(Scp3008LanternBlock.Lit) && Flat.getLightEmission(World, Pos) == 0,
                "a flat lantern still glows");
        FakePlayer User = user(Helper, "econ_lantern");
        ItemStack Cell = new ItemStack(Scp3008EconRegistry.Battery.get(), 2);
        User.setItemInHand(InteractionHand.MAIN_HAND, Cell);
        Flat.useItemOn(Cell, World, User, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(Pos), Direction.UP, Pos, false));
        BlockState Fresh = World.getBlockState(Pos);
        Helper.assertTrue(Fresh.getValue(Scp3008LanternBlock.Charge) == Scp3008LanternBlock.MaxCharge && Fresh.getValue(Scp3008LanternBlock.Lit),
                "a battery did not recharge the lantern");
        Helper.assertTrue(User.getMainHandItem().getCount() == 1, "fitting a battery did not use it up");
        Lamp.toggle(Fresh, World, Pos);
        Helper.assertTrue(World.getBlockState(Pos).getLightEmission(World, Pos) == 0, "switching the lantern off left it glowing");
        List<ItemStack> Drops = Block.getDrops(Fresh.setValue(Scp3008LanternBlock.Charge, 3), World, Pos, null);
        Helper.assertTrue(Drops.size() == 1 && Scp3008LanternItem.charge(Drops.get(0)) == 3, "a picked up lantern lost its charge: " + Drops);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp3008_econ_lantern")
    public static void litLanternLightsTheArea(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Stand = Spot.west(2);
        Helper.setBlock(Stand, Scp3008EconRegistry.Lantern.get().defaultBlockState().setValue(Scp3008LanternBlock.Lit, true));
        Helper.assertTrue(Helper.getBlockState(Stand).getLightEmission() == Scp3008LanternBlock.BrightLight, "the placed lantern is not lit");
        BlockPos Near = Helper.absolutePos(Stand.east(2));
        Helper.succeedWhen(() -> {
            int Source = World.getBrightness(LightLayer.BLOCK, Helper.absolutePos(Stand));
            int Around = World.getBrightness(LightLayer.BLOCK, Near);
            Helper.assertTrue(Source == Scp3008LanternBlock.BrightLight && Around >= Scp3008LanternBlock.BrightLight - 2,
                    "the lantern does not light the floor around it: " + Source + " at the lantern, " + Around + " two blocks away");
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_econ_first_aid")
    public static void firstAidKitHealsAndRegenerates(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = user(Helper, "econ_patient");
        ItemStack Whole = new ItemStack(Scp3008EconRegistry.FirstAidKit.get(), 2);
        User.setItemInHand(InteractionHand.MAIN_HAND, Whole);
        User.setHealth(User.getMaxHealth());
        Helper.assertFalse(Scp3008EconRegistry.FirstAidKit.get().use(World, User, InteractionHand.MAIN_HAND).getResult().consumesAction(),
                "a first aid kit was opened at full health");
        User.setHealth(6.0f);
        ItemStack Left = Scp3008EconRegistry.FirstAidKit.get().finishUsingItem(Whole, World, User);
        Helper.assertTrue(User.getHealth() >= 6.0f + Scp3008EconRegistry.FirstAidKit.get().kind().Heal - 0.01f, "the first aid kit did not heal: " + User.getHealth());
        Helper.assertTrue(User.hasEffect(MobEffects.REGENERATION), "the first aid kit gave no regeneration");
        Helper.assertTrue(Left.getCount() == 1, "the first aid kit was not used up");
        Helper.assertTrue(User.getCooldowns().isOnCooldown(Scp3008EconRegistry.FirstAidKit.get()), "the first aid kit has no cooldown");
        User.removeAllEffects();
        User.setHealth(10.0f);
        ItemStack Roll = new ItemStack(Scp3008EconRegistry.Bandage.get(), 3);
        Scp3008EconRegistry.Bandage.get().finishUsingItem(Roll, World, User);
        Helper.assertTrue(User.getHealth() >= 10.0f + Scp3008EconRegistry.Bandage.get().kind().Heal - 0.01f && Roll.getCount() == 2, "the bandage did not heal");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_econ_tools")
    public static void clawHammerBreaksFurnitureQuickly(GameTestHelper Helper) {
        Optional<HolderSet.Named<Block>> Listed = BuiltInRegistries.BLOCK.getTag(Scp3008HammerItem.Furniture);
        Helper.assertTrue(Listed.isPresent() && Listed.get().size() > 0, "there is no liftable furniture to test against");
        BlockState Piece = Listed.get().get(0).value().defaultBlockState();
        ItemStack Hammer = new ItemStack(Scp3008EconRegistry.ClawHammer.get());
        Helper.assertTrue(Hammer.getDestroySpeed(Piece) >= Scp3008HammerItem.FurnitureSpeed, "the hammer is slow on furniture: " + Hammer.getDestroySpeed(Piece));
        Helper.assertTrue(new ItemStack(Scp3008EconRegistry.Workbench.get()).getDestroySpeed(Piece) <= 1.0f, "bare hands are as quick as the hammer");
        Helper.succeed();
    }
}
