package net.rai.scp_unleashed.fluiddump.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.fluiddump.PollutionMath;
import net.rai.scp_unleashed.fluiddump.network.PollutionPatch;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class PollutionClient {
    public static final double FogVisible = 0.02;
    public static final float FogDarken = 0.55f;
    private static final PollutionPatch[] None = new PollutionPatch[0];
    private static volatile PollutionPatch[] Patches = None;

    private PollutionClient() {
    }

    public static double densityAt(double X, double Y, double Z) {
        PollutionPatch[] Current = Patches;
        double Total = 0.0;
        for (PollutionPatch Patch : Current) {
            double Here = Patch.densityAt(X, Y, Z);
            if (Here > 0.0) {
                Total = PollutionMath.combine(Total, Here);
            }
        }
        return Total;
    }

    public static int tint(int Water, BlockPos Pos) {
        if (Patches.length == 0) {
            return Water;
        }
        double Density = densityAt(Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5);
        return Density <= 0.0 ? Water : PollutionMath.tint(Water, Density);
    }

    public static void receive(List<PollutionPatch> Incoming) {
        PollutionPatch[] Old = Patches;
        PollutionPatch[] Fresh = Incoming.isEmpty() ? None : Incoming.toArray(new PollutionPatch[0]);
        Patches = Fresh;
        refresh(Old, Fresh);
    }

    public static void forget() {
        Patches = None;
    }

    private static void refresh(PollutionPatch[] Old, PollutionPatch[] Fresh) {
        ClientLevel World = Minecraft.getInstance().level;
        if (World == null) {
            return;
        }
        for (PollutionPatch Before : Old) {
            PollutionPatch After = match(Fresh, Before);
            if (After == null) {
                rebuild(World, Before, Before.Radius());
            } else if (!After.looksLike(Before)) {
                rebuild(World, Before, Math.max(Before.Radius(), After.Radius()));
            }
        }
        for (PollutionPatch After : Fresh) {
            if (match(Old, After) == null) {
                rebuild(World, After, After.Radius());
            }
        }
    }

    @Nullable
    private static PollutionPatch match(PollutionPatch[] Within, PollutionPatch Wanted) {
        for (PollutionPatch Candidate : Within) {
            if (Candidate.samePlace(Wanted)) {
                return Candidate;
            }
        }
        return null;
    }

    private static void rebuild(ClientLevel World, PollutionPatch Patch, double Radius) {
        Minecraft Client = Minecraft.getInstance();
        Camera View = Client.gameRenderer.getMainCamera();
        int Reach = (int) Math.ceil(Radius) + 1;
        int Vertical = (int) Math.ceil(PollutionMath.verticalReach(Radius)) + 1;
        int Distance = Client.options.getEffectiveRenderDistance() + 1;
        int CameraX = SectionPos.blockToSectionCoord(View.getBlockPosition().getX());
        int CameraZ = SectionPos.blockToSectionCoord(View.getBlockPosition().getZ());
        int MinX = Math.max(CameraX - Distance, SectionPos.blockToSectionCoord(Patch.X() - Reach));
        int MaxX = Math.min(CameraX + Distance, SectionPos.blockToSectionCoord(Patch.X() + Reach));
        int MinZ = Math.max(CameraZ - Distance, SectionPos.blockToSectionCoord(Patch.Z() - Reach));
        int MaxZ = Math.min(CameraZ + Distance, SectionPos.blockToSectionCoord(Patch.Z() + Reach));
        int MinY = Math.max(World.getMinSection(), SectionPos.blockToSectionCoord(Patch.Y() - Vertical));
        int MaxY = Math.min(World.getMaxSection() - 1, SectionPos.blockToSectionCoord(Patch.Y() + Vertical));
        for (int SectionX = MinX; SectionX <= MaxX; SectionX++) {
            for (int SectionZ = MinZ; SectionZ <= MaxZ; SectionZ++) {
                for (int SectionY = MinY; SectionY <= MaxY; SectionY++) {
                    Client.levelRenderer.setSectionDirty(SectionX, SectionY, SectionZ);
                }
            }
        }
    }

    private static double cameraDensity(Camera View) {
        if (Patches.length == 0 || View.getFluidInCamera() != FogType.WATER) {
            return 0.0;
        }
        Vec3 Eye = View.getPosition();
        return densityAt(Eye.x, Eye.y, Eye.z);
    }

    @SubscribeEvent
    public static void fogColour(ViewportEvent.ComputeFogColor Event) {
        double Density = cameraDensity(Event.getCamera());
        if (Density < FogVisible) {
            return;
        }
        float Share = (float) PollutionMath.tintShare(Density) * 0.85f;
        float Red = ((PollutionMath.Murk >> 16) & 255) / 255.0f * FogDarken;
        float Green = ((PollutionMath.Murk >> 8) & 255) / 255.0f * FogDarken;
        float Blue = (PollutionMath.Murk & 255) / 255.0f * FogDarken;
        Event.setRed(Event.getRed() + (Red - Event.getRed()) * Share);
        Event.setGreen(Event.getGreen() + (Green - Event.getGreen()) * Share);
        Event.setBlue(Event.getBlue() + (Blue - Event.getBlue()) * Share);
    }

    @SubscribeEvent
    public static void fogDistance(ViewportEvent.RenderFog Event) {
        if (Event.getType() != FogType.WATER) {
            return;
        }
        double Density = cameraDensity(Event.getCamera());
        if (Density < FogVisible) {
            return;
        }
        Event.scaleFarPlaneDistance(1.0f - PollutionMath.FogFarShare * (float) PollutionMath.tintShare(Density));
        Event.setCanceled(true);
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        forget();
    }
}
