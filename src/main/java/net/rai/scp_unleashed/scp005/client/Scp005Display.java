package net.rai.scp_unleashed.scp005.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp005.Scp005Registry;
import net.rai.scp_unleashed.storage.client.ItemAnchor;
import net.rai.scp_unleashed.storage.client.LayoutMotion;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

import java.util.List;

public final class Scp005Display {
    public static final ModelResourceLocation Cushion = standalone("display_cushion");
    public static final ModelResourceLocation Board = standalone("display_board");
    public static final float RestX = 7.231f;
    public static final float RestY = 9.765f;
    public static final float RestZ = 8.904f;
    public static final float RestYaw = -28.0f;
    public static final float RestPitch = 70.0f;
    public static final float RestScale = 0.36f;
    public static final float HoverHeight = 0.22f;
    public static final float HoverPeriod = 120.0f;
    public static final float HookX = 8.0f;
    public static final float HookY = 23.0f;
    public static final float HookZ = 12.5f;
    public static final float HangDrop = 5.75f;
    public static final float HangScale = 0.58f;
    public static final float SwingAngle = 3.0f;
    public static final float SwingPeriod = 100.0f;
    private static final Placement PieceOrigin = Placement.at(8.0f, 0.0f, 8.0f);
    private static final Placement Resting = Placement.at(RestX, RestY, RestZ).withRotation(RestYaw, RestPitch, 0.0f).withScale(RestScale);
    private static final Placement Hanging = Placement.at(0.0f, -HangDrop, 0.0f).withRotation(0.0f, 0.0f, -90.0f).withScale(HangScale);
    private static final LayoutMotion Hover = LayoutMotion.bob(HoverHeight, HoverPeriod);
    private static final LayoutMotion Swing = LayoutMotion.sway(SwingAngle, SwingPeriod);

    private Scp005Display() {
    }

    private static ModelResourceLocation standalone(String Name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/scp005/" + Name));
    }

    public static List<ModelResourceLocation> models() {
        return List.of(Cushion, Board);
    }

    public static void register() {
        StorageLayouts.register(Scp005Registry.Key.get(), StorageLayout.sized(Scp005Display::onCushion, Scp005Display::onHook));
    }

    public static void onCushion(StorageLayoutContext Context) {
        Context.platform();
        Context.model(Cushion, PieceOrigin);
        if (!Context.isEmpty()) {
            float Lift = Hover.liftAt(Context.time(), Context.seed());
            Context.item(Context.Stack(), Resting.moved(0.0f, Lift, 0.0f), ItemDisplayContext.NONE, ItemAnchor.Centre);
        }
    }

    public static void onHook(StorageLayoutContext Context) {
        Context.model(Board, PieceOrigin);
        if (!Context.isEmpty()) {
            PoseStack Pose = Context.Pose();
            Pose.pushPose();
            Pose.translate(HookX / 16.0f, HookY / 16.0f, HookZ / 16.0f);
            Pose.mulPose(Axis.ZP.rotationDegrees(Swing.swayAt(Context.time(), Context.seed())));
            Context.item(Context.Stack(), Hanging, ItemDisplayContext.NONE, ItemAnchor.Centre);
            Pose.popPose();
        }
    }
}
