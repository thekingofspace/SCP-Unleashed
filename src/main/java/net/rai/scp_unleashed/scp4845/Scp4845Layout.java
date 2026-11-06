package net.rai.scp_unleashed.scp4845;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class Scp4845Layout {
    public static final ResourceLocation Location = Scp4845Registry.location("scp4845/restaurant_map.json");
    public static final ResourceLocation Template = Scp4845Registry.location("scp4845_restaurant");
    private static volatile Scp4845Layout Cached;

    public record Station(String Kind, BlockPos Block, BlockPos Stand, Direction Facing) {
    }

    public record Seat(BlockPos Pos, Direction Facing) {
    }

    public final Vec3i Size;
    public final BlockPos Anchor;
    public final int[] Building;
    public final List<int[]> Air;
    public final int[] Inside;
    public final List<BlockPos> Entrance;
    public final BlockPos Doorway;
    public final Seat CounterWorker;
    public final List<BlockPos> Registers;
    public final List<BlockPos> Counters;
    public final Seat Manager;
    public final BlockPos Desk;
    public final List<Seat> BreakSeats;
    public final List<Station> Stations;
    public final List<BlockPos> Mop;
    public final List<BlockPos> Chat;
    public final List<BlockPos> KitchenSpawns;
    public final BlockPos Restaff;
    public final BlockPos Fridge;
    public final BlockPos Speaker;
    public final BlockPos DriveWindow;
    public final int[] SignBox;

    private Scp4845Layout(JsonObject Root) {
        Size = of(Root.getAsJsonArray("size"));
        Anchor = of(Root.getAsJsonArray("anchor"));
        Building = box(Root.getAsJsonArray("building"));
        List<int[]> Boxes = new ArrayList<>();
        for (JsonElement Entry : Root.getAsJsonArray("air")) {
            Boxes.add(box(Entry.getAsJsonArray()));
        }
        Air = Collections.unmodifiableList(Boxes);
        Inside = box(Root.getAsJsonArray("inside"));
        Entrance = positions(Root.getAsJsonArray("entrance"));
        Doorway = of(Root.getAsJsonArray("doorway"));
        CounterWorker = seat(Root.getAsJsonObject("counter_worker"), "pos");
        Registers = positions(Root.getAsJsonArray("registers"));
        Counters = positions(Root.getAsJsonArray("counters"));
        JsonObject Boss = Root.getAsJsonObject("manager");
        Manager = seat(Boss, "seat");
        Desk = of(Boss.getAsJsonArray("desk"));
        List<Seat> Seats = new ArrayList<>();
        for (JsonElement Entry : Root.getAsJsonArray("break_seats")) {
            Seats.add(seat(Entry.getAsJsonObject(), "seat"));
        }
        BreakSeats = Collections.unmodifiableList(Seats);
        List<Station> Found = new ArrayList<>();
        for (JsonElement Entry : Root.getAsJsonArray("stations")) {
            JsonObject Row = Entry.getAsJsonObject();
            Found.add(new Station(Row.get("kind").getAsString(), of(Row.getAsJsonArray("block")), of(Row.getAsJsonArray("stand")), direction(Row.get("facing").getAsString())));
        }
        Stations = Collections.unmodifiableList(Found);
        Mop = positions(Root.getAsJsonArray("mop"));
        Chat = positions(Root.getAsJsonArray("chat"));
        KitchenSpawns = positions(Root.getAsJsonArray("kitchen_spawns"));
        Restaff = of(Root.getAsJsonArray("restaff"));
        Fridge = of(Root.getAsJsonArray("fridge"));
        Speaker = of(Root.getAsJsonArray("speaker"));
        DriveWindow = of(Root.getAsJsonArray("drive_window"));
        SignBox = box(Root.getAsJsonArray("sign"));
    }

    private static Direction direction(String Name) {
        Direction Found = Direction.byName(Name);
        return Found != null && Found.getAxis().isHorizontal() ? Found : Direction.NORTH;
    }

    private static Seat seat(JsonObject Row, String Key) {
        return new Seat(of(Row.getAsJsonArray(Key)), direction(Row.get("facing").getAsString()));
    }

    private static BlockPos of(JsonArray Values) {
        return new BlockPos(Values.get(0).getAsInt(), Values.get(1).getAsInt(), Values.get(2).getAsInt());
    }

    private static int[] box(JsonArray Values) {
        int[] Made = new int[6];
        for (int Index = 0; Index < 6; Index++) {
            Made[Index] = Values.get(Index).getAsInt();
        }
        return Made;
    }

    private static List<BlockPos> positions(JsonArray Values) {
        List<BlockPos> Found = new ArrayList<>();
        for (JsonElement Entry : Values) {
            Found.add(of(Entry.getAsJsonArray()));
        }
        return Collections.unmodifiableList(Found);
    }

    public List<Station> stations(String Kind) {
        List<Station> Found = new ArrayList<>();
        for (Station Entry : Stations) {
            if (Entry.Kind().equals(Kind)) {
                Found.add(Entry);
            }
        }
        return Found;
    }

    public static Scp4845Layout get(MinecraftServer Server) {
        Scp4845Layout Found = Cached;
        if (Found == null) {
            Found = load(Server);
            Cached = Found;
        }
        return Found;
    }

    public static void forget() {
        Cached = null;
    }

    private static Scp4845Layout load(MinecraftServer Server) {
        Optional<Resource> Found = Server.getResourceManager().getResource(Location);
        if (Found.isEmpty()) {
            throw new IllegalStateException("Missing SCP-4845 restaurant map " + Location);
        }
        try (Reader Input = Found.get().openAsReader()) {
            return new Scp4845Layout(JsonParser.parseReader(Input).getAsJsonObject());
        } catch (IOException Problem) {
            throw new IllegalStateException("Could not read SCP-4845 restaurant map", Problem);
        }
    }
}
