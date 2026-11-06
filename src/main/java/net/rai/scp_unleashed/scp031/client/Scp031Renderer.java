package net.rai.scp_unleashed.scp031.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp031.Scp031Mimic;

public class Scp031Renderer extends EntityRenderer<Scp031Mimic> {
    private final Scp031FleshRenderer Flesh;
    private final Scp031GuiseRenderer Guise;

    public Scp031Renderer(EntityRendererProvider.Context Context) {
        super(Context);
        this.Flesh = new Scp031FleshRenderer(Context);
        this.Guise = new Scp031GuiseRenderer(Context);
        this.shadowRadius = Scp031GuiseRenderer.ShadowSize;
    }

    @Override
    public Vec3 getRenderOffset(Scp031Mimic Mimic, float PartialTick) {
        return Scp031Disguises.isNear(Mimic) ? Guise.getRenderOffset(Mimic, PartialTick) : Vec3.ZERO;
    }

    @Override
    public void render(Scp031Mimic Mimic, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight) {
        if (Scp031Disguises.isNear(Mimic)) {
            shadowRadius = Scp031GuiseRenderer.ShadowSize;
            Guise.render(Mimic, Yaw, PartialTick, Pose, Buffers, PackedLight);
            return;
        }
        shadowRadius = Scp031FleshRenderer.ShadowSize;
        Flesh.render(Mimic, Yaw, PartialTick, Pose, Buffers, PackedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(Scp031Mimic Mimic) {
        return Scp031Disguises.isNear(Mimic) ? Guise.getTextureLocation(Mimic) : Scp031FleshModel.TexturePath;
    }
}
