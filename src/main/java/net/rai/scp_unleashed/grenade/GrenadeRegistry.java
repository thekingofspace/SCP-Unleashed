package net.rai.scp_unleashed.grenade;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class GrenadeRegistry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    private static final Map<GrenadeKind, DeferredItem<GrenadeItem>> Grenades = new EnumMap<>(GrenadeKind.class);
    private static final List<DeferredItem<GrenadeItem>> CreativeOrder = new ArrayList<>();

    public static final DeferredItem<GrenadeItem> FragGrenade = grenade(GrenadeKind.Frag, Rarity.COMMON);
    public static final DeferredItem<GrenadeItem> IncendiaryGrenade = grenade(GrenadeKind.Incendiary, Rarity.COMMON);
    public static final DeferredItem<GrenadeItem> FlashGrenade = grenade(GrenadeKind.Flash, Rarity.COMMON);
    public static final DeferredItem<GrenadeItem> GasGrenade = grenade(GrenadeKind.Gas, Rarity.UNCOMMON);

    public static final DeferredHolder<EntityType<?>, EntityType<GrenadeEntity>> Grenade = EntityTypeRegister.register("grenade",
            () -> EntityType.Builder.<GrenadeEntity>of(GrenadeEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .eyeHeight(0.125F)
                    .fireImmune()
                    .clientTrackingRange(6)
                    .updateInterval(4)
                    .build("grenade"));

    public static final DeferredHolder<SoundEvent, SoundEvent> PinPull = sound("grenade_pin_pull");
    public static final DeferredHolder<SoundEvent, SoundEvent> PinClick = sound("grenade_pin_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> Throw = sound("grenade_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> Spoon = sound("grenade_spoon");
    public static final DeferredHolder<SoundEvent, SoundEvent> Bounce = sound("grenade_bounce");
    public static final DeferredHolder<SoundEvent, SoundEvent> BounceRing = sound("grenade_bounce_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> Fuse = sound("grenade_fuse");
    public static final DeferredHolder<SoundEvent, SoundEvent> Fizzle = sound("grenade_fizzle");
    public static final DeferredHolder<SoundEvent, SoundEvent> FragBoom = fixedSound("grenade_frag_boom", 96.0F);
    public static final DeferredHolder<SoundEvent, SoundEvent> FragCrack = fixedSound("grenade_frag_crack", 96.0F);
    public static final DeferredHolder<SoundEvent, SoundEvent> FragDebris = sound("grenade_frag_debris");
    public static final DeferredHolder<SoundEvent, SoundEvent> IncendiaryWhoosh = fixedSound("grenade_incendiary_whoosh", 48.0F);
    public static final DeferredHolder<SoundEvent, SoundEvent> IncendiaryIgnite = sound("grenade_incendiary_ignite");
    public static final DeferredHolder<SoundEvent, SoundEvent> IncendiaryBurn = sound("grenade_incendiary_burn");
    public static final DeferredHolder<SoundEvent, SoundEvent> FlashBang = fixedSound("grenade_flash_bang", 80.0F);
    public static final DeferredHolder<SoundEvent, SoundEvent> FlashCrack = fixedSound("grenade_flash_crack", 80.0F);
    public static final DeferredHolder<SoundEvent, SoundEvent> FlashPing = sound("grenade_flash_ping");
    public static final DeferredHolder<SoundEvent, SoundEvent> FlashChime = sound("grenade_flash_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> FlashTinnitus = sound("grenade_flash_tinnitus");
    public static final DeferredHolder<SoundEvent, SoundEvent> FlashSparkle = sound("grenade_flash_sparkle");
    public static final DeferredHolder<SoundEvent, SoundEvent> GasBurst = sound("grenade_gas_burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> GasHiss = sound("grenade_gas_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> GasSpit = sound("grenade_gas_spit");

    public static final ResourceKey<DamageType> ShrapnelDamage = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "grenade_shrapnel"));

    private GrenadeRegistry() {
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
    }

    public static List<DeferredItem<GrenadeItem>> creativeOrder() {
        return Collections.unmodifiableList(CreativeOrder);
    }

    public static Item grenadeItem(GrenadeKind Kind) {
        DeferredItem<GrenadeItem> Entry = Grenades.get(Kind);
        return Entry == null ? Items.AIR : Entry.get();
    }

    private static DeferredItem<GrenadeItem> grenade(GrenadeKind Kind, Rarity Grade) {
        DeferredItem<GrenadeItem> Entry = ItemRegister.register(Kind.getName(),
                () -> new GrenadeItem(Kind, new Item.Properties().stacksTo(GrenadeTuning.StackSize).rarity(Grade)));
        Grenades.put(Kind, Entry);
        CreativeOrder.add(Entry);
        return Entry;
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> fixedSound(String Name, float Range) {
        return SoundRegister.register(Name, () -> SoundEvent.createFixedRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name), Range));
    }
}
