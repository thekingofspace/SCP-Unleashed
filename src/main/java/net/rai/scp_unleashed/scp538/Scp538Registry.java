package net.rai.scp_unleashed.scp538;

import com.mojang.serialization.Codec;
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
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.stranger.StrangerEncounters;
import net.rai.scp_unleashed.transferpod.TransferPods;

import java.util.List;

public final class Scp538Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_538");
    public static final ResourceLocation PodContent = location("scp538");
    public static final String PodLabel = "item.scp_unleashed.transfer_pod.content.scp538";
    public static final int PodColour = 0x8C7AB8;
    public static final ResourceKey<DamageType> ShadowDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp538_shadow"));
    public static final TagKey<EntityType<?>> Immune = TagKey.create(Registries.ENTITY_TYPE, location("scp538_immune"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp538Spider>> Spider = EntityTypeRegister.register("scp538_shadow_spider",
            () -> EntityType.Builder.<Scp538Spider>of(Scp538Spider::new, MobCategory.CREATURE)
                    .sized(Scp538Spider.Width, Scp538Spider.Height)
                    .eyeHeight(Scp538Spider.EyeHeight)
                    .fireImmune()
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build("scp538_shadow_spider"));

    public static final DeferredItem<DeferredSpawnEggItem> Egg = ItemRegister.register("scp538_spawn_egg",
            () -> new DeferredSpawnEggItem(Spider, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> Infection = AttachmentRegister.register("scp538_infection",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT, Saved -> Saved >= 0).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Scp538Trace>> Trace = AttachmentRegister.register("scp538_trace",
            () -> AttachmentType.builder(() -> new Scp538Trace()).build());

    public static final DeferredHolder<SoundEvent, SoundEvent> Skitter = sound("scp538_skitter");
    public static final DeferredHolder<SoundEvent, SoundEvent> SkitterSoft = sound("scp538_skitter_soft");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hiss = sound("scp538_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> HissBreath = sound("scp538_hiss_breath");
    public static final DeferredHolder<SoundEvent, SoundEvent> Bite = sound("scp538_bite");
    public static final DeferredHolder<SoundEvent, SoundEvent> BiteSnap = sound("scp538_bite_snap");
    public static final DeferredHolder<SoundEvent, SoundEvent> Fade = sound("scp538_fade");
    public static final DeferredHolder<SoundEvent, SoundEvent> FadeFizz = sound("scp538_fade_fizz");
    public static final DeferredHolder<SoundEvent, SoundEvent> Burst = sound("scp538_burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> BurstThud = sound("scp538_burst_thud");
    public static final DeferredHolder<SoundEvent, SoundEvent> BurstScatter = sound("scp538_burst_scatter");
    public static final DeferredHolder<SoundEvent, SoundEvent> Crawl = sound("scp538_crawl");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrawlSoft = sound("scp538_crawl_soft");
    public static final DeferredHolder<SoundEvent, SoundEvent> Onset = sound("scp538_onset");
    public static final DeferredHolder<SoundEvent, SoundEvent> OnsetChill = sound("scp538_onset_chill");
    public static final DeferredHolder<SoundEvent, SoundEvent> Pass = sound("scp538_pass");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SkitterLayers = List.of(Skitter, SkitterSoft);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HissLayers = List.of(Hiss, HissBreath);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BiteLayers = List.of(Bite, BiteSnap);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> FadeLayers = List.of(Fade, FadeFizz);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BurstLayers = List.of(Burst, BurstThud, BurstScatter);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CrawlLayers = List.of(Crawl, CrawlSoft);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> OnsetLayers = List.of(Onset, OnsetChill);

    private Scp538Registry() {
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
        AttachmentRegister.register(ModEventBus);
        TransferPods.register(PodContent, PodLabel, PodColour, Scp538Swarm::releaseFromPod, false);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
