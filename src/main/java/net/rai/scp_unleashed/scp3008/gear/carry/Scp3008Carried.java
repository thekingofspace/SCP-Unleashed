package net.rai.scp_unleashed.scp3008.gear.carry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;

public record Scp3008Carried(List<Part> Parts, Optional<Direction> Facing, int Turns) {
    public static final Scp3008Carried Empty = new Scp3008Carried(List.of(), Optional.empty(), 0);

    public record Part(BlockPos Offset, BlockState State, CompoundTag Data) {
        public static final Codec<Part> PartCodec = RecordCodecBuilder.create(Instance -> Instance.group(
                BlockPos.CODEC.fieldOf("offset").forGetter(Part::Offset),
                BlockState.CODEC.fieldOf("state").forGetter(Part::State),
                CompoundTag.CODEC.optionalFieldOf("data", new CompoundTag()).forGetter(Part::Data)).apply(Instance, Part::new));
    }

    public static final Codec<Scp3008Carried> CarriedCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Part.PartCodec.listOf().fieldOf("parts").forGetter(Scp3008Carried::Parts),
            Direction.CODEC.optionalFieldOf("facing").forGetter(Scp3008Carried::Facing),
            Codec.INT.optionalFieldOf("turns", 0).forGetter(Scp3008Carried::Turns)).apply(Instance, Scp3008Carried::new));

    public boolean isEmpty() {
        return Parts.isEmpty();
    }

    public Scp3008Carried turned() {
        return new Scp3008Carried(Parts, Facing, (Turns + 1) % 4);
    }
}
