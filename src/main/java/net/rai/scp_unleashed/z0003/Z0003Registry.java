package net.rai.scp_unleashed.z0003;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.investigation.StartConditions;
import net.rai.scp_unleashed.storage.StorageBehaviours;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class Z0003Registry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("z_0003");
    public static final String TimeoutRuleName = "scpZ0003PlayerTimeout";
    public static final TagKey<EntityType<?>> Petrifiable = TagKey.create(Registries.ENTITY_TYPE, location("z0003_petrifiable"));
    public static final double NearEar = 2.0;
    @Nullable
    private static GameRules.Key<GameRules.BooleanValue> TimeoutRule;

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Z0003Arming>> Arming = ComponentRegister.registerComponentType("z0003_arming",
            Builder -> Builder.persistent(Z0003Arming.ArmingCodec).networkSynchronized(Z0003Arming.ArmingStreamCodec));

    public static final DeferredItem<Z0003DeviceItem> Device = ItemRegister.register("z0003_device",
            () -> new Z0003DeviceItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<Z0003VialItem> Vial = ItemRegister.register("z0003_vial",
            () -> new Z0003VialItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final DeferredHolder<EntityType<?>, EntityType<Z0003Device>> DeviceType = EntityTypeRegister.register("z0003_device",
            () -> EntityType.Builder.<Z0003Device>of(Z0003Device::new, MobCategory.MISC).sized(Z0003Device.Width, Z0003Device.Height)
                    .eyeHeight(Z0003Device.Height * 0.5f).fireImmune().clientTrackingRange(10).updateInterval(2).build("z0003_device"));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Z0003Statue>> Statue = AttachmentRegister.register("z0003_statue",
            () -> AttachmentType.builder(() -> new Z0003Statue(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, false, false, 0.0, 0.0, 0.0, 0L, Z0003Statue.NotCracking))
                    .serialize(Z0003Statue.StatueCodec).sync(Z0003Statue.StatueStreamCodec).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> Passing = AttachmentRegister.register("z0003_passing",
            () -> AttachmentType.builder(() -> 0L).sync((Holder, To) -> Holder == To, ByteBufCodecs.VAR_LONG).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Z0003Stride>> Stride = AttachmentRegister.register("z0003_stride",
            () -> AttachmentType.builder(Z0003Stride::new).build());

    public static final DeferredHolder<SoundEvent, SoundEvent> Arm = sound("z0003_arm");
    public static final DeferredHolder<SoundEvent, SoundEvent> ArmClick = sound("z0003_arm_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> Throw = sound("z0003_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> ThrowWhoosh = sound("z0003_throw_whoosh");
    public static final DeferredHolder<SoundEvent, SoundEvent> Bounce = sound("z0003_bounce");
    public static final DeferredHolder<SoundEvent, SoundEvent> BounceRing = sound("z0003_bounce_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> Activate = sound("z0003_activate");
    public static final DeferredHolder<SoundEvent, SoundEvent> ActivateChime = sound("z0003_activate_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> ActivateHum = sound("z0003_activate_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> Field = sound("z0003_field");
    public static final DeferredHolder<SoundEvent, SoundEvent> FieldRush = sound("z0003_field_rush");
    public static final DeferredHolder<SoundEvent, SoundEvent> Surge = sound("z0003_surge");
    public static final DeferredHolder<SoundEvent, SoundEvent> SurgeWhoosh = sound("z0003_surge_whoosh");
    public static final DeferredHolder<SoundEvent, SoundEvent> SurgeShimmer = sound("z0003_surge_shimmer");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwellShimmer = sound("z0003_swell_shimmer");
    public static final DeferredHolder<SoundEvent, SoundEvent> HoldHum = sound("z0003_hold_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> HoldThrob = sound("z0003_hold_throb");
    public static final DeferredHolder<SoundEvent, SoundEvent> Release = sound("z0003_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> ReleaseShimmer = sound("z0003_release_shimmer");
    public static final DeferredHolder<SoundEvent, SoundEvent> ReleaseWind = sound("z0003_release_wind");
    public static final DeferredHolder<SoundEvent, SoundEvent> Settle = sound("z0003_settle");
    public static final DeferredHolder<SoundEvent, SoundEvent> SettleFade = sound("z0003_settle_fade");
    public static final DeferredHolder<SoundEvent, SoundEvent> Petrify = sound("z0003_petrify");
    public static final DeferredHolder<SoundEvent, SoundEvent> PetrifyGrind = sound("z0003_petrify_grind");
    public static final DeferredHolder<SoundEvent, SoundEvent> PetrifySet = sound("z0003_petrify_set");
    public static final DeferredHolder<SoundEvent, SoundEvent> Crack = sound("z0003_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrackGrit = sound("z0003_crack_grit");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrackCreak = sound("z0003_crack_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrackTick = sound("z0003_crack_tick");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrackHeavy = sound("z0003_crack_heavy");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrackRumble = sound("z0003_crack_rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> Break = sound("z0003_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> BreakShatter = sound("z0003_break_shatter");
    public static final DeferredHolder<SoundEvent, SoundEvent> BreakDebris = sound("z0003_break_debris");
    public static final DeferredHolder<SoundEvent, SoundEvent> BreakCrumble = sound("z0003_break_crumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> TetherSnap = sound("z0003_tether_snap");
    public static final DeferredHolder<SoundEvent, SoundEvent> Yank = sound("z0003_yank");
    public static final DeferredHolder<SoundEvent, SoundEvent> YankWhip = sound("z0003_yank_whip");
    public static final DeferredHolder<SoundEvent, SoundEvent> VialUse = sound("z0003_vial");
    public static final DeferredHolder<SoundEvent, SoundEvent> VialPour = sound("z0003_vial_pour");
    public static final DeferredHolder<SoundEvent, SoundEvent> Pickup = sound("z0003_pickup");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ArmLayers = List.of(Arm, ArmClick);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ThrowLayers = List.of(Throw, ThrowWhoosh);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BounceLayers = List.of(Bounce, BounceRing);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ActivateLayers = List.of(Activate, ActivateChime, ActivateHum);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SurgeLayers = List.of(Surge, SurgeWhoosh, SurgeShimmer, FieldRush);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SwellLayers = List.of(Field, SwellShimmer);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HoldLayers = List.of(HoldHum, HoldThrob);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ReleaseLayers = List.of(Release, ReleaseShimmer, ReleaseWind);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SettleLayers = List.of(Settle, SettleFade);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PetrifyLayers = List.of(Petrify, PetrifyGrind, PetrifySet);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CreakLayers = List.of(CrackCreak, CrackTick);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CrackLayers = List.of(Crack, CrackGrit);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HeavyCrackLayers = List.of(CrackHeavy, CrackRumble);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BreakLayers = List.of(Break, BreakShatter, BreakDebris, BreakCrumble);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SnapLayers = List.of(TetherSnap);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> YankLayers = List.of(Yank, YankWhip);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> VialLayers = List.of(VialUse, VialPour);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PickupLayers = List.of(Pickup);

    private Z0003Registry() {
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        StartConditions.register(Z0003ProcuredCondition.TypeName, Z0003ProcuredCondition.ConditionCodec);
        StorageBehaviours.register(Z0003DeviceItem::is, Z0003Containment.Instance);
        ModEventBus.addListener(FMLCommonSetupEvent.class, Event -> Event.enqueueWork(Z0003Registry::registerRules));
    }

    private static void registerRules() {
        if (TimeoutRule == null) {
            TimeoutRule = GameRules.register(TimeoutRuleName, GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));
        }
    }

    public static boolean playersTimeOut(GameRules Rules) {
        if (TimeoutRule == null) {
            return false;
        }
        GameRules.BooleanValue Rule = Rules.getRule(TimeoutRule);
        return Rule != null && Rule.get();
    }

    public static void setPlayersTimeOut(MinecraftServer Server, boolean Value) {
        if (TimeoutRule != null) {
            Server.getGameRules().getRule(TimeoutRule).set(Value, Server);
        }
    }

    public static void play(Level World, Vec3 Where, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, SoundSource Source, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, Where.x, Where.y, Where.z, Layer.get(), Source, Volume, Pitch);
        }
    }

    public static void playAround(ServerLevel World, Vec3 Centre, double Reach, double Range, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, SoundSource Source,
                                  float Volume, float Pitch) {
        long Seed = World.getRandom().nextLong();
        for (ServerPlayer Listener : World.players()) {
            Vec3 Offset = Listener.getEyePosition().subtract(Centre);
            double Distance = Offset.length();
            if (Distance > Range) {
                continue;
            }
            Vec3 Spot = Distance < 1.0E-4 ? Centre : Centre.add(Offset.scale(Mth.clamp(Distance - NearEar, 0.0, Reach) / Distance));
            for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
                Listener.connection.send(new ClientboundSoundPacket(Layer, Source, Spot.x, Spot.y, Spot.z, Volume, Pitch, Seed));
            }
        }
    }

    public static void stopAround(ServerLevel World, Vec3 Centre, double Range, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, SoundSource Source) {
        for (ServerPlayer Listener : World.players()) {
            if (Listener.getEyePosition().distanceTo(Centre) > Range) {
                continue;
            }
            for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
                Listener.connection.send(new ClientboundStopSoundPacket(Layer.getId(), Source));
            }
        }
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }
}
