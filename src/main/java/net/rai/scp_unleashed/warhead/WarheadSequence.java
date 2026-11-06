package net.rai.scp_unleashed.warhead;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public final class WarheadSequence {
    private final BlockPos Panel;
    private final long Started;
    private final long Deadline;
    private final List<BlockPos> Targets;
    private boolean Locked;

    public WarheadSequence(BlockPos Panel, long Started, long Deadline, List<BlockPos> Targets) {
        this.Panel = Panel.immutable();
        this.Started = Started;
        this.Deadline = Deadline;
        this.Targets = List.copyOf(Targets);
    }

    public BlockPos getPanel() {
        return Panel;
    }

    public long getStarted() {
        return Started;
    }

    public long getDeadline() {
        return Deadline;
    }

    public List<BlockPos> getTargets() {
        return Targets;
    }

    public boolean isLocked() {
        return Locked;
    }

    public void lock() {
        Locked = true;
    }

    public int ticksLeft(long Now) {
        return (int) Math.max(0L, Deadline - Now);
    }

    public boolean targets(BlockPos Warhead) {
        return Targets.contains(Warhead);
    }

    public CompoundTag save() {
        CompoundTag Saved = new CompoundTag();
        Saved.putLong("Panel", Panel.asLong());
        Saved.putLong("Started", Started);
        Saved.putLong("Deadline", Deadline);
        Saved.putBoolean("Locked", Locked);
        ListTag Positions = new ListTag();
        for (BlockPos Target : Targets) {
            Positions.add(LongTag.valueOf(Target.asLong()));
        }
        Saved.put("Targets", Positions);
        return Saved;
    }

    public static WarheadSequence load(CompoundTag Saved) {
        List<BlockPos> Targets = new ArrayList<>();
        ListTag Positions = Saved.getList("Targets", Tag.TAG_LONG);
        for (int Index = 0; Index < Positions.size(); Index++) {
            Targets.add(BlockPos.of(((LongTag) Positions.get(Index)).getAsLong()));
        }
        WarheadSequence Sequence = new WarheadSequence(BlockPos.of(Saved.getLong("Panel")), Saved.getLong("Started"), Saved.getLong("Deadline"), Targets);
        if (Saved.getBoolean("Locked")) {
            Sequence.lock();
        }
        return Sequence;
    }
}
