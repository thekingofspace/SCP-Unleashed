package net.rai.scp_unleashed.playerclone;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ResolvableProfile;
import net.rai.scp_unleashed.dclass.CloningBloodItem;
import net.rai.scp_unleashed.dclass.CloningKind;

import java.util.List;

public class PlayerSampleItem extends CloningBloodItem {
    public PlayerSampleItem(Properties Settings) {
        super(CloningKind.PlayerLine, Settings);
    }

    @Override
    public CompoundTag sampleOf(ItemStack Stack) {
        return PlayerSamples.write(Stack.get(PlayerCloneRegistry.SampleComponent.get()));
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        ResolvableProfile Profile = Stack.get(PlayerCloneRegistry.SampleComponent.get());
        String Name = PlayerSamples.nameOf(Profile);
        if (Name.isEmpty()) {
            Lines.add(Component.translatable("item.scp_unleashed.player_blood_sample.blank").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            Lines.add(Component.translatable("item.scp_unleashed.player_blood_sample.source", Name).withStyle(ChatFormatting.GOLD));
        }
        super.appendHoverText(Stack, Context, Lines, Flag);
    }
}
