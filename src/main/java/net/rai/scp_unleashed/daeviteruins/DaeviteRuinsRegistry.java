package net.rai.scp_unleashed.daeviteruins;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.daeviteruins.block.DaeviteRuinBannerBlock;
import net.rai.scp_unleashed.daeviteruins.block.DaeviteRuinBlockItem;
import net.rai.scp_unleashed.daeviteruins.block.DaeviteRuinBrazierBlock;
import net.rai.scp_unleashed.daeviteruins.block.DaeviteRuinFacingBlock;
import net.rai.scp_unleashed.daeviteruins.entity.DaeviteEmberBolt;
import net.rai.scp_unleashed.daeviteruins.entity.DaeviteFlare;
import net.rai.scp_unleashed.daeviteruins.entity.DaeviteThrownSpear;
import net.rai.scp_unleashed.daeviteruins.item.DaeviteRelicItem;
import net.rai.scp_unleashed.daeviteruins.item.DaeviteRobeItem;
import net.rai.scp_unleashed.daeviteruins.item.DaeviteSpearItem;
import net.rai.scp_unleashed.daeviteruins.item.DaeviteStaffItem;
import net.rai.scp_unleashed.daeviteruins.item.DaeviteSwordItem;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

public final class DaeviteRuinsRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ArmorMaterial> MaterialRegister = DeferredRegister.create(Registries.ARMOR_MATERIAL, Scp_unleashed.ModId);
    public static final List<DeferredItem<? extends Item>> DecorItems = new ArrayList<>();
    public static final List<DeferredItem<? extends Item>> CarriedItems = new ArrayList<>();

    public static final ResourceKey<DamageType> EmberDamage = damage("daevite_relic_ember");
    public static final ResourceKey<DamageType> WardDamage = damage("daevite_relic_ward");
    public static final ResourceKey<DamageType> SpearDamage = damage("daevite_relic_spear");

    public static final int RobeDurability = 9;
    public static final DeferredHolder<SoundEvent, SoundEvent> RobeEquip = sound("daevite_robe_equip");
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> RobeCloth = MaterialRegister.register("daevite_robe_cloth", () -> {
        EnumMap<ArmorItem.Type, Integer> Defense = new EnumMap<>(ArmorItem.Type.class);
        Defense.put(ArmorItem.Type.HELMET, 1);
        Defense.put(ArmorItem.Type.CHESTPLATE, 3);
        Defense.put(ArmorItem.Type.LEGGINGS, 2);
        Defense.put(ArmorItem.Type.BOOTS, 1);
        Defense.put(ArmorItem.Type.BODY, 3);
        return new ArmorMaterial(Defense, 15, RobeEquip, () -> Ingredient.of(Items.RED_WOOL),
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "daevite_robe_cloth"))), 0.0f, 0.0f);
    });

    public static final DeferredBlock<Block> Bricks = BlockRegister.register("daevite_ruin_bricks", () -> new Block(stone(MapColor.COLOR_RED, 2.0f)));
    public static final DeferredBlock<DaeviteRuinFacingBlock> ReliefHost = BlockRegister.register("daevite_ruin_relief_host",
            () -> new DaeviteRuinFacingBlock(stone(MapColor.COLOR_RED, 2.0f), DaeviteRuinFacingBlock.Full));
    public static final DeferredBlock<DaeviteRuinFacingBlock> ReliefGod = BlockRegister.register("daevite_ruin_relief_god",
            () -> new DaeviteRuinFacingBlock(stone(MapColor.COLOR_RED, 2.0f), DaeviteRuinFacingBlock.Full));
    public static final DeferredBlock<RotatedPillarBlock> Column = BlockRegister.register("daevite_ruin_column",
            () -> new RotatedPillarBlock(stone(MapColor.COLOR_ORANGE, 1.8f).sound(SoundType.STONE)));
    public static final DeferredBlock<DaeviteRuinBrazierBlock> Brazier = BlockRegister.register("daevite_ruin_brazier",
            () -> new DaeviteRuinBrazierBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(3.0f, 6.0f).requiresCorrectToolForDrops()
                    .sound(SoundType.COPPER).noOcclusion().lightLevel(State -> State.getValue(DaeviteRuinBrazierBlock.Lit) ? 14 : 0)));
    public static final DeferredBlock<DaeviteRuinBannerBlock> Banner = BlockRegister.register("daevite_ruin_banner",
            () -> new DaeviteRuinBannerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.0f).sound(SoundType.WOOL).noCollission()
                    .noOcclusion().ignitedByLava().pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<DaeviteRuinFacingBlock> IdolHead = BlockRegister.register("daevite_ruin_idol_head",
            () -> new DaeviteRuinFacingBlock(stone(MapColor.COLOR_ORANGE, 2.0f).noOcclusion(), DaeviteRuinFacingBlock.Head));
    public static final DeferredBlock<DaeviteRuinFacingBlock> IdolHand = BlockRegister.register("daevite_ruin_idol_hand",
            () -> new DaeviteRuinFacingBlock(stone(MapColor.COLOR_ORANGE, 2.0f).noOcclusion(), DaeviteRuinFacingBlock.Hand));
    public static final DeferredBlock<DaeviteRuinFacingBlock> WeaponRack = BlockRegister.register("daevite_ruin_weapon_rack",
            () -> new DaeviteRuinFacingBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.5f, 3.0f).sound(SoundType.WOOD).noOcclusion()
                    .ignitedByLava(), DaeviteRuinFacingBlock.Rack));
    public static final DeferredBlock<DaeviteRuinFacingBlock> Urn = BlockRegister.register("daevite_ruin_urn",
            () -> new DaeviteRuinFacingBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_RED).strength(1.2f, 3.0f).requiresCorrectToolForDrops()
                    .sound(SoundType.DECORATED_POT).noOcclusion(), DaeviteRuinFacingBlock.UrnShape));

    public static final DeferredItem<DaeviteRuinBlockItem> BricksItem = decor("daevite_ruin_bricks", Bricks, Rarity.UNCOMMON);
    public static final DeferredItem<DaeviteRuinBlockItem> ReliefHostItem = decor("daevite_ruin_relief_host", ReliefHost, Rarity.UNCOMMON);
    public static final DeferredItem<DaeviteRuinBlockItem> ReliefGodItem = decor("daevite_ruin_relief_god", ReliefGod, Rarity.RARE);
    public static final DeferredItem<DaeviteRuinBlockItem> ColumnItem = decor("daevite_ruin_column", Column, Rarity.UNCOMMON);
    public static final DeferredItem<DaeviteRuinBlockItem> BrazierItem = decor("daevite_ruin_brazier", Brazier, Rarity.UNCOMMON);
    public static final DeferredItem<DaeviteRuinBlockItem> BannerItem = decor("daevite_ruin_banner", Banner, Rarity.UNCOMMON);
    public static final DeferredItem<DaeviteRuinBlockItem> IdolHeadItem = decor("daevite_ruin_idol_head", IdolHead, Rarity.RARE);
    public static final DeferredItem<DaeviteRuinBlockItem> IdolHandItem = decor("daevite_ruin_idol_hand", IdolHand, Rarity.RARE);
    public static final DeferredItem<DaeviteRuinBlockItem> WeaponRackItem = decor("daevite_ruin_weapon_rack", WeaponRack, Rarity.UNCOMMON);
    public static final DeferredItem<DaeviteRuinBlockItem> UrnItem = decor("daevite_ruin_urn", Urn, Rarity.UNCOMMON);

    public static final DeferredItem<DaeviteStaffItem> EmberStaff = carried("daevite_relic_ember_staff",
            () -> new DaeviteStaffItem(DaeviteStaffItem.Spell.Ember, new Item.Properties().durability(DaeviteStaffItem.Spell.Ember.Uses).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<DaeviteStaffItem> FlareStaff = carried("daevite_relic_flare_staff",
            () -> new DaeviteStaffItem(DaeviteStaffItem.Spell.Flare, new Item.Properties().durability(DaeviteStaffItem.Spell.Flare.Uses).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<DaeviteStaffItem> BlinkStaff = carried("daevite_relic_blink_staff",
            () -> new DaeviteStaffItem(DaeviteStaffItem.Spell.Blink, new Item.Properties().durability(DaeviteStaffItem.Spell.Blink.Uses).rarity(Rarity.RARE)));
    public static final DeferredItem<DaeviteStaffItem> WardStaff = carried("daevite_relic_ward_staff",
            () -> new DaeviteStaffItem(DaeviteStaffItem.Spell.Ward, new Item.Properties().durability(DaeviteStaffItem.Spell.Ward.Uses).rarity(Rarity.RARE)));
    public static final DeferredItem<DaeviteSpearItem> Spear = carried("daevite_relic_spear",
            () -> new DaeviteSpearItem(new Item.Properties().durability(DaeviteSpearItem.Uses).rarity(Rarity.UNCOMMON).attributes(DaeviteSpearItem.createAttributes())));
    public static final DeferredItem<DaeviteSwordItem> Sword = carried("daevite_relic_sword",
            () -> new DaeviteSwordItem(new Item.Properties().rarity(Rarity.UNCOMMON).attributes(DaeviteSwordItem.createAttributes())));
    public static final DeferredItem<DaeviteRelicItem> BrassSeal = carried("daevite_relic_brass_seal",
            () -> new DaeviteRelicItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<DaeviteRelicItem> IdolFragment = carried("daevite_relic_idol_fragment",
            () -> new DaeviteRelicItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
    public static final DeferredItem<DaeviteRelicItem> ScorchedBanner = carried("daevite_relic_scorched_banner",
            () -> new DaeviteRelicItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<DaeviteRobeItem> Hood = carried("daevite_robe_hood",
            () -> new DaeviteRobeItem(ArmorItem.Type.HELMET, new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(RobeDurability)).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<DaeviteRobeItem> Vestment = carried("daevite_robe_vestment",
            () -> new DaeviteRobeItem(ArmorItem.Type.CHESTPLATE, new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(RobeDurability))
                    .rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<DaeviteRobeItem> Sash = carried("daevite_robe_sash",
            () -> new DaeviteRobeItem(ArmorItem.Type.LEGGINGS, new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(RobeDurability)).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<DaeviteRobeItem> Sandals = carried("daevite_robe_sandals",
            () -> new DaeviteRobeItem(ArmorItem.Type.BOOTS, new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(RobeDurability)).rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<EntityType<?>, EntityType<DaeviteEmberBolt>> EmberBolt = EntityTypeRegister.register("daevite_relic_ember_bolt",
            () -> EntityType.Builder.<DaeviteEmberBolt>of(DaeviteEmberBolt::new, MobCategory.MISC).sized(0.3f, 0.3f).clientTrackingRange(6).updateInterval(2)
                    .noSave().fireImmune().build("daevite_relic_ember_bolt"));
    public static final DeferredHolder<EntityType<?>, EntityType<DaeviteFlare>> Flare = EntityTypeRegister.register("daevite_relic_flare",
            () -> EntityType.Builder.<DaeviteFlare>of(DaeviteFlare::new, MobCategory.MISC).sized(0.3f, 0.3f).clientTrackingRange(8).updateInterval(2)
                    .noSave().fireImmune().build("daevite_relic_flare"));
    public static final DeferredHolder<EntityType<?>, EntityType<DaeviteThrownSpear>> ThrownSpear = EntityTypeRegister.register("daevite_relic_thrown_spear",
            () -> EntityType.Builder.<DaeviteThrownSpear>of(DaeviteThrownSpear::new, MobCategory.MISC).sized(0.5f, 0.5f).eyeHeight(0.13f).clientTrackingRange(4)
                    .updateInterval(20).build("daevite_relic_thrown_spear"));

    public static final DeferredHolder<SoundEvent, SoundEvent> StaffCast = sound("daevite_relic_staff_cast");
    public static final DeferredHolder<SoundEvent, SoundEvent> StaffFizzle = sound("daevite_relic_staff_fizzle");
    public static final DeferredHolder<SoundEvent, SoundEvent> FlareBurst = sound("daevite_relic_flare_burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> Blink = sound("daevite_relic_blink");
    public static final DeferredHolder<SoundEvent, SoundEvent> WardPulse = sound("daevite_relic_ward_pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> SpearThrow = sound("daevite_relic_spear_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> SpearHit = sound("daevite_relic_spear_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> RuinRumble = sound("daevite_ruin_rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> RuinWake = sound("daevite_ruin_wake");

    private DaeviteRuinsRegistry() {
    }

    private static ResourceKey<DamageType> damage(String Name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    private static BlockBehaviour.Properties stone(MapColor Colour, float Hardness) {
        return BlockBehaviour.Properties.of().mapColor(Colour).instrument(NoteBlockInstrument.BASEDRUM).strength(Hardness, 6.0f).requiresCorrectToolForDrops()
                .sound(SoundType.NETHER_BRICKS);
    }

    private static DeferredItem<DaeviteRuinBlockItem> decor(String Name, Supplier<? extends Block> Placed, Rarity Kind) {
        DeferredItem<DaeviteRuinBlockItem> Entry = ItemRegister.register(Name, () -> new DaeviteRuinBlockItem(Placed.get(), new Item.Properties().rarity(Kind)));
        DecorItems.add(Entry);
        return Entry;
    }

    private static <T extends Item> DeferredItem<T> carried(String Name, Supplier<T> Maker) {
        DeferredItem<T> Entry = ItemRegister.register(Name, Maker);
        CarriedItems.add(Entry);
        return Entry;
    }

    public static void register(IEventBus ModEventBus) {
        MaterialRegister.register(ModEventBus);
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
    }
}
