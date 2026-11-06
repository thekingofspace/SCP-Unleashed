package net.rai.scp_unleashed.scp428;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.StartConditions;
import net.rai.scp_unleashed.stasis.StasisPods;

import java.util.List;

public final class Scp428Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_428");
    public static final ResourceKey<DamageType> AbsorbDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp428_absorb"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp428Core>> Core = EntityTypeRegister.register("scp428",
            () -> EntityType.Builder.<Scp428Core>of(Scp428Core::new, MobCategory.MONSTER)
                    .sized(Scp428Size.SingleDiameter, Scp428Size.SingleDiameter * Scp428Size.HeightShare)
                    .eyeHeight(Scp428Size.SingleDiameter * Scp428Size.HeightShare * Scp428Size.EyeShare)
                    .clientTrackingRange(10)
                    .build("scp428"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp428Instance>> Instance = EntityTypeRegister.register("scp428_instance",
            () -> EntityType.Builder.<Scp428Instance>of(Scp428Instance::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.8f)
                    .eyeHeight(1.62f)
                    .vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT)
                    .clientTrackingRange(10)
                    .build("scp428_instance"));

    public static final DeferredItem<DeferredSpawnEggItem> CoreEgg = ItemRegister.register("scp428_spawn_egg",
            () -> new DeferredSpawnEggItem(Core, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));

    public static final DeferredHolder<SoundEvent, SoundEvent> Squelch = sound("scp428_squelch");
    public static final DeferredHolder<SoundEvent, SoundEvent> SquelchDrag = sound("scp428_squelch_drag");
    public static final DeferredHolder<SoundEvent, SoundEvent> Pulse = sound("scp428_pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> PulseLow = sound("scp428_pulse_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Reach = sound("scp428_reach");
    public static final DeferredHolder<SoundEvent, SoundEvent> ReachCreak = sound("scp428_reach_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> Lash = sound("scp428_lash");
    public static final DeferredHolder<SoundEvent, SoundEvent> LashLatch = sound("scp428_lash_latch");
    public static final DeferredHolder<SoundEvent, SoundEvent> Grab = sound("scp428_grab");
    public static final DeferredHolder<SoundEvent, SoundEvent> GrabHold = sound("scp428_grab_hold");
    public static final DeferredHolder<SoundEvent, SoundEvent> Absorb = sound("scp428_absorb");
    public static final DeferredHolder<SoundEvent, SoundEvent> AbsorbFold = sound("scp428_absorb_fold");
    public static final DeferredHolder<SoundEvent, SoundEvent> Snap = sound("scp428_snap");
    public static final DeferredHolder<SoundEvent, SoundEvent> SnapWhip = sound("scp428_snap_whip");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp428_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HurtSquirm = sound("scp428_hurt_squirm");
    public static final DeferredHolder<SoundEvent, SoundEvent> Death = sound("scp428_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DeathSag = sound("scp428_death_sag");
    public static final DeferredHolder<SoundEvent, SoundEvent> Groan = sound("scp428_groan");
    public static final DeferredHolder<SoundEvent, SoundEvent> GroanLow = sound("scp428_groan_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> InstanceHurt = sound("scp428_instance_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> InstanceDeath = sound("scp428_instance_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hold = sound("scp428_hold");
    public static final DeferredHolder<SoundEvent, SoundEvent> HoldSqueeze = sound("scp428_hold_squeeze");
    public static final DeferredHolder<SoundEvent, SoundEvent> Struggle = sound("scp428_struggle");
    public static final DeferredHolder<SoundEvent, SoundEvent> StruggleStrain = sound("scp428_struggle_strain");
    public static final DeferredHolder<SoundEvent, SoundEvent> BreakFree = sound("scp428_break_free");
    public static final DeferredHolder<SoundEvent, SoundEvent> BreakFreeSnap = sound("scp428_break_free_snap");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SquelchLayers = List.of(Squelch, SquelchDrag);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PulseLayers = List.of(Pulse, PulseLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ReachLayers = List.of(Reach, ReachCreak);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> LashLayers = List.of(Lash, LashLatch);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> GrabLayers = List.of(Grab, GrabHold);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> AbsorbLayers = List.of(Absorb, AbsorbFold);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SnapLayers = List.of(Snap, SnapWhip);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HurtLayers = List.of(HurtSquirm);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DeathLayers = List.of(DeathSag);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HoldLayers = List.of(Hold, HoldSqueeze);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StruggleLayers = List.of(Struggle, StruggleStrain);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BreakFreeLayers = List.of(BreakFree, BreakFreeSnap);

    private Scp428Registry() {
    }

    public static ResourceLocation location(String Name) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        StartConditions.register(Scp428SiteCondition.TypeName, Scp428SiteCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp428Arrival::spawn);
        StasisPods.addReleaseHook((World, Spawned) -> {
            if (Spawned instanceof Scp428Core Released) {
                Released.unfurl(World);
            }
        });
    }
}
