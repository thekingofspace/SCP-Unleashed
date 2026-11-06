package net.rai.scp_unleashed.scp024.games;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp024.Scp024Cue;
import net.rai.scp_unleashed.scp024.Scp024Game;
import net.rai.scp_unleashed.scp024.Scp024Hazards;
import net.rai.scp_unleashed.scp024.Scp024Outcome;
import net.rai.scp_unleashed.scp024.Scp024Quips;
import net.rai.scp_unleashed.scp024.Scp024Registry;
import net.rai.scp_unleashed.scp024.Scp024Session;
import net.rai.scp_unleashed.scp024.plan.Scp024Mat;
import net.rai.scp_unleashed.scp024.plan.Scp024Plan;
import net.rai.scp_unleashed.scp024.plan.Scp024Rooms;

import java.util.ArrayList;
import java.util.List;

public class HotPotatoGame implements Scp024Game {
    public static final int Seconds = 120;
    public static final int Passes = 6;
    public static final int ShortFuse = 90;
    public static final int LongFuse = 140;
    public static final int AwayMin = 30;
    public static final int AwayMax = 60;
    public static final double MinHop = 7.0;
    public static final double Radius = 2.5;
    public static final float Damage = 14.0f;
    public static final int Close = 20;
    private int Fuse;
    private int Back = -1;
    private int Active = -1;
    private int Passed;

    @Override
    public String id() {
        return "hot_potato";
    }

    @Override
    public Scp024Plan plan(long Seed) {
        return Scp024Rooms.hotPotato(Seed);
    }

    @Override
    public int seconds() {
        return Seconds;
    }

    @Override
    public void start(Scp024Session Session) {
        handOver(Session);
        light(Session, Session.Random.nextInt(Session.marks("pedestal").size()));
    }

    private ItemStack potato() {
        return Scp024Hazards.named(new ItemStack(Items.CREEPER_HEAD), "item.scp_unleashed.scp024_hot_potato");
    }

    private void handOver(Scp024Session Session) {
        Session.give(potato());
        Fuse = ShortFuse + Session.Random.nextInt(LongFuse - ShortFuse + 1);
        Back = -1;
    }

    private void light(Scp024Session Session, int Index) {
        List<int[]> Pedestals = Session.marks("pedestal");
        if (Active >= 0) {
            int[] Old = Pedestals.get(Active);
            Session.restore(Old[0], Old[1], Old[2]);
        }
        Active = Index;
        int[] Top = Pedestals.get(Index);
        Session.set(Top[0], Top[1], Top[2], Scp024Mat.SeaLantern);
    }

    private void moveLight(Scp024Session Session) {
        List<int[]> Pedestals = Session.marks("pedestal");
        Vec3 Here = Session.centre(Pedestals.get(Active));
        List<Integer> Far = new ArrayList<>();
        for (int Index = 0; Index < Pedestals.size(); Index++) {
            if (Index != Active && Session.centre(Pedestals.get(Index)).distanceTo(Here) >= MinHop) {
                Far.add(Index);
            }
        }
        light(Session, Far.isEmpty() ? (Active + 1) % Pedestals.size() : Far.get(Session.Random.nextInt(Far.size())));
    }

    private static boolean holding(ServerPlayer User) {
        Inventory Pockets = User.getInventory();
        for (int Slot = 0; Slot < Pockets.getContainerSize(); Slot++) {
            ItemStack Stack = Pockets.getItem(Slot);
            if (Stack.is(Items.CREEPER_HEAD) && Scp024Session.isProp(Stack)) {
                return true;
            }
        }
        return false;
    }

    private static void takePotato(ServerPlayer User) {
        Inventory Pockets = User.getInventory();
        for (int Slot = 0; Slot < Pockets.getContainerSize(); Slot++) {
            ItemStack Stack = Pockets.getItem(Slot);
            if (Stack.is(Items.CREEPER_HEAD) && Scp024Session.isProp(Stack)) {
                Pockets.setItem(Slot, ItemStack.EMPTY);
            }
        }
        User.inventoryMenu.broadcastChanges();
    }

    @Override
    public boolean use(Scp024Session Session, BlockPos Local) {
        ServerPlayer User = Session.player();
        if (User == null || Active < 0 || Back >= 0) {
            return false;
        }
        int[] Top = Session.marks("pedestal").get(Active);
        if (Local.getX() != Top[0] || Local.getY() != Top[1] || Local.getZ() != Top[2] || !holding(User)) {
            return false;
        }
        takePotato(User);
        Passed++;
        if (Fuse < Close) {
            Session.taunt("lucky");
        } else {
            Session.cue(Scp024Cue.Ding);
        }
        Back = Session.ticks() + AwayMin + Session.Random.nextInt(AwayMax - AwayMin + 1);
        moveLight(Session);
        return true;
    }

    @Override
    public Scp024Outcome tick(Scp024Session Session) {
        ServerPlayer User = Session.player();
        if (User == null) {
            return Scp024Outcome.Continue;
        }
        Session.status(Component.translatable("status.scp_unleashed.scp024.potato", Passed, Passes));
        if (Passed >= Passes) {
            takePotato(User);
            return Scp024Outcome.Win;
        }
        if (Back >= 0) {
            if (Session.ticks() >= Back) {
                handOver(Session);
                Scp024Quips.say(User, "catch");
                Session.sound(User.position(), Scp024Registry.ConfettiPop.get(), 0.8f, 0.7f);
            }
            return Scp024Outcome.Continue;
        }
        if (!holding(User)) {
            Session.give(potato());
        }
        Fuse--;
        if (Session.ticks() % Math.max(3, Fuse / 10) == 0) {
            Session.sound(User.position(), Scp024Registry.Fuse.get(), 0.6f, 1.0f + (LongFuse - Fuse) / (float) LongFuse);
            Session.World.sendParticles(ParticleTypes.SMOKE, User.getX(), User.getY() + 1.2, User.getZ(), 3, 0.2, 0.2, 0.2, 0.01);
        }
        if (Active >= 0 && Session.ticks() % 6 == 0) {
            Vec3 Glow = Session.centre(Session.marks("pedestal").get(Active)).add(0.0, 1.1, 0.0);
            Session.World.sendParticles(ParticleTypes.END_ROD, Glow.x, Glow.y, Glow.z, 2, 0.2, 0.2, 0.2, 0.01);
        }
        if (Fuse <= 0) {
            takePotato(User);
            Session.blast(User.position().add(0.0, 0.8, 0.0), Radius, Damage);
            Session.taunt("blast");
            handOver(Session);
            moveLight(Session);
        }
        return Scp024Outcome.Continue;
    }
}
