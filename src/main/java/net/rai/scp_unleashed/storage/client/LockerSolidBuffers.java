package net.rai.scp_unleashed.storage.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;

public record LockerSolidBuffers(MultiBufferSource Inner) implements MultiBufferSource {
    @Override
    public VertexConsumer getBuffer(RenderType Type) {
        return Inner.getBuffer(solid(Type));
    }

    public static RenderType solid(RenderType Type) {
        return LockerItemBuffers.isLooseTranslucent(Type) || Type == Sheets.translucentCullBlockSheet() ? Sheets.cutoutBlockSheet() : Type;
    }
}
