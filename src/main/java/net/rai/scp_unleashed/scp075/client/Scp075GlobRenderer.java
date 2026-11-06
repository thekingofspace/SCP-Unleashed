package net.rai.scp_unleashed.scp075.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp075.Scp075Glob;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp075GlobRenderer extends GeoEntityRenderer<Scp075Glob> {
    public static final String Name = "scp075_glob";
    private static final ResourceLocation ModelPath = Scp075GeoModel.location("geo/entity/scp075/" + Name + ".geo.json");
    private static final ResourceLocation TexturePath = Scp075GeoModel.location("textures/entity/scp075/" + Name + ".png");
    private static final ResourceLocation AnimationPath = Scp075GeoModel.location("animations/entity/scp075/" + Name + ".animation.json");

    public Scp075GlobRenderer(EntityRendererProvider.Context Context) {
        super(Context, new GeoModel<>() {
            @Override
            public ResourceLocation getModelResource(Scp075Glob Glob) {
                return ModelPath;
            }

            @Override
            public ResourceLocation getTextureResource(Scp075Glob Glob) {
                return TexturePath;
            }

            @Override
            public ResourceLocation getAnimationResource(Scp075Glob Glob) {
                return AnimationPath;
            }
        });
        shadowRadius = 0.12f;
    }
}
