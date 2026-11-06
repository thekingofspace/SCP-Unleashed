package net.rai.scp_unleashed.security;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

public final class SecurityRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<MenuType<?>> MenuRegister = DeferredRegister.create(Registries.MENU, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredBlock<SentryTurretBlock> SentryTurret = BlockRegister.register("sentry_turret", () -> new SentryTurretBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(5.0f, 9.0f).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK)
            .isValidSpawn((State, World, Pos, Kind) -> false).isRedstoneConductor((State, World, Pos) -> false)
            .isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false)));
    public static final DeferredBlock<MotionDetectorBlock> MotionDetector = BlockRegister.register("motion_detector", () -> new MotionDetectorBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(1.5f, 6.0f).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.DESTROY)));

    public static final DeferredItem<BlockItem> SentryTurretItem = ItemRegister.registerSimpleBlockItem("sentry_turret", SentryTurret, new Item.Properties());
    public static final DeferredItem<BlockItem> MotionDetectorItem = ItemRegister.registerSimpleBlockItem("motion_detector", MotionDetector, new Item.Properties());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SentryTurretBlockEntity>> SentryTurretEntity = BlockEntityTypeRegister.register("sentry_turret",
            () -> BlockEntityType.Builder.of(SentryTurretBlockEntity::new, SentryTurret.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MotionDetectorBlockEntity>> MotionDetectorEntity = BlockEntityTypeRegister.register("motion_detector",
            () -> BlockEntityType.Builder.of(MotionDetectorBlockEntity::new, MotionDetector.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<SentryTurretMenu>> SentryTurretMenuType = MenuRegister.register("sentry_turret",
            () -> IMenuTypeExtension.create(SentryTurretMenu::new));

    public static final DeferredHolder<SoundEvent, SoundEvent> TurretServo = sound("security_turret_servo");
    public static final DeferredHolder<SoundEvent, SoundEvent> TurretServoHum = sound("security_turret_servo_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> TurretServoTick = sound("security_turret_servo_tick");
    public static final DeferredHolder<SoundEvent, SoundEvent> TurretAlert = sound("security_turret_alert");
    public static final DeferredHolder<SoundEvent, SoundEvent> TurretEmpty = sound("security_turret_empty");
    public static final DeferredHolder<SoundEvent, SoundEvent> TurretPowerUp = sound("security_turret_power_up");
    public static final DeferredHolder<SoundEvent, SoundEvent> DetectorBeep = sound("security_detector_beep");
    public static final DeferredHolder<SoundEvent, SoundEvent> DetectorClick = sound("security_detector_click");

    public static final ResourceKey<DamageType> TurretDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("sentry_turret"));

    private SecurityRegistry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        MenuRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
    }
}
