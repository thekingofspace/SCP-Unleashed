package net.rai.scp_unleashed.scp354.beasts;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
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

import java.util.List;

public final class Scp354BeastRegistry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final TagKey<EntityType<?>> Spawns = TagKey.create(Registries.ENTITY_TYPE, location("scp354_spawns"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp354Brute>> Brute = EntityTypeRegister.register("scp354_brute",
            () -> EntityType.Builder.<Scp354Brute>of(Scp354Brute::new, MobCategory.MONSTER).sized(Scp354Brute.Width, Scp354Brute.Height)
                    .eyeHeight(Scp354Brute.EyeHeight).clientTrackingRange(10).build("scp354_brute"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp354Crawler>> Crawler = EntityTypeRegister.register("scp354_crawler",
            () -> EntityType.Builder.<Scp354Crawler>of(Scp354Crawler::new, MobCategory.MONSTER).sized(Scp354Crawler.Width, Scp354Crawler.Height)
                    .eyeHeight(Scp354Crawler.EyeHeight).clientTrackingRange(10).build("scp354_crawler"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp354Wingbeast>> Wingbeast = EntityTypeRegister.register("scp354_wingbeast",
            () -> EntityType.Builder.<Scp354Wingbeast>of(Scp354Wingbeast::new, MobCategory.MONSTER).sized(Scp354Wingbeast.Width, Scp354Wingbeast.Height)
                    .eyeHeight(Scp354Wingbeast.EyeHeight).clientTrackingRange(12).build("scp354_wingbeast"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp354Leaper>> Leaper = EntityTypeRegister.register("scp354_leaper",
            () -> EntityType.Builder.<Scp354Leaper>of(Scp354Leaper::new, MobCategory.MONSTER).sized(Scp354Leaper.Width, Scp354Leaper.Height)
                    .eyeHeight(Scp354Leaper.EyeHeight).clientTrackingRange(10).build("scp354_leaper"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp354Charger>> Charger = EntityTypeRegister.register("scp354_charger",
            () -> EntityType.Builder.<Scp354Charger>of(Scp354Charger::new, MobCategory.MONSTER).sized(Scp354Charger.Width, Scp354Charger.Height)
                    .eyeHeight(Scp354Charger.EyeHeight).clientTrackingRange(10).build("scp354_charger"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp354HurledEarth>> HurledEarth = EntityTypeRegister.register("scp354_hurled_earth",
            () -> EntityType.Builder.<Scp354HurledEarth>of(Scp354HurledEarth::new, MobCategory.MISC).sized(0.8f, 0.8f).clientTrackingRange(8).updateInterval(2)
                    .build("scp354_hurled_earth"));

    public static final DeferredItem<DeferredSpawnEggItem> BruteEgg = egg("scp354_brute_spawn_egg", Brute);
    public static final DeferredItem<DeferredSpawnEggItem> CrawlerEgg = egg("scp354_crawler_spawn_egg", Crawler);
    public static final DeferredItem<DeferredSpawnEggItem> WingbeastEgg = egg("scp354_wingbeast_spawn_egg", Wingbeast);
    public static final DeferredItem<DeferredSpawnEggItem> LeaperEgg = egg("scp354_leaper_spawn_egg", Leaper);
    public static final DeferredItem<DeferredSpawnEggItem> ChargerEgg = egg("scp354_charger_spawn_egg", Charger);
    public static final List<DeferredItem<DeferredSpawnEggItem>> Eggs = List.of(BruteEgg, CrawlerEgg, WingbeastEgg, LeaperEgg, ChargerEgg);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Drip = ParticleRegister.register("scp354_beast_drip", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Splash = ParticleRegister.register("scp354_beast_splash", () -> new SimpleParticleType(false));

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BeastSplash = pair("scp354_beast_splash");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BeastSurge = pair("scp354_beast_surge");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BeastDrip = single("scp354_beast_drip");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BruteSwing = pair("scp354_brute_swing");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BrutePound = pair("scp354_brute_pound");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BruteRip = single("scp354_brute_rip");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BruteThrow = single("scp354_brute_throw");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BruteClodHit = pair("scp354_brute_clod_hit");
    public static final Scp354Voice BruteVoice = voice("brute", 0.9f, 0.9f);

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CrawlerBite = pair("scp354_crawler_bite");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CrawlerLunge = single("scp354_crawler_lunge");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CrawlerScrape = single("scp354_crawler_scrape");
    public static final Scp354Voice CrawlerVoice = voice("crawler", 1.0f, 0.45f);

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WingbeastSwoop = pair("scp354_wingbeast_swoop");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WingbeastRake = single("scp354_wingbeast_rake");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WingbeastGrab = single("scp354_wingbeast_grab");
    public static final Scp354Voice WingbeastVoice = voice("wingbeast", 1.0f, 0.8f);

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> LeaperLeap = single("scp354_leaper_leap");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> LeaperSlam = pair("scp354_leaper_slam");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> LeaperTongue = pair("scp354_leaper_tongue");
    public static final Scp354Voice LeaperVoice = voice("leaper", 1.0f, 0.6f);

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ChargerPaw = single("scp354_charger_paw");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ChargerBellow = pair("scp354_charger_bellow");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ChargerImpact = pair("scp354_charger_impact");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ChargerCrash = pair("scp354_charger_crash");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ChargerDaze = single("scp354_charger_daze");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ChargerToss = single("scp354_charger_toss");
    public static final Scp354Voice ChargerVoice = voice("charger", 1.0f, 0.85f);

    private Scp354BeastRegistry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static <T extends Scp354Beast> DeferredItem<DeferredSpawnEggItem> egg(String Name, DeferredHolder<EntityType<?>, EntityType<T>> Type) {
        return ItemRegister.register(Name, () -> new DeferredSpawnEggItem(Type, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    private static List<DeferredHolder<SoundEvent, SoundEvent>> single(String Name) {
        return List.of(sound(Name));
    }

    private static List<DeferredHolder<SoundEvent, SoundEvent>> pair(String Name) {
        return List.of(sound(Name), sound(Name + "_body"));
    }

    private static Scp354Voice voice(String Beast, float Pitch, float StepVolume) {
        String Prefix = "scp354_" + Beast + "_";
        return new Scp354Voice(pair(Prefix + "idle"), pair(Prefix + "hurt"), pair(Prefix + "death"), pair(Prefix + "step"), pair(Prefix + "emerge"), Pitch, StepVolume);
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
    }
}
