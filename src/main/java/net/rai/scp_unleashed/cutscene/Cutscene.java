package net.rai.scp_unleashed.cutscene;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.intro.IntroRegistry;
import net.rai.scp_unleashed.scp2521.Scp2521Registry;
import net.rai.scp_unleashed.scp902.Scp902Registry;
import net.rai.scp_unleashed.worldend.WorldEndRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public enum Cutscene {
    WorldEnd(Set.of(WorldEndRegistry.location("worldend_flesh"))),
    Intro(Set.of(IntroRegistry.location("intro_scene"))),
    Scp902Trip(Set.of(ResourceLocation.withDefaultNamespace("tab_list"), Scp902Registry.location("scp902_grade"), Scp902Registry.location("scp902_cinema"))),
    Scp2521Trip(Set.of(Scp2521Registry.location("scp2521_cinema")));

    public static final Set<ResourceLocation> AlwaysKept = Set.of(ResourceLocation.withDefaultNamespace("subtitle_overlay"), ResourceLocation.withDefaultNamespace("saving_indicator"));

    private final Set<ResourceLocation> Own;

    Cutscene(Set<ResourceLocation> Own) {
        this.Own = Own;
    }

    public Set<ResourceLocation> own() {
        return Own;
    }

    public boolean shows(ResourceLocation Layer) {
        return AlwaysKept.contains(Layer) || Own.contains(Layer);
    }

    public static boolean hides(@Nullable Cutscene Active, ResourceLocation Layer) {
        return Active != null && !Active.shows(Layer);
    }
}
