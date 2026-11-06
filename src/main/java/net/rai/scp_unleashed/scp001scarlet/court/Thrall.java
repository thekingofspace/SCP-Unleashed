package net.rai.scp_unleashed.scp001scarlet.court;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class Thrall extends CourtMinion {
    public Thrall(EntityType<? extends Monster> Type, Level World) {
        super(Type, World, CourtKind.Thrall);
    }

    @Override
    protected void addBehaviours() {
        behaviour(2, new Lurch(this));
    }

    @Override
    protected int hitTick() {
        return 9;
    }

    @Override
    protected void actionTick(int Action, int Elapsed) {
        if (Action == Attack && Elapsed == 2) {
            play("attack", 0.9f, 1.0f);
        }
        super.actionTick(Action, Elapsed);
    }

    @Override
    protected String clip(int Action) {
        return Action == Attack ? "attack" : null;
    }

    public static class Lurch extends CourtGoals.Melee {
        public Lurch(CourtMinion Owner) {
            super(Owner, 1.0, 26, 18);
        }
    }
}
