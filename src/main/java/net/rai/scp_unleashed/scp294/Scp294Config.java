package net.rai.scp_unleashed.scp294;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.logging.LogUtils;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Scp294Config {
    public static final String FolderName = "scp_unleashed";
    public static final String FileName = "scp294_drinks.json";
    public static final String DefaultsResource = "/data/scp_unleashed/scp294/default_drinks.json";
    public static final int DefaultColour = 0xB08A5A;
    public static final int MaxDisplay = 48;
    public static final int MaxMessage = 160;
    public static final int MaxSeconds = 3600;
    public static final int MaxAmplifier = 9;
    private static final Logger Log = LogUtils.getLogger();
    private static volatile Map<String, Entry> Table = Map.of();
    private static volatile List<Entry> Entries = List.of();
    private static volatile Path Loaded;

    private Scp294Config() {
    }

    public record Entry(String Name, List<String> Aliases, boolean Refuse, Scp294Drink Drink) {
    }

    public record Parsed(List<Entry> Entries, List<String> Warnings, String Failure) {
        public boolean failed() {
            return Failure != null;
        }
    }

    public record Report(Path File, int Count, List<String> Warnings, String Failure, boolean Created) {
        public boolean failed() {
            return Failure != null;
        }
    }

    public static Path file(MinecraftServer Server) {
        return Server.getWorldPath(LevelResource.ROOT).resolve(FolderName).resolve(FileName).normalize();
    }

    public static String defaults() {
        try (InputStream Stream = Scp294Config.class.getResourceAsStream(DefaultsResource)) {
            if (Stream == null) {
                Log.warn("SCP-294 default drinks are missing from the mod jar ({}); starting with an empty list", DefaultsResource);
                return "{\n  \"version\": 1,\n  \"drinks\": []\n}\n";
            }
            return new String(Stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException Problem) {
            Log.warn("SCP-294 default drinks could not be read: {}", Problem.getMessage());
            return "{\n  \"version\": 1,\n  \"drinks\": []\n}\n";
        }
    }

    public static Report load(MinecraftServer Server) {
        Path Target = file(Server);
        boolean Created = false;
        String Text;
        try {
            if (!Files.exists(Target)) {
                Files.createDirectories(Target.getParent());
                Files.writeString(Target, defaults(), StandardCharsets.UTF_8);
                Created = true;
                Log.info("SCP-294 wrote the default drinks for this world to {}", Target);
            }
            Text = Files.readString(Target, StandardCharsets.UTF_8);
        } catch (IOException Problem) {
            String Failure = Problem.getClass().getSimpleName() + ": " + Problem.getMessage();
            Log.warn("SCP-294 drinks file {} could not be read, keeping {} drinks: {}", Target, Entries.size(), Failure);
            return new Report(Target, Entries.size(), List.of(), Failure, false);
        }
        Parsed Result = parse(Text);
        for (String Warning : Result.Warnings()) {
            Log.warn("SCP-294 drinks file {}: {}", Target, Warning);
        }
        if (Result.failed()) {
            Log.warn("SCP-294 drinks file {} is not valid, keeping {} drinks: {}", Target, Entries.size(), Result.Failure());
            return new Report(Target, Entries.size(), Result.Warnings(), Result.Failure(), Created);
        }
        apply(Result.Entries());
        Loaded = Target;
        Log.info("SCP-294 loaded {} drinks from {}", Result.Entries().size(), Target);
        return new Report(Target, Result.Entries().size(), Result.Warnings(), null, Created);
    }

    public static void apply(List<Entry> Fresh) {
        Map<String, Entry> Lookup = new LinkedHashMap<>();
        for (Entry Drink : Fresh) {
            Lookup.putIfAbsent(Drink.Name(), Drink);
            for (String Alias : Drink.Aliases()) {
                Lookup.putIfAbsent(Alias, Drink);
            }
        }
        Entries = List.copyOf(Fresh);
        Table = Collections.unmodifiableMap(Lookup);
    }

    public static void clear() {
        Entries = List.of();
        Table = Map.of();
        Loaded = null;
    }

    public static Optional<Entry> find(String Key) {
        return Optional.ofNullable(Table.get(Key));
    }

    public static List<Entry> entries() {
        return Entries;
    }

    public static Path loadedFrom() {
        return Loaded;
    }

    public static Parsed parse(String Text) {
        List<String> Warnings = new ArrayList<>();
        JsonElement Root;
        try {
            Root = JsonParser.parseString(Text);
        } catch (RuntimeException Problem) {
            return new Parsed(List.of(), Warnings, "not valid JSON (" + Problem.getMessage() + ")");
        }
        if (!Root.isJsonObject()) {
            return new Parsed(List.of(), Warnings, "the file must hold one JSON object");
        }
        JsonObject Top = Root.getAsJsonObject();
        if (!Top.has("drinks") || !Top.get("drinks").isJsonArray()) {
            return new Parsed(List.of(), Warnings, "the object needs a \"drinks\" list");
        }
        JsonArray Listed = Top.getAsJsonArray("drinks");
        List<Entry> Result = new ArrayList<>();
        Map<String, String> Taken = new LinkedHashMap<>();
        for (int Index = 0; Index < Listed.size(); Index++) {
            Entry Made = entry(Listed.get(Index), Index, Taken, Warnings);
            if (Made != null) {
                Result.add(Made);
            }
        }
        return new Parsed(Result, Warnings, null);
    }

    private static Entry entry(JsonElement Element, int Index, Map<String, String> Taken, List<String> Warnings) {
        String Where = "drink " + (Index + 1);
        if (!Element.isJsonObject()) {
            Warnings.add(Where + " is not an object, skipped");
            return null;
        }
        JsonObject Fields = Element.getAsJsonObject();
        String RawName = string(Fields, "name");
        if (RawName == null) {
            Warnings.add(Where + " has no \"name\", skipped");
            return null;
        }
        String Name = Scp294Names.normalise(RawName);
        Where = Where + " (" + RawName + ")";
        if (Name.isEmpty() || Name.length() > Scp294Names.MaxTyped) {
            Warnings.add(Where + " needs a name of 1 to " + Scp294Names.MaxTyped + " letters or digits, skipped");
            return null;
        }
        if (Taken.containsKey(Name)) {
            Warnings.add(Where + " uses a name already taken by " + Taken.get(Name) + ", skipped");
            return null;
        }
        int Colour = DefaultColour;
        JsonElement ColourValue = Fields.has("colour") ? Fields.get("colour") : Fields.get("color");
        if (ColourValue != null) {
            Integer Read = colour(ColourValue);
            if (Read == null) {
                Warnings.add(Where + " has a colour that is not #RRGGBB, skipped");
                return null;
            }
            Colour = Read;
        }
        List<String> Aliases = new ArrayList<>();
        if (Fields.has("aliases")) {
            if (!Fields.get("aliases").isJsonArray()) {
                Warnings.add(Where + ": \"aliases\" must be a list of names, ignored");
            } else {
                for (JsonElement AliasValue : Fields.getAsJsonArray("aliases")) {
                    String Alias = AliasValue.isJsonPrimitive() && AliasValue.getAsJsonPrimitive().isString() ? Scp294Names.normalise(AliasValue.getAsString()) : "";
                    if (Alias.isEmpty() || Alias.length() > Scp294Names.MaxTyped) {
                        Warnings.add(Where + ": alias " + AliasValue + " is empty or too long, ignored");
                    } else if (Alias.equals(Name) || Aliases.contains(Alias)) {
                        continue;
                    } else if (Taken.containsKey(Alias)) {
                        Warnings.add(Where + ": alias \"" + Alias + "\" is already taken by " + Taken.get(Alias) + ", ignored");
                    } else {
                        Aliases.add(Alias);
                    }
                }
            }
        }
        String Display = string(Fields, "display");
        if (Display != null && (Display.isBlank() || Display.length() > MaxDisplay)) {
            Warnings.add(Where + ": \"display\" must be 1 to " + MaxDisplay + " characters, ignored");
            Display = null;
        }
        String Message = string(Fields, "message");
        if (Message != null && Message.length() > MaxMessage) {
            Warnings.add(Where + ": \"message\" is longer than " + MaxMessage + " characters and was cut");
            Message = Message.substring(0, MaxMessage);
        }
        boolean Refuse = Fields.has("refuse") && Fields.get("refuse").isJsonPrimitive() && Fields.get("refuse").getAsJsonPrimitive().isBoolean() && Fields.get("refuse").getAsBoolean();
        List<Scp294Dose> Effects = new ArrayList<>();
        if (Fields.has("effects")) {
            if (!Fields.get("effects").isJsonArray()) {
                Warnings.add(Where + ": \"effects\" must be a list, ignored");
            } else {
                for (JsonElement EffectValue : Fields.getAsJsonArray("effects")) {
                    Scp294Dose Dose = dose(EffectValue, Where, Warnings);
                    if (Dose != null) {
                        Effects.add(Dose);
                    }
                }
            }
        }
        List<Scp294Action> Actions = new ArrayList<>();
        if (Fields.has("actions")) {
            if (!Fields.get("actions").isJsonArray()) {
                Warnings.add(Where + ": \"actions\" must be a list, ignored");
            } else {
                for (JsonElement ActionValue : Fields.getAsJsonArray("actions")) {
                    Scp294Action Action = action(ActionValue, Where, Warnings);
                    if (Action != null) {
                        Actions.add(Action);
                    }
                }
            }
        }
        Taken.put(Name, RawName);
        for (String Alias : Aliases) {
            Taken.put(Alias, RawName);
        }
        String Pretty = Scp294Names.titleCase(Name);
        Component Title = Display != null ? Component.literal(Display) : Scp294Drink.cupOf(Pretty);
        Scp294Drink Drink = new Scp294Drink("scp294:" + Name.replace(' ', '_'), Pretty, Title, Colour, Message == null ? "" : Message, Effects, Actions);
        return new Entry(Name, List.copyOf(Aliases), Refuse, Drink);
    }

    private static Scp294Dose dose(JsonElement Value, String Where, List<String> Warnings) {
        if (!Value.isJsonObject()) {
            Warnings.add(Where + ": an effect is not an object, ignored");
            return null;
        }
        JsonObject Fields = Value.getAsJsonObject();
        String Id = string(Fields, "id");
        ResourceLocation Location = Id == null ? null : ResourceLocation.tryParse(Id);
        if (Location == null || !BuiltInRegistries.MOB_EFFECT.containsKey(Location)) {
            Warnings.add(Where + ": unknown effect " + (Id == null ? "(no id)" : Id) + ", ignored");
            return null;
        }
        double Seconds = number(Fields, "seconds", 30.0);
        double Amplifier = number(Fields, "amplifier", 0.0);
        if (Seconds < 0.05 || Seconds > MaxSeconds || Amplifier < 0 || Amplifier > MaxAmplifier) {
            Warnings.add(Where + ": effect " + Id + " needs seconds 0.05 to " + MaxSeconds + " and amplifier 0 to " + MaxAmplifier + ", clamped");
        }
        int Ticks = (int) Math.round(Math.max(1.0, Math.min(MaxSeconds * 20.0, Seconds * 20.0)));
        int Strength = (int) Math.max(0, Math.min(MaxAmplifier, Math.round(Amplifier)));
        return new Scp294Dose(Location, Ticks, Strength);
    }

    private static Scp294Action action(JsonElement Value, String Where, List<String> Warnings) {
        if (!Value.isJsonObject()) {
            Warnings.add(Where + ": an action is not an object, ignored");
            return null;
        }
        JsonObject Fields = Value.getAsJsonObject();
        String Type = string(Fields, "type");
        if (Type == null || !Scp294Action.Types.contains(Type)) {
            Warnings.add(Where + ": unknown action " + (Type == null ? "(no type)" : Type) + ", ignored");
            return null;
        }
        return switch (Type) {
            case Scp294Action.Heal -> Scp294Action.of(Type, clamp(Fields, "amount", 4.0, 0.0, 100.0, Where, Warnings));
            case Scp294Action.Damage -> {
                String Message = string(Fields, "death_message");
                yield Scp294Action.of(Type, clamp(Fields, "amount", 4.0, 0.0, 1000.0, Where, Warnings), Message == null ? "" : Message);
            }
            case Scp294Action.Ignite -> Scp294Action.of(Type, clamp(Fields, "seconds", 5.0, 0.0, 60.0, Where, Warnings));
            case Scp294Action.Levitate -> Scp294Action.of(Type, clamp(Fields, "seconds", 3.0, 0.0, 60.0, Where, Warnings), clamp(Fields, "amplifier", 0.0, 0.0, 9.0, Where, Warnings));
            case Scp294Action.TeleportRandom -> Scp294Action.of(Type, clamp(Fields, "radius", 16.0, 1.0, 64.0, Where, Warnings));
            case Scp294Action.Feed -> Scp294Action.of(Type, clamp(Fields, "food", 4.0, 0.0, 20.0, Where, Warnings), clamp(Fields, "saturation", 2.0, 0.0, 20.0, Where, Warnings));
            case Scp294Action.Glow -> Scp294Action.of(Type, clamp(Fields, "seconds", 10.0, 0.0, MaxSeconds, Where, Warnings));
            case Scp294Action.Explode -> Scp294Action.of(Type, clamp(Fields, "power", 1.0, 0.5, 4.0, Where, Warnings));
            case Scp294Action.Vanish -> Scp294Action.of(Type, clamp(Fields, "seconds", 6.0, 2.0, 30.0, Where, Warnings));
            case Scp294Action.Sound -> {
                String Id = string(Fields, "id");
                if (Id == null || ResourceLocation.tryParse(Id) == null) {
                    Warnings.add(Where + ": sound action needs an id like minecraft:block.bell.use, ignored");
                    yield null;
                }
                yield new Scp294Action(Type, clamp(Fields, "volume", 1.0, 0.0, 4.0, Where, Warnings), clamp(Fields, "pitch", 1.0, 0.5, 2.0, Where, Warnings), Id);
            }
            case Scp294Action.Particles -> {
                String Id = string(Fields, "id");
                ResourceLocation Location = Id == null ? null : ResourceLocation.tryParse(Id);
                ParticleType<?> Kind = Location == null ? null : BuiltInRegistries.PARTICLE_TYPE.get(Location);
                if (!(Kind instanceof SimpleParticleType)) {
                    Warnings.add(Where + ": particles action needs a simple particle id like minecraft:end_rod, ignored");
                    yield null;
                }
                yield Scp294Action.of(Type, clamp(Fields, "count", 12.0, 1.0, 200.0, Where, Warnings), Id);
            }
            default -> Scp294Action.of(Type);
        };
    }

    private static float clamp(JsonObject Fields, String Key, double Fallback, double Low, double High, String Where, List<String> Warnings) {
        double Value = number(Fields, Key, Fallback);
        if (Value < Low || Value > High) {
            Warnings.add(Where + ": \"" + Key + "\" must be from " + Low + " to " + High + ", clamped");
        }
        return (float) Math.max(Low, Math.min(High, Value));
    }

    private static double number(JsonObject Fields, String Key, double Fallback) {
        JsonElement Value = Fields.get(Key);
        if (Value instanceof JsonPrimitive Primitive && Primitive.isNumber()) {
            double Result = Primitive.getAsDouble();
            return Double.isFinite(Result) ? Result : Fallback;
        }
        return Fallback;
    }

    private static String string(JsonObject Fields, String Key) {
        JsonElement Value = Fields.get(Key);
        if (Value instanceof JsonPrimitive Primitive && Primitive.isString()) {
            return Primitive.getAsString();
        }
        return null;
    }

    private static Integer colour(JsonElement Value) {
        if (!(Value instanceof JsonPrimitive Primitive) || !Primitive.isString()) {
            return null;
        }
        String Text = Primitive.getAsString().trim();
        if (Text.startsWith("#")) {
            Text = Text.substring(1);
        }
        if (Text.length() != 6) {
            return null;
        }
        try {
            return Integer.parseInt(Text, 16);
        } catch (NumberFormatException Problem) {
            return null;
        }
    }
}
