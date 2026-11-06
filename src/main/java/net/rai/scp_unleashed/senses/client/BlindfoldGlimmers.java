package net.rai.scp_unleashed.senses.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class BlindfoldGlimmers {
    public static final int Radius = 8;
    public static final int MinEmission = 7;
    public static final int MaxSpots = 24;
    public static final int ScanEvery = 10;

    public record Spot(Vec3 Centre, int Emission) {
    }

    private static List<Spot> Spots = List.of();
    private static double Fov = 70.0;

    private BlindfoldGlimmers() {
    }

    public static List<Spot> spots() {
        return Spots;
    }

    public static double fov() {
        return Fov;
    }

    @SubscribeEvent
    public static void captureFov(ViewportEvent.ComputeFov Event) {
        if (Event.usedConfiguredFov()) {
            Fov = Event.getFOV();
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post Event) {
        Minecraft Client = Minecraft.getInstance();
        LocalPlayer User = Client.player;
        if (User == null || !BlindfoldOverlay.active(Client)) {
            if (!Spots.isEmpty()) {
                Spots = List.of();
            }
            return;
        }
        if ((User.tickCount + User.getId()) % ScanEvery != 0) {
            return;
        }
        Spots = scan(User.level(), User.getEyePosition());
    }

    public static List<Spot> scan(Level World, Vec3 Eye) {
        List<Spot> Found = new ArrayList<>();
        BlockPos Origin = BlockPos.containing(Eye);
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Dx = -Radius; Dx <= Radius; Dx++) {
            for (int Dy = -Radius; Dy <= Radius; Dy++) {
                for (int Dz = -Radius; Dz <= Radius; Dz++) {
                    Cursor.set(Origin.getX() + Dx, Origin.getY() + Dy, Origin.getZ() + Dz);
                    if (!World.isLoaded(Cursor)) {
                        continue;
                    }
                    int Emission = World.getBlockState(Cursor).getLightEmission(World, Cursor);
                    if (Emission >= MinEmission) {
                        Found.add(new Spot(Vec3.atCenterOf(Cursor), Emission));
                    }
                }
            }
        }
        Found.sort(Comparator.comparingDouble(Each -> Each.Centre().distanceToSqr(Eye)));
        return Found.size() > MaxSpots ? List.copyOf(Found.subList(0, MaxSpots)) : List.copyOf(Found);
    }
}
