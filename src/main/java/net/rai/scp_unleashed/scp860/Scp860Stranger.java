package net.rai.scp_unleashed.scp860;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.stranger.StrangerVariant;
import net.rai.scp_unleashed.stranger.StrangerVariants;

import java.util.List;

public final class Scp860Stranger {
    public static final String VariantId = "scp860_businessman";
    public static final int VanishColour = 0x1A1A20;
    public static final int GlintColour = 0xB8C0C8;
    public static final int PlateColour = 0x9EA4AC;
    public static final float VoicePitch = 0.72f;
    public static final int Pages = 4;

    public static final ResourceLocation Model = StrangerVariants.location("geo/entity/scp860/businessman.geo.json");
    public static final ResourceLocation Animations = StrangerVariants.location("animations/entity/scp860/businessman.animation.json");
    public static final ResourceLocation Texture = StrangerVariants.location("textures/entity/scp860/businessman.png");
    public static final ResourceLocation Portrait = StrangerVariants.location("textures/gui/scp860/portrait_businessman.png");

    public static final StrangerVariant Businessman = StrangerVariants.register(new StrangerVariant(VariantId, Model, Animations, Texture, Portrait,
            "entity.scp_unleashed.scp860_businessman", "gui.scp_unleashed.stranger.plate.scp860_businessman", "stranger.scp_unleashed.scp860",
            Pages, List.of(), VanishColour, GlintColour, PlateColour, VoicePitch));

    private Scp860Stranger() {
    }

    public static void register() {
    }
}
