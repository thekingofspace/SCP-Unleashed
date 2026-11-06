package net.rai.scp_unleashed.scp3008.furniture.children;

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

public final class Scp3008FurnitureCRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final Map<Scp3008FurnitureCKind, DeferredBlock<Scp3008FurnitureCBlock>> PieceBlocks = new EnumMap<>(Scp3008FurnitureCKind.class);
    public static final List<DeferredItem<BlockItem>> Pieces = new ArrayList<>();

    static {
        for (Scp3008FurnitureCKind Type : Scp3008FurnitureCKind.values()) {
            DeferredBlock<Scp3008FurnitureCBlock> Made = BlockRegister.register(Type.Id,
                    () -> Type.storage() ? new Scp3008FurnitureCStorageBlock(settings(Type), Type) : new Scp3008FurnitureCBlock(settings(Type), Type));
            PieceBlocks.put(Type, Made);
            Pieces.add(ItemRegister.registerSimpleBlockItem(Type.Id, Made, new Item.Properties()));
        }
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp3008FurnitureCStorageEntity>> StorageEntity = BlockEntityTypeRegister.register("scp3008_furnc_storage",
            () -> BlockEntityType.Builder.of(Scp3008FurnitureCStorageEntity::new, PieceBlocks.entrySet().stream().filter(Entry -> Entry.getKey().storage())
                    .map(Entry -> (Block) Entry.getValue().get()).toArray(Block[]::new)).build(null));

    public static final DeferredHolder<SoundEvent, SoundEvent> SwitchClick = sound("scp3008_furnc_switch_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> LampOn = sound("scp3008_furnc_lamp_on");
    public static final DeferredHolder<SoundEvent, SoundEvent> LampOff = sound("scp3008_furnc_lamp_off");
    public static final DeferredHolder<SoundEvent, SoundEvent> LidOpen = sound("scp3008_furnc_lid_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> LidClose = sound("scp3008_furnc_lid_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> ToyRattle = sound("scp3008_furnc_toy_rattle");
    public static final DeferredHolder<SoundEvent, SoundEvent> BoxOpen = sound("scp3008_furnc_box_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> BoxClose = sound("scp3008_furnc_box_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rummage = sound("scp3008_furnc_rummage");
    public static final DeferredHolder<SoundEvent, SoundEvent> OvenOpen = sound("scp3008_furnc_oven_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> OvenClose = sound("scp3008_furnc_oven_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> RockCreak = sound("scp3008_furnc_rock_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> SignTurn = sound("scp3008_furnc_sign_turn");
    public static final DeferredHolder<SoundEvent, SoundEvent> ParasolOpen = sound("scp3008_furnc_parasol_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> ParasolClose = sound("scp3008_furnc_parasol_close");

    private Scp3008FurnitureCRegistry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    public static Scp3008FurnitureCBlock block(Scp3008FurnitureCKind Type) {
        return PieceBlocks.get(Type).get();
    }

    private static BlockBehaviour.Properties settings(Scp3008FurnitureCKind Type) {
        BlockBehaviour.Properties Made = switch (Type.Material) {
            case Fabric -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(0.8f).sound(SoundType.WOOL).ignitedByLava();
            case Metal -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5f, 4.0f).sound(SoundType.METAL);
            case Glass -> BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.6f).sound(SoundType.GLASS);
            case Card -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.6f).sound(SoundType.SCAFFOLDING).ignitedByLava();
            default -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5f, 3.0f).sound(SoundType.WOOD).ignitedByLava();
        };
        Made = Made.noOcclusion().isRedstoneConductor((State, World, Pos) -> false).isSuffocating((State, World, Pos) -> false)
                .isViewBlocking((State, World, Pos) -> false).lightLevel(State -> Scp3008FurnitureCBlock.lightOf(State, Type));
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
