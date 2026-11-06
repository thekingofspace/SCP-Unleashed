package net.rai.scp_unleashed.scp178.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.item.ItemDisplayContext;
import net.rai.scp_unleashed.scp178.Scp178GlassesItem;
import net.rai.scp_unleashed.storage.client.InteriorLayout;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

public final class Scp178Display {
    public static final float SmallScale = 0.42f;
    public static final float TallScale = 0.56f;
    public static final float Tilt = -23.6f;
    public static final float Rest = 2.356f;
    public static final float Middle = -2.451f;
    public static final float FaceYaw = 192.0f;

    private Scp178Display() {
    }

    public static void register() {
        StorageLayouts.register(Stack -> Stack.getItem() instanceof Scp178GlassesItem, StorageLayout.sized(
                Context -> rest(Context, SmallScale),
                Context -> rest(Context, TallScale)));
    }

    private static void rest(StorageLayoutContext Context, float Scale) {
        Context.platform();
        if (Context.isEmpty()) {
            return;
        }
        PoseStack Pose = Context.Pose();
        Pose.pushPose();
        Pose.translate(Context.centreX() / 16.0f, (InteriorLayout.platformTop(Context.Tall()) + Rest * Scale) / 16.0f, Context.centreZ() / 16.0f);
        Pose.mulPose(Axis.YP.rotationDegrees(FaceYaw));
        Pose.translate(0.0f, 0.0f, -Middle * Scale / 16.0f);
        Pose.mulPose(Axis.XP.rotationDegrees(Tilt));
        Pose.scale(Scale, Scale, Scale);
        Context.drawItem(Context.Stack(), ItemDisplayContext.NONE);
        Pose.popPose();
    }
}
