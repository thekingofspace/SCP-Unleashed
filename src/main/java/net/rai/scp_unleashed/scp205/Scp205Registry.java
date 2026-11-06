package net.rai.scp_unleashed.scp205;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.List;

public final class Scp205Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_205");
    public static final ResourceKey<DamageType> DevilDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp205_devil"));
    public static final float LampHardness = 2.5f;
    public static final float LampResistance = 1200.0f;

    public static final DeferredBlock<Scp205LampBlock> Lamp = BlockRegister.register("scp205_lamp",
            () -> new Scp205LampBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(LampHardness, LampResistance).sound(SoundType.LANTERN)
                    .noOcclusion().pushReaction(PushReaction.BLOCK)));
    public static final DeferredItem<BlockItem> LampItem = ItemRegister.register("scp205_lamp",
            () -> new BlockItem(Lamp.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredBlock<Scp205GlowBlock> Glow = BlockRegister.register("scp205_glow",
            () -> new Scp205GlowBlock(BlockBehaviour.Properties.of().replaceable().noCollission().noLootTable().noOcclusion().randomTicks()
                    .strength(-1.0f, 3600000.8f).mapColor(MapColor.NONE).pushReaction(PushReaction.DESTROY).lightLevel(Scp205GlowBlock::emission)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp205LampBlockEntity>> LampEntity = BlockEntityTypeRegister.register("scp205_lamp",
            () -> BlockEntityType.Builder.of(Scp205LampBlockEntity::new, Lamp.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp205Devil>> Devil = EntityTypeRegister.register("scp205_devil",
            () -> EntityType.Builder.<Scp205Devil>of(Scp205Devil::new, MobCategory.MONSTER)
                    .sized(Scp205Devil.Width, Scp205Devil.Height)
                    .eyeHeight(Scp205Devil.EyeHeight)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("scp205_devil"));

    public static final DeferredHolder<SoundEvent, SoundEvent> Click = sound("scp205_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> ClickBody = sound("scp205_click_body");
    public static final DeferredHolder<SoundEvent, SoundEvent> Warm = sound("scp205_warm");
    public static final DeferredHolder<SoundEvent, SoundEvent> Cool = sound("scp205_cool");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hum = sound("scp205_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> HumBuzz = sound("scp205_hum_buzz");
    public static final DeferredHolder<SoundEvent, SoundEvent> Flutter = sound("scp205_flutter");
    public static final DeferredHolder<SoundEvent, SoundEvent> FlutterWhir = sound("scp205_flutter_whir");
    public static final DeferredHolder<SoundEvent, SoundEvent> Whisper = sound("scp205_whisper");
    public static final DeferredHolder<SoundEvent, SoundEvent> WhisperBreath = sound("scp205_whisper_breath");
    public static final DeferredHolder<SoundEvent, SoundEvent> Snarl = sound("scp205_snarl");
    public static final DeferredHolder<SoundEvent, SoundEvent> SnarlGrowl = sound("scp205_snarl_growl");
    public static final DeferredHolder<SoundEvent, SoundEvent> Emerge = sound("scp205_emerge");
    public static final DeferredHolder<SoundEvent, SoundEvent> EmergeWind = sound("scp205_emerge_wind");
    public static final DeferredHolder<SoundEvent, SoundEvent> Fade = sound("scp205_fade");
    public static final DeferredHolder<SoundEvent, SoundEvent> FadeWind = sound("scp205_fade_wind");
    public static final DeferredHolder<SoundEvent, SoundEvent> Pass = sound("scp205_pass");
    public static final DeferredHolder<SoundEvent, SoundEvent> Strike = sound("scp205_strike");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ClickLayers = List.of(Click, ClickBody);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WarmLayers = List.of(Click, ClickBody, Warm);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CoolLayers = List.of(Click, ClickBody, Cool);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HumLayers = List.of(Hum, HumBuzz);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> FlutterLayers = List.of(Flutter, FlutterWhir);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WhisperLayers = List.of(Whisper, WhisperBreath);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SnarlLayers = List.of(Snarl, SnarlGrowl);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> EmergeLayers = List.of(Emerge, EmergeWind, Snarl);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> FadeLayers = List.of(Fade, FadeWind);

    private Scp205Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
