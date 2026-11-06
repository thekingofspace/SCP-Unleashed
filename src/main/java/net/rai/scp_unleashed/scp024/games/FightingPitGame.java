package net.rai.scp_unleashed.scp024.games;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp024.Scp024Cue;
import net.rai.scp_unleashed.scp024.Scp024Game;
import net.rai.scp_unleashed.scp024.Scp024Hazards;
import net.rai.scp_unleashed.scp024.Scp024Outcome;
import net.rai.scp_unleashed.scp024.Scp024Session;
import net.rai.scp_unleashed.scp024.plan.Scp024Plan;
import net.rai.scp_unleashed.scp024.plan.Scp024Rooms;

import java.util.List;

public class FightingPitGame implements Scp024Game {
    public static final int Seconds = 150;
    public static final int WaveGap = 50;
    public static final double SafeDistance = 5.0;
    public static final String[][] Waves = {{"zombie", "zombie"}, {"zombie", "zombie", "skeleton"}, {"zombie", "zombie", "zombie", "spider"}};
    private int Wave = -1;
    private int NextWave;

    @Override
    public String id() {
        return "fighting_pit";
    }

    @Override
    public Scp024Plan plan(long Seed) {
        return Scp024Rooms.fightingPit(Seed);
    }

    @Override
    public int seconds() {
        return Seconds;
    }

    @Override
    public void start(Scp024Session Session) {
        Session.give(Scp024Hazards.named(new ItemStack(Items.IRON_SWORD), "item.scp_unleashed.scp024_show_sword"));
        Session.give(Scp024Hazards.named(new ItemStack(Items.SHIELD), "item.scp_unleashed.scp024_show_shield"));
        NextWave = 20;
    }

    private void spawnWave(Scp024Session Session, ServerPlayer User) {
        List<int[]> Spots = Session.marks("spawn");
        for (String Kind : Waves[Wave]) {
            int[] Spot = Spots.get(Session.Random.nextInt(Spots.size()));
            for (int Try = 0; Try < 6; Try++) {
                int[] Other = Spots.get(Session.Random.nextInt(Spots.size()));
                if (Session.centre(Other).distanceTo(User.position()) > SafeDistance) {
                    Spot = Other;
                    break;
                }
            }
            Vec3 At = Session.centre(Spot);
            Monster Foe = switch (Kind) {
                case "skeleton" -> {
                    Skeleton Archer = new Skeleton(EntityType.SKELETON, Session.World);
                    Archer.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                    yield Scp024Hazards.contestantFoe(Archer, 16.0, -1.0, 0.25);
                }
                case "spider" -> Scp024Hazards.contestantFoe(new Spider(EntityType.SPIDER, Session.World), 16.0, 2.0, 0.3);
                default -> Scp024Hazards.contestantFoe(new Zombie(EntityType.ZOMBIE, Session.World), 20.0, 3.0, 0.23);
            };
            Foe.moveTo(At.x, At.y, At.z, Session.Random.nextFloat() * 360.0f, 0.0f);
            Session.spawn(Foe);
            Scp024Hazards.hunt(Foe, User);
        }
        Session.cue(Scp024Cue.Ooh);
    }

    @Override
    public Scp024Outcome tick(Scp024Session Session) {
        ServerPlayer User = Session.player();
        if (User == null) {
            return Scp024Outcome.Continue;
        }
        int Alive = Session.alive(Mob.class);
        if (Alive == 0 && NextWave < 0) {
            if (Wave >= Waves.length - 1) {
                return Scp024Outcome.Win;
            }
            NextWave = Session.ticks() + WaveGap;
            if (Wave >= 0) {
                Session.cheer("correct");
            }
        }
        if (NextWave >= 0 && Session.ticks() >= NextWave) {
            Wave++;
            NextWave = -1;
            spawnWave(Session, User);
        }
        if (Session.ticks() % 40 == 0) {
            for (Entity Found : Session.spawned()) {
                if (Found instanceof Mob Foe && Foe.getTarget() == null) {
                    Scp024Hazards.hunt(Foe, User);
                }
            }
        }
        Session.status(Component.translatable("status.scp_unleashed.scp024.wave", Math.max(1, Wave + 1), Waves.length, Alive));
        return Scp024Outcome.Continue;
    }
}
