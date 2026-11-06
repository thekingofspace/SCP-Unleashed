package net.rai.scp_unleashed.scp018.client;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp018.Scp018Registry;
import net.rai.scp_unleashed.storage.client.ItemAnchor;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

import java.util.List;

public final class Scp018Display {
    public static final ModelResourceLocation SmallClamp = standalone("clamp_small");
    public static final ModelResourceLocation TallClamp = standalone("clamp_tall");
    public static final float ModelPixels = 12.0f;
    public static final float BallX = 8.0f;
    public static final float BallZ = 9.0f;
    public static final float SmallDiameter = 4.0f;
    public static final float SmallCentreY = 10.0f;
    public static final float TallDiameter = 6.0f;
    public static final float TallCentreY = 21.0f;
    private static final Placement PieceOrigin = Placement.at(8.0f, 0.0f, 8.0f);

    private Scp018Display() {
    }

    private static ModelResourceLocation standalone(String Name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/scp018/" + Name));
    }

    public static List<ModelResourceLocation> models() {
        return List.of(SmallClamp, TallClamp);
    }

    public static void register() {
        StorageLayouts.register(Scp018Registry.BallItem.get(), StorageLayout.sized(Scp018Display::small, Scp018Display::tall));
    }

    public static void small(StorageLayoutContext Context) {
        clamped(Context, SmallClamp, SmallDiameter, SmallCentreY);
    }

    public static void tall(StorageLayoutContext Context) {
        clamped(Context, TallClamp, TallDiameter, TallCentreY);
    }

    private static void clamped(StorageLayoutContext Context, ModelResourceLocation Clamp, float Diameter, float CentreY) {
        Context.platform();
        Context.model(Clamp, PieceOrigin);
        if (Context.isEmpty()) {
            return;
        }
        Placement Where = Placement.at(BallX, CentreY, BallZ).withScale(Diameter / ModelPixels);
        Context.item(Context.Stack(), Where, ItemDisplayContext.NONE, ItemAnchor.Centre);
    }
}
