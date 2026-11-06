package net.rai.scp_unleashed.scp3008.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.rai.scp_unleashed.scp3008.Scp3008Department;
import net.rai.scp_unleashed.scp3008.Scp3008Registry;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Code;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp3008Catalogue {
    private static final Map<String, List<Block>> Lists = new ConcurrentHashMap<>();
    private static final Map<Block, Boolean> Containers = new ConcurrentHashMap<>();

    private Scp3008Catalogue() {
    }

    private static final Map<String, List<Block>> Suffixed = new ConcurrentHashMap<>();

    public static void forget() {
        Lists.clear();
        Containers.clear();
        Suffixed.clear();
        Scp3008Footprint.forget();
        Scp3008Showrooms.forget();
    }

    public static boolean endsWith(Block Candidate, String Suffix) {
        String Path = BuiltInRegistries.BLOCK.getKey(Candidate).getPath();
        return Path.equals(Suffix) || Path.endsWith("_" + Suffix);
    }

    public static List<Block> matching(int Department, String Suffix) {
        return Suffixed.computeIfAbsent(Department + ":" + Suffix, Ignored -> {
            List<Block> Found = new ArrayList<>();
            List<Integer> Order = new ArrayList<>();
            if (Department >= 0) {
                Order.add(Department);
            }
            for (int Index = 0; Index < Scp3008Department.values().length; Index++) {
                if (Index != Department) {
                    Order.add(Index);
                }
            }
            for (int Index : Order) {
                List<Block> Members = new ArrayList<>(members(Scp3008Department.of(Index).FurnitureTag));
                Members.sort((Left, Right) -> BuiltInRegistries.BLOCK.getKey(Left).compareTo(BuiltInRegistries.BLOCK.getKey(Right)));
                for (Block Candidate : Members) {
                    if (endsWith(Candidate, Suffix) && !Found.contains(Candidate)) {
                        Found.add(Candidate);
                    }
                }
                if (!Found.isEmpty() && Index == Department) {
                    break;
                }
            }
            return List.copyOf(Found);
        });
    }

    public static Block pick(int Department, int Salt, String... Suffixes) {
        for (String Suffix : Suffixes) {
            List<Block> Found = matching(Department, Suffix);
            if (!Found.isEmpty()) {
                return Found.get(Math.floorMod(Salt, Found.size()));
            }
        }
        return null;
    }

    public static TagKey<Block> placementOf(int Role) {
        return switch (Role) {
            case Scp3008Code.RoleWall -> Scp3008Registry.WallMounted;
            case Scp3008Code.RoleCeiling -> Scp3008Registry.CeilingHung;
            default -> Scp3008Registry.FloorStanding;
        };
    }

    public static Set<Block> members(TagKey<Block> Tag) {
        Set<Block> Found = new HashSet<>();
        Optional<HolderSet.Named<Block>> Named = BuiltInRegistries.BLOCK.getTag(Tag);
        Named.ifPresent(Holders -> {
            for (Holder<Block> Entry : Holders) {
                Found.add(Entry.value());
            }
        });
        return Found;
    }

    public static boolean container(Block Candidate) {
        return Containers.computeIfAbsent(Candidate, Key -> {
            if (!(Key instanceof EntityBlock Maker)) {
                return false;
            }
            try {
                BlockEntity Made = Maker.newBlockEntity(BlockPos.ZERO, Key.defaultBlockState());
                return Made instanceof RandomizableContainer;
            } catch (RuntimeException Problem) {
                return false;
            }
        });
    }

    public static boolean multiblock(Block Candidate) {
        return Candidate.defaultBlockState().is(Scp3008Registry.Multiblock);
    }

    public static List<Block> candidates(int Department, int Role) {
        String Key = Department + ":" + Role;
        return Lists.computeIfAbsent(Key, Ignored -> build(Department, Role));
    }

    private static List<Block> build(int Department, int Role) {
        Set<Block> InDepartment = members(Scp3008Department.of(Department).FurnitureTag);
        Set<Block> Placed = members(placementOf(Role));
        List<Block> All = new ArrayList<>();
        for (Block Candidate : InDepartment) {
            if (Placed.contains(Candidate)) {
                All.add(Candidate);
            }
        }
        All.sort((Left, Right) -> BuiltInRegistries.BLOCK.getKey(Left).compareTo(BuiltInRegistries.BLOCK.getKey(Right)));
        if (All.isEmpty()) {
            return List.of();
        }
        List<Block> Singles = new ArrayList<>();
        List<Block> Storage = new ArrayList<>();
        List<Block> SingleStorage = new ArrayList<>();
        for (Block Candidate : All) {
            boolean Single = !multiblock(Candidate);
            boolean Holds = container(Candidate);
            if (Single) {
                Singles.add(Candidate);
            }
            if (Holds) {
                Storage.add(Candidate);
            }
            if (Single && Holds) {
                SingleStorage.add(Candidate);
            }
        }
        if (Role == Scp3008Code.RoleFocal) {
            return List.copyOf(All);
        }
        if (Role == Scp3008Code.RoleContainer) {
            return List.copyOf(!SingleStorage.isEmpty() ? SingleStorage : !Storage.isEmpty() ? Storage : List.of(Scp3008Registry.Crate.get()));
        }
        return List.copyOf(Singles.isEmpty() ? All : Singles);
    }
}
