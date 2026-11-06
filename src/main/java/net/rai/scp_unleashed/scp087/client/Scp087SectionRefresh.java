package net.rai.scp_unleashed.scp087.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class Scp087SectionRefresh {
    public static final int Frames = 2;
    public static final long WaitMillis = 50L;
    private static final Logger Log = LogUtils.getLogger();
    private static int Pending;
    private static boolean Resolved;
    private static boolean Broken;
    private static Field GraphField;
    private static Field AreaField;
    private static Field TaskField;
    private static Method ScheduleMethod;

    private Scp087SectionRefresh() {
    }

    public static void request() {
        Pending = Frames;
    }

    private static boolean resolve() {
        if (Resolved) {
            return !Broken;
        }
        Resolved = true;
        try {
            Field Graph = LevelRenderer.class.getDeclaredField("sectionOcclusionGraph");
            Field Area = LevelRenderer.class.getDeclaredField("viewArea");
            Field Task = SectionOcclusionGraph.class.getDeclaredField("fullUpdateTask");
            Method Schedule = SectionOcclusionGraph.class.getDeclaredMethod("scheduleFullUpdate", boolean.class, Camera.class, Vec3.class);
            Graph.setAccessible(true);
            Area.setAccessible(true);
            Task.setAccessible(true);
            Schedule.setAccessible(true);
            GraphField = Graph;
            AreaField = Area;
            TaskField = Task;
            ScheduleMethod = Schedule;
        } catch (ReflectiveOperationException | RuntimeException Problem) {
            Broken = true;
            Log.warn("SCP-087 could not reach the section occlusion graph; loop teleports may show one stale frame", Problem);
        }
        return !Broken;
    }

    private static void settle(SectionOcclusionGraph Graph) throws Exception {
        if (TaskField.get(Graph) instanceof Future<?> Running && !Running.isDone()) {
            Running.get(WaitMillis, TimeUnit.MILLISECONDS);
        }
    }

    public static void run(Camera View) {
        if (Pending <= 0) {
            return;
        }
        Pending--;
        Minecraft Client = Minecraft.getInstance();
        if (Client.level == null || Client.levelRenderer == null || !resolve()) {
            return;
        }
        try {
            SectionOcclusionGraph Graph = (SectionOcclusionGraph) GraphField.get(Client.levelRenderer);
            settle(Graph);
            if (AreaField.get(Client.levelRenderer) instanceof ViewArea Area) {
                Area.repositionCamera(View.getPosition().x, View.getPosition().z);
            }
            ScheduleMethod.invoke(Graph, Client.smartCull, View, View.getPosition());
            settle(Graph);
        } catch (Exception Problem) {
            Broken = true;
            Log.warn("SCP-087 section refresh failed; loop teleports may show one stale frame", Problem);
        }
    }

    public static void reset() {
        Pending = 0;
    }
}
