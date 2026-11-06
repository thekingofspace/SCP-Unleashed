package net.rai.scp_unleashed.dclass.duty.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.dclass.HumanoidEyes;
import net.rai.scp_unleashed.dclass.duty.DClassDutyRegistry;
import net.rai.scp_unleashed.dclass.duty.network.DClassBlinkPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class DClassDutyClient {
    private DClassDutyClient() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent Event) {
        Event.register(DClassDutyRegistry.ManagerMenuType.get(), DClassManagerScreen::new);
    }

    public static void blink(DClassBlinkPayload Payload) {
        Minecraft Game = Minecraft.getInstance();
        if (Game.level == null) {
            return;
        }
        Entity Found = Game.level.getEntity(Payload.EntityId());
        if (Found instanceof DClassLike Person) {
            HumanoidEyes Eyes = Person.eyes();
            if (Eyes != null) {
                Eyes.blinkNow();
            }
        }
    }
}
