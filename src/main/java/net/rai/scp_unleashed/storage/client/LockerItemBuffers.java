package net.rai.scp_unleashed.storage.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.neoforged.neoforge.client.NeoForgeRenderTypes;

public record LockerItemBuffers(MultiBufferSource Inner) implements MultiBufferSource {
    @Override
    public VertexConsumer getBuffer(RenderType Type) {
        return Inner.getBuffer(redirect(Type));
    }

    public static boolean isLooseTranslucent(RenderType Type) {
        return Type == NeoForgeRenderTypes.ITEM_LAYERED_TRANSLUCENT.get() || Type == NeoForgeRenderTypes.ITEM_UNSORTED_TRANSLUCENT.get()
                || Type == Sheets.translucentItemSheet();
    }

    public static RenderType redirect(RenderType Type) {
        return isLooseTranslucent(Type) ? Sheets.translucentCullBlockSheet() : Type;
    }
}
