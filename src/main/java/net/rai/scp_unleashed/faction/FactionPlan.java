package net.rai.scp_unleashed.faction;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FactionPlan {
    private static final Logger Log = LogUtils.getLogger();
    public static final char Skip = ' ';

    public record Choice(@Nullable BlockState State, boolean Leave, int Weight) {
    }

    public record Entry(List<Choice> Choices, int Total, boolean Footing, boolean Late, @Nullable ResourceKey<LootTable> Loot) {
        @Nullable
        public Choice pick(long Seed, int X, int Y, int Z) {
            if (Choices.size() == 1) {
                return Choices.get(0);
            }
            double Roll = FactionNoise.unit(Seed, X, Y, Z) * Total;
            for (Choice Option : Choices) {
                Roll -= Option.Weight();
                if (Roll < 0.0) {
                    return Option;
                }
            }
            return Choices.get(Choices.size() - 1);
        }
    }

    private final String Name;
    private final int Width;
    private final int Height;
    private final int Depth;
    private final int AnchorX;
    private final int AnchorY;
    private final int AnchorZ;
    private final int Clear;
    private final int FootingDepth;
    @Nullable
    private final BlockState FootingState;
    private final Map<Character, Entry> Palette;
    private final char[][][] Cells;
    private final Map<String, List<BlockPos>> Marks;

    private FactionPlan(String Name, int Width, int Height, int Depth, int AnchorX, int AnchorY, int AnchorZ, int Clear, int FootingDepth, @Nullable BlockState FootingState,
                        Map<Character, Entry> Palette, char[][][] Cells, Map<String, List<BlockPos>> Marks) {
        this.Name = Name;
        this.Width = Width;
        this.Height = Height;
        this.Depth = Depth;
        this.AnchorX = AnchorX;
        this.AnchorY = AnchorY;
        this.AnchorZ = AnchorZ;
        this.Clear = Clear;
        this.FootingDepth = FootingDepth;
        this.FootingState = FootingState;
        this.Palette = Palette;
        this.Cells = Cells;
        this.Marks = Marks;
    }

    public String name() {
        return Name;
    }

    public int width() {
        return Width;
    }

    public int height() {
        return Height;
    }

    public int depth() {
        return Depth;
    }

    public int anchorX() {
        return AnchorX;
    }

    public int anchorY() {
        return AnchorY;
    }

    public int anchorZ() {
        return AnchorZ;
    }

    public int clear() {
        return Clear;
    }

    public int footingDepth() {
        return FootingDepth;
    }

    @Nullable
    public BlockState footingState() {
        return FootingState;
    }

    public char cell(int X, int Y, int Z) {
        return Cells[Y][Z][X];
    }

    @Nullable
    public Entry entry(char Key) {
        return Palette.get(Key);
    }

    public Map<String, List<BlockPos>> marks() {
        return Marks;
    }

    public FactionGround.Footprint footprint(int Slope, int LenientSlope) {
        return new FactionGround.Footprint(-AnchorX, -AnchorZ, Width - 1 - AnchorX, Depth - 1 - AnchorZ, Height - AnchorY, Slope, LenientSlope);
    }

    @Nullable
    private static BlockState parseState(String Text) {
        if (Text.equals("air")) {
            return Blocks.AIR.defaultBlockState();
        }
        try {
            return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), Text, false).blockState();
        } catch (CommandSyntaxException Problem) {
            Log.warn("faction plan block state {} is invalid: {}", Text, Problem.getMessage());
            return null;
        }
    }

    private static Choice choice(String Text, int Weight) {
        if (Text.equals("skip")) {
            return new Choice(null, true, Weight);
        }
        BlockState State = parseState(Text);
        return State == null ? new Choice(null, true, Weight) : new Choice(State, false, Weight);
    }

    public static FactionPlan parse(String Name, JsonObject Root) {
        JsonArray Size = Root.getAsJsonArray("size");
        int Width = Size.get(0).getAsInt();
        int Height = Size.get(1).getAsInt();
        int Depth = Size.get(2).getAsInt();
        JsonArray Anchor = Root.getAsJsonArray("anchor");
        Map<Character, Entry> Palette = new HashMap<>();
        for (Map.Entry<String, JsonElement> Row : Root.getAsJsonObject("palette").entrySet()) {
            JsonObject Body = Row.getValue().getAsJsonObject();
            List<Choice> Choices = new ArrayList<>();
            if (Body.has("pick")) {
                for (JsonElement Option : Body.getAsJsonArray("pick")) {
                    JsonArray Pair = Option.getAsJsonArray();
                    Choices.add(choice(Pair.get(0).getAsString(), Math.max(1, Pair.get(1).getAsInt())));
                }
            } else {
                Choices.add(choice(Body.get("state").getAsString(), 1));
            }
            int Total = 0;
            for (Choice Option : Choices) {
                Total += Option.Weight();
            }
            ResourceKey<LootTable> Loot = null;
            if (Body.has("loot")) {
                ResourceLocation Table = ResourceLocation.tryParse(Body.get("loot").getAsString());
                if (Table != null) {
                    Loot = ResourceKey.create(Registries.LOOT_TABLE, Table);
                }
            }
            boolean Footing = Body.has("footing") && Body.get("footing").getAsBoolean();
            boolean Late = Body.has("late") && Body.get("late").getAsBoolean();
            Palette.put(Row.getKey().charAt(0), new Entry(Choices, Total, Footing, Late, Loot));
        }
        char[][][] Cells = new char[Height][Depth][Width];
        JsonArray Layers = Root.getAsJsonArray("layers");
        for (int Y = 0; Y < Height; Y++) {
            JsonArray Rows = Layers.get(Y).getAsJsonArray();
            for (int Z = 0; Z < Depth; Z++) {
                String Row = Rows.get(Z).getAsString();
                for (int X = 0; X < Width; X++) {
                    Cells[Y][Z][X] = X < Row.length() ? Row.charAt(X) : Skip;
                }
            }
        }
        Map<String, List<BlockPos>> Marks = new LinkedHashMap<>();
        if (Root.has("marks")) {
            for (Map.Entry<String, JsonElement> Row : Root.getAsJsonObject("marks").entrySet()) {
                List<BlockPos> Points = new ArrayList<>();
                for (JsonElement Point : Row.getValue().getAsJsonArray()) {
                    JsonArray Triple = Point.getAsJsonArray();
                    Points.add(new BlockPos(Triple.get(0).getAsInt(), Triple.get(1).getAsInt(), Triple.get(2).getAsInt()));
                }
                Marks.put(Row.getKey(), Points);
            }
        }
        BlockState FootingState = Root.has("footing") ? parseState(Root.get("footing").getAsString()) : null;
        int Clear = Root.has("clear") ? Root.get("clear").getAsInt() : 4;
        int FootingDepth = Root.has("footing_depth") ? Root.get("footing_depth").getAsInt() : 24;
        return new FactionPlan(Name, Width, Height, Depth, Anchor.get(0).getAsInt(), Anchor.get(1).getAsInt(), Anchor.get(2).getAsInt(), Clear, FootingDepth, FootingState,
                Palette, Cells, Marks);
    }
}
