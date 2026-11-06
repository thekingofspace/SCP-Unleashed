package net.rai.scp_unleashed.scp018;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.stranger.StrangerVariant;
import net.rai.scp_unleashed.stranger.StrangerVariants;

import java.util.List;

public final class Scp018Stranger {
    public static final String VariantId = "wondertainment";
    public static final int VanishColour = 0xD03C58;
    public static final int GlintColour = 0xB044E8;
    public static final int PlateColour = 0xF2DCA6;
    public static final float VoicePitch = 1.24f;

    public static final ResourceLocation Model = StrangerVariants.location("geo/entity/scp018/wondertainment.geo.json");
    public static final ResourceLocation Animations = StrangerVariants.location("animations/entity/scp018/wondertainment.animation.json");
    public static final ResourceLocation Texture = StrangerVariants.location("textures/entity/scp018/wondertainment.png");
    public static final ResourceLocation Portrait = StrangerVariants.location("textures/gui/scp018/portrait_wondertainment.png");

    public static final StrangerVariant Wondertainment = StrangerVariants.register(new StrangerVariant(VariantId, Model, Animations, Texture, Portrait,
            "entity.scp_unleashed.scp018_wondertainment", "gui.scp_unleashed.stranger.plate.scp018_wondertainment", "stranger.scp_unleashed.scp018_wondertainment",
            3, List.of(), VanishColour, GlintColour, PlateColour, VoicePitch));

    private Scp018Stranger() {
    }

    public static void register() {
    }
}
