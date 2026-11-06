package net.rai.scp_unleashed.scp256;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
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
import net.rai.scp_unleashed.archive.investigation.StartConditions;

import java.util.List;

public final class Scp256Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_256");
    public static final TagKey<Block> Desks = TagKey.create(Registries.BLOCK, location("scp256_desks"));

    public static final DeferredBlock<Scp256TypewriterBlock> Typewriter = BlockRegister.register("scp256_typewriter",
            () -> new Scp256TypewriterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(2.0f, 1200.0f)
                    .sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK)));
    public static final DeferredItem<BlockItem> TypewriterItem = ItemRegister.register("scp256_typewriter",
            () -> new BlockItem(Typewriter.get(), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp256TypewriterBlockEntity>> TypewriterEntity = BlockEntityTypeRegister.register("scp256_typewriter",
            () -> BlockEntityType.Builder.of(Scp256TypewriterBlockEntity::new, Typewriter.get()).build(null));

    public static final DeferredHolder<SoundEvent, SoundEvent> Key = sound("scp256_key");
    public static final DeferredHolder<SoundEvent, SoundEvent> Strike = sound("scp256_key_strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> Space = sound("scp256_space");
    public static final DeferredHolder<SoundEvent, SoundEvent> Ding = sound("scp256_ding");
    public static final DeferredHolder<SoundEvent, SoundEvent> Shimmer = sound("scp256_ding_shimmer");
    public static final DeferredHolder<SoundEvent, SoundEvent> Return = sound("scp256_return");
    public static final DeferredHolder<SoundEvent, SoundEvent> Stop = sound("scp256_return_stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> Paper = sound("scp256_paper");
    public static final DeferredHolder<SoundEvent, SoundEvent> Ratchet = sound("scp256_paper_ratchet");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> KeyLayers = List.of(Key, Strike);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SpaceLayers = List.of(Space);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DingLayers = List.of(Ding, Shimmer);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ReturnLayers = List.of(Return, Ratchet);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StopLayers = List.of(Stop);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PaperLayers = List.of(Paper, Ratchet);

    private Scp256Registry() {
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
        SoundEventRegister.register(ModEventBus);
        StartConditions.register(Scp256DeskCondition.TypeName, Scp256DeskCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
