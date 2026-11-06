package net.rai.scp_unleashed.guns;

public final class GunStats {
    private float Damage = 4.0F;
    private int Pellets = 1;
    private float PelletSpread;
    private float HipSpread = 2.0F;
    private float AimSpread = 0.4F;
    private float BloomPerShot = 0.5F;
    private float BloomLimit = 3.0F;
    private float BloomRecovery = 0.35F;
    private float MovePenalty = 1.5F;
    private float AirPenalty = 3.0F;
    private int Interval = 4;
    private boolean Automatic;
    private int Capacity;
    private int ReloadTicks = 30;
    private float Range = 64.0F;
    private float FalloffStart = 24.0F;
    private float FalloffEnd = 56.0F;
    private float FalloffFloor = 0.6F;
    private float Headshot = 1.5F;
    private float Knockback = 0.15F;
    private float RecoilPitch = 2.5F;
    private float RecoilYaw = 0.8F;
    private float AimZoom = 0.85F;
    private int AimTicks = 4;
    private boolean Scoped;
    private boolean LongGun;
    private int Charges;
    private int ChargeTicks;
    private boolean Piercing;
    private boolean FirearmFlash = true;
    private boolean Heavy;
    private float CrouchSpread = GunFiring.CrouchSteadiness;
    private float CrouchBloom = 1.0F;
    private float CrouchRecoil = 1.0F;
    private float HeatPerShot;
    private float HeatLimit;
    private float HeatCooling;
    private int VentTicks;
    private float Encumbrance;

    public GunStats damage(float Value) {
        Damage = Value;
        return this;
    }

    public GunStats pellets(int Count, float Spread) {
        Pellets = Count;
        PelletSpread = Spread;
        return this;
    }

    public GunStats spread(float Hip, float Aim) {
        HipSpread = Hip;
        AimSpread = Aim;
        return this;
    }

    public GunStats bloom(float PerShot, float Limit, float Recovery) {
        BloomPerShot = PerShot;
        BloomLimit = Limit;
        BloomRecovery = Recovery;
        return this;
    }

    public GunStats movement(float Moving, float Airborne) {
        MovePenalty = Moving;
        AirPenalty = Airborne;
        return this;
    }

    public GunStats interval(int Ticks) {
        Interval = Ticks;
        return this;
    }

    public GunStats automatic() {
        Automatic = true;
        return this;
    }

    public GunStats magazine(int Rounds, int Ticks) {
        Capacity = Rounds;
        ReloadTicks = Ticks;
        return this;
    }

    public GunStats range(float Reach, float Start, float End, float Floor) {
        Range = Reach;
        FalloffStart = Start;
        FalloffEnd = End;
        FalloffFloor = Floor;
        return this;
    }

    public GunStats headshot(float Multiplier) {
        Headshot = Multiplier;
        return this;
    }

    public GunStats knockback(float Strength) {
        Knockback = Strength;
        return this;
    }

    public GunStats recoil(float Pitch, float Yaw) {
        RecoilPitch = Pitch;
        RecoilYaw = Yaw;
        return this;
    }

    public GunStats aim(float Zoom, int Ticks) {
        AimZoom = Zoom;
        AimTicks = Ticks;
        return this;
    }

    public GunStats scoped() {
        Scoped = true;
        return this;
    }

    public GunStats longGun() {
        LongGun = true;
        return this;
    }

    public GunStats charges(int Count, int Ticks) {
        Charges = Count;
        ChargeTicks = Ticks;
        return this;
    }

    public GunStats piercing() {
        Piercing = true;
        return this;
    }

    public boolean isFirearm() {
        return FirearmFlash;
    }

    public GunStats quiet() {
        FirearmFlash = false;
        return this;
    }

    public float getDamage() {
        return Damage;
    }

    public int getPellets() {
        return Pellets;
    }

    public float getPelletSpread() {
        return PelletSpread;
    }

    public float getHipSpread() {
        return HipSpread;
    }

    public float getAimSpread() {
        return AimSpread;
    }

    public float getBloomPerShot() {
        return BloomPerShot;
    }

    public float getBloomLimit() {
        return BloomLimit;
    }

    public float getBloomRecovery() {
        return BloomRecovery;
    }

    public float getMovePenalty() {
        return MovePenalty;
    }

    public float getAirPenalty() {
        return AirPenalty;
    }

    public int getInterval() {
        return Interval;
    }

    public boolean isAutomatic() {
        return Automatic;
    }

    public int getCapacity() {
        return Capacity;
    }

    public int getReloadTicks() {
        return ReloadTicks;
    }

    public float getRange() {
        return Range;
    }

    public float getFalloffStart() {
        return FalloffStart;
    }

    public float getFalloffEnd() {
        return FalloffEnd;
    }

    public float getFalloffFloor() {
        return FalloffFloor;
    }

    public float getHeadshot() {
        return Headshot;
    }

    public float getKnockback() {
        return Knockback;
    }

    public float getRecoilPitch() {
        return RecoilPitch;
    }

    public float getRecoilYaw() {
        return RecoilYaw;
    }

    public float getAimZoom() {
        return AimZoom;
    }

    public int getAimTicks() {
        return AimTicks;
    }

    public boolean isScoped() {
        return Scoped;
    }

    public boolean isLongGun() {
        return LongGun;
    }

    public int getCharges() {
        return Charges;
    }

    public int getChargeTicks() {
        return ChargeTicks;
    }

    public boolean isPiercing() {
        return Piercing;
    }

    public GunStats heavy() {
        Heavy = true;
        return this;
    }

    public boolean isHeavy() {
        return Heavy;
    }

    public boolean usesCharges() {
        return Charges > 0;
    }

    public GunStats braced(float Spread, float Bloom, float Recoil) {
        CrouchSpread = Spread;
        CrouchBloom = Bloom;
        CrouchRecoil = Recoil;
        return this;
    }

    public float getCrouchSpread() {
        return CrouchSpread;
    }

    public float getCrouchBloom() {
        return CrouchBloom;
    }

    public float getCrouchRecoil() {
        return CrouchRecoil;
    }

    public GunStats heat(float PerShot, float Limit, float Cooling, int Vent) {
        HeatPerShot = PerShot;
        HeatLimit = Limit;
        HeatCooling = Cooling;
        VentTicks = Vent;
        return this;
    }

    public boolean usesHeat() {
        return HeatLimit > 0.0F && HeatPerShot > 0.0F;
    }

    public float getHeatPerShot() {
        return HeatPerShot;
    }

    public float getHeatLimit() {
        return HeatLimit;
    }

    public float getHeatCooling() {
        return HeatCooling;
    }

    public int getVentTicks() {
        return VentTicks;
    }

    public int shotsToOverheat() {
        float Net = HeatPerShot - HeatCooling * Interval;
        if (!usesHeat() || Net <= 0.0F) {
            return 0;
        }
        return 1 + (int) Math.ceil((HeatLimit - HeatPerShot) / Net);
    }

    public GunStats encumber(float Slow) {
        Encumbrance = Slow;
        return this;
    }

    public float getEncumbrance() {
        return Encumbrance;
    }

    public float falloff(double Distance) {
        if (Distance <= FalloffStart || FalloffEnd <= FalloffStart) {
            return 1.0F;
        }
        if (Distance >= FalloffEnd) {
            return FalloffFloor;
        }
        float Progress = (float) ((Distance - FalloffStart) / (FalloffEnd - FalloffStart));
        return 1.0F + (FalloffFloor - 1.0F) * Progress;
    }
}
