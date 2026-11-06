package net.rai.scp_unleashed.scp001day.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.rai.scp_unleashed.scp001day.world.Scp001DayPalette;
import net.rai.scp_unleashed.scp001day.plan.Scp001DaySupport;
import net.rai.scp_unleashed.scp001day.plan.Scp001DayCode;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Quaternionf;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.ArchiveRules;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.ArchivePool;
import net.rai.scp_unleashed.archive.document.DocumentClaims;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.archive.document.DocumentRoller;
import net.rai.scp_unleashed.gateway.GatewayDestination;
import net.rai.scp_unleashed.gateway.GatewayStash;
import net.rai.scp_unleashed.gateway.GatewayTravel;
import net.rai.scp_unleashed.gateway.gametest.GatewayGameTests;
import net.rai.scp_unleashed.scp001.Scp001Registry;
import net.rai.scp_unleashed.scp001day.Scp001DayDestination;
import net.rai.scp_unleashed.scp001day.Scp001DayEvents;
import net.rai.scp_unleashed.scp001day.Scp001DayHunger;
import net.rai.scp_unleashed.scp001day.Scp001DayRegistry;
import net.rai.scp_unleashed.scp001day.Scp001DaySunlight;
import net.rai.scp_unleashed.scp001day.Scp001DayTurning;
import net.rai.scp_unleashed.scp001day.Scp001DayVoice;
import net.rai.scp_unleashed.scp001day.Scp001DayWorld;
import net.rai.scp_unleashed.scp001day.entity.Scp001DayFlesh;
import net.rai.scp_unleashed.scp001day.entity.Scp001DayForm;
import net.rai.scp_unleashed.scp001day.entity.Scp001DayFusion;
import net.rai.scp_unleashed.scp001day.entity.Scp001DayRift;
import net.rai.scp_unleashed.scp001day.item.Scp001DayTinItem;
import net.rai.scp_unleashed.scp001day.item.Scp001DayWrapItem;
import net.rai.scp_unleashed.scp001day.plan.Scp001DayPlan;
import net.rai.scp_unleashed.scp001day.sun.Scp001DayExposure;
import net.rai.scp_unleashed.scp001day.sun.Scp001DayFarField;
import net.rai.scp_unleashed.scp001day.sun.Scp001DayRay;
import net.rai.scp_unleashed.scp001day.sun.Scp001DayShade;
import net.rai.scp_unleashed.scp001day.sun.Scp001DaySun;
import net.rai.scp_unleashed.scp001day.sun.SunVector;
import net.rai.scp_unleashed.scp001day.world.Scp001DayChunkGenerator;
import net.rai.scp_unleashed.scp538.gametest.Scp538TestPlayer;
import net.rai.scp_unleashed.voice.VoiceArchive;
import net.rai.scp_unleashed.voice.VoicePlayback;
import net.rai.scp_unleashed.voice.VoicePolicy;
import net.rai.scp_unleashed.voice.gametest.VoiceArchiveGameTests;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp001DayGameTests {
    public static final String Arena = "scp001day_arena";
    public static final SunVector Overhead = new SunVector(0.0, 1.0, 0.0);
    public static final int Floor = 100;

    private Scp001DayGameTests() {
    }

    public static ServerLevel standIn(GameTestHelper Helper) {
        MinecraftServer Server = Helper.getLevel().getServer();
        Scp001DayDestination.standIn(Level.END);
        GatewayGameTests.whenDone(Helper, () -> Scp001DayWorld.standIn(null));
        return Server.getLevel(Level.END);
    }

    public static BlockPos base(int Slot) {
        return new BlockPos(Scp001DayWorld.StandinBaseX + Slot * 64, Floor, Scp001DayWorld.StandinBaseZ);
    }

    public static void fill(ServerLevel World, BlockPos Low, BlockPos High, BlockState State) {
        for (int X = Low.getX(); X <= High.getX(); X++) {
            for (int Z = Low.getZ(); Z <= High.getZ(); Z++) {
                World.getChunk(X >> 4, Z >> 4);
                for (int Y = Low.getY(); Y <= High.getY(); Y++) {
                    World.setBlock(new BlockPos(X, Y, Z), State, 2);
                }
            }
        }
    }

    public static void clear(ServerLevel World, BlockPos Origin) {
        fill(World, Origin.offset(-4, -2, -4), Origin.offset(20, 12, 20), Blocks.AIR.defaultBlockState());
    }

    public static Scp538TestPlayer player(GameTestHelper Helper, ServerLevel World, Vec3 Spot, String Name) {
        Scp538TestPlayer User = new Scp538TestPlayer(World, Name);
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        GatewayGameTests.whenDone(Helper, () -> {
            Scp001DaySunlight.forget(User);
            User.discard();
        });
        return User;
    }

    public static double at(ServerLevel World, double X, double Y, double Z, SunVector Sun) {
        return Scp001DayExposure.point(World, Scp001DayFarField.Open, X, Y, Z, Sun);
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp001day_sunSharedAndDeterministic")
    public static void sunSharedAndDeterministic(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            SunVector First = Scp001DaySun.direction(6002L);
            SunVector Second = Scp001DaySun.direction(6004L);
            Helper.assertTrue(First.equals(Second), "two day times inside one sun step gave different directions");
            Helper.assertTrue(Scp001DaySun.direction(6000L + Scp001DaySun.CycleTicks * 3).equals(Scp001DaySun.direction(6000L)), "the sun cycle does not repeat");
            Helper.assertTrue(Math.abs(Scp001DaySun.elevation(Scp001DaySun.direction(Scp001DaySun.NoonTicks)) - 63.6) < 0.05, "noon is not 63.6 degrees high");
            Helper.assertTrue(Scp001DaySun.direction(Scp001DaySun.NoonTicks).Z() > 0.4, "the noon sun is not in the south");
            Helper.assertTrue(!Scp001DaySun.lethal(Scp001DaySun.direction(18000L)), "midnight sun is lethal");
            Helper.assertTrue(Scp001DaySun.lethal(Scp001DaySun.direction(1000L)), "morning sun is not lethal");
            double Share = Scp001DaySun.daylightShare();
            Helper.assertTrue(Share > 0.58 && Share < 0.66, "the lethal day share drifted: " + Share);
            Helper.assertTrue(!Scp001DaySun.lethal(Scp001DaySun.direction(Scp001DaySun.duskTicks() + 10L)), "the sun is lethal after dusk");
            Helper.assertTrue(Scp001DaySun.lethal(Scp001DaySun.direction(Scp001DaySun.dawnTicks() + 10L)), "the sun is not lethal after dawn");
            Helper.assertTrue(Scp001DaySun.intensity(Scp001DaySun.direction(Scp001DaySun.dawnTicks() + 40L)) < 1.0, "dawn light does not ramp");
            ServerLevel World = Helper.getLevel();
            Helper.assertTrue(Scp001DaySunlight.sun(World).equals(Scp001DaySun.direction(World.getDayTime())), "the server sun is not the shared sun");
            SunVector Sun = Scp001DaySun.direction(9000L);
            Scp001DayRay.Field Open = (X, Y, Z) -> Y == 150 && X == 10 ? Scp001DayRay.Solid : Scp001DayRay.Clear;
            double Again = Scp001DayRay.transmit(Open, Scp001DayFarField.Open, 10.5, 120.0, 10.5, Sun, 320);
            Helper.assertTrue(Again == Scp001DayRay.transmit(Open, Scp001DayFarField.Open, 10.5, 120.0, 10.5, Sun, 320), "the trace is not deterministic");
            Helper.assertTrue(Scp001DayRay.transmit(Open, Scp001DayFarField.Open, 10.5, 120.0, 10.5, Scp001DaySun.direction(18000L), 320) == 0.0, "the trace lights points at night");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp001day_exposureMatchesHandRooms")
    public static void exposureMatchesHandRooms(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerLevel World = standIn(Helper);
            BlockPos Origin = base(1);
            clear(World, Origin);
            BlockState[] Ceilings = {Blocks.STONE.defaultBlockState(), Blocks.AIR.defaultBlockState(), Blocks.GLASS.defaultBlockState(), Blocks.GLASS_PANE.defaultBlockState(),
                    Blocks.OAK_LEAVES.defaultBlockState(), Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.BOTTOM),
                    Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP), Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM),
                    Blocks.OAK_FENCE.defaultBlockState(), Blocks.TINTED_GLASS.defaultBlockState(), Blocks.IRON_BARS.defaultBlockState()};
            double[] Expected = {0.0, 1.0, 1.0, 1.0, (Scp001DayShade.LeafPass / 255.0) * (Scp001DayShade.LeafPass / 255.0), 0.0, 0.0, 0.0, 1.0, 0.0, 1.0};
            for (int Index = 0; Index < Ceilings.length; Index++) {
                BlockPos Column = Origin.offset(Index * 2, 0, 0);
                World.setBlock(Column.above(5), Ceilings[Index], 2);
                double Lit = at(World, Column.getX() + 0.5, Column.getY() + 1.5, Column.getZ() + 0.5, Overhead);
                Helper.assertTrue(Math.abs(Lit - Expected[Index]) < 1.0e-6, "column " + Index + " under " + Ceilings[Index] + " gave " + Lit + " instead of " + Expected[Index]);
            }
            Helper.assertTrue(Scp001DayShade.cell(Blocks.SMOOTH_STONE_SLAB.defaultBlockState()) == Scp001DayRay.cell(0x33, 255), "a bottom slab is not the bottom octants");
            Helper.assertTrue(Scp001DayShade.cell(Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP)) == Scp001DayRay.cell(0xCC, 255), "a top slab is not the top octants");
            Helper.assertTrue(Scp001DayShade.cell(Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH)) == Scp001DayRay.cell(0xF3, 255), "south stairs have the wrong octants");
            BlockPos Room = Origin.offset(0, 0, 8);
            fill(World, Room.offset(-1, -1, -1), Room.offset(7, 5, 7), Blocks.STONE.defaultBlockState());
            fill(World, Room, Room.offset(6, 4, 6), Blocks.AIR.defaultBlockState());
            fill(World, Room.offset(3, 1, 7), Room.offset(3, 3, 7), Blocks.AIR.defaultBlockState());
            SunVector South = new SunVector(0.0, Math.sqrt(0.5), Math.sqrt(0.5));
            Helper.assertTrue(at(World, Room.getX() + 3.5, Room.getY() + 0.2, Room.getZ() + 5.5, South) == 1.0, "the floor patch of the south window is dark");
            Helper.assertTrue(at(World, Room.getX() + 3.5, Room.getY() + 0.2, Room.getZ() + 6.5, South) == 0.0, "the floor in the sill shadow is lit");
            Helper.assertTrue(at(World, Room.getX() + 3.5, Room.getY() + 0.2, Room.getZ() + 3.5, South) == 0.0, "the floor beyond the patch is lit");
            Helper.assertTrue(at(World, Room.getX() + 3.5, Room.getY() + 1.0, Room.getZ() + 6.5, South) == 1.0, "a knee in front of the window is dark");
            Helper.assertTrue(at(World, Room.getX() + 1.5, Room.getY() + 1.0, Room.getZ() + 6.5, South) == 0.0, "a knee beside the window is lit");
            Helper.assertTrue(at(World, Room.getX() + 3.5, Room.getY() + 0.2, Room.getZ() + 6.5, Overhead) == 0.0, "an overhead sun reached through the roof");
            Helper.succeed();
        });
    }

    public static void daytime(GameTestHelper Helper, ServerLevel World, long Time) {
        long Before = World.getServer().overworld().getDayTime();
        GatewayGameTests.whenDone(Helper, () -> World.getServer().overworld().setDayTime(Before));
        World.getServer().overworld().setDayTime(Time);
    }

    public static void bask(ServerLevel World, Scp538TestPlayer User, long From, int Ticks) {
        for (long Tick = From; Tick < From + Ticks; Tick++) {
            Scp001DaySunlight.tick(World, User, Tick);
        }
    }

    public static Scp538TestPlayer sunbather(GameTestHelper Helper, ServerLevel World, int Slot, String Name) {
        BlockPos Origin = base(Slot);
        clear(World, Origin);
        fill(World, Origin.offset(-2, -1, -2), Origin.offset(2, -1, 2), Blocks.STONE.defaultBlockState());
        hold(Helper, World, Origin);
        Scp538TestPlayer User = player(Helper, World, Vec3.atBottomCenterOf(Origin), Name);
        GatewayGameTests.whenDone(Helper, () -> User.getPersistentData().remove(Scp001DayTurning.Key));
        return User;
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp001day_briefExposureSpares")
    public static void briefExposureSpares(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerLevel World = standIn(Helper);
            Scp538TestPlayer User = sunbather(Helper, World, 2, "scp001day_glimpse");
            Scp001DaySunlight.State Probe = new Scp001DaySunlight.State();
            float Noon = Scp001DaySunlight.exposure(World, User, Probe, Scp001DaySun.direction(Scp001DaySun.NoonTicks));
            float Night = Scp001DaySunlight.exposure(World, User, Probe, Scp001DaySun.direction(18000L));
            Helper.assertTrue(Noon > 0.99f, "an open noon body is not fully exposed: " + Noon);
            Helper.assertTrue(Night == 0.0f, "a body is exposed at midnight: " + Night);
            daytime(Helper, World, Scp001DaySun.NoonTicks);
            bask(World, User, 0, Scp001DaySunlight.TriggerTicks - Scp001DaySunlight.CheckTicks);
            Helper.assertTrue(!Scp001DayTurning.turning(User), "a glimpse shorter than the trigger doomed the player");
            World.getServer().overworld().setDayTime(18000L);
            bask(World, User, 100, 40);
            Helper.assertTrue(!Scp001DayTurning.turning(User) && Scp001DaySunlight.state(User).Charge == 0, "the charge did not drain in the dark");
            World.getServer().overworld().setDayTime(Scp001DaySun.NoonTicks);
            bask(World, User, 200, Scp001DaySunlight.TriggerTicks - Scp001DaySunlight.CheckTicks);
            Helper.assertTrue(!Scp001DayTurning.turning(User), "two separate glimpses added up to a doom");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp001day_exposureDooms")
    public static void exposureDooms(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerLevel World = standIn(Helper);
            Scp538TestPlayer User = sunbather(Helper, World, 11, "scp001day_doomed");
            daytime(Helper, World, Scp001DaySun.NoonTicks);
            bask(World, User, 0, Scp001DaySunlight.TriggerTicks + Scp001DaySunlight.CheckTicks);
            Helper.assertTrue(Scp001DayTurning.turning(User), "a full exposure did not start the Turning");
            double Free = User.getAttribute(Attributes.MOVEMENT_SPEED).getValue();
            for (int Tick = 0; Tick < Scp001DayTurning.SyncTicks; Tick++) {
                Scp001DayTurning.tick(User);
            }
            Helper.assertTrue(User.getAttribute(Attributes.MOVEMENT_SPEED).getValue() < Free, "the Turning did not slow the player");
            World.getServer().overworld().setDayTime(18000L);
            bask(World, User, 100, 40);
            Helper.assertTrue(Scp001DayTurning.turning(User), "night cured the Turning");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp001day_turningEndsInFlesh")
    public static void turningEndsInFlesh(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerLevel World = standIn(Helper);
            Scp538TestPlayer User = sunbather(Helper, World, 12, "scp001day_turned");
            BlockPos Origin = base(12);
            fill(World, Origin.offset(-2, 3, -2), Origin.offset(2, 3, 2), Blocks.STONE.defaultBlockState());
            daytime(Helper, World, Scp001DaySun.NoonTicks);
            Scp001DayTurning.begin(World, User);
            Helper.assertTrue(Scp001DayTurning.turning(User), "begin did not doom the player");
            for (int Tick = 0; Tick < Scp001DayTurning.Duration; Tick++) {
                Scp001DaySunlight.tick(World, User, Tick);
                Scp001DayTurning.tick(User);
            }
            Helper.assertTrue(!User.isAlive(), "the Turning did not end in death");
            Helper.assertTrue(!Scp001DayTurning.turning(User), "the Turning outlived the body");
            List<Scp001DayFlesh> Risen = World.getEntitiesOfClass(Scp001DayFlesh.class, new AABB(User.blockPosition()).inflate(3.0), Each -> User.getUUID().equals(Each.carrying()));
            Helper.assertTrue(Risen.size() == 1, "the body did not rise as one 001-1 carrying its voice: " + Risen.size());
            Helper.assertTrue(Risen.get(0).Form == Scp001DayForm.Lurker || Risen.get(0).Form == Scp001DayForm.Crawler, "the body rose as " + Risen.get(0).Form);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp001day_suitImmunePartialNot")
    public static void suitImmunePartialNot(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerLevel World = standIn(Helper);
            Scp538TestPlayer User = sunbather(Helper, World, 13, "scp001day_suited");
            User.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Scp001DayRegistry.SunHat.get()));
            User.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Scp001DayRegistry.SunWraps.get()));
            User.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Scp001DayRegistry.SunLegWraps.get()));
            User.setItemSlot(EquipmentSlot.FEET, new ItemStack(Scp001DayRegistry.SunBoots.get()));
            daytime(Helper, World, Scp001DaySun.NoonTicks);
            bask(World, User, 1, 39);
            Helper.assertTrue(!Scp001DayTurning.turning(User), "the full suit let the sun in");
            User.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
            bask(World, User, 41, Scp001DaySunlight.TriggerTicks + Scp001DaySunlight.CheckTicks);
            Helper.assertTrue(Scp001DayTurning.turning(User), "bare feet in the sun did not doom the player");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp001day_turningPersists")
    public static void turningPersists(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerLevel World = standIn(Helper);
            Scp538TestPlayer User = sunbather(Helper, World, 14, "scp001day_saved");
            Scp001DayTurning.begin(World, User);
            for (int Tick = 0; Tick < 50; Tick++) {
                Scp001DayTurning.tick(User);
            }
            CompoundTag Saved = User.saveWithoutId(new CompoundTag());
            Saved.remove("UUID");
            Scp538TestPlayer Copy = new Scp538TestPlayer(World, "scp001day_loaded");
            Copy.load(Saved);
            Helper.assertTrue(Scp001DayTurning.turning(Copy) && Scp001DayTurning.ticks(Copy) == 50, "the Turning did not survive a save and load: " + Scp001DayTurning.ticks(Copy));
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp001day_noCure")
    public static void noCure(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerLevel World = standIn(Helper);
            Scp538TestPlayer User = sunbather(Helper, World, 15, "scp001day_hopeful");
            Scp001DayTurning.begin(World, User);
            new ItemStack(Items.MILK_BUCKET).finishUsingItem(World, User);
            Helper.assertTrue(Scp001DayTurning.turning(User), "milk cured the Turning");
            User.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            User.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            for (int Tick = 0; Tick < Scp001DayTurning.Duration; Tick++) {
                Scp001DayTurning.tick(User);
            }
            Helper.assertTrue(!User.isAlive(), "a totem saved the player from the Turning");
            List<ItemEntity> Dropped = World.getEntitiesOfClass(ItemEntity.class, new AABB(User.blockPosition()).inflate(4.0), Each -> Each.getItem().is(Items.TOTEM_OF_UNDYING));
            int Totems = User.getInventory().countItem(Items.TOTEM_OF_UNDYING) + Dropped.stream().mapToInt(Each -> Each.getItem().getCount()).sum();
            Dropped.forEach(Entity::discard);
            Helper.assertTrue(Totems == 2, "a totem was spent on the Turning: " + Totems + " left");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp001day_beamsFollowSun")
    public static void beamsFollowSun(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerLevel World = standIn(Helper);
            BlockPos Room = base(3);
            clear(World, Room);
            fill(World, Room.offset(-1, -1, -1), Room.offset(9, 5, 9), Blocks.STONE.defaultBlockState());
            fill(World, Room, Room.offset(8, 4, 8), Blocks.AIR.defaultBlockState());
            fill(World, Room.offset(3, 5, 3), Room.offset(5, 5, 5), Blocks.AIR.defaultBlockState());
            List<Long> Morning = litFloor(World, Room, Scp001DaySun.direction(4500L));
            List<Long> Afternoon = litFloor(World, Room, Scp001DaySun.direction(7500L));
            Helper.assertTrue(!Morning.isEmpty() && !Afternoon.isEmpty(), "a roof hole threw no sun patch");
            Helper.assertTrue(!Morning.equals(Afternoon), "the sun patch did not move with the day");
            double MorningX = Morning.stream().mapToInt(Packed -> BlockPos.of(Packed).getX()).average().orElse(0);
            double AfternoonX = Afternoon.stream().mapToInt(Packed -> BlockPos.of(Packed).getX()).average().orElse(0);
            Helper.assertTrue(MorningX < AfternoonX, "the morning patch is not west of the afternoon patch");
            Helper.succeed();
        });
    }

    private static List<Long> litFloor(ServerLevel World, BlockPos Room, SunVector Sun) {
        List<Long> Lit = new ArrayList<>();
        for (int X = 0; X <= 8; X++) {
            for (int Z = 0; Z <= 8; Z++) {
                if (at(World, Room.getX() + X + 0.5, Room.getY() + 0.05, Room.getZ() + Z + 0.5, Sun) > 0.0) {
                    Lit.add(Room.offset(X, 0, Z).asLong());
                }
            }
        }
        return Lit;
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp001day_armourShieldsAndWears")
    public static void armourShieldsAndWears(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerLevel World = standIn(Helper);
            BlockPos Origin = base(4);
            clear(World, Origin);
            fill(World, Origin.offset(-2, -1, -2), Origin.offset(2, -1, 2), Blocks.STONE.defaultBlockState());
            Scp538TestPlayer User = player(Helper, World, Vec3.atBottomCenterOf(Origin), "scp001day_wrapped");
            SunVector Noon = Scp001DaySun.direction(Scp001DaySun.NoonTicks);
            double Base = User.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
            User.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Scp001DayRegistry.SunHat.get()));
            User.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Scp001DayRegistry.SunWraps.get()));
            User.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Scp001DayRegistry.SunLegWraps.get()));
            User.setItemSlot(EquipmentSlot.FEET, new ItemStack(Scp001DayRegistry.SunBoots.get()));
            User.doTick();
            Scp001DaySunlight.State Probe = new Scp001DaySunlight.State();
            Helper.assertTrue(Scp001DaySunlight.exposure(World, User, Probe, Noon) == 0.0f, "the full wrap set let the sun through");
            double Speed = User.getAttributeValue(Attributes.MOVEMENT_SPEED);
            Helper.assertTrue(Math.abs(Speed - Base * (1.0 + 4 * Scp001DayWrapItem.Slowdown)) < 1.0e-6, "the full set did not slow by 25%: " + Speed + " of " + Base);
            Helper.assertTrue(Scp001DayWrapItem.worn(User) == 4, "the full set is not counted");
            for (EquipmentSlot Slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                int Before = User.getItemBySlot(Slot).getDamageValue();
                Scp001DaySunlight.exposure(World, User, Probe, Noon);
                java.util.Arrays.fill(Probe.Points, 1.0);
                World.getServer().overworld().setDayTime(Scp001DaySun.NoonTicks);
                Scp001DaySunlight.wear(World, User, Probe);
                Helper.assertTrue(User.getItemBySlot(Slot).getDamageValue() > Before, Slot + " did not wear in full sun");
            }
            User.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            User.doTick();
            float Bare = Scp001DaySunlight.exposure(World, User, Probe, Noon);
            Helper.assertTrue(Math.abs(Bare - 1.0f / Scp001DayExposure.Samples) < 0.02f, "a bare head did not give one seventh of the dose: " + Bare);
            Helper.assertTrue(Math.abs(User.getAttributeValue(Attributes.MOVEMENT_SPEED) - Base * (1.0 + 3 * Scp001DayWrapItem.Slowdown)) < 1.0e-6, "three pieces did not slow by three steps");
            User.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            User.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
            float Feet = Scp001DaySunlight.exposure(World, User, Probe, Noon);
            Helper.assertTrue(Math.abs(Feet - 6.0f / Scp001DayExposure.Samples) < 0.02f, "boots alone did not leave six sevenths: " + Feet);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp001day_canLoot")
    public static void canLoot(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerLevel World = Helper.getLevel();
            int Tins = 0;
            for (ResourceKey<LootTable> Key : List.of(Scp001DayChunkGenerator.RoomLoot, Scp001DayChunkGenerator.SewerLoot, Scp001DayChunkGenerator.CampLoot)) {
                LootTable Table = World.getServer().reloadableRegistries().getLootTable(Key);
                Helper.assertTrue(Table != LootTable.EMPTY, Key.location() + " is missing");
                for (int Roll = 0; Roll < 200; Roll++) {
                    LootParams Params = new LootParams.Builder(World).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
                    for (ItemStack Stack : Table.getRandomItems(Params)) {
                        if (Stack.is(Scp001DayRegistry.Tin.get())) {
                            Tins += Stack.getCount();
                        }
                    }
                }
            }
            Helper.assertTrue(Tins > 50, "the loot tables gave too few tins: " + Tins);
            int Total = 0;
            for (Scp001DayTinItem.Outcome Each : Scp001DayTinItem.Outcome.values()) {
                Total += Each.Weight;
            }
            Helper.assertTrue(Total == 100, "tin outcome weights do not sum to 100");
            int[] Counts = new int[Scp001DayTinItem.Outcome.values().length];
            for (int Roll = 0; Roll < 4000; Roll++) {
                Counts[Scp001DayTinItem.roll(World.random).ordinal()]++;
            }
            Helper.assertTrue(Counts[Scp001DayTinItem.Outcome.Plain.ordinal()] > 1900, "plain tins are not the majority");
            ServerLevel End = standIn(Helper);
            Scp538TestPlayer User = player(Helper, End, Vec3.atBottomCenterOf(base(5)), "scp001day_eater");
            for (Scp001DayTinItem.Outcome Each : Scp001DayTinItem.Outcome.values()) {
                Scp001DayTinItem.apply(End, User, Each);
            }
            User.getInventory().add(new ItemStack(Items.BREAD, 5));
            User.getInventory().add(new ItemStack(Scp001DayRegistry.Tin.get(), 2));
            Helper.assertTrue(Scp001DayHunger.spoil(User) == 5, "outside food did not spoil");
            Helper.assertTrue(User.getInventory().countItem(Scp001DayRegistry.Tin.get()) >= 2, "tins spoiled");
            int Containers = 0;
            for (int ChunkX = 0; ChunkX < 8; ChunkX++) {
                for (int ChunkZ = 0; ChunkZ < 8; ChunkZ++) {
                    int[] Seen = new int[1];
                    Scp001DayPlan.containers(ChunkX, ChunkZ, (X, Y, Z, Kind) -> Seen[0]++);
                    Containers += Seen[0];
                }
            }
            Helper.assertTrue(Containers > 10, "the plan placed almost no containers: " + Containers);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp001day_arrivesOnlyByApocalypticRift")
    public static void arrivesOnlyByApocalypticRift(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            MinecraftServer Server = Helper.getLevel().getServer();
            AnomalyDocument Document = AnomalyDocuments.server().get(Scp001DayRegistry.DocumentId);
            Helper.assertTrue(Document != null, "the SCP-001 When Day Breaks file is missing");
            Helper.assertTrue(Document.Pool() == ArchivePool.Apocalyptic, "the file is not in the apocalyptic pool");
            Helper.assertTrue(Document.Classification() == DocumentClass.Apollyon, "the file is not Apollyon");
            Helper.assertTrue(!Document.isListed(), "the file is listed in the normal archive");
            DocumentClaims Claims = DocumentClaims.get(Server);
            Claims.release(Scp001DayRegistry.DocumentId);
            Map<ResourceLocation, AnomalyDocument> Normal = DocumentRoller.eligible(Server, null, ArchivePool.Normal);
            Map<ResourceLocation, AnomalyDocument> Gold = DocumentRoller.eligible(Server, null, ArchivePool.Apocalyptic);
            Helper.assertTrue(!Normal.containsKey(Scp001DayRegistry.DocumentId), "a normal fragment can roll the file");
            Helper.assertTrue(Gold.containsKey(Scp001DayRegistry.DocumentId) && Gold.containsKey(Scp001Registry.DocumentId), "the golden pool does not hold both 001 files");
            int Dealt = 0;
            for (int Roll = 0; Roll < 300; Roll++) {
                if (DocumentRoller.roll(Normal, 5, 5, Helper.getLevel().random).contains(Scp001DayRegistry.DocumentId)) {
                    Dealt++;
                }
            }
            Helper.assertTrue(Dealt == 0, "normal rolls dealt the file");
            Helper.assertTrue(GatewayGameTests.class != null && net.rai.scp_unleashed.gateway.GatewayDestinations.get(Scp001DayDestination.Id) != null, "the gateway destination is not registered");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp001day_gatewayEntryAndRiftEscape")
    public static void gatewayEntryAndRiftEscape(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerLevel End = standIn(Helper);
            GatewayDestination Destination = Scp001DayDestination.standIn(Level.END);
            Vec3 Arrival = Scp001DayDestination.arrivalSpot(0);
            fill(End, BlockPos.containing(Arrival).offset(-3, -1, -3), BlockPos.containing(Arrival).offset(3, -1, 3), Blocks.STONE.defaultBlockState());
            fill(End, BlockPos.containing(Arrival).offset(-3, 0, -3), BlockPos.containing(Arrival).offset(3, 3, 3), Blocks.AIR.defaultBlockState());
            net.neoforged.neoforge.common.util.FakePlayer User = GatewayGameTests.person(Helper, 4.5, 4.5, "scp001day_traveller");
            Vec3 Home = User.position();
            GatewayStash.Spot Back = new GatewayStash.Spot(Helper.getLevel().dimension(), Home, 0.0f);
            Helper.assertTrue(GatewayTravel.enter(User, Destination, Back), "the gateway refused to enter");
            Helper.assertTrue(User.level().dimension() == Level.END, "the traveller did not arrive in the stand-in");
            Helper.assertTrue(User.position().distanceTo(Arrival) < 3.0, "the traveller did not arrive at the entry point: " + User.position());
            Scp001DayRift Rift = Scp001DayRift.create(End, User.getX(), User.getY(), User.getZ());
            End.addFreshEntity(Rift);
            Rift.pass(End, User);
            Helper.assertTrue(User.level().dimension() == Helper.getLevel().dimension(), "the rift did not free the traveller");
            Helper.assertTrue(User.position().distanceTo(Home) < GatewayTravel.SafeRadius + 2.0, "the traveller was not returned near the entry point: " + User.position());
            Helper.assertTrue(Rift.life() <= Rift.age() + Scp001DayRift.AfterUse, "the used rift did not start closing");
            Rift.discard();
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp001day_voiceAdapter")
    public static void voiceAdapter(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerLevel End = standIn(Helper);
            VoicePlayback Before = VoiceArchive.playback();
            VoiceArchiveGameTests.FakePlayback Fake = new VoiceArchiveGameTests.FakePlayback();
            GatewayGameTests.whenDone(Helper, () -> {
                VoiceArchive.setPlayback(Before);
                VoicePolicy.forceMaster(null);
                VoiceArchive.clear(VoicePolicy.Scp001Day);
            });
            VoicePolicy.forceMaster(true);
            VoiceArchive.setPlayback(Fake);
            VoiceArchive.clear(VoicePolicy.Scp001Day);
            BlockPos Origin = base(6);
            clear(End, Origin);
            fill(End, Origin.offset(-2, -1, -2), Origin.offset(2, -1, 2), Blocks.STONE.defaultBlockState());
            Scp538TestPlayer Speaker = player(Helper, End, Vec3.atBottomCenterOf(Origin), "scp001day_speaker");
            Scp001DayEvents.tickDimension(End, Scp001DayVoice.CaptureEvery * 3L);
            Helper.assertTrue(Scp001DayVoice.capturing(Speaker.getUUID()), "the adapter did not capture a player inside the stand-in");
            UUID Target = UUID.randomUUID();
            VoiceArchive.accept(VoiceArchiveGameTests.clip(VoicePolicy.Scp001Day, Speaker.getUUID(), "speaker", System.currentTimeMillis() - 4000L, 40, 7));
            VoiceArchive.accept(VoiceArchiveGameTests.clip(VoicePolicy.Scp001Day, Target, "target", System.currentTimeMillis() - 3000L, 40, 9));
            Entity Source = Scp001DayRift.create(End, Origin.getX() + 0.5, Origin.getY(), Origin.getZ() + 0.5);
            End.addFreshEntity(Source);
            boolean Played = Scp001DayVoice.replay(Source, Speaker.getUUID(), Target);
            Source.discard();
            Helper.assertTrue(Played, "the adapter did not replay a stored clip");
            Helper.assertTrue(!Fake.Played.isEmpty() && Fake.Played.get(0).get(0)[0] == 7, "the replay did not prefer the carried speaker");
            Scp001DayVoice.stop(Speaker.getUUID());
            Helper.assertTrue(!Scp001DayVoice.capturing(Speaker.getUUID()), "capture did not stop");
            Helper.succeed();
        });
    }

    public static void hold(GameTestHelper Helper, ServerLevel World, BlockPos Origin) {
        List<long[]> Forced = new ArrayList<>();
        for (int X = (Origin.getX() - 8) >> 4; X <= (Origin.getX() + 24) >> 4; X++) {
            for (int Z = (Origin.getZ() - 8) >> 4; Z <= (Origin.getZ() + 24) >> 4; Z++) {
                World.setChunkForced(X, Z, true);
                Forced.add(new long[] {X, Z});
            }
        }
        GatewayGameTests.whenDone(Helper, () -> {
            for (Entity Each : World.getEntitiesOfClass(Entity.class, new AABB(Origin).inflate(32.0), Each -> Each instanceof Scp001DayFlesh || Each instanceof ArmorStand)) {
                Each.discard();
            }
            for (long[] Chunk : Forced) {
                World.setChunkForced((int) Chunk[0], (int) Chunk[1], false);
            }
        });
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp001day_grabDragsIntoSun")
    public static void grabDragsIntoSun(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerLevel World = standIn(Helper);
            BlockPos Origin = base(7);
            clear(World, Origin);
            BlockState Stone = Blocks.STONE.defaultBlockState();
            fill(World, Origin.offset(-3, -1, -3), Origin.offset(16, -1, 3), Stone);
            fill(World, Origin.offset(-3, 3, -3), Origin.offset(4, 3, 3), Stone);
            fill(World, Origin.offset(-3, 0, -3), Origin.offset(4, 2, -3), Stone);
            fill(World, Origin.offset(-3, 0, 3), Origin.offset(4, 2, 3), Stone);
            fill(World, Origin.offset(-3, 0, -3), Origin.offset(-3, 2, 3), Stone);
            hold(Helper, World, Origin);
            long Before = World.getServer().overworld().getDayTime();
            GatewayGameTests.whenDone(Helper, () -> World.getServer().overworld().setDayTime(Before));
            World.getServer().overworld().setDayTime(Scp001DaySun.NoonTicks);
            ArmorStand Body = new ArmorStand(World, Origin.getX() + 0.5, Origin.getY(), Origin.getZ() + 0.5);
            World.addFreshEntity(Body);
            Scp001DayFlesh Crawler = Scp001DayFusion.make(World, Scp001DayForm.Crawler.Weight, new Vec3(Origin.getX() - 0.5, Origin.getY(), Origin.getZ() + 0.5), null);
            Helper.assertTrue(Crawler != null && Crawler.Form == Scp001DayForm.Crawler, "no crawler was made");
            float Start = Scp001DaySunlight.exposure(World, Body, new Scp001DaySunlight.State());
            Helper.assertTrue(Start < 0.1f, "the shelter is not shaded: " + Start);
            Helper.assertTrue(Crawler.findLight(World, Crawler.blockPosition()) != null, "the crawler found no sunlit floor");
            Helper.assertTrue(Crawler.grab(World, Body), "the crawler could not grab the body");
            Helper.assertTrue(Scp001DayFlesh.held(Body), "the grab did not slow the body");
            Helper.assertTrue(!Crawler.doHurtTarget(Body), "the flesh struck the body directly");
            float Health = Body.getHealth();
            Helper.startSequence()
                    .thenWaitUntil(() -> Helper.assertTrue(Scp001DaySunlight.exposure(World, Body, new Scp001DaySunlight.State()) > 0.5f,
                            "the body was not dragged into the sun: body " + Body.position() + " crawler " + Crawler.position() + " holding " + (Crawler.victim() != null)))
                    .thenExecute(() -> {
                        Helper.assertTrue(Crawler.victim() != null, "the crawler let go before the sun");
                        Helper.assertTrue(Body.getX() > Origin.getX() + 4.0, "the body lit up without leaving the shelter: " + Body.position());
                        Helper.assertTrue(Body.getHealth() >= Health, "the flesh hurt the body itself");
                    })
                    .thenSucceed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp001day_struggleBreaksGrip")
    public static void struggleBreaksGrip(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerLevel World = standIn(Helper);
            BlockPos Origin = base(8);
            clear(World, Origin);
            fill(World, Origin.offset(-3, -1, -3), Origin.offset(3, -1, 3), Blocks.STONE.defaultBlockState());
            hold(Helper, World, Origin);
            Scp538TestPlayer User = player(Helper, World, Vec3.atBottomCenterOf(Origin), "scp001day_struggler");
            Scp001DayFlesh Lurker = Scp001DayFusion.make(World, Scp001DayForm.Lurker.Weight, Vec3.atBottomCenterOf(Origin).add(1.2, 0.0, 0.0), null);
            Scp001DayFlesh Crawler = Scp001DayFusion.make(World, Scp001DayForm.Crawler.Weight, Vec3.atBottomCenterOf(Origin).add(-1.2, 0.0, 0.0), null);
            Helper.assertTrue(Lurker != null && Lurker.Form == Scp001DayForm.Lurker && Crawler != null, "the test flesh was not made");
            double Free = User.getAttributeValue(Attributes.MOVEMENT_SPEED);
            Helper.assertTrue(Lurker.grab(World, User), "the lurker could not grab");
            Helper.assertTrue(Lurker.carrying() != null && Lurker.carrying().equals(User.getUUID()), "the grab did not carry the victim's voice");
            Helper.assertTrue(!Crawler.grab(World, User), "two flesh held one body");
            double Slowed = User.getAttributeValue(Attributes.MOVEMENT_SPEED);
            Helper.assertTrue(Slowed > Free * 0.5 && Slowed < Free, "a hold must slow, never lock: " + Free + " -> " + Slowed);
            for (int Hit = 1; Hit < Scp001DayForm.Lurker.Grip; Hit++) {
                Lurker.invulnerableTime = 0;
                Lurker.hurt(World.damageSources().playerAttack(User), 0.5f);
                Helper.assertTrue(Lurker.victim() != null, "the lurker let go after " + Hit + " of " + Scp001DayForm.Lurker.Grip + " blows");
            }
            Lurker.invulnerableTime = 0;
            Lurker.hurt(World.damageSources().playerAttack(User), 0.5f);
            Helper.assertTrue(Lurker.victim() == null, "the struggle did not break the grip");
            Helper.assertTrue(Lurker.stunned(), "the lurker did not recoil");
            Helper.assertTrue(!Scp001DayFlesh.held(User), "the slow outlived the grip");
            Helper.assertTrue(!Lurker.grab(World, User), "a recoiling lurker grabbed again");
            Helper.assertTrue(Crawler.grab(World, User), "the crawler could not grab a freed body");
            Crawler.hurt(World.damageSources().inFire(), 0.5f);
            Helper.assertTrue(Crawler.victim() == null && Crawler.stunned(), "fire did not break the grip at once");
            Helper.assertTrue(!Scp001DayFlesh.held(User), "fire left the body slowed");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp001day_fuseAndSplitConserveMass")
    public static void fuseAndSplitConserveMass(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerLevel World = standIn(Helper);
            BlockPos Origin = base(9);
            clear(World, Origin);
            fill(World, Origin.offset(-4, -1, -4), Origin.offset(4, -1, 4), Blocks.STONE.defaultBlockState());
            hold(Helper, World, Origin);
            Vec3 Centre = Vec3.atBottomCenterOf(Origin);
            List<Scp001DayFlesh> Group = new ArrayList<>();
            int[] Masses = {2, 2, 1, 1};
            for (int Index = 0; Index < Masses.length; Index++) {
                Group.add(Scp001DayFusion.make(World, Masses[Index], Centre.add(Math.cos(Index * 1.6) * 0.8, 0.0, Math.sin(Index * 1.6) * 0.8), null));
            }
            Helper.assertTrue(Group.stream().allMatch(Each -> Each != null), "the fuse group was not made");
            Helper.assertTrue(Scp001DayFusion.areaMass(World, Centre) == 6, "the area did not start at mass 6: " + Scp001DayFusion.areaMass(World, Centre));
            Scp001DayFlesh Fused = Scp001DayFusion.fuse(World, Group);
            Helper.assertTrue(Fused != null && Fused.mass() == 6 && Fused.Form == Scp001DayForm.Mass, "four small flesh did not fuse into one mass of 6");
            Helper.assertTrue(Group.stream().allMatch(Scp001DayFlesh::isRemoved), "the fused parts were left behind");
            Helper.assertTrue(Scp001DayFusion.areaMass(World, Centre) == 6, "fusing changed the area mass");
            Scp001DayFlesh Big = Scp001DayFusion.make(World, 20, Centre.add(1.0, 0.0, 0.0), null);
            Helper.assertTrue(Big != null && Big.mass() == 20 && Big.pile(), "a mass of 20 is not a pile");
            Helper.assertTrue(Scp001DayFusion.fuse(World, List.of(Fused, Big)) == null, "a fuse went past the cap of " + Scp001DayForm.MassCap);
            Helper.assertTrue(Fused.isAlive() && Big.isAlive(), "a refused fuse lost flesh");
            Fused.setMass(99);
            Helper.assertTrue(Fused.mass() == Scp001DayForm.MassCap, "setMass did not clamp to the cap: " + Fused.mass());
            Fused.setMass(6);
            List<Scp001DayFlesh> Pieces = Scp001DayFusion.split(World, Big, null);
            int Sum = Pieces.stream().mapToInt(Scp001DayFlesh::mass).sum();
            Helper.assertTrue(Big.isRemoved(), "the split parent survived");
            Helper.assertTrue(Pieces.size() == 1 + Scp001DayFusion.SplitPieces && Sum == 20, "the split did not conserve mass: " + Pieces.size() + " pieces, mass " + Sum);
            Helper.assertTrue(Pieces.get(0).mass() == 20 - Scp001DayFusion.SplitPieces && Pieces.get(0).pile(), "the split remainder is wrong: " + Pieces.get(0).mass());
            Helper.assertTrue(Pieces.stream().skip(1).allMatch(Each -> Each.mass() == 1 && Each.Form == Scp001DayForm.Crawler), "split pieces are not crawlers");
            Helper.assertTrue(Scp001DayFusion.split(World, Pieces.get(1), null).isEmpty() && Pieces.get(1).isAlive(), "a crawler split further");
            Helper.assertTrue(Scp001DayFusion.areaMass(World, Centre) == 26, "splitting changed the area mass: " + Scp001DayFusion.areaMass(World, Centre));
            int Fusions = 0;
            for (int Attempt = 0; Attempt < 80 && Fusions == 0; Attempt++) {
                if (Scp001DayFusion.tryFuse(World, Pieces.get(0))) {
                    Fusions++;
                }
            }
            Helper.assertTrue(Fusions == 1, "tryFuse never fused the group");
            int Area = 0;
            for (Scp001DayFlesh Each : Scp001DayFlesh.near(World, Centre, 8.0)) {
                Helper.assertTrue(Each.mass() <= Scp001DayForm.MassCap, "a body passed the cap: " + Each.mass());
                Area += Each.mass();
            }
            Helper.assertTrue(Area == 26 && Scp001DayFusion.areaMass(World, Centre) == 26, "tryFuse changed the area mass: " + Area);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp001day_lurkerWatchedAndBehind")
    public static void lurkerWatchedAndBehind(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerLevel World = standIn(Helper);
            BlockPos Origin = base(10);
            clear(World, Origin);
            fill(World, Origin.offset(-3, -1, -6), Origin.offset(3, -1, 6), Blocks.STONE.defaultBlockState());
            hold(Helper, World, Origin);
            Scp538TestPlayer User = player(Helper, World, Vec3.atBottomCenterOf(Origin), "scp001day_watcher");
            User.moveTo(User.getX(), User.getY(), User.getZ(), 0.0f, 0.0f);
            User.setYHeadRot(0.0f);
            Scp001DayFlesh Lurker = Scp001DayFusion.make(World, Scp001DayForm.Lurker.Weight, Vec3.atBottomCenterOf(Origin).add(0.0, 0.0, 4.0), null);
            Helper.assertTrue(Lurker != null, "no lurker was made");
            Helper.assertTrue(Lurker.watched(User) && !Lurker.behind(User), "a lurker in plain view is not watched");
            Lurker.moveTo(Origin.getX() + 0.5, Origin.getY(), Origin.getZ() - 3.5, 0.0f, 0.0f);
            Helper.assertTrue(!Lurker.watched(User) && Lurker.behind(User), "a lurker behind the player is watched");
            fill(World, Origin.offset(-2, 0, 2), Origin.offset(2, 2, 2), Blocks.STONE.defaultBlockState());
            Lurker.moveTo(Origin.getX() + 0.5, Origin.getY(), Origin.getZ() + 4.5, 0.0f, 0.0f);
            Helper.assertTrue(!Lurker.watched(User), "a lurker behind a wall is watched");
            Helper.succeed();
        });
    }

    public static final String[] FleshModels = {"crawler", "clinger", "lurker", "mass", "pile_a", "pile_b", "pile_c"};
    public static final float StrayLimit = 20.0f;

    private static JsonObject asset(String Path) {
        try (InputStream Stream = Scp001DayGameTests.class.getResourceAsStream("/assets/scp_unleashed/" + Path)) {
            if (Stream == null) {
                throw new IllegalStateException("missing asset " + Path);
            }
            return JsonParser.parseReader(new InputStreamReader(Stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException Problem) {
            throw new UncheckedIOException(Problem);
        }
    }

    private static float[] vector(JsonElement Value) {
        JsonElement Raw = Value.isJsonObject() ? Value.getAsJsonObject().get("vector") : Value;
        return new float[] {Raw.getAsJsonArray().get(0).getAsFloat(), Raw.getAsJsonArray().get(1).getAsFloat(), Raw.getAsJsonArray().get(2).getAsFloat()};
    }

    private static Quaternionf turn(float X, float Y, float Z) {
        return new Quaternionf().rotateZ((float) Math.toRadians(Z)).rotateY((float) Math.toRadians(-Y)).rotateX((float) Math.toRadians(-X));
    }

    private static float apart(Quaternionf First, Quaternionf Second) {
        return (float) Math.toDegrees(2.0 * Math.acos(Math.min(1.0, Math.abs(First.dot(Second)))));
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp001day_fleshAnimationsSmooth")
    public static void fleshAnimationsSmooth(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            for (String Key : FleshModels) {
                String Name = "scp001_day_" + Key;
                Map<String, float[]> Rest = new HashMap<>();
                for (JsonElement Bone : asset("geo/entity/scp001day/" + Name + ".geo.json").getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")) {
                    JsonObject Entry = Bone.getAsJsonObject();
                    Rest.put(Entry.get("name").getAsString(), Entry.has("rotation") ? vector(Entry.get("rotation")) : new float[3]);
                }
                JsonObject Clips = asset("animations/entity/scp001day/" + Name + ".animation.json").getAsJsonObject("animations");
                for (Map.Entry<String, JsonElement> Clip : Clips.entrySet()) {
                    JsonObject Body = Clip.getValue().getAsJsonObject();
                    if (Clip.getKey().equals("grab") || Clip.getKey().equals("fuse")) {
                        Helper.assertTrue(!Body.has("loop") || !Body.get("loop").isJsonPrimitive() || !"hold_on_last_frame".equals(Body.get("loop").getAsString()),
                                Name + " " + Clip.getKey() + " holds its last frame and would pin the legs");
                    }
                    if (!Body.has("bones")) {
                        continue;
                    }
                    for (Map.Entry<String, JsonElement> Bone : Body.getAsJsonObject("bones").entrySet()) {
                        JsonObject Channels = Bone.getValue().getAsJsonObject();
                        if (!Channels.has("rotation") || !Channels.get("rotation").isJsonObject()) {
                            continue;
                        }
                        float[] Base = Rest.getOrDefault(Bone.getKey(), new float[3]);
                        List<Map.Entry<Float, float[]>> Keys = new ArrayList<>();
                        for (Map.Entry<String, JsonElement> Frame : Channels.getAsJsonObject("rotation").entrySet()) {
                            Keys.add(Map.entry(Float.parseFloat(Frame.getKey()), vector(Frame.getValue())));
                        }
                        Keys.sort(Map.Entry.comparingByKey());
                        for (int Index = 1; Index < Keys.size(); Index++) {
                            float[] First = Keys.get(Index - 1).getValue();
                            float[] Second = Keys.get(Index).getValue();
                            Quaternionf Start = turn(Base[0] + First[0], Base[1] + First[1], Base[2] + First[2]);
                            Quaternionf End = turn(Base[0] + Second[0], Base[1] + Second[1], Base[2] + Second[2]);
                            Quaternionf Lerped = turn(Base[0] + (First[0] + Second[0]) * 0.5f, Base[1] + (First[1] + Second[1]) * 0.5f, Base[2] + (First[2] + Second[2]) * 0.5f);
                            float Stray = apart(Lerped, new Quaternionf(Start).slerp(End, 0.5f));
                            Helper.assertTrue(Stray <= StrayLimit, Name + " " + Clip.getKey() + " " + Bone.getKey() + " sweeps " + Math.round(Stray) + " degrees off course between "
                                    + Keys.get(Index - 1).getKey() + "s and " + Keys.get(Index).getKey() + "s");
                        }
                    }
                }
            }
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp001day_lifelessAndSupported")
    public static void lifelessAndSupported(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            for (int Code = 0; Code < Scp001DayCode.Count; Code++) {
                BlockState State = Scp001DayPalette.state(Code);
                String Name = BuiltInRegistries.BLOCK.getKey(State.getBlock()).getPath();
                boolean Living = State.is(BlockTags.LEAVES) || State.is(BlockTags.FLOWERS) || State.is(BlockTags.SAPLINGS) || State.is(BlockTags.CROPS) || State.is(BlockTags.WOOL_CARPETS)
                        || Name.contains("wart") || Name.contains("moss") || Name.contains("lichen") || Name.contains("vine") || Name.contains("grass") || Name.contains("mushroom")
                        || Name.contains("fungus") || Name.contains("roots") || Name.contains("potted") || Name.contains("bush") || Name.contains("carpet");
                Helper.assertTrue(!Living, "code " + Code + " generates a living or carpet block: " + Name);
            }
            int[][] Cells = {{0, 0}, {1, 2}, {3, 1}};
            int Checked = 0;
            for (int[] Cell : Cells) {
                for (int Dx = 0; Dx < 4; Dx++) {
                    for (int Dz = 0; Dz < 4; Dz++) {
                        List<Scp001DaySupport.Loose> Found = Scp001DaySupport.loose(Cell[0] * 4 + Dx, Cell[1] * 4 + Dz);
                        Helper.assertTrue(Found.isEmpty(), Found.size() + " blocks float in chunk " + (Cell[0] * 4 + Dx) + " " + (Cell[1] * 4 + Dz)
                                + (Found.isEmpty() ? "" : ", first code " + Found.get(0).Code() + " at " + Found.get(0).X() + " " + Found.get(0).Y() + " " + Found.get(0).Z()));
                        Checked++;
                    }
                }
            }
            Helper.assertTrue(Checked == 48, "the support sample was cut short");
            Helper.succeed();
        });
    }
}
