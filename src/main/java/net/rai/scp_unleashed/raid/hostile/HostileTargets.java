package net.rai.scp_unleashed.raid.hostile;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.Tags;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.mtf.MtfOperative;
import net.rai.scp_unleashed.scp268.Scp268;
import net.rai.scp_unleashed.stasis.StasisPods;
import net.rai.scp_unleashed.stasis.StasisRegistry;
import net.rai.scp_unleashed.storage.StorageTags;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HostileTargets {
    public static final Pattern ScpPath = Pattern.compile("^scp(\\d{3,4})(j?)(?:_|$)");
    public static final Pattern ZPath = Pattern.compile("^z(\\d{4})(?:_|$)");

    private HostileTargets() {
    }

    @Nullable
    public static ResourceLocation documentFor(ResourceLocation Id) {
        if (!Scp_unleashed.ModId.equals(Id.getNamespace())) {
            return null;
        }
        Matcher Scp = ScpPath.matcher(Id.getPath());
        if (Scp.find()) {
            String Suffix = Scp.group(2).isEmpty() ? "" : "_" + Scp.group(2);
            return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_" + Scp.group(1) + Suffix);
        }
        Matcher Zed = ZPath.matcher(Id.getPath());
        if (Zed.find()) {
            return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "z_" + Zed.group(1));
        }
        return null;
    }

    @Nullable
    public static DocumentClass classOf(ResourceLocation Id) {
        ResourceLocation Document = documentFor(Id);
        if (Document == null) {
            return null;
        }
        AnomalyDocument Found = AnomalyDocuments.server().get(Document);
        return Found == null ? null : Found.Classification();
    }

    @Nullable
    public static DocumentClass classOf(Entity Body) {
        return classOf(BuiltInRegistries.ENTITY_TYPE.getKey(Body.getType()));
    }

    public static boolean isObjectAnomaly(ItemStack Stack) {
        return StorageTags.isStorable(Stack);
    }

    public static boolean isSafeCreature(Entity Body) {
        return classOf(Body) == DocumentClass.Safe;
    }

    public static boolean capturable(Entity Body) {
        if (!(Body instanceof Mob Creature) || !Creature.isAlive() || Creature.isRemoved() || Body instanceof Player || Body instanceof HostileRaider) {
            return false;
        }
        if (Body instanceof Enemy || Targeting.isStatue(Body) || Creature.isInvulnerable() || Body.isMultipartEntity() || !Body.getType().canSerialize()) {
            return false;
        }
        if (Body.getType().is(StasisRegistry.Immune) || Body.getType().is(Tags.EntityTypes.BOSSES) || StasisPods.isVetoed(Body) || StasisPods.isRiddenByPlayer(Body)) {
            return false;
        }
        return isSafeCreature(Body);
    }

    public static boolean hostileTo(HostileRaider Raider, @Nullable LivingEntity Other) {
        if (Other == null || Other == Raider || !Other.isAlive() || Other.isRemoved() || !Targeting.isTargetable(Other) || Other.level() != Raider.level()) {
            return false;
        }
        if (Other instanceof Player User) {
            return !User.isCreative() && !User.isSpectator() && !Scp268.isNeglected(User);
        }
        if (Other instanceof HostileRaider Rival) {
            return !Rival.factionId().equals(Raider.factionId());
        }
        return Other instanceof MtfOperative || Other instanceof DClass;
    }
}
