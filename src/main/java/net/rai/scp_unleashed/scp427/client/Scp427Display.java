package net.rai.scp_unleashed.scp427.client;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp427.Scp427LocketItem;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.client.InteriorLayout;
import net.rai.scp_unleashed.storage.client.ItemAnchor;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

import java.util.List;

public final class Scp427Display {
    public static final ModelResourceLocation Cushion = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/scp427/display_cushion"));
    public static final float CushionTop = 8.25f;
    public static final float SmallScale = 0.5f;
    public static final float TallScale = 0.6f;
    public static final float RestYaw = 158.0f;
    public static final float RestPitch = -84.0f;
    public static final float Lift = 3.78f;
    public static final float ShiftX = -0.58f;
    public static final float ShiftZ = -1.89f;
    private static final Placement Origin = Placement.at(8.0f, 0.0f, 8.0f);

    private Scp427Display() {
    }

    public static List<ModelResourceLocation> models() {
        return List.of(Cushion);
    }

    public static void register() {
        StorageLayouts.register(Scp427LocketItem::is, StorageLayout.sized(Context -> onCushion(Context, false), Context -> onCushion(Context, true)));
    }

    public static float cushionLift(boolean Tall) {
        return InteriorLayout.platformTop(Tall) - InteriorLayout.platformTop(false);
    }

    private static void onCushion(StorageLayoutContext Context, boolean Tall) {
        Context.platform();
        float Raise = cushionLift(Tall);
        Context.model(Cushion, Origin.moved(0.0f, Raise, 0.0f));
        if (Context.isEmpty()) {
            return;
        }
        float Scale = Tall ? TallScale : SmallScale;
        Placement Resting = Placement.at(StorageGeometry.CentreX + ShiftX * Scale, CushionTop + Raise + Lift * Scale, StorageGeometry.CentreZ + ShiftZ * Scale)
                .withRotation(RestYaw, RestPitch, 0.0f).withScale(Scale);
        Context.item(Context.Stack(), Resting, ItemDisplayContext.NONE, ItemAnchor.Centre);
    }
}
