package net.rai.scp_unleashed.storage.gel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record GelPacked(ItemStack Contents, int Gel, int Hold) {
    public static final Codec<GelPacked> PackedCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("contents", ItemStack.EMPTY).forGetter(GelPacked::Contents),
            Codec.INT.optionalFieldOf("gel", 0).forGetter(GelPacked::Gel),
            Codec.INT.optionalFieldOf("hold", 0).forGetter(GelPacked::Hold)
    ).apply(Instance, GelPacked::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, GelPacked> PackedStreamCodec = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC, GelPacked::Contents,
            ByteBufCodecs.VAR_INT, GelPacked::Gel,
            ByteBufCodecs.VAR_INT, GelPacked::Hold,
            GelPacked::new);

    public GelPacked {
        Contents = Contents.copy();
        Gel = Math.max(0, Math.min(GelRules.MaxGel, Gel));
        Hold = Math.max(0, Math.min(GelRules.HoldTicks, Hold));
    }

    public boolean sealed() {
        return Gel >= GelRules.SealLevel;
    }

    public GelPacked aged(int Ticks) {
        int Left = Ticks;
        int NewHold = Hold;
        if (NewHold > 0) {
            int Used = Math.min(NewHold, Left);
            NewHold -= Used;
            Left -= Used;
        }
        return new GelPacked(Contents, Gel - Left * GelRules.DecayRate, NewHold);
    }

    public GelPacked withContents(ItemStack Stack) {
        return new GelPacked(Stack, Gel, Hold);
    }

    @Override
    public boolean equals(Object Other) {
        return Other instanceof GelPacked That && Gel == That.Gel && Hold == That.Hold && ItemStack.matches(Contents, That.Contents);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * ItemStack.hashItemAndComponents(Contents) + Gel) + Hold;
    }
}
