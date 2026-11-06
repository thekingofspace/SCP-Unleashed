package net.rai.scp_unleashed.scp087;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.StartConditions;

public final class Scp087Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_087");

    public static final DeferredBlock<Scp087SealedBlock> Concrete = BlockRegister.register("scp087_concrete",
            () -> new Scp087SealedBlock(Scp087SealedBlock.sealed(MapColor.STONE).sound(SoundType.STONE)));
    public static final DeferredBlock<Scp087SealedBlock> Floor = BlockRegister.register("scp087_floor",
            () -> new Scp087SealedBlock(Scp087SealedBlock.sealed(MapColor.COLOR_GRAY).sound(SoundType.STONE)));
    public static final DeferredBlock<Scp087StairBlock> Stairs = BlockRegister.register("scp087_stairs",
            () -> new Scp087StairBlock(Floor.get().defaultBlockState(), Scp087SealedBlock.sealed(MapColor.COLOR_GRAY).sound(SoundType.STONE)));
    public static final DeferredBlock<Scp087RailBlock> Rail = BlockRegister.register("scp087_rail",
            () -> new Scp087RailBlock(Scp087SealedBlock.sealed(MapColor.METAL).sound(SoundType.METAL).noOcclusion().noCollission()));
    public static final DeferredBlock<Scp087DoorBlock> Door = BlockRegister.register("scp087_door",
            () -> new Scp087DoorBlock(Scp087SealedBlock.sealed(MapColor.METAL).sound(SoundType.NETHERITE_BLOCK).noOcclusion()));

    public static final DeferredItem<BlockItem> ConcreteItem = ItemRegister.register("scp087_concrete",
            () -> new BlockItem(Concrete.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> FloorItem = ItemRegister.register("scp087_floor",
            () -> new BlockItem(Floor.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> StairsItem = ItemRegister.register("scp087_stairs",
            () -> new BlockItem(Stairs.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> RailItem = ItemRegister.register("scp087_rail",
            () -> new BlockItem(Rail.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> DoorItem = ItemRegister.register("scp087_door",
            () -> new BlockItem(Door.get(), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp087Face>> Face = EntityTypeRegister.register("scp087_face",
            () -> EntityType.Builder.<Scp087Face>of(Scp087Face::new, MobCategory.MISC).sized(Scp087Face.Width, Scp087Face.Height).noSave().noSummon().fireImmune()
                    .clientTrackingRange(4).updateInterval(5).build("scp087_face"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp087Hunter>> Hunter = EntityTypeRegister.register("scp087_hunter",
            () -> EntityType.Builder.<Scp087Hunter>of(Scp087Hunter::new, MobCategory.MONSTER).sized(Scp087Hunter.Width, Scp087Hunter.Height)
                    .eyeHeight(Scp087Hunter.EyeHeight).clientTrackingRange(8).build("scp087_hunter"));

    public static final DeferredItem<DeferredSpawnEggItem> HunterEgg = ItemRegister.register("scp087_spawn_egg",
            () -> new DeferredSpawnEggItem(Hunter, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));

    public static final DeferredHolder<SoundEvent, SoundEvent> DoorOpen = sound("scp087_door_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoorShut = sound("scp087_door_shut");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoorLow = sound("scp087_door_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Drone = sound("scp087_drone");
    public static final DeferredHolder<SoundEvent, SoundEvent> Laugh = sound("scp087_laugh");
    public static final DeferredHolder<SoundEvent, SoundEvent> LaughEcho = sound("scp087_laugh_echo");
    public static final DeferredHolder<SoundEvent, SoundEvent> Footsteps = sound("scp087_footsteps");
    public static final DeferredHolder<SoundEvent, SoundEvent> Breath = sound("scp087_breath");
    public static final DeferredHolder<SoundEvent, SoundEvent> FaceVanish = sound("scp087_face_vanish");
    public static final DeferredHolder<SoundEvent, SoundEvent> HunterAmbient = sound("scp087_hunter_ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> HunterStep = sound("scp087_hunter_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> HunterWindup = sound("scp087_hunter_windup");
    public static final DeferredHolder<SoundEvent, SoundEvent> HunterLunge = sound("scp087_hunter_lunge");
    public static final DeferredHolder<SoundEvent, SoundEvent> HunterSwipe = sound("scp087_hunter_swipe");
    public static final DeferredHolder<SoundEvent, SoundEvent> HunterHurt = sound("scp087_hunter_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HunterDeath = sound("scp087_hunter_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> HunterDeathLow = sound("scp087_hunter_death_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Silence = sound("scp087_silence");

    private Scp087Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        StartConditions.register(Scp087SiteCondition.TypeName, Scp087SiteCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp087Entrance::spawn);
    }
}
