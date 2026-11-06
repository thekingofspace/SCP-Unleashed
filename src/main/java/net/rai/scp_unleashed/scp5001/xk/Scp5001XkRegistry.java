package net.rai.scp_unleashed.scp5001.xk;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp5001.xk.client.XkClientHooks;
import net.rai.scp_unleashed.scp5001.xk.network.XkSkyPayload;
import net.rai.scp_unleashed.scp5001.xk.network.YaldabaothPayload;

import java.util.Comparator;

public final class Scp5001XkRegistry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final ResourceLocation Source = location("scp5001");
    public static final TagKey<EntityType<?>> Immune = TagKey.create(Registries.ENTITY_TYPE, location("scp5001_xk_immune"));
    public static final TicketType<ChunkPos> Ticket = TicketType.create("scp_unleashed_yaldabaoth", Comparator.comparingLong(ChunkPos::toLong), 80);

    public static final DeferredHolder<EntityType<?>, EntityType<Yaldabaoth>> YaldabaothType = EntityTypeRegister.register("yaldabaoth",
            () -> EntityType.Builder.<Yaldabaoth>of(Yaldabaoth::new, MobCategory.MISC).sized(7.0f, 7.0f).eyeHeight(3.5f).fireImmune().noSummon()
                    .clientTrackingRange(32).updateInterval(1).build("yaldabaoth"));

    public static final DeferredHolder<SoundEvent, SoundEvent> Horn = sound("scp5001.xk.horn");
    public static final DeferredHolder<SoundEvent, SoundEvent> Tremor = sound("scp5001.xk.tremor");
    public static final DeferredHolder<SoundEvent, SoundEvent> Broadcast = sound("scp5001.xk.broadcast");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rise = sound("scp5001.xk.rise");
    public static final DeferredHolder<SoundEvent, SoundEvent> Ambient = fight("scp5001.xk.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rumble = fight("scp5001.xk.rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> Roar = fight("scp5001.xk.roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> RoarLow = fight("scp5001.xk.roar_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Bite = fight("scp5001.xk.bite");
    public static final DeferredHolder<SoundEvent, SoundEvent> Flinch = fight("scp5001.xk.flinch");
    public static final DeferredHolder<SoundEvent, SoundEvent> Vanish = fight("scp5001.xk.vanish");
    public static final DeferredHolder<SoundEvent, SoundEvent> RoarGreat = fight("scp5001.xk.roar_great");
    public static final DeferredHolder<SoundEvent, SoundEvent> Lunge = fight("scp5001.xk.lunge");
    public static final DeferredHolder<SoundEvent, SoundEvent> Coil = fight("scp5001.xk.coil");

    private Scp5001XkRegistry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> fight(String Name) {
        return SoundEventRegister.register(Name, () -> net.rai.scp_unleashed.scp5001.core.FightAudio.event(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ModEventBus.addListener(EntityAttributeCreationEvent.class, Event -> Event.put(YaldabaothType.get(), Yaldabaoth.createAttributes().build()));
        ModEventBus.addListener(RegisterPayloadHandlersEvent.class, Scp5001XkRegistry::payloads);
    }

    private static void payloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(XkSkyPayload.PayloadType, XkSkyPayload.PayloadCodec, (Payload, Context) -> XkClientHooks.sky(Payload));
        Registrar.playToClient(YaldabaothPayload.PayloadType, YaldabaothPayload.PayloadCodec, (Payload, Context) -> XkClientHooks.yaldabaoth(Payload));
    }
}
