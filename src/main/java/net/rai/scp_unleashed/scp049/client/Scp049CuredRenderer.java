package net.rai.scp_unleashed.scp049.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp049.Scp049Cured;
import org.joml.Vector3f;

public class Scp049CuredRenderer extends EntityRenderer<Scp049Cured> {
    public static final int Tint = 0xFFA8DC94;
    public static final float ShadowSize = 0.5f;
    private static final ResourceLocation Fallback = ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");

    private final Scp049CuredVillagerRenderer VillagerLook;
    private final Scp049CuredPlayerRenderer PlayerLook;
    private final Scp049CuredPlainRenderer PlainLook;

    public Scp049CuredRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        this.VillagerLook = new Scp049CuredVillagerRenderer(Context);
        this.PlayerLook = new Scp049CuredPlayerRenderer(Context);
        this.PlainLook = new Scp049CuredPlainRenderer(Context);
        shadowRadius = ShadowSize;
    }

    public static float easeRise(float Progress) {
        float Share = Mth.clamp(Progress, 0.0f, 1.0f);
        return Share * Share * (3.0f - 2.0f * Share);
    }

    @Override
    public void render(Scp049Cured Risen, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight) {
        MultiBufferSource Tinted = new Scp049TintBuffers(Buffers, Tint);
        Pose.pushPose();
        float Rise = easeRise(Risen.riseProgress(PartialTick));
        if (Rise < 1.0f) {
            float Body = Mth.rotLerp(PartialTick, Risen.yBodyRotO, Risen.yBodyRot) * Mth.DEG_TO_RAD;
            Pose.mulPose(Axis.of(new Vector3f(Mth.cos(Body), 0.0f, Mth.sin(Body))).rotationDegrees(-90.0f * (1.0f - Rise)));
        }
        byte Kind = Risen.lookKind();
        if (Kind == Scp049Cured.LookVillager) {
            VillagerLook.render(Risen, Yaw, PartialTick, Pose, Tinted, PackedLight);
        } else if (Kind == Scp049Cured.LookPlayer && PlayerLook.canDraw(Risen)) {
            PlayerLook.render(Risen, Yaw, PartialTick, Pose, Tinted, PackedLight);
        } else if (Kind != Scp049Cured.LookMimic || !Scp049Mimicry.render(Risen, Yaw, PartialTick, Pose, Tinted, PackedLight)) {
            PlainLook.render(Risen, Yaw, PartialTick, Pose, Tinted, PackedLight);
        }
        Pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(Scp049Cured Risen) {
        return Fallback;
    }
}
