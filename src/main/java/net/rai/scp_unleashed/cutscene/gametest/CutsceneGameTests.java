package net.rai.scp_unleashed.cutscene.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.cutscene.Cutscene;

import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class CutsceneGameTests {
    public static final String Arena = "gear_arena";
    private static final List<ResourceLocation> OtherMods = List.of(ResourceLocation.fromNamespaceAndPath("appleskin", "food_level_overlay"),
            ResourceLocation.fromNamespaceAndPath("appleskin", "health_restored"), ResourceLocation.fromNamespaceAndPath("jade", "overlay"),
            ResourceLocation.fromNamespaceAndPath("xaerominimap", "minimap"), ResourceLocation.fromNamespaceAndPath("journeymap", "minimap"),
            ResourceLocation.fromNamespaceAndPath("ftbchunks", "minimap"));
    private static final List<ResourceLocation> VanillaHud = List.of(ResourceLocation.withDefaultNamespace("hotbar"), ResourceLocation.withDefaultNamespace("crosshair"),
            ResourceLocation.withDefaultNamespace("player_health"), ResourceLocation.withDefaultNamespace("food_level"), ResourceLocation.withDefaultNamespace("chat"),
            ResourceLocation.withDefaultNamespace("effects"), ResourceLocation.withDefaultNamespace("debug_overlay"), ResourceLocation.withDefaultNamespace("boss_overlay"));
    private static final List<ResourceLocation> OwnHud = List.of(mod("extraction_detector_hud"), mod("gun_hud"),
            mod("notice_hud"));

    private CutsceneGameTests() {
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "cutscenehud_decision")
    public static void decision(GameTestHelper Helper) {
        for (ResourceLocation Layer : concat()) {
            Helper.assertFalse(Cutscene.hides(null, Layer), Layer + " was hidden with no cutscene playing");
        }
        for (Cutscene Scene : Cutscene.values()) {
            for (ResourceLocation Layer : concat()) {
                Helper.assertTrue(Cutscene.hides(Scene, Layer), Layer + " still shows during " + Scene);
            }
            for (ResourceLocation Layer : Scene.own()) {
                Helper.assertFalse(Cutscene.hides(Scene, Layer), Scene + " hid its own layer " + Layer);
            }
            for (ResourceLocation Layer : Cutscene.AlwaysKept) {
                Helper.assertFalse(Cutscene.hides(Scene, Layer), Scene + " hid " + Layer);
            }
            for (Cutscene Other : Cutscene.values()) {
                if (Other == Scene) {
                    continue;
                }
                for (ResourceLocation Layer : Other.own()) {
                    if (!Scene.own().contains(Layer)) {
                        Helper.assertTrue(Cutscene.hides(Scene, Layer), Scene + " showed the " + Other + " layer " + Layer);
                    }
                }
            }
        }
        ResourceLocation TabList = ResourceLocation.withDefaultNamespace("tab_list");
        Helper.assertFalse(Cutscene.hides(Cutscene.Scp902Trip, TabList), "the SCP-902 trip hid the tab list it always kept");
        Helper.assertTrue(Cutscene.hides(Cutscene.Intro, TabList) && Cutscene.hides(Cutscene.Scp2521Trip, TabList), "the tab list showed in the intro or the SCP-2521 trip");
        Helper.assertTrue(Cutscene.Intro.own().contains(mod("intro_scene")), "the intro scene layer is not kept in the intro");
        Helper.assertTrue(Cutscene.Scp902Trip.own().contains(mod("scp902_cinema")) && Cutscene.Scp902Trip.own().contains(mod("scp902_grade")),
                "the SCP-902 trip layers are not kept");
        Helper.assertTrue(Cutscene.Scp2521Trip.own().contains(mod("scp2521_cinema")), "the SCP-2521 cinema layer is not kept");
        Helper.succeed();
    }

    private static ResourceLocation mod(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static List<ResourceLocation> concat() {
        return java.util.stream.Stream.of(OtherMods, VanillaHud, OwnHud).flatMap(List::stream).toList();
    }
}
