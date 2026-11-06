package net.rai.scp_unleashed.scp3008.furniture.bedroom;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class Scp3008FurnitureBRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final Map<Scp3008FurnitureBPiece, DeferredBlock<Scp3008FurnitureBBlock>> Pieces = new EnumMap<>(Scp3008FurnitureBPiece.class);
    public static final List<DeferredItem<BlockItem>> PieceItems = new ArrayList<>();

    static {
        for (Scp3008FurnitureBPiece Piece : Scp3008FurnitureBPiece.values()) {
            DeferredBlock<Scp3008FurnitureBBlock> Made = BlockRegister.register(Piece.Name, () -> Scp3008FurnitureBBlock.make(Piece, properties(Piece)));
            Pieces.put(Piece, Made);
            PieceItems.add(ItemRegister.registerSimpleBlockItem(Piece.Name, Made, new Item.Properties()));
        }
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp3008FurnitureBStorage>> Storage = BlockEntityTypeRegister.register("scp3008_furnb_storage",
            () -> BlockEntityType.Builder.of(Scp3008FurnitureBStorage::new, storageBlocks()).build(null));

    public static final DeferredHolder<SoundEvent, SoundEvent> DrawerOpen = sound("scp3008_furnb_drawer_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> DrawerSlide = sound("scp3008_furnb_drawer_slide");
    public static final DeferredHolder<SoundEvent, SoundEvent> DrawerClose = sound("scp3008_furnb_drawer_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> DrawerKnock = sound("scp3008_furnb_drawer_knock");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoorOpen = sound("scp3008_furnb_door_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoorClose = sound("scp3008_furnb_door_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoorLatch = sound("scp3008_furnb_door_latch");
    public static final DeferredHolder<SoundEvent, SoundEvent> GlassOpen = sound("scp3008_furnb_glass_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> GlassClose = sound("scp3008_furnb_glass_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> LidOpen = sound("scp3008_furnb_lid_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> LidClose = sound("scp3008_furnb_lid_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> LampClick = sound("scp3008_furnb_lamp_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> LampHum = sound("scp3008_furnb_lamp_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> Flush = sound("scp3008_furnb_flush");
    public static final DeferredHolder<SoundEvent, SoundEvent> FlushSwirl = sound("scp3008_furnb_flush_swirl");
    public static final DeferredHolder<SoundEvent, SoundEvent> Tap = sound("scp3008_furnb_tap");
    public static final DeferredHolder<SoundEvent, SoundEvent> BathFill = sound("scp3008_furnb_bath_fill");
    public static final DeferredHolder<SoundEvent, SoundEvent> BathBubbles = sound("scp3008_furnb_bath_bubbles");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChairHiss = sound("scp3008_furnb_chair_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChairCreak = sound("scp3008_furnb_chair_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> BedSit = sound("scp3008_furnb_bed_sit");

    private Scp3008FurnitureBRegistry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static BlockBehaviour.Properties properties(Scp3008FurnitureBPiece Piece) {
        BlockBehaviour.Properties Made = BlockBehaviour.Properties.of().mapColor(Piece.Colour).strength(Piece.Finish.Hardness, Piece.Finish.Resistance)
                .sound(Piece.Finish.Sound).noOcclusion().pushReaction(Piece.multi() ? PushReaction.BLOCK : PushReaction.NORMAL);
        if (Piece.Finish == Scp3008FurnitureBPiece.Feel.Wood || Piece.Finish == Scp3008FurnitureBPiece.Feel.Fabric || Piece.Finish == Scp3008FurnitureBPiece.Feel.Woven) {
            Made = Made.ignitedByLava();
        }
        if (Piece.Action == Scp3008FurnitureBPiece.Trick.Lamp) {
            Made = Made.lightLevel(State -> State.hasProperty(BlockStateProperties.LIT) && State.getValue(BlockStateProperties.LIT) ? Scp3008FurnitureBBlock.LampLight : 0);
        }
        return Made;
    }

    private static Block[] storageBlocks() {
        List<Block> Found = new ArrayList<>();
        for (Map.Entry<Scp3008FurnitureBPiece, DeferredBlock<Scp3008FurnitureBBlock>> Entry : Pieces.entrySet()) {
            if (Entry.getKey().storage()) {
                Found.add(Entry.getValue().get());
            }
        }
        return Found.toArray(new Block[0]);
    }

    public static Scp3008FurnitureBBlock block(Scp3008FurnitureBPiece Piece) {
        return Pieces.get(Piece).get();
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
