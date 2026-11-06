package net.rai.scp_unleashed.z0001.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.storage.client.InteriorLayout;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageLayouts;
import net.rai.scp_unleashed.z0001.Z0001BookItem;
import net.rai.scp_unleashed.z0001.Z0001Ink;

import java.util.List;

public final class Z0001Display {
    public static final ModelResourceLocation Stand = standalone("display_stand");
    public static final ModelResourceLocation Book = standalone("display_book");
    public static final ModelResourceLocation InkedBook = standalone("display_book_inked");
    public static final float SmallScale = 0.5f;
    public static final float TallScale = 0.68f;
    public static final float PivotX = 8.0f;
    public static final float PivotY = 8.0f;
    public static final float PivotZ = 10.0f;
    public static final float Slant = 35.0f;
    public static final float FaceYaw = 0.0f;

    private Z0001Display() {
    }

    private static ModelResourceLocation standalone(String Name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/z0001/" + Name));
    }

    public static List<ModelResourceLocation> models() {
        return List.of(Stand, Book, InkedBook);
    }

    public static void register() {
        StorageLayouts.register(Z0001BookItem::is, Z0001Display::render);
    }

    public static void render(StorageLayoutContext Context) {
        Context.platform();
        if (Context.isEmpty()) {
            return;
        }
        float Scale = Context.Tall() ? TallScale : SmallScale;
        float Top = InteriorLayout.platformTop(Context.Tall());
        boolean Written = Z0001Ink.isSpent(Context.Stack(), Z0001Ink.today(Context.World()));
        PoseStack Pose = Context.Pose();
        Pose.pushPose();
        Pose.translate(Context.centreX() / 16.0f, Top / 16.0f, Context.centreZ() / 16.0f);
        if (FaceYaw != 0.0f) {
            Pose.mulPose(Axis.YP.rotationDegrees(FaceYaw));
        }
        Pose.scale(Scale, Scale, Scale);
        Context.model(Stand, Placement.Origin);
        Pose.translate((PivotX - 8.0f) / 16.0f, PivotY / 16.0f, (PivotZ - 8.0f) / 16.0f);
        Pose.mulPose(Axis.XP.rotationDegrees(-Slant));
        Context.model(Written ? InkedBook : Book, Placement.Origin);
        Pose.popPose();
    }
}
