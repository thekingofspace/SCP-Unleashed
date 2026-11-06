package net.rai.scp_unleashed.scp3008.gear;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp3008.gear.block.Scp3008ShelfBlock;
import net.rai.scp_unleashed.scp3008.gear.block.Scp3008ShelfBlockEntity;
import net.rai.scp_unleashed.scp3008.gear.block.Scp3008TallShelfBlock;
import net.rai.scp_unleashed.scp3008.gear.carry.Scp3008Carried;
import net.rai.scp_unleashed.scp3008.gear.entity.Scp3008ThrownSpear;
import net.rai.scp_unleashed.scp3008.gear.item.Scp3008CrowbarItem;
import net.rai.scp_unleashed.scp3008.gear.item.Scp3008SalvageItem;
import net.rai.scp_unleashed.scp3008.gear.item.Scp3008ShieldItem;
import net.rai.scp_unleashed.scp3008.gear.item.Scp3008SpearItem;
import net.rai.scp_unleashed.scp3008.gear.loot.Scp3008SalvageModifier;

public final class Scp3008GearRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LootModifierRegister =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Scp_unleashed.ModId);

    public static final DeferredBlock<Scp3008ShelfBlock> ShelfShort = BlockRegister.register("scp3008_shelf_short",
            () -> new Scp3008ShelfBlock(shelf()));
    public static final DeferredBlock<Scp3008TallShelfBlock> ShelfTall = BlockRegister.register("scp3008_shelf_tall",
            () -> new Scp3008TallShelfBlock(shelf()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp3008ShelfBlockEntity>> Shelf = BlockEntityTypeRegister.register("scp3008_gear_shelf",
            () -> BlockEntityType.Builder.of(Scp3008ShelfBlockEntity::new, ShelfShort.get(), ShelfTall.get()).build(null));

    public static final DeferredItem<Scp3008CrowbarItem> Crowbar = ItemRegister.register("crowbar",
            () -> new Scp3008CrowbarItem(new Item.Properties().durability(Scp3008CrowbarItem.Uses).attributes(Scp3008CrowbarItem.createAttributes())));
    public static final DeferredItem<Scp3008SalvageItem> TableLeg = ItemRegister.register("scp3008_table_leg",
            () -> new Scp3008SalvageItem(new Item.Properties(), Scp3008SalvageItem.LegBurnTicks));
    public static final DeferredItem<Scp3008SalvageItem> CabinetDoor = ItemRegister.register("scp3008_cabinet_door",
            () -> new Scp3008SalvageItem(new Item.Properties().stacksTo(16), Scp3008SalvageItem.DoorBurnTicks));
    public static final DeferredItem<Scp3008SpearItem> Spear = ItemRegister.register("scp3008_table_leg_spear",
            () -> new Scp3008SpearItem(new Item.Properties().durability(Scp3008SpearItem.Uses).attributes(Scp3008SpearItem.createAttributes())));
    public static final DeferredItem<Scp3008ShieldItem> Shield = ItemRegister.register("scp3008_cabinet_door_shield",
            () -> new Scp3008ShieldItem(new Item.Properties().durability(Scp3008ShieldItem.Uses)));
    public static final DeferredItem<BlockItem> ShelfShortItem = ItemRegister.registerSimpleBlockItem("scp3008_shelf_short", ShelfShort);
    public static final DeferredItem<BlockItem> ShelfTallItem = ItemRegister.registerSimpleBlockItem("scp3008_shelf_tall", ShelfTall);

    public static final DeferredHolder<EntityType<?>, EntityType<Scp3008ThrownSpear>> ThrownSpear = EntityTypeRegister.register("scp3008_gear_thrown_spear",
            () -> EntityType.Builder.<Scp3008ThrownSpear>of(Scp3008ThrownSpear::new, MobCategory.MISC).sized(0.5f, 0.5f).eyeHeight(0.13f).clientTrackingRange(4)
                    .updateInterval(20).build("scp3008_gear_thrown_spear"));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Scp3008Carried>> Carry = AttachmentRegister.register("scp3008_gear_carry",
            () -> AttachmentType.builder(() -> Scp3008Carried.Empty).serialize(Scp3008Carried.CarriedCodec, Value -> !Value.isEmpty()).build());

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<Scp3008SalvageModifier>> Salvage = LootModifierRegister.register("scp3008_gear_salvage",
            () -> Scp3008SalvageModifier.ModifierCodec);

    public static final DeferredHolder<SoundEvent, SoundEvent> CrowbarPry = sound("scp3008_gear_crowbar_pry");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrowbarClank = sound("scp3008_gear_crowbar_clank");
    public static final DeferredHolder<SoundEvent, SoundEvent> CarrySetDown = sound("scp3008_gear_carry_set_down");
    public static final DeferredHolder<SoundEvent, SoundEvent> CarryTurn = sound("scp3008_gear_carry_turn");
    public static final DeferredHolder<SoundEvent, SoundEvent> CarryRefuse = sound("scp3008_gear_carry_refuse");
    public static final DeferredHolder<SoundEvent, SoundEvent> SpearThrow = sound("scp3008_gear_spear_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> SpearHit = sound("scp3008_gear_spear_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> SpearLand = sound("scp3008_gear_spear_land");
    public static final DeferredHolder<SoundEvent, SoundEvent> ShieldThunk = sound("scp3008_gear_shield_thunk");
    public static final DeferredHolder<SoundEvent, SoundEvent> ShelfOpen = sound("scp3008_gear_shelf_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> ShelfClose = sound("scp3008_gear_shelf_close");

    private Scp3008GearRegistry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    private static BlockBehaviour.Properties shelf() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(1.5f, 3.0f).sound(SoundType.WOOD).noOcclusion().ignitedByLava()
                .pushReaction(PushReaction.BLOCK);
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        LootModifierRegister.register(ModEventBus);
    }
}
