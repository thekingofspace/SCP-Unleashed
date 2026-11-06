package net.rai.scp_unleashed.scp063;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

public final class Scp063Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_063");
    public static final ResourceKey<DamageType> BrushDamage = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp063_toothbrush"));
    public static final TagKey<Block> Organic = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp063_organic"));
    public static final TagKey<Block> Immune = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp063_immune"));
    public static final String ErosionRuleName = "scp063Erosion";

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> BrushTime = AttachmentRegister.register("scp063_brush_time",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT, Saved -> Saved > 0).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> Toothless = AttachmentRegister.register("scp063_toothless",
            () -> AttachmentType.builder(() -> Boolean.FALSE).serialize(Codec.BOOL, Saved -> Saved)
                    .sync((Holder, Viewer) -> Holder == Viewer, ByteBufCodecs.BOOL).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> Scrubbing = AttachmentRegister.register("scp063_scrubbing",
            () -> AttachmentType.builder(() -> Boolean.FALSE).sync(ByteBufCodecs.BOOL).build());

    public static final DeferredBlock<Scp063RestingBlock> Resting = BlockRegister.register("scp063_resting_toothbrush", () -> new Scp063RestingBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.2f).sound(SoundType.CANDLE).noCollission().noOcclusion()
                    .pushReaction(PushReaction.BLOCK).isValidSpawn((State, Getter, Pos, Type) -> false).isRedstoneConductor((State, Getter, Pos) -> false)));

    public static final DeferredItem<Scp063ToothbrushItem> Toothbrush = ItemRegister.register("scp063_toothbrush",
            () -> new Scp063ToothbrushItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp063RestingBlockEntity>> RestingEntity = BlockEntityRegister.register("scp063_resting_toothbrush",
            () -> BlockEntityType.Builder.of(Scp063RestingBlockEntity::new, Resting.get()).build(null));

    public static final DeferredHolder<SoundEvent, SoundEvent> Brush = sound("scp063_brush");
    public static final DeferredHolder<SoundEvent, SoundEvent> Foam = sound("scp063_foam");
    public static final DeferredHolder<SoundEvent, SoundEvent> Fresh = sound("scp063_fresh");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rattle = sound("scp063_rattle");
    public static final DeferredHolder<SoundEvent, SoundEvent> TeethGone = sound("scp063_teeth_gone");
    public static final DeferredHolder<SoundEvent, SoundEvent> TeethClatter = sound("scp063_teeth_clatter");
    public static final DeferredHolder<SoundEvent, SoundEvent> Gum = sound("scp063_gum");
    public static final DeferredHolder<SoundEvent, SoundEvent> TeethBack = sound("scp063_teeth_back");
    public static final DeferredHolder<SoundEvent, SoundEvent> Scrub = sound("scp063_scrub");
    public static final DeferredHolder<SoundEvent, SoundEvent> Grit = sound("scp063_grit");
    public static final DeferredHolder<SoundEvent, SoundEvent> Scrubbed = sound("scp063_scrubbed");
    public static final DeferredHolder<SoundEvent, SoundEvent> Settle = sound("scp063_settle");

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FoamSpeck = ParticleRegister.register("scp063_foam", () -> new SimpleParticleType(false));

    public static GameRules.Key<GameRules.BooleanValue> ErosionRule;

    private Scp063Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
        ModEventBus.addListener(FMLCommonSetupEvent.class, Event -> Event.enqueueWork(Scp063Registry::registerRules));
    }

    private static void registerRules() {
        if (ErosionRule == null) {
            ErosionRule = GameRules.register(ErosionRuleName, GameRules.Category.UPDATES, GameRules.BooleanValue.create(true));
        }
    }

    public static boolean erosion(GameRules Rules) {
        if (ErosionRule == null) {
            return true;
        }
        GameRules.BooleanValue Rule = Rules.getRule(ErosionRule);
        return Rule == null || Rule.get();
    }
}
