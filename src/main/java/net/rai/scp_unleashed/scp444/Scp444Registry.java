package net.rai.scp_unleashed.scp444;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.List;

public final class Scp444Registry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_444");

    public static final DeferredHolder<SoundEvent, SoundEvent> Open = sound("scp444_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> OpenLeather = sound("scp444_open_leather");
    public static final DeferredHolder<SoundEvent, SoundEvent> Turn = sound("scp444_turn");
    public static final DeferredHolder<SoundEvent, SoundEvent> TurnPaper = sound("scp444_turn_paper");
    public static final DeferredHolder<SoundEvent, SoundEvent> Close = sound("scp444_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> CloseThud = sound("scp444_close_thud");
    public static final DeferredHolder<SoundEvent, SoundEvent> Settle = sound("scp444_settle");
    public static final DeferredHolder<SoundEvent, SoundEvent> SettleLow = sound("scp444_settle_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hush = sound("scp444_hush");
    public static final DeferredHolder<SoundEvent, SoundEvent> HushWick = sound("scp444_hush_wick");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> OpenLayers = List.of(Open, OpenLeather);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TurnLayers = List.of(Turn, TurnPaper);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CloseLayers = List.of(Close, CloseThud);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SettleLayers = List.of(Settle, SettleLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HushLayers = List.of(Hush, HushWick);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Glyph = ParticleRegister.register("scp444_glyph", () -> new SimpleParticleType(false));

    public static final DeferredItem<Scp444BookItem> Book = ItemRegister.register("scp444_book",
            () -> new Scp444BookItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> Silence = AttachmentRegister.register("scp444_silence",
            () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).build());

    public static final String SpreadRule = "scp444Spread";
    public static GameRules.Key<GameRules.BooleanValue> Spread;

    private Scp444Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
        ModEventBus.addListener(FMLCommonSetupEvent.class, Event -> Event.enqueueWork(Scp444Registry::registerRules));
    }

    private static void registerRules() {
        if (Spread == null) {
            Spread = GameRules.register(SpreadRule, GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
        }
    }

    public static boolean spreads(GameRules Rules) {
        if (Spread == null) {
            return true;
        }
        GameRules.BooleanValue Rule = Rules.getRule(Spread);
        return Rule == null || Rule.get();
    }
}
