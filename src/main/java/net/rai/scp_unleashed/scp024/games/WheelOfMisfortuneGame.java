package net.rai.scp_unleashed.scp024.games;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp024.Scp024Cue;
import net.rai.scp_unleashed.scp024.Scp024Game;
import net.rai.scp_unleashed.scp024.Scp024Hazards;
import net.rai.scp_unleashed.scp024.Scp024Outcome;
import net.rai.scp_unleashed.scp024.Scp024Registry;
import net.rai.scp_unleashed.scp024.Scp024Session;
import net.rai.scp_unleashed.scp024.plan.Scp024Mat;
import net.rai.scp_unleashed.scp024.plan.Scp024Plan;
import net.rai.scp_unleashed.scp024.plan.Scp024Rooms;

import java.util.List;

public class WheelOfMisfortuneGame implements Scp024Game {
    public static final int Size = 17;
    public static final int Steps = 30;
    public static final int HazardTicks = 400;
    public static final int Settle = 30;
    public static final String[] Hazards = {"anvils", "bombs", "arrows", "zombies", "hot_floor", "silverfish"};
    private final Droppers Falling = new Droppers(10.0f, 9.0f, 3.2);
    private final ArrowStorm Arrows = new ArrowStorm();
    private final HotTiles Floor = new HotTiles(0, 0, Size - 1, Size - 1, 0.55);
    private int Step;
    private int NextStep;
    private int Landing;
    private int Lit = -1;
    private int Hazard = -1;
    private int HazardStart = -1;
    private int Next;

    @Override
    public String id() {
        return "wheel_of_misfortune";
    }

    @Override
    public Scp024Plan plan(long Seed) {
        return Scp024Rooms.wheelOfMisfortune(Seed);
    }

    @Override
    public int seconds() {
        return 0;
    }

    @Override
    public int rulePages() {
        return 2;
    }

    @Override
    public void start(Scp024Session Session) {
        Landing = Session.Random.nextInt(Session.marks("segment").size());
        Step = 0;
        NextStep = 0;
        Session.cue(Scp024Cue.Drumroll);
    }

    private void highlight(Scp024Session Session, int Index) {
        List<int[]> Segments = Session.marks("segment");
        if (Lit >= 0) {
            int[] Old = Segments.get(Lit);
            Session.restore(Old[0], Old[1], Old[2]);
        }
        Lit = Index;
        int[] Spot = Segments.get(Index);
        Session.set(Spot[0], Spot[1], Spot[2], Scp024Mat.SeaLantern);
        Session.sound(Session.centre(Spot), Scp024Registry.Click.get(), 0.9f, 1.4f + Step * 0.01f);
    }

    private void spin(Scp024Session Session) {
        List<int[]> Segments = Session.marks("segment");
        int Count = Segments.size();
        int Start = Math.floorMod(Landing - Steps, Count);
        highlight(Session, (Start + Step) % Count);
        Step++;
        int Delay = 1 + (Step * Step) / 110;
        NextStep = Session.ticks() + Delay;
        if (Step > Steps) {
            Hazard = Segments.get(Landing)[3] % Hazards.length;
            HazardStart = Session.ticks() + Settle;
            Next = HazardStart;
            Session.objective(Component.translatable("objective.scp_unleashed.scp024.wheel",
                    Component.translatable("hazard.scp_unleashed.scp024." + Hazards[Hazard]).withStyle(ChatFormatting.RED)));
            Session.taunt("near");
            Session.cue(Scp024Cue.Fanfare);
        }
    }

    private void unleash(Scp024Session Session, ServerPlayer User) {
        switch (Hazards[Hazard]) {
            case "anvils" -> {
                if (Session.ticks() >= Next) {
                    AnvilRainGame.drop(Session, Falling, User, Size, 0.5f, 12.0);
                    Next = Session.ticks() + 9;
                }
            }
            case "bombs" -> {
                if (Session.ticks() >= Next) {
                    TntDodgeGame.lob(Session, Falling, User, List.of(), Size, 0.5f);
                    Next = Session.ticks() + 18;
                }
            }
            case "arrows" -> {
                if (Session.ticks() >= Next) {
                    Arrows.aimed(Session, Size, Size, 1.6, 1.6f, 2.0);
                    Next = Session.ticks() + 12;
                }
            }
            case "zombies" -> {
                if (Session.ticks() == HazardStart) {
                    for (int Count = 0; Count < 3; Count++) {
                        Zombie Foe = new Zombie(EntityType.ZOMBIE, Session.World);
                        Vec3 At = Session.centre(Count * 7 + 1, 1, 1);
                        Foe.moveTo(At.x, At.y, At.z, 0.0f, 0.0f);
                        Scp024Hazards.contestantFoe(Foe, 20.0, 3.0, 0.24);
                        Session.spawn(Foe);
                        Scp024Hazards.hunt(Foe, User);
                    }
                }
            }
            case "hot_floor" -> {
                if (Session.ticks() >= Next) {
                    BlockPos Cell = Session.standing(User);
                    if (Session.Random.nextBoolean()) {
                        Floor.heatNear(Session, Cell.getX(), Cell.getZ(), 2);
                    } else {
                        Floor.heatAnywhere(Session);
                    }
                    Next = Session.ticks() + 4;
                }
            }
            default -> {
                if (Session.ticks() >= Next && Session.alive(Silverfish.class) < 10) {
                    Silverfish Pest = new Silverfish(EntityType.SILVERFISH, Session.World);
                    Vec3 At = Session.centre(Session.Random.nextInt(Size), 1, Session.Random.nextBoolean() ? 0 : Size - 1);
                    Pest.moveTo(At.x, At.y, At.z, 0.0f, 0.0f);
                    Scp024Hazards.contestantFoe(Pest, 8.0, 1.0, 0.3);
                    Session.spawn(Pest);
                    Scp024Hazards.hunt(Pest, User);
                    Next = Session.ticks() + 20;
                }
            }
        }
    }

    @Override
    public Scp024Outcome tick(Scp024Session Session) {
        ServerPlayer User = Session.player();
        if (Hazard < 0) {
            if (Session.ticks() >= NextStep) {
                spin(Session);
            }
            Session.status(Component.translatable("status.scp_unleashed.scp024.spinning"));
            return Scp024Outcome.Continue;
        }
        if (Session.ticks() < HazardStart) {
            return Scp024Outcome.Continue;
        }
        int Left = HazardTicks - (Session.ticks() - HazardStart);
        if (Left <= 0) {
            return Scp024Outcome.Win;
        }
        if (User != null) {
            unleash(Session, User);
            if (Session.ticks() % 40 == 0) {
                for (Entity Found : Session.spawned()) {
                    if (Found instanceof Mob Foe && Foe.getTarget() == null) {
                        Scp024Hazards.hunt(Foe, User);
                    }
                }
            }
        }
        Falling.tick(Session);
        Arrows.tick(Session);
        Floor.tick(Session);
        Session.status(Component.translatable("status.scp_unleashed.scp024.wheel", Component.translatable("hazard.scp_unleashed.scp024." + Hazards[Hazard]), (Left + 19) / 20));
        return Scp024Outcome.Continue;
    }

    @Override
    public void cleanup(Scp024Session Session) {
        Falling.clear();
    }
}
