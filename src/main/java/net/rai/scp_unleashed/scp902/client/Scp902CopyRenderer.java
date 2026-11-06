package net.rai.scp_unleashed.scp902.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.playerclone.client.PlayerCloneSkins;
import net.rai.scp_unleashed.scp902.Scp902Copy;

public class Scp902CopyRenderer extends EntityRenderer<Scp902Copy> {
    private final Scp902CopyBodyRenderer Wide;
    private final Scp902CopyBodyRenderer Slim;

    public Scp902CopyRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        Wide = new Scp902CopyBodyRenderer(Context, false);
        Slim = new Scp902CopyBodyRenderer(Context, true);
        shadowRadius = 0.5f;
    }

    public static PlayerSkin skinOf(Scp902Copy Copy) {
        return PlayerCloneSkins.skinOf(Copy.profile(), Copy.owner() != null ? Copy.owner() : Copy.getUUID());
    }

    @Override
    public ResourceLocation getTextureLocation(Scp902Copy Copy) {
        return skinOf(Copy).texture();
    }

    @Override
    public void render(Scp902Copy Copy, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        PlayerSkin Skin = skinOf(Copy);
        Scp902CopyBodyRenderer Body = Skin.model() == PlayerSkin.Model.SLIM ? Slim : Wide;
        Body.render(Copy, Yaw, PartialTick, Pose, Buffers, Light);
    }
}
