package net.rai.scp_unleashed.z0002;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.investigation.StartConditions;

import java.util.List;
import java.util.UUID;

public final class Z0002Registry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("z_0002");
    public static final ResourceKey<DamageType> SlashDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("z0002_slash"));
    public static final String EnabledRuleName = "scpRedFingerDomains";
    public static final float Unbreakable = 3600000.0f;
    public static final TicketType<UUID> Ticket = TicketType.create("scp_unleashed_z0002", UUID::compareTo);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> DormantUntil = ComponentRegister.registerComponentType("z0002_dormant_until",
            Builder -> Builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    public static final DeferredItem<Z0002FingerItem> Finger = ItemRegister.register("z0002_finger",
            () -> new Z0002FingerItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredBlock<Z0002BarrierBlock> Barrier = BlockRegister.register("z0002_barrier", () -> new Z0002BarrierBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK).strength(-1.0f, Unbreakable).noLootTable().sound(SoundType.AMETHYST).lightLevel(State -> 2)
            .pushReaction(PushReaction.BLOCK).isValidSpawn((State, Getter, Pos, Type) -> false)));
    public static final DeferredBlock<Z0002FloorBlock> Floor = BlockRegister.register("z0002_floor", () -> new Z0002FloorBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK).strength(-1.0f, Unbreakable).noLootTable().sound(SoundType.MUD).lightLevel(State -> 4)
            .pushReaction(PushReaction.BLOCK).isValidSpawn((State, Getter, Pos, Type) -> false)));

    public static final DeferredHolder<EntityType<?>, EntityType<Z0002Shrine>> Shrine = EntityRegister.register("z0002_shrine",
            () -> EntityType.Builder.<Z0002Shrine>of(Z0002Shrine::new, MobCategory.MISC)
                    .sized(Z0002Shrine.Width, Z0002Shrine.Height)
                    .eyeHeight(Z0002Shrine.EyeHeight)
                    .fireImmune()
                    .noSummon()
                    .clientTrackingRange(10)
                    .updateInterval(40)
                    .build("z0002_shrine"));
    public static final DeferredHolder<EntityType<?>, EntityType<Z0002Slash>> Slash = EntityRegister.register("z0002_slash",
            () -> EntityType.Builder.<Z0002Slash>of(Z0002Slash::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("z0002_slash"));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Vapour = ParticleRegister.register("z0002_vapour", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Spark = ParticleRegister.register("z0002_spark", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Shard = ParticleRegister.register("z0002_shard", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Mote = ParticleRegister.register("z0002_mote", () -> new SimpleParticleType(true));

    public static final DeferredHolder<SoundEvent, SoundEvent> Open = sound("z0002_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> OpenToll = sound("z0002_open_toll");
    public static final DeferredHolder<SoundEvent, SoundEvent> OpenCurse = sound("z0002_open_curse");
    public static final DeferredHolder<SoundEvent, SoundEvent> OpenOmen = sound("z0002_open_omen");
    public static final DeferredHolder<SoundEvent, SoundEvent> Slashing = sound("z0002_slash");
    public static final DeferredHolder<SoundEvent, SoundEvent> SlashRip = sound("z0002_slash_rip");
    public static final DeferredHolder<SoundEvent, SoundEvent> SlashHit = sound("z0002_slash_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> SlashPierce = sound("z0002_slash_pierce");
    public static final DeferredHolder<SoundEvent, SoundEvent> Crack = sound("z0002_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrackRing = sound("z0002_crack_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> Shatter = sound("z0002_shatter");
    public static final DeferredHolder<SoundEvent, SoundEvent> ShatterFall = sound("z0002_shatter_fall");
    public static final DeferredHolder<SoundEvent, SoundEvent> ShatterDepth = sound("z0002_shatter_depth");
    public static final DeferredHolder<SoundEvent, SoundEvent> Return = sound("z0002_return");
    public static final DeferredHolder<SoundEvent, SoundEvent> Breath = sound("z0002_breath");
    public static final DeferredHolder<SoundEvent, SoundEvent> Dormant = sound("z0002_dormant");
    public static final DeferredHolder<SoundEvent, SoundEvent> Windup = sound("z0002_windup");
    public static final DeferredHolder<SoundEvent, SoundEvent> WindupCharge = sound("z0002_windup_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> WindupDrone = sound("z0002_windup_drone");
    public static final DeferredHolder<SoundEvent, SoundEvent> Construct = sound("z0002_construct");
    public static final DeferredHolder<SoundEvent, SoundEvent> ConstructHum = sound("z0002_construct_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> Fizzle = sound("z0002_fizzle");
    public static final DeferredHolder<SoundEvent, SoundEvent> FizzleFade = sound("z0002_fizzle_fade");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> OpenLayers = List.of(Open, OpenToll, OpenCurse, OpenOmen);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SlashLayers = List.of(Slashing, SlashRip);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HitLayers = List.of(SlashHit, SlashPierce);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CrackLayers = List.of(Crack, CrackRing);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ShatterLayers = List.of(Shatter, ShatterFall, ShatterDepth);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WindupLayers = List.of(Windup, WindupCharge, WindupDrone);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ConstructLayers = List.of(Construct, ConstructHum);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> FizzleLayers = List.of(Fizzle, FizzleFade);

    public static GameRules.Key<GameRules.BooleanValue> EnabledRule;

    private Z0002Registry() {
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        BlockRegister.register(ModEventBus);
        EntityRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
        StartConditions.register(Z0002ProcuredCondition.TypeName, Z0002ProcuredCondition.ConditionCodec);
        ModEventBus.addListener(FMLCommonSetupEvent.class, Event -> Event.enqueueWork(Z0002Registry::registerRules));
    }

    private static void registerRules() {
        if (EnabledRule == null) {
            EnabledRule = GameRules.register(EnabledRuleName, GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
        }
    }

    public static boolean enabled(GameRules Rules) {
        return EnabledRule == null || Rules.getBoolean(EnabledRule);
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }
}
