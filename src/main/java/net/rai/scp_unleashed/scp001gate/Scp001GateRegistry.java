package net.rai.scp_unleashed.scp001gate;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.StartConditions;

import java.util.List;

public final class Scp001GateRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final ResourceLocation DocumentId = location("scp_001_gate");
    public static final ResourceKey<DamageType> StrikeDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp001_gate_strike"));
    public static final TagKey<EntityType<?>> Avoiders = TagKey.create(Registries.ENTITY_TYPE, location("gate_avoiders"));

    public static final DeferredBlock<Block> Stone = stone("scp001_gate_stone");
    public static final DeferredBlock<Block> StoneCarved = stone("scp001_gate_stone_carved");
    public static final DeferredBlock<Block> StoneWorn = stone("scp001_gate_stone_worn");
    public static final DeferredBlock<Block> Keystone = stone("scp001_gate_keystone");
    public static final DeferredBlock<Scp001GatePortalBlock> Portal = BlockRegister.register("scp001_gate_portal", () -> new Scp001GatePortalBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_LIGHT_BLUE).strength(-1.0f, 3600000.0f).noLootTable().noCollission().noOcclusion().lightLevel(State -> 12).pushReaction(PushReaction.BLOCK)
            .isValidSpawn((State, Getter, Pos, Type) -> false).isSuffocating((State, Getter, Pos) -> false).isViewBlocking((State, Getter, Pos) -> false)));

    public static final DeferredItem<BlockItem> StoneItem = blockItem(Stone);
    public static final DeferredItem<BlockItem> StoneCarvedItem = blockItem(StoneCarved);
    public static final DeferredItem<BlockItem> StoneWornItem = blockItem(StoneWorn);
    public static final DeferredItem<BlockItem> KeystoneItem = blockItem(Keystone);
    public static final List<DeferredItem<BlockItem>> TabBlocks = List.of(StoneItem, StoneCarvedItem, StoneWornItem, KeystoneItem);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp001GatePortalBlockEntity>> PortalEntity = BlockEntityTypeRegister.register("scp001_gate_portal",
            () -> BlockEntityType.Builder.of(Scp001GatePortalBlockEntity::new, Portal.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp001GateGuardian>> GuardianType = EntityTypeRegister.register("scp001_gate_guardian",
            () -> EntityType.Builder.<Scp001GateGuardian>of(Scp001GateGuardian::new, MobCategory.MISC).sized(Scp001GateGuardian.Width, Scp001GateGuardian.Height)
                    .clientTrackingRange(20).updateInterval(2).fireImmune().build("scp001_gate_guardian"));

    public static final DeferredHolder<SoundEvent, SoundEvent> Warn = sound("scp001_gate.warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> WarnLow = sound("scp001_gate.warn_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Crackle = sound("scp001_gate.crackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Raise = sound("scp001_gate.raise");
    public static final DeferredHolder<SoundEvent, SoundEvent> StrikeSwing = sound("scp001_gate.strike_swing");
    public static final DeferredHolder<SoundEvent, SoundEvent> StrikeImpact = sound("scp001_gate.strike_impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> StrikeFlame = sound("scp001_gate.strike_flame");
    public static final DeferredHolder<SoundEvent, SoundEvent> Burn = sound("scp001_gate.burn");
    public static final DeferredHolder<SoundEvent, SoundEvent> Pass = sound("scp001_gate.pass");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hum = sound("scp001_gate.hum");

    private Scp001GateRegistry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredBlock<Block> stone(String Name) {
        return BlockRegister.register(Name, () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASEDRUM)
                .strength(-1.0f, 3600000.0f).noLootTable().sound(SoundType.DEEPSLATE_BRICKS).pushReaction(PushReaction.BLOCK).isValidSpawn((State, Getter, Pos, Type) -> false)));
    }

    private static DeferredItem<BlockItem> blockItem(DeferredBlock<Block> Owner) {
        return ItemRegister.register(Owner.getId().getPath(), () -> new BlockItem(Owner.get(), new Item.Properties().rarity(Rarity.EPIC)));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        StartConditions.register(Scp001GateCondition.TypeName, Scp001GateCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp001GateSites::confirm);
    }
}
