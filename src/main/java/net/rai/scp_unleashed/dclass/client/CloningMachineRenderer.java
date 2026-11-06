package net.rai.scp_unleashed.dclass.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.CloningKind;
import net.rai.scp_unleashed.dclass.CloningMachineBlockEntity;
import net.rai.scp_unleashed.dclass.DClassLike;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

public class CloningMachineRenderer implements BlockEntityRenderer<CloningMachineBlockEntity> {
    public static final ResourceLocation FluidSprite = ResourceLocation.withDefaultNamespace("block/water_still");
    public static final ResourceLocation BubbleSprite = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/dclass/cloning_bubble");
    public static final int FluidColour = 0x80E0806A;
    public static final int BubbleColour = 0xB8FFE6DC;
    public static final float Inset = 3.02f / 16.0f;
    public static final float Floor = 4.0f / 16.0f;
    public static final float FluidFloor = 4.02f / 16.0f;
    public static final float Depth = 23.94f / 16.0f;
    public static final float FullScale = 0.57f;
    public static final float SeedScale = 0.18f;
    public static final float FigureLift = 0.124f;
    public static final float FloatLift = 0.5f;
    public static final float BobHeight = 0.015f;
    public static final int Bubbles = 9;
    private static final Map<CloningKind, Function<ClientLevel, DClassLike>> Specimens = new EnumMap<>(CloningKind.class);

    public CloningMachineRenderer(BlockEntityRendererProvider.Context Context) {
    }

    public static void registerSpecimen(CloningKind Kind, Function<ClientLevel, DClassLike> Factory) {
        Specimens.put(Kind, Factory);
    }

    @Override
    public void render(CloningMachineBlockEntity Tank, float Partial, PoseStack Matrices, MultiBufferSource Buffers, int Light, int Overlay) {
        Direction Front = Tank.getFront();
        float Fill = Tank.fill(Partial);
        int Glow = LightTexture.pack(Math.max(LightTexture.block(Light), 11), LightTexture.sky(Light));
        Matrices.pushPose();
        Matrices.translate(0.5f, 0.0f, 0.5f);
        if (Tank.growing() != CloningKind.None) {
            specimen(Tank, Partial, Matrices, Buffers, Glow, Front);
        }
        if (Fill > 0.001f) {
            fluid(Tank, Partial, Matrices, Buffers, Glow, Fill);
        }
        Matrices.popPose();
    }

    private void specimen(CloningMachineBlockEntity Tank, float Partial, PoseStack Matrices, MultiBufferSource Buffers, int Light, Direction Front) {
        DClassLike Figure = figure(Tank);
        if (Figure == null) {
            return;
        }
        float Grown = Tank.shownProgress(Partial);
        float Eased = Grown * Grown * (3.0f - 2.0f * Grown);
        float Scale = FullScale * (SeedScale + (1.0f - SeedScale) * Eased);
        float Time = Tank.clientTicks() + Partial;
        float Bob = Mth.sin(Time * 0.06f) * BobHeight * Math.min(1.0f, Tank.fill(Partial) * 2.0f);
        Matrices.pushPose();
        Matrices.translate(0.0f, Floor + FigureLift + Bob + (1.0f - Eased) * FloatLift, 0.0f);
        Matrices.scale(Scale, Scale, Scale);
        float Yaw = Front.toYRot();
        Figure.setYRot(Yaw);
        Figure.yRotO = Yaw;
        Figure.yBodyRot = Yaw;
        Figure.yBodyRotO = Yaw;
        Figure.yHeadRot = Yaw;
        Figure.yHeadRotO = Yaw;
        EntityRenderDispatcher Dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        Dispatcher.setRenderShadow(false);
        Dispatcher.render(Figure, 0.0, 0.0, 0.0, Yaw, Partial, Matrices, Buffers, Light);
        Dispatcher.setRenderShadow(true);
        Matrices.popPose();
    }

    @Nullable
    public static DClassLike figure(CloningMachineBlockEntity Tank) {
        if (Tank.specimenMatches()) {
            return Tank.specimen();
        }
        if (!(Tank.getLevel() instanceof ClientLevel World)) {
            return null;
        }
        Function<ClientLevel, DClassLike> Factory = Specimens.get(Tank.growing());
        DClassLike Made = Factory == null ? null : Factory.apply(World);
        if (Made != null) {
            Made.setAppearance(Tank.appearance());
            Made.applySample(Tank.sample());
            Made.markSpecimen();
            Made.setPos(Tank.getBlockPos().getX() + 0.5, Tank.getBlockPos().getY() + Floor, Tank.getBlockPos().getZ() + 0.5);
        }
        Tank.setSpecimen(Made, Tank.growing(), Tank.appearance());
        return Made;
    }

    private void fluid(CloningMachineBlockEntity Tank, float Partial, PoseStack Matrices, MultiBufferSource Buffers, int Light, float Fill) {
        TextureAtlasSprite Water = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(FluidSprite);
        TextureAtlasSprite Bubble = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(BubbleSprite);
        VertexConsumer Consumer = Buffers.getBuffer(Sheets.translucentCullBlockSheet());
        PoseStack.Pose Last = Matrices.last();
        float Low = -0.5f + Inset;
        float High = 0.5f - Inset;
        float Top = FluidFloor + Depth * Fill;
        box(Consumer, Last, Water, Low, FluidFloor, Low, High, Top, High, FluidColour, Light);
        float Time = Tank.clientTicks() + Partial;
        for (int Index = 0; Index < Bubbles; Index++) {
            float Seed = Index * 0.618034f;
            float Rise = (Time * (0.012f + (Seed % 1.0f) * 0.01f) + Seed) % 1.0f;
            float Y = FluidFloor + 0.05f + Rise * (Top - FluidFloor - 0.12f);
            if (Y > Top - 0.06f) {
                continue;
            }
            float X = Low + 0.08f + ((Seed * 7.31f) % 1.0f) * (High - Low - 0.16f) + Mth.sin(Time * 0.2f + Index) * 0.02f;
            float Z = Low + 0.08f + ((Seed * 3.17f) % 1.0f) * (High - Low - 0.16f);
            float Size = (Index % 3 == 0 ? 1.5f : 1.0f) / 16.0f;
            box(Consumer, Last, Bubble, X, Y, Z, X + Size, Y + Size, Z + Size, BubbleColour, LightTexture.FULL_BRIGHT);
        }
    }

    private static void box(VertexConsumer Consumer, PoseStack.Pose Last, TextureAtlasSprite Sprite, float X0, float Y0, float Z0, float X1, float Y1, float Z1, int Colour, int Light) {
        float U0 = Sprite.getU0();
        float U1 = Sprite.getU(0.5f);
        float V0 = Sprite.getV0();
        float V1 = Sprite.getV(0.5f);
        quad(Consumer, Last, Colour, Light, 0, 1, 0, U0, V0, U1, V1, X0, Y1, Z0, X0, Y1, Z1, X1, Y1, Z1, X1, Y1, Z0);
        quad(Consumer, Last, Colour, Light, 0, -1, 0, U0, V0, U1, V1, X0, Y0, Z1, X0, Y0, Z0, X1, Y0, Z0, X1, Y0, Z1);
        quad(Consumer, Last, Colour, Light, 0, 0, -1, U0, V0, U1, V1, X1, Y1, Z0, X1, Y0, Z0, X0, Y0, Z0, X0, Y1, Z0);
        quad(Consumer, Last, Colour, Light, 0, 0, 1, U0, V0, U1, V1, X0, Y1, Z1, X0, Y0, Z1, X1, Y0, Z1, X1, Y1, Z1);
        quad(Consumer, Last, Colour, Light, -1, 0, 0, U0, V0, U1, V1, X0, Y1, Z0, X0, Y0, Z0, X0, Y0, Z1, X0, Y1, Z1);
        quad(Consumer, Last, Colour, Light, 1, 0, 0, U0, V0, U1, V1, X1, Y1, Z1, X1, Y0, Z1, X1, Y0, Z0, X1, Y1, Z0);
    }

    private static void quad(VertexConsumer Consumer, PoseStack.Pose Last, int Colour, int Light, float Nx, float Ny, float Nz, float U0, float V0, float U1, float V1,
                             float Ax, float Ay, float Az, float Bx, float By, float Bz, float Cx, float Cy, float Cz, float Dx, float Dy, float Dz) {
        Consumer.addVertex(Last, Ax, Ay, Az).setColor(Colour).setUv(U0, V0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Light).setNormal(Last, Nx, Ny, Nz);
        Consumer.addVertex(Last, Bx, By, Bz).setColor(Colour).setUv(U0, V1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Light).setNormal(Last, Nx, Ny, Nz);
        Consumer.addVertex(Last, Cx, Cy, Cz).setColor(Colour).setUv(U1, V1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Light).setNormal(Last, Nx, Ny, Nz);
        Consumer.addVertex(Last, Dx, Dy, Dz).setColor(Colour).setUv(U1, V0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Light).setNormal(Last, Nx, Ny, Nz);
    }

    @Override
    public boolean shouldRenderOffScreen(CloningMachineBlockEntity Tank) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(CloningMachineBlockEntity Tank) {
        return Tank.getRenderBounds();
    }
}
