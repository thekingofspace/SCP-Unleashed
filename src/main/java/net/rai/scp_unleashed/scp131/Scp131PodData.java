package net.rai.scp_unleashed.scp131;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

public record Scp131PodData(Scp131Colour Colour, float Health, int Voice) {
    public static final Scp131PodData Fresh = new Scp131PodData(Scp131Colour.Red, (float) Scp131Pod.Health, 0);
    public static final Codec<Scp131PodData> PodCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Scp131Colour.ColourCodec.fieldOf("colour").forGetter(Scp131PodData::Colour),
            Codec.floatRange(0.0f, 1024.0f).optionalFieldOf("health", (float) Scp131Pod.Health).forGetter(Scp131PodData::Health),
            Codec.INT.optionalFieldOf("voice", 0).forGetter(Scp131PodData::Voice)
    ).apply(Instance, Scp131PodData::new));
    public static final StreamCodec<ByteBuf, Scp131PodData> PodStreamCodec = StreamCodec.composite(
            Scp131Colour.ColourStreamCodec, Scp131PodData::Colour,
            ByteBufCodecs.FLOAT, Scp131PodData::Health,
            ByteBufCodecs.VAR_INT, Scp131PodData::Voice,
            Scp131PodData::new);

    public Scp131PodData {
        Health = Mth.clamp(Health, 0.0f, 1024.0f);
    }

    public static Scp131PodData of(ItemStack Stack) {
        return Stack.getOrDefault(Scp131Registry.PodData.get(), Fresh);
    }

    public static Scp131PodData fresh(Scp131Colour Colour, RandomSource Random) {
        return new Scp131PodData(Colour, (float) Scp131Pod.Health, Random.nextInt());
    }

    public ItemStack stack() {
        ItemStack Made = new ItemStack(Scp131Registry.PodItem.get());
        Made.set(Scp131Registry.PodData.get(), this);
        return Made;
    }

    public long seed() {
        return Voice * 2654435761L + Colour.ordinal() * 97L;
    }
}
