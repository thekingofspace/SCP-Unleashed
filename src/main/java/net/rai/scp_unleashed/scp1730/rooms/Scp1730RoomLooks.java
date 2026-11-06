package net.rai.scp_unleashed.scp1730.rooms;

public final class Scp1730RoomLooks {
    public static final float ServerNear = 3.0f;
    public static final float ServerFar = 36.0f;
    public static final float HallFar = 34.0f;
    public static final float DarkFar = 2.6f;

    private Scp1730RoomLooks() {
    }

    public static float lerp(float Share, float From, float To) {
        return From + (To - From) * Share;
    }

    public static float[] fog(float Near, float Far, float Server, float Hall, float Dark) {
        if (Server > 0.0f) {
            Far = lerp(Server, Far, Math.min(Far, ServerFar));
            Near = lerp(Server, Near, Math.min(Near, ServerNear));
        }
        if (Hall > 0.0f) {
            Far = lerp(Hall, Far, Math.min(Far, HallFar));
            Near = lerp(Hall, Near, 0.0f);
        }
        if (Dark > 0.0f) {
            float Ease = Dark * Dark * (3.0f - 2.0f * Dark);
            Far = lerp(Ease, Far, Math.min(Far, DarkFar));
            Near = lerp(Ease, Near, 0.0f);
        }
        return new float[]{Math.min(Near, Far * 0.9f), Far};
    }
}
