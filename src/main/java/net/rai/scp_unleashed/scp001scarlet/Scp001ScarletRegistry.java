package net.rai.scp_unleashed.scp001scarlet;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.StartConditions;
import net.rai.scp_unleashed.faction.Factions;
import net.rai.scp_unleashed.scp001scarlet.cult.ScarletBrazierBlock;
import net.rai.scp_unleashed.scp001scarlet.cult.ScarletCultFaction;
import net.rai.scp_unleashed.scp001scarlet.cult.ScarletCultist;
import net.rai.scp_unleashed.scp001scarlet.cult.ScarletEffigyItem;
import net.rai.scp_unleashed.scp001scarlet.cult.ScarletScorchBlock;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletAnchorBlock;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletGlyphBlock;

public final class Scp001ScarletRegistry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_001_scarlet");
    public static final ResourceLocation Source = location("scp001_scarlet");

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> Seen = AttachmentRegister.register("scp001_scarlet_seen",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).copyOnDeath().build());

    public static final DeferredHolder<EntityType<?>, EntityType<ScarletCultist>> CultistType = EntityTypeRegister.register("scarlet_cultist",
            () -> EntityType.Builder.<ScarletCultist>of(ScarletCultist::new, MobCategory.MONSTER).sized(0.6f, 2.05f).eyeHeight(1.7f).clientTrackingRange(10)
                    .build("scarlet_cultist"));

    public static final DeferredBlock<ScarletBrazierBlock> Brazier = BlockRegister.register("scp001_scarlet_brazier",
            () -> new ScarletBrazierBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(2.5f, 6.0f).requiresCorrectToolForDrops()
                    .sound(SoundType.LANTERN).noOcclusion().lightLevel(State -> State.getValue(ScarletBrazierBlock.Lit) ? 11 : 0).pushReaction(PushReaction.DESTROY)));
    public static final DeferredItem<BlockItem> BrazierItem = ItemRegister.register("scp001_scarlet_brazier", () -> new BlockItem(Brazier.get(), new Item.Properties()));

    public static final DeferredBlock<ScarletScorchBlock> Scorch = BlockRegister.register("scp001_scarlet_scorch",
            () -> new ScarletScorchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.2f).noCollission().noOcclusion()
                    .sound(SoundType.SAND).pushReaction(PushReaction.DESTROY).replaceable()));

    public static final DeferredBlock<ScarletGlyphBlock> GlyphStone = BlockRegister.register("scp001_scarlet_glyph_stone",
            () -> new ScarletGlyphBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(3.0f, 9.0f).requiresCorrectToolForDrops()
                    .sound(SoundType.POLISHED_DEEPSLATE).lightLevel(State -> 5)));
    public static final DeferredItem<BlockItem> GlyphStoneItem = ItemRegister.register("scp001_scarlet_glyph_stone",
            () -> new BlockItem(GlyphStone.get(), new Item.Properties()));

    public static final DeferredBlock<ScarletAnchorBlock> ChainAnchor = BlockRegister.register("scp001_scarlet_chain_anchor",
            () -> new ScarletAnchorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(5.0f, 1200.0f).requiresCorrectToolForDrops()
                    .sound(SoundType.CHAIN).noOcclusion().pushReaction(PushReaction.BLOCK).isValidSpawn((State, Getter, Pos, Type) -> false)));

    public static final DeferredItem<ScarletEffigyItem> Effigy = ItemRegister.register("scp001_scarlet_effigy",
            () -> new ScarletEffigyItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<DeferredSpawnEggItem> CultistEgg = ItemRegister.register("scarlet_cultist_spawn_egg",
            () -> new DeferredSpawnEggItem(CultistType, 0x5C1A1F, 0x8E959A, new Item.Properties()));

    public static final DeferredHolder<SoundEvent, SoundEvent> ChainSnap = sound("scp001_scarlet_chain_snap");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChainRing = sound("scp001_scarlet_chain_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChainRumble = sound("scp001_scarlet_chain_rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> Chant = sound("scp001_scarlet_chant");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChantDrone = sound("scp001_scarlet_chant_drone");
    public static final DeferredHolder<SoundEvent, SoundEvent> Kindle = sound("scp001_scarlet_kindle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Vanish = sound("scp001_scarlet_vanish");
    public static final DeferredHolder<SoundEvent, SoundEvent> VanishHiss = sound("scp001_scarlet_vanish_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> Toll = sound("scp001_scarlet_toll");
    public static final DeferredHolder<SoundEvent, SoundEvent> TollThunder = sound("scp001_scarlet_toll_thunder");
    public static final DeferredHolder<SoundEvent, SoundEvent> Xk2Ambient = sound("scp001_scarlet_xk2_ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> CultistAmbient = sound("scarlet_cultist_ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> CultistHurt = sound("scarlet_cultist_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> CultistDeath = sound("scarlet_cultist_death");

    private Scp001ScarletRegistry() {
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
        SoundEventRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        StartConditions.register(ScarletReadCondition.TypeName, ScarletReadCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, ScarletReadCondition::surface);
        Factions.register(ScarletCultFaction.Instance);
    }
}
