package net.rai.scp_unleashed.scp001scarlet.court;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletCourtHook;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class ScarletCourtRegistry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    private static final Map<CourtKind, DeferredHolder<EntityType<?>, ? extends EntityType<? extends CourtMinion>>> Minions = new EnumMap<>(CourtKind.class);
    private static final List<DeferredItem<DeferredSpawnEggItem>> Eggs = new ArrayList<>();
    private static boolean Installed;

    public static final DeferredHolder<EntityType<?>, EntityType<Thrall>> ThrallType = minion(CourtKind.Thrall, Thrall::new);
    public static final DeferredHolder<EntityType<?>, EntityType<ChainHound>> ChainHoundType = minion(CourtKind.ChainHound, ChainHound::new);
    public static final DeferredHolder<EntityType<?>, EntityType<ThornCrawler>> ThornCrawlerType = minion(CourtKind.ThornCrawler, ThornCrawler::new);
    public static final DeferredHolder<EntityType<?>, EntityType<Eyeling>> EyelingType = minion(CourtKind.Eyeling, Eyeling::new);
    public static final DeferredHolder<EntityType<?>, EntityType<CrimsonKnight>> CrimsonKnightType = minion(CourtKind.CrimsonKnight, CrimsonKnight::new);
    public static final DeferredHolder<EntityType<?>, EntityType<FlayedHerald>> FlayedHeraldType = minion(CourtKind.FlayedHerald, FlayedHerald::new);
    public static final DeferredHolder<EntityType<?>, EntityType<BellRinger>> BellRingerType = minion(CourtKind.BellRinger, BellRinger::new);
    public static final DeferredHolder<EntityType<?>, EntityType<AshWraith>> AshWraithType = minion(CourtKind.AshWraith, AshWraith::new);
    public static final DeferredHolder<EntityType<?>, EntityType<BrideHusk>> BrideHuskType = minion(CourtKind.BrideHusk, BrideHusk::new);
    public static final DeferredHolder<EntityType<?>, EntityType<AntlerStalker>> AntlerStalkerType = minion(CourtKind.AntlerStalker, AntlerStalker::new);
    public static final DeferredHolder<EntityType<?>, EntityType<Gnawer>> GnawerType = minion(CourtKind.Gnawer, Gnawer::new);
    public static final DeferredHolder<EntityType<?>, EntityType<ThroneGolem>> ThroneGolemType = minion(CourtKind.ThroneGolem, ThroneGolem::new);
    public static final DeferredHolder<EntityType<?>, EntityType<StarMoth>> StarMothType = minion(CourtKind.StarMoth, StarMoth::new);

    public static final DeferredHolder<EntityType<?>, EntityType<DaughterThorns>> ThornsType = daughter("scarlet_daughter_thorns", DaughterThorns::new, false);
    public static final DeferredHolder<EntityType<?>, EntityType<DaughterBride>> BrideType = daughter("scarlet_daughter_bride", DaughterBride::new, false);
    public static final DeferredHolder<EntityType<?>, EntityType<DaughterEyes>> EyesType = daughter("scarlet_daughter_eyes", DaughterEyes::new, false);
    public static final DeferredHolder<EntityType<?>, EntityType<DaughterMoon>> MoonType = daughter("scarlet_daughter_moon", DaughterMoon::new, false);
    public static final DeferredHolder<EntityType<?>, EntityType<EyesImage>> EyesImageType = EntityTypeRegister.register("scarlet_daughter_eyes_image",
            () -> EntityType.Builder.<EyesImage>of(EyesImage::new, MobCategory.MISC).sized(1.6f, 6.5f).eyeHeight(5.2f).clientTrackingRange(10).build("scarlet_daughter_eyes_image"));

    public static final DeferredHolder<EntityType<?>, EntityType<CourtProp>> ThornWallType = prop("scarlet_thorn_wall", 1.0f, 2.2f, MobCategory.MONSTER);
    public static final DeferredHolder<EntityType<?>, EntityType<CourtProp>> BriarCageType = prop("scarlet_briar_cage", 1.6f, 2.6f, MobCategory.MONSTER);
    public static final DeferredHolder<EntityType<?>, EntityType<CourtProp>> ChainAnchorType = prop("scarlet_chain_anchor", 0.9f, 0.9f, MobCategory.MONSTER);
    public static final DeferredHolder<EntityType<?>, EntityType<CourtProp>> ThornPatchType = prop("scarlet_thorn_patch", 1.0f, 0.3f, MobCategory.MISC);
    public static final DeferredHolder<EntityType<?>, EntityType<CourtBolt>> BoltType = EntityTypeRegister.register("scarlet_crimson_bolt",
            () -> EntityType.Builder.<CourtBolt>of(CourtBolt::new, MobCategory.MISC).sized(0.4f, 0.4f).clientTrackingRange(6).updateInterval(2).build("scarlet_crimson_bolt"));


    static {
        for (CourtKind Kind : CourtKind.values()) {
            egg(Kind.id(), () -> Minions.get(Kind).get(), Kind.eggBase(), Kind.eggSpots(), Rarity.UNCOMMON);
        }
        egg("scarlet_daughter_thorns", () -> ThornsType.get(), 0x120E12, 0x9E2A2B, Rarity.EPIC);
        egg("scarlet_daughter_bride", () -> BrideType.get(), 0x8E1E24, 0xE8E2D2, Rarity.EPIC);
        egg("scarlet_daughter_eyes", () -> EyesType.get(), 0x161014, 0xF08A30, Rarity.EPIC);
        egg("scarlet_daughter_moon", () -> MoonType.get(), 0x2A2448, 0xB8C8E8, Rarity.EPIC);
    }

    private ScarletCourtRegistry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static <T extends CourtMinion> DeferredHolder<EntityType<?>, EntityType<T>> minion(CourtKind Kind, EntityType.EntityFactory<T> Factory) {
        DeferredHolder<EntityType<?>, EntityType<T>> Holder = EntityTypeRegister.register(Kind.id(), () -> EntityType.Builder.of(Factory, MobCategory.MONSTER)
                .sized(Kind.width(), Kind.height()).eyeHeight(Kind.height() * 0.85f).clientTrackingRange(10).build(Kind.id()));
        Minions.put(Kind, Holder);
        return Holder;
    }

    private static <T extends CourtDaughter> DeferredHolder<EntityType<?>, EntityType<T>> daughter(String Id, EntityType.EntityFactory<T> Factory, boolean Fireproof) {
        return EntityTypeRegister.register(Id, () -> {
            EntityType.Builder<T> Builder = EntityType.Builder.of(Factory, MobCategory.MONSTER).sized(1.6f, 6.5f).eyeHeight(5.2f).clientTrackingRange(12);
            if (Fireproof) {
                Builder.fireImmune();
            }
            return Builder.build(Id);
        });
    }

    private static DeferredHolder<EntityType<?>, EntityType<CourtProp>> prop(String Id, float Width, float Height, MobCategory Category) {
        return EntityTypeRegister.register(Id, () -> EntityType.Builder.<CourtProp>of(CourtProp::new, Category).sized(Width, Height).clientTrackingRange(10).fireImmune().build(Id));
    }

    private static void egg(String Id, Supplier<? extends EntityType<? extends net.minecraft.world.entity.Mob>> Type, int Base, int Spots, Rarity Grade) {
        Eggs.add(ItemRegister.register(Id + "_spawn_egg", () -> new DeferredSpawnEggItem(Type, Base, Spots, new Item.Properties().rarity(Grade))));
    }

    public static EntityType<? extends CourtMinion> minionType(CourtKind Kind) {
        return Minions.get(Kind).get();
    }

    public static EntityType<? extends CourtDaughter> daughterType(int Index) {
        return switch (Index) {
            case 0 -> ThornsType.get();
            case 1 -> BrideType.get();
            case 2 -> EyesType.get();
            default -> MoonType.get();
        };
    }

    public static List<DeferredItem<DeferredSpawnEggItem>> eggs() {
        return List.copyOf(Eggs);
    }

    private static void attributes(EntityAttributeCreationEvent Event) {
        for (CourtKind Kind : CourtKind.values()) {
            Event.put(minionType(Kind), CourtMinion.attributes(Kind).build());
        }
        Event.put(ThornsType.get(), CourtDaughter.attributes(0.22).build());
        Event.put(BrideType.get(), CourtDaughter.attributes(0.24).build());
        Event.put(EyesType.get(), CourtDaughter.attributes(0.3).build());
        Event.put(MoonType.get(), CourtDaughter.attributes(0.26).build());
        Event.put(EyesImageType.get(), EyesImage.attributes().build());
        for (DeferredHolder<EntityType<?>, EntityType<CourtProp>> Prop : List.of(ThornWallType, BriarCageType, ChainAnchorType, ThornPatchType)) {
            Event.put(Prop.get(), CourtProp.attributes().build());
        }
    }

    private static void tabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            for (DeferredItem<DeferredSpawnEggItem> Egg : Eggs) {
                Event.accept(Egg.get());
            }
        }
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        ScarletCourtSounds.SoundEventRegister.register(ModEventBus);
        ModEventBus.addListener(EntityAttributeCreationEvent.class, ScarletCourtRegistry::attributes);
        ModEventBus.addListener(EventPriority.LOWEST, BuildCreativeModeTabContentsEvent.class, ScarletCourtRegistry::tabs);
        ModEventBus.addListener(FMLCommonSetupEvent.class, Event -> Event.enqueueWork(ScarletCourtRegistry::install));
    }

    public static void install() {
        ScarletCourtHook.Holder.set(ScarletCourt.Instance);
        Installed = true;
    }

    public static boolean installed() {
        return Installed;
    }
}
