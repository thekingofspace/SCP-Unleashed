package net.rai.scp_unleashed.dclass;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.theodore.TheodoreRegistry;

import java.util.List;

public final class DClassRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<MenuType<?>> MenuRegister = DeferredRegister.create(Registries.MENU, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<RecipeSerializer<?>> RecipeSerializerRegister = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents DataComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);

    public static final TagKey<Item> BloodMeats = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "dclass_blood_meats"));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PersonnelRecord>> RecordComponent = DataComponentRegister.registerComponentType("dclass_personnel_record",
            Builder -> Builder.persistent(PersonnelRecord.RecordCodec).networkSynchronized(PersonnelRecord.RecordStreamCodec));

    public static final DeferredHolder<SoundEvent, SoundEvent> Mutter = sound("dclass_mutter");
    public static final DeferredHolder<SoundEvent, SoundEvent> Snore = sound("dclass_snore");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("dclass_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> Death = sound("dclass_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> FileOpen = sound("dclass_file_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> FileStamp = sound("dclass_file_stamp");
    public static final DeferredHolder<SoundEvent, SoundEvent> FileStampThud = sound("dclass_file_stamp_thud");
    public static final DeferredHolder<SoundEvent, SoundEvent> TankFill = sound("dclass_tank_fill");
    public static final DeferredHolder<SoundEvent, SoundEvent> TankFillHum = sound("dclass_tank_fill_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> TankFeed = sound("dclass_tank_feed");
    public static final DeferredHolder<SoundEvent, SoundEvent> TankFeedClink = sound("dclass_tank_feed_clink");
    public static final DeferredHolder<SoundEvent, SoundEvent> TankRelease = sound("dclass_tank_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> TankReleaseHiss = sound("dclass_tank_release_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> TankReleaseSplash = sound("dclass_tank_release_splash");
    public static final DeferredHolder<SoundEvent, SoundEvent> TankBubble = sound("dclass_tank_bubble");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TankFillLayers = List.of(TankFill, TankFillHum);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TankFeedLayers = List.of(TankFeed, TankFeedClink);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TankReleaseLayers = List.of(TankRelease, TankReleaseHiss, TankReleaseSplash);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> FileStampLayers = List.of(FileStamp, FileStampThud);

    public static final DeferredBlock<CloningMachineBlock> CloningMachine = BlockRegister.register("cloning_machine", () -> new CloningMachineBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(4.0f, 8.0f).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK)
            .lightLevel(CloningMachineBlock::lightOf).isRedstoneConductor((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false)));
    public static final DeferredItem<BlockItem> CloningMachineItem = ItemRegister.registerSimpleBlockItem("cloning_machine", CloningMachine);

    public static final DeferredItem<CloningBloodItem> Blood = ItemRegister.register("dclass_blood",
            () -> new CloningBloodItem(CloningKind.Personnel, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<PersonnelFileItem> BlankFile = ItemRegister.register("blank_personnel_file",
            () -> new PersonnelFileItem(true, new Item.Properties()));
    public static final DeferredItem<PersonnelFileItem> PersonnelFile = ItemRegister.register("dclass_personnel_file",
            () -> new PersonnelFileItem(false, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CloningMachineBlockEntity>> CloningMachineEntity = BlockEntityTypeRegister.register("cloning_machine",
            () -> BlockEntityType.Builder.of(CloningMachineBlockEntity::new, CloningMachine.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<DClass>> DClassType = EntityTypeRegister.register("dclass",
            () -> EntityType.Builder.<DClass>of(DClass::new, MobCategory.MISC).sized(0.6f, 1.8f).eyeHeight(1.62f).vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT).clientTrackingRange(10).build("dclass"));
    public static final DeferredItem<DeferredSpawnEggItem> DClassEgg = ItemRegister.register("dclass_spawn_egg",
            () -> new DeferredSpawnEggItem(DClassType, 0xE0782A, 0x3A3F45, new Item.Properties()));

    public static final DeferredHolder<MenuType<?>, MenuType<CloningMenu>> CloningMenuType = MenuRegister.register("cloning_machine",
            () -> IMenuTypeExtension.create(CloningMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<DClassInventoryMenu>> InventoryMenuType = MenuRegister.register("dclass_inventory",
            () -> IMenuTypeExtension.create(DClassInventoryMenu::new));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DClassBloodRecipe>> BloodRecipeSerializer = RecipeSerializerRegister.register("dclass_blood",
            DClassBloodRecipe.Serializer::new);

    private DClassRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        MenuRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        RecipeSerializerRegister.register(ModEventBus);
        DataComponentRegister.register(ModEventBus);
        TheodoreRegistry.register(ModEventBus);
        CloningMachineBlockEntity.registerHatch(CloningKind.Personnel, new PersonnelHatch());
    }
}
