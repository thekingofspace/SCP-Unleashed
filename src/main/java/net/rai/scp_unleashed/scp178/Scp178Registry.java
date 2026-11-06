package net.rai.scp_unleashed.scp178;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.EnumMap;
import java.util.List;

public final class Scp178Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ArmorMaterial> MaterialRegister = DeferredRegister.create(Registries.ARMOR_MATERIAL, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_178");
    public static final ResourceKey<DamageType> Taken = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp178_taken"));

    public static final DeferredHolder<SoundEvent, SoundEvent> GlassesOn = sound("scp178_glasses_on");
    public static final DeferredHolder<SoundEvent, SoundEvent> Click = sound("scp178_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> ClickTick = sound("scp178_click_tick");
    public static final DeferredHolder<SoundEvent, SoundEvent> Breath = sound("scp178_breath");
    public static final DeferredHolder<SoundEvent, SoundEvent> BreathLow = sound("scp178_breath_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Whisper = sound("scp178_whisper");
    public static final DeferredHolder<SoundEvent, SoundEvent> WhisperLow = sound("scp178_whisper_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Step = sound("scp178_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> StepTap = sound("scp178_step_tap");
    public static final DeferredHolder<SoundEvent, SoundEvent> Windup = sound("scp178_windup");
    public static final DeferredHolder<SoundEvent, SoundEvent> WindupClick = sound("scp178_windup_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> Lunge = sound("scp178_lunge");
    public static final DeferredHolder<SoundEvent, SoundEvent> LungeShriek = sound("scp178_lunge_shriek");
    public static final DeferredHolder<SoundEvent, SoundEvent> Strike = sound("scp178_strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> StrikeImpact = sound("scp178_strike_impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp178_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HurtHiss = sound("scp178_hurt_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> Fade = sound("scp178_fade");
    public static final DeferredHolder<SoundEvent, SoundEvent> FadeAir = sound("scp178_fade_air");
    public static final DeferredHolder<SoundEvent, SoundEvent> Turn = sound("scp178_turn");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ClickLayers = List.of(Click, ClickTick);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BreathLayers = List.of(Breath, BreathLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WhisperLayers = List.of(Whisper, WhisperLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StepLayers = List.of(Step, StepTap);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WindupLayers = List.of(Windup, WindupClick);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> LungeLayers = List.of(Lunge, LungeShriek);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StrikeLayers = List.of(Strike, StrikeImpact);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HurtLayers = List.of(Hurt, HurtHiss);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> FadeLayers = List.of(Fade, FadeAir);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TurnLayers = List.of(Turn, WhisperLow);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> Cardboard = MaterialRegister.register("scp178_cardboard", () -> {
        EnumMap<ArmorItem.Type, Integer> Defense = new EnumMap<>(ArmorItem.Type.class);
        for (ArmorItem.Type Kind : ArmorItem.Type.values()) {
            Defense.put(Kind, 0);
        }
        return new ArmorMaterial(Defense, 0, GlassesOn, () -> Ingredient.of(Items.PAPER),
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp178_glasses"))), 0.0f, 0.0f);
    });

    public static final DeferredHolder<EntityType<?>, EntityType<Scp178Watcher>> Watcher = EntityTypeRegister.register("scp178_watcher",
            () -> EntityType.Builder.<Scp178Watcher>of(Scp178Watcher::new, MobCategory.MISC)
                    .sized(Scp178Watcher.Width, Scp178Watcher.Height)
                    .eyeHeight(Scp178Watcher.EyeHeight)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .clientTrackingRange(8)
                    .build("scp178_watcher"));

    public static final DeferredItem<Scp178GlassesItem> Glasses = ItemRegister.register("scp178_glasses",
            () -> new Scp178GlassesItem(Cardboard, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    private Scp178Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        MaterialRegister.register(ModEventBus);
    }
}
