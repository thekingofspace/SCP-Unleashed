package net.rai.scp_unleashed.computer.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record ScheduledAction(int Id, String Title, List<ActionStep> OnSteps, List<ActionStep> OffSteps, List<ActionTrigger> Triggers) {
    public static final int MaxTitle = 16;
    public static final int MaxSteps = 8;
    public static final int MaxTriggers = 4;
    public static final Codec<ScheduledAction> ActionCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.INT.fieldOf("id").forGetter(ScheduledAction::Id),
            Codec.STRING.optionalFieldOf("title", "").forGetter(ScheduledAction::Title),
            ActionStep.StepCodec.listOf().optionalFieldOf("on", List.of()).forGetter(ScheduledAction::OnSteps),
            ActionStep.StepCodec.listOf().optionalFieldOf("off", List.of()).forGetter(ScheduledAction::OffSteps),
            ActionTrigger.TriggerCodec.listOf().optionalFieldOf("triggers", List.of()).forGetter(ScheduledAction::Triggers)
    ).apply(Instance, ScheduledAction::new));

    public ScheduledAction {
        Title = ComputerText.upper(Title, MaxTitle);
        if (Title.isEmpty()) {
            Title = "ACTION";
        }
        OnSteps = List.copyOf(OnSteps.size() > MaxSteps ? OnSteps.subList(0, MaxSteps) : OnSteps);
        OffSteps = List.copyOf(OffSteps.size() > MaxSteps ? OffSteps.subList(0, MaxSteps) : OffSteps);
        Triggers = List.copyOf(Triggers.size() > MaxTriggers ? Triggers.subList(0, MaxTriggers) : Triggers);
    }

    public ScheduledAction withId(int NewId) {
        return new ScheduledAction(NewId, Title, OnSteps, OffSteps, Triggers);
    }

    public List<ActionStep> steps(boolean On) {
        return On ? OnSteps : OffSteps;
    }
}
