package net.rai.scp_unleashed.cameras;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

public final class CameraRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredBlock<CameraBlock> Camera = BlockRegister.register("cctv_camera", () -> new CameraBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(2.0f, 6.0f).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.DESTROY)
            .isValidSpawn((State, World, Pos, Kind) -> false).isRedstoneConductor((State, World, Pos) -> false)
            .isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false)));
    public static final DeferredItem<BlockItem> CameraItem = ItemRegister.registerSimpleBlockItem("cctv_camera", Camera);
    public static final DeferredItem<CameraTabletItem> Tablet = ItemRegister.register("camera_tablet", () -> new CameraTabletItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CameraBlockEntity>> CameraEntity = BlockEntityRegister.register("cctv_camera",
            () -> BlockEntityType.Builder.of(CameraBlockEntity::new, Camera.get()).build(null));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CameraLinks>> Links = ComponentRegister.registerComponentType("camera_links",
            Builder -> Builder.persistent(CameraLinks.LinksCodec).networkSynchronized(CameraLinks.LinksStreamCodec).cacheEncoding());

    public static final DeferredHolder<SoundEvent, SoundEvent> Servo = sound("camera_servo");
    public static final DeferredHolder<SoundEvent, SoundEvent> ServoRatchet = sound("camera_servo_ratchet");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwitchClick = sound("camera_switch_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwitchStatic = sound("camera_switch_static");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwitchHiss = sound("camera_switch_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> RecordBeep = sound("camera_record_beep");
    public static final DeferredHolder<SoundEvent, SoundEvent> TabletOpen = sound("camera_tablet_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> TabletLink = sound("camera_tablet_link");

    private CameraRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
    }
}
