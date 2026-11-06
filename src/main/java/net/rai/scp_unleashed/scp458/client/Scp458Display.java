package net.rai.scp_unleashed.scp458.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp458.Scp458BoxItem;
import net.rai.scp_unleashed.scp458.Scp458Contents;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

public final class Scp458Display {
    public static final float Scale = 0.68f;
    public static final float RestZ = 8.7f;
    public static final float SmallLidLimit = 42.0f;
    public static final float TallLidLimit = 88.0f;

    private Scp458Display() {
    }

    public static void register() {
        StorageLayouts.register(Scp458BoxItem::is, StorageLayout.sized(Context -> flat(Context, SmallLidLimit), Context -> flat(Context, TallLidLimit)));
    }

    public static Placement rest() {
        return Placement.at(StorageGeometry.CentreX, StorageGeometry.CavityFloor, RestZ).withScale(Scale);
    }

    private static void flat(StorageLayoutContext Context, float LidLimit) {
        if (Context.isEmpty()) {
            return;
        }
        ItemStack Shown = Context.Stack();
        Scp458Contents Held = Scp458BoxItem.contents(Shown);
        PoseStack Pose = Context.Pose();
        Pose.pushPose();
        rest().apply(Pose);
        Pose.translate(-0.5f, 0.0f, -0.5f);
        Scp458Models.draw(Pose, Context::drawRawModel, Math.min(Scp458BoxItemRenderer.angle(Shown, Context.World().getGameTime(), Context.PartialTick()), LidLimit),
                Held.Pizza().orElse(null), Held.Taken());
        Pose.popPose();
    }
}
