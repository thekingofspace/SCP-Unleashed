package net.rai.scp_unleashed.scp5001.core;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public interface Scp5001Duel {
    int Lunge = 0;
    int Bite = 1;
    int Sweep = 2;
    int Roar = 3;
    int Coil = 4;

    Vec3 duelHead();

    void duelEngage(Entity Opponent, Vec3 Arena);

    void duelAttack(int Move, Vec3 Target);

    void duelReact(Vec3 From, float Power);

    void duelBind(Vec3 Portal, float Progress);

    void duelBanish();
}
