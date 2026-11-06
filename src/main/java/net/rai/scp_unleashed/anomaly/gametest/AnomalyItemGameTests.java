package net.rai.scp_unleashed.anomaly.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyDrops;
import net.rai.scp_unleashed.anomaly.PackedAnomalies;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class AnomalyItemGameTests {
    public static final String Arena = "gear_arena";
    public static final ResourceLocation KeyId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp005_key");

    private AnomalyItemGameTests() {
    }

    private static ItemStack anomaly() {
        return new ItemStack(BuiltInRegistries.ITEM.get(KeyId));
    }

    private static ItemEntity drop(GameTestHelper Helper, ItemStack Stack, Vec3 Relative) {
        ServerLevel World = Helper.getLevel();
        Vec3 At = Helper.absoluteVec(Relative);
        ItemEntity Dropped = new ItemEntity(World, At.x, At.y, At.z, Stack);
        Dropped.setDeltaMovement(Vec3.ZERO);
        World.addFreshEntity(Dropped);
        return Dropped;
    }

    private static void basin(GameTestHelper Helper, int X, int Z) {
        for (int Dx = -1; Dx <= 1; Dx++) {
            for (int Dz = -1; Dz <= 1; Dz++) {
                Helper.setBlock(new BlockPos(X + Dx, 2, Z + Dz), Blocks.STONE);
                Helper.setBlock(new BlockPos(X + Dx, 3, Z + Dz), Dx == 0 && Dz == 0 ? Blocks.LAVA : Blocks.STONE);
                Helper.setBlock(new BlockPos(X + Dx, 4, Z + Dz), Blocks.AIR);
                Helper.setBlock(new BlockPos(X + Dx, 5, Z + Dz), Blocks.AIR);
            }
        }
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "anomaly_item_despawn")
    public static void anomalyItemsNeverDespawn(GameTestHelper Helper) {
        Helper.assertTrue(AnomalyDrops.isAnomalous(anomaly()), "the test key is not counted as anomalous");
        ItemEntity Key = drop(Helper, anomaly(), new Vec3(3.5, 4.0, 3.5));
        ItemEntity Dirt = drop(Helper, new ItemStack(Items.DIRT), new Vec3(6.5, 4.0, 3.5));
        Helper.runAfterDelay(5, () -> {
            Helper.assertTrue(Key.getAge() == -32768, "the dropped anomaly will despawn, age " + Key.getAge());
            Helper.assertTrue(Dirt.getAge() >= 0, "a plain dropped item lost its despawn timer");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "anomaly_item_harm")
    public static void anomalyItemsShrugOffCactusAndFire(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ItemEntity Key = drop(Helper, anomaly(), new Vec3(3.5, 4.0, 3.5));
        ItemEntity Dirt = drop(Helper, new ItemStack(Items.DIRT), new Vec3(6.5, 4.0, 3.5));
        Helper.assertTrue(Key.fireImmune(), "the dropped anomaly is not fireproof");
        Helper.assertFalse(Key.hurt(World.damageSources().cactus(), 100.0f), "cactus hurt the dropped anomaly");
        Helper.assertFalse(Key.hurt(World.damageSources().lava(), 100.0f), "lava hurt the dropped anomaly");
        Helper.assertFalse(Key.hurt(World.damageSources().inFire(), 100.0f), "fire hurt the dropped anomaly");
        Helper.assertTrue(Key.isAlive(), "the dropped anomaly was destroyed");
        Dirt.hurt(World.damageSources().cactus(), 100.0f);
        Helper.assertFalse(Dirt.isAlive(), "cactus no longer destroys plain items");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "anomaly_item_lava")
    public static void anomalyItemsSurviveLava(GameTestHelper Helper) {
        basin(Helper, 3, 3);
        basin(Helper, 7, 3);
        ItemEntity Key = drop(Helper, anomaly(), new Vec3(3.5, 3.4, 3.5));
        ItemEntity Dirt = drop(Helper, new ItemStack(Items.DIRT), new Vec3(7.5, 3.4, 3.5));
        Helper.runAfterDelay(80, () -> {
            Helper.assertTrue(Key.isAlive(), "the anomaly burned up in lava");
            Helper.assertFalse(Dirt.isAlive(), "plain items no longer burn in lava");
            Helper.succeed();
        });
    }

    private static ItemStack packed(ItemStack Holder, ItemStack... Inside) {
        Holder.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(Inside)));
        return Holder;
    }

    private static List<ItemStack> contents(ItemStack Holder) {
        List<ItemStack> Inside = new ArrayList<>();
        Holder.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItemsCopy().forEach(Inside::add);
        return Inside;
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "anomaly_item_packed")
    public static void packedAnomaliesComeOut(GameTestHelper Helper) {
        ItemStack Crate = packed(new ItemStack(Items.CHEST), anomaly(), new ItemStack(Items.TORCH, 2));
        ItemStack Box = packed(new ItemStack(Items.RED_SHULKER_BOX), new ItemStack(Items.DIRT, 5), anomaly(), Crate);
        ItemStack Pouch = new ItemStack(Items.BUNDLE);
        Pouch.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(anomaly(), new ItemStack(Items.STRING, 3))));
        ItemStack Chest = packed(new ItemStack(Items.CHEST), anomaly(), Pouch);
        List<ItemStack> Found = new ArrayList<>();
        Helper.assertTrue(PackedAnomalies.unpack(Box, Found), "nothing came out of the shulker box");
        Helper.assertTrue(PackedAnomalies.unpack(Chest, Found), "nothing came out of the item container");
        Helper.assertTrue(Found.size() == 4 && Found.stream().allMatch(AnomalyDrops::isAnomalous), "expected 4 packed anomalies, found " + Found);
        List<ItemStack> BoxLeft = contents(Box);
        Helper.assertTrue(BoxLeft.size() == 2 && BoxLeft.get(0).is(Items.DIRT) && BoxLeft.get(0).getCount() == 5 && BoxLeft.get(1).is(Items.CHEST),
                "the shulker box lost its ordinary contents: " + BoxLeft);
        List<ItemStack> CrateLeft = contents(BoxLeft.get(1));
        Helper.assertTrue(CrateLeft.size() == 1 && CrateLeft.get(0).is(Items.TORCH) && CrateLeft.get(0).getCount() == 2, "the container inside the shulker box kept the wrong items: " + CrateLeft);
        List<ItemStack> ChestLeft = contents(Chest);
        Helper.assertTrue(ChestLeft.size() == 1 && ChestLeft.get(0).is(Items.BUNDLE), "the item container kept the wrong items: " + ChestLeft);
        List<ItemStack> PouchLeft = new ArrayList<>();
        ChestLeft.get(0).getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY).itemsCopy().forEach(PouchLeft::add);
        Helper.assertTrue(PouchLeft.size() == 1 && PouchLeft.get(0).is(Items.STRING) && PouchLeft.get(0).getCount() == 3, "the bundle kept the wrong items: " + PouchLeft);
        Helper.assertFalse(PackedAnomalies.unpack(Box, new ArrayList<>()), "the emptied shulker box gave up more anomalies");
        Helper.assertFalse(PackedAnomalies.unpack(anomaly(), new ArrayList<>()), "an anomaly was opened as a container");
        ItemStack Stuck = packed(new ItemStack(Items.SHULKER_BOX), packed(new ItemStack(Items.SHULKER_BOX), anomaly()));
        List<ItemStack> StuckFound = new ArrayList<>();
        Helper.assertFalse(PackedAnomalies.unpack(Stuck, StuckFound) || !StuckFound.isEmpty(), "an anomaly came out of a shulker box that could not be written back");
        Helper.assertTrue(contents(contents(Stuck).get(0)).stream().anyMatch(AnomalyDrops::isAnomalous), "an anomaly vanished from a shulker box that could not be written back");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "anomaly_item_packed_death")
    public static void deathDropsPackedAnomalies(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        GameRules.BooleanValue Keep = World.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY);
        GameRules.BooleanValue Drop = World.getGameRules().getRule(AnomalyDrops.DropAnomalies);
        boolean KeepBefore = Keep.get();
        boolean DropBefore = Drop.get();
        Vec3 Spot = Helper.absoluteVec(new Vec3(6.5, 2.0, 3.5));
        AABB Around = new AABB(Spot, Spot).inflate(6.0);
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "anomaly_packer"));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        try {
            Keep.set(true, World.getServer());
            Drop.set(true, World.getServer());
            User.getInventory().add(packed(new ItemStack(Items.BLUE_SHULKER_BOX), anomaly(), new ItemStack(Items.DIRT, 4)));
            AnomalyDrops.died(new LivingDeathEvent(User, World.damageSources().generic()));
            List<ItemEntity> Loose = World.getEntitiesOfClass(ItemEntity.class, Around, Found -> AnomalyDrops.isAnomalous(Found.getItem()));
            Helper.assertTrue(Loose.size() == 1, "the packed anomaly did not drop, " + Loose.size() + " loose anomalies");
            ItemStack Kept = ItemStack.EMPTY;
            for (int Slot = 0; Slot < User.getInventory().getContainerSize(); Slot++) {
                if (User.getInventory().getItem(Slot).is(Items.BLUE_SHULKER_BOX)) {
                    Kept = User.getInventory().getItem(Slot);
                }
            }
            Helper.assertFalse(Kept.isEmpty(), "the player lost the shulker box with keepInventory on");
            List<ItemStack> Left = contents(Kept);
            Helper.assertTrue(Left.size() == 1 && Left.get(0).is(Items.DIRT), "the kept shulker box still holds " + Left);
            World.getEntitiesOfClass(ItemEntity.class, Around).forEach(ItemEntity::discard);
        } finally {
            Keep.set(KeepBefore, World.getServer());
            Drop.set(DropBefore, World.getServer());
        }
        Helper.succeed();
    }
}
