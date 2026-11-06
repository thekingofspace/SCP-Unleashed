package net.rai.scp_unleashed.stranger;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record StrangerVariant(String Id, ResourceLocation Model, ResourceLocation Animations, ResourceLocation Texture, ResourceLocation Portrait,
                              String NameKey, String PlateKey, String Dialogue, int Pages, List<StrangerGift> Gifts,
                              int VanishColour, int GlintColour, int PlateColour, float VoicePitch) {
    public StrangerVariant {
        Gifts = List.copyOf(Gifts);
        Pages = Math.max(1, Pages);
    }

    public Component displayName() {
        return Component.translatable(NameKey);
    }

    public Component plate() {
        return Component.translatable(PlateKey);
    }

    public Component page(String Prefix, int Index) {
        return Component.translatable(Prefix + "." + Index);
    }
}
