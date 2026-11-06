package net.rai.scp_unleashed.stranger.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp018.Scp018Stranger;
import net.rai.scp_unleashed.scp079.Scp079Stranger;
import net.rai.scp_unleashed.scp224.Scp224Stranger;
import net.rai.scp_unleashed.scp860.Scp860Stranger;
import net.rai.scp_unleashed.stranger.StrangerEncounters;
import net.rai.scp_unleashed.stranger.StrangerEntity;
import net.rai.scp_unleashed.stranger.StrangerGift;
import net.rai.scp_unleashed.stranger.StrangerPlan;
import net.rai.scp_unleashed.stranger.StrangerVariant;
import net.rai.scp_unleashed.stranger.StrangerVariants;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class CourierGameTests {
    public static final String Arena = "scp458_arena";
    public static final List<String> Clips = List.of("idle", "talk", "tilt", "adjust", "nod", "appear", "vanish");
    public static final List<String> HumanoidBones = List.of("root", "body", "look", "head", "hood", "right_arm", "left_arm", "right_leg", "left_leg");
    public static final List<String> Retired = List.of("geo/entity/stranger/scientist.geo.json", "animations/entity/stranger/scientist.animation.json",
            "textures/entity/stranger/scientist.png", "textures/entity/stranger/scientist_glowmask.png");
    public static final Map<String, String[]> Others = Map.of(
            "wondertainment", new String[]{"geo/entity/scp018/wondertainment.geo.json", "textures/entity/scp018/wondertainment.png"},
            "scp079_hobbyist", new String[]{"geo/entity/scp079/hobbyist.geo.json", "textures/entity/scp079/hobbyist.png"},
            "scp224_grandfather", new String[]{"geo/entity/scp224/grandfather.geo.json", "textures/entity/scp224/grandfather.png"},
            "scp860_businessman", new String[]{"geo/entity/scp860/businessman.geo.json", "textures/entity/scp860/businessman.png"});

    private static List<StrangerVariant> others() {
        return List.of(Scp018Stranger.Wondertainment, Scp079Stranger.Hobbyist, Scp224Stranger.Grandfather, Scp860Stranger.Businessman);
    }

    private CourierGameTests() {
    }

    private static InputStream asset(ResourceLocation Id) {
        return CourierGameTests.class.getResourceAsStream("/assets/" + Id.getNamespace() + "/" + Id.getPath());
    }

    private static boolean shipped(ResourceLocation Id) {
        try (InputStream Stream = asset(Id)) {
            return Stream != null;
        } catch (IOException Problem) {
            throw new IllegalStateException(Problem);
        }
    }

    private static int[] pngSize(GameTestHelper Helper, ResourceLocation Id) {
        try (InputStream Stream = asset(Id)) {
            Helper.assertTrue(Stream != null, "missing " + Id);
            byte[] Head = Stream.readNBytes(24);
            int Width = (Head[16] & 255) << 24 | (Head[17] & 255) << 16 | (Head[18] & 255) << 8 | (Head[19] & 255);
            int Height = (Head[20] & 255) << 24 | (Head[21] & 255) << 16 | (Head[22] & 255) << 8 | (Head[23] & 255);
            return new int[]{Width, Height};
        } catch (IOException Problem) {
            throw new IllegalStateException(Problem);
        }
    }

    private static JsonObject json(GameTestHelper Helper, ResourceLocation Id) {
        try (InputStream Stream = asset(Id)) {
            Helper.assertTrue(Stream != null, "missing " + Id);
            return JsonParser.parseReader(new InputStreamReader(Stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException Problem) {
            throw new IllegalStateException(Problem);
        }
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "courier_variants")
    public static void courierIsCloakedFigureAndOthersUnchanged(GameTestHelper Helper) {
        StrangerVariant Courier = StrangerVariants.Foundation;
        Helper.assertTrue(Courier.Model().equals(StrangerVariants.SharedModel) && Courier.Model().getPath().equals("geo/entity/stranger/figure.geo.json"), "the courier is not on the cloaked figure model");
        Helper.assertTrue(Courier.Animations().equals(StrangerVariants.SharedAnimations) && Courier.Animations().getPath().equals("animations/entity/stranger/figure.animation.json"),
                "the courier is not on the cloaked figure animations");
        Helper.assertTrue(Courier.Texture().equals(StrangerVariants.SharedTexture) && Courier.Texture().getPath().equals("textures/entity/stranger/foundation.png"),
                "the courier is not on the cloaked figure texture");
        Helper.assertTrue(Courier.PlateKey().equals(StrangerVariants.CourierPlate), "the courier plate changed");
        for (String Gone : Retired) {
            Helper.assertFalse(shipped(StrangerVariants.location(Gone)), "the retired scientist asset " + Gone + " is still shipped");
        }
        Helper.assertTrue(others().size() == Others.size(), "the other variants did not load");
        for (Map.Entry<String, String[]> Entry : Others.entrySet()) {
            Helper.assertTrue(StrangerVariants.exists(Entry.getKey()), "variant " + Entry.getKey() + " is missing");
            StrangerVariant Other = StrangerVariants.get(Entry.getKey());
            Helper.assertTrue(Other.Model().getPath().equals(Entry.getValue()[0]), "variant " + Entry.getKey() + " changed model");
            Helper.assertTrue(Other.Texture().getPath().equals(Entry.getValue()[1]), "variant " + Entry.getKey() + " changed texture");
            Helper.assertFalse(Other.Model().equals(Courier.Model()) || Other.PlateKey().equals(StrangerVariants.CourierPlate), "variant " + Entry.getKey() + " picked up the courier look");
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "courier_assets")
    public static void courierAssetsAndGlowmask(GameTestHelper Helper) {
        StrangerVariant Courier = StrangerVariants.Foundation;
        int[] Base = pngSize(Helper, Courier.Texture());
        ResourceLocation Glow = ResourceLocation.fromNamespaceAndPath(Courier.Texture().getNamespace(), Courier.Texture().getPath().replace(".png", "_glowmask.png"));
        int[] Mask = pngSize(Helper, Glow);
        Helper.assertTrue(Base[0] == Mask[0] && Base[1] == Mask[1], "the courier glowmask does not match the texture size");
        int[] Portrait = pngSize(Helper, Courier.Portrait());
        Helper.assertTrue(Portrait[0] == 128 && Portrait[1] == 64, "the courier portrait sheet is not 128 by 64");
        JsonObject Geometry = json(Helper, Courier.Model()).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        JsonObject Description = Geometry.getAsJsonObject("description");
        Helper.assertTrue(Description.get("texture_width").getAsInt() == Base[0] && Description.get("texture_height").getAsInt() == Base[1], "the courier geometry does not match its texture");
        List<String> Names = new ArrayList<>();
        for (var Bone : Geometry.getAsJsonArray("bones")) {
            Names.add(Bone.getAsJsonObject().get("name").getAsString());
        }
        for (String Wanted : HumanoidBones) {
            Helper.assertTrue(Names.contains(Wanted), "the courier geometry has no " + Wanted + " bone");
        }
        JsonObject Animations = json(Helper, Courier.Animations()).getAsJsonObject("animations");
        for (String Clip : Clips) {
            Helper.assertTrue(Animations.has(Clip), "the courier has no " + Clip + " animation");
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "courier_delivers")
    public static void courierStillDelivers(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        for (int X = 0; X < 16; X++) {
            for (int Z = 0; Z < 16; Z++) {
                Helper.setBlock(new BlockPos(X, 0, Z), Blocks.STONE);
                for (int Y = 1; Y < 5; Y++) {
                    Helper.setBlock(new BlockPos(X, Y, Z), Blocks.AIR);
                }
            }
        }
        BlockPos Middle = Helper.absolutePos(new BlockPos(8, 1, 8));
        CourierTestPlayer Reader = new CourierTestPlayer(World, "courier_reader");
        Reader.moveTo(Middle.getX() + 0.5, Middle.getY(), Middle.getZ() + 0.5, 0.0f, 0.0f);
        StrangerPlan Plan = new StrangerPlan(StrangerVariants.FoundationId, "stranger.scp_unleashed.foundation", 3,
                List.of(new StrangerGift(ResourceLocation.withDefaultNamespace("paper"), 1)), 0, 0, 4, 6, 400);
        Helper.assertTrue(StrangerEncounters.get(World.getServer()).summonNow(Reader, Plan, null), "the courier did not arrive");
        List<StrangerEntity> Found = World.getEntitiesOfClass(StrangerEntity.class, new AABB(Middle).inflate(10.0), Figure -> Figure.isOwner(Reader));
        Helper.assertTrue(Found.size() == 1, "expected one courier, found " + Found.size());
        StrangerEntity Courier = Found.get(0);
        Helper.assertTrue(Courier.variant() == StrangerVariants.Foundation, "the courier arrived as " + Courier.variant().Id());
        Courier.interact(Reader, InteractionHand.MAIN_HAND);
        Helper.assertTrue(Courier.isTalking(), "the courier would not talk");
        Courier.reply(Reader, true);
        boolean Delivered = false;
        for (int Slot = 0; Slot < Reader.getInventory().getContainerSize(); Slot++) {
            Delivered |= Reader.getInventory().getItem(Slot).is(Items.PAPER);
        }
        Helper.assertTrue(Delivered, "the courier did not hand over the gift");
        Helper.assertTrue(Courier.isLeaving(), "the courier did not leave after delivering");
        Helper.succeed();
    }
}
