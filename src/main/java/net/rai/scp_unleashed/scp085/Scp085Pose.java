package net.rai.scp_unleashed.scp085;

public enum Scp085Pose {
    SitSide(true),
    SitFront(true),
    SitGround(true),
    Lie(false),
    Crouch(true),
    LookOut(true),
    Twirl(true),
    Write(true);

    private final boolean Wakeful;

    Scp085Pose(boolean Wakeful) {
        this.Wakeful = Wakeful;
    }

    public boolean wakeful() {
        return Wakeful;
    }
}
