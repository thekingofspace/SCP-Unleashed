package net.rai.scp_unleashed.arrival;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record ArrivalSeen(List<ResourceLocation> Places) {
    public static final ArrivalSeen Fresh = new ArrivalSeen(List.of());
    public static final Codec<ArrivalSeen> SeenCodec = ResourceLocation.CODEC.listOf().xmap(ArrivalSeen::new, ArrivalSeen::Places);

    public ArrivalSeen {
        Places = List.copyOf(Places);
    }

    public boolean has(ResourceLocation Place) {
        return Places.contains(Place);
    }

    public ArrivalSeen with(ResourceLocation Place) {
        if (has(Place)) {
            return this;
        }
        List<ResourceLocation> Grown = new ArrayList<>(Places);
        Grown.add(Place);
        return new ArrivalSeen(Grown);
    }

    public boolean empty() {
        return Places.isEmpty();
    }
}
