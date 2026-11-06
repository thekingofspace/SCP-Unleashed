package net.rai.scp_unleashed.scp079;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.stranger.StrangerVariant;
import net.rai.scp_unleashed.stranger.StrangerVariants;

import java.util.List;

public final class Scp079Stranger {
    public static final String VariantId = "scp079_hobbyist";
    public static final int VanishColour = 0x3C8C4E;
    public static final int GlintColour = 0x9CF5B0;
    public static final int PlateColour = 0xE8DCB8;
    public static final float VoicePitch = 1.12f;

    public static final ResourceLocation Model = StrangerVariants.location("geo/entity/scp079/hobbyist.geo.json");
    public static final ResourceLocation Animations = StrangerVariants.location("animations/entity/scp079/hobbyist.animation.json");
    public static final ResourceLocation Texture = StrangerVariants.location("textures/entity/scp079/hobbyist.png");
    public static final ResourceLocation Portrait = StrangerVariants.location("textures/gui/scp079/portrait_hobbyist.png");

    public static final StrangerVariant Hobbyist = StrangerVariants.register(new StrangerVariant(VariantId, Model, Animations, Texture, Portrait,
            "entity.scp_unleashed.scp079_hobbyist", "gui.scp_unleashed.stranger.plate.scp079_hobbyist", "stranger.scp_unleashed.scp079_hobbyist",
            3, List.of(), VanishColour, GlintColour, PlateColour, VoicePitch));

    private Scp079Stranger() {
    }

    public static void register() {
    }
}
