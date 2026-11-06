package net.rai.scp_unleashed.raid.hostile;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.faction.FactionRegistry;
import net.rai.scp_unleashed.faction.Factions;
import net.rai.scp_unleashed.raid.chaos.ChaosFaction;
import net.rai.scp_unleashed.raid.chaos.ChaosHelicopter;
import net.rai.scp_unleashed.raid.chaos.ChaosInsurgent;
import net.rai.scp_unleashed.raid.serpentshand.SerpentPodItem;
import net.rai.scp_unleashed.raid.serpentshand.SerpentSeal;
import net.rai.scp_unleashed.raid.serpentshand.SerpentsHandAgent;
import net.rai.scp_unleashed.raid.serpentshand.SerpentsHandFaction;
import net.rai.scp_unleashed.stasis.StasisContents;

public final class HostileRegistry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<StasisContents>> PodContents = ComponentRegister.registerComponentType(
            "serpents_hand_pod_contents", Builder -> Builder.persistent(StasisContents.ContentsCodec).networkSynchronized(StasisContents.ContentsStreamCodec));

    public static final DeferredItem<SerpentPodItem> Pod = ItemRegister.register("serpents_hand_stasis_pod",
            () -> new SerpentPodItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<EntityType<?>, EntityType<SerpentsHandAgent>> AgentType = EntityTypeRegister.register("serpents_hand_agent",
            () -> EntityType.Builder.<SerpentsHandAgent>of(SerpentsHandAgent::new, MobCategory.MISC).sized(0.6f, 1.8f).eyeHeight(1.62f)
                    .vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT).clientTrackingRange(10).noSave().build("serpents_hand_agent"));
    public static final DeferredHolder<EntityType<?>, EntityType<ChaosInsurgent>> InsurgentType = EntityTypeRegister.register("chaos_insurgent",
            () -> EntityType.Builder.<ChaosInsurgent>of(ChaosInsurgent::new, MobCategory.MISC).sized(0.6f, 1.8f).eyeHeight(1.62f)
                    .vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT).clientTrackingRange(10).noSave().build("chaos_insurgent"));
    public static final DeferredHolder<EntityType<?>, EntityType<SerpentSeal>> SealType = EntityTypeRegister.register("serpents_hand_seal",
            () -> EntityType.Builder.<SerpentSeal>of(SerpentSeal::new, MobCategory.MISC).sized(3.0f, 0.2f).fireImmune().noSave()
                    .clientTrackingRange(10).updateInterval(20).build("serpents_hand_seal"));
    public static final DeferredHolder<EntityType<?>, EntityType<ChaosHelicopter>> HelicopterType = EntityTypeRegister.register("chaos_helicopter",
            () -> EntityType.Builder.<ChaosHelicopter>of(ChaosHelicopter::new, MobCategory.MISC).sized(3.0f, 3.2f).fireImmune().noSave()
                    .clientTrackingRange(16).updateInterval(2).build("chaos_helicopter"));

    public static final DeferredHolder<SoundEvent, SoundEvent> SealOpen = sound("serpents_hand_seal_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> SealChime = sound("serpents_hand_seal_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> SealHum = sound("serpents_hand_seal_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> SealRise = sound("serpents_hand_seal_rise");
    public static final DeferredHolder<SoundEvent, SoundEvent> SealSink = sound("serpents_hand_seal_sink");
    public static final DeferredHolder<SoundEvent, SoundEvent> SealClose = sound("serpents_hand_seal_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> SerpentWarning = sound("serpents_hand_warning");
    public static final DeferredHolder<SoundEvent, SoundEvent> ForceLock = sound("chaos_insurgency_force");
    public static final DeferredHolder<SoundEvent, SoundEvent> ForceStrike = sound("chaos_insurgency_force_strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> BreachPlant = sound("chaos_insurgency_breach_plant");
    public static final DeferredHolder<SoundEvent, SoundEvent> BreachBeep = sound("chaos_insurgency_breach_beep");

    public static final String SerpentRuleName = "scpSerpentsHandRaids";
    public static final String ChaosRuleName = "scpChaosInsurgencyRaids";
    public static final String BreachRuleName = "scpChaosBreaching";
    public static GameRules.Key<GameRules.BooleanValue> SerpentRule;
    public static GameRules.Key<GameRules.BooleanValue> ChaosRule;
    public static GameRules.Key<GameRules.BooleanValue> BreachRule;

    private HostileRegistry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        FactionRegistry.bootstrap(ModEventBus);
        ItemRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
        ModEventBus.addListener(FMLCommonSetupEvent.class, Event -> Event.enqueueWork(HostileRegistry::registerRules));
        Factions.register(SerpentsHandFaction.Instance);
        Factions.register(ChaosFaction.Instance);
    }

    private static void registerRules() {
        if (SerpentRule == null) {
            SerpentRule = GameRules.register(SerpentRuleName, GameRules.Category.MOBS, GameRules.BooleanValue.create(true));
        }
        if (ChaosRule == null) {
            ChaosRule = GameRules.register(ChaosRuleName, GameRules.Category.MOBS, GameRules.BooleanValue.create(true));
        }
        if (BreachRule == null) {
            BreachRule = GameRules.register(BreachRuleName, GameRules.Category.MOBS, GameRules.BooleanValue.create(true));
        }
    }

    public static boolean ruleOn(MinecraftServer Server, GameRules.Key<GameRules.BooleanValue> Key) {
        if (Key == null) {
            return true;
        }
        GameRules.BooleanValue Rule = Server.getGameRules().getRule(Key);
        return Rule == null || Rule.get();
    }
}
