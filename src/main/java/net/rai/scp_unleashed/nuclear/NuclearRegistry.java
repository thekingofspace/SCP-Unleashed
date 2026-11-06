package net.rai.scp_unleashed.nuclear;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.materials.MaterialBlock;
import net.rai.scp_unleashed.materials.MaterialItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NuclearRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final float OreHardness = 4.5f;
    public static final float OreBlastResistance = 3.0f;
    public static final int MaterialStackSize = 64;
    public static final int RodStackSize = 16;

    private static final List<DeferredItem<? extends Item>> CreativeOrder = new ArrayList<>();

    public static final DeferredBlock<MaterialBlock> DeepslateUraniumOre = BlockRegister.register("deepslate_uranium_ore", () -> new MaterialBlock(oreProperties()));
    public static final DeferredBlock<MaterialBlock> DeepslateThoriumOre = BlockRegister.register("deepslate_thorium_ore", () -> new MaterialBlock(oreProperties()));

    public static final DeferredItem<BlockItem> DeepslateUraniumOreItem = listed(ItemRegister.registerSimpleBlockItem("deepslate_uranium_ore", DeepslateUraniumOre));
    public static final DeferredItem<BlockItem> DeepslateThoriumOreItem = listed(ItemRegister.registerSimpleBlockItem("deepslate_thorium_ore", DeepslateThoriumOre));
    public static final DeferredItem<MaterialItem> RawUranium = material("raw_uranium", MaterialStackSize, Rarity.COMMON);
    public static final DeferredItem<MaterialItem> RawThorium = material("raw_thorium", MaterialStackSize, Rarity.COMMON);
    public static final DeferredItem<MaterialItem> UraniumIngot = material("uranium_ingot", MaterialStackSize, Rarity.COMMON);
    public static final DeferredItem<MaterialItem> ThoriumIngot = material("thorium_ingot", MaterialStackSize, Rarity.COMMON);
    public static final DeferredItem<MaterialItem> UraniumFuelRod = material("uranium_fuel_rod", RodStackSize, Rarity.UNCOMMON);
    public static final DeferredItem<MaterialItem> ThoriumFuelRod = material("thorium_fuel_rod", RodStackSize, Rarity.UNCOMMON);
    public static final DeferredItem<MaterialItem> DepletedFuelRod = material("depleted_fuel_rod", RodStackSize, Rarity.COMMON);
    public static final DeferredItem<MaterialItem> ReactiveMaterial = material("reactive_material", MaterialStackSize, Rarity.RARE);

    private NuclearRegistry() {
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
    }

    public static List<DeferredItem<? extends Item>> creativeOrder() {
        return Collections.unmodifiableList(CreativeOrder);
    }

    private static BlockBehaviour.Properties oreProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(OreHardness, OreBlastResistance).sound(SoundType.DEEPSLATE);
    }

    private static DeferredItem<MaterialItem> material(String Name, int StackSize, Rarity Grade) {
        return listed(ItemRegister.register(Name, () -> new MaterialItem(new Item.Properties().stacksTo(StackSize).rarity(Grade))));
    }

    private static <T extends Item> DeferredItem<T> listed(DeferredItem<T> Entry) {
        CreativeOrder.add(Entry);
        return Entry;
    }
}
