package net.rai.scp_unleashed.z0003.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.client.InteriorLayout;
import net.rai.scp_unleashed.storage.client.ItemAnchor;
import net.rai.scp_unleashed.storage.client.ItemPose;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageLayouts;
import net.rai.scp_unleashed.z0003.Z0003DeviceItem;
import net.rai.scp_unleashed.z0003.Z0003Registry;

public final class Z0003Display {
    public static final float SmallScale = 0.18f;
    public static final float TallScale = 0.22f;
    public static final float ModelHalf = 15.0f;
    public static final float Lift = 0.6f;
    public static final float BobHeight = 0.6f;
    public static final float BobPeriod = 60.0f;
    public static final float SpinSpeed = 1.2f;
    public static final float Tilt = 8.0f;
    public static final float VialSmallScale = 0.5f;
    public static final float VialTallScale = 0.85f;
    public static final float VialYaw = 20.0f;
    public static final float VialRise = 7.0f;

    private Z0003Display() {
    }

    public static void register() {
        StorageLayouts.register(Z0003DeviceItem::is, Z0003Display::render);
        StorageLayouts.register(Z0003Registry.Vial.get(), StorageLayout.sized(vial(false), vial(true)));
    }

    public static InteriorLayout vial(boolean Tall) {
        float Scale = Tall ? VialTallScale : VialSmallScale;
        Placement Rest = Placement.at(StorageGeometry.CentreX, InteriorLayout.platformTop(Tall) + VialRise * Scale, StorageGeometry.CentreZ).withYaw(VialYaw).withScale(Scale);
        return InteriorLayout.pedestal(Tall).withStance(ItemPose.resting(Rest).withAnchor(ItemAnchor.Centre).withDisplay(ItemDisplayContext.NONE));
    }

    public static void render(StorageLayoutContext Context) {
        Context.platform();
        if (Context.isEmpty()) {
            return;
        }
        float Scale = Context.Tall() ? TallScale : SmallScale;
        float Time = Context.time();
        float Bob = Mth.sin(Time * Mth.TWO_PI / BobPeriod) * BobHeight;
        float Bottom = InteriorLayout.platformTop(Context.Tall()) + Lift + Bob;
        PoseStack Pose = Context.Pose();
        Pose.pushPose();
        Pose.translate(Context.centreX() / 16.0f, (Bottom + ModelHalf * Scale) / 16.0f, Context.centreZ() / 16.0f);
        Pose.mulPose(Axis.YP.rotationDegrees(Time * SpinSpeed + (Context.seed() % 360L)));
        Pose.mulPose(Axis.XP.rotationDegrees(Tilt));
        Pose.scale(Scale, Scale, Scale);
        Context.drawItem(Context.Stack(), ItemDisplayContext.NONE);
        Pose.popPose();
    }
}
