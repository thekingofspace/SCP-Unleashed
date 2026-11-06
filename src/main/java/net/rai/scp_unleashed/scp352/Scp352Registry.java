package net.rai.scp_unleashed.scp352;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.ReaderTargets;
import net.rai.scp_unleashed.archive.investigation.StartConditions;
import net.rai.scp_unleashed.stasis.StasisPods;

import java.util.List;

public final class Scp352Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_352");
    public static final ResourceKey<DamageType> Hunt = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp352_hunt"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp352Crone>> Crone = EntityTypeRegister.register("scp352",
            () -> EntityType.Builder.<Scp352Crone>of(Scp352Crone::new, MobCategory.MONSTER)
                    .sized(Scp352Crone.LowWidth, Scp352Crone.LowHeight)
                    .eyeHeight(Scp352Crone.LowEye)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build("scp352"));

    public static final DeferredItem<DeferredSpawnEggItem> SpawnEgg = ItemRegister.register("scp352_spawn_egg",
            () -> new DeferredSpawnEggItem(Crone, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<SoundEvent, SoundEvent> CaneTap = sound("scp352_cane_tap");
    public static final DeferredHolder<SoundEvent, SoundEvent> CaneKnock = sound("scp352_cane_knock");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rustle = sound("scp352_rustle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Mutter = sound("scp352_mutter");
    public static final DeferredHolder<SoundEvent, SoundEvent> MutterLow = sound("scp352_mutter_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Cackle = sound("scp352_cackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Crackle = sound("scp352_crackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrackleBone = sound("scp352_crackle_bone");
    public static final DeferredHolder<SoundEvent, SoundEvent> Squawk = sound("scp352_squawk");
    public static final DeferredHolder<SoundEvent, SoundEvent> SquawkWitch = sound("scp352_squawk_witch");
    public static final DeferredHolder<SoundEvent, SoundEvent> Step = sound("scp352_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> StepClaw = sound("scp352_step_claw");
    public static final DeferredHolder<SoundEvent, SoundEvent> Kick = sound("scp352_kick");
    public static final DeferredHolder<SoundEvent, SoundEvent> KickRake = sound("scp352_kick_rake");
    public static final DeferredHolder<SoundEvent, SoundEvent> Swipe = sound("scp352_swipe");
    public static final DeferredHolder<SoundEvent, SoundEvent> Snore = sound("scp352_snore");
    public static final DeferredHolder<SoundEvent, SoundEvent> SnoreRattle = sound("scp352_snore_rattle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Breath = sound("scp352_breath");
    public static final DeferredHolder<SoundEvent, SoundEvent> Wake = sound("scp352_wake");
    public static final DeferredHolder<SoundEvent, SoundEvent> Settle = sound("scp352_settle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp352_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HurtSquawk = sound("scp352_hurt_squawk");
    public static final DeferredHolder<SoundEvent, SoundEvent> Death = sound("scp352_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DeathSigh = sound("scp352_death_sigh");
    public static final DeferredHolder<SoundEvent, SoundEvent> LeapWindup = sound("scp352_leap_windup");
    public static final DeferredHolder<SoundEvent, SoundEvent> Leap = sound("scp352_leap");
    public static final DeferredHolder<SoundEvent, SoundEvent> LeapCackle = sound("scp352_leap_cackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Land = sound("scp352_land");
    public static final DeferredHolder<SoundEvent, SoundEvent> LandThud = sound("scp352_land_thud");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CaneLayers = List.of(CaneTap, CaneKnock);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> MutterLayers = List.of(Mutter, MutterLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CrackleLayers = List.of(Crackle, CrackleBone);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SquawkLayers = List.of(Squawk, SquawkWitch);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StepLayers = List.of(Step, StepClaw);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> KickLayers = List.of(Kick, KickRake);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SnoreLayers = List.of(Snore, SnoreRattle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HurtLayers = List.of(Hurt, HurtSquawk);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DeathLayers = List.of(Death, DeathSigh);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> LeapLayers = List.of(Leap, LeapCackle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> LandLayers = List.of(Land, LandThud);

    private Scp352Registry() {
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
        StartConditions.register(Scp352WanderCondition.TypeName, Scp352WanderCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp352Arrival::manifest);
        ReaderTargets.onHandoff(Scp352WanderCondition::handOff);
        StasisPods.addCaptureVeto(Scp352Crone::refusesStasis);
        StasisPods.addReleaseHook((World, Spawned) -> {
            if (Spawned instanceof Scp352Crone Creature) {
                Creature.leaveStasis(World);
            }
        });
    }
}
