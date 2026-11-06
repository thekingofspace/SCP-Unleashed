package net.rai.scp_unleashed.scp024.games;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp024.Scp024Game;
import net.rai.scp_unleashed.scp024.Scp024Hazards;
import net.rai.scp_unleashed.scp024.Scp024Outcome;
import net.rai.scp_unleashed.scp024.Scp024Session;
import net.rai.scp_unleashed.scp024.Scp024Stage;
import net.rai.scp_unleashed.scp024.plan.Scp024Plan;
import net.rai.scp_unleashed.scp024.plan.Scp024Rooms;
import org.jetbrains.annotations.Nullable;

public class ChampionBoutGame implements Scp024Game {
    public static final int Seconds = 90;
    public static final double Health = 32.0;
    public static final double Strength = 2.0;
    public static final double Speed = 0.24;
    @Nullable
    private Zombie Champion;

    @Override
    public String id() {
        return "champion_bout";
    }

    @Override
    public Scp024Plan plan(long Seed) {
        return Scp024Rooms.championBout(Seed);
    }

    @Override
    public int seconds() {
        return Seconds;
    }

    @Override
    public void start(Scp024Session Session) {
        Session.give(Scp024Hazards.named(new ItemStack(Items.IRON_SWORD), "item.scp_unleashed.scp024_show_sword"));
        Session.give(Scp024Hazards.named(new ItemStack(Items.SHIELD), "item.scp_unleashed.scp024_show_shield"));
        int[] Mark = Session.mark("champion");
        Vec3 At = Session.centre(Mark);
        Zombie Fighter = new Zombie(EntityType.ZOMBIE, Session.World);
        Fighter.moveTo(At.x, At.y, At.z, Scp024Stage.yaw(Mark[3]), 0.0f);
        Scp024Hazards.contestantFoe(Fighter, Health, Strength, Speed);
        Scp024Hazards.setBase(Fighter, Attributes.KNOCKBACK_RESISTANCE, 0.4);
        Fighter.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        Fighter.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        Fighter.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
        Fighter.setCustomName(Component.translatable("entity.scp_unleashed.scp024_champion").withStyle(ChatFormatting.GOLD));
        Fighter.setCustomNameVisible(true);
        Session.spawn(Fighter);
        Scp024Hazards.hunt(Fighter, Session.player());
        Champion = Fighter;
    }

    @Override
    public Scp024Outcome tick(Scp024Session Session) {
        ServerPlayer User = Session.player();
        if (Champion == null || !Champion.isAlive()) {
            return Scp024Outcome.Win;
        }
        if (Session.ticks() % 40 == 0 && Champion.getTarget() == null) {
            Scp024Hazards.hunt(Champion, User);
        }
        Session.status(Component.translatable("status.scp_unleashed.scp024.champion", (int) Math.ceil(Champion.getHealth()), (int) Health));
        return Scp024Outcome.Continue;
    }
}
