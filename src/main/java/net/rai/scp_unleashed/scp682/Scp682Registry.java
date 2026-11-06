package net.rai.scp_unleashed.scp682;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.StartConditions;
import net.rai.scp_unleashed.scp076.Scp076Abel;
import net.rai.scp_unleashed.scp173.Scp173Hunt;
import net.rai.scp_unleashed.scp173.Scp173Sight;
import net.rai.scp_unleashed.scp407.Scp407Titan;
import net.rai.scp_unleashed.scp457.Scp457Goals;
import net.rai.scp_unleashed.stasis.StasisPods;

import java.util.List;

public final class Scp682Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_682");
    public static final ResourceKey<DamageType> CommandDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp682_command"));
    public static final ResourceKey<DamageType> AcidDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("sulfuric_acid"));
    public static final TagKey<Fluid> AcidFluid = TagKey.create(Registries.FLUID, location("sulfuric_acid"));
    public static final TagKey<EntityType<?>> Rivals = TagKey.create(Registries.ENTITY_TYPE, location("scp682_rivals"));
    public static final TagKey<EntityType<?>> Wary = TagKey.create(Registries.ENTITY_TYPE, location("scp682_wary"));
    public static final TagKey<EntityType<?>> Ignored = TagKey.create(Registries.ENTITY_TYPE, location("scp682_ignored"));
    public static final TagKey<EntityType<?>> Soothing = TagKey.create(Registries.ENTITY_TYPE, location("scp682_soothing"));
    public static final TagKey<EntityType<?>> Unseen = TagKey.create(Registries.ENTITY_TYPE, location("scp682_unseen"));
    public static final TagKey<EntityType<?>> PlagueCarriers = TagKey.create(Registries.ENTITY_TYPE, location("scp682_plague_carriers"));
    public static final TagKey<EntityType<?>> Predators = TagKey.create(Registries.ENTITY_TYPE, location("scp682_predators"));
    public static final TagKey<Block> Unbreakable = TagKey.create(Registries.BLOCK, location("scp682_unbreakable"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp682>> Scp682Type = EntityTypeRegister.register("scp682",
            () -> EntityType.Builder.<Scp682>of(Scp682::new, MobCategory.MONSTER)
                    .sized(Scp682.Width, Scp682.Height)
                    .eyeHeight(Scp682.EyeHeight)
                    .clientTrackingRange(12)
                    .build("scp682"));

    public static final DeferredItem<DeferredSpawnEggItem> Egg = ItemRegister.register("scp682_spawn_egg",
            () -> new DeferredSpawnEggItem(Scp682Type, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<Scp682RemoverItem> Remover = ItemRegister.register("scp682_remover",
            () -> new Scp682RemoverItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredHolder<SoundEvent, SoundEvent> Roar = sound("scp682_roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> RoarDeep = sound("scp682_roar_deep");
    public static final DeferredHolder<SoundEvent, SoundEvent> Growl = sound("scp682_growl");
    public static final DeferredHolder<SoundEvent, SoundEvent> GrowlLow = sound("scp682_growl_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Breathe = sound("scp682_breathe");
    public static final DeferredHolder<SoundEvent, SoundEvent> BreatheWet = sound("scp682_breathe_wet");
    public static final DeferredHolder<SoundEvent, SoundEvent> Step = sound("scp682_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> StepHeavy = sound("scp682_step_heavy");
    public static final DeferredHolder<SoundEvent, SoundEvent> Bite = sound("scp682_bite");
    public static final DeferredHolder<SoundEvent, SoundEvent> BiteCrunch = sound("scp682_bite_crunch");
    public static final DeferredHolder<SoundEvent, SoundEvent> Swipe = sound("scp682_swipe");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwipeRake = sound("scp682_swipe_rake");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp682_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HurtRumble = sound("scp682_hurt_rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> Glance = sound("scp682_glance");
    public static final DeferredHolder<SoundEvent, SoundEvent> GlanceScrape = sound("scp682_glance_scrape");
    public static final DeferredHolder<SoundEvent, SoundEvent> Collapse = sound("scp682_collapse");
    public static final DeferredHolder<SoundEvent, SoundEvent> CollapseThud = sound("scp682_collapse_thud");
    public static final DeferredHolder<SoundEvent, SoundEvent> Mend = sound("scp682_mend");
    public static final DeferredHolder<SoundEvent, SoundEvent> MendCrack = sound("scp682_mend_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> Adapt = sound("scp682_adapt");
    public static final DeferredHolder<SoundEvent, SoundEvent> AdaptCrackle = sound("scp682_adapt_crackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Shrink = sound("scp682_shrink");
    public static final DeferredHolder<SoundEvent, SoundEvent> ShrinkHiss = sound("scp682_shrink_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> Grow = sound("scp682_grow");
    public static final DeferredHolder<SoundEvent, SoundEvent> GrowCrack = sound("scp682_grow_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> Eyes = sound("scp682_eyes");
    public static final DeferredHolder<SoundEvent, SoundEvent> EyesWet = sound("scp682_eyes_wet");
    public static final DeferredHolder<SoundEvent, SoundEvent> Seal = sound("scp682_seal");
    public static final DeferredHolder<SoundEvent, SoundEvent> Calm = sound("scp682_calm");
    public static final DeferredHolder<SoundEvent, SoundEvent> Swim = sound("scp682_swim");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwimSurge = sound("scp682_swim_surge");
    public static final DeferredHolder<SoundEvent, SoundEvent> Smash = sound("scp682_smash");
    public static final DeferredHolder<SoundEvent, SoundEvent> Emerge = sound("scp682_emerge");
    public static final DeferredHolder<SoundEvent, SoundEvent> EmergeRumble = sound("scp682_emerge_rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> CommandRise = sound("scp682_command_rise");
    public static final DeferredHolder<SoundEvent, SoundEvent> CommandClank = sound("scp682_command_clank");
    public static final DeferredHolder<SoundEvent, SoundEvent> CommandSink = sound("scp682_command_sink");
    public static final DeferredHolder<SoundEvent, SoundEvent> Removed = sound("scp682_removed");
    public static final DeferredHolder<SoundEvent, SoundEvent> RemovedFizz = sound("scp682_removed_fizz");
    public static final DeferredHolder<SoundEvent, SoundEvent> Ricochet = sound("scp682_ricochet");
    public static final DeferredHolder<SoundEvent, SoundEvent> RicochetRing = sound("scp682_ricochet_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> Shed = sound("scp682_shed");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RoarLayers = List.of(Roar, RoarDeep);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> GrowlLayers = List.of(Growl, GrowlLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BreatheLayers = List.of(Breathe, BreatheWet);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StepLayers = List.of(Step, StepHeavy);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BiteLayers = List.of(Bite, BiteCrunch);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SwipeLayers = List.of(Swipe, SwipeRake);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HurtLayers = List.of(Hurt, HurtRumble);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> GlanceLayers = List.of(Glance, GlanceScrape);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CollapseLayers = List.of(Collapse, CollapseThud);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> MendLayers = List.of(Mend, MendCrack);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> AdaptLayers = List.of(Adapt, AdaptCrackle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ShrinkLayers = List.of(Shrink, ShrinkHiss);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> GrowLayers = List.of(Grow, GrowCrack);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> EyesLayers = List.of(Eyes, EyesWet);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SwimLayers = List.of(Swim, SwimSurge);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> EmergeLayers = List.of(Emerge, EmergeRumble);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CommandLayers = List.of(CommandRise, CommandClank);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RemovedLayers = List.of(Removed, RemovedFizz);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RicochetLayers = List.of(Ricochet, RicochetRing);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ShedLayers = List.of(Shed, HurtRumble, GrowlLow);

    private Scp682Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        StartConditions.register(Scp682LakeCondition.TypeName, Scp682LakeCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp682Lake::manifest);
        StasisPods.addCaptureVeto(Scp682::refusesStasis);
        Scp173Sight.addWatcher(Scp682Rivals::watchesStatue);
        Scp173Hunt.addPrey(Scp682Rivals::preyForStatue);
        InfectionImmunity.addSpare(Scp682::spared);
        InfectionImmunity.addAbsorber(Scp682::takes, Scp682::absorb);
        Scp457Goals.addPrey(Scp682Rivals::preyForBurning);
        Scp407Titan.addQuarry(Scp682Rivals::quarryForTitan);
        Scp076Abel.addPrey(Scp682Rivals::preyForAbel);
    }
}
