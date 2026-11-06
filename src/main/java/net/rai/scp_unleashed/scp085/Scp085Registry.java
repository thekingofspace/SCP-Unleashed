package net.rai.scp_unleashed.scp085;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

public final class Scp085Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_085");

    public static final DeferredHolder<SoundEvent, SoundEvent> Scribble = sound("scp085_scribble");
    public static final DeferredHolder<SoundEvent, SoundEvent> Page = sound("scp085_page");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hop = sound("scp085_hop");
    public static final DeferredHolder<SoundEvent, SoundEvent> Shift = sound("scp085_shift");
    public static final DeferredHolder<SoundEvent, SoundEvent> SheetPlace = sound("scp085_sheet_place");
    public static final DeferredHolder<SoundEvent, SoundEvent> SheetBreak = sound("scp085_sheet_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> SheetHit = sound("scp085_sheet_hit");

    public static final SoundType SheetSound = new DeferredSoundType(1.0f, 1.0f, SheetBreak, SheetHit, SheetPlace, SheetHit, SheetHit);

    public static final DeferredBlock<Scp085DrawingBlock> Drawing = BlockRegister.register("scp085_drawing", () -> new Scp085DrawingBlock(sheetProperties()));
    public static final DeferredBlock<Scp085HomeSheetBlock> HomeSheet = BlockRegister.register("scp085_home_sheet", () -> new Scp085HomeSheetBlock(sheetProperties()));

    public static final DeferredItem<Scp085SheetItem> DrawingItem = ItemRegister.register("scp085_drawing",
            () -> new Scp085SheetItem(Drawing.get(), new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Scp085SheetItem> HomeSheetItem = ItemRegister.register("scp085_home_sheet",
            () -> new Scp085SheetItem(HomeSheet.get(), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp085SheetBlockEntity>> SheetEntity = BlockEntityTypeRegister.register("scp085_sheet",
            () -> BlockEntityType.Builder.of(Scp085SheetBlockEntity::new, Drawing.get(), HomeSheet.get()).build(null));

    private Scp085Registry() {
    }

    private static BlockBehaviour.Properties sheetProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.3f, 0.5f).sound(SheetSound).noCollission().noOcclusion()
                .pushReaction(PushReaction.DESTROY).isValidSpawn((State, World, Pos, Kind) -> false).isRedstoneConductor((State, World, Pos) -> false)
                .isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
