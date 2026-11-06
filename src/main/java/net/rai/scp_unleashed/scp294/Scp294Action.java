package net.rai.scp_unleashed.scp294;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record Scp294Action(String Type, float Amount, float Extra, String Text) {
    public static final String Heal = "heal";
    public static final String Damage = "damage";
    public static final String Ignite = "ignite";
    public static final String Levitate = "levitate";
    public static final String TeleportRandom = "teleport_random";
    public static final String ClearEffects = "clear_effects";
    public static final String Feed = "feed";
    public static final String Extinguish = "extinguish";
    public static final String Glow = "glow";
    public static final String Explode = "explode";
    public static final String Vanish = "vanish";
    public static final String Arsenic = "arsenic";
    public static final String Sound = "sound";
    public static final String Particles = "particles";
    public static final List<String> Types = List.of(Heal, Damage, Ignite, Levitate, TeleportRandom, ClearEffects, Feed, Extinguish, Glow, Explode, Vanish, Arsenic, Sound, Particles);

    public static final Codec<Scp294Action> ActionCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.STRING.fieldOf("type").forGetter(Scp294Action::Type),
            Codec.FLOAT.optionalFieldOf("amount", 0.0f).forGetter(Scp294Action::Amount),
            Codec.FLOAT.optionalFieldOf("extra", 0.0f).forGetter(Scp294Action::Extra),
            Codec.STRING.optionalFieldOf("text", "").forGetter(Scp294Action::Text)
    ).apply(Instance, Scp294Action::new));

    public static Scp294Action of(String Type) {
        return new Scp294Action(Type, 0.0f, 0.0f, "");
    }

    public static Scp294Action of(String Type, float Amount) {
        return new Scp294Action(Type, Amount, 0.0f, "");
    }

    public static Scp294Action of(String Type, float Amount, float Extra) {
        return new Scp294Action(Type, Amount, Extra, "");
    }

    public static Scp294Action of(String Type, float Amount, String Text) {
        return new Scp294Action(Type, Amount, 0.0f, Text);
    }

    public boolean is(String Wanted) {
        return Type.equals(Wanted);
    }
}
