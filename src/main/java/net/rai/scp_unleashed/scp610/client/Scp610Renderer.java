package net.rai.scp_unleashed.scp610.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp610.Scp610Creature;
import net.rai.scp_unleashed.scp610.Scp610Form;
import net.rai.scp_unleashed.scp610.Scp610Stalker;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

public class Scp610Renderer<T extends Scp610Creature> extends GeoEntityRenderer<T> {
    public Scp610Renderer(EntityRendererProvider.Context Context, Scp610Form Form, float Shadow) {
        super(Context, new Scp610GeoModel<>(Form.id()));
        withScale(Form.scale());
        shadowRadius = Shadow;
        addRenderLayer(new Scp610GlowLayer<>(this, Form.id()));
    }

    @Override
    public void render(T Creature, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Creature.setRenderPartial(PartialTick);
        Creature.applyLockedYaw();
        super.render(Creature, Creature.locksYaw() ? Creature.getYRot() : Yaw, PartialTick, Pose, Buffers, Light);
    }

    public static final float HiddenAlpha = 0.12f;

    @Nullable
    @Override
    public RenderType getRenderType(T Creature, ResourceLocation Texture, @Nullable MultiBufferSource Buffers, float PartialTick) {
        if (Creature instanceof Scp610Stalker Stalker && Stalker.hidden()) {
            return RenderType.entityTranslucent(Texture);
        }
        return RenderType.entityCutoutNoCull(Texture);
    }

    @Override
    public Color getRenderColor(T Creature, float PartialTick, int PackedLight) {
        if (Creature instanceof Scp610Stalker Stalker && Stalker.hidden()) {
            return Color.ofRGBA(1.0f, 1.0f, 1.0f, HiddenAlpha);
        }
        return super.getRenderColor(Creature, PartialTick, PackedLight);
    }

    @Override
    public int getPackedOverlay(T Creature, float U, float PartialTick) {
        return Creature.deathTime > 0 ? OverlayTexture.NO_OVERLAY : super.getPackedOverlay(Creature, U, PartialTick);
    }

    @Override
    protected float getDeathMaxRotation(T Creature) {
        return 0.0f;
    }
}
