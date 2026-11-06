package net.rai.scp_unleashed.scp031;

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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.StartConditions;

public final class Scp031Registry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_031");
    public static final ResourceKey<DamageType> EmbraceDamage = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp031_embrace"));
    public static final TagKey<EntityType<?>> PersonLike = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "person_like"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp031Mimic>> Mimic = EntityTypeRegister.register("scp031",
            () -> EntityType.Builder.<Scp031Mimic>of(Scp031Mimic::new, MobCategory.MONSTER)
                    .sized(Scp031Mimic.Width, Scp031Mimic.Height)
                    .eyeHeight(Scp031Mimic.EyeHeight)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("scp031"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp031Polyp>> Polyp = EntityTypeRegister.register("scp031_polyp",
            () -> EntityType.Builder.<Scp031Polyp>of(Scp031Polyp::new, MobCategory.MISC)
                    .sized(Scp031Polyp.Widths[0], Scp031Polyp.Heights[0])
                    .clientTrackingRange(8)
                    .build("scp031_polyp"));

    public static final DeferredItem<DeferredSpawnEggItem> MimicEgg = ItemRegister.register("scp031_spawn_egg",
            () -> new DeferredSpawnEggItem(Mimic, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<DeferredSpawnEggItem> PolypEgg = ItemRegister.register("scp031_polyp_spawn_egg",
            () -> new DeferredSpawnEggItem(Polyp, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<SoundEvent, SoundEvent> Embrace = sound("scp031_embrace");
    public static final DeferredHolder<SoundEvent, SoundEvent> EmbraceSoft = sound("scp031_embrace_soft");
    public static final DeferredHolder<SoundEvent, SoundEvent> Squeeze = sound("scp031_squeeze");
    public static final DeferredHolder<SoundEvent, SoundEvent> Release = sound("scp031_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp031_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> Death = sound("scp031_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> Collapse = sound("scp031_collapse");
    public static final DeferredHolder<SoundEvent, SoundEvent> PolypSquelch = sound("scp031_polyp_squelch");
    public static final DeferredHolder<SoundEvent, SoundEvent> PolypGrow = sound("scp031_polyp_grow");
    public static final DeferredHolder<SoundEvent, SoundEvent> PolypWither = sound("scp031_polyp_wither");
    public static final DeferredHolder<SoundEvent, SoundEvent> PolypBurst = sound("scp031_polyp_burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> PolypRise = sound("scp031_polyp_rise");
    public static final DeferredHolder<SoundEvent, SoundEvent> PolypRiseLow = sound("scp031_polyp_rise_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> PolypSprout = sound("scp031_polyp_sprout");

    private Scp031Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        StartConditions.register(Scp031VillageCondition.TypeName, Scp031VillageCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp031VillageCondition::manifest);
    }
}
