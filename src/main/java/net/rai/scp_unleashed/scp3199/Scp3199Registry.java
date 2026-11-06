package net.rai.scp_unleashed.scp3199;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.StartConditions;
import net.rai.scp_unleashed.stasis.StasisPods;

import java.util.List;

public final class Scp3199Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_3199");

    public static final DeferredHolder<EntityType<?>, EntityType<Scp3199>> Scp3199Type = EntityTypeRegister.register("scp3199",
            () -> EntityType.Builder.<Scp3199>of(Scp3199::new, MobCategory.MONSTER)
                    .sized(Scp3199.Width, Scp3199.Height)
                    .eyeHeight(Scp3199.EyeHeight)
                    .clientTrackingRange(10)
                    .build("scp3199"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp3199Egg>> EggType = EntityTypeRegister.register("scp3199_egg",
            () -> EntityType.Builder.<Scp3199Egg>of(Scp3199Egg::new, MobCategory.MISC)
                    .sized(Scp3199Egg.Width, Scp3199Egg.Height)
                    .fireImmune()
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build("scp3199_egg"));

    public static final DeferredBlock<Scp3199EggBlock> Nest = BlockRegister.register("scp3199_egg",
            () -> new Scp3199EggBlock(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).noCollission().noOcclusion().strength(-1.0f, 3600000.0f).noLootTable()
                    .pushReaction(PushReaction.BLOCK).sound(SoundType.FROGSPAWN).isValidSpawn((State, World, Pos, Type) -> false)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp3199EggBlockEntity>> NestEntity = BlockEntityTypeRegister.register("scp3199_egg",
            () -> BlockEntityType.Builder.of(Scp3199EggBlockEntity::new, Nest.get()).build(null));

    public static final DeferredItem<DeferredSpawnEggItem> Egg = ItemRegister.register("scp3199_spawn_egg",
            () -> new DeferredSpawnEggItem(Scp3199Type, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<SoundEvent, SoundEvent> Shriek = sound("scp3199_shriek");
    public static final DeferredHolder<SoundEvent, SoundEvent> ShriekLow = sound("scp3199_shriek_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> ShriekRasp = sound("scp3199_shriek_rasp");
    public static final DeferredHolder<SoundEvent, SoundEvent> Attack = sound("scp3199_attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> AttackSnap = sound("scp3199_attack_snap");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp3199_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HurtWet = sound("scp3199_hurt_wet");
    public static final DeferredHolder<SoundEvent, SoundEvent> Death = sound("scp3199_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DeathWet = sound("scp3199_death_wet");
    public static final DeferredHolder<SoundEvent, SoundEvent> Step = sound("scp3199_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> StepWet = sound("scp3199_step_wet");
    public static final DeferredHolder<SoundEvent, SoundEvent> Lay = sound("scp3199_lay");
    public static final DeferredHolder<SoundEvent, SoundEvent> LaySquelch = sound("scp3199_lay_squelch");
    public static final DeferredHolder<SoundEvent, SoundEvent> Gulp = sound("scp3199_gulp");
    public static final DeferredHolder<SoundEvent, SoundEvent> Cackle = sound("scp3199_cackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hatch = sound("scp3199_hatch");
    public static final DeferredHolder<SoundEvent, SoundEvent> HatchCrack = sound("scp3199_hatch_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> Pulse = sound("scp3199_pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> Squish = sound("scp3199_squish");
    public static final DeferredHolder<SoundEvent, SoundEvent> Dissolve = sound("scp3199_dissolve");
    public static final DeferredHolder<SoundEvent, SoundEvent> DissolveFizz = sound("scp3199_dissolve_fizz");
    public static final DeferredHolder<SoundEvent, SoundEvent> Arrive = sound("scp3199_arrive");
    public static final DeferredHolder<SoundEvent, SoundEvent> ArriveFar = sound("scp3199_arrive_far");
    public static final DeferredHolder<SoundEvent, SoundEvent> Swim = sound("scp3199_swim");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ShriekLayers = List.of(Shriek, ShriekLow, ShriekRasp);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> AttackLayers = List.of(Attack, AttackSnap);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HurtLayers = List.of(Hurt, HurtWet);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DeathLayers = List.of(Death, DeathWet);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StepLayers = List.of(Step, StepWet);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HatchLayers = List.of(Hatch, HatchCrack);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DissolveLayers = List.of(Dissolve, DissolveFizz);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ArriveLayers = List.of(Arrive, ArriveFar);

    private Scp3199Registry() {
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
        Scp3199Rules.register(ModEventBus);
        StartConditions.register(Scp3199ArrivalCondition.TypeName, Scp3199ArrivalCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp3199ArrivalCondition::manifest);
        StasisPods.addCaptureVeto(Target -> Target instanceof Scp3199Egg || Target instanceof Scp3199 Creature && (Creature.isLaying() || Creature.nesting().laying() >= 0));
        StasisPods.addReleaseHook((World, Spawned) -> {
            if (Spawned instanceof Scp3199 Creature) {
                Creature.nesting().unsettle(Creature);
            }
        });
    }
}
