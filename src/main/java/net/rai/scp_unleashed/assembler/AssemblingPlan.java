package net.rai.scp_unleashed.assembler;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;

public final class AssemblingPlan {
    private static final int Unlimited = Integer.MAX_VALUE / 4;

    private AssemblingPlan() {
    }

    @Nullable
    public static int[] solve(List<SizedIngredient> Needs, List<ItemStack> Items) {
        int NeedCount = Needs.size();
        int SlotCount = Items.size();
        int Source = 0;
        int Sink = NeedCount + SlotCount + 1;
        int[][] Capacity = new int[Sink + 1][Sink + 1];
        int Wanted = 0;
        for (int Need = 0; Need < NeedCount; Need++) {
            Capacity[Source][1 + Need] = Needs.get(Need).count();
            Wanted += Needs.get(Need).count();
        }
        for (int Slot = 0; Slot < SlotCount; Slot++) {
            ItemStack Stack = Items.get(Slot);
            if (Stack.isEmpty()) {
                continue;
            }
            boolean Used = false;
            for (int Need = 0; Need < NeedCount; Need++) {
                if (Needs.get(Need).ingredient().test(Stack)) {
                    Capacity[1 + Need][1 + NeedCount + Slot] = Unlimited;
                    Used = true;
                }
            }
            if (!Used) {
                return null;
            }
            Capacity[1 + NeedCount + Slot][Sink] = Stack.getCount();
        }
        int[][] Flow = new int[Sink + 1][Sink + 1];
        int Total = 0;
        int[] Parent = new int[Sink + 1];
        while (Total < Wanted) {
            int Pushed = augment(Capacity, Flow, Parent, Source, Sink);
            if (Pushed <= 0) {
                break;
            }
            Total += Pushed;
        }
        if (Total < Wanted) {
            return null;
        }
        int[] Taken = new int[SlotCount];
        for (int Slot = 0; Slot < SlotCount; Slot++) {
            Taken[Slot] = Flow[1 + NeedCount + Slot][Sink];
        }
        return Taken;
    }

    private static int augment(int[][] Capacity, int[][] Flow, int[] Parent, int Source, int Sink) {
        Arrays.fill(Parent, -1);
        Parent[Source] = Source;
        ArrayDeque<Integer> Queue = new ArrayDeque<>();
        Queue.add(Source);
        while (!Queue.isEmpty() && Parent[Sink] < 0) {
            int Node = Queue.poll();
            for (int Next = 0; Next < Parent.length; Next++) {
                if (Parent[Next] < 0 && Capacity[Node][Next] - Flow[Node][Next] > 0) {
                    Parent[Next] = Node;
                    Queue.add(Next);
                }
            }
        }
        if (Parent[Sink] < 0) {
            return 0;
        }
        int Bottleneck = Unlimited;
        for (int Node = Sink; Node != Source; Node = Parent[Node]) {
            Bottleneck = Math.min(Bottleneck, Capacity[Parent[Node]][Node] - Flow[Parent[Node]][Node]);
        }
        for (int Node = Sink; Node != Source; Node = Parent[Node]) {
            Flow[Parent[Node]][Node] += Bottleneck;
            Flow[Node][Parent[Node]] -= Bottleneck;
        }
        return Bottleneck;
    }
}
