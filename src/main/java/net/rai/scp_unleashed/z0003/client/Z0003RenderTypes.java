package net.rai.scp_unleashed.z0003.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import foundry.veil.api.client.render.VeilRenderBridge;
import foundry.veil.api.client.render.VeilRenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import net.rai.scp_unleashed.shadercompat.client.VeilPost;
import net.rai.scp_unleashed.z0003.Z0003Registry;

import java.util.List;
import java.util.function.Function;

public final class Z0003RenderTypes {
    public static final ResourceLocation StoneTexture = Z0003Registry.location("textures/entity/z0003/stone.png");
    public static final List<ResourceLocation> CrackedTextures = List.of(
            Z0003Registry.location("textures/entity/z0003/stone_cracked_0.png"),
            Z0003Registry.location("textures/entity/z0003/stone_cracked_1.png"),
            Z0003Registry.location("textures/entity/z0003/stone_cracked_2.png"),
            Z0003Registry.location("textures/entity/z0003/stone_cracked_3.png"));
    public static final ResourceLocation GlowTexture = Z0003Registry.location("textures/entity/z0003/glow.png");
    public static final ResourceLocation PlasmaTexture = Z0003Registry.location("textures/entity/z0003/plasma.png");
    public static final ResourceLocation HaloTexture = Z0003Registry.location("textures/entity/z0003/halo.png");

    public static final ResourceLocation StoneProgram = VeilPost.program("z0003/stone");
    public static final ResourceLocation FieldProgram = VeilPost.program("z0003/field");

    public static final RenderType FieldGlow = RenderType.create("scp_unleashed_z0003_field_glow", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 8192,
            false, false, RenderType.CompositeState.builder()
                    .setShaderState(VeilRenderBridge.shaderState(FieldProgram))
                    .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.CULL)
                    .createCompositeState(false));
    public static final RenderType FieldBody = RenderType.create("scp_unleashed_z0003_field_body", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 8192,
            false, false, RenderType.CompositeState.builder()
                    .setShaderState(VeilRenderBridge.shaderState(FieldProgram))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.CULL)
                    .createCompositeState(false));

    private static final Function<RenderType, RenderType> Stoned = Util.memoize(Z0003RenderTypes::stoned);
    private static final Function<ResourceLocation, RenderType> Additive = Util.memoize(Texture -> RenderType.create("scp_unleashed_z0003_additive",
            DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_EYES_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(Texture, false, false))
                    .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false)));

    private Z0003RenderTypes() {
    }

    public static boolean fieldShaded() {
        return !ShaderCompat.useFallback() && VeilPost.programReady(FieldProgram);
    }

    public static RenderType fallbackGlow(ResourceLocation Texture) {
        return RenderType.entityTranslucentEmissive(Texture);
    }

    public static RenderType fallbackBody(ResourceLocation Texture) {
        return RenderType.entityTranslucent(Texture);
    }

    public static boolean fallback() {
        return ShaderCompat.useFallback() || !VeilPost.programReady(StoneProgram);
    }

    public static RenderType stone(RenderType Original) {
        return Stoned.apply(Original);
    }

    public static RenderType fallbackStone(float Crack) {
        if (Crack <= 0.0f) {
            return RenderType.entityCutoutNoCull(StoneTexture);
        }
        int Stage = Math.min(CrackedTextures.size() - 1, (int) (Crack * CrackedTextures.size()));
        return RenderType.entityCutoutNoCull(CrackedTextures.get(Stage));
    }

    private static RenderType stoned(RenderType Original) {
        return new RenderType("scp_unleashed_z0003_stone", Original.format(), Original.mode(), Original.bufferSize(), false, Original.sortOnUpload(),
                () -> {
                    Original.setupRenderState();
                    VeilRenderSystem.setShader(StoneProgram);
                }, Original::clearRenderState) {
        };
    }

    public static RenderType glow(ResourceLocation Texture) {
        return ShaderCompat.useFallback() ? RenderType.entityTranslucentEmissive(Texture) : Additive.apply(Texture);
    }

    public static boolean additive() {
        return !ShaderCompat.useFallback();
    }
}
