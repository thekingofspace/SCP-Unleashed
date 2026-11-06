package net.rai.scp_unleashed.guidebook.client;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record GuidePage(ResourceLocation Id, ResourceLocation Chapter, int Order, GuideText Title, @Nullable ResourceLocation Icon, List<GuideBlock> Blocks) {
}
