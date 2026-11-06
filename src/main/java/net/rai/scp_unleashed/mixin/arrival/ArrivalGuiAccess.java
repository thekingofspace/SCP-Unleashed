package net.rai.scp_unleashed.mixin.arrival;

import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Gui.class)
public interface ArrivalGuiAccess {
    @Accessor("title")
    Component scpArrivalTitle();

    @Accessor("titleTime")
    int scpArrivalTitleTime();
}
