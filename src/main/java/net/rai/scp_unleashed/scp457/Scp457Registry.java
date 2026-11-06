package net.rai.scp_unleashed.scp457;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
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

public final class Scp457Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final String SpreadRuleName = "scp457Spread";
    public static final ResourceLocation DocumentId = location("scp_457");
    public static final ResourceKey<DamageType> BurnDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp457_burn"));
    public static final ResourceKey<DamageType> DousedDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp457_doused"));

    public static GameRules.Key<GameRules.BooleanValue> SpreadRule;

    public static final DeferredHolder<EntityType<?>, EntityType<Scp457>> Burning = EntityTypeRegister.register("scp457",
            () -> EntityType.Builder.<Scp457>of(Scp457::new, MobCategory.MONSTER)
                    .sized(Scp457.Width, Scp457.Height)
                    .eyeHeight(Scp457.EyeHeight)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("scp457"));

    public static final DeferredItem<DeferredSpawnEggItem> BurningEgg = ItemRegister.register("scp457_spawn_egg",
            () -> new DeferredSpawnEggItem(Burning, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));

    public static final DeferredBlock<Scp457AshPileBlock> AshPile = BlockRegister.register("scp457_ash_pile", () -> new Scp457AshPileBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY).strength(Scp457AshPileBlock.Hardness, Scp457AshPileBlock.Resistance).sound(SoundType.SAND).noCollission().noOcclusion()
            .pushReaction(PushReaction.BLOCK).lightLevel(Scp457AshPileBlock::glow).isValidSpawn((State, World, Pos, Kind) -> false)));

    public static final DeferredItem<BlockItem> AshPileItem = ItemRegister.registerSimpleBlockItem("scp457_ash_pile", AshPile);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp457AshPileBlockEntity>> AshPileEntity = BlockEntityTypeRegister.register("scp457_ash_pile",
            () -> BlockEntityType.Builder.of(Scp457AshPileBlockEntity::new, AshPile.get()).build(null));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Flame = ParticleRegister.register("scp457_flame", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Lick = ParticleRegister.register("scp457_lick", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Ember = ParticleRegister.register("scp457_ember", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Ash = ParticleRegister.register("scp457_ash", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Steam = ParticleRegister.register("scp457_steam", () -> new SimpleParticleType(true));

    public static final DeferredHolder<SoundEvent, SoundEvent> Crackle = sound("scp457_crackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Breath = sound("scp457_breath");
    public static final DeferredHolder<SoundEvent, SoundEvent> BreathLow = sound("scp457_breath_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rasp = sound("scp457_rasp");
    public static final DeferredHolder<SoundEvent, SoundEvent> Step = sound("scp457_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> StepAsh = sound("scp457_step_ash");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp457_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HurtBurn = sound("scp457_hurt_burn");
    public static final DeferredHolder<SoundEvent, SoundEvent> Swipe = sound("scp457_swipe");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwipeBreath = sound("scp457_swipe_breath");
    public static final DeferredHolder<SoundEvent, SoundEvent> Ignite = sound("scp457_ignite");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hiss = sound("scp457_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> Douse = sound("scp457_douse");
    public static final DeferredHolder<SoundEvent, SoundEvent> DouseCry = sound("scp457_douse_cry");
    public static final DeferredHolder<SoundEvent, SoundEvent> Collapse = sound("scp457_collapse");
    public static final DeferredHolder<SoundEvent, SoundEvent> CollapseDust = sound("scp457_collapse_dust");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rise = sound("scp457_rise");
    public static final DeferredHolder<SoundEvent, SoundEvent> RiseRoar = sound("scp457_rise_roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> Gutter = sound("scp457_gutter");
    public static final DeferredHolder<SoundEvent, SoundEvent> FrostSnuff = sound("scp457_frost_snuff");
    public static final DeferredHolder<SoundEvent, SoundEvent> FrostCrackle = sound("scp457_frost_crackle");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SwipeLayers = List.of(Swipe, SwipeBreath);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DouseLayers = List.of(Douse, DouseCry);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CollapseLayers = List.of(Collapse, CollapseDust);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RiseLayers = List.of(Rise, RiseRoar);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> FrostLayers = List.of(Hiss, FrostSnuff, FrostCrackle);

    private Scp457Registry() {
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
        BlockRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        StartConditions.register(Scp457SiteCondition.TypeName, Scp457SiteCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp457Arrival::spawn);
        ModEventBus.addListener(FMLCommonSetupEvent.class, Event -> Event.enqueueWork(Scp457Registry::registerRules));
    }

    private static void registerRules() {
        if (SpreadRule == null) {
            SpreadRule = GameRules.register(SpreadRuleName, GameRules.Category.MOBS, GameRules.BooleanValue.create(true));
        }
    }

    public static boolean spreads(GameRules Rules) {
        if (SpreadRule == null) {
            return true;
        }
        GameRules.BooleanValue Rule = Rules.getRule(SpreadRule);
        return Rule == null || Rule.get();
    }
}
