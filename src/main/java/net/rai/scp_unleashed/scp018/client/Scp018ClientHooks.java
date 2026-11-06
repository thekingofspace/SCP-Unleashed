package net.rai.scp_unleashed.scp018.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.rai.scp_unleashed.scp018.Scp018Ball;
import net.rai.scp_unleashed.scp018.network.BallPathPayload;

public final class Scp018ClientHooks {
    private Scp018ClientHooks() {
    }

    public static void path(BallPathPayload Payload) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.level == null) {
            return;
        }
        Entity Found = Client.level.getEntity(Payload.EntityId());
        if (Found instanceof Scp018Ball Ball) {
            Ball.acceptPath(Payload.Segments());
        }
    }
}
