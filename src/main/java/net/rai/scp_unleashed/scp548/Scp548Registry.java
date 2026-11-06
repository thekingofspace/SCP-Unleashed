package net.rai.scp_unleashed.scp548;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
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
import net.rai.scp_unleashed.stranger.StrangerEncounters;
import net.rai.scp_unleashed.transferpod.TransferPods;

import java.util.List;

public final class Scp548Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<MobEffect> MobEffectRegister = DeferredRegister.create(Registries.MOB_EFFECT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_548");
    public static final ResourceLocation PodContent = location("scp548");
    public static final String PodLabel = "item.scp_unleashed.transfer_pod.content.scp548";
    public static final int PodColour = 0x9ED8F2;

    public static final DeferredHolder<EntityType<?>, EntityType<Scp548Spider>> Spider = EntityTypeRegister.register("scp548_ice_spider",
            () -> EntityType.Builder.<Scp548Spider>of(Scp548Spider::new, MobCategory.CREATURE)
                    .sized(Scp548Spider.Width, Scp548Spider.Height)
                    .eyeHeight(Scp548Spider.EyeHeight)
                    .clientTrackingRange(10)
                    .build("scp548_ice_spider"));

    public static final DeferredItem<DeferredSpawnEggItem> Egg = ItemRegister.register("scp548_spawn_egg",
            () -> new DeferredSpawnEggItem(Spider, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredHolder<MobEffect, Scp548FrostbiteEffect> Frostbite = MobEffectRegister.register("scp548_frostbite", Scp548FrostbiteEffect::new);

    public static final DeferredHolder<SoundEvent, SoundEvent> Skitter = sound("scp548_skitter");
    public static final DeferredHolder<SoundEvent, SoundEvent> SkitterFrost = sound("scp548_skitter_frost");
    public static final DeferredHolder<SoundEvent, SoundEvent> Chitter = sound("scp548_chitter");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChitterCrackle = sound("scp548_chitter_crackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Crackle = sound("scp548_crackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrackleChime = sound("scp548_crackle_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> Bite = sound("scp548_bite");
    public static final DeferredHolder<SoundEvent, SoundEvent> BiteCrunch = sound("scp548_bite_crunch");
    public static final DeferredHolder<SoundEvent, SoundEvent> Frost = sound("scp548_frost");
    public static final DeferredHolder<SoundEvent, SoundEvent> FrostHiss = sound("scp548_frost_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> Eat = sound("scp548_eat");
    public static final DeferredHolder<SoundEvent, SoundEvent> EatCrunch = sound("scp548_eat_crunch");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp548_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HurtChip = sound("scp548_hurt_chip");
    public static final DeferredHolder<SoundEvent, SoundEvent> Death = sound("scp548_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DeathShatter = sound("scp548_death_shatter");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hatch = sound("scp548_hatch");
    public static final DeferredHolder<SoundEvent, SoundEvent> Shatter = sound("scp548_shatter");
    public static final DeferredHolder<SoundEvent, SoundEvent> ShatterFrost = sound("scp548_shatter_frost");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SkitterLayers = List.of(Skitter, SkitterFrost);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ChitterLayers = List.of(Chitter, ChitterCrackle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CrackleLayers = List.of(Crackle, CrackleChime);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BiteLayers = List.of(Bite, BiteCrunch);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> FrostLayers = List.of(Frost, FrostHiss);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> EatLayers = List.of(Eat, EatCrunch);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HurtLayers = List.of(Hurt, HurtChip);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DeathLayers = List.of(Death, DeathShatter);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ShatterLayers = List.of(Shatter, ShatterFrost);

    private Scp548Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        MobEffectRegister.register(ModEventBus);
        TransferPods.register(PodContent, PodLabel, PodColour, Scp548Swarm::releaseFromPod, false);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
