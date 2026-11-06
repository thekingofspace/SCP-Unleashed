package net.rai.scp_unleashed.warhead;

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
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

public final class WarheadRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceKey<DamageType> AlphaDamage = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "warhead_alpha"));

    public static final DeferredBlock<AlphaWarheadBlock> AlphaWarhead = BlockRegister.register("warhead_alpha", () -> new AlphaWarheadBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(25.0f, 1200.0f).sound(SoundType.NETHERITE_BLOCK).noOcclusion()
            .pushReaction(PushReaction.BLOCK).isValidSpawn((State, World, Pos, Kind) -> false).isSuffocating((State, World, Pos) -> false)
            .isViewBlocking((State, World, Pos) -> false).isRedstoneConductor((State, World, Pos) -> false).lightLevel(AlphaWarheadBlock::lightOf)));
    public static final DeferredBlock<WarheadPanelBlock> Panel = BlockRegister.register("warhead_panel", () -> new WarheadPanelBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY).requiresCorrectToolForDrops().strength(8.0f, 600.0f).sound(SoundType.METAL).noOcclusion()
            .pushReaction(PushReaction.BLOCK).isValidSpawn((State, World, Pos, Kind) -> false).isRedstoneConductor((State, World, Pos) -> false)
            .lightLevel(WarheadPanelBlock::lightOf)));

    public static final DeferredItem<BlockItem> AlphaWarheadItem = ItemRegister.register("warhead_alpha",
            () -> new BlockItem(AlphaWarhead.get(), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<BlockItem> PanelItem = ItemRegister.register("warhead_panel",
            () -> new BlockItem(Panel.get(), new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlphaWarheadBlockEntity>> AlphaWarheadEntity = BlockEntityTypeRegister.register("warhead_alpha",
            () -> BlockEntityType.Builder.of(AlphaWarheadBlockEntity::new, AlphaWarhead.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WarheadPanelBlockEntity>> PanelEntity = BlockEntityTypeRegister.register("warhead_panel",
            () -> BlockEntityType.Builder.of(WarheadPanelBlockEntity::new, Panel.get()).build(null));

    public static final DeferredHolder<SoundEvent, SoundEvent> Siren = sound("warhead_siren");
    public static final DeferredHolder<SoundEvent, SoundEvent> SirenLow = sound("warhead_siren_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> SirenChorus = sound("warhead_siren_chorus");
    public static final DeferredHolder<SoundEvent, SoundEvent> SirenThird = sound("warhead_siren_third");
    public static final DeferredHolder<SoundEvent, SoundEvent> KlaxonRasp = sound("warhead_klaxon_rasp");
    public static final DeferredHolder<SoundEvent, SoundEvent> KlaxonEdge = sound("warhead_klaxon_edge");
    public static final DeferredHolder<SoundEvent, SoundEvent> Chime = sound("warhead_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChimeSoft = sound("warhead_chime_soft");
    public static final DeferredHolder<SoundEvent, SoundEvent> Blast = sound("warhead_blast");
    public static final DeferredHolder<SoundEvent, SoundEvent> BlastCrack = sound("warhead_blast_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> BlastRoar = sound("warhead_blast_roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> BlastFar = sound("warhead_blast_far");
    public static final DeferredHolder<SoundEvent, SoundEvent> BlastSub = sound("warhead_blast_sub");
    public static final DeferredHolder<SoundEvent, SoundEvent> Debris = sound("warhead_debris");
    public static final DeferredHolder<SoundEvent, SoundEvent> LeverClunk = sound("warhead_lever_clunk");
    public static final DeferredHolder<SoundEvent, SoundEvent> LeverClick = sound("warhead_lever_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> CoverOpen = sound("warhead_cover_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> CoverClose = sound("warhead_cover_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> ButtonPress = sound("warhead_button");
    public static final DeferredHolder<SoundEvent, SoundEvent> AbortPress = sound("warhead_abort");
    public static final DeferredHolder<SoundEvent, SoundEvent> ControlLocked = sound("warhead_locked");
    public static final DeferredHolder<SoundEvent, SoundEvent> Armed = sound("warhead_armed");
    public static final DeferredHolder<SoundEvent, SoundEvent> Disarmed = sound("warhead_disarmed");
    public static final DeferredHolder<SoundEvent, SoundEvent> Engage = sound("warhead_engage");
    public static final DeferredHolder<SoundEvent, SoundEvent> Cancel = sound("warhead_cancel");

    private WarheadRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
    }
}
