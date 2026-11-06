package net.rai.scp_unleashed.scp268.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.rai.scp_unleashed.scp268.Scp268Registry;
import net.rai.scp_unleashed.storage.client.InteriorLayout;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

import java.util.List;

public final class Scp268Display {
    public static final ModelResourceLocation Stand = standalone("display_stand");
    public static final ModelResourceLocation TallStand = standalone("display_stand_tall");
    public static final float SmallScale = 0.600f;
    public static final float TallScale = 0.600f;
    public static final float SmallSeat = 4.450f;
    public static final float TallSeat = 11.450f;
    public static final float CapBase = 3.0f;
    public static final float CapTop = 9.0f;
    public static final float FaceYaw = 30.0f;

    private Scp268Display() {
    }

    private static ModelResourceLocation standalone(String Name) {
        return ModelResourceLocation.standalone(Scp268Registry.location("block/scp268/" + Name));
    }

    public static List<ModelResourceLocation> models() {
        return List.of(Stand, TallStand);
    }

    public static void register() {
        StorageLayouts.register(Scp268Registry.Cap.get(), Scp268Display::render);
    }

    public static void render(StorageLayoutContext Context) {
        Context.platform();
        if (Context.isEmpty()) {
            return;
        }
        boolean Tall = Context.Tall();
        float Scale = Tall ? TallScale : SmallScale;
        float Seat = Tall ? TallSeat : SmallSeat;
        PoseStack Pose = Context.Pose();
        Pose.pushPose();
        Pose.translate(Context.centreX() / 16.0f, InteriorLayout.platformTop(Tall) / 16.0f, Context.centreZ() / 16.0f);
        if (FaceYaw != 0.0f) {
            Pose.mulPose(Axis.YP.rotationDegrees(FaceYaw));
        }
        Pose.scale(Scale, Scale, Scale);
        Context.model(Tall ? TallStand : Stand, Placement.Origin);
        Pose.translate(0.0f, (Seat - CapBase + 8.0f) / 16.0f, 0.0f);
        Context.drawItem(Context.Stack(), ItemDisplayContext.NONE);
        Pose.popPose();
    }
}
