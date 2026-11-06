package net.rai.scp_unleashed.scp3008.furniture.living;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
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

import java.util.EnumMap;
import java.util.Map;

public final class Scp3008FurnitureARegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final Map<Scp3008FurnitureAPiece, DeferredBlock<Block>> Furniture = new EnumMap<>(Scp3008FurnitureAPiece.class);
    public static final Map<Scp3008FurnitureAPiece, DeferredItem<BlockItem>> FurnitureItems = new EnumMap<>(Scp3008FurnitureAPiece.class);

    static {
        for (Scp3008FurnitureAPiece Piece : Scp3008FurnitureAPiece.values()) {
            DeferredBlock<Block> Made = BlockRegister.register(Piece.Id, () -> make(Piece));
            Furniture.put(Piece, Made);
            FurnitureItems.put(Piece, ItemRegister.registerSimpleBlockItem(Piece.Id, Made, new Item.Properties()));
        }
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp3008FurnitureAStorageBlockEntity>> Storage = BlockEntityTypeRegister.register("scp3008_furna_storage",
            () -> BlockEntityType.Builder.of(Scp3008FurnitureAStorageBlockEntity::new, Furniture.entrySet().stream().filter(Entry -> Entry.getKey().storage())
                    .map(Entry -> Entry.getValue().get()).toArray(Block[]::new)).build(null));

    public static final DeferredHolder<SoundEvent, SoundEvent> DoorOpen = sound("scp3008_furna_door_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoorClose = sound("scp3008_furna_door_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> SoftClick = sound("scp3008_furna_soft_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> DrawerOpen = sound("scp3008_furna_drawer_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> DrawerClose = sound("scp3008_furna_drawer_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> FridgeOpen = sound("scp3008_furna_fridge_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> FridgeClose = sound("scp3008_furna_fridge_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> FridgeSeal = sound("scp3008_furna_fridge_seal");
    public static final DeferredHolder<SoundEvent, SoundEvent> GlassRattle = sound("scp3008_furna_glass_rattle");
    public static final DeferredHolder<SoundEvent, SoundEvent> BooksOut = sound("scp3008_furna_books_out");
    public static final DeferredHolder<SoundEvent, SoundEvent> BooksIn = sound("scp3008_furna_books_in");
    public static final DeferredHolder<SoundEvent, SoundEvent> HobClick = sound("scp3008_furna_hob_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> HobIgnite = sound("scp3008_furna_hob_ignite");
    public static final DeferredHolder<SoundEvent, SoundEvent> Cushion = sound("scp3008_furna_cushion");
    public static final DeferredHolder<SoundEvent, SoundEvent> SeatWood = sound("scp3008_furna_seat_wood");

    private Scp3008FurnitureARegistry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    public static Block block(Scp3008FurnitureAPiece Piece) {
        return Furniture.get(Piece).get();
    }

    private static BlockBehaviour.Properties properties(Scp3008FurnitureAPiece Piece) {
        BlockBehaviour.Properties Made = switch (Piece.Finish) {
            case Fabric -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(0.8f).sound(SoundType.WOOL).ignitedByLava();
            case Metal -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f, 6.0f).sound(SoundType.METAL);
            default -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5f, 3.0f).sound(SoundType.WOOD).ignitedByLava();
        };
        Made = Made.noOcclusion().isRedstoneConductor((State, World, Pos) -> false).isSuffocating((State, World, Pos) -> false)
                .isViewBlocking((State, World, Pos) -> false);
        if (Piece.paired()) {
            Made = Made.pushReaction(PushReaction.BLOCK);
        }
        if (Piece.Kind == Scp3008FurnitureAPiece.Form.Stove) {
            Made = Made.lightLevel(Scp3008FurnitureAStoveBlock::lightOf);
        }
        return Made;
    }

    private static Block make(Scp3008FurnitureAPiece Piece) {
        BlockBehaviour.Properties Made = properties(Piece);
        return switch (Piece.Kind) {
            case Pair -> new Scp3008FurnitureAPairBlock(Made, Piece);
            case Cabinet -> new Scp3008FurnitureACabinetBlock(Made, Piece);
            case Tall -> new Scp3008FurnitureATallCabinetBlock(Made, Piece);
            case Stove -> new Scp3008FurnitureAStoveBlock(Made, Piece);
            default -> new Scp3008FurnitureABlock(Made, Piece);
        };
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
    }
}
