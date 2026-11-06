package net.rai.scp_unleashed.scp096;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.StartConditions;

import java.util.EnumMap;
import java.util.List;

public final class Scp096Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ArmorMaterial> MaterialRegister = DeferredRegister.create(Registries.ARMOR_MATERIAL, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_096");
    public static final ResourceKey<DamageType> Caught = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp096_caught"));
    public static final TagKey<Block> RageProof = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp096_rage_proof"));
    public static final int GogglesDefense = 1;
    public static final int GogglesEnchantability = 10;
    public static final int GogglesDurabilityFactor = 18;

    public static final DeferredHolder<SoundEvent, SoundEvent> Step = sound("scp096_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> Pad = sound("scp096_pad");
    public static final DeferredHolder<SoundEvent, SoundEvent> RunStep = sound("scp096_run_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> RunThud = sound("scp096_run_thud");
    public static final DeferredHolder<SoundEvent, SoundEvent> Wade = sound("scp096_wade");
    public static final DeferredHolder<SoundEvent, SoundEvent> WadeSwish = sound("scp096_wade_swish");
    public static final DeferredHolder<SoundEvent, SoundEvent> Crying = sound("scp096_crying");
    public static final DeferredHolder<SoundEvent, SoundEvent> Triggered = sound("scp096_triggered");
    public static final DeferredHolder<SoundEvent, SoundEvent> TriggeredFrom50 = sound("scp096_triggered_50");
    public static final DeferredHolder<SoundEvent, SoundEvent> TriggeredFrom100 = sound("scp096_triggered_100");
    public static final DeferredHolder<SoundEvent, SoundEvent> TriggeredFrom150 = sound("scp096_triggered_150");
    public static final DeferredHolder<SoundEvent, SoundEvent> Enraged = sound("scp096_enraged");
    public static final DeferredHolder<SoundEvent, SoundEvent> Strike = sound("scp096_strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> StrikeImpact = sound("scp096_strike_impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> Tear = sound("scp096_tear");
    public static final DeferredHolder<SoundEvent, SoundEvent> TearCrack = sound("scp096_tear_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp096_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> Death = sound("scp096_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> BagOn = sound("scp096_bag_on");
    public static final DeferredHolder<SoundEvent, SoundEvent> BagOff = sound("scp096_bag_off");
    public static final DeferredHolder<SoundEvent, SoundEvent> BagTear = sound("scp096_bag_tear");
    public static final DeferredHolder<SoundEvent, SoundEvent> GogglesEquip = sound("scp096_goggles_equip");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StrikeLayers = List.of(Strike, StrikeImpact);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TearLayers = List.of(Tear, TearCrack);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> Scramble = MaterialRegister.register("scp096_scramble", () -> {
        EnumMap<ArmorItem.Type, Integer> Defense = new EnumMap<>(ArmorItem.Type.class);
        Defense.put(ArmorItem.Type.HELMET, GogglesDefense);
        Defense.put(ArmorItem.Type.CHESTPLATE, 0);
        Defense.put(ArmorItem.Type.LEGGINGS, 0);
        Defense.put(ArmorItem.Type.BOOTS, 0);
        Defense.put(ArmorItem.Type.BODY, 0);
        return new ArmorMaterial(Defense, GogglesEnchantability, GogglesEquip, () -> Ingredient.of(Items.TINTED_GLASS),
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scramble_goggles"))), 0.0f, 0.0f);
    });

    public static final DeferredHolder<EntityType<?>, EntityType<Scp096ShyGuy>> ShyGuy = EntityTypeRegister.register("scp096_shy_guy",
            () -> EntityType.Builder.<Scp096ShyGuy>of(Scp096ShyGuy::new, MobCategory.MONSTER)
                    .sized(Scp096ShyGuy.Width, Scp096ShyGuy.Height)
                    .eyeHeight(Scp096ShyGuy.EyeHeight)
                    .clientTrackingRange(12)
                    .updateInterval(2)
                    .build("scp096_shy_guy"));

    public static final DeferredItem<DeferredSpawnEggItem> SpawnEgg = ItemRegister.register("scp096_spawn_egg",
            () -> new DeferredSpawnEggItem(ShyGuy, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredItem<Scp096BagItem> Bag = ItemRegister.register("scp096_bag",
            () -> new Scp096BagItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Scp096GogglesItem> Goggles = ItemRegister.register("scramble_goggles",
            () -> new Scp096GogglesItem(Scramble, new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
                    .durability(ArmorItem.Type.HELMET.getDurability(GogglesDurabilityFactor))));

    private Scp096Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        MaterialRegister.register(ModEventBus);
        StartConditions.register(Scp096NightfallCondition.TypeName, Scp096NightfallCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp096NightfallCondition::manifest);
    }
}
