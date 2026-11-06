package net.rai.scp_unleashed.z0001;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.investigation.StartConditions;
import net.rai.scp_unleashed.storage.StorageBehaviours;

import java.util.List;

public final class Z0001Registry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("z_0001");
    public static final ResourceKey<DamageType> BlackBookDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("black_book"));
    public static final String CooldownRuleName = "scpBlackBookCooldownDays";
    public static final int DefaultCooldownDays = 1;

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Z0001Page>> Page = ComponentRegister.registerComponentType("z0001_page",
            Builder -> Builder.persistent(Z0001Page.PageCodec).networkSynchronized(Z0001Page.PageStreamCodec));

    public static final DeferredItem<Z0001BookItem> Book = ItemRegister.register("z0001_book",
            () -> new Z0001BookItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredHolder<SoundEvent, SoundEvent> BookOpen = sound("z0001_book_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> BookLeather = sound("z0001_book_leather");
    public static final DeferredHolder<SoundEvent, SoundEvent> BookClose = sound("z0001_book_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> PageTurn = sound("z0001_page_turn");
    public static final DeferredHolder<SoundEvent, SoundEvent> PageRustle = sound("z0001_page_rustle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Quill = sound("z0001_quill");
    public static final DeferredHolder<SoundEvent, SoundEvent> QuillScratch = sound("z0001_quill_scratch");
    public static final DeferredHolder<SoundEvent, SoundEvent> InkTake = sound("z0001_ink_take");
    public static final DeferredHolder<SoundEvent, SoundEvent> InkSoak = sound("z0001_ink_soak");
    public static final DeferredHolder<SoundEvent, SoundEvent> InkFade = sound("z0001_ink_fade");
    public static final DeferredHolder<SoundEvent, SoundEvent> InkHiss = sound("z0001_ink_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> Heartbeat = sound("z0001_heartbeat");
    public static final DeferredHolder<SoundEvent, SoundEvent> HeartbeatThump = sound("z0001_heartbeat_thump");
    public static final DeferredHolder<SoundEvent, SoundEvent> Whisper = sound("z0001_whisper");
    public static final DeferredHolder<SoundEvent, SoundEvent> WhisperBreath = sound("z0001_whisper_breath");
    public static final DeferredHolder<SoundEvent, SoundEvent> Toll = sound("z0001_toll");
    public static final DeferredHolder<SoundEvent, SoundEvent> TollResonance = sound("z0001_toll_resonance");
    public static final DeferredHolder<SoundEvent, SoundEvent> TollDepth = sound("z0001_toll_depth");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> OpenLayers = List.of(BookOpen, BookLeather);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CloseLayers = List.of(BookClose, BookLeather);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TurnLayers = List.of(PageTurn, PageRustle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> QuillLayers = List.of(Quill, QuillScratch);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TakeLayers = List.of(InkTake, InkSoak);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> FadeLayers = List.of(InkFade, InkHiss);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HeartLayers = List.of(Heartbeat, HeartbeatThump);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WhisperLayers = List.of(Whisper, WhisperBreath);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TollLayers = List.of(Toll, TollResonance, TollDepth);

    public static GameRules.Key<GameRules.IntegerValue> CooldownRule;

    private Z0001Registry() {
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
        StartConditions.register(Z0001ProcuredCondition.TypeName, Z0001ProcuredCondition.ConditionCodec);
        StorageBehaviours.register(Z0001BookItem::is, Z0001Containment.Instance);
        ModEventBus.addListener(FMLCommonSetupEvent.class, Event -> Event.enqueueWork(Z0001Registry::registerRules));
    }

    private static void registerRules() {
        if (CooldownRule == null) {
            CooldownRule = GameRules.register(CooldownRuleName, GameRules.Category.PLAYER, GameRules.IntegerValue.create(DefaultCooldownDays));
        }
    }

    public static int cooldownDays(GameRules Rules) {
        if (CooldownRule == null) {
            return DefaultCooldownDays;
        }
        return Math.max(0, Rules.getInt(CooldownRule));
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }
}
