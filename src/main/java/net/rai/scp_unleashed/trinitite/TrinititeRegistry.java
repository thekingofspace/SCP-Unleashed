package net.rai.scp_unleashed.trinitite;

import net.minecraft.util.valueproviders.UniformInt;
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
import net.rai.scp_unleashed.materials.MaterialItem;
import net.rai.scp_unleashed.wire.ChargingRedstone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TrinititeRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final float OreHardness = 3.0f;
    public static final float DeepslateOreHardness = 4.5f;
    public static final float OreBlastResistance = 3.0f;
    public static final float BlockHardness = 5.0f;
    public static final float BlockBlastResistance = 6.0f;
    public static final int OreMinExperience = 1;
    public static final int OreMaxExperience = 4;
    public static final int ChargeCost = 150;
    public static final int ChargedLight = 6;

    private static final List<DeferredItem<? extends Item>> ResourceOrder = new ArrayList<>();

    public static final DeferredBlock<TrinititeOreBlock> TrinititeOre = BlockRegister.register("trinitite_ore", () -> new TrinititeOreBlock(
            UniformInt.of(OreMinExperience, OreMaxExperience), BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops().strength(OreHardness, OreBlastResistance)));
    public static final DeferredBlock<TrinititeOreBlock> DeepslateTrinititeOre = BlockRegister.register("deepslate_trinitite_ore", () -> new TrinititeOreBlock(
            UniformInt.of(OreMinExperience, OreMaxExperience), BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops().strength(DeepslateOreHardness, OreBlastResistance).sound(SoundType.DEEPSLATE)));
    public static final DeferredBlock<TrinititeBlock> Trinitite = BlockRegister.register("trinitite_block", () -> new TrinititeBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GREEN).requiresCorrectToolForDrops().strength(BlockHardness, BlockBlastResistance).sound(SoundType.AMETHYST)));
    public static final DeferredBlock<ChargedTrinititeBlock> ChargedTrinitite = BlockRegister.register("charged_trinitite_block", () -> new ChargedTrinititeBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.GLOW_LICHEN).requiresCorrectToolForDrops().strength(BlockHardness, BlockBlastResistance)
                    .sound(SoundType.AMETHYST).lightLevel(State -> ChargedLight)));

    public static final DeferredItem<BlockItem> TrinititeOreItem = listed(ItemRegister.registerSimpleBlockItem("trinitite_ore", TrinititeOre));
    public static final DeferredItem<BlockItem> DeepslateTrinititeOreItem = listed(ItemRegister.registerSimpleBlockItem("deepslate_trinitite_ore", DeepslateTrinititeOre));
    public static final DeferredItem<MaterialItem> TrinititeShard = listed(ItemRegister.register("trinitite_shard", () -> new MaterialItem(new Item.Properties())));
    public static final DeferredItem<BlockItem> TrinititeItem = listed(ItemRegister.registerSimpleBlockItem("trinitite_block", Trinitite));
    public static final DeferredItem<BlockItem> ChargedTrinititeItem = listed(ItemRegister.registerSimpleBlockItem("charged_trinitite_block", ChargedTrinitite));

    public static final ChargingRedstone.Charge Charging = ChargingRedstone.register(Trinitite, ChargedTrinitite, ChargeCost);

    private TrinititeRegistry() {
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
    }

    public static List<DeferredItem<? extends Item>> resourceOrder() {
        return Collections.unmodifiableList(ResourceOrder);
    }

    private static <T extends Item> DeferredItem<T> listed(DeferredItem<T> Entry) {
        ResourceOrder.add(Entry);
        return Entry;
    }
}
