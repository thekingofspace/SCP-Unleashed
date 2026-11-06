package net.rai.scp_unleashed.scp303;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
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

import java.util.List;

public final class Scp303Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_303");
    public static final TagKey<Block> IgnoredDoors = TagKey.create(Registries.BLOCK, location("scp303_ignored_doors"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp303Doorman>> Doorman = EntityTypeRegister.register("scp303_doorman",
            () -> EntityType.Builder.<Scp303Doorman>of(Scp303Doorman::new, MobCategory.MONSTER)
                    .sized(Scp303Doorman.Width, Scp303Doorman.Height)
                    .eyeHeight(Scp303Doorman.EyeHeight)
                    .clientTrackingRange(10)
                    .build("scp303_doorman"));

    public static final DeferredItem<DeferredSpawnEggItem> DoormanEgg = ItemRegister.register("scp303_doorman_spawn_egg",
            () -> new DeferredSpawnEggItem(Doorman, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<SoundEvent, SoundEvent> Step = sound("scp303_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> StepThud = sound("scp303_step_thud");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rattle = sound("scp303_rattle");
    public static final DeferredHolder<SoundEvent, SoundEvent> RattleKnock = sound("scp303_rattle_knock");
    public static final DeferredHolder<SoundEvent, SoundEvent> Creak = sound("scp303_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> CreakLow = sound("scp303_creak_low");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RattleLayers = List.of(Rattle, RattleKnock);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CreakLayers = List.of(Creak, CreakLow);

    private Scp303Registry() {
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
        StartConditions.register(Scp303ArrivalCondition.TypeName, Scp303ArrivalCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp303Arrival::arrive);
    }
}
