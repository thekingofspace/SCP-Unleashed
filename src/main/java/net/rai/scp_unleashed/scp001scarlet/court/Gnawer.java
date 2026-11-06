package net.rai.scp_unleashed.scp001scarlet.court;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class Gnawer extends CourtMinion {
    public Gnawer(EntityType<? extends Monster> Type, Level World) {
        super(Type, World, CourtKind.Gnawer);
    }

    @Override
    protected void addBehaviours() {
        behaviour(2, new Scurry(this));
    }

    @Override
    protected int hitTick() {
        return 5;
    }

    @Override
    protected double reach() {
        return 1.1;
    }

    @Override
    protected String clip(int Action) {
        return Action == Attack ? "attack" : null;
    }

    public static class Scurry extends CourtGoals.Melee {
        public Scurry(CourtMinion Owner) {
            super(Owner, 1.2, 14, 10);
        }
    }
}
