package net.rai.scp_unleashed.scp3008.econ;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp3008.econ.block.Scp3008LanternBlock;
import net.rai.scp_unleashed.scp3008.econ.block.Scp3008WorkbenchBlock;
import net.rai.scp_unleashed.scp3008.econ.item.Scp3008AllenKeyItem;
import net.rai.scp_unleashed.scp3008.econ.item.Scp3008DriverItem;
import net.rai.scp_unleashed.scp3008.econ.item.Scp3008HammerItem;
import net.rai.scp_unleashed.scp3008.econ.item.Scp3008LanternItem;
import net.rai.scp_unleashed.scp3008.econ.item.Scp3008MedicalItem;
import net.rai.scp_unleashed.scp3008.econ.item.Scp3008StoreArmorItem;
import net.rai.scp_unleashed.scp3008.econ.item.Scp3008StoreItem;
import net.rai.scp_unleashed.scp3008.item.Scp3008FoodItem;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

public final class Scp3008EconRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<ArmorMaterial> MaterialRegister = DeferredRegister.create(Registries.ARMOR_MATERIAL, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final List<DeferredItem<? extends Item>> Materials = new ArrayList<>();
    public static final List<DeferredItem<? extends Item>> Survival = new ArrayList<>();
    public static final List<DeferredItem<? extends Item>> Foods = new ArrayList<>();
    public static final List<DeferredItem<? extends Item>> Placeables = new ArrayList<>();
    public static final TagKey<Item> StoreMaterials = TagKey.create(Registries.ITEM, location("scp3008_econ/store_materials"));
    public static final TagKey<Item> DisassemblyTools = TagKey.create(Registries.ITEM, location("scp3008_econ/disassembly_tools"));

    public static final int PaddingDurability = 9;
    public static final int PaddingEnchantability = 12;

    public static final DeferredHolder<SoundEvent, SoundEvent> LanternClick = sound("scp3008_econ_lantern_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> LanternHum = sound("scp3008_econ_lantern_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> LanternBattery = sound("scp3008_econ_lantern_battery");
    public static final DeferredHolder<SoundEvent, SoundEvent> LanternSnap = sound("scp3008_econ_lantern_snap");
    public static final DeferredHolder<SoundEvent, SoundEvent> LanternFade = sound("scp3008_econ_lantern_fade");
    public static final DeferredHolder<SoundEvent, SoundEvent> BandageWrap = sound("scp3008_econ_bandage_wrap");
    public static final DeferredHolder<SoundEvent, SoundEvent> BandageTie = sound("scp3008_econ_bandage_tie");
    public static final DeferredHolder<SoundEvent, SoundEvent> KitOpen = sound("scp3008_econ_kit_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> PaddingEquip = sound("scp3008_econ_padding_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> ColanderEquip = sound("scp3008_econ_colander_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> Unscrew = sound("scp3008_econ_unscrew");
    public static final DeferredHolder<SoundEvent, SoundEvent> FlatPack = sound("scp3008_econ_flatpack");

    public static final DeferredItem<Scp3008StoreItem> Particleboard = material("scp3008_particleboard", 64);
    public static final DeferredItem<Scp3008StoreItem> SteelTube = material("scp3008_steel_tube", 64);
    public static final DeferredItem<Scp3008StoreItem> HexFittings = material("scp3008_hex_fittings", 64);
    public static final DeferredItem<Scp3008StoreItem> FabricScraps = material("scp3008_fabric_scraps", 64);
    public static final DeferredItem<Scp3008StoreItem> FoamFilling = material("scp3008_foam_filling", 64);
    public static final DeferredItem<Scp3008StoreItem> Cardboard = material("scp3008_cardboard", 64);
    public static final DeferredItem<Scp3008StoreItem> AcrylicSheet = material("scp3008_acrylic_sheet", 64);
    public static final DeferredItem<Scp3008StoreItem> LedBulb = material("scp3008_led_bulb", 16);
    public static final DeferredItem<Scp3008StoreItem> Battery = material("scp3008_battery", 16);
    public static final DeferredItem<Scp3008AllenKeyItem> AllenKey = listed(Materials, "scp3008_allen_key",
            () -> new Scp3008AllenKeyItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> Padding = MaterialRegister.register("scp3008_econ_padding", () -> {
        EnumMap<ArmorItem.Type, Integer> Defense = new EnumMap<>(ArmorItem.Type.class);
        Defense.put(ArmorItem.Type.HELMET, 2);
        Defense.put(ArmorItem.Type.CHESTPLATE, 4);
        Defense.put(ArmorItem.Type.LEGGINGS, 3);
        Defense.put(ArmorItem.Type.BOOTS, 1);
        Defense.put(ArmorItem.Type.BODY, 4);
        return new ArmorMaterial(Defense, PaddingEnchantability, PaddingEquip, () -> Ingredient.of(FabricScraps.get()),
                List.of(new ArmorMaterial.Layer(location("scp3008_econ_padding"))), 0.0f, 0.0f);
    });

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> Colander = MaterialRegister.register("scp3008_econ_colander", () -> {
        EnumMap<ArmorItem.Type, Integer> Defense = new EnumMap<>(ArmorItem.Type.class);
        Defense.put(ArmorItem.Type.HELMET, 2);
        Defense.put(ArmorItem.Type.CHESTPLATE, 4);
        Defense.put(ArmorItem.Type.LEGGINGS, 3);
        Defense.put(ArmorItem.Type.BOOTS, 1);
        Defense.put(ArmorItem.Type.BODY, 4);
        return new ArmorMaterial(Defense, PaddingEnchantability, ColanderEquip, () -> Ingredient.of(SteelTube.get()),
                List.of(new ArmorMaterial.Layer(location("scp3008_econ_colander"))), 0.0f, 0.0f);
    });

    public static final DeferredItem<Scp3008HammerItem> ClawHammer = listed(Survival, "scp3008_claw_hammer",
            () -> new Scp3008HammerItem(new Item.Properties().durability(Scp3008HammerItem.Uses).attributes(Scp3008HammerItem.createAttributes())
                    .component(DataComponents.TOOL, Scp3008HammerItem.createTool())));
    public static final DeferredItem<Scp3008MedicalItem> Bandage = listed(Survival, "scp3008_bandage",
            () -> new Scp3008MedicalItem(new Item.Properties().stacksTo(16), Scp3008MedicalItem.Kind.Bandage));
    public static final DeferredItem<Scp3008MedicalItem> FirstAidKit = listed(Survival, "scp3008_first_aid_kit",
            () -> new Scp3008MedicalItem(new Item.Properties().stacksTo(4), Scp3008MedicalItem.Kind.Kit));
    public static final DeferredItem<Scp3008StoreArmorItem> ColanderHelmet = armor("scp3008_colander_helmet", Colander, ArmorItem.Type.HELMET, "scp3008_econ_colander");
    public static final DeferredItem<Scp3008StoreArmorItem> CushionVest = armor("scp3008_cushion_vest", Padding, ArmorItem.Type.CHESTPLATE, "scp3008_econ_vest");
    public static final DeferredItem<Scp3008StoreArmorItem> CardboardGreaves = armor("scp3008_cardboard_greaves", Padding, ArmorItem.Type.LEGGINGS, null);
    public static final DeferredItem<Scp3008StoreArmorItem> FeltSlippers = armor("scp3008_felt_slippers", Padding, ArmorItem.Type.BOOTS, "scp3008_econ_slippers");
    public static final DeferredItem<Scp3008DriverItem> Screwdriver = listed(Survival, "scp3008_flatpack_screwdriver",
            () -> new Scp3008DriverItem(new Item.Properties().durability(Scp3008DriverItem.Uses).component(DataComponents.TOOL, Scp3008DriverItem.createTool())));

    public static final DeferredItem<Scp3008FoodItem> Crispbread = food("scp3008_crispbread", 16,
            () -> new FoodProperties.Builder().nutrition(3).saturationModifier(0.5f).build(), false);
    public static final DeferredItem<Scp3008FoodItem> CinnamonBun = food("scp3008_cinnamon_bun", 16,
            () -> new FoodProperties.Builder().nutrition(5).saturationModifier(0.5f).build(), false);
    public static final DeferredItem<Scp3008FoodItem> CannedHerring = food("scp3008_canned_herring", 16,
            () -> new FoodProperties.Builder().nutrition(6).saturationModifier(0.75f).build(), false);
    public static final DeferredItem<Scp3008FoodItem> ChocolateBar = food("scp3008_chocolate_bar", 16,
            () -> new FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).fast().effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0), 1.0f).build(), false);
    public static final DeferredItem<Scp3008FoodItem> ColdBrew = food("scp3008_cold_brew", 16,
            () -> new FoodProperties.Builder().nutrition(1).saturationModifier(0.2f).alwaysEdible().effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 1200, 0), 1.0f).build(), true);

    public static final DeferredBlock<Scp3008LanternBlock> Lantern = BlockRegister.register("scp3008_battery_lantern",
            () -> new Scp3008LanternBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.5f).sound(SoundType.LANTERN).noOcclusion()
                    .lightLevel(Scp3008LanternBlock::light).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<Scp3008WorkbenchBlock> Workbench = BlockRegister.register("scp3008_flatpack_workbench",
            () -> new Scp3008WorkbenchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(1.5f, 3.0f).sound(SoundType.WOOD).noOcclusion().ignitedByLava()));
    public static final DeferredItem<Scp3008LanternItem> LanternItem = listed(Placeables, "scp3008_battery_lantern",
            () -> new Scp3008LanternItem(Lantern.get(), new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Scp3008StoreItem.Placed> WorkbenchItem = listed(Placeables, "scp3008_flatpack_workbench",
            () -> new Scp3008StoreItem.Placed(Workbench.get(), new Item.Properties()));

    private Scp3008EconRegistry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static <T extends Item> DeferredItem<T> listed(List<DeferredItem<? extends Item>> Group, String Name, Supplier<T> Factory) {
        DeferredItem<T> Made = ItemRegister.register(Name, Factory);
        Group.add(Made);
        return Made;
    }

    private static DeferredItem<Scp3008StoreItem> material(String Name, int Stack) {
        return listed(Materials, Name, () -> new Scp3008StoreItem(new Item.Properties().stacksTo(Stack)));
    }

    private static DeferredItem<Scp3008StoreArmorItem> armor(String Name, DeferredHolder<ArmorMaterial, ArmorMaterial> Material, ArmorItem.Type Slot, String Texture) {
        ResourceLocation Worn = Texture == null ? null : location("textures/models/armor/" + Texture + ".png");
        return listed(Survival, Name, () -> new Scp3008StoreArmorItem(Material, Slot, new Item.Properties().durability(Slot.getDurability(PaddingDurability)), Worn));
    }

    private static DeferredItem<Scp3008FoodItem> food(String Name, int Stack, Supplier<FoodProperties> Food, boolean Drink) {
        return listed(Foods, Name, () -> new Scp3008FoodItem(new Item.Properties().stacksTo(Stack).food(Food.get()), Drink, "item.scp_unleashed." + Name + ".tooltip"));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        MaterialRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
    }
}
