package net.rai.scp_unleashed.space;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.CountConfiguration;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class SpaceRegistry {
    public static final ResourceKey<Level> MoonLevel = ResourceKey.create(Registries.DIMENSION, location("moon"));
    public static final ResourceKey<DimensionType> MoonType = ResourceKey.create(Registries.DIMENSION_TYPE, location("moon"));
    public static final ResourceKey<DamageType> VacuumDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("vacuum_exposure"));
    public static final ResourceLocation MoonEffects = location("moon");

    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<MenuType<?>> MenuRegister = DeferredRegister.create(Registries.MENU, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ArmorMaterial> MaterialRegister = DeferredRegister.create(Registries.ARMOR_MATERIAL, Scp_unleashed.ModId);
    public static final DeferredRegister<Feature<?>> FeatureRegister = DeferredRegister.create(Registries.FEATURE, Scp_unleashed.ModId);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> Air = ComponentRegister.registerComponentType("space_air",
            Builder -> Builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SpaceSuitTank>> SuitTank = ComponentRegister.registerComponentType("space_suit_tank",
            Builder -> Builder.persistent(SpaceSuitTank.TankCodec).networkSynchronized(SpaceSuitTank.TankStreamCodec));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SuitMaterial = MaterialRegister.register("space_suit", () -> suitMaterial());

    public static final DeferredBlock<Block> LunarStone = BlockRegister.register("lunar_stone", () -> new Block(stone()));
    public static final DeferredBlock<SlabBlock> LunarStoneSlab = BlockRegister.register("lunar_stone_slab", () -> new SlabBlock(stone()));
    public static final DeferredBlock<StairBlock> LunarStoneStairs = BlockRegister.register("lunar_stone_stairs", () -> new StairBlock(LunarStone.get().defaultBlockState(), stone()));
    public static final DeferredBlock<WallBlock> LunarStoneWall = BlockRegister.register("lunar_stone_wall", () -> new WallBlock(stone().forceSolidOn()));
    public static final DeferredBlock<Block> PolishedLunarStone = BlockRegister.register("polished_lunar_stone", () -> new Block(stone()));
    public static final DeferredBlock<Block> LunarStoneBricks = BlockRegister.register("lunar_stone_bricks", () -> new Block(stone()));
    public static final DeferredBlock<SlabBlock> LunarStoneBrickSlab = BlockRegister.register("lunar_stone_brick_slab", () -> new SlabBlock(stone()));
    public static final DeferredBlock<StairBlock> LunarStoneBrickStairs = BlockRegister.register("lunar_stone_brick_stairs", () -> new StairBlock(LunarStoneBricks.get().defaultBlockState(), stone()));
    public static final DeferredBlock<WallBlock> LunarStoneBrickWall = BlockRegister.register("lunar_stone_brick_wall", () -> new WallBlock(stone().forceSolidOn()));
    public static final DeferredBlock<ColoredFallingBlock> LunarDust = BlockRegister.register("lunar_dust", () -> new ColoredFallingBlock(new ColorRGBA(0x8E8D8A), BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_LIGHT_GRAY).instrument(NoteBlockInstrument.SNARE).strength(0.5F).sound(SoundType.SAND)));
    public static final DeferredBlock<Block> LunarRegolith = BlockRegister.register("lunar_regolith", () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE).instrument(NoteBlockInstrument.SNARE).strength(0.7F).sound(SoundType.GRAVEL)));
    public static final DeferredBlock<AirInfuserBlock> AirInfuser = BlockRegister.register("air_infuser", () -> new AirInfuserBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.5F, 6.0F).sound(SoundType.METAL).noOcclusion()
            .lightLevel(State -> State.getValue(AirInfuserBlock.Active) ? AirInfuserBlock.ActiveLight : 0)));

    public static final DeferredItem<BlockItem> LunarStoneItem = ItemRegister.registerSimpleBlockItem("lunar_stone", LunarStone);
    public static final DeferredItem<BlockItem> LunarStoneSlabItem = ItemRegister.registerSimpleBlockItem("lunar_stone_slab", LunarStoneSlab);
    public static final DeferredItem<BlockItem> LunarStoneStairsItem = ItemRegister.registerSimpleBlockItem("lunar_stone_stairs", LunarStoneStairs);
    public static final DeferredItem<BlockItem> LunarStoneWallItem = ItemRegister.registerSimpleBlockItem("lunar_stone_wall", LunarStoneWall);
    public static final DeferredItem<BlockItem> PolishedLunarStoneItem = ItemRegister.registerSimpleBlockItem("polished_lunar_stone", PolishedLunarStone);
    public static final DeferredItem<BlockItem> LunarStoneBricksItem = ItemRegister.registerSimpleBlockItem("lunar_stone_bricks", LunarStoneBricks);
    public static final DeferredItem<BlockItem> LunarStoneBrickSlabItem = ItemRegister.registerSimpleBlockItem("lunar_stone_brick_slab", LunarStoneBrickSlab);
    public static final DeferredItem<BlockItem> LunarStoneBrickStairsItem = ItemRegister.registerSimpleBlockItem("lunar_stone_brick_stairs", LunarStoneBrickStairs);
    public static final DeferredItem<BlockItem> LunarStoneBrickWallItem = ItemRegister.registerSimpleBlockItem("lunar_stone_brick_wall", LunarStoneBrickWall);
    public static final DeferredItem<BlockItem> LunarDustItem = ItemRegister.registerSimpleBlockItem("lunar_dust", LunarDust);
    public static final DeferredItem<BlockItem> LunarRegolithItem = ItemRegister.registerSimpleBlockItem("lunar_regolith", LunarRegolith);
    public static final DeferredItem<BlockItem> AirInfuserItem = ItemRegister.registerSimpleBlockItem("air_infuser", AirInfuser);

    private static final Map<SpaceSuitPiece, DeferredItem<SpaceSuitItem>> Suit = new EnumMap<>(SpaceSuitPiece.class);
    public static final DeferredItem<SpaceSuitItem> SuitHelmet = suit(SpaceSuitPiece.Helmet);
    public static final DeferredItem<SpaceSuitItem> SuitChestplate = suit(SpaceSuitPiece.Chestplate);
    public static final DeferredItem<SpaceSuitItem> SuitLeggings = suit(SpaceSuitPiece.Leggings);
    public static final DeferredItem<SpaceSuitItem> SuitBoots = suit(SpaceSuitPiece.Boots);
    public static final DeferredItem<AirCanisterItem> AirCanister = ItemRegister.register("air_canister", () -> new AirCanisterItem(new Item.Properties()
            .stacksTo(AirCanisterItem.StackSize).component(Air.get(), 0)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AirInfuserBlockEntity>> AirInfuserEntity = BlockEntityRegister.register("air_infuser",
            () -> BlockEntityType.Builder.of(AirInfuserBlockEntity::new, AirInfuser.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<AirInfuserMenu>> AirInfuserMenuType = MenuRegister.register("air_infuser",
            () -> new MenuType<>(AirInfuserMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<Feature<?>, CraterFeature> Crater = FeatureRegister.register("moon_crater", () -> new CraterFeature(CraterConfiguration.ConfigCodec));
    public static final DeferredHolder<Feature<?>, BoulderFeature> Boulder = FeatureRegister.register("moon_boulder", () -> new BoulderFeature(CountConfiguration.CODEC));

    public static final DeferredHolder<SoundEvent, SoundEvent> AirBeep = sound("space_air_beep");
    public static final DeferredHolder<SoundEvent, SoundEvent> AirAlarm = sound("space_air_alarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> SuitRefill = sound("space_suit_refill");
    public static final DeferredHolder<SoundEvent, SoundEvent> SuitSeal = sound("space_suit_seal");
    public static final DeferredHolder<SoundEvent, SoundEvent> VacuumGasp = sound("space_vacuum_gasp");
    public static final DeferredHolder<SoundEvent, SoundEvent> FlameSnuff = sound("space_flame_snuff");
    public static final DeferredHolder<SoundEvent, SoundEvent> InfuserHum = sound("space_infuser_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> InfuserDone = sound("space_infuser_done");

    private SpaceRegistry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    public static DeferredItem<SpaceSuitItem> suitItem(SpaceSuitPiece Piece) {
        return Suit.get(Piece);
    }

    private static DeferredItem<SpaceSuitItem> suit(SpaceSuitPiece Piece) {
        DeferredItem<SpaceSuitItem> Entry = ItemRegister.register(Piece.getId(), () -> new SpaceSuitItem(Piece, new Item.Properties()
                .durability(Piece.getArmorType().getDurability(SpaceSuitPiece.DurabilityFactor)).rarity(Rarity.UNCOMMON)));
        Suit.put(Piece, Entry);
        return Entry;
    }

    private static BlockBehaviour.Properties stone() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
                .sound(SoundType.STONE);
    }

    private static ArmorMaterial suitMaterial() {
        EnumMap<ArmorItem.Type, Integer> Defense = new EnumMap<>(ArmorItem.Type.class);
        Defense.put(ArmorItem.Type.HELMET, 2);
        Defense.put(ArmorItem.Type.CHESTPLATE, 5);
        Defense.put(ArmorItem.Type.LEGGINGS, 4);
        Defense.put(ArmorItem.Type.BOOTS, 2);
        Defense.put(ArmorItem.Type.BODY, 5);
        Supplier<Ingredient> Repair = () -> Ingredient.of(SpaceTags.AluminumPlates);
        Holder<SoundEvent> Equip = SoundEvents.ARMOR_EQUIP_IRON;
        return new ArmorMaterial(Defense, 10, Equip, Repair, List.of(new ArmorMaterial.Layer(location("space_suit"))), 1.0F, 0.05F);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        MaterialRegister.register(ModEventBus);
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityRegister.register(ModEventBus);
        MenuRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
        FeatureRegister.register(ModEventBus);
    }
}
