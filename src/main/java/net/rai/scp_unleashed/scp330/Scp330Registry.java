package net.rai.scp_unleashed.scp330;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class Scp330Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<MobEffect> MobEffectRegister = DeferredRegister.create(Registries.MOB_EFFECT, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_330");
    public static final ResourceKey<DamageType> GreedDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp330_greed"));
    public static final ResourceKey<DamageType> PopDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp330_pop"));
    public static final int CandyStack = 16;

    public static final DeferredBlock<Scp330BowlBlock> Bowl = BlockRegister.register("scp330_bowl",
            () -> new Scp330BowlBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(1.5f, 1200.0f).sound(SoundType.DECORATED_POT)
                    .noOcclusion().pushReaction(PushReaction.BLOCK).isRedstoneConductor((State, World, Pos) -> false)
                    .isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false)));
    public static final DeferredItem<BlockItem> BowlItem = ItemRegister.register("scp330_bowl",
            () -> new BlockItem(Bowl.get(), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp330BowlBlockEntity>> BowlEntity = BlockEntityTypeRegister.register("scp330_bowl",
            () -> BlockEntityType.Builder.of(Scp330BowlBlockEntity::new, Bowl.get()).build(null));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Scp330Memory>> Memory = ComponentRegister.registerComponentType("scp330_memory",
            Builder -> Builder.persistent(Scp330Memory.MemoryCodec).networkSynchronized(Scp330Memory.MemoryStreamCodec));

    public static final Map<Scp330Colour, DeferredItem<Scp330CandyItem>> Candies = candies();

    public static final DeferredHolder<MobEffect, Scp330Effect> Greed = MobEffectRegister.register("scp330_greed",
            () -> new Scp330Effect(MobEffectCategory.HARMFUL, 0x6E2A3A, true));
    public static final DeferredHolder<MobEffect, Scp330Effect> Barrier = MobEffectRegister.register("scp330_barrier",
            () -> new Scp330Effect(MobEffectCategory.BENEFICIAL, 0x7FB8F0, false));
    public static final DeferredHolder<MobEffect, Scp330Effect> Bloom = MobEffectRegister.register("scp330_bloom",
            () -> new Scp330Effect(MobEffectCategory.BENEFICIAL, 0x6CC24A, false));
    public static final DeferredHolder<MobEffect, Scp330Effect> Sight = MobEffectRegister.register("scp330_sight",
            () -> new Scp330Effect(MobEffectCategory.BENEFICIAL, 0x9C5CD6, false));
    public static final DeferredHolder<MobEffect, Scp330Effect> Wrath = MobEffectRegister.register("scp330_wrath",
            () -> new Scp330Effect(MobEffectCategory.BENEFICIAL, 0x3A3540, false));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Scp330GreedState>> GreedState = AttachmentRegister.register("scp330_greed",
            () -> AttachmentType.builder(() -> Scp330GreedState.Fresh).serialize(Scp330GreedState.StateCodec).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Float>> Shell = AttachmentRegister.register("scp330_barrier",
            () -> AttachmentType.builder(() -> 0.0f).serialize(Codec.FLOAT).build());

    public static final DeferredHolder<SoundEvent, SoundEvent> Take = sound("scp330_take");
    public static final DeferredHolder<SoundEvent, SoundEvent> TakeRustle = sound("scp330_take_rustle");
    public static final DeferredHolder<SoundEvent, SoundEvent> GreedSigh = sound("scp330_greed");
    public static final DeferredHolder<SoundEvent, SoundEvent> GreedDrain = sound("scp330_greed_drain");
    public static final DeferredHolder<SoundEvent, SoundEvent> GreedToll = sound("scp330_greed_toll");
    public static final DeferredHolder<SoundEvent, SoundEvent> ShellUp = sound("scp330_barrier_up");
    public static final DeferredHolder<SoundEvent, SoundEvent> ShellChime = sound("scp330_barrier_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> ShellChimeBell = sound("scp330_barrier_chime_bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> ShellBreak = sound("scp330_barrier_shatter");
    public static final DeferredHolder<SoundEvent, SoundEvent> ShellBreakGlass = sound("scp330_barrier_shatter_glass");
    public static final DeferredHolder<SoundEvent, SoundEvent> Pop = sound("scp330_pop");
    public static final DeferredHolder<SoundEvent, SoundEvent> PopFizz = sound("scp330_pop_fizz");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TakeLayers = List.of(Take, TakeRustle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> GreedLayers = List.of(GreedSigh, GreedDrain, GreedToll);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> UpLayers = List.of(ShellUp);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ChimeLayers = List.of(ShellChime, ShellChimeBell);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ShatterLayers = List.of(ShellBreak, ShellBreakGlass);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PopLayers = List.of(Pop, PopFizz);

    private Scp330Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    private static Map<Scp330Colour, DeferredItem<Scp330CandyItem>> candies() {
        Map<Scp330Colour, DeferredItem<Scp330CandyItem>> Made = new EnumMap<>(Scp330Colour.class);
        FoodProperties Sweet = new FoodProperties.Builder().nutrition(1).saturationModifier(0.1f).fast().alwaysEdible().build();
        for (Scp330Colour Colour : Scp330Colour.values()) {
            Made.put(Colour, ItemRegister.register(Colour.itemId(),
                    () -> new Scp330CandyItem(Colour, new Item.Properties().stacksTo(CandyStack).rarity(Rarity.UNCOMMON).food(Sweet))));
        }
        return Made;
    }

    public static Scp330CandyItem candy(Scp330Colour Colour) {
        return Candies.get(Colour).get();
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        MobEffectRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
