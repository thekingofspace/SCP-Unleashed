package net.rai.scp_unleashed.scp001scarlet.king;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletAnchorBlock;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletThrone;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public record ScarletArena(BlockPos Throne, List<BlockPos> Pillars, List<BlockPos> Seats, List<BlockPos> Anchors) {
    public static final int Count = ScarletThrone.Pillars;
    public static final int PillarRadius = ScarletThrone.PillarRadius;
    public static final int PillarHeight = ScarletThrone.PillarHeight;
    public static final int SeatRadius = ScarletThrone.ThroneRadius;
    public static final double FightRadius = ScarletThrone.FloorRadius;
    public static BiFunction<ServerLevel, BlockPos, List<BlockPos>> PillarSource = ScarletThrone::pillars;
    public static BiFunction<ServerLevel, BlockPos, List<BlockPos>> SeatSource = ScarletArena::standInSeats;
    public static BiFunction<ServerLevel, BlockPos, List<BlockPos>> AnchorSource = ScarletThrone::anchorBlocks;

    public static ScarletArena of(ServerLevel World, BlockPos Throne) {
        List<BlockPos> Pillars = PillarSource.apply(World, Throne);
        List<BlockPos> Seats = SeatSource.apply(World, Throne);
        List<BlockPos> Fixed = Pillars.size() == Count ? List.copyOf(Pillars) : standInPillars(World, Throne);
        List<BlockPos> Anchors = AnchorSource.apply(World, Throne);
        return new ScarletArena(Throne.immutable(), Fixed, Seats.size() == Count ? List.copyOf(Seats) : standInSeats(World, Throne),
                Anchors.size() == Count ? List.copyOf(Anchors) : Fixed);
    }

    public Vec3 anchor(int Band) {
        BlockPos Pos = Anchors.get(Band);
        if (Pos.equals(Pillars.get(Band))) {
            return Vec3.atCenterOf(Pos);
        }
        return ScarletAnchorBlock.ring(Pos, ScarletThrone.facing(Pos.getX() - Throne.getX(), Pos.getZ() - Throne.getZ()));
    }

    public static List<BlockPos> standInPillars(ServerLevel World, BlockPos Throne) {
        return ring(Throne, PillarRadius, PillarHeight);
    }

    public static List<BlockPos> standInSeats(ServerLevel World, BlockPos Throne) {
        return ring(Throne, SeatRadius, 1);
    }

    public static BlockPos dais(ServerLevel World, BlockPos Throne) {
        return ScarletThrone.dais(World, Throne);
    }

    public static List<BlockPos> ring(BlockPos Centre, int Radius, int Lift) {
        List<BlockPos> Points = new ArrayList<>();
        for (int Index = 0; Index < Count; Index++) {
            double Angle = Index * Math.PI * 2.0 / Count;
            Points.add(Centre.offset((int) Math.round(Math.sin(Angle) * Radius), Lift, (int) Math.round(-Math.cos(Angle) * Radius)));
        }
        return Points;
    }

    public Vec3 centre() {
        return Vec3.atBottomCenterOf(Throne);
    }

    public CompoundTag save() {
        CompoundTag Tag = new CompoundTag();
        Tag.putLong("Throne", Throne.asLong());
        ListTag PillarTags = new ListTag();
        for (BlockPos Pillar : Pillars) {
            PillarTags.add(LongTag.valueOf(Pillar.asLong()));
        }
        Tag.put("Pillars", PillarTags);
        ListTag SeatTags = new ListTag();
        for (BlockPos Seat : Seats) {
            SeatTags.add(LongTag.valueOf(Seat.asLong()));
        }
        Tag.put("Seats", SeatTags);
        ListTag AnchorTags = new ListTag();
        for (BlockPos Anchor : Anchors) {
            AnchorTags.add(LongTag.valueOf(Anchor.asLong()));
        }
        Tag.put("Anchors", AnchorTags);
        return Tag;
    }

    public static ScarletArena load(CompoundTag Tag) {
        BlockPos Throne = BlockPos.of(Tag.getLong("Throne"));
        List<BlockPos> Pillars = new ArrayList<>();
        for (Tag Entry : Tag.getList("Pillars", Tag.TAG_LONG)) {
            Pillars.add(BlockPos.of(((LongTag) Entry).getAsLong()));
        }
        List<BlockPos> Seats = new ArrayList<>();
        for (Tag Entry : Tag.getList("Seats", Tag.TAG_LONG)) {
            Seats.add(BlockPos.of(((LongTag) Entry).getAsLong()));
        }
        if (Pillars.size() != Count) {
            Pillars = ring(Throne, PillarRadius, PillarHeight);
        }
        if (Seats.size() != Count) {
            Seats = ring(Throne, SeatRadius, 1);
        }
        List<BlockPos> Anchors = new ArrayList<>();
        for (Tag Entry : Tag.getList("Anchors", Tag.TAG_LONG)) {
            Anchors.add(BlockPos.of(((LongTag) Entry).getAsLong()));
        }
        if (Anchors.size() != Count) {
            Anchors = Pillars;
        }
        return new ScarletArena(Throne, Pillars, Seats, Anchors);
    }
}
