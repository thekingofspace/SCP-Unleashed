package net.rai.scp_unleashed.scp111;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public record Scp111Egg(Scp111Variant Variant, int Stage) {
    public static final int MaxStage = 2;
    public static final int StageTicks = 3000;
    public static final int StageJitter = 600;
    public static final int TotalTicks = (MaxStage + 1) * StageTicks;
    public static final Scp111Egg Fresh = new Scp111Egg(Scp111Variant.Ember, 0);
    public static final Codec<Scp111Egg> EggCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Scp111Variant.VariantCodec.fieldOf("variant").forGetter(Scp111Egg::Variant),
            Codec.intRange(0, MaxStage).optionalFieldOf("stage", 0).forGetter(Scp111Egg::Stage)
    ).apply(Instance, Scp111Egg::new));
    public static final StreamCodec<ByteBuf, Scp111Egg> EggStreamCodec = StreamCodec.composite(
            Scp111Variant.VariantStreamCodec, Scp111Egg::Variant,
            ByteBufCodecs.VAR_INT, Scp111Egg::Stage,
            Scp111Egg::new);

    public Scp111Egg {
        Stage = Mth.clamp(Stage, 0, MaxStage);
    }

    public static Scp111Egg of(ItemStack Stack) {
        return Stack.getOrDefault(Scp111Registry.EggData.get(), Fresh);
    }

    public static Scp111Egg fromRemaining(Scp111Variant Variant, int Remaining) {
        int Left = Mth.positiveCeilDiv(Math.max(0, Remaining), StageTicks);
        return new Scp111Egg(Variant, MaxStage + 1 - Math.max(1, Left));
    }

    public int remaining() {
        return (MaxStage + 1 - Stage) * StageTicks;
    }

    public Scp111Clutch.Entry entry() {
        return new Scp111Clutch.Entry(Variant, remaining());
    }

    public ItemStack stack(int Count) {
        ItemStack Made = new ItemStack(Scp111Registry.EggItem.get(), Count);
        Made.set(Scp111Registry.EggData.get(), this);
        return Made;
    }
}
