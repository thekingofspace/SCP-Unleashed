package net.rai.scp_unleashed.scp3008.furniture.electronics;

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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class Scp3008FurnitureERegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final Map<Scp3008FurnitureEKind, DeferredBlock<Scp3008FurnitureEBlock>> PieceBlocks = new EnumMap<>(Scp3008FurnitureEKind.class);
    public static final List<DeferredItem<BlockItem>> Pieces = new ArrayList<>();

    static {
        for (Scp3008FurnitureEKind Type : Scp3008FurnitureEKind.values()) {
            DeferredBlock<Scp3008FurnitureEBlock> Made = BlockRegister.register(Type.Id,
                    () -> Type.storage() ? new Scp3008FurnitureEStorageBlock(settings(Type), Type) : new Scp3008FurnitureEBlock(settings(Type), Type));
            PieceBlocks.put(Type, Made);
            Pieces.add(ItemRegister.registerSimpleBlockItem(Type.Id, Made, new Item.Properties()));
        }
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp3008FurnitureEStorageEntity>> StorageEntity = BlockEntityTypeRegister.register("scp3008_furne_storage",
            () -> BlockEntityType.Builder.of(Scp3008FurnitureEStorageEntity::new, PieceBlocks.entrySet().stream().filter(Entry -> Entry.getKey().storage())
                    .map(Entry -> (Block) Entry.getValue().get()).toArray(Block[]::new)).build(null));

    public static final DeferredHolder<SoundEvent, SoundEvent> Click = sound("scp3008_furne_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> PowerOn = sound("scp3008_furne_power_on");
    public static final DeferredHolder<SoundEvent, SoundEvent> PowerOff = sound("scp3008_furne_power_off");
    public static final DeferredHolder<SoundEvent, SoundEvent> LampOn = sound("scp3008_furne_lamp_on");
    public static final DeferredHolder<SoundEvent, SoundEvent> LampOff = sound("scp3008_furne_lamp_off");
    public static final DeferredHolder<SoundEvent, SoundEvent> DrawerOpen = sound("scp3008_furne_drawer_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> DrawerClose = sound("scp3008_furne_drawer_close");

    private Scp3008FurnitureERegistry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    public static Scp3008FurnitureEBlock block(Scp3008FurnitureEKind Type) {
        return PieceBlocks.get(Type).get();
    }

    private static BlockBehaviour.Properties settings(Scp3008FurnitureEKind Type) {
        BlockBehaviour.Properties Made = switch (Type.Material) {
            case Fabric -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.0f).sound(SoundType.WOOL).ignitedByLava();
            case Metal -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5f, 4.0f).sound(SoundType.METAL);
            case Card -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.6f).sound(SoundType.SCAFFOLDING).ignitedByLava();
            default -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5f, 3.0f).sound(SoundType.WOOD).ignitedByLava();
        };
        Made = Made.noOcclusion().isRedstoneConductor((State, World, Pos) -> false).isSuffocating((State, World, Pos) -> false)
                .isViewBlocking((State, World, Pos) -> false).lightLevel(State -> Scp3008FurnitureEBlock.lightOf(State, Type));
        if (Type.multiblock() || Type.storage()) {
            Made = Made.pushReaction(PushReaction.BLOCK);
        }
        return Made;
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
