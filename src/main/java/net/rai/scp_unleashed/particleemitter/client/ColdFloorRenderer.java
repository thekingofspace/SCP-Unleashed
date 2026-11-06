package net.rai.scp_unleashed.particleemitter.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.particleemitter.ParticleEmitterBlockEntity;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

public final class ColdFloorRenderer {
    public static final ResourceLocation FogTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/entity/particle_emitter/fog.png");
    public static final int Wrap = 192;
    public static final int Period = 72000;
    public static final float[] LayerHeights = {0.05f, 0.21f, 0.4f};
    public static final float[] LayerAlpha = {0.9f, 0.75f, 0.55f};
    public static final float[] LayerTiles = {4.0f, 6.0f, 8.0f};
    public static final float[] LayerFlip = {1.0f, -1.0f, 1.0f};
    public static final float[] ScrollU = {0.004f, -0.003f, 0.002f};
    public static final float[] ScrollV = {0.0015f, 0.0035f, -0.004f};
    public static final float[] LayerShade = {0.92f, 0.96f, 1.0f};
    public static final float Red = 0.95f;
    public static final float Green = 0.97f;
    public static final float Blue = 1.0f;
    public static final float Wave = 0.035f;
    public static final float Breath = 0.14f;
    public static final float CurtainTop = 0.42f;
    public static final float CurtainFoot = 0.12f;
    public static final float CurtainTile = 4.0f;
    public static final float CurtainFall = 0.02f;
    public static final float[] CurtainInsets = {0.12f, 0.42f};
    public static final float[] CurtainShare = {1.0f, 0.6f};
    public static final float CurtainAlphaTop = 0.5f;
    public static final float CurtainAlphaBottom = 0.3f;
    public static final float StreamHalf = 0.3f;
    public static final float StreamFoot = 0.1f;
    public static final float StreamAlphaTop = 0.3f;
    public static final float StreamAlphaBottom = 0.42f;
    private static final int[][] CornerSteps = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
    private static final float Turn = Mth.TWO_PI / Wrap;

    private ColdFloorRenderer() {
    }

    public static void render(ParticleEmitterBlockEntity Emitter, float Partial, PoseStack Pose, MultiBufferSource Buffers) {
        ColdFloorEffect Fog = ColdFloorEffect.of(Emitter);
        Level World = Emitter.getLevel();
        if (Fog == null || World == null || !Fog.showing() || ShaderCompat.renderingShadowPass()) {
            return;
        }
        float Time = (World.getGameTime() % Period) + Partial;
        int Layers = layers(Minecraft.getInstance().options.particles().get());
        VertexConsumer Buffer = Buffers.getBuffer(RenderType.entityTranslucent(FogTexture));
        PoseStack.Pose Last = Pose.last();
        BlockPos Origin = Emitter.getBlockPos();
        ColdFloorField.Cell[] Cells = Fog.cells();
        float[] Shown = new float[4];
        for (int Slot = 0; Slot < Cells.length; Slot++) {
            ColdFloorField.Cell Here = Cells[Slot];
            if (!Here.Grounded() || !Fog.owned(Slot)) {
                continue;
            }
            float Peak = 0.0f;
            for (int Corner = 0; Corner < 4; Corner++) {
                Shown[Corner] = Fog.corner(Slot, Corner, Partial);
                Peak = Math.max(Peak, Shown[Corner]);
            }
            if (Peak < ColdFloorEffect.Visible) {
                continue;
            }
            float Rx = Here.X() - Origin.getX();
            float Ry = Here.Y() - Origin.getY();
            float Rz = Here.Z() - Origin.getZ();
            int BaseU = Math.floorMod(Here.X(), Wrap);
            int BaseV = Math.floorMod(Here.Z(), Wrap);
            int PackedLight = Fog.light(Slot);
            for (int Layer = 0; Layer < Layers; Layer++) {
                float Shade = LayerShade[Layer];
                for (int Corner = 0; Corner < 4; Corner++) {
                    int Dx = CornerSteps[Corner][0];
                    int Dz = CornerSteps[Corner][1];
                    int Cu = BaseU + Dx;
                    int Cv = BaseV + Dz;
                    float Y = Ry + Fog.height(Slot, Corner) + LayerHeights[Layer] + wave(Layer, Cu, Cv, Time);
                    float Alpha = Shown[Corner] * LayerAlpha[Layer] * breathe(Layer, Cu, Cv, Time);
                    float U = Cu / LayerTiles[Layer] * LayerFlip[Layer] + Time * ScrollU[Layer];
                    float V = Cv / LayerTiles[Layer] + Time * ScrollV[Layer];
                    vertex(Buffer, Last, Rx + Dx, Y, Rz + Dz, U, V, Shade, Alpha, PackedLight, 0.0f, 1.0f, 0.0f);
                }
            }
        }
        pours(Fog, Buffer, Last, Origin, Partial, Time, Math.min(Layers, CurtainInsets.length));
        stream(Fog, Buffer, Last, Origin, Partial, Time);
    }

    private static void pours(ColdFloorEffect Fog, VertexConsumer Buffer, PoseStack.Pose Last, BlockPos Origin, float Partial, float Time, int Sheets) {
        ColdFloorField.Cell[] Cells = Fog.cells();
        int[] Pours = Fog.pours();
        for (int Pour = 0; Pour < Pours.length; Pour++) {
            int Slot = Pours[Pour];
            ColdFloorField.Cell Here = Cells[Slot];
            float Strength = Fog.alpha(Slot, Partial);
            if (Strength < ColdFloorEffect.Visible) {
                continue;
            }
            float Rx = Here.X() - Origin.getX();
            float Ry = Here.Y() - Origin.getY();
            float Rz = Here.Z() - Origin.getZ();
            float Top = Ry + Fog.pourTop(Pour) + CurtainTop;
            float Bottom = Ry + Fog.pourBottom(Pour) + CurtainFoot;
            float Drop = (Top - Bottom) / CurtainTile;
            int PackedLight = Fog.light(Slot);
            boolean AlongX = Here.StepX() != 0;
            int Span = Math.floorMod(AlongX ? Here.Z() : Here.X(), Wrap);
            float Slide = Time * CurtainFall;
            for (int Sheet = 0; Sheet < Sheets; Sheet++) {
                float Inset = CurtainInsets[Sheet];
                float Share = CurtainShare[Sheet] * Strength;
                float TopAlpha = Share * CurtainAlphaTop;
                float BottomAlpha = Share * CurtainAlphaBottom;
                float U0 = Span / CurtainTile + Sheet * 0.37f;
                float U1 = (Span + 1) / CurtainTile + Sheet * 0.37f;
                float V0 = -Slide;
                float V1 = Drop - Slide;
                if (AlongX) {
                    float X = Rx + (Here.StepX() > 0 ? Inset : 1.0f - Inset);
                    float Nx = Here.StepX();
                    vertex(Buffer, Last, X, Top, Rz, U0, V0, 1.0f, TopAlpha, PackedLight, Nx, 0.0f, 0.0f);
                    vertex(Buffer, Last, X, Top, Rz + 1.0f, U1, V0, 1.0f, TopAlpha, PackedLight, Nx, 0.0f, 0.0f);
                    vertex(Buffer, Last, X, Bottom, Rz + 1.0f, U1, V1, 1.0f, BottomAlpha, PackedLight, Nx, 0.0f, 0.0f);
                    vertex(Buffer, Last, X, Bottom, Rz, U0, V1, 1.0f, BottomAlpha, PackedLight, Nx, 0.0f, 0.0f);
                } else {
                    float Z = Rz + (Here.StepZ() > 0 ? Inset : 1.0f - Inset);
                    float Nz = Here.StepZ();
                    vertex(Buffer, Last, Rx, Top, Z, U0, V0, 1.0f, TopAlpha, PackedLight, 0.0f, 0.0f, Nz);
                    vertex(Buffer, Last, Rx + 1.0f, Top, Z, U1, V0, 1.0f, TopAlpha, PackedLight, 0.0f, 0.0f, Nz);
                    vertex(Buffer, Last, Rx + 1.0f, Bottom, Z, U1, V1, 1.0f, BottomAlpha, PackedLight, 0.0f, 0.0f, Nz);
                    vertex(Buffer, Last, Rx, Bottom, Z, U0, V1, 1.0f, BottomAlpha, PackedLight, 0.0f, 0.0f, Nz);
                }
            }
        }
    }

    private static void stream(ColdFloorEffect Fog, VertexConsumer Buffer, PoseStack.Pose Last, BlockPos Origin, float Partial, float Time) {
        int Slot = Fog.stream();
        if (Slot < 0) {
            return;
        }
        float Strength = Fog.alpha(Slot, Partial);
        if (Strength < ColdFloorEffect.Visible) {
            return;
        }
        ColdFloorField.Cell Here = Fog.cells()[Slot];
        float Cx = Here.X() - Origin.getX() + 0.5f;
        float Cz = Here.Z() - Origin.getZ() + 0.5f;
        float Ry = Here.Y() - Origin.getY();
        float Top = Ry + Fog.streamTop();
        float Bottom = Ry + Fog.streamBottom() + StreamFoot;
        float Drop = (Top - Bottom) / CurtainTile;
        float Slide = Time * CurtainFall;
        float TopAlpha = Strength * StreamAlphaTop;
        float BottomAlpha = Strength * StreamAlphaBottom;
        int PackedLight = Fog.light(Slot);
        for (int Side = 0; Side < 2; Side++) {
            float Ax = -StreamHalf;
            float Az = Side == 0 ? -StreamHalf : StreamHalf;
            float Bx = StreamHalf;
            float Bz = -Az;
            float Nz = Side == 0 ? -0.7071f : 0.7071f;
            float U0 = Side * 0.5f;
            float U1 = U0 + 0.25f;
            vertex(Buffer, Last, Cx + Ax, Top, Cz + Az, U0, -Slide, 1.0f, TopAlpha, PackedLight, 0.7071f, 0.0f, Nz);
            vertex(Buffer, Last, Cx + Bx, Top, Cz + Bz, U1, -Slide, 1.0f, TopAlpha, PackedLight, 0.7071f, 0.0f, Nz);
            vertex(Buffer, Last, Cx + Bx, Bottom, Cz + Bz, U1, Drop - Slide, 1.0f, BottomAlpha, PackedLight, 0.7071f, 0.0f, Nz);
            vertex(Buffer, Last, Cx + Ax, Bottom, Cz + Az, U0, Drop - Slide, 1.0f, BottomAlpha, PackedLight, 0.7071f, 0.0f, Nz);
        }
    }

    private static float wave(int Layer, int Cu, int Cv, float Time) {
        float First = Mth.sin(Turn * (24 * Cu + 16 * Cv) + Time * 0.035f + Layer * 1.7f);
        float Second = Mth.sin(Turn * (-16 * Cu + 32 * Cv) + Time * 0.05f + Layer * 0.9f);
        return Wave * (First + 0.5f * Second) * (Layer + 1) / 3.0f;
    }

    private static float breathe(int Layer, int Cu, int Cv, float Time) {
        return 1.0f + Breath * Mth.sin(Turn * (40 * Cu + 24 * Cv) + Time * 0.045f + Layer * 2.1f);
    }

    private static int layers(ParticleStatus Status) {
        if (Status == ParticleStatus.ALL) {
            return LayerHeights.length;
        }
        return Status == ParticleStatus.DECREASED ? 2 : 1;
    }

    private static void vertex(VertexConsumer Buffer, PoseStack.Pose Last, float X, float Y, float Z, float U, float V, float Shade, float Alpha, int PackedLight,
                               float Nx, float Ny, float Nz) {
        Buffer.addVertex(Last, X, Y, Z)
                .setColor(Red * Shade, Green * Shade, Blue * Shade, Mth.clamp(Alpha, 0.0f, 1.0f))
                .setUv(U, V)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(PackedLight)
                .setNormal(Last, Nx, Ny, Nz);
    }
}
