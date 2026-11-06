package net.rai.scp_unleashed.mining;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.computer.ComputerDeviceBlock;

public final class MiningRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);

    public static final DeferredBlock<PointMinerBlock> PointMiner = BlockRegister.register("point_miner", () -> new PointMinerBlock(base()
            .lightLevel(State -> State.getValue(PointMinerBlock.Status).getLight())));
    public static final DeferredBlock<PointBankBlock> PointBank = BlockRegister.register("point_bank", () -> new PointBankBlock(base()
            .lightLevel(State -> ComputerDeviceBlock.lightOf(State, 3))));

    public static final DeferredItem<BlockItem> PointMinerItem = ItemRegister.registerSimpleBlockItem("point_miner", PointMiner);
    public static final DeferredItem<BlockItem> PointBankItem = ItemRegister.register("point_bank", () -> new BlockItem(PointBank.get(), new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PointMinerBlockEntity>> PointMinerEntity = BlockEntityTypeRegister.register("point_miner",
            () -> BlockEntityType.Builder.of(PointMinerBlockEntity::new, PointMiner.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PointBankBlockEntity>> PointBankEntity = BlockEntityTypeRegister.register("point_bank",
            () -> BlockEntityType.Builder.of(PointBankBlockEntity::new, PointBank.get()).build(null));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> BalanceComponent = ComponentRegister.registerComponentType("point_bank_balance",
            Builder -> Builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    public static final DeferredHolder<SoundEvent, SoundEvent> Hum = sound("mining_hum");

    private MiningRegistry() {
    }

    private static BlockBehaviour.Properties base() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion()
                .isValidSpawn((State, World, Pos, Kind) -> false).isRedstoneConductor((State, World, Pos) -> false)
                .isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
    }
}
