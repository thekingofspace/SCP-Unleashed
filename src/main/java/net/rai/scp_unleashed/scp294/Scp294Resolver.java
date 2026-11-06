package net.rai.scp_unleashed.scp294;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class Scp294Resolver {
    public static final int MinLetters = 3;
    public static final int MaxRepeat = 4;
    public static final int HotFluid = 1000;
    public static final int WaterColour = 0x3F76E4;
    public static final int LavaColour = 0xD9531E;
    public static final int MilkColour = 0xF2F0EA;
    public static final int HoneyColour = 0xF2A72B;
    public static final int FluidFallback = 0x7A8794;
    public static final String MessagePrefix = "message.scp_unleashed.scp294.";
    public static final Set<String> Withheld = Set.of("blood", "urine", "pee", "piss", "vomit", "puke", "spit", "saliva", "sweat", "tears", "snot", "bile",
            "semen", "poo", "poop", "bleach", "alcohol", "beer", "wine", "vodka", "whisky", "whiskey", "rum", "gin", "tequila", "liquor", "cocaine",
            "heroin", "meth", "lsd", "weed", "drugs", "medicine", "cough syrup");
    public static final Set<String> Unpourable = Set.of("cup", "cups", "a cup", "mug", "a mug", "glass", "a glass", "bottle", "a bottle", "bucket", "a bucket",
            "nothing", "empty", "air", "void", "the void", "anything", "something", "everything");
    public static final List<Mystery> Gentle = List.of(
            new Mystery("minecraft:speed", 20, 60), new Mystery("minecraft:haste", 20, 60), new Mystery("minecraft:jump_boost", 20, 60),
            new Mystery("minecraft:night_vision", 30, 90), new Mystery("minecraft:slow_falling", 15, 40), new Mystery("minecraft:luck", 30, 90),
            new Mystery("minecraft:regeneration", 5, 10), new Mystery("minecraft:absorption", 30, 60), new Mystery("minecraft:water_breathing", 30, 90),
            new Mystery("minecraft:fire_resistance", 20, 60), new Mystery("minecraft:dolphins_grace", 15, 40), new Mystery("minecraft:glowing", 15, 40),
            new Mystery("minecraft:invisibility", 10, 30));
    public static final List<Mystery> Odd = List.of(
            new Mystery("minecraft:nausea", 4, 7), new Mystery("minecraft:slowness", 5, 10), new Mystery("minecraft:hunger", 10, 20),
            new Mystery("minecraft:levitation", 1, 3), new Mystery("minecraft:mining_fatigue", 5, 10));
    public static final int OddChance = 4;
    public static final int MysteryMessages = 4;
    private static volatile Catalogue Names;

    private Scp294Resolver() {
    }

    public enum Kind {
        Config, Known, Fluid, Potion, Mystery, Refused
    }

    public record Mystery(String Effect, int MinSeconds, int MaxSeconds) {
    }

    public record Result(Kind Source, String Typed, String Key, Scp294Drink Drink) {
        public boolean refused() {
            return Source == Kind.Refused;
        }
    }

    private record Catalogue(Map<String, Fluid> Fluids, Map<String, Holder<Potion>> Potions, Set<String> Solids) {
    }

    public static void forget() {
        Names = null;
    }

    public static Result resolve(MinecraftServer Server, String Raw) {
        String Typed = Scp294Names.sanitize(Raw);
        String Key = Scp294Names.normalise(Typed);
        if (Key.isEmpty()) {
            return new Result(Kind.Refused, Typed, Key, null);
        }
        Optional<Scp294Config.Entry> Configured = Scp294Config.find(Key);
        if (Configured.isPresent()) {
            Scp294Config.Entry Found = Configured.get();
            return Found.Refuse() ? new Result(Kind.Refused, Typed, Key, null) : new Result(Kind.Config, Typed, Key, Found.Drink());
        }
        Scp294Drink Known = known(Key);
        if (Known != null) {
            return new Result(Kind.Known, Typed, Key, Known);
        }
        Catalogue Lookup = index();
        Fluid Liquid = Lookup.Fluids().get(Key);
        if (Liquid != null) {
            return new Result(Kind.Fluid, Typed, Key, fluid(Liquid, Key));
        }
        Holder<Potion> Brew = Lookup.Potions().get(potionKey(Key));
        if (Brew != null) {
            return new Result(Kind.Potion, Typed, Key, potion(Brew, potionKey(Key)));
        }
        if (nonsense(Key) || withheld(Key) || Unpourable.contains(Key) || Lookup.Solids().contains(Key)) {
            return new Result(Kind.Refused, Typed, Key, null);
        }
        long Seed = Server == null ? 0L : Server.overworld().getSeed();
        return new Result(Kind.Mystery, Typed, Key, mystery(Key, Seed));
    }

    public static boolean nonsense(String Key) {
        int Letters = 0;
        int Digits = 0;
        boolean Vowel = false;
        int Run = 0;
        char Last = 0;
        for (int Index = 0; Index < Key.length(); Index++) {
            char Letter = Key.charAt(Index);
            if (Letter >= 'a' && Letter <= 'z') {
                Letters++;
                if ("aeiouy".indexOf(Letter) >= 0) {
                    Vowel = true;
                }
            } else if (Letter >= '0' && Letter <= '9') {
                Digits++;
            }
            Run = Letter == Last && Letter != ' ' ? Run + 1 : 1;
            Last = Letter;
            if (Run >= MaxRepeat) {
                return true;
            }
        }
        return Letters < MinLetters || !Vowel || Digits > Letters;
    }

    public static boolean withheld(String Key) {
        if (Withheld.contains(Key)) {
            return true;
        }
        for (String Word : Key.split(" ")) {
            if (Withheld.contains(Word)) {
                return true;
            }
        }
        return false;
    }

    private static String potionKey(String Key) {
        String Result = Key;
        if (Result.startsWith("potion of ")) {
            Result = Result.substring("potion of ".length());
        }
        if (Result.endsWith(" potion")) {
            Result = Result.substring(0, Result.length() - " potion".length());
        }
        return Result;
    }

    private static Catalogue index() {
        Catalogue Cached = Names;
        if (Cached != null) {
            return Cached;
        }
        Map<String, Fluid> FluidNames = new HashMap<>();
        Language Words = Language.getInstance();
        for (Fluid Liquid : BuiltInRegistries.FLUID) {
            if (Liquid == Fluids.EMPTY || !Liquid.isSource(Liquid.defaultFluidState())) {
                continue;
            }
            ResourceLocation Id = BuiltInRegistries.FLUID.getKey(Liquid);
            FluidNames.putIfAbsent(Scp294Names.normalise(Id.getPath()), Liquid);
            FluidType Type = Liquid.getFluidType();
            String Description = Words.getOrDefault(Type.getDescriptionId(), "");
            if (!Description.isEmpty() && !Description.equals(Type.getDescriptionId())) {
                FluidNames.putIfAbsent(Scp294Names.normalise(Description), Liquid);
            }
        }
        Map<String, Holder<Potion>> PotionNames = new HashMap<>();
        BuiltInRegistries.POTION.holders().forEach(Brew -> PotionNames.putIfAbsent(Scp294Names.normalise(Brew.key().location().getPath()), Brew));
        Set<String> Solids = new HashSet<>();
        BuiltInRegistries.BLOCK.forEach(Placed -> {
            if (!(Placed instanceof LiquidBlock)) {
                Solids.add(Scp294Names.normalise(BuiltInRegistries.BLOCK.getKey(Placed).getPath()));
            }
        });
        BuiltInRegistries.ITEM.forEach(Thing -> {
            if (Thing instanceof BucketItem || Thing instanceof Scp294CupItem) {
                return;
            }
            UseAnim Animation;
            try {
                Animation = new ItemStack(Thing).getUseAnimation();
            } catch (RuntimeException Problem) {
                Animation = UseAnim.NONE;
            }
            if (Animation != UseAnim.DRINK) {
                Solids.add(Scp294Names.normalise(BuiltInRegistries.ITEM.getKey(Thing).getPath()));
            }
        });
        BuiltInRegistries.ENTITY_TYPE.forEach(Type -> Solids.add(Scp294Names.normalise(BuiltInRegistries.ENTITY_TYPE.getKey(Type).getPath())));
        Solids.removeAll(FluidNames.keySet());
        Solids.remove("");
        Catalogue Built = new Catalogue(Map.copyOf(FluidNames), Map.copyOf(PotionNames), Set.copyOf(Solids));
        Names = Built;
        return Built;
    }

    public static Scp294Drink known(String Key) {
        return switch (Key) {
            case "water" -> new Scp294Drink("known:water", "Water", Scp294Drink.cupOf(Component.translatable("block.minecraft.water")), WaterColour,
                    MessagePrefix + "water", List.of(), List.of(Scp294Action.of(Scp294Action.Extinguish)));
            case "lava" -> lava();
            case "milk" -> new Scp294Drink("known:milk", "Milk", Scp294Drink.cupOf("Milk"), MilkColour, MessagePrefix + "milk", List.of(),
                    List.of(Scp294Action.of(Scp294Action.ClearEffects)));
            case "honey" -> new Scp294Drink("known:honey", "Honey", Scp294Drink.cupOf("Honey"), HoneyColour, MessagePrefix + "honey",
                    List.of(new Scp294Dose(ResourceLocation.withDefaultNamespace("regeneration"), 80, 0)), List.of(Scp294Action.of(Scp294Action.Feed, 6.0f, 1.2f)));
            default -> null;
        };
    }

    private static Scp294Drink lava() {
        return new Scp294Drink("known:lava", "Lava", Scp294Drink.cupOf(Component.translatable("block.minecraft.lava")), LavaColour, MessagePrefix + "lava", List.of(),
                List.of(Scp294Action.of(Scp294Action.Ignite, 8.0f), Scp294Action.of(Scp294Action.Damage, 6.0f, "death.attack.scp_unleashed.scp294_lava")));
    }

    public static Scp294Drink fluid(Fluid Liquid, String Key) {
        ResourceLocation Id = BuiltInRegistries.FLUID.getKey(Liquid);
        if (Liquid.isSame(Fluids.WATER)) {
            return known("water");
        }
        if (Liquid.isSame(Fluids.LAVA)) {
            return lava();
        }
        if (Id.getPath().equals("milk")) {
            return known("milk");
        }
        FluidType Type = Liquid.getFluidType();
        Component Inner = Component.translatable(Type.getDescriptionId());
        String Name = Scp294Names.titleCase(Key);
        int Colour = colourOf(Liquid);
        if (Type.getTemperature() >= HotFluid) {
            return new Scp294Drink("fluid:" + Id, Name, Scp294Drink.cupOf(Inner), Colour, MessagePrefix + "lava", List.of(),
                    List.of(Scp294Action.of(Scp294Action.Ignite, 6.0f), Scp294Action.of(Scp294Action.Damage, 5.0f, "death.attack.scp_unleashed.scp294_drink")));
        }
        List<Scp294Dose> Effects = List.of(new Scp294Dose(ResourceLocation.withDefaultNamespace("nausea"), 200, 0),
                new Scp294Dose(ResourceLocation.withDefaultNamespace("hunger"), 600, 0), new Scp294Dose(ResourceLocation.withDefaultNamespace("poison"), 100, 0));
        return new Scp294Drink("fluid:" + Id, Name, Scp294Drink.cupOf(Inner), Colour, MessagePrefix + "fluid", Effects, List.of());
    }

    private static int colourOf(Fluid Liquid) {
        try {
            MapColor Shade = Liquid.defaultFluidState().createLegacyBlock().getMapColor(null, null);
            if (Shade != null && Shade != MapColor.NONE) {
                return Shade.col;
            }
        } catch (RuntimeException Problem) {
            return FluidFallback;
        }
        return FluidFallback;
    }

    public static Scp294Drink potion(Holder<Potion> Brew, String Key) {
        List<Scp294Dose> Effects = new ArrayList<>();
        for (MobEffectInstance Effect : Brew.value().getEffects()) {
            ResourceLocation Id = BuiltInRegistries.MOB_EFFECT.getKey(Effect.getEffect().value());
            if (Id != null) {
                Effects.add(new Scp294Dose(Id, Math.max(1, Effect.getDuration()), Math.min(9, Effect.getAmplifier())));
            }
        }
        String Name = Scp294Names.titleCase(Key);
        return new Scp294Drink("potion:" + BuiltInRegistries.POTION.getKey(Brew.value()), Name, Scp294Drink.cupOf(Name), PotionContents.getColor(Brew) & 0xFFFFFF,
                MessagePrefix + "potion", Effects, List.of());
    }

    public static Scp294Drink mystery(String Key, long Seed) {
        long Hash = Scp294Names.hash(Key, Seed);
        long Second = Scp294Names.mix(Hash);
        long Third = Scp294Names.mix(Second);
        boolean Strange = Math.floorMod(Hash, OddChance) == 0;
        List<Mystery> Pool = Strange ? Odd : Gentle;
        Mystery Pick = Pool.get((int) Math.floorMod(Second, (long) Pool.size()));
        int Span = Pick.MaxSeconds() - Pick.MinSeconds() + 1;
        int Seconds = Pick.MinSeconds() + (int) Math.floorMod(Third, (long) Span);
        float Hue = Math.floorMod(Hash >>> 8, 360L) / 360.0f;
        float Saturation = 0.35f + Math.floorMod(Second >>> 16, 41L) / 100.0f;
        float Brightness = 0.55f + Math.floorMod(Third >>> 16, 38L) / 100.0f;
        int Colour = Mth.hsvToRgb(Hue, Saturation, Brightness) & 0xFFFFFF;
        String Name = Scp294Names.titleCase(Key);
        int Line = (int) Math.floorMod(Third >>> 24, (long) MysteryMessages);
        List<Scp294Action> Actions = new ArrayList<>();
        Actions.add(Scp294Action.of(Scp294Action.Feed, 1.0f + Math.floorMod(Hash >>> 40, 3L), 0.5f));
        List<Scp294Dose> Effects = List.of(new Scp294Dose(ResourceLocation.parse(Pick.Effect()), Seconds * 20, 0));
        return new Scp294Drink("mystery:" + Key.replace(' ', '_'), Name, Scp294Drink.cupOf(Name), Colour, MessagePrefix + "mystery." + Line, Effects, Actions);
    }
}
