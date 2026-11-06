package net.rai.scp_unleashed.scp4666.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.lang.LangIndex;
import net.rai.scp_unleashed.scp4666.network.Scp4666DiaryPayload;
import net.rai.scp_unleashed.scp4666.network.Scp4666FxPayload;
import net.rai.scp_unleashed.scp4666.network.Scp4666PosePayload;
import net.rai.scp_unleashed.scp4666.network.Scp4666SyncPayload;

import java.util.ArrayList;
import java.util.List;

public final class Scp4666ClientHooks {
    public static final int MaxPages = 12;
    public static final float LookUp = -55.0f;

    private Scp4666ClientHooks() {
    }

    public static void sync(Scp4666SyncPayload Payload) {
        Scp4666ClientState.sync(Payload);
    }

    public static void effect(Scp4666FxPayload Payload) {
        Scp4666ClientState.effect(Payload);
    }

    public static void pose(Scp4666PosePayload Payload) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.level == null || !(Client.level.getEntity(Payload.EntityId()) instanceof Player Body)) {
            return;
        }
        boolean Fresh = Payload.Lying() && Body.getForcedPose() != Pose.SLEEPING;
        Body.setForcedPose(Payload.Lying() ? Pose.SLEEPING : null);
        Body.setPose(Payload.Lying() ? Pose.SLEEPING : Pose.STANDING);
        Body.refreshDimensions();
        if (Fresh && Body == Client.player) {
            Body.setXRot(LookUp);
        }
    }

    public static void diary(Scp4666DiaryPayload Payload) {
        String Prefix = "book.scp_unleashed.scp4666_diary." + Payload.Volume();
        List<Component> Pages = new ArrayList<>();
        for (int Index : LangIndex.current().indicesOr(Prefix, 1)) {
            if (Pages.size() >= MaxPages) {
                break;
            }
            Pages.add(Component.translatable(Prefix + "." + Index));
        }
        Minecraft.getInstance().setScreen(new BookViewScreen(new BookViewScreen.BookAccess(Pages)));
    }
}
