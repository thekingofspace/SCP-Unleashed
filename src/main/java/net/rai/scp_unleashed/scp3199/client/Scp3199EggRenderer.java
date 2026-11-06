package net.rai.scp_unleashed.scp3199.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.rai.scp_unleashed.scp3199.Scp3199Egg;
import net.rai.scp_unleashed.scp3199.Scp3199Registry;

import java.util.ArrayList;
import java.util.List;

public class Scp3199EggRenderer extends EntityRenderer<Scp3199Egg> {
    public static final ModelResourceLocation FreshModel = ModelResourceLocation.standalone(Scp3199Registry.location("block/scp3199_egg_fresh"));
    private final RandomSource Random = RandomSource.create();
    private BakedModel CachedModel;
    private List<BakedQuad> CachedQuads = List.of();

    public Scp3199EggRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        shadowRadius = 0.0f;
        shadowStrength = 0.0f;
    }

    @Override
    public void render(Scp3199Egg Egg, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        List<BakedQuad> Quads = quads(Minecraft.getInstance().getModelManager().getModel(FreshModel));
        if (Quads.isEmpty()) {
            return;
        }
        Pose.pushPose();
        Pose.mulPose(Axis.YP.rotationDegrees(-Mth.lerp(PartialTick, Egg.yRotO, Egg.getYRot())));
        Pose.translate(-0.5, 0.0, -0.5);
        VertexConsumer Consumer = Buffers.getBuffer(Sheets.cutoutBlockSheet());
        PoseStack.Pose Last = Pose.last();
        for (BakedQuad Quad : Quads) {
            Consumer.putBulkData(Last, Quad, 1.0f, 1.0f, 1.0f, 1.0f, Light, OverlayTexture.NO_OVERLAY);
        }
        Pose.popPose();
    }

    private List<BakedQuad> quads(BakedModel Baked) {
        if (Baked != CachedModel) {
            CachedModel = Baked;
            List<BakedQuad> Found = new ArrayList<>();
            for (Direction Side : Direction.values()) {
                Random.setSeed(42L);
                Found.addAll(Baked.getQuads(null, Side, Random));
            }
            Random.setSeed(42L);
            Found.addAll(Baked.getQuads(null, null, Random));
            CachedQuads = Found;
        }
        return CachedQuads;
    }

    @Override
    public ResourceLocation getTextureLocation(Scp3199Egg Egg) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
