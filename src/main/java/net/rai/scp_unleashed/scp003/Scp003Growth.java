package net.rai.scp_unleashed.scp003;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.Nullable;

public final class Scp003Growth {
    private Scp003Growth() {
    }

    @Nullable
    public static EntityType<? extends Scp003Creature> typeFor(int Stage) {
        return switch (Stage) {
            case 1 -> Scp003Registry.Larva.get();
            case 2 -> Scp003Registry.Crawler.get();
            case 3 -> Scp003Registry.Host.get();
            case 4 -> Scp003Registry.Alpha.get();
            default -> null;
        };
    }

    @Nullable
    public static <T extends Scp003Creature> T transform(Scp003Creature From, EntityType<T> To, byte Action, int Ticks) {
        if (!(From.level() instanceof ServerLevel World) || From.isTransferred() || From.isRemoved()) {
            return null;
        }
        T Next = To.create(World);
        if (Next == null) {
            return null;
        }
        Next.moveTo(From.getX(), From.getY(), From.getZ(), From.getYRot(), From.getXRot());
        Next.setYHeadRot(From.getYHeadRot());
        Next.setYBodyRot(From.yBodyRot);
        Next.inherit(From);
        float Ratio = From.getMaxHealth() > 0.0f ? From.getHealth() / From.getMaxHealth() : 1.0f;
        Next.setHealth(Math.max(1.0f, (float) Next.getAttributeValue(Attributes.MAX_HEALTH) * Math.max(0.5f, Ratio)));
        Next.setAction(Action, Ticks);
        if (From instanceof Scp003Host Builder) {
            Builder.abandonBuild(false);
        }
        Scp003Colony Colony = Scp003Colony.get(World.getServer());
        if (From instanceof Scp003Alpha && !(Next instanceof Scp003Alpha)) {
            Colony.clearAlpha(From.lineage());
        }
        From.markTransferred();
        From.discard();
        World.addFreshEntity(Next);
        if (Next instanceof Scp003Alpha Promoted) {
            Colony.setAlpha(Promoted, World);
        }
        return Next;
    }
}
