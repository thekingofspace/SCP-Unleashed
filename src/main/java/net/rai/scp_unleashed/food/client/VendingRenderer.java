package net.rai.scp_unleashed.food.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.items.IItemHandler;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.food.Snack;
import net.rai.scp_unleashed.food.SnackItem;
import net.rai.scp_unleashed.food.SnackKind;
import net.rai.scp_unleashed.food.VendingMachineBlockEntity;

import java.util.Arrays;
import java.util.List;

public class VendingRenderer implements BlockEntityRenderer<VendingMachineBlockEntity> {
    public static final List<ResourceLocation> Models = Arrays.stream(Snack.values()).map(VendingRenderer::modelOf).toList();
    public static final float TopRowFloor = 22.0f;
    public static final float RowPitch = 5.0f;
    public static final float[] ColumnCentre = {12.5f, 7.5f};
    public static final float ShelfFront = 4.0f;
    public static final float ProductHeight = 4.0f;
    public static final float PushDistance = 2.95f;
    public static final float TrayFloor = 1.0f;
    public static final float[] TrayCentre = {12.4f, 7.6f, 10.1f, 13.2f};
    public static final float[] TrayDepth = {3.4f, 3.2f, 3.9f, 4.3f};
    public static final float[] TrayYaw = {14.0f, -12.0f, 58.0f, -36.0f};
    public static final float LingerTicks = 6.0f;

    public VendingRenderer(BlockEntityRendererProvider.Context Context) {
    }

    private static ResourceLocation modelOf(Snack Product) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/food/vending_machine/product_" + Product.getName());
    }

    public static float rowFloor(Snack Product) {
        return TopRowFloor - RowPitch * (Product.ordinal() / Snack.Columns);
    }

    public static float columnCentre(Snack Product) {
        return ColumnCentre[Product.ordinal() % Snack.Columns];
    }

    public static float width(Snack Product) {
        return Product.getKind() == SnackKind.Bag ? 4.0f : 3.0f;
    }

    public static float depth(Snack Product) {
        return Product.getKind() == SnackKind.Bag ? 2.0f : 3.0f;
    }

    @Override
    public void render(VendingMachineBlockEntity Machine, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Machine.getLevel();
        if (World == null) {
            return;
        }
        VertexConsumer Consumer = Buffers.getBuffer(Sheets.cutoutBlockSheet());
        Pose.pushPose();
        Pose.translate(0.5, 0.0, 0.5);
        Pose.mulPose(Axis.YP.rotationDegrees(-facingAngle(Machine.getFront())));
        Pose.translate(-0.5, 0.0, -0.5);
        int Light = LevelRenderer.getLightColor(World, Machine.getBlockPos().relative(Machine.getFront()));
        tray(Machine.getTray(), Pose, Consumer, Light);
        Snack Falling = Snack.byIndex(Machine.getShownVending());
        float Age = Machine.getShownAge(PartialTick);
        if (Falling != null && Age < VendingMachineBlockEntity.VendTicks + LingerTicks) {
            falling(Falling, Age, Pose, Consumer);
        }
        Pose.popPose();
    }

    private static void falling(Snack Product, float Age, PoseStack Pose, VertexConsumer Consumer) {
        float Push;
        float Drop;
        if (Age < VendingMachineBlockEntity.PushTicks) {
            float Fraction = Age / VendingMachineBlockEntity.PushTicks;
            Push = PushDistance * Fraction * Fraction * (3.0f - 2.0f * Fraction);
            Drop = 0.0f;
        } else {
            float Fraction = Math.min(1.0f, (Age - VendingMachineBlockEntity.PushTicks) / VendingMachineBlockEntity.FallTicks);
            Push = PushDistance;
            Drop = (rowFloor(Product) - TrayFloor) * Fraction * Fraction;
        }
        Pose.pushPose();
        Pose.translate(0.0, -Drop / 16.0, -Push / 16.0);
        model(Pose, Consumer, modelOf(Product), LightTexture.FULL_BRIGHT);
        Pose.popPose();
    }

    private static void tray(IItemHandler Tray, PoseStack Pose, VertexConsumer Consumer, int Light) {
        for (int Slot = 0; Slot < Tray.getSlots() && Slot < TrayCentre.length; Slot++) {
            ItemStack Stack = Tray.getStackInSlot(Slot);
            if (Stack.isEmpty() || !(Stack.getItem() instanceof SnackItem Snacked)) {
                continue;
            }
            Snack Product = Snacked.getProduct();
            Pose.pushPose();
            Pose.translate(TrayCentre[Slot] / 16.0, (TrayFloor + width(Product) / 2.0f) / 16.0, TrayDepth[Slot] / 16.0);
            Pose.mulPose(Axis.YP.rotationDegrees(TrayYaw[Slot]));
            Pose.mulPose(Axis.ZP.rotationDegrees(90.0f));
            Pose.translate(-columnCentre(Product) / 16.0, -(rowFloor(Product) + ProductHeight / 2.0f) / 16.0, -(ShelfFront + depth(Product) / 2.0f) / 16.0);
            model(Pose, Consumer, modelOf(Product), Light);
            Pose.popPose();
        }
    }

    private static void model(PoseStack Pose, VertexConsumer Consumer, ResourceLocation Location, int Light) {
        Minecraft Game = Minecraft.getInstance();
        BakedModel Baked = Game.getModelManager().getModel(ModelResourceLocation.standalone(Location));
        ModelBlockRenderer Renderer = Game.getBlockRenderer().getModelRenderer();
        Renderer.renderModel(Pose.last(), Consumer, null, Baked, 1.0f, 1.0f, 1.0f, Light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
    }

    private static float facingAngle(Direction Front) {
        return switch (Front) {
            case EAST -> 90.0f;
            case SOUTH -> 180.0f;
            case WEST -> 270.0f;
            default -> 0.0f;
        };
    }

    @Override
    public boolean shouldRenderOffScreen(VendingMachineBlockEntity Machine) {
        return false;
    }

    @Override
    public AABB getRenderBoundingBox(VendingMachineBlockEntity Machine) {
        return Machine.getRenderBounds();
    }
}
