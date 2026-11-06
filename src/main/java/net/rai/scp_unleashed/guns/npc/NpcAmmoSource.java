package net.rai.scp_unleashed.guns.npc;

import net.minecraft.world.item.Item;

public interface NpcAmmoSource {
    int take(Item AmmoItem, int Wanted);
}
