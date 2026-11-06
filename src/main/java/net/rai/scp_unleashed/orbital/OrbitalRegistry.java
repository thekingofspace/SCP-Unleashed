package net.rai.scp_unleashed.orbital;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class OrbitalRegistry {
    public static final float ChargeRange = 64.0F;
    public static final float NearRange = 24.0F;
    public static final float StrikeRange = 192.0F;
    public static final float ThunderRange = 320.0F;
    public static final float BeamRange = 128.0F;

    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<OrbitalLink>> Link = ComponentRegister.registerComponentType("orbital_link",
            Builder -> Builder.persistent(OrbitalLink.LinkCodec).networkSynchronized(OrbitalLink.LinkStreamCodec));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<OrbitalRecharge>> Recharge = ComponentRegister.registerComponentType("orbital_recharge",
            Builder -> Builder.persistent(OrbitalRecharge.RechargeCodec).networkSynchronized(OrbitalRecharge.RechargeStreamCodec));

    public static final Map<OrbitalPart, DeferredItem<OrbitalPartItem>> Parts = registerParts();
    public static final DeferredItem<OrbitalDesignatorItem> Designator = ItemRegister.register("orbital_designator",
            () -> new OrbitalDesignatorItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredHolder<EntityType<?>, EntityType<OrbitalStrikeEntity>> StrikeType = EntityTypeRegister.register("orbital_strike",
            () -> EntityType.Builder.<OrbitalStrikeEntity>of(OrbitalStrikeEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(24)
                    .updateInterval(20)
                    .fireImmune()
                    .noSummon()
                    .build("orbital_strike"));

    public static final DeferredHolder<SoundEvent, SoundEvent> ChargeHum = fixedSound("orbital_charge_hum", ChargeRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> ChargeRise = fixedSound("orbital_charge_rise", ChargeRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> ChargeLock = fixedSound("orbital_charge_lock", ChargeRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> Descend = fixedSound("orbital_descend", BeamRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> StrikeBoom = fixedSound("orbital_strike_boom", StrikeRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> StrikeThunder = fixedSound("orbital_strike_thunder", ThunderRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> StrikeBeam = fixedSound("orbital_strike_beam", BeamRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> StrikeBlast = fixedSound("orbital_strike_blast", StrikeRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> StrikeCrack = fixedSound("orbital_strike_crack", StrikeRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> StrikeRumble = fixedSound("orbital_strike_rumble", BeamRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> BeamFade = fixedSound("orbital_beam_fade", BeamRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> Sizzle = fixedSound("orbital_sizzle", NearRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> DesignatorLink = fixedSound("orbital_designator_link", NearRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> DesignatorConfirm = fixedSound("orbital_designator_confirm", NearRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> DesignatorDeny = fixedSound("orbital_designator_deny", NearRange);
    public static final DeferredHolder<SoundEvent, SoundEvent> PartDeployed = fixedSound("orbital_part_deployed", NearRange);

    public static final ResourceKey<DamageType> StrikeDamage = ResourceKey.create(Registries.DAMAGE_TYPE, id("orbital_strike"));
    public static final TagKey<Block> StrikeProof = TagKey.create(Registries.BLOCK, id("orbital_strike_proof"));

    private OrbitalRegistry() {
    }

    public static ResourceLocation id(String Name) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name);
    }

    public static OrbitalPartItem part(OrbitalPart Part) {
        return Parts.get(Part).get();
    }

    private static Map<OrbitalPart, DeferredItem<OrbitalPartItem>> registerParts() {
        Map<OrbitalPart, DeferredItem<OrbitalPartItem>> Result = new EnumMap<>(OrbitalPart.class);
        for (OrbitalPart Part : OrbitalPart.values()) {
            Result.put(Part, ItemRegister.register(Part.getItemName(), () -> new OrbitalPartItem(Part, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant())));
        }
        return Collections.unmodifiableMap(Result);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> fixedSound(String Name, float Range) {
        return SoundRegister.register(Name, () -> SoundEvent.createFixedRangeEvent(id(Name), Range));
    }

    public static void register(IEventBus ModEventBus) {
        ComponentRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
    }
}
