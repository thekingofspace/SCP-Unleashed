package net.rai.scp_unleashed.scp105.photo096.client;

import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp105.client.Scp105Display;
import net.rai.scp_unleashed.scp105.photo096.Scp105Photo096Item;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.client.ItemAnchor;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp105Photo096ClientEvents {
    private Scp105Photo096ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(Scp105Photo096ClientEvents::register);
    }

    public static void register() {
        StorageLayouts.register(Scp105Photo096Item::is, StorageLayout.sized(
                Context -> onWall(Context, Scp105Display.SmallScale, Scp105Display.SmallCentreY),
                Context -> onWall(Context, Scp105Display.TallScale, Scp105Display.TallCentreY)));
    }

    private static void onWall(StorageLayoutContext Context, float Scale, float CentreY) {
        if (Context.isEmpty()) {
            return;
        }
        Placement Pinned = Placement.at(StorageGeometry.CentreX, CentreY, Scp105Display.WallZ).withScale(Scale);
        Context.item(Context.Stack(), Pinned, ItemDisplayContext.FIXED, ItemAnchor.Centre);
    }
}
