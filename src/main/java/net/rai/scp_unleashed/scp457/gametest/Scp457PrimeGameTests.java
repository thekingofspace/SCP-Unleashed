package net.rai.scp_unleashed.scp457.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp457.Scp457;
import net.rai.scp_unleashed.scp457.Scp457Arrival;
import net.rai.scp_unleashed.scp457.Scp457AshPileBlock;
import net.rai.scp_unleashed.scp457.Scp457AshPileBlockEntity;
import net.rai.scp_unleashed.scp457.Scp457Registry;
import net.rai.scp_unleashed.scp457.Scp457Spread;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp457PrimeGameTests {
    public static final String Arena = Scp457GameTests.Arena;
    public static final String TestName = "Kindled Test";
    public static final int Settle = Scp457.CollapseTicks + 6;

    private Scp457PrimeGameTests() {
    }

    private static Scp457 prime(GameTestHelper Helper, double X, double Z) {
        Scp457 Burning = Scp457GameTests.burning(Helper, X, Z);
        Burning.setPrime(true);
        Burning.setNoAi(true);
        return Burning;
    }

    @Nullable
    private static BlockPos pile(GameTestHelper Helper) {
        int Base = Scp457GameTests.stand(Helper);
        for (int X = 0; X < 32; X++) {
            for (int Z = 0; Z < 32; Z++) {
                for (int Y = Base - 2; Y <= Base + 2; Y++) {
                    BlockPos Pos = Helper.absolutePos(new BlockPos(X, Y, Z));
                    if (Helper.getLevel().getBlockState(Pos).is(Scp457Registry.AshPile.get())) {
                        return Pos;
                    }
                }
            }
        }
        return null;
    }

    private static int piles(GameTestHelper Helper) {
        int Found = 0;
        int Base = Scp457GameTests.stand(Helper);
        for (int X = 0; X < 32; X++) {
            for (int Z = 0; Z < 32; Z++) {
                for (int Y = Base - 2; Y <= Base + 2; Y++) {
                    if (Helper.getLevel().getBlockState(Helper.absolutePos(new BlockPos(X, Y, Z))).is(Scp457Registry.AshPile.get())) {
                        Found++;
                    }
                }
            }
        }
        return Found;
    }

    @Nullable
    private static CompoundTag remains(GameTestHelper Helper, BlockPos Pos) {
        return Helper.getLevel().getBlockEntity(Pos) instanceof Scp457AshPileBlockEntity Pile ? Pile.remains() : null;
    }

    private static List<Scp457> alive(GameTestHelper Helper) {
        return new ArrayList<>(Helper.getLevel().getEntities(Scp457Registry.Burning.get(), Scp457::isAlive));
    }

    private static InteractionResult strike(FakePlayer User, BlockPos Pos) {
        return User.gameMode.useItemOn(User, User.serverLevel(), User.getMainHandItem(), InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(Pos), Direction.UP, Pos, false));
    }

    private static FakePlayer lighter(GameTestHelper Helper, double X, double Z) {
        FakePlayer User = Scp457GameTests.watcher(Helper, X, Z);
        User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
        return User;
    }

    private static int items(GameTestHelper Helper, BlockPos Pos) {
        return Helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(Pos).inflate(4.0)).size();
    }

    private static void assertPile(GameTestHelper Helper, Scp457 Burning, String What) {
        BlockPos Found = pile(Helper);
        Helper.assertTrue(Burning.isRemoved(), "Prime has not finished collapsing after " + What + Scp457GameTests.state(Burning));
        Helper.assertTrue(Found != null, "Prime left no ash pile after " + What + Scp457GameTests.state(Burning));
        CompoundTag Held = remains(Helper, Found);
        Helper.assertTrue(Held != null && Held.getBoolean("Prime"), "the ash pile from " + What + " does not hold Prime");
        Helper.assertTrue(Scp457GameTests.count(Helper) == 0, "a collapsed Prime is still counted: " + Scp457GameTests.count(Helper));
    }

    @AfterBatch(batch = "scp457_primeAshOnDamage")
    public static void afterDamage(ServerLevel World) {
        Scp457GameTests.calm(World);
    }

    @AfterBatch(batch = "scp457_primeAshInWater")
    public static void afterWater(ServerLevel World) {
        Scp457GameTests.calm(World);
    }

    @AfterBatch(batch = "scp457_primeAshInRain")
    public static void afterRain(ServerLevel World) {
        Scp457GameTests.calm(World);
    }

    @AfterBatch(batch = "scp457_cloneLeavesNoPile")
    public static void afterClone(ServerLevel World) {
        Scp457GameTests.calm(World);
    }

    @AfterBatch(batch = "scp457_relightRevivesPrime")
    public static void afterRelight(ServerLevel World) {
        Scp457GameTests.calm(World);
    }

    @AfterBatch(batch = "scp457_relightKeepsCap")
    public static void afterCap(ServerLevel World) {
        Scp457GameTests.calm(World);
    }

    @AfterBatch(batch = "scp457_pileMoves")
    public static void afterDrops(ServerLevel World) {
        Scp457GameTests.calm(World);
    }

    @AfterBatch(batch = "scp457_primeFlagSaves")
    public static void afterSaves(ServerLevel World) {
        Scp457GameTests.calm(World);
    }

    @AfterBatch(batch = "scp457_primeOrigins")
    public static void afterOrigins(ServerLevel World) {
        Scp457GameTests.calm(World);
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp457_primeAshOnDamage")
    public static void primeAshOnDamage(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp457GameTests.begin(Helper);
        Scp457GameTests.rule(World, GameRules.RULE_DOFIRETICK, true);
        int Base = Scp457GameTests.stand(Helper);
        for (int X = 13; X <= 19; X++) {
            for (int Z = 13; Z <= 19; Z++) {
                Helper.setBlock(new BlockPos(X, Base - 1, Z), Blocks.OAK_PLANKS);
            }
        }
        Scp457 Burning = prime(Helper, 16.5, 16.5);
        Burning.setCustomName(Component.literal(TestName));
        Burning.hurt(World.damageSources().generic(), 1000.0f);
        Helper.assertTrue(Burning.isDeadOrDying(), "lethal damage did not bring Prime down" + Scp457GameTests.state(Burning));
        Helper.assertTrue(!Burning.isRemoved(), "Prime vanished at once instead of collapsing");
        Helper.startSequence()
                .thenWaitUntil(() -> assertPile(Helper, Burning, "lethal damage"))
                .thenExecute(() -> {
                    BlockPos Found = pile(Helper);
                    Helper.assertTrue(Found.equals(Helper.absolutePos(new BlockPos(16, Base, 16))), "the ash pile settled away from where Prime fell: " + Found.toShortString());
                    Helper.assertTrue(!World.getBlockState(Found).getValue(Scp457AshPileBlock.Waterlogged), "a dry collapse left a sodden pile");
                    CompoundTag Held = remains(Helper, Found);
                    Helper.assertTrue(Held.getString("CustomName").contains(TestName), "the ash pile lost Prime's name: " + Held.getString("CustomName"));
                    Helper.assertTrue(items(Helper, Found) == 0, "Prime's collapse dropped loot");
                })
                .thenExecuteAfter(40, () -> {
                    Helper.assertTrue(Scp457GameTests.fires(Helper) == 0, "the ash pile set the planks alight");
                    Helper.assertTrue(piles(Helper) == 1, "expected one ash pile, found " + piles(Helper));
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 140, batch = "scp457_primeAshInWater")
    public static void primeAshInWater(GameTestHelper Helper) {
        Scp457GameTests.begin(Helper);
        Scp457 Burning = prime(Helper, 16.5, 16.5);
        Helper.runAfterDelay(5, () -> Helper.setBlock(new BlockPos(16, Scp457GameTests.stand(Helper), 16), Blocks.WATER));
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Burning.isDeadOrDying() && Burning.deathKind() == Scp457.DeathWater, "water did not put Prime out" + Scp457GameTests.state(Burning)))
                .thenWaitUntil(() -> assertPile(Helper, Burning, "standing in water"))
                .thenExecute(() -> {
                    BlockPos Found = pile(Helper);
                    BlockPos Origin = Helper.absolutePos(new BlockPos(16, Scp457GameTests.stand(Helper), 16));
                    Helper.assertTrue(Found.distManhattan(Origin) <= 8, "the ash pile settled far from where Prime went out: " + Found.toShortString());
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 140, batch = "scp457_primeAshInRain", skyAccess = true)
    public static void primeAshInRain(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp457GameTests.begin(Helper);
        World.setWeatherParameters(0, 6000, true, false);
        World.setRainLevel(1.0f);
        Scp457 Burning = prime(Helper, 16.5, 16.5);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Burning.isDeadOrDying() && Burning.deathKind() == Scp457.DeathWater, "rain did not put Prime out" + Scp457GameTests.state(Burning)))
                .thenExecute(() -> Helper.assertTrue(Burning.tickCount <= Scp457.RainInterval * 3, "rain took " + Burning.tickCount + " ticks to put Prime out"))
                .thenWaitUntil(() -> assertPile(Helper, Burning, "the rain"))
                .thenExecute(() -> Scp457GameTests.clearSkies(World))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp457_cloneLeavesNoPile")
    public static void cloneLeavesNoPile(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp457GameTests.begin(Helper);
        Scp457 Killer = prime(Helper, 6.5, 16.5);
        Villager Bystander = Helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(22.5, Scp457GameTests.stand(Helper), 16.5));
        Bystander.hurt(World.damageSources().source(Scp457Registry.BurnDamage, Killer), 1000.0f);
        Scp457[] Risen = new Scp457[1];
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Scp457GameTests.count(Helper) == 2, "the kill did not raise an offshoot" + Scp457GameTests.state(Killer)))
                .thenExecute(() -> {
                    List<Scp457> Found = alive(Helper);
                    Found.remove(Killer);
                    Helper.assertTrue(Found.size() == 1, "expected one offshoot, found " + Found.size());
                    Risen[0] = Found.get(0);
                    Helper.assertTrue(!Risen[0].isPrime(), "an offshoot rose as Prime");
                    Helper.assertTrue(Killer.isPrime(), "the killer lost its Prime status");
                    Risen[0].hurt(World.damageSources().generic(), 1000.0f);
                })
                .thenExecuteAfter(Settle, () -> {
                    Helper.assertTrue(Risen[0].isRemoved(), "the offshoot did not finish dying" + Scp457GameTests.state(Risen[0]));
                    Helper.assertTrue(piles(Helper) == 0, "the offshoot left an ash pile");
                    Helper.assertTrue(Scp457GameTests.count(Helper) == 1, "expected only Prime left, found " + Scp457GameTests.count(Helper));
                    Scp457 Plain = Scp457GameTests.burning(Helper, 22.5, 6.5);
                    Helper.assertTrue(!Plain.isPrime(), "an instance with no origin came up as Prime");
                    Plain.setNoAi(true);
                    Plain.hurt(World.damageSources().generic(), 1000.0f);
                })
                .thenExecuteAfter(Settle, () -> {
                    Helper.assertTrue(piles(Helper) == 0, "an ordinary instance left an ash pile");
                    Scp457GameTests.tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp457_relightRevivesPrime")
    public static void relightRevivesPrime(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp457GameTests.begin(Helper);
        Scp457 Burning = prime(Helper, 16.5, 16.5);
        Burning.setCustomName(Component.literal(TestName));
        UUID Original = Burning.getUUID();
        FakePlayer User = lighter(Helper, 16.5, 24.5);
        BlockPos[] Spot = new BlockPos[1];
        Burning.hurt(World.damageSources().generic(), 1000.0f);
        Helper.startSequence()
                .thenWaitUntil(() -> assertPile(Helper, Burning, "lethal damage"))
                .thenExecute(() -> {
                    Spot[0] = pile(Helper);
                    InteractionResult Result = strike(User, Spot[0]);
                    Helper.assertTrue(Result.consumesAction(), "striking the pile with flint and steel did nothing: " + Result);
                    Helper.assertTrue(World.getBlockState(Spot[0]).isAir(), "the ash pile is still there after it was relit");
                    List<Scp457> Found = alive(Helper);
                    Helper.assertTrue(Found.size() == 1, "expected one revived Prime, found " + Found.size());
                    Scp457 Back = Found.get(0);
                    Helper.assertTrue(Back.isPrime(), "the revived instance is not Prime");
                    Helper.assertTrue(Back.isRising(), "the revived Prime did not rise");
                    Helper.assertTrue(Back.getHealth() >= Back.getMaxHealth(), "the revived Prime came back hurt: " + Back.getHealth());
                    Helper.assertTrue(Back.hasCustomName() && Back.getCustomName().getString().equals(TestName), "the revived Prime lost its name");
                    Helper.assertTrue(Back.getUUID().equals(Original), "the revived Prime changed identity");
                    Helper.assertTrue(Back.blockPosition().equals(Spot[0]), "Prime rose away from its pile: " + Back.blockPosition().toShortString());
                    Helper.assertTrue(User.getMainHandItem().getDamageValue() == 1, "relighting cost " + User.getMainHandItem().getDamageValue() + " durability");
                    InteractionResult Again = strike(User, Spot[0]);
                    Helper.assertTrue(alive(Helper).size() == 1, "a second strike raised another Prime: " + Again);
                    Helper.assertTrue(User.getMainHandItem().getDamageValue() == 1, "a strike on bare ground still cost durability");
                    BlockPos Sodden = Helper.absolutePos(new BlockPos(8, Scp457GameTests.stand(Helper), 8));
                    World.setBlockAndUpdate(Sodden, Scp457Registry.AshPile.get().defaultBlockState().setValue(Scp457AshPileBlock.Waterlogged, true));
                    strike(User, Sodden);
                    Helper.assertTrue(World.getBlockState(Sodden).is(Scp457Registry.AshPile.get()), "a sodden pile was relit");
                    Helper.assertTrue(alive(Helper).size() == 1, "a sodden pile raised a Prime");
                    Helper.assertTrue(User.getMainHandItem().getDamageValue() == 1, "a failed strike on a sodden pile cost durability");
                    Scp457GameTests.dismiss(Helper, User);
                    Scp457GameTests.tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp457_relightKeepsCap")
    public static void relightKeepsCap(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp457GameTests.begin(Helper);
        List<Scp457> Filled = new ArrayList<>();
        for (int Index = 0; Index < Scp457Spread.MaxPerDimension; Index++) {
            Scp457 Made = Scp457GameTests.burning(Helper, 2.5 + (Index % 8) * 3.0, 3.5 + (Index / 8) * 3.0);
            Made.setNoAi(true);
            Filled.add(Made);
        }
        BlockPos Spot = Helper.absolutePos(new BlockPos(16, Scp457GameTests.stand(Helper), 22));
        World.setBlockAndUpdate(Spot, Scp457Registry.AshPile.get().defaultBlockState());
        FakePlayer User = lighter(Helper, 16.5, 27.5);
        strike(User, Spot);
        Helper.assertTrue(World.getBlockState(Spot).is(Scp457Registry.AshPile.get()), "the pile was relit past the cap");
        Helper.assertTrue(Scp457GameTests.count(Helper) == Scp457Spread.MaxPerDimension, "the cap of " + Scp457Spread.MaxPerDimension + " did not hold: " + Scp457GameTests.count(Helper));
        Helper.assertTrue(User.getMainHandItem().getDamageValue() == 0, "a refused relight cost durability");
        Filled.get(0).discard();
        strike(User, Spot);
        Helper.assertTrue(World.getBlockState(Spot).isAir(), "the pile did not relight once there was room under the cap");
        Helper.assertTrue(Scp457GameTests.count(Helper) == Scp457Spread.MaxPerDimension, "relighting went past the cap: " + Scp457GameTests.count(Helper));
        Helper.assertTrue(alive(Helper).stream().filter(Scp457::isPrime).count() == 1, "the relit pile did not raise exactly one Prime");
        Scp457GameTests.dismiss(Helper, User);
        Scp457GameTests.tidy(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp457_pileMoves")
    public static void pileMoves(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp457GameTests.begin(Helper);
        int Base = Scp457GameTests.stand(Helper);
        BlockPos Spot = Helper.absolutePos(new BlockPos(16, Base, 16));
        BlockPos Elsewhere = Helper.absolutePos(new BlockPos(13, Base, 16));
        BlockState State = Scp457Registry.AshPile.get().defaultBlockState();
        World.setBlockAndUpdate(Spot, State);
        CompoundTag Held = new CompoundTag();
        Held.putString("CustomName", "\"" + TestName + "\"");
        if (World.getBlockEntity(Spot) instanceof Scp457AshPileBlockEntity Pile) {
            Pile.setRemains(Held);
        }
        FakePlayer Survivor = Scp457GameTests.watcher(Helper, 18.5, 16.5);
        FakePlayer Builder = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "scp457_builder"));
        Builder.moveTo(Survivor.getX(), Survivor.getY(), Survivor.getZ() + 2.0, 0.0f, 0.0f);
        Builder.setGameMode(GameType.CREATIVE);
        Helper.assertTrue(State.getDestroyProgress(Survivor, World, Spot) > 0.0f, "a survival player cannot dig the ash pile");
        Helper.assertTrue(State.is(BlockTags.MINEABLE_WITH_SHOVEL), "a shovel is not the tool for the ash pile");
        Helper.assertTrue(State.getBlock().getExplosionResistance() >= 1200.0f, "the ash pile can be blown apart");
        Helper.assertTrue(State.getPistonPushReaction() == PushReaction.BLOCK, "pistons can move or break the ash pile");
        Helper.assertTrue(State.is(BlockTags.WITHER_IMMUNE) && State.is(BlockTags.DRAGON_IMMUNE), "bosses can break the ash pile");
        Helper.assertTrue(Survivor.gameMode.destroyBlock(Spot), "a survival player could not dig up the ash pile");
        Helper.assertTrue(World.getBlockState(Spot).isAir(), "the ash pile is still there after digging");
        List<ItemEntity> Dropped = Helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(Spot).inflate(4.0));
        Helper.assertTrue(Dropped.size() == 1 && Dropped.get(0).getItem().is(Scp457Registry.AshPileItem.get()), "digging the ash pile did not drop exactly one heap");
        ItemStack Carried = Dropped.get(0).getItem();
        Dropped.get(0).discard();
        World.setBlockAndUpdate(Elsewhere, State);
        BlockItem.updateCustomBlockEntityTag(World, Survivor, Elsewhere, Carried);
        CompoundTag Moved = remains(Helper, Elsewhere);
        Helper.assertTrue(Moved != null && Moved.getString("CustomName").contains(TestName), "the moved ash pile lost what it held");
        Helper.assertTrue(Builder.gameMode.destroyBlock(Elsewhere), "a creative player could not clear the ash pile");
        Helper.assertTrue(World.getBlockState(Elsewhere).isAir(), "the ash pile is still there after a creative player broke it");
        Helper.assertTrue(items(Helper, Elsewhere) == 0, "breaking the ash pile in creative dropped an item");
        Scp457GameTests.dismiss(Helper, Survivor);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp457_primeFlagSaves")
    public static void primeFlagSaves(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp457GameTests.begin(Helper);
        Scp457 Burning = prime(Helper, 8.5, 8.5);
        Scp457 Offshoot = Scp457GameTests.burning(Helper, 12.5, 8.5);
        Offshoot.setNoAi(true);
        CompoundTag PrimeSaved = Burning.saveWithoutId(new CompoundTag());
        CompoundTag OffshootSaved = Offshoot.saveWithoutId(new CompoundTag());
        Scp457 PrimeLoaded = Scp457Registry.Burning.get().create(World);
        PrimeLoaded.load(PrimeSaved);
        Scp457 OffshootLoaded = Scp457Registry.Burning.get().create(World);
        OffshootLoaded.load(OffshootSaved);
        Helper.assertTrue(PrimeLoaded.isPrime(), "Prime lost its status through a save and load");
        Helper.assertTrue(!OffshootLoaded.isPrime(), "an offshoot became Prime through a save and load");
        CompoundTag Legacy = PrimeSaved.copy();
        Legacy.remove("Prime");
        Scp457 LegacyLoaded = Scp457Registry.Burning.get().create(World);
        LegacyLoaded.load(Legacy);
        Helper.assertTrue(!LegacyLoaded.isPrime(), "an instance saved before Prime existed loaded as Prime");
        BlockPos Spot = Helper.absolutePos(new BlockPos(20, Scp457GameTests.stand(Helper), 20));
        BlockState State = Scp457Registry.AshPile.get().defaultBlockState();
        World.setBlockAndUpdate(Spot, State);
        Scp457AshPileBlockEntity Pile = (Scp457AshPileBlockEntity) World.getBlockEntity(Spot);
        Pile.setRemains(PrimeSaved);
        CompoundTag PileSaved = Pile.saveWithFullMetadata(World.registryAccess());
        BlockEntity Reloaded = BlockEntity.loadStatic(Spot, State, PileSaved, World.registryAccess());
        Helper.assertTrue(Reloaded instanceof Scp457AshPileBlockEntity Back && Back.holdsRemains() && Back.remains().getBoolean("Prime")
                && Back.remains().getUUID("UUID").equals(Burning.getUUID()), "the ash pile lost what it holds through a save and load");
        Scp457GameTests.tidy(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp457_primeOrigins")
    public static void primeOrigins(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp457GameTests.begin(Helper);
        Scp457 Hatched = Scp457Registry.Burning.get().spawn(World, Helper.absolutePos(new BlockPos(6, Scp457GameTests.stand(Helper), 6)), MobSpawnType.SPAWN_EGG);
        Helper.assertTrue(Hatched != null && Hatched.isPrime(), "an instance from the spawn egg is not Prime");
        Scp457 Summoned = Scp457Registry.Burning.get().spawn(World, Helper.absolutePos(new BlockPos(12, Scp457GameTests.stand(Helper), 6)), MobSpawnType.COMMAND);
        Helper.assertTrue(Summoned != null && Summoned.isPrime(), "an instance from a command is not Prime");
        Vec3 Arrived = Helper.absoluteVec(new Vec3(20.5, Scp457GameTests.stand(Helper), 20.5));
        Helper.assertTrue(Scp457Arrival.place(World, Arrived, 0.0f), "the arrival could not place SCP-457");
        List<? extends Scp457> Found = World.getEntities(Scp457Registry.Burning.get(), Other -> Other.isAlive() && Other.distanceToSqr(Arrived) < 1.0);
        Helper.assertTrue(Found.size() == 1 && Found.get(0).isPrime(), "the instance from the arrival is not Prime");
        Scp457 Risen = Scp457Spread.rise(World, Helper.absoluteVec(new Vec3(26.5, Scp457GameTests.stand(Helper), 10.5)), 0.0f);
        Helper.assertTrue(Risen != null && !Risen.isPrime(), "an instance raised from a kill came up as Prime");
        Scp457GameTests.tidy(Helper);
        Helper.succeed();
    }
}
