package net.rai.scp_unleashed.lang;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

public final class LangTree {
    public static final String Separator = ".";
    public static final String SelfKey = "";
    private static final Pattern UnsupportedFormat = Pattern.compile("%(\\d+\\$)?[\\d.]*[df]");

    private LangTree() {
    }

    public static JsonObject read(Reader Source) {
        JsonReader Json = new JsonReader(Source);
        Json.setLenient(true);
        JsonElement Root = JsonParser.parseReader(Json);
        if (!Root.isJsonObject()) {
            throw new JsonParseException("a language file must hold one JSON object, found " + (Root.isJsonArray() ? "an array" : Root.isJsonNull() ? "nothing" : "a value"));
        }
        return Root.getAsJsonObject();
    }

    public static void flatten(JsonObject Root, BiConsumer<String, String> Output) {
        for (Map.Entry<String, JsonElement> Member : Root.entrySet()) {
            if (!Member.getKey().isEmpty()) {
                flattenValue(Member.getKey(), Member.getValue(), Output);
            }
        }
    }

    public static Map<String, String> flatten(JsonObject Root) {
        Map<String, String> Flat = new LinkedHashMap<>();
        flatten(Root, Flat::put);
        return Flat;
    }

    private static void flattenValue(String Key, JsonElement Value, BiConsumer<String, String> Output) {
        if (Value == null || Value.isJsonNull()) {
            return;
        }
        if (Value.isJsonPrimitive()) {
            Output.accept(Key, normalise(Value.getAsString()));
            return;
        }
        if (Value.isJsonArray()) {
            JsonArray Items = Value.getAsJsonArray();
            for (int Index = 0; Index < Items.size(); Index++) {
                flattenValue(Key + Separator + Index, Items.get(Index), Output);
            }
            return;
        }
        for (Map.Entry<String, JsonElement> Member : Value.getAsJsonObject().entrySet()) {
            String Child = Member.getKey().equals(SelfKey) ? Key : Key + Separator + Member.getKey();
            flattenValue(Child, Member.getValue(), Output);
        }
    }

    public static String normalise(String Value) {
        return UnsupportedFormat.matcher(Value).replaceAll("%$1s");
    }
}
