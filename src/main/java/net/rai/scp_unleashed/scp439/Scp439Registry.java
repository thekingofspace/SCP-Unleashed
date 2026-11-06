package net.rai.scp_unleashed.scp439;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.StartConditions;
import net.rai.scp_unleashed.stasis.StasisPods;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class Scp439Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_439");
    public static final String SpreadRuleName = "scp439Spread";
    public static final ResourceKey<DamageType> Hollowed = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp439_hollowed"));
    public static final ResourceKey<DamageType> Collapse = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp439_collapse"));
    @Nullable
    private static GameRules.Key<GameRules.BooleanValue> SpreadRule;

    public static final DeferredHolder<EntityType<?>, EntityType<Scp439Queen>> Queen = EntityTypeRegister.register("scp439_queen",
            () -> EntityType.Builder.<Scp439Queen>of(Scp439Queen::new, MobCategory.MONSTER).sized(Scp439Queen.Width, Scp439Queen.Height)
                    .eyeHeight(Scp439Queen.EyeHeight).clientTrackingRange(10).build("scp439_queen"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp439Worker>> Worker = EntityTypeRegister.register("scp439_worker",
            () -> EntityType.Builder.<Scp439Worker>of(Scp439Worker::new, MobCategory.MONSTER).sized(Scp439Worker.Width, Scp439Worker.Height)
                    .eyeHeight(Scp439Worker.EyeHeight).clientTrackingRange(8).build("scp439_worker"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp439Brute>> Brute = EntityTypeRegister.register("scp439_brute",
            () -> EntityType.Builder.<Scp439Brute>of(Scp439Brute::new, MobCategory.MONSTER).sized(Scp439Brute.Width, Scp439Brute.Height)
                    .eyeHeight(Scp439Brute.EyeHeight).clientTrackingRange(10).build("scp439_brute"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp439Hive>> Hive = EntityTypeRegister.register("scp439_hive",
            () -> EntityType.Builder.<Scp439Hive>of(Scp439Hive::new, MobCategory.MISC).sized(Scp439Hive.Width, Scp439Hive.Height)
                    .eyeHeight(Scp439Hive.EyeHeight).clientTrackingRange(10).updateInterval(10).build("scp439_hive"));

    public static final DeferredItem<DeferredSpawnEggItem> QueenEgg = ItemRegister.register("scp439_queen_spawn_egg",
            () -> new DeferredSpawnEggItem(Queen, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<DeferredSpawnEggItem> WorkerEgg = ItemRegister.register("scp439_worker_spawn_egg",
            () -> new DeferredSpawnEggItem(Worker, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<DeferredSpawnEggItem> BruteEgg = ItemRegister.register("scp439_brute_spawn_egg",
            () -> new DeferredSpawnEggItem(Brute, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));
    public static final List<DeferredItem<DeferredSpawnEggItem>> Eggs = List.of(QueenEgg, WorkerEgg, BruteEgg);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Flake = ParticleRegister.register("scp439_flake", () -> new SimpleParticleType(false));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Scp439Infection.State>> Infection = AttachmentRegister.register("scp439_infection",
            () -> AttachmentType.builder(Scp439Infection.State::blank).serialize(Scp439Infection.State.StateCodec, Saved -> Saved.Duration > 0)
                    .sync(Scp439Infection.State.SyncCodec).build());

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> QueenIdle = pair("scp439_queen_idle");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> QueenHurt = pair("scp439_queen_hurt");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> QueenDeath = pair("scp439_queen_death");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> QueenStep = pair("scp439_queen_step");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> QueenHiss = single("scp439_queen_hiss");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> QueenBite = pair("scp439_queen_bite");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> QueenBurrow = single("scp439_queen_burrow");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> QueenEmerge = pair("scp439_queen_emerge");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WorkerIdle = pair("scp439_worker_idle");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WorkerHurt = single("scp439_worker_hurt");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WorkerDeath = single("scp439_worker_death");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WorkerStep = pair("scp439_worker_step");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WorkerBite = single("scp439_worker_bite");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WorkerPickup = single("scp439_worker_pickup");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WorkerDeliver = single("scp439_worker_deliver");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BruteIdle = pair("scp439_brute_idle");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BruteHurt = pair("scp439_brute_hurt");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BruteDeath = pair("scp439_brute_death");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BruteStep = pair("scp439_brute_step");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BruteAlarm = pair("scp439_brute_alarm");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BruteBite = pair("scp439_brute_bite");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HiveRustle = single("scp439_hive_rustle");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HiveGrow = pair("scp439_hive_grow");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HiveHurt = pair("scp439_hive_hurt");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HiveDeath = pair("scp439_hive_death");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HiveBirth = single("scp439_hive_birth");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HostRustle = single("scp439_host_rustle");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> Crumble = single("scp439_collapse");

    private Scp439Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static List<DeferredHolder<SoundEvent, SoundEvent>> pair(String Name) {
        return List.of(sound(Name), sound(Name + "_body"));
    }

    private static List<DeferredHolder<SoundEvent, SoundEvent>> single(String Name) {
        return List.of(sound(Name));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static boolean spreads(GameRules Rules) {
        if (SpreadRule == null) {
            return true;
        }
        GameRules.BooleanValue Rule = Rules.getRule(SpreadRule);
        return Rule == null || Rule.get();
    }

    public static void setSpread(MinecraftServer Server, boolean Value) {
        if (SpreadRule != null) {
            Server.getGameRules().getRule(SpreadRule).set(Value, Server);
        }
    }

    private static void registerRules() {
        if (SpreadRule == null) {
            SpreadRule = GameRules.register(SpreadRuleName, GameRules.Category.MOBS, GameRules.BooleanValue.create(true));
        }
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        StartConditions.register(Scp439ArrivalCondition.TypeName, Scp439ArrivalCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp439ArrivalCondition::manifest);
        StasisPods.addCaptureVeto(Found -> Found instanceof Scp439Queen Queen && Queen.isActing());
        ModEventBus.addListener(FMLCommonSetupEvent.class, Event -> Event.enqueueWork(Scp439Registry::registerRules));
    }
}
