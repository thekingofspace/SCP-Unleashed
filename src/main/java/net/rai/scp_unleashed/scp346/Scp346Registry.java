package net.rai.scp_unleashed.scp346;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
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
import net.rai.scp_unleashed.archive.investigation.StartConditions;

import java.util.List;

public final class Scp346Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_346");

    public static final DeferredHolder<EntityType<?>, EntityType<Scp346Pterosaur>> Pterosaur = EntityTypeRegister.register("scp346",
            () -> EntityType.Builder.<Scp346Pterosaur>of(Scp346Pterosaur::new, MobCategory.CREATURE)
                    .sized(Scp346Pterosaur.Width, Scp346Pterosaur.Height)
                    .eyeHeight(Scp346Pterosaur.EyeHeight)
                    .clientTrackingRange(10)
                    .build("scp346"));

    public static final DeferredItem<DeferredSpawnEggItem> Egg = ItemRegister.register("scp346_spawn_egg",
            () -> new DeferredSpawnEggItem(Pterosaur, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<SoundEvent, SoundEvent> Chirr = sound("scp346_chirr");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChirrClick = sound("scp346_chirr_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> Call = sound("scp346_call");
    public static final DeferredHolder<SoundEvent, SoundEvent> Clack = sound("scp346_clack");
    public static final DeferredHolder<SoundEvent, SoundEvent> Flap = sound("scp346_flap");
    public static final DeferredHolder<SoundEvent, SoundEvent> Coo = sound("scp346_coo");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp346_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> Death = sound("scp346_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> Eat = sound("scp346_eat");
    public static final DeferredHolder<SoundEvent, SoundEvent> Step = sound("scp346_step");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ChirrLayers = List.of(Chirr, ChirrClick);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CallLayers = List.of(Call, Clack);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> EatLayers = List.of(Eat, Clack);

    private Scp346Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        StartConditions.register(Scp346FlockCondition.TypeName, Scp346FlockCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp346FlockCondition::manifest);
    }
}
