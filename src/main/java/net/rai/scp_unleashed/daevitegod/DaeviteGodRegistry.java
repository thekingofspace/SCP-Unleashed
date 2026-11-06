package net.rai.scp_unleashed.daevitegod;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
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
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.EnumMap;
import java.util.List;

public final class DaeviteGodRegistry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<ArmorMaterial> MaterialRegister = DeferredRegister.create(Registries.ARMOR_MATERIAL, Scp_unleashed.ModId);

    public static final int CrownDefense = 3;
    public static final float CrownToughness = 2.0f;
    public static final float CrownSteadiness = 0.05f;
    public static final int CrownEnchantability = 25;
    public static final int CrownDurabilityFactor = 37;

    public static final ResourceKey<DamageType> SmiteDamage = damage("daevite_god_smite");
    public static final ResourceKey<DamageType> SpikeDamage = damage("daevite_god_spikes");
    public static final ResourceKey<DamageType> BladeDamage = damage("daevite_god_blade");
    public static final ResourceKey<DamageType> ShockwaveDamage = damage("daevite_god_shockwave");

    public static final DeferredHolder<EntityType<?>, EntityType<DaeviteGod>> God = EntityTypeRegister.register("daevite_god",
            () -> EntityType.Builder.<DaeviteGod>of(DaeviteGod::new, MobCategory.MONSTER)
                    .sized(DaeviteGod.GodWidth, DaeviteGod.GodHeight)
                    .eyeHeight(DaeviteGod.GodEye)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .build("daevite_god"));
    public static final DeferredHolder<EntityType<?>, EntityType<DaeviteGodSpirit>> Spirit = EntityTypeRegister.register("daevite_god_spirit",
            () -> EntityType.Builder.<DaeviteGodSpirit>of(DaeviteGodSpirit::new, MobCategory.MONSTER)
                    .sized(DaeviteGodSpirit.Width, DaeviteGodSpirit.Height)
                    .eyeHeight(DaeviteGodSpirit.EyeHeight)
                    .fireImmune()
                    .noSave()
                    .clientTrackingRange(10)
                    .build("daevite_god_spirit"));
    public static final DeferredHolder<EntityType<?>, EntityType<DaeviteGodSigil>> Sigil = EntityTypeRegister.register("daevite_god_sigil",
            () -> EntityType.Builder.<DaeviteGodSigil>of(DaeviteGodSigil::new, MobCategory.MISC)
                    .sized(0.5f, 0.25f)
                    .fireImmune()
                    .noSave()
                    .noSummon()
                    .clientTrackingRange(12)
                    .updateInterval(20)
                    .build("daevite_god_sigil"));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> CrownMaterial = MaterialRegister.register("daevite_god_crown", () -> {
        EnumMap<ArmorItem.Type, Integer> Defense = new EnumMap<>(ArmorItem.Type.class);
        Defense.put(ArmorItem.Type.HELMET, CrownDefense);
        Defense.put(ArmorItem.Type.CHESTPLATE, 0);
        Defense.put(ArmorItem.Type.LEGGINGS, 0);
        Defense.put(ArmorItem.Type.BOOTS, 0);
        Defense.put(ArmorItem.Type.BODY, 0);
        return new ArmorMaterial(Defense, CrownEnchantability, DaeviteGodRegistry.CrownEquip, () -> Ingredient.of(Items.GOLD_INGOT),
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "daevite_god_crown"))), CrownToughness, CrownSteadiness);
    });

    public static final DeferredItem<DaeviteGodCrownItem> Crown = ItemRegister.register("daevite_god_crown",
            () -> new DaeviteGodCrownItem(CrownMaterial, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()
                    .durability(ArmorItem.Type.HELMET.getDurability(CrownDurabilityFactor))));
    public static final DeferredItem<DeferredSpawnEggItem> SpawnEgg = ItemRegister.register("daevite_god_spawn_egg",
            () -> new DeferredSpawnEggItem(God, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Ember = ParticleRegister.register("daevite_god_ember", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Ash = ParticleRegister.register("daevite_god_ash", () -> new SimpleParticleType(false));

    public static final DeferredHolder<SoundEvent, SoundEvent> Entrance = sound("daevite_god_entrance", 64.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> EntranceLow = sound("daevite_god_entrance_low", 64.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> Rumble = sound("daevite_god_rumble", 64.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> Chant = sound("daevite_god_chant", 64.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> ChantVoices = sound("daevite_god_chant_voices", 64.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> Ambient = sound("daevite_god_ambient", 32.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> KingAmbient = sound("daevite_god_king_ambient", 24.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("daevite_god_hurt", 24.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> HurtLow = sound("daevite_god_hurt_low", 24.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> Death = sound("daevite_god_death", 64.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> DeathLow = sound("daevite_god_death_low", 64.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> Crumble = sound("daevite_god_crumble", 48.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> Cast = sound("daevite_god_cast", 32.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> CastWhisper = sound("daevite_god_cast_whisper", 32.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> SigilLightning = sound("daevite_god_sigil_lightning", 24.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> SigilSpikes = sound("daevite_god_sigil_spikes", 24.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> SigilSpirits = sound("daevite_god_sigil_spirits", 24.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> SigilHum = sound("daevite_god_sigil_hum", 24.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> Strike = sound("daevite_god_strike", 48.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> StrikeThunder = sound("daevite_god_strike_thunder", 48.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> Spikes = sound("daevite_god_spikes", 24.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> SpikesCrack = sound("daevite_god_spikes_crack", 24.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> Transform = sound("daevite_god_transform", 64.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> TransformLow = sound("daevite_god_transform_low", 64.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> Reform = sound("daevite_god_reform", 64.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> ReformBell = sound("daevite_god_reform_bell", 64.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> Swing = sound("daevite_god_swing", 24.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> SwingHeavy = sound("daevite_god_swing_heavy", 24.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> Impact = sound("daevite_god_impact", 24.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> ImpactRing = sound("daevite_god_impact_ring", 24.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> Thrust = sound("daevite_god_thrust", 24.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> Leap = sound("daevite_god_leap", 32.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> Slam = sound("daevite_god_slam", 48.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> SlamBoom = sound("daevite_god_slam_boom", 48.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> Shockwave = sound("daevite_god_shockwave", 32.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> Step = sound("daevite_god_step", 24.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> StepClank = sound("daevite_god_step_clank", 24.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> Roar = sound("daevite_god_roar", 48.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> RoarLow = sound("daevite_god_roar_low", 48.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> Blink = sound("daevite_god_blink", 32.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> BlinkLow = sound("daevite_god_blink_low", 32.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> SpiritRise = sound("daevite_god_spirit_rise");
    public static final DeferredHolder<SoundEvent, SoundEvent> SpiritAmbient = sound("daevite_god_spirit_ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SpiritAttack = sound("daevite_god_spirit_attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> SpiritHurt = sound("daevite_god_spirit_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SpiritDeath = sound("daevite_god_spirit_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrownEquip = sound("daevite_god_crown_equip");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> EntranceLayers = List.of(Entrance, EntranceLow, Rumble);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ChantLayers = List.of(Chant, ChantVoices);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HurtLayers = List.of(Hurt, HurtLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DeathLayers = List.of(Death, DeathLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CastLayers = List.of(Cast, CastWhisper);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StrikeLayers = List.of(Strike, StrikeThunder);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SpikeLayers = List.of(Spikes, SpikesCrack);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TransformLayers = List.of(Transform, TransformLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ReformLayers = List.of(Reform, ReformBell);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SwingLayers = List.of(Swing, SwingHeavy);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ImpactLayers = List.of(Impact, ImpactRing);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SlamLayers = List.of(Slam, SlamBoom);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StepLayers = List.of(Step, StepClank);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RoarLayers = List.of(Roar, RoarLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BlinkLayers = List.of(Blink, BlinkLow);

    private DaeviteGodRegistry() {
    }

    private static ResourceKey<DamageType> damage(String Name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name, float Range) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createFixedRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name), Range));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        MaterialRegister.register(ModEventBus);
    }
}
