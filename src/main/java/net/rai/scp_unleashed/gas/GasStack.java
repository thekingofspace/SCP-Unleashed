package net.rai.scp_unleashed.gas;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class GasStack {
    public static final GasStack EMPTY = new GasStack(null, 0);
    public static final Codec<GasStack> CODEC = RecordCodecBuilder.create(Fields -> Fields.group(
            GasRegistry.Gases.byNameCodec().fieldOf("gas").forGetter(GasStack::getGas),
            ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(GasStack::getAmount)
    ).apply(Fields, GasStack::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, GasStack> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(GasRegistry.GasKey), GasStack::getGas,
            ByteBufCodecs.VAR_INT, GasStack::getAmount,
            GasStack::new);

    @Nullable
    private final Gas Kind;
    private final int Amount;

    public GasStack(@Nullable Gas Kind, int Amount) {
        this.Kind = Amount > 0 ? Kind : null;
        this.Amount = Kind == null ? 0 : Math.max(0, Amount);
    }

    public boolean isEmpty() {
        return Kind == null || Amount <= 0;
    }

    @Nullable
    public Gas getGas() {
        return Kind;
    }

    public int getAmount() {
        return isEmpty() ? 0 : Amount;
    }

    public boolean is(@Nullable Gas Other) {
        return !isEmpty() && Kind == Other;
    }

    public boolean isSameGas(GasStack Other) {
        return !isEmpty() && !Other.isEmpty() && Kind == Other.Kind;
    }

    public GasStack copyWithAmount(int NewAmount) {
        return isEmpty() ? EMPTY : new GasStack(Kind, NewAmount);
    }

    public GasStack grow(int Extra) {
        return copyWithAmount(Amount + Extra);
    }

    public GasStack shrink(int Less) {
        return copyWithAmount(Amount - Less);
    }

    public CompoundTag save() {
        CompoundTag Tag = new CompoundTag();
        if (!isEmpty()) {
            Tag.putString("Gas", Kind.getId().toString());
            Tag.putInt("Amount", Amount);
        }
        return Tag;
    }

    public static GasStack load(CompoundTag Tag) {
        if (!Tag.contains("Gas")) {
            return EMPTY;
        }
        ResourceLocation Id = ResourceLocation.tryParse(Tag.getString("Gas"));
        Gas Kind = Id == null ? null : GasRegistry.Gases.get(Id);
        return Kind == null ? EMPTY : new GasStack(Kind, Tag.getInt("Amount"));
    }

    @Override
    public boolean equals(Object Other) {
        return Other instanceof GasStack Stack && Stack.Kind == Kind && Stack.getAmount() == getAmount();
    }

    @Override
    public int hashCode() {
        return Objects.hash(Kind, getAmount());
    }

    @Override
    public String toString() {
        return isEmpty() ? "GasStack[empty]" : "GasStack[" + Kind.getId() + " x" + Amount + "]";
    }
}
