package net.rai.scp_unleashed.guns;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GunsRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<MenuType<?>> MenuRegister = DeferredRegister.create(Registries.MENU, Scp_unleashed.ModId);
    public static final DeferredRegister<RecipeType<?>> RecipeTypeRegister = DeferredRegister.create(Registries.RECIPE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<RecipeSerializer<?>> RecipeSerializerRegister = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    private static final List<DeferredItem<? extends Item>> CreativeOrder = new ArrayList<>();
    private static final Map<GunKind, DeferredItem<GunItem>> Guns = new EnumMap<>(GunKind.class);
    private static final Map<String, DeferredItem<AmmoItem>> Ammo = new HashMap<>();

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LoadedAmmo = ComponentRegister.registerComponentType("guns_ammo",
            Builder -> Builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> Charges = ComponentRegister.registerComponentType("guns_charges",
            Builder -> Builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GunTimer>> Reload = ComponentRegister.registerComponentType("guns_reload",
            Builder -> Builder.networkSynchronized(GunTimer.TimerStreamCodec));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GunTimer>> Charging = ComponentRegister.registerComponentType("guns_charging",
            Builder -> Builder.networkSynchronized(GunTimer.TimerStreamCodec));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GunHeat>> Heat = ComponentRegister.registerComponentType("guns_heat",
            Builder -> Builder.networkSynchronized(GunHeat.HeatStreamCodec));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GunTimer>> Venting = ComponentRegister.registerComponentType("guns_venting",
            Builder -> Builder.networkSynchronized(GunTimer.TimerStreamCodec));

    public static final DeferredBlock<GunBenchBlock> GunBench = BlockRegister.register("gun_bench", () -> new GunBenchBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.5F, 6.0F).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredItem<BlockItem> GunBenchItem = listed(ItemRegister.registerSimpleBlockItem("gun_bench", GunBench));

    public static final DeferredItem<GunItem> Pistol = gun(GunKind.Pistol, Rarity.COMMON);
    public static final DeferredItem<GunItem> Revolver = gun(GunKind.Revolver, Rarity.COMMON);
    public static final DeferredItem<GunItem> AssaultRifle = gun(GunKind.AssaultRifle, Rarity.UNCOMMON);
    public static final DeferredItem<GunItem> Shotgun = gun(GunKind.Shotgun, Rarity.UNCOMMON);
    public static final DeferredItem<GunItem> SniperRifle = gun(GunKind.SniperRifle, Rarity.RARE);
    public static final DeferredItem<GunItem> ParticleDisruptor = gun(GunKind.ParticleDisruptor, Rarity.EPIC);
    public static final DeferredItem<GunItem> DoubleBarrel = gun(GunKind.DoubleBarrel, Rarity.UNCOMMON);
    public static final DeferredItem<GunItem> Spiker = gun(GunKind.Spiker, Rarity.UNCOMMON);
    public static final DeferredItem<GunItem> CombatShotgun = gun(GunKind.CombatShotgun, Rarity.UNCOMMON);
    public static final DeferredItem<GunItem> MachineGun = gun(GunKind.MachineGun, Rarity.RARE);

    public static final DeferredItem<AmmoItem> HandgunRounds = ammo("handgun_rounds");
    public static final DeferredItem<AmmoItem> RifleRounds = ammo("rifle_rounds");
    public static final DeferredItem<AmmoItem> SniperRounds = ammo("sniper_rounds");
    public static final DeferredItem<AmmoItem> ShotgunShells = ammo("shotgun_shells");
    public static final DeferredItem<AmmoItem> Nails = ammo("nails");

    public static final DeferredItem<GunPartItem> GunSpring = part("gun_spring", Rarity.COMMON);
    public static final DeferredItem<GunPartItem> FiringMechanism = part("firing_mechanism", Rarity.COMMON);
    public static final DeferredItem<GunPartItem> GunReceiver = part("gun_receiver", Rarity.COMMON);
    public static final DeferredItem<GunPartItem> ShortBarrel = part("short_barrel", Rarity.COMMON);
    public static final DeferredItem<GunPartItem> LongBarrel = part("long_barrel", Rarity.COMMON);
    public static final DeferredItem<GunPartItem> HeavyBarrel = part("heavy_barrel", Rarity.UNCOMMON);
    public static final DeferredItem<GunPartItem> GunStock = part("gun_stock", Rarity.COMMON);
    public static final DeferredItem<GunPartItem> GunMagazine = part("gun_magazine", Rarity.COMMON);
    public static final DeferredItem<GunPartItem> RevolverCylinder = part("revolver_cylinder", Rarity.COMMON);
    public static final DeferredItem<GunPartItem> SniperScope = part("sniper_scope", Rarity.COMMON);
    public static final DeferredItem<GunPartItem> DisruptorCore = part("disruptor_core", Rarity.RARE);

    public static final DeferredHolder<MenuType<?>, MenuType<GunBenchMenu>> GunBenchMenuType = MenuRegister.register("gun_bench",
            () -> new MenuType<>(GunBenchMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<RecipeType<?>, RecipeType<GunCraftingRecipe>> GunCraftingType = RecipeTypeRegister.register("gun_crafting",
            () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "gun_crafting")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<GunCraftingRecipe>> GunCraftingSerializer = RecipeSerializerRegister.register("gun_crafting",
            GunCraftingRecipe.Serializer::new);

    public static final DeferredHolder<SoundEvent, SoundEvent> ShotCrack = fixedSound("guns_shot_crack", 64.0F);
    public static final DeferredHolder<SoundEvent, SoundEvent> ShotPunch = fixedSound("guns_shot_punch", 48.0F);
    public static final DeferredHolder<SoundEvent, SoundEvent> ShotWhump = fixedSound("guns_shot_whump", 64.0F);
    public static final DeferredHolder<SoundEvent, SoundEvent> ShotHeavy = fixedSound("guns_shot_heavy", 112.0F);
    public static final DeferredHolder<SoundEvent, SoundEvent> ShotBoom = fixedSound("guns_shot_boom", 112.0F);
    public static final DeferredHolder<SoundEvent, SoundEvent> ShotEcho = fixedSound("guns_shot_echo", 160.0F);
    public static final DeferredHolder<SoundEvent, SoundEvent> ShotThunder = fixedSound("guns_shot_thunder", 160.0F);
    public static final DeferredHolder<SoundEvent, SoundEvent> Pneumatic = fixedSound("guns_pneumatic", 32.0F);
    public static final DeferredHolder<SoundEvent, SoundEvent> PneumaticVent = sound("guns_pneumatic_vent");
    public static final DeferredHolder<SoundEvent, SoundEvent> MechanismClick = sound("guns_mechanism_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> MechanismClunk = sound("guns_mechanism_clunk");
    public static final DeferredHolder<SoundEvent, SoundEvent> MechanismOpen = sound("guns_mechanism_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> MechanismClose = sound("guns_mechanism_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> MechanismChain = sound("guns_mechanism_chain");
    public static final DeferredHolder<SoundEvent, SoundEvent> MechanismDoor = sound("guns_mechanism_door");
    public static final DeferredHolder<SoundEvent, SoundEvent> MechanismLoad = sound("guns_mechanism_load");
    public static final DeferredHolder<SoundEvent, SoundEvent> DisruptorCharge = fixedSound("guns_disruptor_charge", 64.0F);
    public static final DeferredHolder<SoundEvent, SoundEvent> DisruptorPower = fixedSound("guns_disruptor_power", 48.0F);
    public static final DeferredHolder<SoundEvent, SoundEvent> DisruptorWhine = fixedSound("guns_disruptor_whine", 48.0F);
    public static final DeferredHolder<SoundEvent, SoundEvent> DisruptorDischarge = fixedSound("guns_disruptor_discharge", 160.0F);
    public static final DeferredHolder<SoundEvent, SoundEvent> DisruptorFade = sound("guns_disruptor_fade");
    public static final DeferredHolder<SoundEvent, SoundEvent> DisruptorBurnout = sound("guns_disruptor_burnout");
    public static final DeferredHolder<SoundEvent, SoundEvent> Fizz = sound("guns_fizz");
    public static final DeferredHolder<SoundEvent, SoundEvent> HitMarker = sound("guns_hit_marker");
    public static final DeferredHolder<SoundEvent, SoundEvent> BenchAnvil = sound("guns_bench_anvil");
    public static final DeferredHolder<SoundEvent, SoundEvent> BenchSmithing = sound("guns_bench_smithing");

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Spark = ParticleRegister.register("guns_spark", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> EnergySpark = ParticleRegister.register("guns_energy_spark", () -> new SimpleParticleType(true));

    public static final ResourceKey<DamageType> GunshotDamage = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "gunshot"));
    public static final ResourceKey<DamageType> DisruptionDamage = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "particle_disruption"));
    public static final ResourceLocation EncumbranceId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "guns_encumbrance");

    private GunsRegistry() {
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        MenuRegister.register(ModEventBus);
        RecipeTypeRegister.register(ModEventBus);
        RecipeSerializerRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
    }

    public static List<DeferredItem<? extends Item>> creativeOrder() {
        return Collections.unmodifiableList(CreativeOrder);
    }

    public static Item gunItem(GunKind Kind) {
        DeferredItem<GunItem> Entry = Guns.get(Kind);
        return Entry == null ? Items.AIR : Entry.get();
    }

    public static Item ammoItem(String Name) {
        DeferredItem<AmmoItem> Entry = Ammo.get(Name);
        return Entry == null ? Items.AIR : Entry.get();
    }

    public static void adoptGun(GunKind Kind, DeferredItem<GunItem> Entry) {
        Guns.put(Kind, listed(Entry));
    }

    public static void adoptAmmo(String Name, DeferredItem<AmmoItem> Entry) {
        Ammo.put(Name, listed(Entry));
    }

    private static DeferredItem<GunItem> gun(GunKind Kind, Rarity Grade) {
        DeferredItem<GunItem> Entry = listed(ItemRegister.register(Kind.getName(), () -> new GunItem(Kind, gunProperties(Kind, Grade))));
        Guns.put(Kind, Entry);
        return Entry;
    }

    private static Item.Properties gunProperties(GunKind Kind, Rarity Grade) {
        Item.Properties Properties = new Item.Properties().stacksTo(1).rarity(Grade);
        if (Kind.getStats().getEncumbrance() > 0.0F) {
            Properties = Properties.attributes(ItemAttributeModifiers.builder().add(Attributes.MOVEMENT_SPEED,
                    new AttributeModifier(EncumbranceId, -Kind.getStats().getEncumbrance(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.MAINHAND).build());
        }
        if (Kind.getStats().usesCharges()) {
            return Properties.component(Charges.get(), Kind.getStats().getCharges());
        }
        return Properties.component(LoadedAmmo.get(), 0);
    }

    private static DeferredItem<AmmoItem> ammo(String Name) {
        DeferredItem<AmmoItem> Entry = listed(ItemRegister.register(Name, () -> new AmmoItem(new Item.Properties())));
        Ammo.put(Name, Entry);
        return Entry;
    }

    private static DeferredItem<GunPartItem> part(String Name, Rarity Grade) {
        return listed(ItemRegister.register(Name, () -> new GunPartItem(new Item.Properties().rarity(Grade))));
    }

    private static <T extends Item> DeferredItem<T> listed(DeferredItem<T> Entry) {
        CreativeOrder.add(Entry);
        return Entry;
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> fixedSound(String Name, float Range) {
        return SoundRegister.register(Name, () -> SoundEvent.createFixedRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name), Range));
    }
}
