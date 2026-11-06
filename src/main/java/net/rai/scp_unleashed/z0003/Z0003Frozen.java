package net.rai.scp_unleashed.z0003;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;

public record Z0003Frozen(int Age, float BodyYaw, float BodyYawO, float HeadYaw, float HeadYawO, float Pitch, float PitchO, float Yaw, float YawO,
                          float WalkSpeedO, float WalkSpeed, float WalkPosition, float Attack, float AttackO, int HurtTime) {
    public static final float Tolerance = 1.0E-3f;

    public static Z0003Frozen capture(LivingEntity Living) {
        WalkAnimationState Walk = Living.walkAnimation;
        return new Z0003Frozen(Living.tickCount, Living.yBodyRot, Living.yBodyRotO, Living.yHeadRot, Living.yHeadRotO, Living.getXRot(), Living.xRotO,
                Living.getYRot(), Living.yRotO, Walk.speed(0.0f), Walk.speed(), Walk.position(), Living.attackAnim, Living.oAttackAnim, Living.hurtTime);
    }

    public static Z0003Frozen held(LivingEntity Living, Z0003Statue Statue, int Age) {
        return new Z0003Frozen(Age, Statue.BodyYaw(), Statue.BodyYaw(), Statue.HeadYaw(), Statue.HeadYaw(), Statue.Pitch(), Statue.Pitch(), Statue.HeadYaw(),
                Statue.HeadYaw(), Statue.WalkSpeed(), Statue.WalkSpeed(), Statue.WalkPosition(), Statue.Swing(), Statue.Swing(), 0);
    }

    public Z0003Frozen still() {
        return new Z0003Frozen(Age, BodyYaw, BodyYaw, HeadYaw, HeadYaw, Pitch, Pitch, Yaw, Yaw, WalkSpeed, WalkSpeed, WalkPosition, Attack, Attack, 0);
    }

    public void apply(LivingEntity Living) {
        Living.tickCount = Age;
        Living.yBodyRot = BodyYaw;
        Living.yBodyRotO = BodyYawO;
        Living.yHeadRot = HeadYaw;
        Living.yHeadRotO = HeadYawO;
        Living.setXRot(Pitch);
        Living.xRotO = PitchO;
        Living.setYRot(Yaw);
        Living.yRotO = YawO;
        Living.attackAnim = Attack;
        Living.oAttackAnim = AttackO;
        Living.hurtTime = HurtTime;
        walk(Living.walkAnimation, WalkSpeedO, WalkSpeed, WalkPosition);
    }

    public static void walk(WalkAnimationState Walk, float SpeedO, float Speed, float Position) {
        Walk.update(Position - Walk.position() - SpeedO - Speed, 1.0f);
        Walk.update(SpeedO, 1.0f);
        Walk.update(Speed, 1.0f);
    }

    public boolean matches(Z0003Frozen Other) {
        return Age == Other.Age && near(BodyYaw, Other.BodyYaw) && near(BodyYawO, Other.BodyYawO) && near(HeadYaw, Other.HeadYaw) && near(HeadYawO, Other.HeadYawO)
                && near(Pitch, Other.Pitch) && near(PitchO, Other.PitchO) && near(Yaw, Other.Yaw) && near(YawO, Other.YawO) && near(WalkSpeedO, Other.WalkSpeedO)
                && near(WalkSpeed, Other.WalkSpeed) && near(WalkPosition, Other.WalkPosition) && near(Attack, Other.Attack) && near(AttackO, Other.AttackO)
                && HurtTime == Other.HurtTime;
    }

    public boolean samePose(Z0003Frozen Other) {
        return near(BodyYaw, Other.BodyYaw) && near(HeadYaw, Other.HeadYaw) && near(Pitch, Other.Pitch) && near(WalkSpeed, Other.WalkSpeed)
                && near(WalkPosition, Other.WalkPosition) && near(Attack, Other.Attack);
    }

    private static boolean near(float A, float B) {
        return Math.abs(A - B) <= Tolerance * Math.max(1.0f, Math.abs(A));
    }
}
