package net.rai.scp_unleashed.scp860.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp860.Scp860KeyItem;
import net.rai.scp_unleashed.scp860.Scp860Registry;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.client.ItemAnchor;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayoutContext;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp860Display {
    public static final ModelResourceLocation Stand = ModelResourceLocation.standalone(Scp860Registry.location("block/scp860/display_stand"));
    public static final ModelResourceLocation Panel = ModelResourceLocation.standalone(Scp860Registry.location("block/scp860/display_panel"));
    public static final float StandZ = 9.5f;
    public static final float StandYaw = -10.0f;
    public static final float BoardPitch = 45.0f;
    public static final float KeyScale = 0.6f;
    public static final float KeyRoll = 24.0f;
    public static final float KeyUp = 5.67f;
    public static final float KeyBack = -0.38f;
    public static final float PegX = 8.0f;
    public static final float PegY = 22.0f;
    public static final float PegZ = 12.4f;
    public static final float HangScale = 0.66f;
    public static final float HangYaw = 8.0f;
    public static final float HangTilt = -84.0f;
    public static final float RingOffset = 6.15f;
    private static final Placement PieceOrigin = Placement.at(8.0f, 0.0f, 8.0f);
    private static final Placement StandSpot = Placement.at(StorageGeometry.CentreX, StorageGeometry.CavityFloor, StandZ).withYaw(StandYaw);
    private static final Placement Resting = Placement.at(0.0f, KeyUp, KeyBack).withRotation(0.0f, BoardPitch, KeyRoll).withScale(KeyScale);
    private static final Placement HangFrame = Placement.at(PegX, PegY, PegZ).withRotation(HangYaw, 0.0f, HangTilt).withScale(HangScale);
    private static final Placement Ring = Placement.at(RingOffset, 0.0f, 0.0f);
    public static final StorageLayout Layout = StorageLayout.sized(Scp860Display::onStand, Scp860Display::onPeg);

    private Scp860Display() {
    }

    public static List<ModelResourceLocation> models() {
        return List.of(Stand, Panel);
    }

    public static void register() {
        StorageLayouts.register(Scp860KeyItem::is, Layout);
    }

    public static void onStand(StorageLayoutContext Context) {
        PoseStack Pose = Context.Pose();
        Pose.pushPose();
        StandSpot.apply(Pose);
        Context.model(Stand, Placement.Origin);
        if (!Context.isEmpty()) {
            Context.item(Context.Stack(), Resting, ItemDisplayContext.NONE, ItemAnchor.Centre);
        }
        Pose.popPose();
    }

    public static void onPeg(StorageLayoutContext Context) {
        Context.model(Panel, PieceOrigin);
        if (Context.isEmpty()) {
            return;
        }
        PoseStack Pose = Context.Pose();
        Pose.pushPose();
        HangFrame.apply(Pose);
        Context.item(Context.Stack(), Ring, ItemDisplayContext.NONE, ItemAnchor.Centre);
        Pose.popPose();
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(Scp860Display::register);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        for (ModelResourceLocation Model : models()) {
            Event.register(Model);
        }
    }
}
