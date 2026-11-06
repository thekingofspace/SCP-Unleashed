package net.rai.scp_unleashed.scp902.client;

import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp902.Scp902BoxItem;
import net.rai.scp_unleashed.scp902.Scp902Registry;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.client.ItemAnchor;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp902Display {
    public static final float SmallScale = 0.74f;
    public static final float TallScale = 0.8f;
    public static final float SmallYaw = -14.0f;
    public static final float TallYaw = -10.0f;
    public static final float RestX = 7.8f;
    public static final float RestZ = 9.0f;
    private static final Placement SmallRest = Placement.at(RestX, StorageGeometry.CavityFloor, RestZ).withYaw(SmallYaw).withScale(SmallScale);
    private static final Placement TallRest = Placement.at(RestX, StorageGeometry.CavityFloor, RestZ).withYaw(TallYaw).withScale(TallScale);
    public static final StorageLayout Layout = StorageLayout.sized(Context -> onFloor(Context, SmallRest), Context -> onFloor(Context, TallRest));

    private Scp902Display() {
    }

    public static void register() {
        StorageLayouts.register(Scp902BoxItem::is, Layout);
    }

    public static ItemStack shut(ItemStack Stored) {
        if (!Stored.has(Scp902Registry.LatchComponent.get())) {
            return Stored;
        }
        ItemStack Shut = Stored.copy();
        Shut.remove(Scp902Registry.LatchComponent.get());
        return Shut;
    }

    private static void onFloor(StorageLayoutContext Context, Placement Rest) {
        if (Context.isEmpty()) {
            return;
        }
        Context.item(shut(Context.Stack()), Rest, ItemDisplayContext.NONE, ItemAnchor.Bottom);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(Scp902Display::register);
    }
}
