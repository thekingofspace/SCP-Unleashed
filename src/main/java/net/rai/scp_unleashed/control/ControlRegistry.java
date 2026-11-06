package net.rai.scp_unleashed.control;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.stasis.StasisPods;

import java.util.List;

public final class ControlRegistry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ControlSession>> Session = AttachmentRegister.register("control_session",
            () -> AttachmentType.builder(() -> ControlSession.None).serialize(ControlSession.SessionCodec, Saved -> Saved.active()).copyOnDeath().build());

    public static final DeferredHolder<EntityType<?>, EntityType<VacantBody>> VacantBodyType = EntityTypeRegister.register("vacant_body",
            () -> EntityType.Builder.<VacantBody>of(VacantBody::new, MobCategory.MISC).sized(0.6f, 1.25f).eyeHeight(1.05f).clientTrackingRange(10).updateInterval(20)
                    .fireImmune().build("vacant_body"));

    public static final DeferredItem<ControllerItem> Controller = ItemRegister.register("controller",
            () -> new ControllerItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<SoundEvent, SoundEvent> Link = sound("control_link");
    public static final DeferredHolder<SoundEvent, SoundEvent> LinkHum = sound("control_link_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> LinkChime = sound("control_link_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> Unlink = sound("control_unlink");
    public static final DeferredHolder<SoundEvent, SoundEvent> UnlinkDrop = sound("control_unlink_drop");
    public static final DeferredHolder<SoundEvent, SoundEvent> Severed = sound("control_severed");
    public static final DeferredHolder<SoundEvent, SoundEvent> SeveredCrack = sound("control_severed_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> SeveredDrone = sound("control_severed_drone");
    public static final DeferredHolder<SoundEvent, SoundEvent> Refuse = sound("control_refuse");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> LinkLayers = List.of(Link, LinkHum, LinkChime);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> UnlinkLayers = List.of(Unlink, UnlinkDrop);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SeveredLayers = List.of(Severed, SeveredCrack, SeveredDrone);

    private ControlRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        StasisPods.addCaptureVeto(ControlSessions::isBoundToSession);
    }
}
