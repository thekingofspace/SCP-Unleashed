package net.rai.scp_unleashed.scp465.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.dclass.client.DClassRenderer;
import net.rai.scp_unleashed.scp465.Scp465Guest;

public class Scp465GuestRenderer extends DClassRenderer<Scp465Guest> {
    public Scp465GuestRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp465GuestModel());
    }

    @Override
    public void render(Scp465Guest Owner, float Yaw, float PartialTick, PoseStack Matrices, MultiBufferSource Buffers, int Light) {
        Owner.setRenderPartial(PartialTick);
        super.render(Owner, Yaw, PartialTick, Matrices, Buffers, Light);
    }
}
