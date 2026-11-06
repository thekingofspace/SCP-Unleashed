package net.rai.scp_unleashed.scp001scarlet.court.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.rai.scp_unleashed.scp001scarlet.court.AshWraith;
import net.rai.scp_unleashed.scp001scarlet.court.DaughterEyes;
import net.rai.scp_unleashed.scp001scarlet.court.DaughterMoon;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class CourtGlowLayer<T extends Entity & GeoAnimatable> extends GeoRenderLayer<T> {
    private final ResourceLocation Texture;
    private final ResourceLocation Flare;
    private final ResourceLocation Full;
    private final RenderType Glow;
    private final RenderType FlareGlow;
    private final RenderType FullGlow;
    @Nullable
    private Boolean Present;
    @Nullable
    private Boolean FlarePresent;
    @Nullable
    private Boolean FullPresent;

    public CourtGlowLayer(GeoRenderer<T> Renderer, String Name) {
        super(Renderer);
        Texture = CourtGeoModel.texture(Name, "_glowmask");
        Flare = CourtGeoModel.texture(Name, "_glowmask_flare");
        Glow = RenderType.eyes(Texture);
        FlareGlow = RenderType.eyes(Flare);
        Full = CourtGeoModel.texture(Name, "_glowmask_full");
        FullGlow = RenderType.eyes(Full);
    }

    private boolean fullPresent() {
        if (FullPresent == null) {
            FullPresent = Minecraft.getInstance().getResourceManager().getResource(Full).isPresent();
        }
        return FullPresent;
    }

    private boolean present() {
        if (Present == null) {
            Present = Minecraft.getInstance().getResourceManager().getResource(Texture).isPresent();
        }
        return Present;
    }

    private boolean flarePresent() {
        if (FlarePresent == null) {
            FlarePresent = Minecraft.getInstance().getResourceManager().getResource(Flare).isPresent();
        }
        return FlarePresent;
    }

    @Override
    public void render(PoseStack Pose, T Subject, BakedGeoModel Model, @Nullable RenderType Type, MultiBufferSource Buffers, @Nullable VertexConsumer Buffer,
                       float PartialTick, int PackedLight, int PackedOverlay) {
        if (!present() || Subject.isInvisible() || ShaderCompat.renderingShadowPass()) {
            return;
        }
        if (Subject instanceof DaughterMoon Moon && Moon.translucent() || Subject instanceof AshWraith Wraith && Wraith.phased()) {
            return;
        }
        RenderType Chosen = Glow;
        if (Subject instanceof DaughterEyes Eyes && Eyes.flaring() && flarePresent()) {
            Chosen = FlareGlow;
        } else if (Subject instanceof DaughterMoon Moon && Moon.fullMoon() && fullPresent()) {
            Chosen = FullGlow;
        }
        getRenderer().reRender(Model, Pose, Buffers, Subject, Chosen, Buffers.getBuffer(Chosen), PartialTick, LightTexture.FULL_BRIGHT, PackedOverlay, 0xFFFFFFFF);
    }
}
