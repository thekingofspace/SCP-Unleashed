package net.rai.scp_unleashed.scp009;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.storage.StorageBehaviours;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

public final class Scp009Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<MobEffect> MobEffectRegister = DeferredRegister.create(Registries.MOB_EFFECT, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_009");
    public static final ResourceKey<DamageType> CrystallizedDamage = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp009_crystallized"));

    public static final DeferredBlock<RedIceBlock> RedIce = BlockRegister.register("scp009_red_ice", () -> new RedIceBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_RED).instrument(NoteBlockInstrument.CHIME).friction(0.98f).strength(1.5f, 6.0f).sound(SoundType.AMETHYST)
            .requiresCorrectToolForDrops().noOcclusion().lightLevel(State -> RedIceBlock.Glow)
            .isValidSpawn((State, Getter, Pos, Type) -> false).isRedstoneConductor((State, Getter, Pos) -> false)));
    public static final DeferredItem<BlockItem> RedIceItem = ItemRegister.registerSimpleBlockItem("scp009_red_ice", RedIce);

    public static final DeferredItem<Scp009CrystalItem> Shard = ItemRegister.register("scp009_shard",
            () -> new Scp009CrystalItem(RedIceGrade.Shard, new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));
    public static final DeferredItem<Scp009CrystalItem> Fragment = ItemRegister.register("scp009_fragment",
            () -> new Scp009CrystalItem(RedIceGrade.Fragment, new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredHolder<MobEffect, DoomedEffect> Doomed = MobEffectRegister.register("scp009_doomed", DoomedEffect::new);

    public static final DeferredHolder<SoundEvent, SoundEvent> Freeze = sound("scp009_freeze");
    public static final DeferredHolder<SoundEvent, SoundEvent> FreezeChime = sound("scp009_freeze_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> Crackle = sound("scp009_crackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Exposure = sound("scp009_exposure");
    public static final DeferredHolder<SoundEvent, SoundEvent> ExposureSting = sound("scp009_exposure_sting");
    public static final DeferredHolder<SoundEvent, SoundEvent> Heartbeat = sound("scp009_heartbeat");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoomCrackle = sound("scp009_doom_crackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Shatter = sound("scp009_shatter");
    public static final DeferredHolder<SoundEvent, SoundEvent> ShatterChime = sound("scp009_shatter_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> Warning = sound("scp009_warning");
    public static final DeferredHolder<SoundEvent, SoundEvent> Settle = sound("scp009_settle");

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Glint = ParticleRegister.register("scp009_glint", () -> new SimpleParticleType(false));

    public static GameRules.Key<GameRules.BooleanValue> CrystalSpread;

    public static final DeferredHolder<EntityType<?>, EntityType<Scp009Statue>> Statue = EntityTypeRegister.register("scp009_statue",
            () -> EntityType.Builder.<Scp009Statue>of(Scp009Statue::new, MobCategory.MISC)
                    .sized(Scp009Statue.DefaultWidth, Scp009Statue.DefaultHeight)
                    .fireImmune()
                    .noSummon()
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .build("scp009_statue"));

    private Scp009Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        MobEffectRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        StorageBehaviours.register(Scp009CrystalItem::is, Scp009Containment.Instance);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
        ModEventBus.addListener(FMLCommonSetupEvent.class, Event -> Event.enqueueWork(Scp009Registry::registerRules));
    }

    private static void registerRules() {
        if (CrystalSpread == null) {
            CrystalSpread = GameRules.register("scp009CrystalSpread", GameRules.Category.UPDATES, GameRules.BooleanValue.create(false));
        }
    }

    public static boolean crystalSpread(GameRules Rules) {
        if (CrystalSpread == null) {
            return false;
        }
        GameRules.BooleanValue Rule = Rules.getRule(CrystalSpread);
        return Rule != null && Rule.get();
    }
}
