package net.rai.scp_unleashed.scp492;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
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

import java.util.List;

public final class Scp492Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_492");
    public static final TagKey<Item> Frightening = TagKey.create(Registries.ITEM, location("scp492_frightening"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp492Figure>> Figure = EntityTypeRegister.register("scp492_figure",
            () -> EntityType.Builder.<Scp492Figure>of(Scp492Figure::new, MobCategory.CREATURE)
                    .sized(Scp492Figure.Width, Scp492Figure.Height)
                    .eyeHeight(Scp492Figure.EyeHeight)
                    .clientTrackingRange(10)
                    .build("scp492_figure"));

    public static final DeferredItem<DeferredSpawnEggItem> Egg = ItemRegister.register("scp492_spawn_egg",
            () -> new DeferredSpawnEggItem(Figure, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<SoundEvent, SoundEvent> Chatter = sound("scp492_chatter");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChatterRustle = sound("scp492_chatter_rustle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Ahoy = sound("scp492_ahoy");
    public static final DeferredHolder<SoundEvent, SoundEvent> AhoySwag = sound("scp492_ahoy_swag");
    public static final DeferredHolder<SoundEvent, SoundEvent> Alert = sound("scp492_alert");
    public static final DeferredHolder<SoundEvent, SoundEvent> AlertBell = sound("scp492_alert_bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> Fear = sound("scp492_fear");
    public static final DeferredHolder<SoundEvent, SoundEvent> FearRustle = sound("scp492_fear_rustle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rustle = sound("scp492_rustle");
    public static final DeferredHolder<SoundEvent, SoundEvent> RustleStraw = sound("scp492_rustle_straw");
    public static final DeferredHolder<SoundEvent, SoundEvent> Tear = sound("scp492_tear");
    public static final DeferredHolder<SoundEvent, SoundEvent> TearSpill = sound("scp492_tear_spill");
    public static final DeferredHolder<SoundEvent, SoundEvent> Sew = sound("scp492_sew");
    public static final DeferredHolder<SoundEvent, SoundEvent> SewStuff = sound("scp492_sew_stuff");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rise = sound("scp492_rise");
    public static final DeferredHolder<SoundEvent, SoundEvent> Refuse = sound("scp492_refuse");
    public static final DeferredHolder<SoundEvent, SoundEvent> Step = sound("scp492_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> Burn = sound("scp492_burn");
    public static final DeferredHolder<SoundEvent, SoundEvent> BurnCrackle = sound("scp492_burn_crackle");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ChatterLayers = List.of(Chatter, ChatterRustle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> AhoyLayers = List.of(Ahoy, AhoySwag);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> AlertLayers = List.of(Alert, AlertBell);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> FearLayers = List.of(Fear, FearRustle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RustleLayers = List.of(Rustle, RustleStraw);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TearLayers = List.of(Tear, TearSpill);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SewLayers = List.of(Sew, SewStuff);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BurnLayers = List.of(Burn, BurnCrackle);

    private Scp492Registry() {
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
        StartConditions.register(Scp492OutskirtsCondition.TypeName, Scp492OutskirtsCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp492OutskirtsCondition::manifest);
    }
}
