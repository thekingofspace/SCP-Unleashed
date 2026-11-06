package net.rai.scp_unleashed.rocketry.rocket;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.ArrayList;
import java.util.List;

public record RocketryDestinationsPayload(List<RocketDestinations.Entry> Entries) implements CustomPacketPayload {
    public static final int MaxEntries = 64;
    public static final Type<RocketryDestinationsPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "rocketry_destinations"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RocketDestinations.Entry> EntryCodec = StreamCodec.composite(
            RocketDestination.DestinationStreamCodec, RocketDestinations.Entry::Destination,
            ByteBufCodecs.BOOL, RocketDestinations.Entry::Available,
            RocketDestinations.Entry::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, RocketryDestinationsPayload> PayloadCodec = EntryCodec
            .apply(ByteBufCodecs.list(MaxEntries))
            .map(Listed -> new RocketryDestinationsPayload(List.copyOf(Listed)), Payload -> new ArrayList<>(Payload.Entries()));

    @Override
    public Type<RocketryDestinationsPayload> type() {
        return PayloadType;
    }
}
