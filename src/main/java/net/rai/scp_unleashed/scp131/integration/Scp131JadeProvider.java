package net.rai.scp_unleashed.scp131.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp131.Scp131Pod;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp131JadeProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp131_pod");
    private static final String Prefix = "jade.scp_unleashed.scp131.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    public static String moodKey(byte Mood) {
        return switch (Mood) {
            case Scp131Pod.MoodHappy -> "happy";
            case Scp131Pod.MoodCurious -> "curious";
            case Scp131Pod.MoodStartled -> "startled";
            case Scp131Pod.MoodShy -> "shy";
            case Scp131Pod.MoodSad -> "sad";
            case Scp131Pod.MoodPlayful -> "playful";
            default -> "content";
        };
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp131Pod Pod)) {
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "colour", Pod.getColour().displayName()).withStyle(ChatFormatting.GRAY));
        byte Mood = Pod.isSad() ? Scp131Pod.MoodSad : Pod.getMood();
        ChatFormatting Tone = Mood == Scp131Pod.MoodSad ? ChatFormatting.BLUE : Mood == Scp131Pod.MoodStartled ? ChatFormatting.GOLD : ChatFormatting.GREEN;
        Tooltip.add(Component.translatable(Prefix + "mood", Component.translatable(Prefix + "mood." + moodKey(Mood)).withStyle(Tone)).withStyle(ChatFormatting.GRAY));
    }
}
