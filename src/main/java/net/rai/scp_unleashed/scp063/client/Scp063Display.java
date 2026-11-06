package net.rai.scp_unleashed.scp063.client;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp063.Scp063ToothbrushItem;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.client.InteriorLayout;
import net.rai.scp_unleashed.storage.client.ItemAnchor;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

import java.util.List;

public final class Scp063Display {
    public static final ModelResourceLocation LeafBed = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/scp063/leaf_bed"));
    public static final float SmallScale = 0.29f;
    public static final float TallScale = 0.37f;
    public static final float SmallYaw = 35.0f;
    public static final float TallYaw = 32.0f;
    public static final float Lift = 0.05f;
    public static final float ModelBottom = 7.0f;
    public static final float ModelCentreX = 8.5f;
    public static final float ModelMiddle = 8.0f;
    public static final float BedTop = 1.25f;
    public static final float SmallBedScale = 1.0f;
    public static final float TallBedScale = 1.25f;
    public static final float BedYaw = 8.0f;

    private Scp063Display() {
    }

    public static List<ModelResourceLocation> models() {
        return List.of(LeafBed);
    }

    public static void register() {
        StorageLayouts.register(Scp063ToothbrushItem::is, (StorageLayout) Scp063Display::onLeafBed);
    }

    public static float bedScale(boolean Tall) {
        return Tall ? TallBedScale : SmallBedScale;
    }

    public static Placement bed(boolean Tall) {
        return Placement.at(StorageGeometry.CentreX, InteriorLayout.platformTop(Tall), StorageGeometry.CentreZ).withYaw(BedYaw).withScale(bedScale(Tall));
    }

    public static Placement rest(boolean Tall) {
        float Scale = Tall ? TallScale : SmallScale;
        float Yaw = Tall ? TallYaw : SmallYaw;
        float Offset = (ModelCentreX - ModelMiddle) * Scale;
        float Top = InteriorLayout.platformTop(Tall) + BedTop * bedScale(Tall) + (ModelMiddle - ModelBottom) * Scale + Lift;
        float X = StorageGeometry.CentreX - Offset * Mth.cos(Yaw * Mth.DEG_TO_RAD);
        float Z = StorageGeometry.CentreZ + Offset * Mth.sin(Yaw * Mth.DEG_TO_RAD);
        return Placement.at(X, Top, Z).withYaw(Yaw).withScale(Scale);
    }

    private static void onLeafBed(StorageLayoutContext Context) {
        Context.platform();
        Context.model(LeafBed, bed(Context.Tall()));
        if (!Context.isEmpty()) {
            Context.item(Context.Stack(), rest(Context.Tall()), ItemDisplayContext.NONE, ItemAnchor.Centre);
        }
    }
}
