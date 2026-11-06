package net.rai.scp_unleashed.wire;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;

public record WireLink(BlockPos Target, Direction TargetFacing, int Slot, WireTier Tier, boolean TargetBlock, @Nullable Direction LocalFace, boolean Hidden) {
    public static final Codec<WireLink> LinkCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            BlockPos.CODEC.fieldOf("target").forGetter(WireLink::Target),
            Direction.CODEC.fieldOf("facing").forGetter(WireLink::TargetFacing),
            Codec.INT.optionalFieldOf("slot", 0).forGetter(WireLink::Slot),
            WireTier.TierCodec.optionalFieldOf("tier", WireTier.Coated).forGetter(WireLink::Tier),
            Codec.BOOL.optionalFieldOf("block", false).forGetter(WireLink::TargetBlock),
            Direction.CODEC.optionalFieldOf("local").forGetter(Link -> Optional.ofNullable(Link.LocalFace())),
            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(WireLink::Hidden)
    ).apply(Instance, (Target, Facing, Slot, Tier, Block, Local, Hidden) -> new WireLink(Target, Facing, Slot, Tier, Block, Local.orElse(null), Hidden)));

    public WireLink(BlockPos Target, Direction TargetFacing, int Slot, WireTier Tier, boolean TargetBlock, @Nullable Direction LocalFace) {
        this(Target, TargetFacing, Slot, Tier, TargetBlock, LocalFace, false);
    }

    public WireLink withHidden(boolean Hide) {
        return new WireLink(Target, TargetFacing, Slot, Tier, TargetBlock, LocalFace, Hide);
    }

    public Vec3 targetAttach() {
        return Wires.attachPoint(Target, TargetFacing, TargetBlock);
    }

    public static int freeSlot(Collection<WireLink> First, Collection<WireLink> Second) {
        int Slot = 0;
        while (usesSlot(First, Slot) || usesSlot(Second, Slot)) {
            Slot++;
        }
        return Slot;
    }

    private static boolean usesSlot(Collection<WireLink> Links, int Slot) {
        for (WireLink Link : Links) {
            if (Link.Slot() == Slot) {
                return true;
            }
        }
        return false;
    }
}
