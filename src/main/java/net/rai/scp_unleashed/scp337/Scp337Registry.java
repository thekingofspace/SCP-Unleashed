package net.rai.scp_unleashed.scp337;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.StartConditions;

import java.util.List;

public final class Scp337Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_337");
    public static final ResourceKey<DamageType> LashDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp337_lash"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp337Mass>> Mass = EntityTypeRegister.register("scp337",
            () -> EntityType.Builder.<Scp337Mass>of(Scp337Mass::new, MobCategory.MONSTER)
                    .sized(Scp337Mass.Width, Scp337Mass.Height)
                    .eyeHeight(Scp337Mass.EyeHeight)
                    .clientTrackingRange(10)
                    .build("scp337"));

    public static final DeferredItem<DeferredSpawnEggItem> MassEgg = ItemRegister.register("scp337_spawn_egg",
            () -> new DeferredSpawnEggItem(Mass, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));

    public static final DeferredHolder<SoundEvent, SoundEvent> Rustle = sound("scp337_rustle");
    public static final DeferredHolder<SoundEvent, SoundEvent> RustleMoss = sound("scp337_rustle_moss");
    public static final DeferredHolder<SoundEvent, SoundEvent> Swish = sound("scp337_swish");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwishRope = sound("scp337_swish_rope");
    public static final DeferredHolder<SoundEvent, SoundEvent> Crack = sound("scp337_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrackSnap = sound("scp337_crack_snap");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hit = sound("scp337_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> HitWrap = sound("scp337_hit_wrap");
    public static final DeferredHolder<SoundEvent, SoundEvent> Retract = sound("scp337_retract");
    public static final DeferredHolder<SoundEvent, SoundEvent> Drag = sound("scp337_drag");
    public static final DeferredHolder<SoundEvent, SoundEvent> DragCloth = sound("scp337_drag_cloth");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp337_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HurtTear = sound("scp337_hurt_tear");
    public static final DeferredHolder<SoundEvent, SoundEvent> Death = sound("scp337_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DeathSag = sound("scp337_death_sag");
    public static final DeferredHolder<SoundEvent, SoundEvent> Settle = sound("scp337_settle");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RustleLayers = List.of(Rustle, RustleMoss);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SwishLayers = List.of(Swish, SwishRope);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CrackLayers = List.of(Crack, CrackSnap);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HitLayers = List.of(Hit, HitWrap);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DragLayers = List.of(Drag, DragCloth);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HurtLayers = List.of(HurtTear);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DeathLayers = List.of(DeathSag);

    private Scp337Registry() {
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
        StartConditions.register(Scp337SiteCondition.TypeName, Scp337SiteCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp337Arrival::spawn);
    }
}
