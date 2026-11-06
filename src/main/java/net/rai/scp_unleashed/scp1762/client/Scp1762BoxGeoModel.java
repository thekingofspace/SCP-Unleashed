package net.rai.scp_unleashed.scp1762.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.scp1762.Scp1762BoxBlockEntity;
import net.rai.scp_unleashed.scp1762.Scp1762Registry;
import net.rai.scp_unleashed.scp1762.Scp1762Stage;

public class Scp1762BoxGeoModel extends SharedGeoModel<Scp1762BoxBlockEntity> {
    public static final ResourceLocation ModelPath = Scp1762Registry.location("geo/block/scp1762/scp1762_box.geo.json");
    public static final ResourceLocation TexturePath = Scp1762Registry.location("textures/block/scp1762/box.png");
    public static final ResourceLocation LaterPath = Scp1762Registry.location("textures/block/scp1762/box_were.png");
    public static final ResourceLocation AnimationPath = Scp1762Registry.location("animations/block/scp1762/scp1762_box.animation.json");

    @Override
    public ResourceLocation getModelResource(Scp1762BoxBlockEntity Box) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp1762BoxBlockEntity Box) {
        return Scp1762Stage.free(Box.stage()) ? LaterPath : TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp1762BoxBlockEntity Box) {
        return AnimationPath;
    }

    @Override
    public RenderType getRenderType(Scp1762BoxBlockEntity Box, ResourceLocation Texture) {
        return RenderType.entityCutoutNoCull(Texture);
    }
}
