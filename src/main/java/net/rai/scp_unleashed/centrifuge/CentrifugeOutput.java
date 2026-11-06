package net.rai.scp_unleashed.centrifuge;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public record CentrifugeOutput(ItemStack Stack, float Chance) {
    public static final List<String> Keys = List.of("id", "count", "components", "chance");
    public static final Codec<Float> ChanceCodec = Codec.FLOAT.validate(Value -> Value > 0.0f && Value <= 1.0f
            ? DataResult.success(Value) : DataResult.error(() -> "chance must be above 0 and at most 1, got " + Value));
    private static final MapCodec<CentrifugeOutput> Fields = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            ItemStack.ITEM_NON_AIR_CODEC.fieldOf("id").forGetter(Output -> Output.Stack().getItemHolder()),
            ExtraCodecs.intRange(1, 99).optionalFieldOf("count", 1).forGetter(Output -> Output.Stack().getCount()),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(Output -> Output.Stack().getComponentsPatch()),
            ChanceCodec.optionalFieldOf("chance", 1.0f).forGetter(CentrifugeOutput::Chance)
    ).apply(Instance, CentrifugeOutput::of));
    public static final Codec<CentrifugeOutput> OutputCodec = strict(Fields.codec()).validate(CentrifugeOutput::validate);
    public static final StreamCodec<RegistryFriendlyByteBuf, CentrifugeOutput> OutputStreamCodec = StreamCodec.composite(
            ItemStack.STREAM_CODEC, CentrifugeOutput::Stack,
            ByteBufCodecs.FLOAT, CentrifugeOutput::Chance,
            CentrifugeOutput::new);

    private static CentrifugeOutput of(Holder<Item> Product, int Count, DataComponentPatch Patch, float Chance) {
        return new CentrifugeOutput(new ItemStack(Product, Count, Patch), Chance);
    }

    private static DataResult<CentrifugeOutput> validate(CentrifugeOutput Output) {
        ItemStack Stack = Output.Stack();
        DataResult<?> Components = ItemStack.validateComponents(Stack.getComponents());
        if (Components.isError()) {
            return DataResult.error(() -> "result " + Stack.getItemHolder().getRegisteredName() + " has invalid components: "
                    + Components.error().map(DataResult.Error::message).orElse("unknown problem"));
        }
        if (Stack.getCount() > Stack.getMaxStackSize()) {
            return DataResult.error(() -> "result " + Stack.getItemHolder().getRegisteredName() + " has count " + Stack.getCount()
                    + " but stacks only to " + Stack.getMaxStackSize());
        }
        return DataResult.success(Output);
    }

    private static Codec<CentrifugeOutput> strict(Codec<CentrifugeOutput> Inner) {
        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<CentrifugeOutput, T>> decode(DynamicOps<T> Ops, T Encoded) {
                Optional<MapLike<T>> Entries = Ops.getMap(Encoded).result();
                if (Entries.isPresent()) {
                    List<String> Unknown = Entries.get().entries()
                            .map(Entry -> Ops.getStringValue(Entry.getFirst()).result().orElse(String.valueOf(Entry.getFirst())))
                            .filter(Key -> !Keys.contains(Key))
                            .toList();
                    if (!Unknown.isEmpty()) {
                        return DataResult.error(() -> "unknown key(s) " + Unknown + " in a centrifuging result, expected only " + Keys);
                    }
                }
                return Inner.decode(Ops, Encoded);
            }

            @Override
            public <T> DataResult<T> encode(CentrifugeOutput Value, DynamicOps<T> Ops, T Prefix) {
                return Inner.encode(Value, Ops, Prefix);
            }
        };
    }

    public boolean isGuaranteed() {
        return Chance >= 1.0f;
    }

    public int percent() {
        return Math.max(1, Math.round(Chance * 100.0f));
    }

    public ItemStack roll(RandomSource Random) {
        return isGuaranteed() || Random.nextFloat() < Chance ? Stack.copy() : ItemStack.EMPTY;
    }
}
