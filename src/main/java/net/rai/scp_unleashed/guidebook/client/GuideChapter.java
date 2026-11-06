package net.rai.scp_unleashed.guidebook.client;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record GuideChapter(ResourceLocation Id, int Order, GuideText Title, @Nullable ResourceLocation Icon, List<GuidePage> Pages) {
}
