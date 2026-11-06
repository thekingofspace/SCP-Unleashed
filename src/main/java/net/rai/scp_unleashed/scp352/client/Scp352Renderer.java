package net.rai.scp_unleashed.scp352.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp352.Scp352Crone;
import net.rai.scp_unleashed.scp352.Scp352Phase;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

public class Scp352Renderer extends GeoEntityRenderer<Scp352Crone> {
    public static final float LowShadow = 0.45f;
    public static final float TallShadow = 0.55f;
    public static final float RoostShadow = 0.75f;
    public static final float FadeStart = 26.0f;

    public Scp352Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp352GeoModel());
        shadowRadius = LowShadow;
    }

    public static float fade(Scp352Crone Creature, float Partial) {
        if (Creature.deathTime <= 0) {
            return 1.0f;
        }
        float Time = Creature.deathTime + Partial;
        return Mth.clamp(1.0f - (Time - FadeStart) / (Scp352Crone.DeathTicks - FadeStart), 0.0f, 1.0f);
    }

    @Override
    public void render(Scp352Crone Creature, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Creature.setRenderPartial(PartialTick);
        int Size = Creature.deathTime > 0 ? Creature.deathKind() : Creature.sizeClass();
        shadowRadius = (Size == Scp352Phase.Tall ? TallShadow : Size == Scp352Phase.Roost ? RoostShadow : LowShadow) * fade(Creature, PartialTick);
        super.render(Creature, Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Override
    public Color getRenderColor(Scp352Crone Creature, float Partial, int Light) {
        float Alpha = fade(Creature, Partial);
        return Alpha >= 1.0f ? super.getRenderColor(Creature, Partial, Light) : Color.ofRGBA(1.0f, 1.0f, 1.0f, Alpha);
    }

    @Override
    public RenderType getRenderType(Scp352Crone Creature, ResourceLocation Texture, @Nullable MultiBufferSource Buffers, float Partial) {
        return fade(Creature, Partial) < 1.0f ? RenderType.entityTranslucent(Texture) : super.getRenderType(Creature, Texture, Buffers, Partial);
    }

    @Override
    protected float getDeathMaxRotation(Scp352Crone Creature) {
        return 0.0f;
    }
}
