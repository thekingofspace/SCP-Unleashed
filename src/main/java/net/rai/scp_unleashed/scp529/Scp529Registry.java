package net.rai.scp_unleashed.scp529;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class Scp529Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_529");
    public static final TagKey<Item> Cheese = TagKey.create(Registries.ITEM, location("scp529_cheese"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp529Cat>> Josie = EntityTypeRegister.register("scp529_cat",
            () -> EntityType.Builder.<Scp529Cat>of(Scp529Cat::new, MobCategory.CREATURE)
                    .sized(Scp529Cat.Width, Scp529Cat.Height)
                    .clientTrackingRange(8)
                    .build("scp529_cat"));

    public static final DeferredItem<DeferredSpawnEggItem> Egg = ItemRegister.register("scp529_spawn_egg",
            () -> new DeferredSpawnEggItem(Josie, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<SoundEvent, SoundEvent> Meow = sound("scp529_meow");
    public static final DeferredHolder<SoundEvent, SoundEvent> MeowPurr = sound("scp529_meow_purr");
    public static final DeferredHolder<SoundEvent, SoundEvent> Trill = sound("scp529_trill");
    public static final DeferredHolder<SoundEvent, SoundEvent> TrillPurr = sound("scp529_trill_purr");
    public static final DeferredHolder<SoundEvent, SoundEvent> Mew = sound("scp529_mew");
    public static final DeferredHolder<SoundEvent, SoundEvent> MewSoft = sound("scp529_mew_soft");
    public static final DeferredHolder<SoundEvent, SoundEvent> Purr = sound("scp529_purr");
    public static final DeferredHolder<SoundEvent, SoundEvent> PurrDeep = sound("scp529_purr_deep");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hiss = sound("scp529_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> HissLow = sound("scp529_hiss_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp529_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HurtHiss = sound("scp529_hurt_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> Death = sound("scp529_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> Eat = sound("scp529_eat");
    public static final DeferredHolder<SoundEvent, SoundEvent> EatPurr = sound("scp529_eat_purr");
    public static final DeferredHolder<SoundEvent, SoundEvent> Beg = sound("scp529_beg");
    public static final DeferredHolder<SoundEvent, SoundEvent> BegTrill = sound("scp529_beg_trill");
    public static final DeferredHolder<SoundEvent, SoundEvent> Yowl = sound("scp529_yowl");
    public static final DeferredHolder<SoundEvent, SoundEvent> YowlStray = sound("scp529_yowl_stray");
    public static final DeferredHolder<SoundEvent, SoundEvent> Swat = sound("scp529_swat");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwatHiss = sound("scp529_swat_hiss");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> MeowLayers = List.of(Meow, MeowPurr);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TrillLayers = List.of(Trill, TrillPurr);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> MewLayers = List.of(Mew, MewSoft);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PurrLayers = List.of(Purr, PurrDeep);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HissLayers = List.of(Hiss, HissLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HurtLayers = List.of(Hurt, HurtHiss);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DeathLayers = List.of(Death);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> EatLayers = List.of(Eat, EatPurr);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BegLayers = List.of(Beg, BegTrill);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> YowlLayers = List.of(Yowl, YowlStray);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SwatLayers = List.of(Swat, SwatHiss);

    private Scp529Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    @Nullable
    public static List<DeferredHolder<SoundEvent, SoundEvent>> voice(SoundEvent Sound) {
        if (Sound == SoundEvents.CAT_AMBIENT || Sound == Meow.get()) {
            return MeowLayers;
        }
        if (Sound == SoundEvents.CAT_PURREOW || Sound == Trill.get()) {
            return TrillLayers;
        }
        if (Sound == SoundEvents.CAT_STRAY_AMBIENT || Sound == Mew.get()) {
            return MewLayers;
        }
        if (Sound == SoundEvents.CAT_PURR || Sound == Purr.get()) {
            return PurrLayers;
        }
        if (Sound == SoundEvents.CAT_HISS || Sound == Hiss.get()) {
            return HissLayers;
        }
        if (Sound == SoundEvents.CAT_HURT || Sound == Hurt.get()) {
            return HurtLayers;
        }
        if (Sound == SoundEvents.CAT_DEATH || Sound == Death.get()) {
            return DeathLayers;
        }
        if (Sound == SoundEvents.CAT_EAT || Sound == Eat.get()) {
            return EatLayers;
        }
        if (Sound == SoundEvents.CAT_BEG_FOR_FOOD || Sound == Beg.get()) {
            return BegLayers;
        }
        if (Sound == Yowl.get()) {
            return YowlLayers;
        }
        if (Sound == Swat.get()) {
            return SwatLayers;
        }
        return null;
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        StartConditions.register(Scp529StrayCondition.TypeName, Scp529StrayCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp529StrayCondition::manifest);
    }
}
