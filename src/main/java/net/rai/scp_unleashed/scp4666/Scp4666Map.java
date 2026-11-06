package net.rai.scp_unleashed.scp4666;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class Scp4666Map {
    public static final ResourceLocation Location = Scp4666Registry.location("scp4666/cabin_map.json");
    public static final ResourceLocation Template = Scp4666Registry.location("scp4666_cabin");
    private static volatile Scp4666Map Cached;

    public record Window(BlockPos Pos, Direction Out, int Width, int Height, String Room) {
        public Vec3 outside(double Distance) {
            Vec3 Middle = Vec3.atBottomCenterOf(Pos).add(Out.getClockWise().getStepX() * (Width - 1) * 0.5, 0.0, Out.getClockWise().getStepZ() * (Width - 1) * 0.5);
            return Middle.add(Out.getStepX() * Distance, 0.0, Out.getStepZ() * Distance);
        }

        public Vec3 glass() {
            Vec3 Middle = Vec3.atCenterOf(Pos).add(Out.getClockWise().getStepX() * (Width - 1) * 0.5, (Height - 1) * 0.5, Out.getClockWise().getStepZ() * (Width - 1) * 0.5);
            return Middle;
        }

        public List<BlockPos> cells() {
            List<BlockPos> Found = new ArrayList<>();
            Direction Along = Out.getClockWise();
            for (int Step = 0; Step < Width; Step++) {
                for (int Rise = 0; Rise < Height; Rise++) {
                    Found.add(Pos.relative(Along, Step).above(Rise));
                }
            }
            return Found;
        }

        public boolean covers(BlockPos Target) {
            return cells().contains(Target);
        }
    }

    public record Bed(BlockPos Head, Direction Facing, String Room) {
    }

    public record Patched(BlockPos Pos, String State, int Since, boolean Fill) {
    }

    public final BlockPos TemplateOrigin;
    public final BlockPos TemplateFar;
    public final BlockPos Centre;
    public final BlockPos FrontDoor;
    public final BlockPos SideDoor;
    public final BlockPos BoxSpot;
    public final BlockPos ExitDoor;
    public final BlockPos Tree;
    public final BlockPos Fireplace;
    public final BlockPos Switch;
    public final BlockPos Gramophone;
    public final BlockPos Calendar;
    public final BlockPos BasementCentre;
    public final BlockPos BasementWake;
    public final BlockPos AtticHatch;
    public final BlockPos AtticPeek;
    public final BlockPos CellarDoor;
    public final BlockPos CellarBehind;
    public final BlockPos Heater;
    public final BlockPos CoalBin;
    public final BlockPos ChoppingBlock;
    public final BlockPos Workbench;
    public final BlockPos Offering;
    public final BlockPos Stove;
    public final BlockPos ChoreList;
    public final List<Patched> Patch;
    public final List<BlockPos> Sills;
    public final List<BlockPos> InnerSills;
    public final int PorchMinX;
    public final int PorchMinZ;
    public final int PorchMaxX;
    public final int PorchMaxZ;
    public final BlockPos Chimney;
    public final BlockPos GateOrigin;
    public final Direction GateFacing;
    public final Vec3 Spawn;
    public final float SpawnYaw;
    public final int Ground;
    public final int Floor;
    public final int CabinMinX;
    public final int CabinMaxX;
    public final int CabinMinZ;
    public final int CabinMaxZ;
    public final int CabinMinY;
    public final int CabinMaxY;
    public final int BasementMinY;
    public final int BasementMaxY;
    public final List<BlockPos> Lamps;
    public final List<Window> Windows;
    public final List<Bed> Beds;
    public final List<BlockPos> Presents;
    public final List<BlockPos> Stockings;
    public final List<BlockPos> BasementStairs;
    public final List<Vec3> RoofPath;
    public final List<BlockPos> Path;

    private Scp4666Map(JsonObject Root) {
        TemplateOrigin = pos(Root, "template_origin");
        TemplateFar = Root.has("template_size") ? TemplateOrigin.offset(of(Root.getAsJsonArray("template_size"))).offset(-1, -1, -1) : TemplateOrigin.offset(32, 20, 24);
        Centre = pos(Root, "centre");
        FrontDoor = pos(Root, "front_door");
        SideDoor = pos(Root, "side_door");
        BoxSpot = pos(Root, "box");
        ExitDoor = pos(Root, "exit_door");
        Tree = pos(Root, "tree");
        Fireplace = pos(Root, "fireplace");
        Switch = pos(Root, "switch");
        Gramophone = pos(Root, "gramophone");
        Calendar = pos(Root, "calendar");
        BasementCentre = pos(Root, "basement_centre");
        BasementWake = pos(Root, "basement_wake");
        AtticHatch = pos(Root, "attic_hatch");
        AtticPeek = pos(Root, "attic_peek");
        CellarDoor = Root.has("cellar_door") ? pos(Root, "cellar_door") : BasementCentre;
        CellarBehind = Root.has("cellar_behind") ? pos(Root, "cellar_behind") : BasementCentre;
        Heater = Root.has("heater") ? pos(Root, "heater") : BasementCentre;
        CoalBin = Root.has("coal_bin") ? pos(Root, "coal_bin") : SideDoor;
        ChoppingBlock = Root.has("chopping_block") ? pos(Root, "chopping_block") : SideDoor;
        Workbench = Root.has("workbench") ? pos(Root, "workbench") : BasementCentre;
        Offering = Root.has("offering") ? pos(Root, "offering") : Tree;
        Stove = Root.has("stove") ? pos(Root, "stove") : Centre;
        ChoreList = Root.has("chore_list") ? pos(Root, "chore_list") : Stove.above();
        List<Patched> Changes = new ArrayList<>();
        if (Root.has("patch")) {
            for (JsonElement Entry : Root.getAsJsonArray("patch")) {
                JsonObject Row = Entry.getAsJsonObject();
                Changes.add(new Patched(of(Row.getAsJsonArray("pos")), Row.get("state").getAsString(), Row.has("since") ? Row.get("since").getAsInt() : 6,
                        Row.has("fill") && Row.get("fill").getAsBoolean()));
            }
        }
        Patch = Collections.unmodifiableList(Changes);
        Sills = Root.has("sills") ? positions(Root.getAsJsonArray("sills")) : List.of();
        InnerSills = Root.has("inner_sills") ? positions(Root.getAsJsonArray("inner_sills")) : List.of();
        JsonArray Porch = Root.has("porch_bounds") ? Root.getAsJsonArray("porch_bounds") : null;
        PorchMinX = Porch == null ? FrontDoor.getX() : Porch.get(0).getAsInt();
        PorchMinZ = Porch == null ? FrontDoor.getZ() : Porch.get(1).getAsInt();
        PorchMaxX = Porch == null ? FrontDoor.getX() : Porch.get(2).getAsInt();
        PorchMaxZ = Porch == null ? FrontDoor.getZ() : Porch.get(3).getAsInt();
        Chimney = pos(Root, "chimney");
        GateOrigin = pos(Root, "gate");
        GateFacing = Direction.byName(Root.get("gate_facing").getAsString());
        Spawn = vec(Root.getAsJsonArray("spawn"));
        SpawnYaw = Root.get("spawn_yaw").getAsFloat();
        Ground = Root.get("ground").getAsInt();
        Floor = Root.get("floor").getAsInt();
        JsonArray Bounds = Root.getAsJsonArray("cabin_bounds");
        CabinMinX = Bounds.get(0).getAsInt();
        CabinMinY = Bounds.get(1).getAsInt();
        CabinMinZ = Bounds.get(2).getAsInt();
        CabinMaxX = Bounds.get(3).getAsInt();
        CabinMaxY = Bounds.get(4).getAsInt();
        CabinMaxZ = Bounds.get(5).getAsInt();
        JsonArray Basement = Root.getAsJsonArray("basement_y");
        BasementMinY = Basement.get(0).getAsInt();
        BasementMaxY = Basement.get(1).getAsInt();
        Lamps = positions(Root.getAsJsonArray("lamps"));
        Presents = positions(Root.getAsJsonArray("presents"));
        Stockings = positions(Root.getAsJsonArray("stockings"));
        BasementStairs = positions(Root.getAsJsonArray("basement_stairs"));
        Path = positions(Root.getAsJsonArray("path"));
        List<Vec3> Roof = new ArrayList<>();
        for (JsonElement Entry : Root.getAsJsonArray("roof_path")) {
            Roof.add(vec(Entry.getAsJsonArray()));
        }
        RoofPath = Collections.unmodifiableList(Roof);
        List<Window> Found = new ArrayList<>();
        for (JsonElement Entry : Root.getAsJsonArray("windows")) {
            JsonObject Row = Entry.getAsJsonObject();
            Found.add(new Window(of(Row.getAsJsonArray("pos")), Direction.byName(Row.get("out").getAsString()), Row.get("width").getAsInt(), Row.get("height").getAsInt(),
                    Row.get("room").getAsString()));
        }
        Windows = Collections.unmodifiableList(Found);
        List<Bed> Sleep = new ArrayList<>();
        for (JsonElement Entry : Root.getAsJsonArray("beds")) {
            JsonObject Row = Entry.getAsJsonObject();
            Sleep.add(new Bed(of(Row.getAsJsonArray("head")), Direction.byName(Row.get("facing").getAsString()), Row.get("room").getAsString()));
        }
        Beds = Collections.unmodifiableList(Sleep);
    }

    private static BlockPos pos(JsonObject Root, String Key) {
        return of(Root.getAsJsonArray(Key));
    }

    private static BlockPos of(JsonArray Values) {
        return new BlockPos(Values.get(0).getAsInt(), Values.get(1).getAsInt(), Values.get(2).getAsInt());
    }

    private static Vec3 vec(JsonArray Values) {
        return new Vec3(Values.get(0).getAsDouble(), Values.get(1).getAsDouble(), Values.get(2).getAsDouble());
    }

    private static List<BlockPos> positions(JsonArray Values) {
        List<BlockPos> Found = new ArrayList<>();
        for (JsonElement Entry : Values) {
            Found.add(of(Entry.getAsJsonArray()));
        }
        return Collections.unmodifiableList(Found);
    }

    public static Scp4666Map get(MinecraftServer Server) {
        Scp4666Map Found = Cached;
        if (Found == null) {
            Found = load(Server);
            Cached = Found;
        }
        return Found;
    }

    public static void forget() {
        Cached = null;
    }

    private static Scp4666Map load(MinecraftServer Server) {
        Optional<Resource> Found = Server.getResourceManager().getResource(Location);
        if (Found.isEmpty()) {
            throw new IllegalStateException("missing SCP-4666 cabin map " + Location);
        }
        try (Reader Input = Found.get().openAsReader()) {
            return new Scp4666Map(JsonParser.parseReader(Input).getAsJsonObject());
        } catch (Exception Problem) {
            throw new IllegalStateException("unreadable SCP-4666 cabin map " + Location, Problem);
        }
    }

    public boolean insideCabin(Vec3 Point) {
        return Point.x >= CabinMinX && Point.x < CabinMaxX + 1 && Point.z >= CabinMinZ && Point.z < CabinMaxZ + 1 && Point.y >= BasementMinY && Point.y < CabinMaxY + 1;
    }

    public boolean onPorch(Vec3 Point) {
        return Point.x >= PorchMinX && Point.x < PorchMaxX + 1 && Point.z >= PorchMinZ && Point.z < PorchMaxZ + 1 && Point.y < Floor + 4;
    }

    public boolean inTheSnow(Vec3 Point) {
        return !insideCabin(Point) && !onPorch(Point);
    }

    public boolean inBasement(Vec3 Point) {
        return insideCabin(Point) && Point.y < Floor;
    }

    public int windowAt(BlockPos Target) {
        for (int Index = 0; Index < Windows.size(); Index++) {
            if (Windows.get(Index).covers(Target)) {
                return Index;
            }
        }
        return -1;
    }

    public boolean nearHeater(Vec3 Point, double Reach) {
        return Vec3.atCenterOf(Heater).distanceToSqr(Point) <= Reach * Reach;
    }

    public boolean nearHearth(Vec3 Point, double Reach) {
        return Vec3.atCenterOf(Fireplace).distanceToSqr(Point) <= Reach * Reach;
    }

    @Nullable
    public Window nearestWindow(Vec3 Point, double Reach) {
        Window Best = null;
        double BestDistance = Reach * Reach;
        for (Window Entry : Windows) {
            double Distance = Entry.glass().distanceToSqr(Point);
            if (Distance < BestDistance) {
                BestDistance = Distance;
                Best = Entry;
            }
        }
        return Best;
    }

    public double distanceFromCentre(Vec3 Point) {
        double Dx = Point.x - (Centre.getX() + 0.5);
        double Dz = Point.z - (Centre.getZ() + 0.5);
        return Math.sqrt(Dx * Dx + Dz * Dz);
    }
}
