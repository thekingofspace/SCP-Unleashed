package net.rai.scp_unleashed.scp024;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.projectile.Projectile;
import net.rai.scp_unleashed.scp024.plan.Scp024Plan;

import java.util.ArrayList;
import java.util.List;

public interface Scp024Game {
    String id();

    Scp024Plan plan(long Seed);

    int seconds();

    void start(Scp024Session Session);

    Scp024Outcome tick(Scp024Session Session);

    default boolean survival() {
        return false;
    }

    default double killDepth() {
        return -4.0;
    }

    default double floorY() {
        return 1.0;
    }

    default boolean lit() {
        return true;
    }

    default int rulePages() {
        return 2;
    }

    default boolean use(Scp024Session Session, BlockPos Local) {
        return false;
    }

    default void hit(Scp024Session Session, Projectile Shot, BlockPos Local) {
    }

    default void cleanup(Scp024Session Session) {
    }

    default Component displayName() {
        return Component.translatable("game.scp_unleashed.scp024." + id());
    }

    default List<Component> rules() {
        List<Component> Pages = new ArrayList<>();
        for (int Page = 0; Page < rulePages(); Page++) {
            Pages.add(Component.translatable("game.scp_unleashed.scp024." + id() + ".rules." + Page));
        }
        return Pages;
    }
}
