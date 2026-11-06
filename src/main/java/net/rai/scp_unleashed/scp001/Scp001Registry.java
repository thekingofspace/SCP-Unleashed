package net.rai.scp_unleashed.scp001;

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

public final class Scp001Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_001");
    public static final ResourceKey<DamageType> Claw = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp001_claw"));
    public static final ResourceKey<DamageType> Singularity = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp001_singularity"));
    public static final TagKey<Block> SingularityProof = TagKey.create(Registries.BLOCK, location("scp001_singularity_proof"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp001Prototype>> Prototype = EntityTypeRegister.register("scp001",
            () -> EntityType.Builder.<Scp001Prototype>of(Scp001Prototype::new, MobCategory.MONSTER)
                    .sized(Scp001Prototype.Width, Scp001Prototype.Height)
                    .eyeHeight(Scp001Prototype.EyeHeight)
                    .fireImmune()
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .build("scp001"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp001Singularity>> Orb = EntityTypeRegister.register("scp001_singularity",
            () -> EntityType.Builder.<Scp001Singularity>of(Scp001Singularity::new, MobCategory.MISC)
                    .sized(Scp001Singularity.Width, Scp001Singularity.Width)
                    .fireImmune()
                    .noSave()
                    .noSummon()
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("scp001_singularity"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp001Portal>> Portal = EntityTypeRegister.register("scp001_portal",
            () -> EntityType.Builder.<Scp001Portal>of(Scp001Portal::new, MobCategory.MISC)
                    .sized(Scp001Portal.Width, Scp001Portal.Height)
                    .fireImmune()
                    .noSave()
                    .clientTrackingRange(12)
                    .updateInterval(2)
                    .build("scp001_portal"));

    public static final DeferredItem<DeferredSpawnEggItem> SpawnEgg = ItemRegister.register("scp001_spawn_egg",
            () -> new DeferredSpawnEggItem(Prototype, 0xB9B6B0, 0x7A0E16, new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<SoundEvent, SoundEvent> Skitter = sound("scp001_skitter");
    public static final DeferredHolder<SoundEvent, SoundEvent> SkitterClick = sound("scp001_skitter_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> Chitter = sound("scp001_chitter");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChitterLow = sound("scp001_chitter_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp001_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HurtClick = sound("scp001_hurt_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> Warp = sound("scp001_warp");
    public static final DeferredHolder<SoundEvent, SoundEvent> WarpCrackle = sound("scp001_warp_crackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> WarpChime = sound("scp001_warp_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> WarpTick = sound("scp001_warp_tick");
    public static final DeferredHolder<SoundEvent, SoundEvent> FlashCharge = sound("scp001_flash_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> FlashWhine = sound("scp001_flash_whine");
    public static final DeferredHolder<SoundEvent, SoundEvent> FlashBurst = sound("scp001_flash_burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> FlashShatter = sound("scp001_flash_shatter");
    public static final DeferredHolder<SoundEvent, SoundEvent> Strain = sound("scp001_strain");
    public static final DeferredHolder<SoundEvent, SoundEvent> StrainVoice = sound("scp001_strain_voice");
    public static final DeferredHolder<SoundEvent, SoundEvent> PortalOpen = sound("scp001_portal");
    public static final DeferredHolder<SoundEvent, SoundEvent> PortalHum = sound("scp001_portal_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> PortalGlass = sound("scp001_portal_glass");
    public static final DeferredHolder<SoundEvent, SoundEvent> PortalClose = sound("scp001_portal_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> Slash = sound("scp001_slash");
    public static final DeferredHolder<SoundEvent, SoundEvent> SlashHiss = sound("scp001_slash_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> Strike = sound("scp001_strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> StrikeBone = sound("scp001_strike_bone");
    public static final DeferredHolder<SoundEvent, SoundEvent> Lunge = sound("scp001_lunge");
    public static final DeferredHolder<SoundEvent, SoundEvent> Reel = sound("scp001_reel");
    public static final DeferredHolder<SoundEvent, SoundEvent> ReelChitter = sound("scp001_reel_chitter");
    public static final DeferredHolder<SoundEvent, SoundEvent> Settle = sound("scp001_settle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Dissolve = sound("scp001_dissolve");
    public static final DeferredHolder<SoundEvent, SoundEvent> DissolveShatter = sound("scp001_dissolve_shatter");
    public static final DeferredHolder<SoundEvent, SoundEvent> Knock = sound("scp001_knock");
    public static final DeferredHolder<SoundEvent, SoundEvent> KnockWood = sound("scp001_knock_wood");
    public static final DeferredHolder<SoundEvent, SoundEvent> Tap = sound("scp001_tap");
    public static final DeferredHolder<SoundEvent, SoundEvent> Snap = sound("scp001_snap");
    public static final DeferredHolder<SoundEvent, SoundEvent> SnapDry = sound("scp001_snap_dry");
    public static final DeferredHolder<SoundEvent, SoundEvent> Click = sound("scp001_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> ClickBone = sound("scp001_click_bone");
    public static final DeferredHolder<SoundEvent, SoundEvent> VoiceLow = sound("scp001_voice_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> VoiceLowBody = sound("scp001_voice_low_body");
    public static final DeferredHolder<SoundEvent, SoundEvent> VoiceHigh = sound("scp001_voice_high");
    public static final DeferredHolder<SoundEvent, SoundEvent> VoiceHighThin = sound("scp001_voice_high_thin");
    public static final DeferredHolder<SoundEvent, SoundEvent> Breath = sound("scp001_breath");
    public static final DeferredHolder<SoundEvent, SoundEvent> Drift = sound("scp001_drift");
    public static final DeferredHolder<SoundEvent, SoundEvent> SingularityCharge = sound("scp001_singularity_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> SingularityChargeLow = sound("scp001_singularity_charge_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> SingularityForm = sound("scp001_singularity_form");
    public static final DeferredHolder<SoundEvent, SoundEvent> SingularityHum = sound("scp001_singularity_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> SingularityLaunch = sound("scp001_singularity_launch");
    public static final DeferredHolder<SoundEvent, SoundEvent> SingularityImplode = sound("scp001_singularity_implode");
    public static final DeferredHolder<SoundEvent, SoundEvent> SingularityImplodeGlass = sound("scp001_singularity_implode_glass");
    public static final DeferredHolder<SoundEvent, SoundEvent> SingularityFizzle = sound("scp001_singularity_fizzle");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SkitterLayers = List.of(Skitter, SkitterClick);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ChitterLayers = List.of(Chitter, ChitterLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HurtLayers = List.of(Hurt, HurtClick);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WarpLayers = List.of(Warp, WarpCrackle, WarpChime);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ChargeLayers = List.of(FlashCharge, FlashWhine);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StrainLayers = List.of(Strain, StrainVoice);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BurstLayers = List.of(FlashBurst, FlashShatter);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PortalLayers = List.of(PortalOpen, PortalHum, PortalGlass);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SlashLayers = List.of(Slash, SlashHiss);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StrikeLayers = List.of(Strike, StrikeBone);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> LungeLayers = List.of(Lunge, SkitterClick);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ReelLayers = List.of(Reel, ReelChitter);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DissolveLayers = List.of(Dissolve, DissolveShatter);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> KnockLayers = List.of(Knock, KnockWood);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TapLayers = List.of(Tap);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SnapLayers = List.of(Snap, SnapDry);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ClickLayers = List.of(Click, ClickBone);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> LowVoiceLayers = List.of(VoiceLow, VoiceLowBody);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HighVoiceLayers = List.of(VoiceHigh, VoiceHighThin);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BreathLayers = List.of(Breath);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DriftLayers = List.of(Drift);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WarpTickLayers = List.of(WarpTick);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SingularityChargeLayers = List.of(SingularityCharge, SingularityChargeLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SingularityFormLayers = List.of(SingularityForm);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SingularityHumLayers = List.of(SingularityHum);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SingularityLaunchLayers = List.of(SingularityLaunch);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SingularityImplodeLayers = List.of(SingularityImplode, SingularityImplodeGlass);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SingularityFizzleLayers = List.of(SingularityFizzle);

    private Scp001Registry() {
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
        StartConditions.register(Scp001ArrivalCondition.TypeName, Scp001ArrivalCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp001ArrivalCondition::manifest);
        StasisPods.addCaptureVeto(Scp001Prototype::refusesStasis);
        StasisPods.addReleaseHook((World, Spawned) -> {
            if (Spawned instanceof Scp001Prototype Creature) {
                Creature.leaveStasis(World);
            }
        });
    }
}
