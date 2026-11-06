package net.rai.scp_unleashed.z0004;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.investigation.StartConditions;

import java.util.List;

public final class Z0004Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("z_0004");
    public static final ResourceKey<LootTable> RewardTable = ResourceKey.create(Registries.LOOT_TABLE, location("gameplay/z0004_reward"));

    public static final DeferredBlock<Z0004ChestBlock> Chest = BlockRegister.register("z0004_chest",
            () -> new Z0004ChestBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5f, 1200.0f).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredItem<BlockItem> ChestItem = ItemRegister.register("z0004_chest",
            () -> new BlockItem(Chest.get(), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Z0004ChestBlockEntity>> ChestEntity = BlockEntityTypeRegister.register("z0004_chest",
            () -> BlockEntityType.Builder.of(Z0004ChestBlockEntity::new, Chest.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<Z0004Ship>> Ship = EntityTypeRegister.register("z0004_ship",
            () -> EntityType.Builder.<Z0004Ship>of(Z0004Ship::new, MobCategory.MISC).sized(Z0004Ship.Width, Z0004Ship.Height).eyeHeight(Z0004Ship.Height * 0.6f)
                    .fireImmune().clientTrackingRange(16).updateInterval(2).build("z0004_ship"));
    public static final DeferredHolder<EntityType<?>, EntityType<Z0004Pirate>> Crew = EntityTypeRegister.register("z0004_crew",
            () -> EntityType.Builder.<Z0004Pirate>of(Z0004Pirate::new, MobCategory.MONSTER).sized(Z0004Pirate.Width, Z0004Pirate.Height)
                    .eyeHeight(Z0004Pirate.EyeHeight).clientTrackingRange(10).build("z0004_crew"));
    public static final DeferredHolder<EntityType<?>, EntityType<Z0004Captain>> Captain = EntityTypeRegister.register("z0004_captain",
            () -> EntityType.Builder.<Z0004Captain>of(Z0004Captain::new, MobCategory.MONSTER).sized(Z0004Captain.CaptainWidth, Z0004Captain.CaptainHeight)
                    .eyeHeight(Z0004Captain.CaptainEyeHeight).clientTrackingRange(10).build("z0004_captain"));
    public static final DeferredHolder<EntityType<?>, EntityType<Z0004Cannonball>> Cannonball = EntityTypeRegister.register("z0004_cannonball",
            () -> EntityType.Builder.<Z0004Cannonball>of(Z0004Cannonball::new, MobCategory.MISC).sized(0.5f, 0.5f).fireImmune().clientTrackingRange(10)
                    .updateInterval(1).build("z0004_cannonball"));
    public static final DeferredHolder<EntityType<?>, EntityType<Z0004Shot>> Shot = EntityTypeRegister.register("z0004_shot",
            () -> EntityType.Builder.<Z0004Shot>of(Z0004Shot::new, MobCategory.MISC).sized(0.2f, 0.2f).fireImmune().clientTrackingRange(8)
                    .updateInterval(1).build("z0004_shot"));

    public static final DeferredItem<DeferredSpawnEggItem> CutlassEgg = crewEgg("z0004_crew_cutlass_spawn_egg", Z0004Pirate.Cutlass);
    public static final DeferredItem<DeferredSpawnEggItem> HookEgg = crewEgg("z0004_crew_hook_spawn_egg", Z0004Pirate.Hook);
    public static final DeferredItem<DeferredSpawnEggItem> PistolEgg = crewEgg("z0004_crew_pistol_spawn_egg", Z0004Pirate.Pistol);
    public static final DeferredItem<DeferredSpawnEggItem> BlunderbussEgg = crewEgg("z0004_crew_blunderbuss_spawn_egg", Z0004Pirate.Blunderbuss);
    public static final DeferredItem<DeferredSpawnEggItem> CaptainEgg = ItemRegister.register("z0004_captain_spawn_egg",
            () -> new DeferredSpawnEggItem(Captain, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<Z0004CutlassItem> Cutlass = ItemRegister.register("z0004_cutlass",
            () -> new Z0004CutlassItem(Tiers.IRON, new Item.Properties().rarity(Rarity.RARE).attributes(Z0004CutlassItem.createAttributes(Tiers.IRON, 3, -2.2f))));

    public static final DeferredHolder<SoundEvent, SoundEvent> ChestOpen = sound("z0004_chest_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChestCreak = sound("z0004_chest_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChestClose = sound("z0004_chest_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> SeaAir = sound("z0004_sea_air");
    public static final DeferredHolder<SoundEvent, SoundEvent> Reward = sound("z0004_reward");
    public static final DeferredHolder<SoundEvent, SoundEvent> RewardCoins = sound("z0004_reward_coins");
    public static final DeferredHolder<SoundEvent, SoundEvent> Bell = sound("z0004_bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> BellResonate = sound("z0004_bell_resonate");
    public static final DeferredHolder<SoundEvent, SoundEvent> Horn = sound("z0004_horn");
    public static final DeferredHolder<SoundEvent, SoundEvent> HornLow = sound("z0004_horn_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Surface = sound("z0004_surface");
    public static final DeferredHolder<SoundEvent, SoundEvent> SurfaceSwell = sound("z0004_surface_swell");
    public static final DeferredHolder<SoundEvent, SoundEvent> Creak = sound("z0004_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> CreakRigging = sound("z0004_creak_rigging");
    public static final DeferredHolder<SoundEvent, SoundEvent> Sails = sound("z0004_sails");
    public static final DeferredHolder<SoundEvent, SoundEvent> Fuse = sound("z0004_fuse");
    public static final DeferredHolder<SoundEvent, SoundEvent> Cannon = sound("z0004_cannon");
    public static final DeferredHolder<SoundEvent, SoundEvent> CannonEcho = sound("z0004_cannon_echo");
    public static final DeferredHolder<SoundEvent, SoundEvent> Splash = sound("z0004_splash");
    public static final DeferredHolder<SoundEvent, SoundEvent> Impact = sound("z0004_impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> ShipHurt = sound("z0004_ship_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> ShipBreak = sound("z0004_ship_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> ShipBreakTimber = sound("z0004_ship_break_timber");
    public static final DeferredHolder<SoundEvent, SoundEvent> Sink = sound("z0004_sink");
    public static final DeferredHolder<SoundEvent, SoundEvent> SinkSwell = sound("z0004_sink_swell");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rattle = sound("z0004_rattle");
    public static final DeferredHolder<SoundEvent, SoundEvent> RattleCoral = sound("z0004_rattle_coral");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrewHurt = sound("z0004_crew_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrewDeath = sound("z0004_crew_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrewStep = sound("z0004_crew_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> Bellow = sound("z0004_bellow");
    public static final DeferredHolder<SoundEvent, SoundEvent> Swing = sound("z0004_swing");
    public static final DeferredHolder<SoundEvent, SoundEvent> Flintlock = sound("z0004_flintlock");
    public static final DeferredHolder<SoundEvent, SoundEvent> FlintlockWater = sound("z0004_flintlock_water");
    public static final DeferredHolder<SoundEvent, SoundEvent> Blunderbuss = sound("z0004_blunderbuss");
    public static final DeferredHolder<SoundEvent, SoundEvent> Dive = sound("z0004_dive");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> OpenLayers = List.of(ChestOpen, ChestCreak, SeaAir);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CloseLayers = List.of(ChestClose);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RewardLayers = List.of(Reward, RewardCoins);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BellLayers = List.of(Bell, BellResonate);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HornLayers = List.of(Horn, HornLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SurfaceLayers = List.of(Surface, SurfaceSwell);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CreakLayers = List.of(Creak, CreakRigging);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CannonLayers = List.of(Cannon, CannonEcho);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BreakLayers = List.of(ShipBreak, ShipBreakTimber);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SinkLayers = List.of(Sink, SinkSwell);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RattleLayers = List.of(Rattle, RattleCoral);

    private Z0004Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    private static DeferredItem<DeferredSpawnEggItem> crewEgg(String Name, int Variant) {
        return ItemRegister.register(Name, () -> {
            CompoundTag Data = new CompoundTag();
            Data.putString("id", location("z0004_crew").toString());
            Data.putInt(Z0004Pirate.VariantKey, Variant);
            return new DeferredSpawnEggItem(Crew, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.RARE).component(DataComponents.ENTITY_DATA, CustomData.of(Data)));
        });
    }

    public static void play(Level World, Vec3 Where, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, SoundSource Source, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, Where.x, Where.y, Where.z, Layer.get(), Source, Volume, Pitch);
        }
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
        ModEventBus.addListener(EntityAttributeCreationEvent.class, Z0004Registry::attributes);
        StartConditions.register(Z0004ProcuredCondition.TypeName, Z0004ProcuredCondition.ConditionCodec);
    }

    private static void attributes(EntityAttributeCreationEvent Event) {
        Event.put(Ship.get(), Z0004Ship.createAttributes().build());
        Event.put(Crew.get(), Z0004Pirate.createAttributes().build());
        Event.put(Captain.get(), Z0004Captain.createCaptainAttributes().build());
    }
}
