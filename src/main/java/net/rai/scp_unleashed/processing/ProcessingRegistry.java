package net.rai.scp_unleashed.processing;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
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

public final class ProcessingRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<MenuType<?>> MenuTypeRegister = DeferredRegister.create(Registries.MENU, Scp_unleashed.ModId);
    public static final DeferredRegister<RecipeType<?>> RecipeTypeRegister = DeferredRegister.create(Registries.RECIPE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<RecipeSerializer<?>> RecipeSerializerRegister = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredBlock<ElectricFurnaceBlock> ElectricFurnace = BlockRegister.register("electric_furnace", () -> new ElectricFurnaceBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.5f, 6.0f).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK)
            .lightLevel(State -> State.getValue(ProcessingMachineBlock.Active) ? ElectricFurnaceBlock.ActiveLight : 0)));
    public static final DeferredBlock<ItemPressBlock> ItemPress = BlockRegister.register("item_press", () -> new ItemPressBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.5f, 6.0f).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK)));

    public static final DeferredItem<BlockItem> ElectricFurnaceItem = ItemRegister.registerSimpleBlockItem("electric_furnace", ElectricFurnace);
    public static final DeferredItem<BlockItem> ItemPressItem = ItemRegister.registerSimpleBlockItem("item_press", ItemPress);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricFurnaceBlockEntity>> ElectricFurnaceEntity = BlockEntityTypeRegister.register("electric_furnace",
            () -> BlockEntityType.Builder.of(ElectricFurnaceBlockEntity::new, ElectricFurnace.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ItemPressBlockEntity>> ItemPressEntity = BlockEntityTypeRegister.register("item_press",
            () -> BlockEntityType.Builder.of(ItemPressBlockEntity::new, ItemPress.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<ElectricFurnaceMenu>> ElectricFurnaceMenuType = MenuTypeRegister.register("electric_furnace",
            () -> new MenuType<>(ElectricFurnaceMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<ItemPressMenu>> ItemPressMenuType = MenuTypeRegister.register("item_press",
            () -> new MenuType<>(ItemPressMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<RecipeType<?>, RecipeType<ElectricSmeltingRecipe>> ElectricSmeltingType = RecipeTypeRegister.register("electric_smelting",
            () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "electric_smelting")));
    public static final DeferredHolder<RecipeType<?>, RecipeType<PressingRecipe>> PressingType = RecipeTypeRegister.register("pressing",
            () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "pressing")));

    public static final DeferredHolder<RecipeSerializer<?>, ElectricSmeltingRecipe.Serializer> ElectricSmeltingSerializer = RecipeSerializerRegister.register("electric_smelting",
            ElectricSmeltingRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, PressingRecipe.Serializer> PressingSerializer = RecipeSerializerRegister.register("pressing",
            PressingRecipe.Serializer::new);

    public static final DeferredHolder<SoundEvent, SoundEvent> FurnaceHum = sound("electric_furnace_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> PressThunk = sound("item_press_thunk");

    private ProcessingRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        MenuTypeRegister.register(ModEventBus);
        RecipeTypeRegister.register(ModEventBus);
        RecipeSerializerRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
    }
}
