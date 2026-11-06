package net.rai.scp_unleashed.scp024.games;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp024.Scp024Cue;
import net.rai.scp_unleashed.scp024.Scp024Game;
import net.rai.scp_unleashed.scp024.Scp024Hazards;
import net.rai.scp_unleashed.scp024.Scp024Outcome;
import net.rai.scp_unleashed.scp024.Scp024Session;
import net.rai.scp_unleashed.scp024.plan.Scp024Plan;
import net.rai.scp_unleashed.scp024.plan.Scp024Rooms;

import java.util.List;

public class SumoRingGame implements Scp024Game {
    public static final int Seconds = 45;
    public static final int Wrestlers = 3;
    public static final int Respawn = 80;
    public static final double Shove = 0.95;
    public static final double Lift = 0.32;
    private int NextSpawn = -1;
    private int LastShove = Integer.MIN_VALUE;

    @Override
    public String id() {
        return "sumo_ring";
    }

    @Override
    public Scp024Plan plan(long Seed) {
        return Scp024Rooms.sumoRing(Seed);
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
    public double killDepth() {
        return -3.0;
    }

    @Override
    public void start(Scp024Session Session) {
        ItemStack Paddle = Scp024Hazards.named(new ItemStack(Items.STICK), "item.scp_unleashed.scp024_sumo_paddle");
        Holder<Enchantment> Knockback = Session.World.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.KNOCKBACK);
        Paddle.enchant(Knockback, 2);
        Session.give(Paddle);
        for (int Index = 0; Index < Wrestlers; Index++) {
            wrestler(Session);
        }
    }

    private void wrestler(Scp024Session Session) {
        List<int[]> Spots = Session.marks("spawn");
        int[] Spot = Spots.get(Session.Random.nextInt(Spots.size()));
        Zombie Sumo = new Zombie(EntityType.ZOMBIE, Session.World);
        Vec3 At = Session.centre(Spot);
        Sumo.moveTo(At.x, At.y, At.z, Session.Random.nextFloat() * 360.0f, 0.0f);
        Scp024Hazards.contestantFoe(Sumo, 24.0, 1.0, 0.26);
        Scp024Hazards.setBase(Sumo, Attributes.KNOCKBACK_RESISTANCE, 0.0);
        Sumo.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        Sumo.setCustomName(Component.translatable("entity.scp_unleashed.scp024_wrestler"));
        Session.spawn(Sumo);
        Scp024Hazards.hunt(Sumo, Session.player());
    }

    @Override
    public Scp024Outcome tick(Scp024Session Session) {
        ServerPlayer User = Session.player();
        if (User == null) {
            return Scp024Outcome.Continue;
        }
        for (Entity Found : Session.spawned()) {
            if (Found instanceof Zombie Sumo && Session.height(Sumo) < -3.0) {
                Sumo.discard();
                Session.cue(Scp024Cue.Cheer);
            }
        }
        int Alive = Session.alive(Zombie.class);
        if (Alive < Wrestlers && NextSpawn < 0) {
            NextSpawn = Session.ticks() + Respawn;
        }
        if (NextSpawn >= 0 && Session.ticks() >= NextSpawn) {
            NextSpawn = -1;
            wrestler(Session);
        }
        int Struck = User.getLastHurtByMobTimestamp();
        if (Struck != LastShove && User.tickCount - Struck <= 1 && User.getLastHurtByMob() instanceof Zombie Attacker && Session.spawned().contains(Attacker)) {
            LastShove = Struck;
            Vec3 Away = User.position().subtract(Attacker.position()).multiply(1.0, 0.0, 1.0);
            if (Away.lengthSqr() > 1.0E-4) {
                Away = Away.normalize();
                User.push(Away.x * Shove, Lift, Away.z * Shove);
                User.hurtMarked = true;
            }
        }
        double Edge = Math.sqrt(Math.pow(Session.local(User.position()).x - 8.5, 2) + Math.pow(Session.local(User.position()).z - 8.5, 2));
        if (Edge > 5.6) {
            Session.taunt("near");
        }
        return Scp024Outcome.Continue;
    }
}
