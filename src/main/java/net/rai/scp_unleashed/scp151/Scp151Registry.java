package net.rai.scp_unleashed.scp151;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
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

import java.util.List;

public final class Scp151Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_151");
    public static final ResourceKey<DamageType> DrowningDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp151_drowning"));

    public static final DeferredBlock<Scp151PaintingBlock> Painting = BlockRegister.register("scp151_painting",
            () -> new Scp151PaintingBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5f, 1200.0f).sound(SoundType.WOOD)
                    .noOcclusion().pushReaction(PushReaction.BLOCK).isRedstoneConductor((State, World, Pos) -> false)
                    .isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false)));
    public static final DeferredItem<BlockItem> PaintingItem = ItemRegister.register("scp151_painting",
            () -> new BlockItem(Painting.get(), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp151PaintingBlockEntity>> PaintingEntity = BlockEntityTypeRegister.register("scp151_painting",
            () -> BlockEntityType.Builder.of(Scp151PaintingBlockEntity::new, Painting.get()).build(null));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> LastGaze = AttachmentRegister.register("scp151_last_gaze",
            () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG, Saved -> Saved > 0L).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> SentState = AttachmentRegister.register("scp151_sent_state",
            () -> AttachmentType.builder(() -> -1).build());

    public static final DeferredHolder<SoundEvent, SoundEvent> Waves = sound("scp151_waves");
    public static final DeferredHolder<SoundEvent, SoundEvent> WavesWash = sound("scp151_waves_wash");
    public static final DeferredHolder<SoundEvent, SoundEvent> GazeLoop = sound("scp151_gaze_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> GazeEnter = sound("scp151_gaze_enter");
    public static final DeferredHolder<SoundEvent, SoundEvent> Gurgle = sound("scp151_gurgle");
    public static final DeferredHolder<SoundEvent, SoundEvent> GurgleBubbles = sound("scp151_gurgle_bubbles");
    public static final DeferredHolder<SoundEvent, SoundEvent> Gasp = sound("scp151_gasp");
    public static final DeferredHolder<SoundEvent, SoundEvent> GaspBreath = sound("scp151_gasp_breath");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rustle = sound("scp151_rustle");
    public static final DeferredHolder<SoundEvent, SoundEvent> RustleCloth = sound("scp151_rustle_cloth");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WaveLayers = List.of(Waves, WavesWash);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> GurgleLayers = List.of(Gurgle, GurgleBubbles);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> GaspLayers = List.of(Gasp, GaspBreath);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RustleLayers = List.of(Rustle, RustleCloth);

    private Scp151Registry() {
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
        AttachmentRegister.register(ModEventBus);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
