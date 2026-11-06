package net.rai.scp_unleashed.scp055.client;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp055.Scp055CaseBlock;
import net.rai.scp_unleashed.scp055.Scp055CaseItem;
import net.rai.scp_unleashed.scp055.Scp055Registry;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.client.InteriorLayout;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

import java.util.List;

public final class Scp055Display {
    public static final ModelResourceLocation Cradle = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/scp055/cradle"));
    public static final float SmallScale = 0.68f;
    public static final float TallScale = 0.82f;
    public static final float CradleLift = 1.0f;
    public static final float RestZ = 9.0f;

    private Scp055Display() {
    }

    public static List<ModelResourceLocation> models() {
        return List.of(Cradle);
    }

    public static void register() {
        StorageLayouts.register(Scp055CaseItem::is, StorageLayout.sized(Context -> onCradle(Context, false), Context -> onCradle(Context, true)));
    }

    private static void onCradle(StorageLayoutContext Context, boolean Tall) {
        float Base = Tall ? InteriorLayout.TallPlatformTop : StorageGeometry.CavityFloor;
        float Scale = Tall ? TallScale : SmallScale;
        if (Tall) {
            Context.platform();
        }
        Context.model(Cradle, Placement.at(StorageGeometry.CentreX, Base, RestZ).withScale(Scale));
        if (!Context.isEmpty()) {
            Context.block(Scp055Registry.Case.get().defaultBlockState().setValue(Scp055CaseBlock.FACING, Direction.SOUTH), Placement.at(StorageGeometry.CentreX, Base + CradleLift * Scale, RestZ).withScale(Scale));
        }
    }
}
