package net.rai.scp_unleashed.advmaterials;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;

public final class AdvancedMaterialsRegistry {
    public static final ResourceLocation RecipeId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "advanced_materials");
    public static final TagKey<Block> Magnetized = BlockTags.create(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "magnetized"));

    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<MenuType<?>> MenuRegister = DeferredRegister.create(Registries.MENU, Scp_unleashed.ModId);
    public static final DeferredRegister<RecipeType<?>> RecipeTypeRegister = DeferredRegister.create(Registries.RECIPE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<RecipeSerializer<?>> RecipeSerializerRegister = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredBlock<AdvancedMaterialsBenchBlock> Bench = BlockRegister.register("advanced_materials_bench", () -> new AdvancedMaterialsBenchBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(4.0f, 8.0f).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK)
            .lightLevel(AdvancedMaterialsBenchBlock::lightOf)));
    public static final DeferredItem<BlockItem> BenchItem = ItemRegister.registerSimpleBlockItem("advanced_materials_bench", Bench);
    public static final DeferredBlock<MagnetizedBlock> MagnetizedSteel = BlockRegister.register("magnetized_block", () -> new MagnetizedBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY).requiresCorrectToolForDrops().strength(8.0f, 1200.0f).sound(SoundType.NETHERITE_BLOCK).pushReaction(PushReaction.BLOCK)));
    public static final DeferredItem<BlockItem> MagnetizedSteelItem = ItemRegister.registerSimpleBlockItem("magnetized_block", MagnetizedSteel);
    public static final DeferredBlock<MagnetizedDoorBlock> MagnetizedDoor = BlockRegister.register("magnetized_bulkhead_door", () -> new MagnetizedDoorBlock(doorProperties()));
    public static final DeferredItem<BlockItem> MagnetizedDoorItem = ItemRegister.register("magnetized_bulkhead_door",
            () -> new BlockItem(MagnetizedDoor.get(), new Item.Properties()));
    public static final DeferredBlock<MagnetizedTallDoorBlock> MagnetizedTallDoor = BlockRegister.register("magnetized_tall_bulkhead_door",
            () -> new MagnetizedTallDoorBlock(doorProperties()));
    public static final DeferredItem<BlockItem> MagnetizedTallDoorItem = ItemRegister.registerSimpleBlockItem("magnetized_tall_bulkhead_door", MagnetizedTallDoor);
    public static final DeferredBlock<MagnetizedSingleDoorBlock> MagnetizedSingleDoor = BlockRegister.register("magnetized_single_bulkhead_door",
            () -> new MagnetizedSingleDoorBlock(doorProperties()));
    public static final DeferredItem<BlockItem> MagnetizedSingleDoorItem = ItemRegister.registerSimpleBlockItem("magnetized_single_bulkhead_door", MagnetizedSingleDoor);
    public static final List<DeferredBlock<? extends MagnetizedDoorBlock>> MagnetizedDoors = List.of(MagnetizedDoor, MagnetizedTallDoor, MagnetizedSingleDoor);
    public static final List<DeferredItem<BlockItem>> MagnetizedDoorItems = List.of(MagnetizedDoorItem, MagnetizedTallDoorItem, MagnetizedSingleDoorItem);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AdvancedMaterialsBenchBlockEntity>> BenchEntity = BlockEntityRegister.register("advanced_materials_bench",
            () -> BlockEntityType.Builder.of(AdvancedMaterialsBenchBlockEntity::new, Bench.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MagnetizedDoorBlockEntity>> DoorEntity = BlockEntityRegister.register("magnetized_bulkhead_door",
            () -> BlockEntityType.Builder.of(MagnetizedDoorBlockEntity::new, MagnetizedDoors.stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<AdvancedMaterialsMenu>> BenchMenu = MenuRegister.register("advanced_materials_bench",
            () -> IMenuTypeExtension.create(AdvancedMaterialsMenu::new));
    public static final DeferredHolder<RecipeType<?>, RecipeType<AdvancedMaterialsRecipe>> RecipeKind = RecipeTypeRegister.register("advanced_materials",
            () -> RecipeType.simple(RecipeId));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AdvancedMaterialsRecipe>> RecipeSerializerHolder = RecipeSerializerRegister.register("advanced_materials",
            AdvancedMaterialsRecipe.Serializer::new);

    public static final DeferredHolder<SoundEvent, SoundEvent> BenchWork = sound("advanced_materials_bench_work");
    public static final DeferredHolder<SoundEvent, SoundEvent> BenchCharge = sound("advanced_materials_bench_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> BenchFinish = sound("advanced_materials_bench_finish");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoorClamp = sound("magnetized_bulkhead_door_clamp");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoorRelease = sound("magnetized_bulkhead_door_release");

    private AdvancedMaterialsRegistry() {
    }

    private static BlockBehaviour.Properties doorProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).requiresCorrectToolForDrops().strength(25.0f, 1200.0f).sound(SoundType.NETHERITE_BLOCK).noOcclusion()
                .dynamicShape().pushReaction(PushReaction.BLOCK).isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false)
                .isRedstoneConductor((State, World, Pos) -> false);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityRegister.register(ModEventBus);
        MenuRegister.register(ModEventBus);
        RecipeTypeRegister.register(ModEventBus);
        RecipeSerializerRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
    }
}
