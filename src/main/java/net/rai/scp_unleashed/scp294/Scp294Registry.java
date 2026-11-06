package net.rai.scp_unleashed.scp294;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.inventory.MenuType;
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
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.stranger.StrangerEncounters;
import net.rai.scp_unleashed.archive.investigation.StartConditions;
import net.rai.scp_unleashed.dclass.theodore.Theodore;

import java.util.List;

public final class Scp294Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<MenuType<?>> MenuRegister = DeferredRegister.create(Registries.MENU, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_294");
    public static final ResourceKey<DamageType> DrinkDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp294_drink"));
    public static final ResourceKey<DamageType> AscendedDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp294_ascended"));

    public static final DeferredHolder<SoundEvent, SoundEvent> Key = sound("scp294_key");
    public static final DeferredHolder<SoundEvent, SoundEvent> Coin = sound("scp294_coin");
    public static final DeferredHolder<SoundEvent, SoundEvent> CoinRing = sound("scp294_coin_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> Whirr = sound("scp294_whirr");
    public static final DeferredHolder<SoundEvent, SoundEvent> WhirrGrind = sound("scp294_whirr_grind");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hum = sound("scp294_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> Drop = sound("scp294_drop");
    public static final DeferredHolder<SoundEvent, SoundEvent> DropTap = sound("scp294_drop_tap");
    public static final DeferredHolder<SoundEvent, SoundEvent> Pour = sound("scp294_pour");
    public static final DeferredHolder<SoundEvent, SoundEvent> PourBucket = sound("scp294_pour_bucket");
    public static final DeferredHolder<SoundEvent, SoundEvent> PourSplash = sound("scp294_pour_splash");
    public static final DeferredHolder<SoundEvent, SoundEvent> Pop = sound("scp294_pop");
    public static final DeferredHolder<SoundEvent, SoundEvent> PopSlide = sound("scp294_pop_slide");
    public static final DeferredHolder<SoundEvent, SoundEvent> Refuse = sound("scp294_refuse");
    public static final DeferredHolder<SoundEvent, SoundEvent> RefuseLow = sound("scp294_refuse_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Drink = sound("scp294_drink");
    public static final DeferredHolder<SoundEvent, SoundEvent> Radiance = sound("scp294_radiance");
    public static final DeferredHolder<SoundEvent, SoundEvent> Chime = sound("scp294_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> Vanish = sound("scp294_vanish");
    public static final DeferredHolder<SoundEvent, SoundEvent> Waves = sound("scp294_waves");
    public static final DeferredHolder<SoundEvent, SoundEvent> Sparkle = sound("scp294_sparkle");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CoinCue = List.of(Coin, CoinRing);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WhirrCue = List.of(Whirr, WhirrGrind);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DropCue = List.of(Drop, DropTap);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PourCue = List.of(Pour, PourBucket, PourSplash);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PopCue = List.of(Pop, PopSlide);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RefuseCue = List.of(Refuse, RefuseLow);

    public static final DeferredBlock<Scp294MachineBlock> Machine = BlockRegister.register("scp294_machine",
            () -> new Scp294MachineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(4.0f, 1200.0f).sound(SoundType.METAL)
                    .noOcclusion().pushReaction(PushReaction.BLOCK).lightLevel(Scp294MachineBlock::lightOf)
                    .isValidSpawn((State, World, Pos, Type) -> false)));
    public static final DeferredItem<BlockItem> MachineItem = ItemRegister.register("scp294_machine",
            () -> new BlockItem(Machine.get(), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp294MachineBlockEntity>> MachineEntity = BlockEntityTypeRegister.register("scp294_machine",
            () -> BlockEntityType.Builder.of(Scp294MachineBlockEntity::new, Machine.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<Scp294Menu>> KeyboardMenu = MenuRegister.register("scp294_keyboard",
            () -> IMenuTypeExtension.create(Scp294Menu::new));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Scp294Drink>> DrinkData = ComponentRegister.registerComponentType("scp294_drink",
            Builder -> Builder.persistent(Scp294Drink.DrinkCodec).networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(Scp294Drink.DrinkCodec)));

    public static final DeferredItem<Scp294CupItem> Cup = ItemRegister.register("scp294_cup",
            () -> new Scp294CupItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    private Scp294Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
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
        MenuRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
        StartConditions.register(Scp294ArrivalCondition.TypeName, Scp294ArrivalCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
        Theodore.ArsenicLike.add(Scp294CupItem::isArsenic);
    }
}
