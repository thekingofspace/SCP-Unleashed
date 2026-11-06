package net.rai.scp_unleashed.scp458;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntFunction;

public enum Scp458Pizza implements StringRepresentable {
    Hawaiian("hawaiian"),
    Cheese("cheese"),
    Pepperoni("pepperoni"),
    Margherita("margherita"),
    MeatLovers("meat_lovers"),
    Supreme("supreme"),
    Veggie("veggie"),
    Mushroom("mushroom"),
    BbqChicken("bbq_chicken"),
    Buffalo("buffalo"),
    FourCheese("four_cheese"),
    WhiteGarlic("white_garlic"),
    Pesto("pesto"),
    SausagePepper("sausage_pepper"),
    OliveOnion("olive_onion"),
    SpicyPepper("spicy_pepper");

    public static final String NamePrefix = "pizza.scp_unleashed.scp458.";
    public static final Codec<Scp458Pizza> PizzaCodec = StringRepresentable.fromEnum(Scp458Pizza::values);
    public static final IntFunction<Scp458Pizza> ById = ByIdMap.continuous(Scp458Pizza::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final StreamCodec<ByteBuf, Scp458Pizza> PizzaStreamCodec = ByteBufCodecs.idMapper(ById, Scp458Pizza::ordinal);

    private final String Id;

    Scp458Pizza(String Id) {
        this.Id = Id;
    }

    public String id() {
        return Id;
    }

    @Override
    public String getSerializedName() {
        return Id;
    }

    public String nameKey() {
        return NamePrefix + Id;
    }

    public Component displayName() {
        return Component.translatable(nameKey());
    }

    public static Scp458Pizza random(RandomSource Random) {
        Scp458Pizza[] All = values();
        return All[Random.nextInt(All.length)];
    }

    @Nullable
    public static Scp458Pizza byId(String Name) {
        for (Scp458Pizza Each : values()) {
            if (Each.Id.equals(Name)) {
                return Each;
            }
        }
        return null;
    }
}
