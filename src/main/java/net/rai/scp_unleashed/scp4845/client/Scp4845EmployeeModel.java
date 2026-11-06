package net.rai.scp_unleashed.scp4845.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.dclass.client.DClassGeoModel;
import net.rai.scp_unleashed.scp4845.Scp4845Registry;
import net.rai.scp_unleashed.scp4845.staff.Scp4845Employee;
import net.rai.scp_unleashed.scp4845.staff.Scp4845Task;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.data.EntityModelData;

import java.util.EnumSet;
import java.util.Set;

public class Scp4845EmployeeModel extends DClassGeoModel<Scp4845Employee> {
    public static final ResourceLocation EmployeeModel = Scp4845Registry.location("geo/entity/scp4845/scp4845_employee.geo.json");
    public static final ResourceLocation EmployeeAnimations = Scp4845Registry.location("animations/entity/scp4845/scp4845_employee.animation.json");
    public static final ResourceLocation ManagerTexture = Scp4845Registry.location("textures/entity/scp4845/manager.png");
    public static final boolean[] Caps = {true, false, false, true};
    public static final boolean[] Headsets = {false, true, false, true};
    public static final float SquintLid = 0.4f;
    public static final float SquintStart = 0.6f;
    public static final float SquintEnd = 1.9f;
    public static final float BreakLength = 7.0f;
    public static final float[][] Chewing = {{1.4f, 3.0f}, {3.4f, 4.9f}};
    public static final Set<Scp4845Task> SteadyGaze = EnumSet.of(Scp4845Task.Register, Scp4845Task.HandOver, Scp4845Task.Grill, Scp4845Task.Fry, Scp4845Task.Prep,
            Scp4845Task.Mop, Scp4845Task.Wipe, Scp4845Task.Restock, Scp4845Task.Wash, Scp4845Task.Break, Scp4845Task.Type, Scp4845Task.Sigh, Scp4845Task.Stretch,
            Scp4845Task.Glance);
    private static final ResourceLocation[] Textures = textures();

    private static ResourceLocation[] textures() {
        ResourceLocation[] Made = new ResourceLocation[Scp4845Employee.Looks];
        for (int Index = 0; Index < Made.length; Index++) {
            Made[Index] = Scp4845Registry.location("textures/entity/scp4845/employee_" + Index + ".png");
        }
        return Made;
    }

    @Override
    public ResourceLocation getModelResource(Scp4845Employee Owner) {
        return EmployeeModel;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp4845Employee Owner) {
        return EmployeeAnimations;
    }

    @Override
    public ResourceLocation getTextureResource(Scp4845Employee Owner) {
        return Owner.isManager() ? ManagerTexture : Textures[Math.floorMod(Owner.look(), Textures.length)];
    }

    @Nullable
    private GeoBone part(String Name) {
        return getAnimationProcessor().getBone(Name);
    }

    private void show(String Name, boolean Shown) {
        GeoBone Found = part(Name);
        if (Found != null) {
            Found.setHidden(!Shown);
        }
    }

    @Override
    public void setCustomAnimations(Scp4845Employee Owner, long InstanceId, AnimationState<Scp4845Employee> Animated) {
        super.setCustomAnimations(Owner, InstanceId, Animated);
        Scp4845Task Task = Owner.task();
        for (String Prop : Scp4845Task.AllProps) {
            show(Prop, Task.Props.contains(Prop));
        }
        int Look = Math.floorMod(Owner.look(), Caps.length);
        boolean Manager = Owner.isManager();
        show("hat_cap", !Manager && Caps[Look]);
        show("hat_visor", !Manager && !Caps[Look]);
        show("headset", !Manager && Headsets[Look]);
        if (!Owner.isAlive()) {
            return;
        }
        float Elapsed = ((Owner.level() == null ? 0L : Owner.level().getGameTime()) - Owner.taskStart() + Animated.getPartialTick()) / 20.0f;
        if (SteadyGaze.contains(Task)) {
            steady(Animated);
        }
        if (Task == Scp4845Task.Glance && Elapsed >= SquintStart && Elapsed <= SquintEnd) {
            squint("right_lid");
            squint("left_lid");
        }
        if (Task == Scp4845Task.Break) {
            float Loop = Elapsed % BreakLength;
            boolean Chew = false;
            for (float[] Window : Chewing) {
                Chew |= Loop >= Window[0] && Loop <= Window[1];
            }
            GeoBone Mouth = part("mouth");
            if (Mouth != null) {
                Mouth.setHidden(!(Chew && (Owner.tickCount / 3) % 2 == 0));
            }
        }
    }

    private void steady(AnimationState<Scp4845Employee> Animated) {
        GeoBone Head = part("head");
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        if (Head == null || Data == null) {
            return;
        }
        float HeadX = Mth.clamp(Data.headPitch(), -HeadPitchLimit, HeadPitchLimit) * Mth.DEG_TO_RAD;
        float HeadY = Mth.clamp(Mth.wrapDegrees(Data.netHeadYaw()), -HeadYawLimit, HeadYawLimit) * Mth.DEG_TO_RAD;
        Head.setRotX(Head.getRotX() - HeadX);
        Head.setRotY(Head.getRotY() - HeadY);
    }

    private void squint(String Name) {
        GeoBone Found = part(Name);
        if (Found != null && (Found.isHidden() || Found.getScaleY() < SquintLid)) {
            Found.setHidden(false);
            Found.setScaleY(SquintLid);
        }
    }
}
