package net.rai.scp_unleashed.refinery;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.materials.MaterialBlock;
import net.rai.scp_unleashed.materials.MaterialItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RefineryRegistry {
    public static final ResourceLocation PurifyingId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "ore_purifying");
    public static final float OreHardness = 3.0f;
    public static final float DeepslateOreHardness = 4.5f;
    public static final float OreBlastResistance = 3.0f;

    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<MenuType<?>> MenuTypeRegister = DeferredRegister.create(Registries.MENU, Scp_unleashed.ModId);
    public static final DeferredRegister<RecipeType<?>> RecipeTypeRegister = DeferredRegister.create(Registries.RECIPE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<RecipeSerializer<?>> RecipeSerializerRegister = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    private static final List<DeferredItem<? extends Item>> ResourceOrder = new ArrayList<>();

    public static final DeferredBlock<OrePurifierBlock> OrePurifier = BlockRegister.register("ore_purifier", () -> new OrePurifierBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.5f, 6.0f).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK)
            .isRedstoneConductor((State, World, Pos) -> false).isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false)
            .isValidSpawn((State, World, Pos, Type) -> false).lightLevel(OrePurifierBlock::lightOf)));
    public static final DeferredItem<BlockItem> OrePurifierItem = ItemRegister.registerSimpleBlockItem("ore_purifier", OrePurifier);

    public static final DeferredBlock<MaterialBlock> BauxiteOre = BlockRegister.register("bauxite_ore", () -> new MaterialBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(OreHardness, OreBlastResistance)));
    public static final DeferredBlock<MaterialBlock> DeepslateBauxiteOre = BlockRegister.register("deepslate_bauxite_ore", () -> new MaterialBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(DeepslateOreHardness, OreBlastResistance)
            .sound(SoundType.DEEPSLATE)));

    public static final DeferredItem<BlockItem> BauxiteOreItem = listed(ItemRegister.registerSimpleBlockItem("bauxite_ore", BauxiteOre));
    public static final DeferredItem<BlockItem> DeepslateBauxiteOreItem = listed(ItemRegister.registerSimpleBlockItem("deepslate_bauxite_ore", DeepslateBauxiteOre));
    public static final DeferredItem<MaterialItem> RawBauxite = material("raw_bauxite", Rarity.COMMON);
    public static final DeferredItem<MaterialItem> IronDust = material("iron_dust", Rarity.COMMON);
    public static final DeferredItem<MaterialItem> CopperDust = material("copper_dust", Rarity.COMMON);
    public static final DeferredItem<MaterialItem> GoldDust = material("gold_dust", Rarity.COMMON);
    public static final DeferredItem<MaterialItem> AluminiumDust = material("aluminium_dust", Rarity.COMMON);
    public static final DeferredItem<MaterialItem> UraniumDust = material("uranium_dust", Rarity.COMMON);
    public static final DeferredItem<MaterialItem> ThoriumDust = material("thorium_dust", Rarity.COMMON);
    public static final DeferredItem<MaterialItem> AluminiumIngot = material("aluminium_ingot", Rarity.COMMON);
    public static final DeferredItem<MaterialItem> AluminiumPlate = material("aluminium_plate", Rarity.COMMON);
    public static final DeferredItem<MaterialItem> MeteorDust = material("meteor_dust", Rarity.UNCOMMON);
    public static final DeferredItem<MaterialItem> MeteoricPlate = material("meteoric_plate", Rarity.UNCOMMON);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OrePurifierBlockEntity>> OrePurifierEntity = BlockEntityTypeRegister.register("ore_purifier",
            () -> BlockEntityType.Builder.of(OrePurifierBlockEntity::new, OrePurifier.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<OrePurifierMenu>> OrePurifierMenuType = MenuTypeRegister.register("ore_purifier",
            () -> IMenuTypeExtension.create(OrePurifierMenu::new));
    public static final DeferredHolder<RecipeType<?>, RecipeType<PurifyingRecipe>> Purifying = RecipeTypeRegister.register("ore_purifying",
            () -> RecipeType.simple(PurifyingId));
    public static final DeferredHolder<RecipeSerializer<?>, PurifyingRecipe.Serializer> PurifyingSerializer = RecipeSerializerRegister.register("ore_purifying",
            PurifyingRecipe.Serializer::new);

    public static final DeferredHolder<SoundEvent, SoundEvent> PurifierChurn = sound("refinery_purifier_churn");
    public static final DeferredHolder<SoundEvent, SoundEvent> PurifierSlosh = sound("refinery_purifier_slosh");
    public static final DeferredHolder<SoundEvent, SoundEvent> PurifierGrind = sound("refinery_purifier_grind");
    public static final DeferredHolder<SoundEvent, SoundEvent> PurifierDrain = sound("refinery_purifier_drain");

    private RefineryRegistry() {
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

    public static List<DeferredItem<? extends Item>> resourceOrder() {
        return Collections.unmodifiableList(ResourceOrder);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    private static DeferredItem<MaterialItem> material(String Name, Rarity Grade) {
        return listed(ItemRegister.register(Name, () -> new MaterialItem(new Item.Properties().rarity(Grade))));
    }

    private static <T extends Item> DeferredItem<T> listed(DeferredItem<T> Entry) {
        ResourceOrder.add(Entry);
        return Entry;
    }
}
