package net.rai.scp_unleashed.scp024.games;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp024.Scp024Cue;
import net.rai.scp_unleashed.scp024.Scp024Game;
import net.rai.scp_unleashed.scp024.Scp024Hazards;
import net.rai.scp_unleashed.scp024.Scp024Outcome;
import net.rai.scp_unleashed.scp024.Scp024Session;
import net.rai.scp_unleashed.scp024.Scp024Stage;
import net.rai.scp_unleashed.scp024.plan.Scp024Plan;
import net.rai.scp_unleashed.scp024.plan.Scp024Rooms;

import java.util.ArrayList;
import java.util.List;

public class ShootingGalleryGame implements Scp024Game {
    public static final int Seconds = 45;
    public static final int FirstWave = 2;
    public static final int MostShooters = 5;
    public static final int ReinforceEvery = 200;
    private final List<int[]> Free = new ArrayList<>();

    @Override
    public String id() {
        return "shooting_gallery";
    }

    @Override
    public Scp024Plan plan(long Seed) {
        return Scp024Rooms.shootingGallery(Seed);
    }

    @Override
    public int seconds() {
        return Seconds;
    }

    @Override
    public boolean survival() {
        return true;
    }

    @Override
    public void start(Scp024Session Session) {
        Free.addAll(Session.marks("turret"));
        for (int Index = 0; Index < FirstWave; Index++) {
            shooter(Session);
        }
    }

    private void shooter(Scp024Session Session) {
        if (Free.isEmpty()) {
            return;
        }
        int[] Pocket = Free.remove(Session.Random.nextInt(Free.size()));
        Skeleton Marksman = new Skeleton(EntityType.SKELETON, Session.World);
        Vec3 Spot = Session.centre(Pocket);
        Marksman.moveTo(Spot.x, Spot.y, Spot.z, Scp024Stage.yaw(Pocket[3]), 0.0f);
        Marksman.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        Scp024Hazards.contestantFoe(Marksman, 20.0, -1.0, 0.0);
        Scp024Hazards.setBase(Marksman, Attributes.KNOCKBACK_RESISTANCE, 1.0);
        Session.spawn(Marksman);
        Scp024Hazards.hunt(Marksman, Session.player());
    }

    @Override
    public Scp024Outcome tick(Scp024Session Session) {
        ServerPlayer User = Session.player();
        if (Session.ticks() % ReinforceEvery == 0 && Session.alive(Skeleton.class) < MostShooters) {
            shooter(Session);
            Session.cue(Scp024Cue.Ooh);
        }
        if (Session.ticks() % 20 == 0) {
            for (Entity Found : Session.spawned()) {
                if (Found instanceof Skeleton Marksman) {
                    Scp024Hazards.hunt(Marksman, User);
                }
            }
        }
        Session.status(Component.translatable("status.scp_unleashed.scp024.shooters", Session.alive(Skeleton.class)));
        return Scp024Outcome.Continue;
    }
}
