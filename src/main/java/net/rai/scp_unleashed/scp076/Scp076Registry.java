package net.rai.scp_unleashed.scp076;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
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

public final class Scp076Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_076");
    public static final ResourceKey<DamageType> SealedDamage = damage("scp076_sealed");
    public static final ResourceKey<DamageType> KnifeDamage = damage("scp076_knife");
    public static final ResourceKey<DamageType> BladesDamage = damage("scp076_blades");

    public static final DeferredBlock<Scp076SealedBlock> VaultStone = BlockRegister.register("scp076_vault_stone",
            () -> new Scp076SealedBlock(Scp076SealedBlock.sealed(MapColor.COLOR_BLACK).sound(SoundType.POLISHED_DEEPSLATE)));
    public static final DeferredBlock<Scp076DoorBlock> VaultDoor = BlockRegister.register("scp076_vault_door",
            () -> new Scp076DoorBlock(Scp076SealedBlock.sealed(MapColor.COLOR_BLACK).sound(SoundType.POLISHED_DEEPSLATE).noOcclusion()
                    .isRedstoneConductor((State, World, Pos) -> false).isSuffocating((State, World, Pos) -> !State.getValue(Scp076DoorBlock.Open))
                    .isViewBlocking((State, World, Pos) -> !State.getValue(Scp076DoorBlock.Open))));
    public static final DeferredBlock<Scp076CoffinBlock> Coffin = BlockRegister.register("scp076_coffin",
            () -> new Scp076CoffinBlock(Scp076SealedBlock.sealed(MapColor.COLOR_BLACK).sound(SoundType.DEEPSLATE_BRICKS).noOcclusion().lightLevel(State -> 2)
                    .isRedstoneConductor((State, World, Pos) -> false).isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false)));

    public static final DeferredItem<BlockItem> VaultStoneItem = ItemRegister.register("scp076_vault_stone",
            () -> new BlockItem(VaultStone.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> VaultDoorItem = ItemRegister.register("scp076_vault_door",
            () -> new BlockItem(VaultDoor.get(), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> CoffinItem = ItemRegister.register("scp076_coffin",
            () -> new BlockItem(Coffin.get(), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp076DoorBlockEntity>> DoorEntity = BlockEntityTypeRegister.register("scp076_vault_door",
            () -> BlockEntityType.Builder.of(Scp076DoorBlockEntity::new, VaultDoor.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp076CoffinBlockEntity>> CoffinEntity = BlockEntityTypeRegister.register("scp076_coffin",
            () -> BlockEntityType.Builder.of(Scp076CoffinBlockEntity::new, Coffin.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp076Abel>> Abel = EntityTypeRegister.register("scp076",
            () -> EntityType.Builder.<Scp076Abel>of(Scp076Abel::new, MobCategory.MONSTER).sized(Scp076Abel.Width, Scp076Abel.Height)
                    .eyeHeight(Scp076Abel.EyeHeight).clientTrackingRange(10).build("scp076"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp076Knife>> Knife = EntityTypeRegister.register("scp076_knife",
            () -> EntityType.Builder.<Scp076Knife>of(Scp076Knife::new, MobCategory.MISC).sized(0.3f, 0.3f).clientTrackingRange(6).updateInterval(4)
                    .noSave().build("scp076_knife"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp076Blades>> Blades = EntityTypeRegister.register("scp076_blades",
            () -> EntityType.Builder.<Scp076Blades>of(Scp076Blades::new, MobCategory.MISC).sized(0.9f, 1.4f).clientTrackingRange(6).updateInterval(2)
                    .noSave().noSummon().fireImmune().build("scp076_blades"));

    public static final DeferredItem<DeferredSpawnEggItem> AbelEgg = ItemRegister.register("scp076_spawn_egg",
            () -> new DeferredSpawnEggItem(Abel, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));

    public static final DeferredHolder<SoundEvent, SoundEvent> LockTurn = sound("scp076_lock_turn");
    public static final DeferredHolder<SoundEvent, SoundEvent> LockClunk = sound("scp076_lock_clunk");
    public static final DeferredHolder<SoundEvent, SoundEvent> LockRattle = sound("scp076_lock_rattle");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoorGrind = sound("scp076_door_grind");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoorRumble = sound("scp076_door_rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoorShut = sound("scp076_door_shut");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoorBoom = sound("scp076_door_boom");
    public static final DeferredHolder<SoundEvent, SoundEvent> LidScrape = sound("scp076_lid_scrape");
    public static final DeferredHolder<SoundEvent, SoundEvent> LidDrop = sound("scp076_lid_drop");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rise = sound("scp076_rise");
    public static final DeferredHolder<SoundEvent, SoundEvent> RiseLow = sound("scp076_rise_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Breath = sound("scp076_breath");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp076_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> Death = sound("scp076_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> Crumble = sound("scp076_crumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> Slash = sound("scp076_slash");
    public static final DeferredHolder<SoundEvent, SoundEvent> SlashRing = sound("scp076_slash_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> Stab = sound("scp076_stab");
    public static final DeferredHolder<SoundEvent, SoundEvent> Throw = sound("scp076_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> KnifeLand = sound("scp076_knife_land");
    public static final DeferredHolder<SoundEvent, SoundEvent> KnifeHit = sound("scp076_knife_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> Summon = sound("scp076_summon");
    public static final DeferredHolder<SoundEvent, SoundEvent> BladesWarn = sound("scp076_blades_warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> BladesWarnLow = sound("scp076_blades_warn_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> BladesErupt = sound("scp076_blades_erupt");
    public static final DeferredHolder<SoundEvent, SoundEvent> BladesRing = sound("scp076_blades_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> Seal = sound("scp076_seal");
    public static final DeferredHolder<SoundEvent, SoundEvent> SealLow = sound("scp076_seal_low");

    private Scp076Registry() {
    }

    private static ResourceKey<DamageType> damage(String Name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        StartConditions.register(Scp076VaultCondition.TypeName, Scp076VaultCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp076Vault::spawn);
    }
}
