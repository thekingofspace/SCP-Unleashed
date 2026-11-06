package net.rai.scp_unleashed.mtf;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;

public final class MtfRegistry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> NextCall = AttachmentRegister.register("mtf_next_call",
            () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG, Saved -> Saved > 0L).copyOnDeath().build());

    public static final DeferredItem<MtfSignalRadioItem> SignalRadio = ItemRegister.register("mtf_signal_radio",
            () -> new MtfSignalRadioItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<EntityType<?>, EntityType<MtfOperative>> OperativeType = EntityTypeRegister.register("mtf_operative",
            () -> EntityType.Builder.<MtfOperative>of(MtfOperative::new, MobCategory.MISC).sized(0.6f, 1.8f).eyeHeight(1.62f)
                    .vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT).clientTrackingRange(10).noSave().build("mtf_operative"));
    public static final DeferredHolder<EntityType<?>, EntityType<MtfHelicopter>> HelicopterType = EntityTypeRegister.register("mtf_helicopter",
            () -> EntityType.Builder.<MtfHelicopter>of(MtfHelicopter::new, MobCategory.MISC).sized(3.0f, 3.2f).fireImmune().noSave()
                    .clientTrackingRange(16).updateInterval(2).build("mtf_helicopter"));

    public static final DeferredHolder<SoundEvent, SoundEvent> Rotor = sound("mtf_helicopter_rotor");
    public static final DeferredHolder<SoundEvent, SoundEvent> Turbine = sound("mtf_helicopter_turbine");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rumble = sound("mtf_helicopter_rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> Thump = sound("mtf_helicopter_thump");
    public static final DeferredHolder<SoundEvent, SoundEvent> RopeDrop = sound("mtf_rope_drop");
    public static final DeferredHolder<SoundEvent, SoundEvent> RopeWhip = sound("mtf_rope_whip");
    public static final DeferredHolder<SoundEvent, SoundEvent> RopeSlide = sound("mtf_rope_slide");
    public static final DeferredHolder<SoundEvent, SoundEvent> RopeLand = sound("mtf_rope_land");
    public static final DeferredHolder<SoundEvent, SoundEvent> RopeLandGear = sound("mtf_rope_land_gear");
    public static final DeferredHolder<SoundEvent, SoundEvent> RadioClick = sound("mtf_radio_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> RadioBeep = sound("mtf_radio_beep");
    public static final DeferredHolder<SoundEvent, SoundEvent> RadioDeny = sound("mtf_radio_deny");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RopeDropLayers = List.of(RopeDrop, RopeWhip);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RopeLandLayers = List.of(RopeLand, RopeLandGear);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CallLayers = List.of(RadioClick, RadioBeep);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DenyLayers = List.of(RadioClick, RadioDeny);

    private MtfRegistry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
    }
}
