package net.rai.scp_unleashed.cameras;

import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public record CameraTrackingView(ChunkTrackingView.Positioned Body, ChunkTrackingView.Positioned Lens) implements ChunkTrackingView {
    @Override
    public boolean contains(int X, int Z, boolean Outer) {
        return Body.contains(X, Z, Outer) || Lens.contains(X, Z, Outer);
    }

    @Override
    public void forEach(Consumer<ChunkPos> Action) {
        Body.forEach(Action);
        Lens.forEach(Pos -> {
            if (!Body.contains(Pos)) {
                Action.accept(Pos);
            }
        });
    }

    @Nullable
    public static ChunkPos centre(ChunkTrackingView Tracked) {
        if (Tracked instanceof CameraTrackingView Joined) {
            return Joined.Body().center();
        }
        return Tracked instanceof ChunkTrackingView.Positioned Single ? Single.center() : null;
    }

    @Nullable
    public static ChunkPos lensCentre(ChunkTrackingView Tracked) {
        return Tracked instanceof CameraTrackingView Joined ? Joined.Lens().center() : null;
    }

    public static List<ChunkPos> leaving(ChunkTrackingView From, ChunkTrackingView To) {
        List<ChunkPos> Found = new ArrayList<>();
        From.forEach(Pos -> {
            if (!To.contains(Pos)) {
                Found.add(Pos);
            }
        });
        return Found;
    }
}
