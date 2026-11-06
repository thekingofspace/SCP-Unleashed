package net.rai.scp_unleashed.playerclone;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.CloningKind;
import net.rai.scp_unleashed.dclass.CloningMachineBlockEntity;

import java.util.List;

public final class PlayerCloneRegistry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents DataComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);

    public static final ResourceKey<DamageType> NeedleDamage = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "sampling_needle"));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResolvableProfile>> SampleComponent = DataComponentRegister.registerComponentType("player_sample",
            Builder -> Builder.persistent(ResolvableProfile.CODEC).networkSynchronized(ResolvableProfile.STREAM_CODEC));

    public static final DeferredHolder<SoundEvent, SoundEvent> NeedleDraw = sound("sampling_needle_draw");
    public static final DeferredHolder<SoundEvent, SoundEvent> NeedleClick = sound("sampling_needle_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> NeedleClink = sound("sampling_needle_clink");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> NeedleLayers = List.of(NeedleDraw, NeedleClick, NeedleClink);

    public static final DeferredItem<SamplingNeedleItem> SamplingNeedle = ItemRegister.register("sampling_needle",
            () -> new SamplingNeedleItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<PlayerSampleItem> BloodSample = ItemRegister.register("player_blood_sample",
            () -> new PlayerSampleItem(new Item.Properties().stacksTo(16)));

    public static final DeferredHolder<EntityType<?>, EntityType<PlayerClone>> CloneType = EntityTypeRegister.register("player_clone",
            () -> EntityType.Builder.<PlayerClone>of(PlayerClone::new, MobCategory.MISC).sized(0.6f, 1.8f).eyeHeight(1.62f).vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT)
                    .clientTrackingRange(10).build("player_clone"));

    private PlayerCloneRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        DataComponentRegister.register(ModEventBus);
        CloningMachineBlockEntity.registerHatch(CloningKind.PlayerLine, new PlayerCloneHatch());
    }
}
