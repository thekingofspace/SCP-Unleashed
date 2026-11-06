package net.rai.scp_unleashed.scp3008.gear.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp3008.gear.Scp3008GearRegistry;
import net.rai.scp_unleashed.scp3008.gear.Scp3008GearTags;
import net.rai.scp_unleashed.scp3008.gear.block.Scp3008ShelfBlock;
import net.rai.scp_unleashed.scp3008.gear.block.Scp3008ShelfBlockEntity;
import net.rai.scp_unleashed.scp3008.gear.block.Scp3008TallShelfBlock;
import net.rai.scp_unleashed.scp3008.gear.carry.Scp3008Carried;
import net.rai.scp_unleashed.scp3008.gear.carry.Scp3008Carry;
import net.rai.scp_unleashed.scp3008.gear.carry.Scp3008CarryParts;
import net.rai.scp_unleashed.scp3008.gear.entity.Scp3008ThrownSpear;
import net.rai.scp_unleashed.scp3008.gear.item.Scp3008CrowbarItem;
import net.rai.scp_unleashed.scp3008.gear.item.Scp3008SpearItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp3008GearGameTests {
    public static final String Arena = "scp3008_gear_arena";

    private Scp3008GearGameTests() {
    }

    public static FakePlayer user(GameTestHelper Helper, double X, double Z, float Yaw, String Name) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), Name));
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, 2.0, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        User.setYHeadRot(Yaw);
        User.setGameMode(GameType.SURVIVAL);
        return User;
    }

    private static BlockState shelf(Direction Facing) {
        return Scp3008GearRegistry.ShelfShort.get().defaultBlockState().setValue(Scp3008ShelfBlock.Facing, Facing);
    }

    private static int count(GameTestHelper Helper, Item Kind) {
        AABB Area = new AABB(Helper.absolutePos(BlockPos.ZERO)).inflate(14);
        int Total = 0;
        for (ItemEntity Dropped : Helper.getLevel().getEntitiesOfClass(ItemEntity.class, Area)) {
            if (Dropped.getItem().is(Kind)) {
                Total += Dropped.getItem().getCount();
            }
        }
        return Total;
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_gear_lift")
    public static void crowbarLiftsAndPlacesShelfWithContents(GameTestHelper Helper) {
        BlockPos Spot = new BlockPos(5, 2, 5);
        Helper.setBlock(Spot, shelf(Direction.NORTH));
        Scp3008ShelfBlockEntity Stored = Helper.getBlockEntity(Spot);
        Stored.setItem(3, new ItemStack(Items.DIAMOND, 12));
        FakePlayer User = user(Helper, 5.5, 8.5, 180.0f, "scp3008_gear_lifter");
        ItemStack Bar = new ItemStack(Scp3008GearRegistry.Crowbar.get());
        User.setItemInHand(InteractionHand.MAIN_HAND, Bar);
        BlockPos At = Helper.absolutePos(Spot);
        InteractionResult Result = Scp3008GearRegistry.Crowbar.get().onItemUseFirst(Bar, new UseOnContext(User, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(At), Direction.UP, At, false)));
        Helper.assertTrue(Result == InteractionResult.SUCCESS, "the crowbar did not lift the shelf: " + Result);
        Helper.assertTrue(Helper.getBlockState(Spot).isAir(), "the shelf is still standing after the lift");
        Helper.assertTrue(Bar.getDamageValue() == Scp3008CrowbarItem.LiftWear, "lifting did not wear the crowbar");
        Helper.assertTrue(count(Helper, Items.DIAMOND) == 0, "lifting spilled the shelf's contents");
        Scp3008Carried Held = Scp3008Carry.carried(User);
        Helper.assertTrue(Held.Parts().size() == 1 && Held.Parts().get(0).Data().contains("Items"), "the carried shelf lost its block entity data");
        Scp3008Carry.tick(User);
        AttributeInstance Speed = User.getAttribute(Attributes.MOVEMENT_SPEED);
        Helper.assertTrue(Speed != null && Speed.hasModifier(Scp3008Carry.SlowId), "carrying does not slow the player");
        BlockPos Target = new BlockPos(5, 2, 3);
        Helper.assertTrue(Scp3008Carry.place(User, Helper.absolutePos(Target)) == Scp3008Carry.Outcome.Placed, "the shelf could not be set down");
        BlockState Placed = Helper.getBlockState(Target);
        Helper.assertTrue(Placed.is(Scp3008GearRegistry.ShelfShort.get()) && Placed.getValue(Scp3008ShelfBlock.Facing) == Direction.NORTH,
                "the shelf was not set down facing away from the player: " + Placed);
        Scp3008ShelfBlockEntity Moved = Helper.getBlockEntity(Target);
        Helper.assertTrue(Moved.getItem(3).is(Items.DIAMOND) && Moved.getItem(3).getCount() == 12, "the shelf contents did not travel with it");
        Helper.assertFalse(Scp3008Carry.carrying(User), "the player is still carrying after setting down");
        Scp3008Carry.tick(User);
        Helper.assertFalse(Speed.hasModifier(Scp3008Carry.SlowId), "the carry slowness stayed after setting down");
        Helper.assertTrue(Scp3008Carry.lift(User, Helper.absolutePos(Target)) == Scp3008Carry.Outcome.Lifted, "the shelf could not be lifted a second time");
        Helper.assertTrue(Scp3008Carry.turn(User) == Scp3008Carry.Outcome.Turned, "the carried shelf did not turn");
        BlockPos Turned = new BlockPos(7, 2, 3);
        Helper.assertTrue(Scp3008Carry.place(User, Helper.absolutePos(Turned)) == Scp3008Carry.Outcome.Placed, "the turned shelf could not be set down");
        Helper.assertTrue(Helper.getBlockState(Turned).getValue(Scp3008ShelfBlock.Facing) == Direction.EAST, "one turn did not rotate the shelf clockwise");
        Scp3008ShelfBlockEntity Again = Helper.getBlockEntity(Turned);
        Helper.assertTrue(Again.getItem(3).getCount() == 12, "the contents were lost on the second carry");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_gear_refuse")
    public static void crowbarRefusesWhatItCannotLift(GameTestHelper Helper) {
        FakePlayer User = user(Helper, 6.5, 9.5, 180.0f, "scp3008_gear_refuser");
        ItemStack Bar = new ItemStack(Scp3008GearRegistry.Crowbar.get());
        User.setItemInHand(InteractionHand.MAIN_HAND, Bar);
        BlockPos Stone = new BlockPos(2, 2, 2);
        Helper.setBlock(Stone, Blocks.STONE);
        BlockPos StoneAt = Helper.absolutePos(Stone);
        InteractionResult Result = Scp3008GearRegistry.Crowbar.get().onItemUseFirst(Bar, new UseOnContext(User, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(StoneAt), Direction.UP, StoneAt, false)));
        Helper.assertTrue(Result == InteractionResult.PASS && Helper.getBlockState(Stone).is(Blocks.STONE), "the crowbar took plain stone");
        BlockPos Bedrock = new BlockPos(3, 2, 2);
        Helper.setBlock(Bedrock, Blocks.BEDROCK);
        Helper.assertTrue(Scp3008Carry.lift(User, Helper.absolutePos(Bedrock)) == Scp3008Carry.Outcome.Ignored, "bedrock was not ignored");
        Helper.assertTrue(Helper.getBlockState(Bedrock).is(Blocks.BEDROCK), "bedrock moved");
        Block Fixed = fixedAndLiftable();
        if (Fixed != null) {
            BlockPos FixedSpot = new BlockPos(4, 2, 2);
            Helper.setBlock(FixedSpot, Fixed);
            Helper.assertTrue(Scp3008Carry.lift(User, Helper.absolutePos(FixedSpot)) == Scp3008Carry.Outcome.Refused, "an unbreakable block was lifted");
            Helper.assertTrue(Helper.getBlockState(FixedSpot).is(Fixed), "an unbreakable block moved");
        }
        Helper.assertTrue(Scp3008Carry.fixed(Helper.getLevel(), Helper.absolutePos(Bedrock), Blocks.BEDROCK.defaultBlockState()), "bedrock does not count as fixed");
        BlockPos First = new BlockPos(6, 2, 6);
        BlockPos Second = new BlockPos(8, 2, 6);
        Helper.setBlock(First, shelf(Direction.NORTH));
        Helper.setBlock(Second, shelf(Direction.NORTH));
        Helper.assertTrue(Scp3008Carry.lift(User, Helper.absolutePos(First)) == Scp3008Carry.Outcome.Lifted, "the first shelf was not lifted");
        Helper.assertTrue(Scp3008Carry.lift(User, Helper.absolutePos(Second)) == Scp3008Carry.Outcome.Ignored, "a second shelf was lifted while carrying");
        Helper.assertTrue(Helper.getBlockState(Second).is(Scp3008GearRegistry.ShelfShort.get()), "the second shelf moved");
        Helper.assertTrue(Scp3008Carry.place(User, StoneAt) == Scp3008Carry.Outcome.Refused, "the shelf was set down inside stone");
        Helper.assertTrue(Scp3008Carry.carrying(User) && Helper.getBlockState(Stone).is(Blocks.STONE), "a refused set down changed something");
        Helper.assertTrue(Scp3008Carry.place(User, Helper.absolutePos(First)) == Scp3008Carry.Outcome.Placed, "the shelf could not go back");
        User.setGameMode(GameType.ADVENTURE);
        Helper.assertTrue(Scp3008Carry.lift(User, Helper.absolutePos(First)) == Scp3008Carry.Outcome.Refused, "an adventure mode player lifted a shelf");
        Helper.assertTrue(Helper.getBlockState(First).is(Scp3008GearRegistry.ShelfShort.get()), "an adventure mode player moved a shelf");
        Helper.succeed();
    }

    private static Block fixedAndLiftable() {
        Optional<HolderSet.Named<Block>> Listed = BuiltInRegistries.BLOCK.getTag(Scp3008GearTags.Liftable);
        if (Listed.isEmpty()) {
            return null;
        }
        for (Holder<Block> Entry : Listed.get()) {
            if (Entry.value().defaultBlockState().is(Scp3008GearTags.Unbreakable)) {
                return Entry.value();
            }
        }
        return null;
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_gear_tall")
    public static void tallShelfLiftsWhole(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Spot = new BlockPos(3, 2, 3);
        BlockState Lower = Scp3008GearRegistry.ShelfTall.get().defaultBlockState().setValue(Scp3008ShelfBlock.Facing, Direction.SOUTH);
        Helper.setBlock(Spot, Lower);
        Scp3008TallShelfBlock.placeWhole(World, Helper.absolutePos(Spot), Lower);
        Helper.assertTrue(Helper.getBlockState(Spot.above()).is(Scp3008GearRegistry.ShelfTall.get())
                && Helper.getBlockState(Spot.above()).getValue(Scp3008TallShelfBlock.Half) == DoubleBlockHalf.UPPER, "placeWhole did not add the upper half");
        Scp3008ShelfBlockEntity Stored = Helper.getBlockEntity(Spot);
        Helper.assertTrue(Stored.getContainerSize() == 18, "the tall shelf does not hold two rows");
        Stored.setItem(10, new ItemStack(Items.APPLE, 5));
        FakePlayer User = user(Helper, 8.5, 8.5, 90.0f, "scp3008_gear_tall_lifter");
        Helper.assertTrue(Scp3008Carry.lift(User, Helper.absolutePos(Spot.above())) == Scp3008Carry.Outcome.Lifted, "lifting from the upper half failed");
        Helper.assertTrue(Helper.getBlockState(Spot).isAir() && Helper.getBlockState(Spot.above()).isAir(), "a half of the tall shelf stayed behind");
        Helper.assertTrue(count(Helper, Items.APPLE) == 0 && count(Helper, Scp3008GearRegistry.ShelfTallItem.get()) == 0, "lifting dropped items");
        Scp3008Carried Held = Scp3008Carry.carried(User);
        Helper.assertTrue(Held.Parts().size() == 2 && Held.Parts().get(0).Offset().equals(BlockPos.ZERO)
                && Held.Parts().get(0).State().getValue(Scp3008TallShelfBlock.Half) == DoubleBlockHalf.LOWER, "the lower half is not the anchor");
        BlockPos Low = new BlockPos(6, 2, 6);
        Helper.setBlock(Low.above(), Blocks.STONE);
        Helper.assertTrue(Scp3008Carry.place(User, Helper.absolutePos(Low)) == Scp3008Carry.Outcome.Refused, "the tall shelf went under a one block gap");
        Helper.assertTrue(Helper.getBlockState(Low).isAir() && Scp3008Carry.carrying(User), "a refused tall shelf changed the world");
        BlockPos Target = new BlockPos(8, 2, 6);
        Helper.assertTrue(Scp3008Carry.place(User, Helper.absolutePos(Target)) == Scp3008Carry.Outcome.Placed, "the tall shelf could not be set down");
        BlockState Down = Helper.getBlockState(Target);
        BlockState Up = Helper.getBlockState(Target.above());
        Helper.assertTrue(Down.is(Scp3008GearRegistry.ShelfTall.get()) && Down.getValue(Scp3008TallShelfBlock.Half) == DoubleBlockHalf.LOWER
                && Down.getValue(Scp3008ShelfBlock.Facing) == Direction.WEST, "the lower half is wrong: " + Down);
        Helper.assertTrue(Up.is(Scp3008GearRegistry.ShelfTall.get()) && Up.getValue(Scp3008TallShelfBlock.Half) == DoubleBlockHalf.UPPER
                && Up.getValue(Scp3008ShelfBlock.Facing) == Direction.WEST, "the upper half is wrong: " + Up);
        Scp3008ShelfBlockEntity Moved = Helper.getBlockEntity(Target);
        Helper.assertTrue(Moved.getItem(10).getCount() == 5, "the tall shelf lost its contents");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp3008_gear_parts")
    public static void partsAreFoundForEveryShape(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Foot = new BlockPos(2, 2, 3);
        BlockState Bed = Blocks.RED_BED.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        Helper.setBlock(Foot, Bed.setValue(BlockStateProperties.BED_PART, BedPart.FOOT));
        Helper.setBlock(Foot.north(), Bed.setValue(BlockStateProperties.BED_PART, BedPart.HEAD));
        List<BlockPos> BedParts = Scp3008CarryParts.parts(World, Helper.absolutePos(Foot.north()), Helper.getBlockState(Foot.north()), false);
        Helper.assertTrue(BedParts.size() == 2 && BedParts.get(0).equals(Helper.absolutePos(Foot.north())), "the bed parts are wrong: " + BedParts);
        BlockPos Door = new BlockPos(5, 2, 3);
        Helper.setBlock(Door, Blocks.OAK_DOOR.defaultBlockState());
        Helper.setBlock(Door.above(), Blocks.OAK_DOOR.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
        List<BlockPos> DoorParts = Scp3008CarryParts.parts(World, Helper.absolutePos(Door.above()), Helper.getBlockState(Door.above()), false);
        Helper.assertTrue(DoorParts.size() == 2 && DoorParts.get(0).equals(Helper.absolutePos(Door)), "the door parts are wrong: " + DoorParts);
        BlockPos Pair = new BlockPos(8, 2, 3);
        Helper.setBlock(Pair, Blocks.OAK_PLANKS);
        Helper.setBlock(Pair.east(), Blocks.OAK_PLANKS);
        Helper.assertTrue(Scp3008CarryParts.parts(World, Helper.absolutePos(Pair), Blocks.OAK_PLANKS.defaultBlockState(), true).size() == 2, "a spread did not find two blocks");
        Helper.assertTrue(Scp3008CarryParts.parts(World, Helper.absolutePos(Pair), Blocks.OAK_PLANKS.defaultBlockState(), false).size() == 1, "a single block spread");
        for (BlockPos Cube : BlockPos.betweenClosed(new BlockPos(2, 2, 7), new BlockPos(4, 3, 9))) {
            Helper.setBlock(Cube, Blocks.OAK_PLANKS);
        }
        Helper.assertTrue(Scp3008CarryParts.parts(World, Helper.absolutePos(new BlockPos(3, 3, 8)), Blocks.OAK_PLANKS.defaultBlockState(), true).size()
                > Scp3008CarryParts.MaxParts, "a big lump did not exceed the part limit");
        Helper.assertTrue(Scp3008CarryParts.isMain(Blocks.STONE.defaultBlockState()), "a plain block is not its own main part");
        Helper.assertFalse(Scp3008CarryParts.isMain(Blocks.OAK_DOOR.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER)),
                "an upper half counts as a main part");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_gear_release")
    public static void carriedPieceIsSetDownOrDropped(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Spot = new BlockPos(3, 2, 3);
        Helper.setBlock(Spot, shelf(Direction.NORTH));
        Scp3008ShelfBlockEntity Stored = Helper.getBlockEntity(Spot);
        Stored.setItem(0, new ItemStack(Items.BREAD, 7));
        FakePlayer User = user(Helper, 6.5, 6.5, 0.0f, "scp3008_gear_dropper");
        Helper.assertTrue(Scp3008Carry.lift(User, Helper.absolutePos(Spot)) == Scp3008Carry.Outcome.Lifted, "the shelf was not lifted");
        NeoForge.EVENT_BUS.post(new LivingDeathEvent(User, World.damageSources().generic()));
        Helper.assertFalse(Scp3008Carry.carrying(User), "dying kept the shelf in hand");
        BlockPos Feet = User.blockPosition();
        boolean Found = false;
        for (BlockPos Near : BlockPos.betweenClosed(Feet.offset(-2, 0, -2), Feet.offset(2, 1, 2))) {
            if (World.getBlockState(Near).is(Scp3008GearRegistry.ShelfShort.get()) && World.getBlockEntity(Near) instanceof Scp3008ShelfBlockEntity Kept
                    && Kept.getItem(0).getCount() == 7) {
                Found = true;
            }
        }
        Helper.assertTrue(Found, "dying did not set the shelf down nearby with its contents");
        BlockPos Other = new BlockPos(9, 2, 9);
        Helper.setBlock(Other, shelf(Direction.NORTH));
        Scp3008ShelfBlockEntity Filled = Helper.getBlockEntity(Other);
        Filled.setItem(4, new ItemStack(Items.COAL, 9));
        Helper.assertTrue(Scp3008Carry.lift(User, Helper.absolutePos(Other)) == Scp3008Carry.Outcome.Lifted, "the second shelf was not lifted");
        Scp3008Carry.drop(World, Helper.absoluteVec(new Vec3(9.5, 2.2, 9.5)), Scp3008Carry.carried(User));
        User.removeData(Scp3008GearRegistry.Carry);
        Helper.assertTrue(count(Helper, Scp3008GearRegistry.ShelfShortItem.get()) == 1, "dropping did not give back exactly one shelf");
        Helper.assertTrue(count(Helper, Items.COAL) == 9, "dropping did not spill the shelf's contents");
        Helper.succeed();
    }

    private static Block plainMember(TagKey<Block> Tag) {
        Optional<HolderSet.Named<Block>> Listed = BuiltInRegistries.BLOCK.getTag(Tag);
        if (Listed.isEmpty()) {
            return null;
        }
        for (Holder<Block> Entry : Listed.get()) {
            BlockState State = Entry.value().defaultBlockState();
            if (Scp3008CarryParts.isMain(State) && !State.isAir()) {
                return Entry.value();
            }
        }
        return null;
    }

    private static int amount(List<ItemStack> Drops, Item Kind) {
        return Drops.stream().filter(Stack -> Stack.is(Kind)).mapToInt(ItemStack::getCount).sum();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp3008_gear_salvage")
    public static void salvageDropsLegsAndDoors(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos At = Helper.absolutePos(new BlockPos(3, 2, 3));
        Block Table = plainMember(Scp3008GearTags.TableLegs);
        Block Cabinet = plainMember(Scp3008GearTags.CabinetDoors);
        Helper.assertTrue(Table != null && Cabinet != null, "nothing is tagged for salvage");
        List<ItemStack> TableDrops = Block.getDrops(Table.defaultBlockState(), World, At, null);
        Helper.assertTrue(amount(TableDrops, Scp3008GearRegistry.TableLeg.get()) >= 1, BuiltInRegistries.BLOCK.getKey(Table) + " gave no table legs: " + TableDrops);
        Helper.assertTrue(amount(TableDrops, Scp3008GearRegistry.CabinetDoor.get()) == 0 || Table.defaultBlockState().is(Scp3008GearTags.CabinetDoors),
                "a table gave a cabinet door");
        List<ItemStack> CabinetDrops = Block.getDrops(Cabinet.defaultBlockState(), World, At, null);
        Helper.assertTrue(amount(CabinetDrops, Scp3008GearRegistry.CabinetDoor.get()) >= 1, BuiltInRegistries.BLOCK.getKey(Cabinet) + " gave no cabinet doors: " + CabinetDrops);
        ItemStack Silk = new ItemStack(Items.DIAMOND_AXE);
        Silk.enchant(World.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
        List<ItemStack> Gentle = Block.getDrops(Table.defaultBlockState(), World, At, null, null, Silk);
        Helper.assertTrue(amount(Gentle, Scp3008GearRegistry.TableLeg.get()) == 0, "silk touch still pulled the legs off");
        List<ItemStack> Plain = Block.getDrops(Blocks.OAK_PLANKS.defaultBlockState(), World, At, null);
        Helper.assertTrue(amount(Plain, Scp3008GearRegistry.TableLeg.get()) == 0, "an untagged block gave table legs");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp3008_gear_spear")
    public static void spearReachesAndFlies(GameTestHelper Helper) {
        ItemStack Spear = new ItemStack(Scp3008GearRegistry.Spear.get());
        ItemAttributeModifiers Modifiers = Spear.getAttributeModifiers();
        boolean Reach = Modifiers.modifiers().stream().anyMatch(Entry -> Entry.attribute().is(Attributes.ENTITY_INTERACTION_RANGE) && Entry.modifier().id().equals(Scp3008SpearItem.ReachId));
        boolean Hits = Modifiers.modifiers().stream().anyMatch(Entry -> Entry.attribute().is(Attributes.ATTACK_DAMAGE) && Entry.modifier().amount() == Scp3008SpearItem.Damage);
        Helper.assertTrue(Reach && Hits, "the spear is missing its reach or damage");
        Husk Target = Helper.spawn(EntityType.HUSK, new Vec3(8.5, 2.0, 6.5));
        Target.setNoAi(true);
        float Full = Target.getHealth();
        FakePlayer User = user(Helper, 1.5, 6.5, -90.0f, "scp3008_gear_thrower");
        User.setItemInHand(InteractionHand.MAIN_HAND, Spear);
        Scp3008ThrownSpear Thrown = Scp3008SpearItem.throwSpear(Helper.getLevel(), User, Spear);
        Helper.assertTrue(Thrown.isAlive() && User.getMainHandItem().isEmpty(), "the spear did not leave the hand");
        Helper.succeedWhen(() -> Helper.assertTrue(Target.getHealth() < Full, "the thrown spear did not hurt the target"));
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_gear_shield")
    public static void shieldBlocksLikeAShield(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp3008GearTestPlayer User = new Scp3008GearTestPlayer(World, "scp3008_gear_blocker");
        Vec3 Spot = Helper.absoluteVec(new Vec3(2.5, 2.0, 6.5));
        User.moveTo(Spot.x, Spot.y, Spot.z, -90.0f, 0.0f);
        User.setYHeadRot(-90.0f);
        User.setGameMode(GameType.SURVIVAL);
        ItemStack Shield = new ItemStack(Scp3008GearRegistry.Shield.get());
        User.setItemInHand(InteractionHand.OFF_HAND, Shield);
        User.startUsingItem(InteractionHand.OFF_HAND);
        User.holdUseFor(10);
        Helper.assertTrue(User.isBlocking(), "the door shield does not block");
        Husk Attacker = Helper.spawn(EntityType.HUSK, new Vec3(4.5, 2.0, 6.5));
        Attacker.setNoAi(true);
        float Before = User.getHealth();
        User.hurt(World.damageSources().mobAttack(Attacker), 6.0f);
        Helper.assertTrue(User.getHealth() == Before, "a blocked hit still hurt");
        Helper.assertTrue(Shield.getDamageValue() > 0, "blocking did not wear the shield");
        User.stopUsingItem();
        User.invulnerableTime = 0;
        User.hurt(World.damageSources().mobAttack(Attacker), 6.0f);
        Helper.assertTrue(User.getHealth() < Before, "an unblocked hit did nothing");
        User.discard();
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_gear_shelves")
    public static void shelvesStoreStackAndClimb(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Spot = new BlockPos(3, 2, 3);
        Helper.setBlock(Spot, shelf(Direction.NORTH));
        Helper.setBlock(Spot.above(), shelf(Direction.NORTH));
        Helper.assertTrue(Helper.getBlockState(Spot.above()).is(Scp3008GearRegistry.ShelfShort.get()), "a shelf would not stack");
        Helper.assertTrue(Helper.getBlockEntity(Spot) instanceof RandomizableContainer, "the short shelf is not a loot container");
        Scp3008ShelfBlockEntity Stored = Helper.getBlockEntity(Spot);
        Helper.assertTrue(Stored.getContainerSize() == 9, "the short shelf does not hold one row");
        ResourceKey<LootTable> Loot = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace("chests/simple_dungeon"));
        Stored.setLootTable(Loot, 7L);
        Helper.assertTrue(Loot.equals(Stored.getLootTable()), "the shelf did not keep a loot table");
        BlockState State = Helper.getBlockState(Spot);
        VoxelShape Shape = State.getCollisionShape(World, Helper.absolutePos(Spot));
        Helper.assertTrue(!Shape.isEmpty() && !State.isCollisionShapeFullBlock(World, Helper.absolutePos(Spot)), "the shelf collision is wrong");
        Helper.assertTrue(Math.abs(Shape.bounds().minZ - 0.5) < 1.0e-6, "a north facing shelf does not leave its front half open");
        Helper.assertFalse(State.isPathfindable(PathComputationType.LAND), "mobs would path through a shelf");
        FakePlayer Climber = user(Helper, 3.5, 3.2, 180.0f, "scp3008_gear_climber");
        Helper.assertTrue(Climber.onClimbable(), "the open front of a shelf cannot be climbed");
        for (Block Kind : List.<Block>of(Scp3008GearRegistry.ShelfShort.get(), Scp3008GearRegistry.ShelfTall.get())) {
            BlockState Default = Kind.defaultBlockState();
            Helper.assertTrue(Default.is(Scp3008GearTags.Liftable) && Default.is(Scp3008GearTags.Warehouse) && Default.is(Scp3008GearTags.FloorStanding),
                    BuiltInRegistries.BLOCK.getKey(Kind) + " is missing a contract tag");
        }
        Helper.assertTrue(Scp3008GearRegistry.ShelfTall.get().defaultBlockState().is(Scp3008GearTags.Multiblock), "the tall shelf is not tagged as a multiblock");
        BlockPos Tall = new BlockPos(7, 2, 3);
        BlockState Lower = Scp3008GearRegistry.ShelfTall.get().defaultBlockState();
        Helper.setBlock(Tall, Lower);
        Scp3008TallShelfBlock.placeWhole(World, Helper.absolutePos(Tall), Lower);
        Scp3008ShelfBlockEntity Big = Helper.getBlockEntity(Tall);
        Big.setItem(17, new ItemStack(Items.STICK, 3));
        Helper.assertTrue(World.getBlockEntity(Helper.absolutePos(Tall.above())) == null, "the upper half has its own storage");
        World.destroyBlock(Helper.absolutePos(Tall.above()), true);
        Helper.assertTrue(Helper.getBlockState(Tall).isAir(), "breaking the upper half left the lower half");
        Helper.assertTrue(count(Helper, Scp3008GearRegistry.ShelfTallItem.get()) == 1, "breaking a tall shelf did not drop exactly one");
        Helper.assertTrue(count(Helper, Items.STICK) == 3, "breaking a tall shelf did not spill its contents");
        Helper.succeed();
    }
}
