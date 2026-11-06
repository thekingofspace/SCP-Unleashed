package net.rai.scp_unleashed.scp001day;

import com.mojang.serialization.MapCodec;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.gateway.GatewayDestinations;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.rai.scp_unleashed.scp001day.entity.Scp001DayFlesh;
import net.rai.scp_unleashed.scp001day.entity.Scp001DayForm;
import net.rai.scp_unleashed.scp001day.entity.Scp001DayRift;
import net.rai.scp_unleashed.scp001day.item.Scp001DayTinItem;
import net.rai.scp_unleashed.scp001day.item.Scp001DayWrapItem;
import net.rai.scp_unleashed.scp001day.world.Scp001DayChunkGenerator;

public final class Scp001DayRegistry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ArmorMaterial> MaterialRegister = DeferredRegister.create(Registries.ARMOR_MATERIAL, Scp_unleashed.ModId);
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> GeneratorRegister = DeferredRegister.create(Registries.CHUNK_GENERATOR, Scp_unleashed.ModId);
    public static final ResourceLocation DocumentId = location("scp_001_day");
    public static final ResourceKey<DamageType> SunDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp001_day_sun"));
    public static final int TinNutrition = 6;
    public static final float TinSaturation = 0.6f;
    public static final int HatDurability = 150;
    public static final int WrapsDurability = 220;
    public static final int LegDurability = 190;
    public static final int BootDurability = 130;

    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<Scp001DayChunkGenerator>> Generator = GeneratorRegister.register("scp001_day_city", () -> Scp001DayChunkGenerator.GeneratorCodec);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> WrapMaterial = MaterialRegister.register("scp001_day_sun_wrap", () -> {
        EnumMap<ArmorItem.Type, Integer> Defense = Util.make(new EnumMap<>(ArmorItem.Type.class), Map -> {
            Map.put(ArmorItem.Type.HELMET, 1);
            Map.put(ArmorItem.Type.CHESTPLATE, 2);
            Map.put(ArmorItem.Type.LEGGINGS, 2);
            Map.put(ArmorItem.Type.BOOTS, 1);
            Map.put(ArmorItem.Type.BODY, 2);
        });
        return new ArmorMaterial(Defense, 12, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.WHITE_WOOL), List.of(new ArmorMaterial.Layer(location("scp001_day_sun_wrap"))), 0.0f, 0.0f);
    });

    public static final DeferredItem<Scp001DayTinItem> Tin = ItemRegister.register("scp001_day_tin",
            () -> new Scp001DayTinItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON).food(new FoodProperties.Builder().nutrition(TinNutrition).saturationModifier(TinSaturation).build())));
    public static final DeferredItem<Scp001DayWrapItem> SunHat = ItemRegister.register("scp001_day_sun_hat",
            () -> new Scp001DayWrapItem(WrapMaterial, ArmorItem.Type.HELMET, new Item.Properties().durability(HatDurability), "scp001_day_sun_hat"));
    public static final DeferredItem<Scp001DayWrapItem> SunWraps = ItemRegister.register("scp001_day_sun_wraps",
            () -> new Scp001DayWrapItem(WrapMaterial, ArmorItem.Type.CHESTPLATE, new Item.Properties().durability(WrapsDurability), "scp001_day_sun_wraps"));
    public static final DeferredItem<Scp001DayWrapItem> SunLegWraps = ItemRegister.register("scp001_day_sun_leg_wraps",
            () -> new Scp001DayWrapItem(WrapMaterial, ArmorItem.Type.LEGGINGS, new Item.Properties().durability(LegDurability), "scp001_day_sun_leg_wraps"));
    public static final DeferredItem<Scp001DayWrapItem> SunBoots = ItemRegister.register("scp001_day_sun_boots",
            () -> new Scp001DayWrapItem(WrapMaterial, ArmorItem.Type.BOOTS, new Item.Properties().durability(BootDurability), "scp001_day_sun_boots"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp001DayRift>> RiftType = EntityTypeRegister.register("scp001_day_rift",
            () -> EntityType.Builder.<Scp001DayRift>of(Scp001DayRift::new, MobCategory.MISC).sized(Scp001DayRift.Width, Scp001DayRift.Height).clientTrackingRange(16).updateInterval(20).fireImmune().build("scp001_day_rift"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp001DayFlesh>> CrawlerType = flesh(Scp001DayForm.Crawler);
    public static final DeferredHolder<EntityType<?>, EntityType<Scp001DayFlesh>> ClingerType = flesh(Scp001DayForm.Clinger);
    public static final DeferredHolder<EntityType<?>, EntityType<Scp001DayFlesh>> LurkerType = flesh(Scp001DayForm.Lurker);
    public static final DeferredHolder<EntityType<?>, EntityType<Scp001DayFlesh>> MassType = flesh(Scp001DayForm.Mass);
    public static final DeferredItem<DeferredSpawnEggItem> CrawlerEgg = egg(Scp001DayForm.Crawler, CrawlerType);
    public static final DeferredItem<DeferredSpawnEggItem> ClingerEgg = egg(Scp001DayForm.Clinger, ClingerType);
    public static final DeferredItem<DeferredSpawnEggItem> LurkerEgg = egg(Scp001DayForm.Lurker, LurkerType);
    public static final DeferredItem<DeferredSpawnEggItem> MassEgg = egg(Scp001DayForm.Mass, MassType);

    public static final DeferredHolder<SoundEvent, SoundEvent> FleshAmbient = sound("scp001_day.flesh_ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> FleshHurt = sound("scp001_day.flesh_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> FleshDeath = sound("scp001_day.flesh_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> FleshGrab = sound("scp001_day.flesh_grab");
    public static final DeferredHolder<SoundEvent, SoundEvent> FleshRecoil = sound("scp001_day.flesh_recoil");
    public static final DeferredHolder<SoundEvent, SoundEvent> FleshFuse = sound("scp001_day.flesh_fuse");
    public static final DeferredHolder<SoundEvent, SoundEvent> FleshSplit = sound("scp001_day.flesh_split");
    public static final DeferredHolder<SoundEvent, SoundEvent> FleshMurmur = sound("scp001_day.flesh_murmur");
    public static final DeferredHolder<SoundEvent, SoundEvent> Burn = sound("scp001_day.burn");
    public static final DeferredHolder<SoundEvent, SoundEvent> BurnFlesh = sound("scp001_day.burn_flesh");
    public static final DeferredHolder<SoundEvent, SoundEvent> TinOpen = sound("scp001_day.tin_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> TinEmpty = sound("scp001_day.tin_empty");
    public static final DeferredHolder<SoundEvent, SoundEvent> RiftHum = sound("scp001_day.rift_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> RiftPass = sound("scp001_day.rift_pass");
    public static final DeferredHolder<SoundEvent, SoundEvent> WrapTear = sound("scp001_day.wrap_tear");
    public static final DeferredHolder<SoundEvent, SoundEvent> Crumble = sound("scp001_day.crumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> TurningStart = sound("scp001_day.turning_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> TurningWet = sound("scp001_day.turning_wet");
    public static final DeferredHolder<SoundEvent, SoundEvent> TurningGroan = sound("scp001_day.turning_groan");
    public static final DeferredHolder<SoundEvent, SoundEvent> TurningEnd = sound("scp001_day.turning_end");

    private Scp001DayRegistry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<EntityType<?>, EntityType<Scp001DayFlesh>> flesh(Scp001DayForm Form) {
        String Name = "scp001_day_" + Form.Key;
        return EntityTypeRegister.register(Name, () -> EntityType.Builder.<Scp001DayFlesh>of((Type, World) -> new Scp001DayFlesh(Type, World, Form), MobCategory.MONSTER)
                .sized(Form.Width, Form.Height).eyeHeight(Form.Height * 0.8f).clientTrackingRange(10).fireImmune().build(Name));
    }

    private static DeferredItem<DeferredSpawnEggItem> egg(Scp001DayForm Form, DeferredHolder<EntityType<?>, EntityType<Scp001DayFlesh>> Type) {
        return ItemRegister.register("scp001_day_" + Form.Key + "_spawn_egg", () -> new DeferredSpawnEggItem(Type, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        MaterialRegister.register(ModEventBus);
        GeneratorRegister.register(ModEventBus);
        if (GatewayDestinations.get(Scp001DayDestination.Id) == null) {
            GatewayDestinations.register(Scp001DayDestination.make());
        }
    }
}
