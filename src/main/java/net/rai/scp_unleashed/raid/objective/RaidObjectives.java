package net.rai.scp_unleashed.raid.objective;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.raid.Raid;
import net.rai.scp_unleashed.storage.StorageTags;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

public final class RaidObjectives {
    public static final ResourceLocation MarchId = id("march");
    public static final ResourceLocation BreakDoorsId = id("break_doors");
    public static final ResourceLocation SwitchesId = id("flip_switches");
    public static final ResourceLocation TheftId = id("steal");
    public static final ResourceLocation FleeId = id("flee");
    public static final Predicate<BlockState> WoodenDoors = State -> State.is(BlockTags.WOODEN_DOORS) || State.is(BlockTags.WOODEN_TRAPDOORS)
            || State.is(BlockTags.FENCE_GATES);
    public static final Predicate<BlockState> Switches = State -> State.getBlock() instanceof LeverBlock || State.getBlock() instanceof ButtonBlock;
    public static final Predicate<ItemStack> Anomalies = StorageTags::isStorable;

    private record Simple(ResourceLocation Id, BiConsumer<Raid, Mob> Attach) implements RaidObjective {
        @Override
        public ResourceLocation id() {
            return Id;
        }

        @Override
        public void attach(Raid Current, Mob Raider) {
            if (!RaidGoal.attached(Raider, Id)) {
                Attach.accept(Current, Raider);
            }
        }
    }

    private RaidObjectives() {
    }

    private static ResourceLocation id(String Name) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name);
    }

    public static RaidObjective custom(ResourceLocation Id, BiConsumer<Raid, Mob> Attach) {
        return new Simple(Id, Attach);
    }

    public static RaidObjective march(int Priority, double Speed) {
        return new Simple(MarchId, (Current, Raider) -> Raider.goalSelector.addGoal(Priority, new RaidMarchGoal(Raider, MarchId, Speed)));
    }

    public static RaidObjective breakDoors(int Priority, Predicate<BlockState> Breakable, int BreakTicks) {
        return new Simple(BreakDoorsId, (Current, Raider) -> {
            if (Raider.getNavigation() instanceof GroundPathNavigation Ground) {
                Ground.setCanOpenDoors(true);
            }
            Raider.goalSelector.addGoal(Priority, new RaidBreakDoorGoal(Raider, BreakDoorsId, Breakable, BreakTicks));
        });
    }

    public static RaidObjective flipSwitches(int Priority, Predicate<BlockState> Switch, int Radius) {
        return new Simple(SwitchesId, (Current, Raider) -> Raider.goalSelector.addGoal(Priority, new RaidSwitchGoal(Raider, SwitchesId, Switch, Radius)));
    }

    public static RaidObjective steal(int Priority, int FleePriority, Predicate<ItemStack> Wanted, int Radius, double Speed) {
        return new Simple(TheftId, (Current, Raider) -> {
            Raider.goalSelector.addGoal(Priority, new RaidTheftGoal(Raider, TheftId, Wanted, Radius, Speed));
            if (!RaidGoal.attached(Raider, FleeId)) {
                Raider.goalSelector.addGoal(FleePriority, new RaidFleeGoal(Raider, FleeId, Speed * 1.15));
            }
        });
    }
}
