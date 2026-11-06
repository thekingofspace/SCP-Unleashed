package net.rai.scp_unleashed.client.geo;

import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

public final class SharedBones {
    public static final int Remembered = 512;
    public static final int Fields = 9;
    private final Map<Long, Held> Poses = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Held> Eldest) {
            return size() > Remembered;
        }
    };

    private record Held(GeoBone[] Bones, float[] Values, boolean[] Hidden, boolean[] Children) {
    }

    public static boolean hiddenByDefault(GeoBone Found) {
        return Boolean.TRUE.equals(Found.shouldNeverRender());
    }

    public static void rest(Iterable<GeoBone> Bones) {
        for (GeoBone Found : Bones) {
            BoneSnapshot Initial = Found.getInitialSnapshot();
            if (Initial != null) {
                Found.setRotX(Initial.getRotX());
                Found.setRotY(Initial.getRotY());
                Found.setRotZ(Initial.getRotZ());
                Found.setPosX(Initial.getOffsetX());
                Found.setPosY(Initial.getOffsetY());
                Found.setPosZ(Initial.getOffsetZ());
                Found.setScaleX(Initial.getScaleX());
                Found.setScaleY(Initial.getScaleY());
                Found.setScaleZ(Initial.getScaleZ());
            }
            Found.setHidden(hiddenByDefault(Found));
            Found.setChildrenHidden(false);
            Found.resetStateChanges();
        }
    }

    public static void release(Iterable<GeoBone> Bones) {
        rest(Bones);
    }

    public void animate(long InstanceId, Collection<GeoBone> Bones, BooleanSupplier Process, Runnable Pose) {
        rest(Bones);
        boolean Processed = Process.getAsBoolean();
        if (Processed) {
            remember(InstanceId, Bones);
            return;
        }
        if (!recall(InstanceId, Bones)) {
            rest(Bones);
            Pose.run();
            remember(InstanceId, Bones);
        }
    }

    public void remember(long InstanceId, Collection<GeoBone> Bones) {
        GeoBone[] Order = Bones.toArray(new GeoBone[0]);
        float[] Values = new float[Order.length * Fields];
        boolean[] Hidden = new boolean[Order.length];
        boolean[] Children = new boolean[Order.length];
        for (int Index = 0; Index < Order.length; Index++) {
            GeoBone Found = Order[Index];
            int At = Index * Fields;
            Values[At] = Found.getRotX();
            Values[At + 1] = Found.getRotY();
            Values[At + 2] = Found.getRotZ();
            Values[At + 3] = Found.getPosX();
            Values[At + 4] = Found.getPosY();
            Values[At + 5] = Found.getPosZ();
            Values[At + 6] = Found.getScaleX();
            Values[At + 7] = Found.getScaleY();
            Values[At + 8] = Found.getScaleZ();
            Hidden[Index] = Found.isHidden();
            Children[Index] = Found.isHidingChildren();
        }
        Poses.put(InstanceId, new Held(Order, Values, Hidden, Children));
    }

    public boolean recall(long InstanceId, Collection<GeoBone> Bones) {
        Held Found = Poses.get(InstanceId);
        if (Found == null || Found.Bones().length != Bones.size()) {
            return false;
        }
        int Index = 0;
        for (GeoBone Current : Bones) {
            if (Found.Bones()[Index++] != Current) {
                return false;
            }
        }
        for (Index = 0; Index < Found.Bones().length; Index++) {
            GeoBone Current = Found.Bones()[Index];
            int At = Index * Fields;
            float[] Values = Found.Values();
            Current.setRotX(Values[At]);
            Current.setRotY(Values[At + 1]);
            Current.setRotZ(Values[At + 2]);
            Current.setPosX(Values[At + 3]);
            Current.setPosY(Values[At + 4]);
            Current.setPosZ(Values[At + 5]);
            Current.setScaleX(Values[At + 6]);
            Current.setScaleY(Values[At + 7]);
            Current.setScaleZ(Values[At + 8]);
            Current.setHidden(Found.Hidden()[Index]);
            Current.setChildrenHidden(Found.Children()[Index]);
        }
        return true;
    }

    public void forget() {
        Poses.clear();
    }
}
