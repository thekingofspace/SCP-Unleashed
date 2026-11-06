package net.rai.scp_unleashed.scp261;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.stranger.StrangerEncounters;
import net.rai.scp_unleashed.archive.investigation.StartConditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class Scp261Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<MobEffect> MobEffectRegister = DeferredRegister.create(Registries.MOB_EFFECT, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_261");
    public static final ResourceKey<DamageType> StaticDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp261_static"));
    public static final TagKey<EntityType<?>> Fearless = TagKey.create(Registries.ENTITY_TYPE, location("scp261_fearless"));

    public static final DeferredHolder<SoundEvent, SoundEvent> Coin = sound("scp261_coin");
    public static final DeferredHolder<SoundEvent, SoundEvent> CoinDrop = sound("scp261_coin_drop");
    public static final DeferredHolder<SoundEvent, SoundEvent> Whirr = sound("scp261_whirr");
    public static final DeferredHolder<SoundEvent, SoundEvent> WhirrGrind = sound("scp261_whirr_grind");
    public static final DeferredHolder<SoundEvent, SoundEvent> Thunk = sound("scp261_thunk");
    public static final DeferredHolder<SoundEvent, SoundEvent> ThunkBox = sound("scp261_thunk_box");
    public static final DeferredHolder<SoundEvent, SoundEvent> Eject = sound("scp261_eject");
    public static final DeferredHolder<SoundEvent, SoundEvent> Swallow = sound("scp261_swallow");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwallowBuzz = sound("scp261_swallow_buzz");
    public static final DeferredHolder<SoundEvent, SoundEvent> Bonus = sound("scp261_bonus");
    public static final DeferredHolder<SoundEvent, SoundEvent> Refuse = sound("scp261_refuse");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hum = sound("scp261_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> Chew = sound("scp261_chew");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChewPop = sound("scp261_chew_pop");
    public static final DeferredHolder<SoundEvent, SoundEvent> GumPop = sound("scp261_gum_pop");
    public static final DeferredHolder<SoundEvent, SoundEvent> GumBounce = sound("scp261_gum_bounce");
    public static final DeferredHolder<SoundEvent, SoundEvent> Zap = sound("scp261_zap");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZapCrackle = sound("scp261_zap_crackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Flip = sound("scp261_flip");
    public static final DeferredHolder<SoundEvent, SoundEvent> FlipShimmer = sound("scp261_flip_shimmer");
    public static final DeferredHolder<SoundEvent, SoundEvent> Freeze = sound("scp261_freeze");
    public static final DeferredHolder<SoundEvent, SoundEvent> FreezeTinkle = sound("scp261_freeze_tinkle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Ink = sound("scp261_ink");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hiccup = sound("scp261_hiccup");
    public static final DeferredHolder<SoundEvent, SoundEvent> HiccupPlop = sound("scp261_hiccup_plop");
    public static final DeferredHolder<SoundEvent, SoundEvent> Thud = sound("scp261_thud");
    public static final DeferredHolder<SoundEvent, SoundEvent> ThudCrunch = sound("scp261_thud_crunch");
    public static final DeferredHolder<SoundEvent, SoundEvent> Shrink = sound("scp261_shrink");
    public static final DeferredHolder<SoundEvent, SoundEvent> ShrinkTinkle = sound("scp261_shrink_tinkle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rewind = sound("scp261_rewind");
    public static final DeferredHolder<SoundEvent, SoundEvent> RewindHum = sound("scp261_rewind_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> Poof = sound("scp261_poof");
    public static final DeferredHolder<SoundEvent, SoundEvent> PoofPuff = sound("scp261_poof_puff");
    public static final DeferredHolder<SoundEvent, SoundEvent> Summon = sound("scp261_summon");
    public static final DeferredHolder<SoundEvent, SoundEvent> SummonChime = sound("scp261_summon_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> Dread = sound("scp261_dread");
    public static final DeferredHolder<SoundEvent, SoundEvent> DreadHum = sound("scp261_dread_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> Growl = sound("scp261_growl");
    public static final DeferredHolder<SoundEvent, SoundEvent> Sated = sound("scp261_sated");
    public static final DeferredHolder<SoundEvent, SoundEvent> Lucky = sound("scp261_lucky");
    public static final DeferredHolder<SoundEvent, SoundEvent> Sizzle = sound("scp261_sizzle");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CoinCue = List.of(Coin, CoinDrop);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WhirrCue = List.of(Whirr, WhirrGrind);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ThunkCue = List.of(Thunk, ThunkBox);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SwallowCue = List.of(Swallow, SwallowBuzz);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ChewCue = List.of(Chew, ChewPop);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> GumCue = List.of(GumPop, GumBounce);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ZapCue = List.of(Zap, ZapCrackle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> FlipCue = List.of(Flip, FlipShimmer);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> FreezeCue = List.of(Freeze, FreezeTinkle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HiccupCue = List.of(Hiccup, HiccupPlop);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ThudCue = List.of(Thud, ThudCrunch);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ShrinkCue = List.of(Shrink, ShrinkTinkle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RewindCue = List.of(Rewind, RewindHum);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PoofCue = List.of(Poof, PoofPuff);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SummonCue = List.of(Summon, SummonChime);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DreadCue = List.of(Dread, DreadHum);

    public static final DeferredHolder<MobEffect, Scp261Effect> Buoyant = MobEffectRegister.register("scp261_buoyant",
            () -> (Scp261Effect) new Scp261Effect(MobEffectCategory.BENEFICIAL, 0xF28BC4)
                    .addAttributeModifier(Attributes.GRAVITY, location("effect.scp261_buoyant.gravity"), -0.6, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                    .addAttributeModifier(Attributes.SAFE_FALL_DISTANCE, location("effect.scp261_buoyant.fall"), 8.0, AttributeModifier.Operation.ADD_VALUE)
                    .addAttributeModifier(Attributes.JUMP_STRENGTH, location("effect.scp261_buoyant.jump"), 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    public static final DeferredHolder<MobEffect, Scp261Effect> Echoing = MobEffectRegister.register("scp261_echoing",
            () -> new Scp261Effect(MobEffectCategory.NEUTRAL, 0x3FB7B0));
    public static final DeferredHolder<MobEffect, Scp261Effect> Charged = MobEffectRegister.register("scp261_charged",
            () -> new Scp261Effect(MobEffectCategory.BENEFICIAL, 0xF2D43A, 8, Scp261Effects::crackle));
    public static final DeferredHolder<MobEffect, Scp261Effect> Topsy = MobEffectRegister.register("scp261_topsy",
            () -> new Scp261Effect(MobEffectCategory.HARMFUL, 0xC9D6E2));
    public static final DeferredHolder<MobEffect, Scp261Effect> Hollow = MobEffectRegister.register("scp261_hollow",
            () -> new Scp261Effect(MobEffectCategory.NEUTRAL, 0x8A5A34));
    public static final DeferredHolder<MobEffect, Scp261Effect> Frostfoot = MobEffectRegister.register("scp261_frostfoot",
            () -> new Scp261Effect(MobEffectCategory.BENEFICIAL, 0x8FD8F2, 2, Scp261Effects::frost));
    public static final DeferredHolder<MobEffect, Scp261Effect> Fortunate = MobEffectRegister.register("scp261_fortunate",
            () -> (Scp261Effect) new Scp261Effect(MobEffectCategory.BENEFICIAL, 0x4FB04A)
                    .addAttributeModifier(Attributes.LUCK, location("effect.scp261_fortunate.luck"), 2.0, AttributeModifier.Operation.ADD_VALUE));
    public static final DeferredHolder<MobEffect, Scp261Effect> Smoldering = MobEffectRegister.register("scp261_smoldering",
            () -> new Scp261Effect(MobEffectCategory.NEUTRAL, 0xE2602A, 3, Scp261Effects::embers));
    public static final DeferredHolder<MobEffect, Scp261Effect> Inksight = MobEffectRegister.register("scp261_inksight",
            () -> new Scp261Effect(MobEffectCategory.NEUTRAL, 0x4A3A78, Scp261Effects.InkInterval, Scp261Effects::inksight));
    public static final DeferredHolder<MobEffect, Scp261Effect> Heavy = MobEffectRegister.register("scp261_heavy",
            () -> (Scp261Effect) new Scp261Effect(MobEffectCategory.HARMFUL, 0x7C8592, 1, Scp261Effects::sink)
                    .addAttributeModifier(Attributes.GRAVITY, location("effect.scp261_heavy.gravity"), 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                    .addAttributeModifier(Attributes.JUMP_STRENGTH, location("effect.scp261_heavy.jump"), 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                    .addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, location("effect.scp261_heavy.knockback"), 0.9, AttributeModifier.Operation.ADD_VALUE)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, location("effect.scp261_heavy.speed"), -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.SAFE_FALL_DISTANCE, location("effect.scp261_heavy.fall"), -1.0, AttributeModifier.Operation.ADD_VALUE));
    public static final DeferredHolder<MobEffect, Scp261Effect> Borrowed = MobEffectRegister.register("scp261_borrowed",
            () -> new Scp261Effect(MobEffectCategory.HARMFUL, 0xB39AE0));
    public static final DeferredHolder<MobEffect, Scp261Effect> Pocket = MobEffectRegister.register("scp261_pocket",
            () -> (Scp261Effect) new Scp261Effect(MobEffectCategory.NEUTRAL, 0xD8A25A)
                    .addAttributeModifier(Attributes.SCALE, location("effect.scp261_pocket.scale"), -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    public static final DeferredHolder<MobEffect, Scp261Effect> Rewinding = MobEffectRegister.register("scp261_rewinding",
            () -> new Scp261Effect(MobEffectCategory.NEUTRAL, 0xC8324A, 10, Scp261Effects::rewindTrail));
    public static final DeferredHolder<MobEffect, Scp261Effect> Hiccups = MobEffectRegister.register("scp261_hiccups",
            () -> new Scp261Effect(MobEffectCategory.HARMFUL, 0xFF9A2E, Scp261Effects.HiccupInterval, Scp261Effects::hiccup));
    public static final DeferredHolder<MobEffect, Scp261Effect> Dreadful = MobEffectRegister.register("scp261_dread",
            () -> new Scp261Effect(MobEffectCategory.BENEFICIAL, 0x5E8F3A, Scp261Dread.Interval, Scp261Dread::pulse));

    public static final DeferredBlock<Scp261VendingBlock> Vending = BlockRegister.register("scp261_vending",
            () -> new Scp261VendingBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(4.0f, 1200.0f).sound(SoundType.METAL)
                    .noOcclusion().pushReaction(PushReaction.BLOCK).lightLevel(Scp261VendingBlock::lightOf)
                    .isValidSpawn((State, World, Pos, Type) -> false)));
    public static final DeferredItem<BlockItem> VendingItem = ItemRegister.register("scp261_vending",
            () -> new BlockItem(Vending.get(), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp261VendingBlockEntity>> VendingEntity = BlockEntityTypeRegister.register("scp261_vending",
            () -> BlockEntityType.Builder.of(Scp261VendingBlockEntity::new, Vending.get()).build(null));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CompoundTag>> RewindAnchor = AttachmentRegister.register("scp261_rewind",
            () -> AttachmentType.builder(() -> new CompoundTag()).serialize(CompoundTag.CODEC, Saved -> !Saved.isEmpty()).build());

    private static final Map<Scp261Snack, DeferredItem<Scp261SnackItem>> Snacks = new EnumMap<>(Scp261Snack.class);
    private static final List<DeferredItem<Scp261SnackItem>> SnackOrder = new ArrayList<>();

    static {
        for (Scp261Snack Treat : Scp261Snack.values()) {
            DeferredItem<Scp261SnackItem> Registered = ItemRegister.register(Treat.getId(),
                    () -> new Scp261SnackItem(Treat, new Item.Properties().stacksTo(16).rarity(Treat.getTier().getRarity())));
            Snacks.put(Treat, Registered);
            SnackOrder.add(Registered);
        }
    }

    private Scp261Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static Scp261SnackItem snack(Scp261Snack Treat) {
        return Snacks.get(Treat).get();
    }

    public static List<DeferredItem<Scp261SnackItem>> snacks() {
        return Collections.unmodifiableList(SnackOrder);
    }

    public static void playCue(Level World, Vec3 Where, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, SoundSource Source, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, Where.x, Where.y, Where.z, Layer.get(), Source, Volume, Pitch);
        }
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        MobEffectRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        StartConditions.register(Scp261ArrivalCondition.TypeName, Scp261ArrivalCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
