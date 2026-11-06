package net.rai.scp_unleashed.scp024.games;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.rai.scp_unleashed.scp024.Scp024Cue;
import net.rai.scp_unleashed.scp024.Scp024Game;
import net.rai.scp_unleashed.scp024.Scp024Hazards;
import net.rai.scp_unleashed.scp024.Scp024Outcome;
import net.rai.scp_unleashed.scp024.Scp024Registry;
import net.rai.scp_unleashed.scp024.Scp024Session;
import net.rai.scp_unleashed.scp024.plan.Scp024Mat;
import net.rai.scp_unleashed.scp024.plan.Scp024Plan;
import net.rai.scp_unleashed.scp024.plan.Scp024Rooms;

import java.util.ArrayList;
import java.util.List;

public class TargetPracticeGame implements Scp024Game {
    public static final int Seconds = 60;
    public static final int Needed = 10;
    public static final int Showing = 2;
    public static final int Lifetime = 80;
    public static final int Quiver = 24;
    public static final int Refill = 12;
    public static final int LowArrows = 4;
    private final List<Integer> Active = new ArrayList<>();
    private final List<Integer> Expires = new ArrayList<>();
    private int Hits;

    @Override
    public String id() {
        return "target_practice";
    }

    @Override
    public Scp024Plan plan(long Seed) {
        return Scp024Rooms.targetPractice(Seed);
    }

    @Override
    public int seconds() {
        return Seconds;
    }

    @Override
    public double killDepth() {
        return -3.0;
    }

    @Override
    public void start(Scp024Session Session) {
        Session.give(Scp024Hazards.bow());
        Session.give(new ItemStack(Items.ARROW, Quiver));
        for (int Index = 0; Index < Showing; Index++) {
            raise(Session);
        }
    }

    private void raise(Scp024Session Session) {
        List<int[]> Spots = Session.marks("target");
        for (int Try = 0; Try < 12; Try++) {
            int Pick = Session.Random.nextInt(Spots.size());
            if (Active.contains(Pick)) {
                continue;
            }
            int[] Spot = Spots.get(Pick);
            Session.set(Spot[0], Spot[1], Spot[2], Scp024Mat.Target);
            Active.add(Pick);
            Expires.add(Session.ticks() + Lifetime);
            Session.sound(Session.centre(Spot), Scp024Registry.Click.get(), 0.8f, 1.5f);
            return;
        }
    }

    private void lower(Scp024Session Session, int Slot) {
        int[] Spot = Session.marks("target").get(Active.get(Slot));
        Session.restore(Spot[0], Spot[1], Spot[2]);
        Active.remove(Slot);
        Expires.remove(Slot);
    }

    @Override
    public void hit(Scp024Session Session, Projectile Shot, BlockPos Local) {
        List<int[]> Spots = Session.marks("target");
        for (int Slot = 0; Slot < Active.size(); Slot++) {
            int[] Spot = Spots.get(Active.get(Slot));
            if (Spot[0] == Local.getX() && Spot[1] == Local.getY() && Spot[2] == Local.getZ()) {
                lower(Session, Slot);
                Hits++;
                Session.cue(Scp024Cue.Ding);
                Shot.discard();
                raise(Session);
                return;
            }
        }
    }

    private static int arrows(ServerPlayer User) {
        Inventory Pockets = User.getInventory();
        int Count = 0;
        for (int Slot = 0; Slot < Pockets.getContainerSize(); Slot++) {
            ItemStack Stack = Pockets.getItem(Slot);
            if (Stack.is(Items.ARROW) && Scp024Session.isProp(Stack)) {
                Count += Stack.getCount();
            }
        }
        return Count;
    }

    @Override
    public Scp024Outcome tick(Scp024Session Session) {
        ServerPlayer User = Session.player();
        if (Hits >= Needed) {
            return Scp024Outcome.Win;
        }
        for (int Slot = Active.size() - 1; Slot >= 0; Slot--) {
            if (Session.ticks() >= Expires.get(Slot)) {
                lower(Session, Slot);
                raise(Session);
            }
        }
        while (Active.size() < Showing) {
            int Before = Active.size();
            raise(Session);
            if (Active.size() == Before) {
                break;
            }
        }
        if (User != null && Session.ticks() % 40 == 0 && arrows(User) < LowArrows) {
            Session.give(new ItemStack(Items.ARROW, Refill));
        }
        Session.status(Component.translatable("status.scp_unleashed.scp024.targets", Hits, Needed));
        return Scp024Outcome.Continue;
    }
}
