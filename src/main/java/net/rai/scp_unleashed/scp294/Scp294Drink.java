package net.rai.scp_unleashed.scp294;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import java.util.List;

public record Scp294Drink(String Id, String Name, Component Title, int Colour, String Message, List<Scp294Dose> Effects, List<Scp294Action> Actions) {
    public static final String CupOfKey = "item.scp_unleashed.scp294_cup.of";
    public static final Codec<Scp294Drink> DrinkCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.STRING.fieldOf("id").forGetter(Scp294Drink::Id),
            Codec.STRING.optionalFieldOf("name", "").forGetter(Scp294Drink::Name),
            ComponentSerialization.CODEC.fieldOf("title").forGetter(Scp294Drink::Title),
            Codec.INT.fieldOf("colour").forGetter(Scp294Drink::Colour),
            Codec.STRING.optionalFieldOf("message", "").forGetter(Scp294Drink::Message),
            Scp294Dose.DoseCodec.listOf().optionalFieldOf("effects", List.of()).forGetter(Scp294Drink::Effects),
            Scp294Action.ActionCodec.listOf().optionalFieldOf("actions", List.of()).forGetter(Scp294Drink::Actions)
    ).apply(Instance, Scp294Drink::new));

    public Scp294Drink {
        Effects = List.copyOf(Effects);
        Actions = List.copyOf(Actions);
        Colour = Colour & 0xFFFFFF;
    }

    public static Component cupOf(Component Inner) {
        return Component.translatable(CupOfKey, Inner);
    }

    public static Component cupOf(String Inner) {
        return cupOf(Component.literal(Inner));
    }

    public boolean has(String Type) {
        for (Scp294Action Action : Actions) {
            if (Action.is(Type)) {
                return true;
            }
        }
        return false;
    }

    public boolean isArsenic() {
        return has(Scp294Action.Arsenic);
    }

    public Component messageText() {
        return Component.translatableWithFallback(Message, Message, Name);
    }
}
