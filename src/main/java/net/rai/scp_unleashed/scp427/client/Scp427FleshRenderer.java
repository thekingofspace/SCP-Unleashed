package net.rai.scp_unleashed.scp427.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp427.Scp427Flesh;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

public class Scp427FleshRenderer extends GeoEntityRenderer<Scp427Flesh> {
    public static final float ShadowSize = 0.9f;
    public static final float FadeStart = 26.0f;

    public Scp427FleshRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp427FleshModel());
        shadowRadius = ShadowSize;
    }

    @Override
    public void render(Scp427Flesh Creature, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Creature.setRenderPartial(PartialTick);
        super.render(Creature, Yaw, PartialTick, Pose, Buffers, Light);
    }

    public static float fade(Scp427Flesh Creature, float PartialTick) {
        if (Creature.deathTime <= 0) {
            return 1.0f;
        }
        float Dying = Creature.deathTime + PartialTick;
        return Mth.clamp(1.0f - (Dying - FadeStart) / (Scp427Flesh.DeathTicks - FadeStart), 0.0f, 1.0f);
    }

    @Override
    public Color getRenderColor(Scp427Flesh Creature, float PartialTick, int PackedLight) {
        float Shown = fade(Creature, PartialTick);
        return Shown >= 1.0f ? Color.WHITE : Color.ofRGBA(1.0f, 1.0f, 1.0f, Shown);
    }

    @Override
    public RenderType getRenderType(Scp427Flesh Creature, ResourceLocation Texture, @Nullable MultiBufferSource Buffers, float PartialTick) {
        return fade(Creature, PartialTick) < 1.0f ? RenderType.entityTranslucent(Texture) : RenderType.entityCutoutNoCull(Texture);
    }

    @Override
    protected float getDeathMaxRotation(Scp427Flesh Creature) {
        return 0.0f;
    }
}
