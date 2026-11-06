package net.rai.scp_unleashed.scp107.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.rai.scp_unleashed.scp107.Scp107RainKind;
import net.rai.scp_unleashed.scp107.Scp107ShellBlock;
import net.rai.scp_unleashed.scp107.Scp107ShellBlockEntity;

import java.util.Optional;

public class Scp107ShellRenderer implements BlockEntityRenderer<Scp107ShellBlockEntity> {
    public static final int LowestLevel = 4;
    public static final float FilmHeight = 5.35f;
    public static final float BrimHeight = 7.6f;
    public static final float SurgeShare = 0.12f;
    public static final float PourAlpha = 0.85f;
    public static final float ShimmerAlpha = 0.34f;
    public static final float ShimmerSwing = 0.12f;
    public static final ResourceLocation MilkStill = ResourceLocation.fromNamespaceAndPath("neoforge", "block/milk_still");
    public static final int MilkTint = 0xFFFFF8EE;
    private static final int[][][] Levels = {
            {{7, 5, 9, 11}, {6, 6, 7, 10}, {9, 6, 10, 10}},
            {{6, 4, 10, 12}, {5, 5, 6, 11}, {10, 5, 11, 11}, {4, 7, 5, 9}, {11, 7, 12, 9}},
            {{6, 3, 10, 13}, {5, 4, 6, 12}, {10, 4, 11, 12}, {4, 5, 5, 11}, {11, 5, 12, 11}},
            {{7, 2, 9, 14}, {5, 3, 7, 13}, {9, 3, 11, 13}, {4, 4, 5, 12}, {11, 4, 12, 12}, {3, 7, 4, 9}, {12, 7, 13, 9}}
    };

    public Scp107ShellRenderer(BlockEntityRendererProvider.Context Context) {
    }

    private static float smooth(float Value) {
        float Clamped = Mth.clamp(Value, 0.0f, 1.0f);
        return Clamped * Clamped * (3.0f - 2.0f * Clamped);
    }

    @Override
    public void render(Scp107ShellBlockEntity Shell, float Partial, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Shell.getLevel();
        BlockState State = Shell.getBlockState();
        if (World == null || !(State.getBlock() instanceof Scp107ShellBlock)) {
            return;
        }
        Optional<Scp107RainKind> Raining = Scp107ClientRain.kind();
        float Seconds = (World.getGameTime() + Partial) / 20.0f;
        float Shimmer = Raining.isPresent()
                ? (ShimmerAlpha + ShimmerSwing * Mth.sin(Seconds * 1.7f)) * Mth.clamp(Scp107ClientRain.strength(Partial) * 1.5f + 0.2f, 0.0f, 1.0f) : 0.0f;
        float Progress = Shell.pourProgress(Partial);
        Optional<Scp107RainKind> Poured = Shell.pouredKind();
        if (Progress >= 0.0f && Poured.isPresent()) {
            float Fill = Progress < SurgeShare ? smooth(Progress / SurgeShare) : 1.0f - smooth((Progress - SurgeShare) / (1.0f - SurgeShare));
            float Height = FilmHeight + (BrimHeight - FilmHeight) * Fill;
            float Alpha = Mth.lerp(Fill, Shimmer, PourAlpha);
            surface(Poured.get(), World, Shell.getBlockPos(), State, Height, Alpha, Pose, Buffers, PackedLight);
            return;
        }
        if (Raining.isPresent() && Shimmer > 0.01f) {
            surface(Raining.get(), World, Shell.getBlockPos(), State, FilmHeight, Shimmer, Pose, Buffers, PackedLight);
        }
    }

    private static void surface(Scp107RainKind Kind, Level World, BlockPos Pos, BlockState State, float Height, float Alpha, PoseStack Pose,
                                MultiBufferSource Buffers, int PackedLight) {
        if (Alpha <= 0.01f) {
            return;
        }
        float Level0 = Math.abs(Height - Math.round(Height)) < 0.03f ? Height + 0.05f : Height;
        int Index = Mth.clamp(Mth.ceil(Level0) - 1 - LowestLevel, 0, Levels.length - 1);
        Optional<Fluid> Liquid = Kind.fluid();
        ResourceLocation Still = MilkStill;
        int Tint = MilkTint;
        if (Liquid.isPresent()) {
            IClientFluidTypeExtensions Extensions = IClientFluidTypeExtensions.of(Liquid.get());
            Still = Extensions.getStillTexture();
            Tint = Extensions.getTintColor(Liquid.get().defaultFluidState(), World, Pos);
        }
        TextureAtlasSprite Sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(Still);
        float Red = ((Tint >> 16) & 255) / 255.0f;
        float Green = ((Tint >> 8) & 255) / 255.0f;
        float Blue = (Tint & 255) / 255.0f;
        int Light = Kind == Scp107RainKind.Lava ? LightTexture.FULL_BRIGHT : PackedLight;
        boolean Crosswise = State.getValue(Scp107ShellBlock.Facing).getAxis() == Direction.Axis.X;
        float Y = Level0 / 16.0f;
        VertexConsumer Consumer = Buffers.getBuffer(Sheets.translucentCullBlockSheet());
        PoseStack.Pose Last = Pose.last();
        for (int[] Rect : Levels[Index]) {
            float X0 = (Crosswise ? Rect[1] : Rect[0]) / 16.0f;
            float Z0 = (Crosswise ? Rect[0] : Rect[1]) / 16.0f;
            float X1 = (Crosswise ? Rect[3] : Rect[2]) / 16.0f;
            float Z1 = (Crosswise ? Rect[2] : Rect[3]) / 16.0f;
            corner(Consumer, Last, Sprite, X0, Y, Z0, Red, Green, Blue, Alpha, Light);
            corner(Consumer, Last, Sprite, X0, Y, Z1, Red, Green, Blue, Alpha, Light);
            corner(Consumer, Last, Sprite, X1, Y, Z1, Red, Green, Blue, Alpha, Light);
            corner(Consumer, Last, Sprite, X1, Y, Z0, Red, Green, Blue, Alpha, Light);
        }
    }

    private static void corner(VertexConsumer Consumer, PoseStack.Pose Last, TextureAtlasSprite Sprite, float X, float Y, float Z, float Red, float Green, float Blue,
                               float Alpha, int Light) {
        Consumer.addVertex(Last, X, Y, Z)
                .setColor(Red, Green, Blue, Alpha)
                .setUv(Sprite.getU(X), Sprite.getV(Z))
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(Light)
                .setNormal(Last, 0.0f, 1.0f, 0.0f);
    }
}
