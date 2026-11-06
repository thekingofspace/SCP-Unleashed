package net.rai.scp_unleashed.scp224;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.stranger.StrangerVariant;
import net.rai.scp_unleashed.stranger.StrangerVariants;

import java.util.List;

public final class Scp224Stranger {
    public static final String VariantId = "scp224_grandfather";
    public static final int VanishColour = 0x4A3424;
    public static final int GlintColour = 0xE8C47C;
    public static final int PlateColour = 0xE6DCC2;
    public static final float VoicePitch = 0.68f;

    public static final ResourceLocation Model = StrangerVariants.location("geo/entity/scp224/grandfather.geo.json");
    public static final ResourceLocation Animations = StrangerVariants.location("animations/entity/scp224/grandfather.animation.json");
    public static final ResourceLocation Texture = StrangerVariants.location("textures/entity/scp224/grandfather.png");
    public static final ResourceLocation Portrait = StrangerVariants.location("textures/gui/scp224/portrait_grandfather.png");

    public static final StrangerVariant Grandfather = StrangerVariants.register(new StrangerVariant(VariantId, Model, Animations, Texture, Portrait,
            "entity.scp_unleashed.scp224_grandfather", "gui.scp_unleashed.stranger.plate.scp224_grandfather", "stranger.scp_unleashed.scp224_grandfather",
            3, List.of(), VanishColour, GlintColour, PlateColour, VoicePitch));

    private Scp224Stranger() {
    }

    public static void register() {
    }
}
