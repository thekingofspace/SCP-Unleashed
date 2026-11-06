package net.rai.scp_unleashed.dclass.theodore;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.CloningBloodItem;
import net.rai.scp_unleashed.dclass.CloningKind;
import net.rai.scp_unleashed.dclass.CloningMachineBlockEntity;

import java.util.List;

public final class TheodoreRegistry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredHolder<SoundEvent, SoundEvent> Mutter = sound("theodore_mutter");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("theodore_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> Death = sound("theodore_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> Eat = sound("theodore_eat");
    public static final DeferredHolder<SoundEvent, SoundEvent> EatCrunch = sound("theodore_eat_crunch");
    public static final DeferredHolder<SoundEvent, SoundEvent> Split = sound("theodore_split");
    public static final DeferredHolder<SoundEvent, SoundEvent> SplitPop = sound("theodore_split_pop");
    public static final DeferredHolder<SoundEvent, SoundEvent> SplitWet = sound("theodore_split_wet");
    public static final DeferredHolder<SoundEvent, SoundEvent> Merge = sound("theodore_merge");
    public static final DeferredHolder<SoundEvent, SoundEvent> MergeRoar = sound("theodore_merge_roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> MergeGrunt = sound("theodore_merge_grunt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MassAmbient = sound("amalgamation_ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> MassHurt = sound("amalgamation_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MassDeath = sound("amalgamation_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> MassDeathWail = sound("amalgamation_death_wail");
    public static final DeferredHolder<SoundEvent, SoundEvent> MassStep = sound("amalgamation_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> MassStepSquelch = sound("amalgamation_step_squelch");
    public static final DeferredHolder<SoundEvent, SoundEvent> MassAttack = sound("amalgamation_attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> MassSlam = sound("amalgamation_slam");
    public static final DeferredHolder<SoundEvent, SoundEvent> MassSlamCrack = sound("amalgamation_slam_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> MassRoar = sound("amalgamation_roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> MassRoarLow = sound("amalgamation_roar_low");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> EatLayers = List.of(Eat, EatCrunch);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SplitLayers = List.of(Split, SplitPop, SplitWet);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> MergeLayers = List.of(Merge, MergeRoar, MergeGrunt);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> MassDeathLayers = List.of(MassDeath, MassDeathWail);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> MassStepLayers = List.of(MassStep, MassStepSquelch);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> MassSlamLayers = List.of(MassSlam, MassSlamCrack);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> MassRoarLayers = List.of(MassRoar, MassRoarLow);

    public static final DeferredHolder<EntityType<?>, EntityType<Theodore>> TheodoreType = EntityTypeRegister.register("theodore",
            () -> EntityType.Builder.<Theodore>of(Theodore::new, MobCategory.MISC).sized(0.6f, 1.8f).eyeHeight(1.62f).vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT).clientTrackingRange(10).build("theodore"));
    public static final DeferredHolder<EntityType<?>, EntityType<TheodoreAmalgamation>> AmalgamationType = EntityTypeRegister.register("theodore_amalgamation",
            () -> EntityType.Builder.<TheodoreAmalgamation>of(TheodoreAmalgamation::new, MobCategory.MONSTER).sized(1.9f, 3.0f).eyeHeight(2.55f)
                    .clientTrackingRange(12).build("theodore_amalgamation"));

    public static final DeferredItem<ArsenicItem> Arsenic = ItemRegister.register("arsenic",
            () -> new ArsenicItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));
    public static final DeferredItem<CloningBloodItem> TheodoreBlood = ItemRegister.register("theodore_blood",
            () -> new CloningBloodItem(CloningKind.TheodoreLine, new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));
    public static final DeferredItem<DeferredSpawnEggItem> TheodoreEgg = ItemRegister.register("theodore_spawn_egg",
            () -> new DeferredSpawnEggItem(TheodoreType, 0x2E3640, 0xC88E62, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> AmalgamationEgg = ItemRegister.register("theodore_amalgamation_spawn_egg",
            () -> new DeferredSpawnEggItem(AmalgamationType, 0x96603F, 0x5A1A1E, new Item.Properties()));
    public static final DeferredItem<DogTagsItem> DogTags = ItemRegister.register("theodore_dog_tags",
            () -> new DogTagsItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    private TheodoreRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        CloningMachineBlockEntity.registerHatch(CloningKind.TheodoreLine, new TheodoreHatch());
    }
}
