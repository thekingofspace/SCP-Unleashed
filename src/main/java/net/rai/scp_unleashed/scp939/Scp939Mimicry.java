package net.rai.scp_unleashed.scp939;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class Scp939Mimicry {
    public static final String Cow = "cow";
    public static final String Sheep = "sheep";
    public static final String Pig = "pig";
    public static final String Chicken = "chicken";
    public static final String Goat = "goat";
    public static final String Villager = "villager";
    public static final String Person = "person";
    public static final int LureHmmShare = 40;
    public static final int LureTradeShare = 20;
    public static final int LureYesShare = 15;
    public static final int LureNoShare = 15;
    private static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> Voices = Map.of(Cow, Scp939Registry.MimicCow, Sheep, Scp939Registry.MimicSheep,
            Pig, Scp939Registry.MimicPig, Chicken, Scp939Registry.MimicChicken, Goat, Scp939Registry.MimicGoat, Villager, Scp939Registry.MimicVillager,
            Person, Scp939Registry.MimicPerson);

    private Scp939Mimicry() {
    }

    @Nullable
    public static String voiceOf(@Nullable Entity Victim) {
        if (Victim instanceof Cow) {
            return Cow;
        }
        if (Victim instanceof Sheep) {
            return Sheep;
        }
        if (Victim instanceof Pig) {
            return Pig;
        }
        if (Victim instanceof Chicken) {
            return Chicken;
        }
        if (Victim instanceof Goat) {
            return Goat;
        }
        if (Victim instanceof AbstractVillager) {
            return Villager;
        }
        return Scp939Senses.isPerson(Victim) ? Person : null;
    }

    public static DeferredHolder<SoundEvent, SoundEvent> lureSound(RandomSource Random) {
        int Roll = Random.nextInt(100);
        if (Roll < LureHmmShare) {
            return Scp939Registry.LureHmm;
        }
        if (Roll < LureHmmShare + LureTradeShare) {
            return Scp939Registry.LureTrade;
        }
        if (Roll < LureHmmShare + LureTradeShare + LureYesShare) {
            return Scp939Registry.LureYes;
        }
        if (Roll < LureHmmShare + LureTradeShare + LureYesShare + LureNoShare) {
            return Scp939Registry.LureNo;
        }
        return Scp939Registry.LureHurt;
    }

    @Nullable
    public static DeferredHolder<SoundEvent, SoundEvent> sound(String Voice) {
        return Voices.get(Voice);
    }
}
