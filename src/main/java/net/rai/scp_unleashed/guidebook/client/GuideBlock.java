package net.rai.scp_unleashed.guidebook.client;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public sealed interface GuideBlock {
    record Paragraph(GuideText Text) implements GuideBlock {
    }

    record Heading(GuideText Text) implements GuideBlock {
    }

    record Note(GuideText Text) implements GuideBlock {
    }

    record Step(@Nullable ResourceLocation Icon, GuideText Text, boolean Bullet) implements GuideBlock {
    }

    record Icons(List<ResourceLocation> Items, List<ResourceLocation> Links, boolean Flow, @Nullable GuideText Caption) implements GuideBlock {
        @Nullable
        public ResourceLocation linkAt(int Index) {
            return Index < Links.size() ? Links.get(Index) : null;
        }
    }

    record Picture(ResourceLocation Texture, int Width, int Height, int U, int V, int TextureWidth, int TextureHeight, @Nullable GuideText Caption) implements GuideBlock {
    }
}
