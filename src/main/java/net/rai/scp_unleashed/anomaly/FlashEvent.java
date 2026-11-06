package net.rai.scp_unleashed.anomaly;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

public class FlashEvent extends Event {
    private final ServerLevel World;
    private final Vec3 Centre;
    private final double Radius;
    @Nullable
    private final Entity Source;

    public FlashEvent(ServerLevel World, Vec3 Centre, double Radius, @Nullable Entity Source) {
        this.World = World;
        this.Centre = Centre;
        this.Radius = Radius;
        this.Source = Source;
    }

    public ServerLevel world() {
        return World;
    }

    public Vec3 centre() {
        return Centre;
    }

    public double radius() {
        return Radius;
    }

    @Nullable
    public Entity source() {
        return Source;
    }

    public static void fire(ServerLevel World, Vec3 Centre, double Radius, @Nullable Entity Source) {
        NeoForge.EVENT_BUS.post(new FlashEvent(World, Centre, Radius, Source));
    }
}
