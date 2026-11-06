package net.rai.scp_unleashed.daeviteruins.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.InclusiveRange;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentTable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.daeviteruins.plan.RuinCanvas;
import net.rai.scp_unleashed.daeviteruins.plan.RuinDanger;
import net.rai.scp_unleashed.daeviteruins.plan.RuinMat;
import net.rai.scp_unleashed.daeviteruins.plan.RuinPlan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class DaeviteRuinsBuilder {
    public static final int FoundationDepth = 16;
    public static final int CanopyClear = 8;
    public static final int Flags = Block.UPDATE_CLIENTS;
    private static final Direction[] Around = Direction.values();

    private record Cell(int X, int Y, int Z, RuinMat Mat, int Facing, int Variant) {
    }

    private DaeviteRuinsBuilder() {
    }

    public static boolean protectedState(ServerLevel World, BlockPos Pos) {
        BlockState There = World.getBlockState(Pos);
        return !There.isAir() && !DaeviteRuinsPalette.natural(There) || World.getBlockEntity(Pos) != null;
    }

    private static boolean put(ServerLevel World, BlockPos Pos, BlockState State) {
        if (World.isOutsideBuildHeight(Pos) || protectedState(World, Pos)) {
            return false;
        }
        World.setBlock(Pos, State, Flags);
        return true;
    }

    private static BlockState surface(ServerLevel World, BlockPos Origin) {
        BlockState Found = World.getBlockState(Origin);
        if (Found.is(BlockTags.DIRT) || Found.is(BlockTags.SAND) || Found.is(Blocks.SNOW_BLOCK) || Found.is(Blocks.GRAVEL) || Found.is(BlockTags.TERRACOTTA)) {
            return Found;
        }
        return Blocks.GRASS_BLOCK.defaultBlockState();
    }

    private static boolean solid(ServerLevel World, BlockPos Pos) {
        BlockState There = World.getBlockState(Pos);
        return There.getFluidState().isEmpty() && There.isFaceSturdy(World, Pos, Direction.UP) && !There.is(BlockTags.LEAVES);
    }

    public static void build(ServerLevel World, DaeviteRuin Ruin, RuinPlan Plan, BlockPos Origin, Direction Front) {
        RandomSource Random = RandomSource.create(Origin.asLong() ^ World.getSeed() ^ Ruin.Page * 31L);
        BlockState Ground = surface(World, Origin);
        List<Cell> Solid = new ArrayList<>();
        List<Cell> Loose = new ArrayList<>();
        List<Cell> Empty = new ArrayList<>();
        Map<Long, Integer> Lowest = new HashMap<>();
        Map<Long, RuinMat> LowestMat = new HashMap<>();
        Plan.forEach((X, Y, Z, Mat, Facing, Variant) -> {
            Cell Entry = new Cell(X, Y, Z, Mat, Facing, Variant);
            if (Mat.isAir()) {
                Empty.add(Entry);
                return;
            }
            if (Mat.isAttachment() || Mat.shape() == RuinMat.Shape.Fluid) {
                Loose.add(Entry);
            } else {
                Solid.add(Entry);
            }
            long Column = RuinPlan.key(X, 0, Z);
            if (!Mat.isAttachment() && (!Lowest.containsKey(Column) || Y < Lowest.get(Column))) {
                Lowest.put(Column, Y);
                LowestMat.put(Column, Mat);
            }
        });
        for (Cell Entry : Empty) {
            BlockPos Pos = DaeviteRuinsPalette.toWorld(Origin, Front, Entry.X(), Entry.Y(), Entry.Z());
            BlockState There = World.getBlockState(Pos);
            if (!There.isAir() && !protectedState(World, Pos)) {
                World.setBlock(Pos, Blocks.AIR.defaultBlockState(), Flags);
            }
        }
        int Top = Plan.high()[1];
        Set<Long> Columns = new HashSet<>();
        for (Cell Entry : Solid) {
            if (Entry.Y() >= 0) {
                Columns.add(RuinPlan.key(Entry.X(), 0, Entry.Z()));
            }
        }
        for (long Column : Columns) {
            int X = RuinPlan.keyX(Column);
            int Z = RuinPlan.keyZ(Column);
            for (int Y = 1; Y <= Top + CanopyClear; Y++) {
                if (Plan.has(X, Y, Z)) {
                    continue;
                }
                BlockPos Pos = DaeviteRuinsPalette.toWorld(Origin, Front, X, Y, Z);
                if (World.isOutsideBuildHeight(Pos)) {
                    break;
                }
                BlockState There = World.getBlockState(Pos);
                if (!There.isAir() && (There.is(BlockTags.LOGS) || There.is(BlockTags.LEAVES) || There.is(Blocks.VINE) || There.is(Blocks.BAMBOO)
                        || There.is(BlockTags.REPLACEABLE) && Y > 1) && World.getBlockEntity(Pos) == null) {
                    World.setBlock(Pos, Blocks.AIR.defaultBlockState(), Flags);
                }
            }
        }
        Solid.sort(Comparator.comparingInt(Cell::Y));
        for (Cell Entry : Solid) {
            BlockPos Pos = DaeviteRuinsPalette.toWorld(Origin, Front, Entry.X(), Entry.Y(), Entry.Z());
            BlockState State;
            if (Entry.Mat() == RuinMat.Ground) {
                BlockState There = World.getBlockState(Pos);
                if (There.is(BlockTags.DIRT) || There.is(BlockTags.SAND) || There.is(BlockTags.BASE_STONE_OVERWORLD) || There.is(Blocks.SNOW_BLOCK)) {
                    continue;
                }
                State = Ground;
            } else {
                State = DaeviteRuinsPalette.placed(Entry.Mat(), Entry.Facing(), Entry.Variant(), Front);
            }
            put(World, Pos, State);
        }
        for (Map.Entry<Long, Integer> Column : Lowest.entrySet()) {
            int X = RuinPlan.keyX(Column.getKey());
            int Z = RuinPlan.keyZ(Column.getKey());
            RuinMat Base = LowestMat.get(Column.getKey());
            boolean Earthen = Base == RuinMat.Ground || Base == RuinMat.CoarseDirt || Base == RuinMat.Dirt || Base == RuinMat.Gravel || Base == RuinMat.AshPowder
                    || Base == RuinMat.SootPowder || Base == RuinMat.PackedMud || Base == RuinMat.Tuff;
            for (int Depth = 1; Depth <= FoundationDepth; Depth++) {
                BlockPos Pos = DaeviteRuinsPalette.toWorld(Origin, Front, X, Column.getValue() - Depth, Z);
                if (World.isOutsideBuildHeight(Pos) || Plan.has(X, Column.getValue() - Depth, Z) || solid(World, Pos)) {
                    break;
                }
                BlockState Fill = Earthen ? Blocks.DIRT.defaultBlockState() : Random.nextFloat() < 0.6f ? Blocks.SMOOTH_BASALT.defaultBlockState()
                        : Blocks.BLACKSTONE.defaultBlockState();
                put(World, Pos, Fill);
            }
        }
        for (Cell Entry : Loose) {
            BlockPos Pos = DaeviteRuinsPalette.toWorld(Origin, Front, Entry.X(), Entry.Y(), Entry.Z());
            BlockState State;
            if (Entry.Mat() == RuinMat.Growth) {
                State = DaeviteRuinsPalette.growth(World, Pos, Random.nextDouble());
                if (State.isAir()) {
                    continue;
                }
            } else {
                State = DaeviteRuinsPalette.placed(Entry.Mat(), Entry.Facing(), Entry.Variant(), Front);
            }
            if (Entry.Mat().shape() != RuinMat.Shape.Fluid && Entry.Mat().shape() != RuinMat.Shape.Web && !State.canSurvive(World, Pos)) {
                continue;
            }
            put(World, Pos, State);
        }
        int Tier = Plan.tier();
        for (Cell Entry : Solid) {
            BlockPos Pos = DaeviteRuinsPalette.toWorld(Origin, Front, Entry.X(), Entry.Y(), Entry.Z());
            BlockEntity Found = World.getBlockEntity(Pos);
            if (Found == null) {
                continue;
            }
            switch (Entry.Mat()) {
                case Chest, TrappedChest, Barrel, DecoratedPot -> {
                    if (Entry.Variant() != RuinCanvas.LootNone) {
                        RandomizableContainer.setBlockEntityLootTable(World, Random, Pos, DaeviteRuinsLoot.container(Entry.Variant(), Tier));
                    }
                }
                case SuspiciousSand, SuspiciousGravel -> {
                    if (Found instanceof BrushableBlockEntity Buried) {
                        Buried.setLootTable(DaeviteRuinsLoot.dig(Tier), Random.nextLong());
                    }
                }
                case Dispenser -> {
                    if (Found instanceof DispenserBlockEntity Launcher) {
                        load(Launcher, Entry.Variant(), Random);
                    }
                }
                case Spawner -> {
                    if (Found instanceof SpawnerBlockEntity Cage) {
                        configure(World, Pos, Cage, Entry.Variant(), Tier);
                    }
                }
                default -> {
                }
            }
        }
        for (Cell Entry : Solid) {
            BlockPos Pos = DaeviteRuinsPalette.toWorld(Origin, Front, Entry.X(), Entry.Y(), Entry.Z());
            BlockState There = World.getBlockState(Pos);
            Block Kind = There.getBlock();
            if (Kind instanceof WallBlock || Kind instanceof FenceBlock || Kind instanceof IronBarsBlock || Kind instanceof StairBlock || Kind instanceof ChainBlock) {
                BlockState Shaped = Block.updateFromNeighbourShapes(There, World, Pos);
                if (Shaped != There && !Shaped.isAir()) {
                    World.setBlock(Pos, Shaped, Flags);
                }
            }
        }
        for (Cell Entry : Empty) {
            if (Entry.Y() >= 0) {
                continue;
            }
            BlockPos Pos = DaeviteRuinsPalette.toWorld(Origin, Front, Entry.X(), Entry.Y(), Entry.Z());
            for (Direction Side : Around) {
                BlockPos Next = Pos.relative(Side);
                if (!World.getFluidState(Next).isEmpty() && !protectedState(World, Next)) {
                    World.setBlock(Next, Blocks.SMOOTH_BASALT.defaultBlockState(), Flags);
                }
            }
        }
        record(World, Ruin, Plan, Origin, Front);
    }

    private static void load(DispenserBlockEntity Launcher, int Ammo, RandomSource Random) {
        int Slots = 3 + Random.nextInt(3);
        for (int Slot = 0; Slot < Slots; Slot++) {
            ItemStack Stack = switch (Ammo) {
                case RuinDanger.FireCharges -> new ItemStack(Items.FIRE_CHARGE, 3 + Random.nextInt(4));
                case RuinDanger.SlowArrows -> Slot % 2 == 0 ? PotionContents.createItemStack(Items.TIPPED_ARROW, Potions.SLOWNESS).copyWithCount(4 + Random.nextInt(4))
                        : new ItemStack(Items.ARROW, 6 + Random.nextInt(6));
                case RuinDanger.PoisonArrows -> Slot % 2 == 0 ? PotionContents.createItemStack(Items.TIPPED_ARROW, Potions.POISON).copyWithCount(4 + Random.nextInt(4))
                        : new ItemStack(Items.FIRE_CHARGE, 3 + Random.nextInt(3));
                default -> new ItemStack(Items.ARROW, 6 + Random.nextInt(8));
            };
            Launcher.setItem(Slot * 2 % 9, Stack);
        }
        Launcher.setChanged();
    }

    private static void configure(ServerLevel World, BlockPos Pos, SpawnerBlockEntity Cage, int Kind, int Tier) {
        CompoundTag Entity = new CompoundTag();
        Entity.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(DaeviteRuinsGuardians.typeFor(Kind)).toString());
        Optional<EquipmentTable> Gear = Kind == RuinDanger.Blaze ? Optional.empty()
                : Optional.of(new EquipmentTable(DaeviteRuinsLoot.gear(Tier, DaeviteRuinsGuardians.archer(Kind)), dropChances(0.04f, DaeviteRuinsGuardians.archer(Kind))));
        SpawnData Data = new SpawnData(Entity, Optional.of(new SpawnData.CustomSpawnRules(new InclusiveRange<>(0, 15), new InclusiveRange<>(0, 15))), Gear);
        CompoundTag Tag = new CompoundTag();
        SpawnData.CODEC.encodeStart(NbtOps.INSTANCE, Data).result().ifPresent(Encoded -> Tag.put("SpawnData", Encoded));
        Tag.putShort("Delay", (short) 20);
        Tag.putShort("MinSpawnDelay", (short) (Tier >= 5 ? 240 : 320));
        Tag.putShort("MaxSpawnDelay", (short) (Tier >= 5 ? 560 : 720));
        Tag.putShort("SpawnCount", (short) (Tier >= 5 ? 3 : 2));
        Tag.putShort("MaxNearbyEntities", (short) (Tier >= 5 ? 5 : 4));
        Tag.putShort("RequiredPlayerRange", (short) 16);
        Tag.putShort("SpawnRange", (short) 4);
        Cage.getSpawner().load(World, Pos, Tag);
        Cage.setChanged();
        BlockState State = World.getBlockState(Pos);
        World.sendBlockUpdated(Pos, State, State, Block.UPDATE_ALL);
    }

    public static Map<EquipmentSlot, Float> dropChances(float Chance, boolean Archer) {
        Map<EquipmentSlot, Float> Chances = new HashMap<>();
        for (EquipmentSlot Slot : EquipmentSlot.values()) {
            Chances.put(Slot, Archer && Slot == EquipmentSlot.HEAD ? 0.0f : Chance);
        }
        return Chances;
    }

    private static Direction facing(int Facing, Direction Front) {
        Direction Local = switch (Facing) {
            case RuinMat.North -> Direction.NORTH;
            case RuinMat.East -> Direction.EAST;
            case RuinMat.West -> Direction.WEST;
            default -> Direction.SOUTH;
        };
        return DaeviteRuinsPalette.turnFor(Front).rotate(Local);
    }

    private static void record(ServerLevel World, DaeviteRuin Ruin, RuinPlan Plan, BlockPos Origin, Direction Front) {
        Ruin.Site = Origin.immutable();
        Ruin.Front = Front;
        Ruin.Guardians.clear();
        Ruin.Traps.clear();
        for (int[] Spot : Plan.marks(RuinPlan.Guardian)) {
            BlockPos Pos = DaeviteRuinsPalette.toWorld(Origin, Front, Spot[0], Spot[1], Spot[2]);
            Ruin.Guardians.add(new DaeviteRuin.Guardian(Pos, Spot[4], facing(Spot[3], Front).get2DDataValue()));
        }
        Map<Integer, DaeviteRuin.Trap> Groups = new HashMap<>();
        for (String Label : new String[]{RuinDanger.TrapTread, RuinDanger.TrapFall}) {
            for (int[] Spot : Plan.marks(Label)) {
                DaeviteRuin.Trap Entry = Groups.computeIfAbsent(Spot[4], Group -> new DaeviteRuin.Trap(Group, DaeviteRuin.Collapse));
                BlockPos Pos = DaeviteRuinsPalette.toWorld(Origin, Front, Spot[0], Spot[1], Spot[2]);
                (Label.equals(RuinDanger.TrapTread) ? Entry.Triggers : Entry.Effects).add(Pos);
            }
        }
        for (String Label : new String[]{RuinDanger.TrapPlate, RuinDanger.TrapChest, RuinDanger.TrapDispenser}) {
            for (int[] Spot : Plan.marks(Label)) {
                DaeviteRuin.Trap Entry = Groups.computeIfAbsent(Spot[4], Group -> new DaeviteRuin.Trap(Group, DaeviteRuin.Dart));
                BlockPos Pos = DaeviteRuinsPalette.toWorld(Origin, Front, Spot[0], Spot[1], Spot[2]);
                (Label.equals(RuinDanger.TrapDispenser) ? Entry.Effects : Entry.Triggers).add(Pos);
            }
        }
        Ruin.Traps.addAll(Groups.values());
        List<int[]> Centre = Plan.marks(RuinPlan.Centre);
        Ruin.Centre = Centre.isEmpty() ? Origin.above() : DaeviteRuinsPalette.toWorld(Origin, Front, Centre.get(0)[0], Centre.get(0)[1], Centre.get(0)[2]);
        List<int[]> Door = Plan.marks(RuinPlan.Entrance);
        Ruin.Entrance = Door.isEmpty() ? Ruin.Centre : DaeviteRuinsPalette.toWorld(Origin, Front, Door.get(0)[0], Door.get(0)[1], Door.get(0)[2]);
        int[] Low = Plan.low();
        int[] High = Plan.high();
        BlockPos A = DaeviteRuinsPalette.toWorld(Origin, Front, Low[0], Low[1], Low[2]);
        BlockPos B = DaeviteRuinsPalette.toWorld(Origin, Front, High[0], High[1], High[2]);
        Ruin.Low = new BlockPos(Math.min(A.getX(), B.getX()), Math.min(A.getY(), B.getY()), Math.min(A.getZ(), B.getZ()));
        Ruin.High = new BlockPos(Math.max(A.getX(), B.getX()), Math.max(A.getY(), B.getY()), Math.max(A.getZ(), B.getZ()));
        Ruin.State = DaeviteRuin.Built;
        Ruin.Woken = false;
        DaeviteRuinsWorld.get(World.getServer()).setDirty();
    }
}
