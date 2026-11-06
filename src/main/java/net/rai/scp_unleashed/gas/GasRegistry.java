package net.rai.scp_unleashed.gas;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.steam.SteamReleases;

public final class GasRegistry {
    public static final ResourceKey<Registry<Gas>> GasKey = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "gas"));
    public static final ResourceLocation VaporizingId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "vaporizing");

    public static final DeferredRegister<Gas> GasRegister = DeferredRegister.create(GasKey, Scp_unleashed.ModId);
    public static final Registry<Gas> Gases = GasRegister.makeRegistry(Builder -> Builder.sync(true));
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<MenuType<?>> MenuRegister = DeferredRegister.create(Registries.MENU, Scp_unleashed.ModId);
    public static final DeferredRegister<RecipeType<?>> RecipeTypeRegister = DeferredRegister.create(Registries.RECIPE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<RecipeSerializer<?>> RecipeSerializerRegister = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredHolder<Gas, Gas> Steam = GasRegister.register("steam", () -> new Gas(0xFFE2E8EE, false, GasReleases::steam));
    public static final DeferredHolder<Gas, Gas> ToxicGas = GasRegister.register("toxic_gas", () -> new Gas(0xFF8DB43C, true, GasReleases::toxic));
    public static final DeferredHolder<Gas, Gas> LowPressureSteam = GasRegister.register("low_pressure_steam", () -> new Gas(0xFFC2D6E4, false, SteamReleases::lowPressure));

    public static final DeferredBlock<GasTankBlock> GasTank = BlockRegister.register("gas_tank", () -> new GasTankBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.5f, 6.0f).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredItem<BlockItem> GasTankItem = ItemRegister.registerSimpleBlockItem("gas_tank", GasTank);
    public static final DeferredBlock<VaporizerBlock> Vaporizer = BlockRegister.register("liquid_vaporizer", () -> new VaporizerBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.5f, 6.0f).sound(SoundType.METAL)
            .lightLevel(State -> State.getValue(VaporizerBlock.Active) ? VaporizerBlock.ActiveLight : 0)));
    public static final DeferredItem<BlockItem> VaporizerItem = ItemRegister.registerSimpleBlockItem("liquid_vaporizer", Vaporizer);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GasTankBlockEntity>> GasTankEntity = BlockEntityRegister.register("gas_tank",
            () -> BlockEntityType.Builder.of(GasTankBlockEntity::new, GasTank.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VaporizerBlockEntity>> VaporizerEntity = BlockEntityRegister.register("liquid_vaporizer",
            () -> BlockEntityType.Builder.of(VaporizerBlockEntity::new, Vaporizer.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<VaporizerMenu>> VaporizerMenuType = MenuRegister.register("liquid_vaporizer",
            () -> new MenuType<>(VaporizerMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<RecipeType<?>, RecipeType<VaporizingRecipe>> Vaporizing = RecipeTypeRegister.register("vaporizing",
            () -> RecipeType.simple(VaporizingId));
    public static final DeferredHolder<RecipeSerializer<?>, VaporizingRecipe.Serializer> VaporizingSerializer = RecipeSerializerRegister.register("vaporizing",
            VaporizingRecipe.Serializer::new);

    public static final DeferredHolder<SoundEvent, SoundEvent> VaporizerBoil = sound("vaporizer_boil");

    private GasRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        GasRegister.register(ModEventBus);
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityRegister.register(ModEventBus);
        MenuRegister.register(ModEventBus);
        RecipeTypeRegister.register(ModEventBus);
        RecipeSerializerRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
    }
}
