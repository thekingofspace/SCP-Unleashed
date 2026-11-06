package net.rai.scp_unleashed.scp148;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.EnumMap;
import java.util.List;

public final class Scp148Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ArmorMaterial> MaterialRegister = DeferredRegister.create(Registries.ARMOR_MATERIAL, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_148");
    public static final int DeliveryIngots = 27;
    public static final int HelmetDefense = 2;
    public static final float HelmetToughness = 0.0f;
    public static final float HelmetSteadiness = 0.0f;
    public static final int HelmetEnchantability = 9;
    public static final int HelmetDurabilityFactor = 37;
    public static final float BlockHardness = 5.0f;
    public static final float BlockBlastResistance = 1200.0f;

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> Alloy = MaterialRegister.register("scp148_alloy", () -> {
        EnumMap<ArmorItem.Type, Integer> Defense = new EnumMap<>(ArmorItem.Type.class);
        Defense.put(ArmorItem.Type.HELMET, HelmetDefense);
        Defense.put(ArmorItem.Type.CHESTPLATE, 0);
        Defense.put(ArmorItem.Type.LEGGINGS, 0);
        Defense.put(ArmorItem.Type.BOOTS, 0);
        Defense.put(ArmorItem.Type.BODY, 0);
        return new ArmorMaterial(Defense, HelmetEnchantability, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(Scp148Registry.Ingot.get()),
                List.of(new ArmorMaterial.Layer(location("scp148_helmet"))), HelmetToughness, HelmetSteadiness);
    });

    public static final DeferredItem<Scp148AlloyItem> Ingot = ItemRegister.register("scp148_ingot",
            () -> new Scp148AlloyItem(new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredBlock<Scp148Block> Ward = BlockRegister.register("scp148_block",
            () -> new Scp148Block(BlockBehaviour.Properties.of().mapColor(MapColor.EMERALD).instrument(NoteBlockInstrument.CHIME).requiresCorrectToolForDrops()
                    .strength(BlockHardness, BlockBlastResistance).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> WardItem = ItemRegister.register("scp148_block",
            () -> new BlockItem(Ward.get(), new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<Scp148HelmetItem> Helmet = ItemRegister.register("scp148_helmet",
            () -> new Scp148HelmetItem(Alloy, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()
                    .durability(ArmorItem.Type.HELMET.getDurability(HelmetDurabilityFactor))));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp148BlockEntity>> WardEntity = BlockEntityTypeRegister.register("scp148_block",
            () -> BlockEntityType.Builder.of(Scp148BlockEntity::new, Ward.get()).build(null));

    public static final DeferredHolder<SoundEvent, SoundEvent> Hum = sound("scp148_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> HumRing = sound("scp148_hum_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> Chime = sound("scp148_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChimeShimmer = sound("scp148_chime_shimmer");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HumLayers = List.of(Hum, HumRing);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ChimeLayers = List.of(Chime, ChimeShimmer);

    private Scp148Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        MaterialRegister.register(ModEventBus);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
