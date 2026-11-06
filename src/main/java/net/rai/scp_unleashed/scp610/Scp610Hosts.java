package net.rai.scp_unleashed.scp610;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

public final class Scp610Hosts {
    public static final float SmallSize = 0.9f;
    public static final float LargeHeight = 2.2f;
    public static final float LargeWidth = 1.3f;

    public record Rule(Predicate<LivingEntity> Test, Scp610Form Form) {
    }

    private static final List<Rule> Rules = new CopyOnWriteArrayList<>();

    static {
        add(Host -> Host instanceof AbstractVillager || Host instanceof AbstractIllager || Host instanceof Witch, Scp610Form.Villager);
        add(Host -> Host instanceof Pig, Scp610Form.Pig);
        add(Host -> Host instanceof Cow, Scp610Form.Cow);
        add(Host -> Host instanceof Sheep, Scp610Form.Sheep);
        add(Host -> Host instanceof Chicken, Scp610Form.Chicken);
        add(Host -> Host instanceof AbstractHorse && !(Host instanceof Llama), Scp610Form.Horse);
        add(Host -> Host instanceof Zombie, Scp610Form.Shambler);
    }

    private Scp610Hosts() {
    }

    public static void add(Predicate<LivingEntity> Test, Scp610Form Form) {
        Rules.add(new Rule(Test, Form));
    }

    @Nullable
    public static Scp610Form dedicated(LivingEntity Host) {
        for (Rule Each : Rules) {
            if (Each.Test().test(Host)) {
                return Each.Form();
            }
        }
        return null;
    }

    public static Scp610Form generic(float Width, float Height) {
        if (Height <= SmallSize && Width <= SmallSize) {
            return Scp610Form.Crawler;
        }
        if (Height >= LargeHeight || Width >= LargeWidth) {
            return Scp610Form.Hulk;
        }
        return Scp610Form.Shambler;
    }

    public static Scp610Form formFor(LivingEntity Host) {
        if (Host instanceof Player) {
            return Scp610Form.Apostle;
        }
        Scp610Form Found = dedicated(Host);
        return Found != null ? Found : generic(Host.getBbWidth(), Host.getBbHeight());
    }

    @Nullable
    public static Scp610Creature convert(ServerLevel World, LivingEntity Host) {
        if (InfectionImmunity.isImmune(Host, Scp610Registry.Immune)) {
            Scp610Infection.forget(Host);
            return null;
        }
        Scp610Form Form = formFor(Host);
        Scp610Infection.forget(Host);
        Scp610Creature Risen = Scp610Spawning.raise(World, Form, Host.position(), Host.getYRot(), true);
        if (Risen != null && Host instanceof Mob Body && Body.hasCustomName()) {
            Risen.setCustomName(Body.getCustomName());
        }
        Host.discard();
        return Risen;
    }
}
