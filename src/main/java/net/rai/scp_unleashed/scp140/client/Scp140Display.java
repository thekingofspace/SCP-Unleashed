package net.rai.scp_unleashed.scp140.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.rai.scp_unleashed.scp140.Scp140BookItem;
import net.rai.scp_unleashed.scp140.Scp140Motion;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.client.InteriorLayout;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

public final class Scp140Display {
    public static final float SmallScale = 0.66f;
    public static final float TallScale = 0.7f;
    public static final float FaceYaw = 180.0f;

    private Scp140Display() {
    }

    public static void register() {
        StorageLayouts.register(Scp140BookItem::is, Scp140Display::render);
    }

    public static void render(StorageLayoutContext Context) {
        Context.platform();
        if (Context.isEmpty()) {
            return;
        }
        Scp140Motion Motion = Scp140Shelf.motion(Context.Locker().getBlockPos(), Context.World().getGameTime());
        float Scale = Context.Tall() ? TallScale : SmallScale;
        float Top = InteriorLayout.platformTop(Context.Tall());
        PoseStack Pose = Context.Pose();
        Pose.pushPose();
        Pose.translate(StorageGeometry.CentreX / 16.0f, Top / 16.0f, StorageGeometry.CentreZ / 16.0f);
        Pose.mulPose(Axis.YP.rotationDegrees(FaceYaw));
        Pose.scale(Scale, Scale, Scale);
        Pose.translate(-0.5f, 0.0f, -0.5f);
        Scp140BookModels.render(Pose, Context::drawRawModel, Motion.open(Context.PartialTick()), Motion.leaf(Context.PartialTick()), Scp140ChronicleView.written() > 0);
        Pose.popPose();
    }
}
