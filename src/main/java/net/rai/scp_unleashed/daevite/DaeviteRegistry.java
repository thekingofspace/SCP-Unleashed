package net.rai.scp_unleashed.daevite;

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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.daevite.block.DaeviteAltarBlock;
import net.rai.scp_unleashed.daevite.block.DaeviteBrazierBlock;
import net.rai.scp_unleashed.daevite.block.DaeviteDecor;
import net.rai.scp_unleashed.daevite.block.DaeviteDecorBlock;
import net.rai.scp_unleashed.daevite.block.DaeviteDrumBlock;
import net.rai.scp_unleashed.daevite.block.DaeviteLanternBlock;
import net.rai.scp_unleashed.daevite.block.DaeviteRugBlock;
import net.rai.scp_unleashed.daevite.block.DaeviteTapestryBlock;
import net.rai.scp_unleashed.daevite.entity.DaeviteBolt;
import net.rai.scp_unleashed.daevite.entity.DaeviteCircle;
import net.rai.scp_unleashed.daevite.entity.DaeviteKnight;
import net.rai.scp_unleashed.daevite.entity.DaeviteOverseer;
import net.rai.scp_unleashed.faction.FactionRegistry;
import net.rai.scp_unleashed.faction.Factions;

import java.util.List;

public final class DaeviteRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceKey<DamageType> BoltDamage = damage("daevite_bolt");
    public static final ResourceKey<DamageType> BurstDamage = damage("daevite_burst");
    public static final ResourceKey<DamageType> BindingDamage = damage("daevite_binding");

    public static final DeferredBlock<DaeviteDecorBlock> WarBanner = BlockRegister.register(DaeviteDecor.WarBanner.id(),
            () -> new DaeviteDecorBlock(decor(MapColor.COLOR_RED, SoundType.WOOD, 1.0f).noOcclusion().ignitedByLava(), DaeviteDecor.WarBanner));
    public static final DeferredBlock<DaeviteBrazierBlock> Brazier = BlockRegister.register(DaeviteDecor.Brazier.id(),
            () -> new DaeviteBrazierBlock(decor(MapColor.GOLD, SoundType.METAL, 2.5f).noOcclusion().requiresCorrectToolForDrops()
                    .lightLevel(State -> State.getValue(DaeviteBrazierBlock.Lit) ? 15 : 0)));
    public static final DeferredBlock<DaeviteDecorBlock> Idol = BlockRegister.register(DaeviteDecor.Idol.id(),
            () -> new DaeviteDecorBlock(decor(MapColor.GOLD, SoundType.METAL, 3.0f).noOcclusion().requiresCorrectToolForDrops(), DaeviteDecor.Idol));
    public static final DeferredBlock<DaeviteRugBlock> Rug = BlockRegister.register(DaeviteDecor.Rug.id(),
            () -> new DaeviteRugBlock(decor(MapColor.COLOR_RED, SoundType.WOOL, 0.2f).noOcclusion().ignitedByLava()));
    public static final DeferredBlock<DaeviteDecorBlock> TentCloth = BlockRegister.register(DaeviteDecor.TentCloth.id(),
            () -> new DaeviteDecorBlock(decor(MapColor.COLOR_RED, SoundType.WOOL, 0.8f).ignitedByLava().instrument(NoteBlockInstrument.GUITAR), DaeviteDecor.TentCloth));
    public static final DeferredBlock<DaeviteDecorBlock> WeaponRack = BlockRegister.register(DaeviteDecor.WeaponRack.id(),
            () -> new DaeviteDecorBlock(decor(MapColor.WOOD, SoundType.WOOD, 2.0f).noOcclusion().ignitedByLava(), DaeviteDecor.WeaponRack));
    public static final DeferredBlock<DaeviteLanternBlock> Lantern = BlockRegister.register(DaeviteDecor.Lantern.id(),
            () -> new DaeviteLanternBlock(decor(MapColor.GOLD, SoundType.LANTERN, 2.5f).noOcclusion().requiresCorrectToolForDrops().lightLevel(State -> 14)
                    .pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<DaeviteDecorBlock> BrassUrn = BlockRegister.register(DaeviteDecor.BrassUrn.id(),
            () -> new DaeviteDecorBlock(decor(MapColor.GOLD, SoundType.METAL, 2.0f).noOcclusion().requiresCorrectToolForDrops(), DaeviteDecor.BrassUrn));
    public static final DeferredBlock<DaeviteDrumBlock> WarDrum = BlockRegister.register(DaeviteDecor.WarDrum.id(),
            () -> new DaeviteDrumBlock(decor(MapColor.COLOR_BROWN, SoundType.WOOD, 1.5f).noOcclusion().ignitedByLava()));
    public static final DeferredBlock<DaeviteDecorBlock> SigilStone = BlockRegister.register(DaeviteDecor.SigilStone.id(),
            () -> new DaeviteDecorBlock(decor(MapColor.COLOR_BLACK, SoundType.POLISHED_DEEPSLATE, 3.5f).requiresCorrectToolForDrops()
                    .instrument(NoteBlockInstrument.BASEDRUM), DaeviteDecor.SigilStone));
    public static final DeferredBlock<DaeviteTapestryBlock> Tapestry = BlockRegister.register(DaeviteDecor.Tapestry.id(),
            () -> new DaeviteTapestryBlock(decor(MapColor.COLOR_RED, SoundType.WOOL, 0.5f).noOcclusion().noCollission().ignitedByLava()));
    public static final DeferredBlock<DaeviteAltarBlock> Altar = BlockRegister.register(DaeviteDecor.Altar.id(),
            () -> new DaeviteAltarBlock(decor(MapColor.COLOR_BLACK, SoundType.POLISHED_DEEPSLATE, 6.0f).noOcclusion().requiresCorrectToolForDrops()
                    .lightLevel(State -> 11)));

    public static final List<DeferredBlock<? extends Block>> DecorBlocks = List.of(WarBanner, Brazier, Idol, Rug, TentCloth, WeaponRack, Lantern, BrassUrn, WarDrum,
            SigilStone, Tapestry, Altar);

    public static final DeferredItem<BlockItem> WarBannerItem = blockItem(WarBanner);
    public static final DeferredItem<BlockItem> BrazierItem = blockItem(Brazier);
    public static final DeferredItem<BlockItem> IdolItem = blockItem(Idol);
    public static final DeferredItem<BlockItem> RugItem = blockItem(Rug);
    public static final DeferredItem<BlockItem> TentClothItem = blockItem(TentCloth);
    public static final DeferredItem<BlockItem> WeaponRackItem = blockItem(WeaponRack);
    public static final DeferredItem<BlockItem> LanternItem = blockItem(Lantern);
    public static final DeferredItem<BlockItem> BrassUrnItem = blockItem(BrassUrn);
    public static final DeferredItem<BlockItem> WarDrumItem = blockItem(WarDrum);
    public static final DeferredItem<BlockItem> SigilStoneItem = blockItem(SigilStone);
    public static final DeferredItem<BlockItem> TapestryItem = blockItem(Tapestry);
    public static final DeferredItem<BlockItem> AltarItem = blockItem(Altar);

    public static final List<DeferredItem<BlockItem>> DecorItems = List.of(WarBannerItem, BrazierItem, IdolItem, RugItem, TentClothItem, WeaponRackItem, LanternItem,
            BrassUrnItem, WarDrumItem, SigilStoneItem, TapestryItem, AltarItem);

    public static final DeferredHolder<EntityType<?>, EntityType<DaeviteKnight>> Knight = EntityTypeRegister.register("daevite_knight",
            () -> EntityType.Builder.<DaeviteKnight>of(DaeviteKnight::new, MobCategory.MONSTER).sized(DaeviteKnight.Width, DaeviteKnight.Height)
                    .eyeHeight(DaeviteKnight.EyeHeight).clientTrackingRange(10).build("daevite_knight"));
    public static final DeferredHolder<EntityType<?>, EntityType<DaeviteOverseer>> Overseer = EntityTypeRegister.register("daevite_overseer",
            () -> EntityType.Builder.<DaeviteOverseer>of(DaeviteOverseer::new, MobCategory.MONSTER).sized(DaeviteOverseer.Width, DaeviteOverseer.Height)
                    .eyeHeight(DaeviteOverseer.EyeHeight).clientTrackingRange(10).fireImmune().build("daevite_overseer"));
    public static final DeferredHolder<EntityType<?>, EntityType<DaeviteBolt>> Bolt = EntityTypeRegister.register("daevite_bolt",
            () -> EntityType.Builder.<DaeviteBolt>of(DaeviteBolt::new, MobCategory.MISC).sized(0.35f, 0.35f).clientTrackingRange(8).updateInterval(2).noSave()
                    .fireImmune().build("daevite_bolt"));
    public static final DeferredHolder<EntityType<?>, EntityType<DaeviteCircle>> Circle = EntityTypeRegister.register("daevite_circle",
            () -> EntityType.Builder.<DaeviteCircle>of(DaeviteCircle::new, MobCategory.MISC).sized(0.5f, 0.1f).clientTrackingRange(10).updateInterval(10)
                    .fireImmune().noSummon().noSave().build("daevite_circle"));

    public static final DeferredItem<DeferredSpawnEggItem> KnightEgg = ItemRegister.register("daevite_knight_spawn_egg",
            () -> new DeferredSpawnEggItem(Knight, 0x8E1A16, 0xC99A3A, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> OverseerEgg = ItemRegister.register("daevite_overseer_spawn_egg",
            () -> new DeferredSpawnEggItem(Overseer, 0x5E0E10, 0xE0B04A, new Item.Properties()));

    public static final DeferredHolder<SoundEvent, SoundEvent> KnightIdle = sound("daevite_knight_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> KnightHurt = sound("daevite_knight_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> KnightDeath = sound("daevite_knight_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> ArmourStep = sound("daevite_armour_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> SpearThrust = sound("daevite_spear_thrust");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChargeCry = sound("daevite_charge_cry");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChargeHit = sound("daevite_charge_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChargeWall = sound("daevite_charge_wall");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwordSlash = sound("daevite_sword_slash");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwordHeavy = sound("daevite_sword_heavy");
    public static final DeferredHolder<SoundEvent, SoundEvent> GuardClang = sound("daevite_guard_clang");
    public static final DeferredHolder<SoundEvent, SoundEvent> BladeBite = sound("daevite_blade_bite");
    public static final DeferredHolder<SoundEvent, SoundEvent> OverseerIdle = sound("daevite_overseer_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> OverseerHurt = sound("daevite_overseer_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> OverseerDeath = sound("daevite_overseer_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> Cast = sound("daevite_cast");
    public static final DeferredHolder<SoundEvent, SoundEvent> BoltFire = sound("daevite_bolt_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> BoltHit = sound("daevite_bolt_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> Heal = sound("daevite_heal");
    public static final DeferredHolder<SoundEvent, SoundEvent> BindForm = sound("daevite_bind_form");
    public static final DeferredHolder<SoundEvent, SoundEvent> BindSnap = sound("daevite_bind_snap");
    public static final DeferredHolder<SoundEvent, SoundEvent> Burst = sound("daevite_burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> Evade = sound("daevite_evade");
    public static final DeferredHolder<SoundEvent, SoundEvent> Collapse = sound("daevite_collapse");
    public static final DeferredHolder<SoundEvent, SoundEvent> Arrive = sound("daevite_arrive");
    public static final DeferredHolder<SoundEvent, SoundEvent> RitualBegin = sound("daevite_ritual_begin");
    public static final DeferredHolder<SoundEvent, SoundEvent> RitualHum = sound("daevite_ritual_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> RitualRumble = sound("daevite_ritual_rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> RitualBreak = sound("daevite_ritual_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> RitualCall = sound("daevite_ritual_call");
    public static final DeferredHolder<SoundEvent, SoundEvent> Drum = sound("daevite_drum");
    public static final DeferredHolder<SoundEvent, SoundEvent> Horn = sound("daevite_horn");

    private DaeviteRegistry() {
    }

    private static BlockBehaviour.Properties decor(MapColor Colour, SoundType Sound, float Strength) {
        return BlockBehaviour.Properties.of().mapColor(Colour).sound(Sound).strength(Strength, Strength * 2.0f);
    }

    private static DeferredItem<BlockItem> blockItem(DeferredBlock<? extends Block> Owner) {
        return ItemRegister.register(Owner.getId().getPath(), () -> new BlockItem(Owner.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    }

    private static ResourceKey<DamageType> damage(String Name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        FactionRegistry.bootstrap(ModEventBus);
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        Factions.register(DaeviteFaction.Instance);
    }
}
