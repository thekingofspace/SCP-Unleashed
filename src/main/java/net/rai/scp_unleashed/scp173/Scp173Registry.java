package net.rai.scp_unleashed.scp173;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.StartConditions;

import java.util.List;

public final class Scp173Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_173");
    public static final String CloningRuleName = "scp173Cloning";
    public static final ResourceKey<DamageType> Snap = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp173_snap"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp173Statue>> Statue = EntityTypeRegister.register("scp173",
            () -> EntityType.Builder.<Scp173Statue>of(Scp173Statue::new, MobCategory.MONSTER)
                    .sized(Scp173Statue.Width, Scp173Statue.Height)
                    .eyeHeight(Scp173Statue.EyeHeight)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("scp173"));

    public static final DeferredBlock<Scp173ResidueBlock> Residue = BlockRegister.register("scp173_residue", () -> new Scp173ResidueBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_BROWN).strength(Scp173ResidueBlock.Hardness).requiresCorrectToolForDrops().sound(SoundType.PACKED_MUD)
            .noOcclusion().noCollission().pushReaction(PushReaction.DESTROY)));

    public static final DeferredItem<BlockItem> ResidueItem = ItemRegister.register("scp173_residue",
            () -> new Scp173ResidueItem(Residue.get(), new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> SpawnEgg = ItemRegister.register("scp173_spawn_egg",
            () -> new DeferredSpawnEggItem(Statue, 0xB8A882, 0x6E261C, new Item.Properties()));

    public static final DeferredHolder<SoundEvent, SoundEvent> Scrape = sound("scp173_scrape");
    public static final DeferredHolder<SoundEvent, SoundEvent> ScrapeGrit = sound("scp173_scrape_grit");
    public static final DeferredHolder<SoundEvent, SoundEvent> ScrapeDrag = sound("scp173_scrape_drag");
    public static final DeferredHolder<SoundEvent, SoundEvent> Pose = sound("scp173_pose");
    public static final DeferredHolder<SoundEvent, SoundEvent> PoseClack = sound("scp173_pose_clack");
    public static final DeferredHolder<SoundEvent, SoundEvent> Crack = sound("scp173_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrackSnap = sound("scp173_crack_snap");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrackThud = sound("scp173_crack_thud");
    public static final DeferredHolder<SoundEvent, SoundEvent> Chip = sound("scp173_chip");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChipGrit = sound("scp173_chip_grit");
    public static final DeferredHolder<SoundEvent, SoundEvent> Crumble = sound("scp173_crumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrumbleGrit = sound("scp173_crumble_grit");
    public static final DeferredHolder<SoundEvent, SoundEvent> Glance = sound("scp173_glance");
    public static final DeferredHolder<SoundEvent, SoundEvent> GlanceRing = sound("scp173_glance_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> Clone = sound("scp173_clone");
    public static final DeferredHolder<SoundEvent, SoundEvent> CloneGrind = sound("scp173_clone_grind");
    public static final DeferredHolder<SoundEvent, SoundEvent> Door = sound("scp173_door");
    public static final DeferredHolder<SoundEvent, SoundEvent> PodAlarm = sound("scp173_pod_alarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> PodAlarmPip = sound("scp173_pod_alarm_pip");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ScrapeLayers = List.of(Scrape, ScrapeGrit, ScrapeDrag);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PoseLayers = List.of(Pose, PoseClack);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CrackLayers = List.of(Crack, CrackSnap, CrackThud);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ChipLayers = List.of(Chip, ChipGrit);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CrumbleLayers = List.of(Crumble, CrumbleGrit);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> GlanceLayers = List.of(Glance, GlanceRing);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CloneLayers = List.of(Clone, CloneGrind);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PodAlarmLayers = List.of(PodAlarm, PodAlarmPip);

    public static GameRules.Key<GameRules.BooleanValue> CloningRule;

    private Scp173Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        StartConditions.register(Scp173ArrivalCondition.TypeName, Scp173ArrivalCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp173ArrivalCondition::manifest);
        ModEventBus.addListener(FMLCommonSetupEvent.class, Event -> Event.enqueueWork(Scp173Registry::registerRules));
    }

    private static void registerRules() {
        if (CloningRule == null) {
            CloningRule = GameRules.register(CloningRuleName, GameRules.Category.MOBS, GameRules.BooleanValue.create(true));
        }
    }

    public static boolean cloning(GameRules Rules) {
        if (CloningRule == null) {
            return true;
        }
        GameRules.BooleanValue Rule = Rules.getRule(CloningRule);
        return Rule == null || Rule.get();
    }
}
