package net.rai.scp_unleashed.scp1762.client;

import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp1762.Scp1762BoxItem;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.client.ItemAnchor;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp1762Display {
    public static final float SmallScale = 0.66f;
    public static final float TallScale = 0.7f;
    public static final float SmallYaw = 190.0f;
    public static final float TallYaw = 186.0f;
    public static final float RestZ = 9.0f;
    private static final Placement SmallRest = Placement.at(StorageGeometry.CentreX, StorageGeometry.CavityFloor, RestZ).withYaw(SmallYaw).withScale(SmallScale);
    private static final Placement TallRest = Placement.at(StorageGeometry.CentreX, StorageGeometry.CavityFloor, RestZ).withYaw(TallYaw).withScale(TallScale);
    public static final StorageLayout Layout = StorageLayout.sized(Context -> onFloor(Context, SmallRest), Context -> onFloor(Context, TallRest));

    private Scp1762Display() {
    }

    public static void register() {
        StorageLayouts.register(Scp1762BoxItem::is, Layout);
    }

    private static void onFloor(StorageLayoutContext Context, Placement Rest) {
        if (Context.isEmpty()) {
            return;
        }
        Context.item(Context.Stack(), Rest, ItemDisplayContext.NONE, ItemAnchor.Bottom);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(Scp1762Display::register);
    }
}
