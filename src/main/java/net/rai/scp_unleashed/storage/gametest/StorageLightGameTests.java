package net.rai.scp_unleashed.storage.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.storage.StorageContainerBlock;
import net.rai.scp_unleashed.storage.StorageEnvironment;
import net.rai.scp_unleashed.storage.StorageLight;
import net.rai.scp_unleashed.storage.StorageRegistry;
import net.rai.scp_unleashed.storage.TallStorageContainerBlock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class StorageLightGameTests {
    public static final String Arena = "scp444_arena";
    public static final int OldForcedLight = 12;
    public static final int SubtleLimit = 8;
    public static final List<String> Shells = List.of("container/shell_normal", "container/shell_normal_stacked", "container/shell_heated", "container/shell_heated_stacked",
            "container/shell_cold", "container/shell_cold_stacked", "container_tall/lower_normal", "container_tall/lower_normal_stacked", "container_tall/lower_heated",
            "container_tall/lower_heated_stacked", "container_tall/lower_cold", "container_tall/lower_cold_stacked", "container_tall/upper_normal", "container_tall/upper_heated",
            "container_tall/upper_cold");
    public static final List<String> CavityTextures = List.of("#interior", "#floor");
    private static final BlockPos Spot = new BlockPos(6, 2, 8);

    private StorageLightGameTests() {
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "storage_light")
    public static void storedItemsFollowTheRoomLight(GameTestHelper Helper) {
        Helper.assertTrue(StorageLight.LampLevel > 0 && StorageLight.LampLevel <= SubtleLimit, "the interior lamp should be a subtle light, got " + StorageLight.LampLevel);
        Helper.assertTrue(StorageLight.LampLevel < OldForcedLight, "the interior lamp is as bright as the old forced block light");
        for (int BlockLevel = 0; BlockLevel <= StorageLight.MaxLevel; BlockLevel++) {
            for (int SkyLevel = 0; SkyLevel <= StorageLight.MaxLevel; SkyLevel++) {
                int Room = StorageLight.pack(BlockLevel, SkyLevel);
                int Inside = StorageLight.interior(Room);
                Helper.assertTrue(StorageLight.sky(Inside) == SkyLevel, "the interior changed the sky light from " + SkyLevel + " to " + StorageLight.sky(Inside));
                Helper.assertTrue(StorageLight.block(Inside) == Math.max(BlockLevel, StorageLight.LampLevel),
                        "block light " + BlockLevel + " became " + StorageLight.block(Inside) + " inside the container");
                Helper.assertTrue(StorageLight.block(Inside) >= BlockLevel, "the interior is darker than the room at block light " + BlockLevel);
            }
        }
        int Dark = StorageLight.interior(StorageLight.pack(0, 0));
        Helper.assertTrue(Dark != StorageLight.FullBright && StorageLight.block(Dark) == StorageLight.LampLevel && StorageLight.sky(Dark) == 0,
                "a pitch dark room should leave only the lamp light inside, got block " + StorageLight.block(Dark) + " sky " + StorageLight.sky(Dark));
        int Day = StorageLight.pack(13, 15);
        Helper.assertTrue(StorageLight.interior(Day) == Day, "a bright room should light stored items exactly like the room");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "storage_light")
    public static void layoutLightNeverDarkens(GameTestHelper Helper) {
        for (int BlockLevel = 0; BlockLevel <= StorageLight.MaxLevel; BlockLevel++) {
            for (int Wanted = -2; Wanted < StorageLight.MaxLevel; Wanted++) {
                int Room = StorageLight.pack(BlockLevel, 9);
                int Lit = StorageLight.atLeast(Room, Wanted);
                Helper.assertTrue(StorageLight.block(Lit) == Math.max(BlockLevel, Math.max(0, Wanted)), "asking for block light " + Wanted + " at room light " + BlockLevel
                        + " gave " + StorageLight.block(Lit));
                Helper.assertTrue(StorageLight.sky(Lit) == 9, "a layout light changed the sky light");
            }
            Helper.assertTrue(StorageLight.atLeast(StorageLight.pack(BlockLevel, 3), StorageLight.MaxLevel) == StorageLight.FullBright,
                    "a full bright layout should stay full bright");
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "storage_light")
    public static void lampLightsTheWholeCavity(GameTestHelper Helper) {
        Path Root = ModList.get().getModFileById(Scp_unleashed.ModId).getFile().findResource("assets", Scp_unleashed.ModId, "models", "block", "storage");
        int Lit = 0;
        int Lamps = 0;
        List<String> Problems = new ArrayList<>();
        for (String Name : Shells) {
            JsonObject Shell;
            try {
                Shell = JsonParser.parseString(Files.readString(Root.resolve(Name + ".json"), StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (IOException | RuntimeException Problem) {
                Helper.fail("could not read the storage model " + Name + ": " + Problem);
                return;
            }
            for (JsonElement Element : Shell.getAsJsonArray("elements")) {
                for (Map.Entry<String, JsonElement> Face : Element.getAsJsonObject().getAsJsonObject("faces").entrySet()) {
                    JsonObject Entry = Face.getValue().getAsJsonObject();
                    String Texture = Entry.get("texture").getAsString();
                    int Light = Entry.has("neoforge_data") && Entry.getAsJsonObject("neoforge_data").has("block_light")
                            ? Entry.getAsJsonObject("neoforge_data").get("block_light").getAsInt() : 0;
                    if (CavityTextures.contains(Texture)) {
                        Lit++;
                        if (Light != StorageLight.LampLevel) {
                            Problems.add(Name + " " + Face.getKey() + " " + Texture + " has block light " + Light);
                        }
                    } else if (Texture.equals("#lamp")) {
                        Lamps++;
                        if (Light != StorageLight.MaxLevel) {
                            Problems.add(Name + " lamp face " + Face.getKey() + " is not emissive");
                        }
                    }
                }
            }
        }
        Helper.assertTrue(Problems.isEmpty(), "the lamp light of the walls and of the stored items differ: " + Problems);
        Helper.assertTrue(Lit >= Shells.size() * 3, "too few cavity faces found: " + Lit);
        Helper.assertTrue(Lamps >= 18, "too few lamp faces found: " + Lamps);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "storage_light")
    public static void containerCellCarriesTheRoomLight(GameTestHelper Helper) {
        BlockPos Absolute = Helper.absolutePos(Spot);
        List<BlockState> States = new ArrayList<>();
        for (Direction Facing : Direction.Plane.HORIZONTAL) {
            for (StorageEnvironment Environment : StorageEnvironment.values()) {
                for (boolean Open : new boolean[]{false, true}) {
                    BlockState Small = StorageRegistry.SmallContainer.get().defaultBlockState().setValue(StorageContainerBlock.Facing, Facing)
                            .setValue(StorageContainerBlock.Environment, Environment).setValue(StorageContainerBlock.Open, Open);
                    BlockState Tall = StorageRegistry.TallContainer.get().defaultBlockState().setValue(StorageContainerBlock.Facing, Facing)
                            .setValue(StorageContainerBlock.Environment, Environment).setValue(StorageContainerBlock.Open, Open);
                    States.add(Small);
                    States.add(Tall);
                    States.add(Tall.setValue(TallStorageContainerBlock.Half, DoubleBlockHalf.UPPER));
                }
            }
        }
        for (BlockState State : States) {
            Helper.assertTrue(State.getLightBlock(Helper.getLevel(), Absolute) == 0, State + " blocks light, so its own cell would be dark and stored items would lose the room light");
            Helper.assertTrue(State.propagatesSkylightDown(Helper.getLevel(), Absolute), State + " stops sky light in its cell");
        }
        Helper.succeed();
    }
}
