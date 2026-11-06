package net.rai.scp_unleashed.scp131;

import software.bernie.geckolib.animatable.client.GeoRenderProvider;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class Scp131Cues {
    public static volatile BiConsumer<Scp131Pod, Byte> Handler = (Pod, Kind) -> {
    };
    public static volatile Consumer<Consumer<GeoRenderProvider>> Renderer = Accept -> {
    };

    private Scp131Cues() {
    }
}
