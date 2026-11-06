package net.rai.scp_unleashed.guns;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.client.GunsClientHooks;
import net.rai.scp_unleashed.guns.network.GunAckPayload;
import net.rai.scp_unleashed.guns.network.GunFirePayload;
import net.rai.scp_unleashed.guns.network.GunReloadPayload;
import net.rai.scp_unleashed.guns.network.GunShotPayload;
import net.rai.scp_unleashed.guns.network.GunSoundPayload;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class GunsEvents {
    private GunsEvents() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToServer(GunFirePayload.PayloadType, GunFirePayload.PayloadCodec, GunFirePayload::handle);
        Registrar.playToServer(GunReloadPayload.PayloadType, GunReloadPayload.PayloadCodec, GunReloadPayload::handle);
        Registrar.playToClient(GunShotPayload.PayloadType, GunShotPayload.PayloadCodec, (Payload, Context) -> GunsClientHooks.onShot(Payload));
        Registrar.playToClient(GunAckPayload.PayloadType, GunAckPayload.PayloadCodec, (Payload, Context) -> GunsClientHooks.onAck(Payload));
        Registrar.playToClient(GunSoundPayload.PayloadType, GunSoundPayload.PayloadCodec, (Payload, Context) -> GunsClientHooks.onSound(Payload));
    }

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() != ModCreativeTabs.Weapons.getKey()) {
            return;
        }
        for (DeferredItem<? extends Item> Entry : GunsRegistry.creativeOrder()) {
            Item Listed = Entry.get();
            if (Listed instanceof GunItem Gun) {
                Event.accept(GunItem.loaded(Gun.getKind()));
            } else {
                Event.accept(new ItemStack(Listed));
            }
        }
    }

    @SubscribeEvent
    public static void adjustKnockback(LivingKnockBackEvent Event) {
        GunDamage.adjustKnockback(Event);
    }

    @SubscribeEvent
    public static void forgetShooter(PlayerEvent.PlayerLoggedOutEvent Event) {
        GunFiring.forget(Event.getEntity().getUUID());
        GunSoundMixer.forget(Event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void forgetShooters(ServerStoppedEvent Event) {
        GunFiring.forgetAll();
        GunSounds.clear();
    }

    @SubscribeEvent
    public static void openSounds(LevelTickEvent.Pre Event) {
        GunSoundMixer.begin(Event.getLevel());
    }

    @SubscribeEvent
    public static void tickSounds(LevelTickEvent.Post Event) {
        GunSounds.tick(Event.getLevel());
    }
}
