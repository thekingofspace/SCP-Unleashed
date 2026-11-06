package net.rai.scp_unleashed.scp016;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.dclass.DClassAppearance;
import net.rai.scp_unleashed.dclass.DClassLike;

public final class Scp016Host {
    public static final String[] Fabrics = {"orange", "robe", "shirt"};
    public static final String[] Bands = {"light", "dark"};
    public static final int Variants = Fabrics.length * Bands.length;
    public static final int Unset = -1;
    public static final int Orange = 0;
    public static final int Robe = 1;
    public static final int Shirt = 2;
    public static final int LightestDarkSkin = 3;

    private Scp016Host() {
    }

    public static int of(LivingEntity Fallen, RandomSource Random) {
        if (Fallen instanceof DClassLike Body) {
            return code(Orange, DClassAppearance.skin(Body.appearance()) >= LightestDarkSkin ? 1 : 0);
        }
        if (Fallen instanceof AbstractVillager) {
            return code(Robe, 0);
        }
        if (Fallen instanceof Player) {
            return code(Shirt, Random.nextInt(Bands.length));
        }
        if (ControlBodies.isPersonLike(Fallen)) {
            return code(Orange, Random.nextInt(Bands.length));
        }
        return random(Random);
    }

    public static int random(RandomSource Random) {
        return Random.nextInt(Variants);
    }

    public static int code(int Fabric, int Band) {
        return Fabric * Bands.length + Band;
    }

    public static int clamp(int Code) {
        return Code < 0 || Code >= Variants ? 0 : Code;
    }

    public static String name(int Code) {
        int Safe = clamp(Code);
        return Fabrics[Safe / Bands.length] + "_" + Bands[Safe % Bands.length];
    }
}
