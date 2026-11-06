package net.rai.scp_unleashed.scp500.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassFood;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.scp075.Scp075Registry;
import net.rai.scp_unleashed.scp143.Scp143Registry;
import net.rai.scp_unleashed.scp217.Scp217Clockwork;
import net.rai.scp_unleashed.scp253.Scp253Infection;
import net.rai.scp_unleashed.scp500.Scp500BottleBlock;
import net.rai.scp_unleashed.scp500.Scp500BottleBlockEntity;
import net.rai.scp_unleashed.scp500.Scp500BottleItem;
import net.rai.scp_unleashed.scp500.Scp500Registry;
import net.rai.scp_unleashed.scp500.Scp500Stock;
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
public final class Scp500GameTests {
    public static final String Arena = "scp500_arena";
    public static final BlockPos Spot = new BlockPos(3, 2, 3);
    public static final BlockPos Elsewhere = new BlockPos(8, 2, 4);
    public static final BlockPos LockerSpot = new BlockPos(12, 2, 4);
    public static final long ExpectedPeriod = 7L * 24000L;

    private Scp500GameTests() {
    }

    private static ServerPlayer player(GameTestHelper Helper, String Name) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), Name));
        Vec3 Where = Helper.absoluteVec(new Vec3(Spot.getX() + 0.5, Spot.getY(), Spot.getZ() + 2.5));
        User.moveTo(Where.x, Where.y, Where.z, 180.0f, 0.0f);
        World.addNewPlayer(User);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, ServerPlayer User) {
        if (!User.isRemoved()) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    private static Scp500BottleBlockEntity place(GameTestHelper Helper, BlockPos Relative, int Pills) {
        Helper.setBlock(Relative, Scp500Registry.Bottle.get().defaultBlockState().setValue(Scp500BottleBlock.Facing, Direction.SOUTH).setValue(Scp500BottleBlock.Pills, Pills));
        return bottle(Helper, Relative);
    }

    private static Scp500BottleBlockEntity bottle(GameTestHelper Helper, BlockPos Relative) {
        BlockEntity Found = Helper.getLevel().getBlockEntity(Helper.absolutePos(Relative));
        Helper.assertTrue(Found instanceof Scp500BottleBlockEntity, "No SCP-500 bottle entity at " + Relative);
        return (Scp500BottleBlockEntity) Found;
    }

    private static int pillsAt(GameTestHelper Helper, BlockPos Relative) {
        return Scp500BottleBlock.pills(Helper.getLevel().getBlockState(Helper.absolutePos(Relative)));
    }

    private static int carried(ServerPlayer User) {
        return User.getInventory().countItem(Scp500Registry.Pill.get());
    }

    private static void clear(GameTestHelper Helper, BlockPos... Spots) {
        for (BlockPos Relative : Spots) {
            Helper.setBlock(Relative, Blocks.AIR.defaultBlockState());
        }
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp500_takeToEmpty")
    public static void takeToEmpty(GameTestHelper Helper) {
        place(Helper, Spot, Scp500Stock.Full);
        ServerPlayer User = player(Helper, "scp500_taker");
        try {
            User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COBBLESTONE, 4));
            Helper.useBlock(Spot, User);
            Helper.assertTrue(carried(User) == 1, "Using the bottle with a block in hand did not give a pill");
            Helper.assertTrue(User.getMainHandItem().is(Items.COBBLESTONE) && User.getMainHandItem().getCount() == 4, "The held block was used instead of the bottle");
            User.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            for (int Taken = 2; Taken <= Scp500Stock.Full; Taken++) {
                Helper.useBlock(Spot, User);
                Helper.assertTrue(carried(User) == Taken, "Take " + Taken + " left the player with " + carried(User) + " pills");
                Helper.assertTrue(pillsAt(Helper, Spot) == Scp500Stock.Full - Taken, "After take " + Taken + " the bottle shows " + pillsAt(Helper, Spot));
            }
            Helper.assertTrue(pillsAt(Helper, Spot) == 0, "The bottle is not empty after eight takes");
            Helper.useBlock(Spot, User);
            Helper.useBlock(Spot, User);
            Helper.assertTrue(carried(User) == Scp500Stock.Full, "An empty bottle still gave a pill: " + carried(User));
            Helper.assertTrue(pillsAt(Helper, Spot) == 0, "An empty bottle changed its count to " + pillsAt(Helper, Spot));
            Helper.assertFalse(bottle(Helper, Spot).take(), "An empty bottle allowed a take");
        } finally {
            dismiss(Helper, User);
            clear(Helper, Spot);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp500_weeklyRestock")
    public static void weeklyRestock(GameTestHelper Helper) {
        Helper.assertTrue(Scp500Stock.Period == ExpectedPeriod, "The restock period is " + Scp500Stock.Period + " ticks, not seven days");
        long Start = 1000L;
        Scp500Stock Low = new Scp500Stock(2, Start + Scp500Stock.Period);
        Helper.assertTrue(Low.at(Start + Scp500Stock.Period - 1L).Pills() == 2, "The bottle restocked before seven days had passed");
        Scp500Stock Week = Low.at(Start + Scp500Stock.Period);
        Helper.assertTrue(Week.Pills() == Scp500Stock.Full && Week.Restock() == Start + 2L * Scp500Stock.Period, "Seven days did not restock to eight: " + Week);
        Scp500Stock Weeks = Low.at(Start + 3L * Scp500Stock.Period + 500L);
        Helper.assertTrue(Weeks.Pills() == Scp500Stock.Full, "Three missed restocks gave " + Weeks.Pills() + " pills instead of eight");
        Helper.assertTrue(Weeks.Restock() == Start + 4L * Scp500Stock.Period, "Missed restocks broke the weekly rhythm: " + Weeks);
        Helper.assertTrue(new Scp500Stock(Scp500Stock.Full + 5, Start).Pills() == Scp500Stock.Full, "A stock above eight was kept");
        Scp500BottleBlockEntity Bottle = place(Helper, Spot, Scp500Stock.Full);
        ServerLevel World = Helper.getLevel();
        long Now = World.getGameTime();
        for (int Index = 0; Index < 5; Index++) {
            Helper.assertTrue(Bottle.take(), "Take " + Index + " failed on a stocked bottle");
        }
        Helper.assertTrue(pillsAt(Helper, Spot) == 3, "Five takes left " + pillsAt(Helper, Spot) + " pills");
        Helper.assertTrue(Bottle.restock() == Now + Scp500Stock.Period, "A new bottle's restock is not seven days away: " + (Bottle.restock() - Now));
        BlockPos At = Helper.absolutePos(Spot);
        Helper.assertTrue(World.getBlockState(At).isRandomlyTicking(), "A bottle that is not full does not watch for its restock");
        Bottle.refreshAt(Now + Scp500Stock.Period - 1L);
        Helper.assertTrue(pillsAt(Helper, Spot) == 3, "The bottle refilled before seven days had passed");
        Bottle.restockAt(Now - 5L);
        World.getBlockState(At).randomTick(World, At, World.getRandom());
        Helper.assertTrue(pillsAt(Helper, Spot) == Scp500Stock.Full, "The bottle did not refill when its restock came due: " + pillsAt(Helper, Spot));
        Helper.assertTrue(bottle(Helper, Spot).restock() == Now - 5L + Scp500Stock.Period, "The next restock is not a week after the last one");
        Helper.assertFalse(World.getBlockState(At).isRandomlyTicking(), "A full bottle still asks for random ticks");
        for (int Index = 0; Index < 6; Index++) {
            bottle(Helper, Spot).take();
        }
        Helper.assertTrue(pillsAt(Helper, Spot) == 2, "Six takes left " + pillsAt(Helper, Spot) + " pills");
        long Later = Now + 3L * Scp500Stock.Period + 100L;
        Scp500Stock Caught = bottle(Helper, Spot).refreshAt(Later);
        Helper.assertTrue(Caught.Pills() == Scp500Stock.Full && pillsAt(Helper, Spot) == Scp500Stock.Full, "Missed restocks gave " + pillsAt(Helper, Spot) + " pills instead of eight");
        Helper.assertTrue(Caught.Restock() > Later && Caught.Restock() <= Later + Scp500Stock.Period, "The restock after missed weeks is not within the next week");
        clear(Helper, Spot);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp500_keepsStock")
    public static void keepsStock(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp500BottleBlockEntity Bottle = place(Helper, Spot, Scp500Stock.Full);
        ServerPlayer User = player(Helper, "scp500_mover");
        try {
            for (int Index = 0; Index < 3; Index++) {
                Bottle.take();
            }
            long Restock = Bottle.restock();
            BlockPos At = Helper.absolutePos(Spot);
            BlockState State = World.getBlockState(At);
            CompoundTag Saved = Bottle.saveWithFullMetadata(World.registryAccess());
            BlockEntity Loaded = BlockEntity.loadStatic(At, State, Saved, World.registryAccess());
            Helper.assertTrue(Loaded instanceof Scp500BottleBlockEntity Copy && Copy.restock() == Restock, "The restock time did not survive a save");
            BlockState Reloaded = NbtUtils.readBlockState(World.holderLookup(Registries.BLOCK), NbtUtils.writeBlockState(State));
            Helper.assertTrue(Scp500BottleBlock.pills(Reloaded) == 5, "The pill count did not survive a save: " + Scp500BottleBlock.pills(Reloaded));
            List<ItemStack> ByHand = Block.getDrops(State, World, At, Bottle, User, ItemStack.EMPTY);
            List<ItemStack> ByPick = Block.getDrops(State, World, At, Bottle, User, new ItemStack(Items.IRON_PICKAXE));
            for (List<ItemStack> Drops : List.of(ByHand, ByPick)) {
                Helper.assertTrue(Drops.size() == 1 && Scp500BottleItem.is(Drops.get(0)), "Breaking the bottle dropped " + Drops);
                Scp500Stock Carried = Drops.get(0).get(Scp500Registry.Stock.get());
                Helper.assertTrue(Carried != null && Carried.Pills() == 5 && Carried.Restock() == Restock, "The dropped bottle carries " + Carried);
                Helper.assertTrue(Scp500BottleItem.pills(Drops.get(0)) == 5, "The dropped bottle reports the wrong count");
            }
            clear(Helper, Spot);
            ItemStack Carried = ByPick.get(0);
            User.setItemInHand(InteractionHand.MAIN_HAND, Carried);
            BlockPos Floor = At.below();
            InteractionResult Placed = Scp500Registry.BottleItem.get().place(new BlockPlaceContext(World, User, InteractionHand.MAIN_HAND, Carried,
                    new BlockHitResult(Vec3.atCenterOf(Floor).add(0.0, 0.5, 0.0), Direction.UP, Floor, false)));
            Helper.assertTrue(Placed.consumesAction() && World.getBlockState(At).is(Scp500Registry.Bottle.get()),
                    "The carried bottle could not be set down again: " + Placed + ", found " + World.getBlockState(At));
            Helper.assertTrue(pillsAt(Helper, Spot) == 5, "Setting the bottle down again changed its count to " + pillsAt(Helper, Spot));
            Helper.assertTrue(bottle(Helper, Spot).restock() == Restock, "Setting the bottle down again reset its restock time");
            Helper.assertTrue(bottle(Helper, Spot).stockAt(World.getGameTime()).Pills() == 5, "Moving the bottle refilled it");
        } finally {
            dismiss(Helper, User);
            clear(Helper, Spot);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp500_instantSwallow")
    public static void instantSwallow(GameTestHelper Helper) {
        ServerPlayer User = player(Helper, "scp500_swallower");
        try {
            User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Scp500Registry.Pill.get(), 3));
            User.setHealth(6.0f);
            ItemStack Held = User.getMainHandItem();
            InteractionResultHolder<ItemStack> Result = Held.use(Helper.getLevel(), User, InteractionHand.MAIN_HAND);
            Helper.assertTrue(Result.getResult().consumesAction(), "Using a pill did nothing: " + Result.getResult());
            Helper.assertFalse(User.isUsingItem(), "The pill started a held use instead of being taken at once");
            Helper.assertTrue(User.getMainHandItem().getCount() == 2, "The pill was not used up at once: " + User.getMainHandItem());
            Helper.assertTrue(User.getHealth() >= User.getMaxHealth(), "The pill did not act at once: " + User.getHealth());
            Helper.assertTrue(Scp500Registry.Pill.get().getDefaultMaxStackSize() == 64, "Pills do not stack to 64");
        } finally {
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp500_curesEverything")
    public static void curesEverything(GameTestHelper Helper) {
        ServerPlayer User = player(Helper, "scp500_patient");
        try {
            User.addEffect(new MobEffectInstance(MobEffects.POISON, 400, 1));
            User.addEffect(new MobEffectInstance(MobEffects.WITHER, 400, 1));
            User.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 2));
            User.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 0));
            User.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0));
            User.addEffect(new MobEffectInstance(Scp075Registry.Corrosion, 400, 0));
            User.addEffect(new MobEffectInstance(Scp143Registry.Bleeding, 400, 0));
            Helper.assertTrue(Scp253Infection.infect(User), "SCP-253 could not be applied");
            Helper.assertTrue(Scp217Clockwork.infect(User), "SCP-217 could not be applied");
            User.setRemainingFireTicks(120);
            User.setTicksFrozen(80);
            User.setHealth(3.0f);
            User.getFoodData().setFoodLevel(2);
            User.getFoodData().setSaturation(0.0f);
            User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Scp500Registry.Pill.get()));
            User.getMainHandItem().use(Helper.getLevel(), User, InteractionHand.MAIN_HAND);
            Helper.assertTrue(User.getActiveEffects().isEmpty(), "Effects survived the pill: " + User.getActiveEffects());
            Helper.assertFalse(Scp253Infection.isInfected(User), "SCP-253 survived the pill");
            Helper.assertFalse(Scp217Clockwork.isInfected(User), "SCP-217 survived the pill");
            Helper.assertTrue(User.getRemainingFireTicks() <= 0 && User.getTicksFrozen() == 0, "The player is still burning or frozen");
            Helper.assertTrue(User.getHealth() >= User.getMaxHealth(), "Health was not filled: " + User.getHealth());
            Helper.assertTrue(User.getFoodData().getFoodLevel() == 20, "Hunger was not filled: " + User.getFoodData().getFoodLevel());
            Helper.assertTrue(User.getFoodData().getSaturationLevel() >= 20.0f, "Saturation was not filled: " + User.getFoodData().getSaturationLevel());
            Helper.assertTrue(User.getMainHandItem().isEmpty(), "The pill was not used up");
        } finally {
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp500_givenToDClass")
    public static void givenToDClass(GameTestHelper Helper) {
        ServerPlayer User = player(Helper, "scp500_giver");
        DClass Body = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), Elsewhere);
        try {
            Body.addEffect(new MobEffectInstance(MobEffects.POISON, 400, 1));
            Body.addEffect(new MobEffectInstance(Scp143Registry.Bleeding, 400, 0));
            Body.setHealth(4.0f);
            ItemStack Pills = new ItemStack(Scp500Registry.Pill.get(), 2);
            User.setItemInHand(InteractionHand.MAIN_HAND, Pills);
            InteractionResult Result = Pills.interactLivingEntity(User, Body, InteractionHand.MAIN_HAND);
            Helper.assertTrue(Result.consumesAction(), "A D-Class could not be given a pill: " + Result);
            Helper.assertTrue(Pills.getCount() == 1, "Giving a pill did not use one up");
            Helper.assertTrue(Body.getActiveEffects().isEmpty(), "The D-Class kept its effects: " + Body.getActiveEffects());
            Helper.assertTrue(Body.getHealth() >= Body.getMaxHealth(), "The D-Class was not healed: " + Body.getHealth());
            Helper.assertTrue(Body.food().food() == DClassFood.MaxFood, "The D-Class was not fed: " + Body.food().food());
            Helper.assertTrue(Pills.interactLivingEntity(User, User, InteractionHand.MAIN_HAND) == InteractionResult.PASS, "A player could give a pill to themselves through the entity path");
        } finally {
            Body.discard();
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp500_lockerKeepsIt")
    public static void lockerKeepsIt(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Helper.setBlock(LockerSpot, StorageRegistry.SmallContainer.get().defaultBlockState().setValue(StorageContainerBlock.Facing, Direction.NORTH));
        Helper.assertTrue(Helper.getBlockEntity(LockerSpot) instanceof StorageContainerBlockEntity, "no storage locker formed");
        StorageContainerBlockEntity Locker = Helper.getBlockEntity(LockerSpot);
        Scp500Stock Kept = new Scp500Stock(3, World.getGameTime() + 54321L);
        ItemStack Bottle = new ItemStack(Scp500Registry.BottleItem.get());
        Bottle.set(Scp500Registry.Stock.get(), Kept);
        ItemStack Pill = new ItemStack(Scp500Registry.Pill.get(), 5);
        Helper.assertTrue(StorageTags.isStorable(Bottle) && StorageTags.isStorable(Pill), "SCP-500 is not tagged as storable");
        Helper.assertTrue(Locker.refusalFor(Bottle.copyWithCount(1)) == null, "the locker refuses the bottle: " + Locker.refusalFor(Bottle));
        Helper.assertTrue(Locker.refusalFor(Pill.copyWithCount(1)) == null, "the locker refuses a pill: " + Locker.refusalFor(Pill));
        Player Keeper = Helper.makeMockPlayer(GameType.SURVIVAL);
        Keeper.setItemInHand(InteractionHand.MAIN_HAND, Bottle);
        Locker.store(Keeper, Keeper.getMainHandItem());
        Helper.assertTrue(Keeper.getMainHandItem().isEmpty(), "storing the bottle left it in the hand");
        Helper.assertTrue(Scp500BottleItem.is(Locker.peekContents()), "the locker did not take the bottle: " + Locker.peekContents());
        Helper.assertTrue(Kept.equals(Locker.peekContents().get(Scp500Registry.Stock.get())), "the stored bottle lost its stock: " + Locker.peekContents().get(Scp500Registry.Stock.get()));
        BlockPos At = Helper.absolutePos(LockerSpot);
        CompoundTag Saved = Locker.saveWithFullMetadata(World.registryAccess());
        BlockEntity Loaded = BlockEntity.loadStatic(At, World.getBlockState(At), Saved, World.registryAccess());
        Helper.assertTrue(Loaded instanceof StorageContainerBlockEntity Copy && Kept.equals(Copy.peekContents().get(Scp500Registry.Stock.get())),
                "the stored bottle's pill count and restock did not survive a save");
        Locker.take(Keeper, InteractionHand.MAIN_HAND);
        ItemStack Back = Keeper.getMainHandItem();
        Helper.assertTrue(Scp500BottleItem.is(Back) && Kept.equals(Back.get(Scp500Registry.Stock.get())), "the bottle came back out as " + Back + " " + Back.get(Scp500Registry.Stock.get()));
        Helper.assertTrue(Scp500BottleItem.pills(Back) == 3, "the bottle taken back out reports " + Scp500BottleItem.pills(Back) + " pills");
        Helper.assertFalse(Locker.hasContents(), "the locker still holds the bottle after it was taken");
        Helper.setBlock(LockerSpot, Blocks.AIR.defaultBlockState());
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp500_strangerBringsIt")
    public static void strangerBringsIt(GameTestHelper Helper) {
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp500Registry.DocumentId);
        Helper.assertTrue(Document != null, "The SCP-500 dossier did not load");
        Helper.assertTrue(Document.Classification() == DocumentClass.Safe, "SCP-500 is not filed as Safe");
        Helper.assertTrue(Document.Condition() instanceof StrangerEncounterCondition, "SCP-500 does not arrive with the stranger");
        StrangerPlan Plan = ((StrangerEncounterCondition) Document.Condition()).plan();
        Helper.assertTrue(Plan.Gifts().size() == 1, "The stranger brings " + Plan.Gifts().size() + " gifts");
        StrangerGift Gift = Plan.Gifts().get(0);
        ItemStack Stack = Gift.toStack();
        Helper.assertTrue(Scp500BottleItem.is(Stack) && Stack.getCount() == 1, "The stranger's gift is not the bottle: " + Stack);
        Helper.assertTrue(Scp500BottleItem.pills(Stack) == Scp500Stock.Full, "The delivered bottle is not full");
        Helper.assertTrue(AnomalySpawners.spawnerFor(Scp500Registry.DocumentId).isPresent(), "No spawner is registered for SCP-500");
        ServerLevel World = Helper.getLevel();
        ServerPlayer Reader = player(Helper, "scp500_reader");
        try {
            BlockPos Pos = Helper.absolutePos(Elsewhere);
            boolean Spawned = AnomalySpawners.spawnerFor(Scp500Registry.DocumentId).get()
                    .spawn(new AnomalySpawners.Discovery(World, Pos, Reader, Scp500Registry.DocumentId, Document, false));
            Helper.assertTrue(Spawned, "The SCP-500 spawner refused the delivery");
            for (BlockPos Each : BlockPos.betweenClosed(Pos.offset(-10, -2, -6), Pos.offset(10, 4, 6))) {
                Helper.assertFalse(World.getBlockState(Each).is(Scp500Registry.Bottle.get()), "The delivery placed a bottle at " + Each);
            }
        } finally {
            dismiss(Helper, Reader);
        }
        Helper.succeed();
    }
}
