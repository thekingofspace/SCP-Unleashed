package net.rai.scp_unleashed.marksman.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.client.GunRenderTypes;
import net.rai.scp_unleashed.marksman.CoinEntity;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class CoinRenderer extends EntityRenderer<CoinEntity> {
    public static final ModelResourceLocation CoinModel = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "item/marksman/coin"));
    public static final float Size = 0.42F;
    public static final float SpinPerTick = 38.0F;
    public static final float SpinSeed = 47.0F;
    public static final int LightFloor = 9;
    public static final float GlintSharpness = 8.0F;
    public static final float GlintSize = 0.5F;
    public static final float HaloSize = 0.16F;
    public static final float HaloAlpha = 0.22F;
    public static final int GlintSpikes = 4;
    private static final Direction[] QuadSides = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
    private final RandomSource Random = RandomSource.create();

    public CoinRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        this.shadowRadius = 0.0F;
    }

    public static float spin(CoinEntity Coin, float Partial) {
        return (Coin.tickCount + Partial) * SpinPerTick + Coin.getId() * SpinSeed;
    }

    @Override
    public void render(CoinEntity Coin, float EntityYaw, float Partial, PoseStack Pose, MultiBufferSource Buffers, int PackedLight) {
        float Spin = spin(Coin, Partial);
        float Heading = Coin.getYRot();
        Quaternionf Turn = new Quaternionf().rotateY(-Heading * Mth.DEG_TO_RAD).rotateX(Spin * Mth.DEG_TO_RAD);
        Pose.pushPose();
        Pose.translate(0.0F, (float) CoinEntity.HalfHeight, 0.0F);
        Pose.pushPose();
        Pose.mulPose(Turn);
        Pose.scale(Size, Size, Size);
        Pose.translate(-0.5F, -0.5F, -0.5F);
        BakedModel Model = Minecraft.getInstance().getModelManager().getModel(CoinModel);
        VertexConsumer Consumer = Buffers.getBuffer(Sheets.cutoutBlockSheet());
        int Light = LightTexture.pack(Math.max(LightTexture.block(PackedLight), LightFloor), LightTexture.sky(PackedLight));
        PoseStack.Pose Last = Pose.last();
        for (Direction Side : QuadSides) {
            Random.setSeed(42L);
            for (BakedQuad Quad : Model.getQuads(null, Side, Random, ModelData.EMPTY, null)) {
                Consumer.putBulkData(Last, Quad, 1.0F, 1.0F, 1.0F, 1.0F, Light, OverlayTexture.NO_OVERLAY);
            }
        }
        Pose.popPose();
        if (ShaderCompat.renderingShadowPass()) {
            Pose.popPose();
            return;
        }
        Vector3f Normal = Turn.transform(new Vector3f(0.0F, 0.0F, 1.0F));
        Vec3 Viewer = entityRenderDispatcher.camera.getPosition();
        Vec3 Toward = Viewer.subtract(Coin.centre(Partial));
        double Distance = Toward.length();
        float Facing = Distance < 1.0E-4 ? 0.0F : (float) Math.abs((Normal.x() * Toward.x + Normal.y() * Toward.y + Normal.z() * Toward.z) / Distance);
        float Glint = (float) Math.pow(Facing, GlintSharpness);
        Pose.mulPose(entityRenderDispatcher.cameraOrientation());
        Pose.translate(0.0F, 0.0F, 0.02F);
        VertexConsumer Glow = Buffers.getBuffer(GunRenderTypes.HandGlow);
        Matrix4f Matrix = Pose.last().pose();
        halo(Glow, Matrix, HaloSize, 1.0F, 0.7F, 0.25F, HaloAlpha + 0.3F * Glint);
        if (Glint > 0.03F) {
            star(Glow, Matrix, GlintSize * (0.35F + 0.65F * Glint), 1.0F, 0.94F, 0.72F, Glint);
        }
        Pose.popPose();
        super.render(Coin, EntityYaw, Partial, Pose, Buffers, PackedLight);
    }

    private static void halo(VertexConsumer Consumer, Matrix4f Matrix, float Radius, float Red, float Green, float Blue, float Alpha) {
        int Sides = 12;
        for (int Index = 0; Index < Sides; Index++) {
            double First = Mth.TWO_PI * Index / Sides;
            double Second = Mth.TWO_PI * (Index + 1) / Sides;
            fan(Consumer, Matrix, (float) (Math.cos(First) * Radius), (float) (Math.sin(First) * Radius), (float) (Math.cos(Second) * Radius), (float) (Math.sin(Second) * Radius),
                    Red, Green, Blue, Alpha);
        }
    }

    private static void star(VertexConsumer Consumer, Matrix4f Matrix, float Radius, float Red, float Green, float Blue, float Alpha) {
        int Points = GlintSpikes * 2;
        for (int Index = 0; Index < Points; Index++) {
            double First = Mth.TWO_PI * Index / Points;
            double Second = Mth.TWO_PI * (Index + 1) / Points;
            float RadiusFirst = Index % 2 == 0 ? Radius : Radius * 0.16F;
            float RadiusSecond = Index % 2 == 0 ? Radius * 0.16F : Radius;
            fan(Consumer, Matrix, (float) (Math.cos(First) * RadiusFirst), (float) (Math.sin(First) * RadiusFirst), (float) (Math.cos(Second) * RadiusSecond),
                    (float) (Math.sin(Second) * RadiusSecond), Red, Green, Blue, Alpha);
        }
    }

    private static void fan(VertexConsumer Consumer, Matrix4f Matrix, float Ax, float Ay, float Bx, float By, float Red, float Green, float Blue, float Alpha) {
        float Clamped = Mth.clamp(Alpha, 0.0F, 1.0F);
        Consumer.addVertex(Matrix, 0.0F, 0.0F, 0.0F).setColor(Red, Green, Blue, Clamped);
        Consumer.addVertex(Matrix, Ax, Ay, 0.0F).setColor(Red, Green, Blue, 0.0F);
        Consumer.addVertex(Matrix, Bx, By, 0.0F).setColor(Red, Green, Blue, 0.0F);
        Consumer.addVertex(Matrix, 0.0F, 0.0F, 0.0F).setColor(Red, Green, Blue, Clamped);
    }

    @Override
    public ResourceLocation getTextureLocation(CoinEntity Coin) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
