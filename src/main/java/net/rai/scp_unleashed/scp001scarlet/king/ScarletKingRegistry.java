package net.rai.scp_unleashed.scp001scarlet.king;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import com.mojang.serialization.Codec;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunsRegistry;
import net.rai.scp_unleashed.scp001scarlet.king.loot.ScarletArmorItem;
import net.rai.scp_unleashed.scp001scarlet.king.loot.ScarletPodBlock;
import net.rai.scp_unleashed.scp001scarlet.king.loot.ScarletPodBlockEntity;
import net.rai.scp_unleashed.scp001scarlet.king.loot.ScarletRemainsBlock;
import net.rai.scp_unleashed.scp001scarlet.king.loot.ScarletRifleItem;

import java.util.EnumMap;
import java.util.List;

public final class ScarletKingRegistry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ArmorMaterial> MaterialRegister = DeferredRegister.create(Registries.ARMOR_MATERIAL, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);

    public static final int ArmorDurability = 45;
    public static final float ArmorToughness = 4.0f;
    public static final float ArmorKnockback = 0.15f;
    public static final int ArmorEnchantability = 18;

    public static final DeferredHolder<SoundEvent, SoundEvent> ChoirLow = sound("scarlet_king_choir_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChoirBell = sound("scarlet_king_choir_bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> Roar = sound("scarlet_king_roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> Growl = sound("scarlet_king_growl");
    public static final DeferredHolder<SoundEvent, SoundEvent> Chain = sound("scarlet_king_chain");
    public static final DeferredHolder<SoundEvent, SoundEvent> Slam = sound("scarlet_king_slam");
    public static final DeferredHolder<SoundEvent, SoundEvent> Impact = sound("scarlet_king_impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> Nova = sound("scarlet_king_nova");
    public static final DeferredHolder<SoundEvent, SoundEvent> Beam = sound("scarlet_king_beam");
    public static final DeferredHolder<SoundEvent, SoundEvent> Flame = sound("scarlet_king_flame");
    public static final DeferredHolder<SoundEvent, SoundEvent> Thorn = sound("scarlet_king_thorn");
    public static final DeferredHolder<SoundEvent, SoundEvent> Snap = sound("scarlet_king_snap");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scarlet_king_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> Death = sound("scarlet_king_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> End = sound("scarlet_king_end");
    public static final DeferredHolder<SoundEvent, SoundEvent> Whisper = sound("scarlet_king_whisper");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rumble = sound("scarlet_king_rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> Bind = sound("scarlet_king_bind");
    public static final DeferredHolder<SoundEvent, SoundEvent> BindChime = sound("scarlet_king_bind_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> RifleShot = sound("scarlet_rifle_shot");
    public static final DeferredHolder<SoundEvent, SoundEvent> RifleCrack = sound("scarlet_rifle_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> RifleRecharge = sound("scarlet_rifle_recharge");
    public static final DeferredHolder<SoundEvent, SoundEvent> RifleEmpty = sound("scarlet_rifle_empty");
    public static final DeferredHolder<SoundEvent, SoundEvent> RifleReady = sound("scarlet_rifle_ready");
    public static final DeferredHolder<SoundEvent, SoundEvent> ArmorEquip = sound("scarlet_armor_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> PodBeat = sound("scarlet_dormant_pod_beat");
    public static final DeferredHolder<SoundEvent, SoundEvent> PodHatch = sound("scarlet_dormant_pod_hatch");
    public static final DeferredHolder<SoundEvent, SoundEvent> ThroneCrack = sound("scarlet_king_throne_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> ThroneRise = sound("scarlet_king_throne_rise");
    public static final DeferredHolder<SoundEvent, SoundEvent> ThroneDone = sound("scarlet_king_throne_done");
    public static final DeferredHolder<SoundEvent, SoundEvent> DeathSlack = sound("scarlet_king_death_slack");
    public static final DeferredHolder<SoundEvent, SoundEvent> DeathKnee = sound("scarlet_king_death_knee");
    public static final DeferredHolder<SoundEvent, SoundEvent> DeathCrack = sound("scarlet_king_death_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> DeathAsh = sound("scarlet_king_death_ash");
    public static final DeferredHolder<SoundEvent, SoundEvent> DeathCollapse = sound("scarlet_king_death_collapse");
    public static final DeferredHolder<SoundEvent, SoundEvent> Kneel = sound("scarlet_king_kneel");
    public static final DeferredHolder<SoundEvent, SoundEvent> RiseUp = sound("scarlet_king_rise");
    public static final DeferredHolder<SoundEvent, SoundEvent> WardHit = sound("scarlet_king_ward_hit");

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> Scarlet = MaterialRegister.register("scarlet", () -> {
        EnumMap<ArmorItem.Type, Integer> Defense = new EnumMap<>(ArmorItem.Type.class);
        Defense.put(ArmorItem.Type.HELMET, 4);
        Defense.put(ArmorItem.Type.CHESTPLATE, 9);
        Defense.put(ArmorItem.Type.LEGGINGS, 7);
        Defense.put(ArmorItem.Type.BOOTS, 4);
        Defense.put(ArmorItem.Type.BODY, 13);
        return new ArmorMaterial(Defense, ArmorEnchantability, ArmorEquip, () -> Ingredient.EMPTY,
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scarlet"))), ArmorToughness, ArmorKnockback);
    });

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> RifleShotTick = ComponentRegister.registerComponentType("scarlet_rifle_shot",
            Builder -> Builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    public static final DeferredHolder<EntityType<?>, EntityType<ScarletKing>> KingType = EntityTypeRegister.register("scarlet_king",
            () -> EntityType.Builder.<ScarletKing>of(ScarletKing::new, MobCategory.MONSTER)
                    .sized(ScarletKing.Width, ScarletKing.Height)
                    .eyeHeight(ScarletKing.EyeHeight)
                    .fireImmune()
                    .clientTrackingRange(20)
                    .updateInterval(2)
                    .build("scarlet_king"));
    public static final DeferredHolder<EntityType<?>, EntityType<ScarletThorn>> ThornType = EntityTypeRegister.register("scarlet_king_thorn",
            () -> EntityType.Builder.<ScarletThorn>of(ScarletThorn::new, MobCategory.MISC)
                    .sized(ScarletThorn.Width, ScarletThorn.Height)
                    .fireImmune()
                    .noSave()
                    .clientTrackingRange(12)
                    .updateInterval(4)
                    .build("scarlet_king_thorn"));

    public static final DeferredItem<ScarletArmorItem> Helmet = armor("scarlet_armor_helmet", ArmorItem.Type.HELMET);
    public static final DeferredItem<ScarletArmorItem> Chestplate = armor("scarlet_armor_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<ScarletArmorItem> Leggings = armor("scarlet_armor_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<ScarletArmorItem> Boots = armor("scarlet_armor_boots", ArmorItem.Type.BOOTS);

    public static final DeferredItem<GunItem> Rifle = ItemRegister.register("scarlet_rifle", () -> new ScarletRifleItem(new Item.Properties().stacksTo(1)
            .rarity(Rarity.EPIC).fireResistant().component(GunsRegistry.LoadedAmmo.get(), ScarletRifleItem.Capacity)));

    public static final DeferredBlock<ScarletPodBlock> Pod = BlockRegister.register("scarlet_dormant_pod",
            () -> new ScarletPodBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.8f, 6.0f).sound(SoundType.HONEY_BLOCK)
                    .noOcclusion().pushReaction(PushReaction.DESTROY).lightLevel(State -> 3 + 2 * State.getValue(ScarletPodBlock.Stage))
                    .isValidSpawn((State, Getter, Pos, Type) -> false).isRedstoneConductor((State, Getter, Pos) -> false)
                    .isSuffocating((State, Getter, Pos) -> false).isViewBlocking((State, Getter, Pos) -> false)));
    public static final DeferredItem<BlockItem> PodItem = ItemRegister.register("scarlet_dormant_pod",
            () -> new BlockItem(Pod.get(), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ScarletPodBlockEntity>> PodEntity = BlockEntityRegister.register("scarlet_dormant_pod",
            () -> BlockEntityType.Builder.of(ScarletPodBlockEntity::new, Pod.get()).build(null));

    public static final DeferredBlock<ScarletRemainsBlock> Remains = BlockRegister.register("scarlet_king_remains",
            () -> new ScarletRemainsBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(2.0f, 1200.0f).sound(SoundType.BONE_BLOCK)
                    .noOcclusion().lightLevel(State -> 4).isValidSpawn((State, Getter, Pos, Type) -> false)));
    public static final DeferredItem<BlockItem> RemainsItem = ItemRegister.register("scarlet_king_remains",
            () -> new BlockItem(Remains.get(), new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    static {
        GunsRegistry.adoptGun(GunKind.ScarletRifle, Rifle);
    }

    private ScarletKingRegistry() {
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockRegister.register(ModEventBus);
        BlockEntityRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
        MaterialRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredItem<ScarletArmorItem> armor(String Name, ArmorItem.Type Slot) {
        return ItemRegister.register(Name, () -> new ScarletArmorItem(Slot, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()
                .durability(Slot.getDurability(ArmorDurability))));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }
}
