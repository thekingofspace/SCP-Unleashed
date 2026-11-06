package net.rai.scp_unleashed.materials;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MaterialsRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final float SteelHardness = 6.0f;
    public static final float SteelBlastResistance = 8.0f;

    private static final List<DeferredItem<? extends Item>> CreativeOrder = new ArrayList<>();

    public static final DeferredBlock<MaterialBlock> SteelBlock = BlockRegister.register("steel_block", () -> new MaterialBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops()
            .strength(SteelHardness, SteelBlastResistance).sound(SoundType.METAL)));

    public static final DeferredItem<MaterialItem> SteelBlend = material("steel_blend");
    public static final DeferredItem<MaterialItem> SteelIngot = material("steel_ingot");
    public static final DeferredItem<MaterialItem> SteelNugget = material("steel_nugget");
    public static final DeferredItem<BlockItem> SteelBlockItem = listed(ItemRegister.registerSimpleBlockItem("steel_block", SteelBlock));

    public static final DeferredItem<MaterialItem> IronPlate = material("iron_plate");
    public static final DeferredItem<MaterialItem> CopperPlate = material("copper_plate");
    public static final DeferredItem<MaterialItem> GoldPlate = material("gold_plate");
    public static final DeferredItem<MaterialItem> SteelPlate = material("steel_plate");

    public static final DeferredItem<MaterialItem> IronRod = material("iron_rod");
    public static final DeferredItem<MaterialItem> SteelRod = material("steel_rod");

    public static final DeferredItem<MaterialItem> CarbonDust = material("carbon_dust");
    public static final DeferredItem<MaterialItem> Silicon = material("silicon");

    public static final DeferredItem<MaterialItem> Rubber = material("rubber");
    public static final DeferredItem<MaterialItem> InsulationSheet = material("insulation_sheet");

    public static final DeferredItem<MaterialItem> CopperCoil = material("copper_coil");
    public static final DeferredItem<MaterialItem> HeatingElement = material("heating_element");
    public static final DeferredItem<MaterialItem> CircuitBoard = material("circuit_board");
    public static final DeferredItem<MaterialItem> Capacitor = material("capacitor");
    public static final DeferredItem<MaterialItem> ElectricMotor = material("electric_motor");
    public static final DeferredItem<MaterialItem> AdvancedCircuit = material("advanced_circuit");

    public static final DeferredItem<MaterialItem> EnergumAlloyIngot = material("energum_alloy_ingot");
    public static final DeferredItem<MaterialItem> SuperconductorCoil = material("superconductor_coil");

    private MaterialsRegistry() {
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
    }

    public static List<DeferredItem<? extends Item>> creativeOrder() {
        return Collections.unmodifiableList(CreativeOrder);
    }

    private static DeferredItem<MaterialItem> material(String Name) {
        return listed(ItemRegister.register(Name, () -> new MaterialItem(new Item.Properties())));
    }

    private static <T extends Item> DeferredItem<T> listed(DeferredItem<T> Entry) {
        CreativeOrder.add(Entry);
        return Entry;
    }
}
