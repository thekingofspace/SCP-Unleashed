package net.rai.scp_unleashed.scp999;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
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

import java.util.List;

public final class Scp999Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_999");
    public static final int CandyNutrition = 2;
    public static final float CandySaturation = 0.2f;
    public static final float BowlHardness = 0.6f;
    public static final float BowlResistance = 1.0f;
    public static final SoundType BowlSound = new SoundType(1.0f, 1.35f, SoundEvents.DECORATED_POT_BREAK, SoundEvents.DECORATED_POT_STEP,
            SoundEvents.DECORATED_POT_PLACE, SoundEvents.DECORATED_POT_HIT, SoundEvents.DECORATED_POT_FALL);

    public static final DeferredHolder<EntityType<?>, EntityType<Scp999Blob>> Blob = EntityTypeRegister.register("scp999_blob",
            () -> EntityType.Builder.<Scp999Blob>of(Scp999Blob::new, MobCategory.CREATURE)
                    .sized(Scp999Blob.Width, Scp999Blob.Height)
                    .eyeHeight(Scp999Blob.EyeHeight)
                    .clientTrackingRange(10)
                    .build("scp999_blob"));

    public static final DeferredItem<DeferredSpawnEggItem> Egg = ItemRegister.register("scp999_spawn_egg",
            () -> new DeferredSpawnEggItem(Blob, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<ChocolateOrbsItem> Candy = ItemRegister.register("chocolate_orbs",
            () -> new ChocolateOrbsItem(new Item.Properties().food(new FoodProperties.Builder().nutrition(CandyNutrition).saturationModifier(CandySaturation).fast().build())));

    public static final DeferredBlock<PinkDogBowlBlock> Bowl = BlockRegister.register("pink_dog_bowl",
            () -> new PinkDogBowlBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(BowlHardness, BowlResistance).sound(BowlSound)
                    .noOcclusion().pushReaction(PushReaction.DESTROY).isValidSpawn((State, Getter, Pos, Type) -> false)
                    .isRedstoneConductor((State, Getter, Pos) -> false).isSuffocating((State, Getter, Pos) -> false).isViewBlocking((State, Getter, Pos) -> false)));
    public static final DeferredItem<BlockItem> BowlItem = ItemRegister.register("pink_dog_bowl", () -> new PinkDogBowlItem(Bowl.get(), new Item.Properties()));

    public static final DeferredHolder<SoundEvent, SoundEvent> Chirp = sound("scp999_chirp");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChirpSqueak = sound("scp999_chirp_squeak");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChirpSparkle = sound("scp999_chirp_sparkle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Trill = sound("scp999_trill");
    public static final DeferredHolder<SoundEvent, SoundEvent> TrillSqueak = sound("scp999_trill_squeak");
    public static final DeferredHolder<SoundEvent, SoundEvent> TrillSparkle = sound("scp999_trill_sparkle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Coo = sound("scp999_coo");
    public static final DeferredHolder<SoundEvent, SoundEvent> CooPurr = sound("scp999_coo_purr");
    public static final DeferredHolder<SoundEvent, SoundEvent> CooSquish = sound("scp999_coo_squish");
    public static final DeferredHolder<SoundEvent, SoundEvent> Munch = sound("scp999_munch");
    public static final DeferredHolder<SoundEvent, SoundEvent> MunchCrunch = sound("scp999_munch_crunch");
    public static final DeferredHolder<SoundEvent, SoundEvent> MunchSquish = sound("scp999_munch_squish");
    public static final DeferredHolder<SoundEvent, SoundEvent> Squeak = sound("scp999_squeak");
    public static final DeferredHolder<SoundEvent, SoundEvent> SqueakWhine = sound("scp999_squeak_whine");
    public static final DeferredHolder<SoundEvent, SoundEvent> SqueakSquish = sound("scp999_squeak_squish");
    public static final DeferredHolder<SoundEvent, SoundEvent> Sigh = sound("scp999_sigh");
    public static final DeferredHolder<SoundEvent, SoundEvent> SighSoft = sound("scp999_sigh_soft");
    public static final DeferredHolder<SoundEvent, SoundEvent> Squish = sound("scp999_squish");
    public static final DeferredHolder<SoundEvent, SoundEvent> SquishWet = sound("scp999_squish_wet");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hum = sound("scp999_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> HumTone = sound("scp999_hum_tone");
    public static final DeferredHolder<SoundEvent, SoundEvent> Giggle = sound("scp999_giggle");
    public static final DeferredHolder<SoundEvent, SoundEvent> GiggleHaggle = sound("scp999_giggle_haggle");
    public static final DeferredHolder<SoundEvent, SoundEvent> GiggleTickle = sound("scp999_giggle_tickle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hop = sound("scp999_hop");
    public static final DeferredHolder<SoundEvent, SoundEvent> HopSoft = sound("scp999_hop_soft");
    public static final DeferredHolder<SoundEvent, SoundEvent> BowlFill = sound("pink_dog_bowl_fill");
    public static final DeferredHolder<SoundEvent, SoundEvent> BowlFillClink = sound("pink_dog_bowl_fill_clink");
    public static final DeferredHolder<SoundEvent, SoundEvent> BowlTake = sound("pink_dog_bowl_take");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ChirpLayers = List.of(Chirp, ChirpSqueak, ChirpSparkle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TrillLayers = List.of(Trill, TrillSqueak, TrillSparkle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CooLayers = List.of(Coo, CooPurr, CooSquish);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> MunchLayers = List.of(Munch, MunchCrunch, MunchSquish);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SqueakLayers = List.of(Squeak, SqueakWhine, SqueakSquish);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SighLayers = List.of(Sigh, SighSoft);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SquishLayers = List.of(Squish, SquishWet);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HumLayers = List.of(Hum, HumTone);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> GiggleLayers = List.of(Giggle, GiggleHaggle, GiggleTickle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HopLayers = List.of(Hop, HopSoft);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> FillLayers = List.of(BowlFill, BowlFillClink);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TakeLayers = List.of(BowlTake);

    private Scp999Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        StartConditions.register(Scp999ArrivalCondition.TypeName, Scp999ArrivalCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp999ArrivalCondition::manifest);
    }
}
