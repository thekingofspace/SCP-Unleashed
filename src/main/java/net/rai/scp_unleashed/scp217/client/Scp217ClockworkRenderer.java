package net.rai.scp_unleashed.scp217.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

public final class Scp217ClockworkRenderer {
    public static final float Pixel = 1.0f / 16.0f;
    public static final float Snap = 4.0f;
    public static final int FirstVisibleStage = 2;
    private static final Direction[] QuadSides = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
    private static final RandomSource Random = RandomSource.create();

    private Scp217ClockworkRenderer() {
    }

    public static boolean covered(LivingEntity Owner, String Part) {
        EquipmentSlot Slot = switch (Part) {
            case "head" -> EquipmentSlot.HEAD;
            case "right_leg", "left_leg" -> EquipmentSlot.LEGS;
            default -> EquipmentSlot.CHEST;
        };
        ItemStack Worn = Owner.getItemBySlot(Slot);
        return Worn.getItem() instanceof ArmorItem Armour && Armour.getEquipmentSlot() == Slot;
    }

    public static float angle(Scp217Layout.Piece Item, float Steps) {
        if (Item.Step() == 0.0f) {
            return Item.Roll();
        }
        float Whole = Mth.floor(Steps);
        float Eased = Mth.clamp((Steps - Whole) * Snap, 0.0f, 1.0f);
        Eased = Eased * Eased * (3.0f - 2.0f * Eased);
        return Item.Roll() + Item.Step() * (Whole + Eased);
    }

    public static void draw(PoseStack Pose, MultiBufferSource Buffers, int Light, LivingEntity Owner, String Part, int Stage, float Steps, boolean Slim, boolean FirstPerson) {
        draw(Pose, Buffers, Light, Owner, Part, Stage, Steps, Slim, FirstPerson, false);
    }

    public static void draw(PoseStack Pose, MultiBufferSource Buffers, int Light, LivingEntity Owner, String Part, int Stage, float Steps, boolean Slim, boolean FirstPerson,
                            boolean Perfect) {
        if (Stage < FirstVisibleStage) {
            return;
        }
        boolean Covered = covered(Owner, Part);
        for (Scp217Layout.Piece Item : Scp217Layout.pieces()) {
            if (Item.Stage() > Stage || !Item.Part().equals(Part) || FirstPerson && !Item.FirstPerson() || Covered && !Item.Armour()) {
                continue;
            }
            Scp217Layout.Shape Info = Scp217Layout.shape(Item.Model());
            if (Info == null) {
                continue;
            }
            BakedModel Model = Minecraft.getInstance().getModelManager().getModel(Info.pick(Perfect));
            Vector3f At = Item.At();
            Vector3f Shift = Slim ? Item.Slim() : new Vector3f();
            Pose.pushPose();
            Pose.translate((At.x() + Shift.x()) * Pixel, (At.y() + Shift.y()) * Pixel, (At.z() + Shift.z()) * Pixel);
            Pose.mulPose(Axis.YP.rotationDegrees(Item.Rotation().y()));
            Pose.mulPose(Axis.XP.rotationDegrees(Item.Rotation().x()));
            Pose.mulPose(Axis.ZP.rotationDegrees(Item.Rotation().z()));
            Pose.mulPose(Axis.XP.rotationDegrees(180.0f));
            float Turn = angle(Item, Steps);
            if (Turn != 0.0f) {
                Pose.mulPose(switch (Info.Axis()) {
                    case "x" -> Axis.XP.rotationDegrees(Turn);
                    case "y" -> Axis.YP.rotationDegrees(Turn);
                    default -> Axis.ZP.rotationDegrees(Turn);
                });
            }
            Pose.translate(-Info.Pivot().x() * Pixel, -Info.Pivot().y() * Pixel, -Info.Pivot().z() * Pixel);
            quads(Pose.last(), Buffers.getBuffer(Sheets.cutoutBlockSheet()), Model, Light);
            Pose.popPose();
        }
    }

    private static void quads(PoseStack.Pose Last, VertexConsumer Consumer, BakedModel Model, int Light) {
        for (Direction Side : QuadSides) {
            Random.setSeed(42L);
            for (BakedQuad Quad : Model.getQuads(null, Side, Random, ModelData.EMPTY, null)) {
                Consumer.putBulkData(Last, Quad, 1.0f, 1.0f, 1.0f, 1.0f, Light, OverlayTexture.NO_OVERLAY);
            }
        }
    }
}
