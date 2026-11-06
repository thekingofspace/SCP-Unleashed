package net.rai.scp_unleashed.computer.client.apps;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.computer.client.ComputerScreen;
import net.rai.scp_unleashed.computer.client.OsApp;
import net.rai.scp_unleashed.computer.client.OsField;
import net.rai.scp_unleashed.computer.client.OsLayout;
import net.rai.scp_unleashed.computer.client.OsUi;
import net.rai.scp_unleashed.computer.data.ActionStep;
import net.rai.scp_unleashed.computer.data.ActionTrigger;
import net.rai.scp_unleashed.computer.data.ChatChannels;
import net.rai.scp_unleashed.computer.data.ComputerText;
import net.rai.scp_unleashed.computer.data.ScheduledAction;
import net.rai.scp_unleashed.computer.data.SoundEntry;
import net.rai.scp_unleashed.computer.net.ChatService;
import net.rai.scp_unleashed.computer.net.ComputerCodecs;
import net.rai.scp_unleashed.computer.network.ComputerAction;
import net.rai.scp_unleashed.wire.WireChannels;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SchedulerApp implements OsApp {
    public static final String Prefix = "gui.scp_unleashed.computer.scheduler.";
    public static final int ActionHeight = 22;
    public static final int VisibleActions = 5;
    public static final int EntryHeight = 13;
    public static final int VisibleEntries = 8;

    private final List<ActionRow> Actions = new ArrayList<>();
    private final List<Target> Channels = new ArrayList<>();
    private final List<SoundEntry> Sounds = new ArrayList<>();
    private final OsField Title = new OsField(ScheduledAction.MaxTitle, true, OsField.AnyText);
    private final OsField StepText = new OsField(ActionStep.MaxText, false, OsField.AnyText);
    private final OsField StepPort = new OsField(WireChannels.PortDigits, false, OsField.Digits);
    private final OsField TriggerPort = new OsField(WireChannels.PortDigits, false, OsField.Digits);
    private final List<ActionStep> OnSteps = new ArrayList<>();
    private final List<ActionStep> OffSteps = new ArrayList<>();
    private final List<ActionTrigger> Triggers = new ArrayList<>();
    private int Max = 12;
    private int Clock;
    private int Scroll;
    private View Showing = View.Overview;
    private int EditId;
    private int Tab;
    private int EntryScroll;
    private int StepIndex = -1;
    private ActionStep.Kind StepType = ActionStep.Kind.Message;
    private String StepTarget = ActionStep.AllChannels;
    private String StepTargetName = "";
    private boolean StepTimed = true;
    private int StepSeconds = 10;
    private int StepSound = -1;
    private boolean StepStart = true;
    private int TriggerIndex = -1;
    private ActionTrigger.Kind TriggerType = ActionTrigger.Kind.Time;
    private ActionTrigger.Event TriggerEvent = ActionTrigger.Event.Activate;
    private int TriggerValue = 18 * 60;
    private ComputerScreen Owner;

    @Override
    public void accept(CompoundTag Data, HolderLookup.Provider Registries) {
        Actions.clear();
        Max = Data.getInt("Max");
        Clock = Data.getInt("Clock");
        ListTag Saved = Data.getList("Actions", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Saved.size(); Index++) {
            CompoundTag Each = Saved.getCompound(Index);
            ComputerCodecs.decode(ScheduledAction.ActionCodec, Each, Registries)
                    .ifPresent(Action -> Actions.add(new ActionRow(Action, Each.getBoolean("Active"), Each.getLong("For"), Each.getInt("Holds"))));
        }
        Channels.clear();
        ListTag Known = Data.getList("Channels", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Known.size(); Index++) {
            CompoundTag Each = Known.getCompound(Index);
            Channels.add(new Target(Each.getString("Key"), Each.getString("Name")));
        }
        Sounds.clear();
        Sounds.addAll(ComputerCodecs.decodeList(SoundEntry.EntryCodec, Data, "Sounds", Registries));
    }

    @Override
    public boolean back() {
        switch (Showing) {
            case Step, Trigger -> {
                Showing = View.Action;
                return true;
            }
            case Action -> {
                Showing = View.Overview;
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    public boolean submit(OsField Field) {
        if (Showing == View.Step) {
            if (StepType != ActionStep.Kind.Message || !StepText.getValue().isBlank()) {
                confirmStep();
            }
            return true;
        }
        if (Showing == View.Trigger) {
            confirmTrigger();
            return true;
        }
        if (Showing == View.Action) {
            Owner.ui().focus(null);
            return true;
        }
        return false;
    }

    @Override
    public void render(ComputerScreen Screen, OsUi Ui, int X, int Y, int W, int H) {
        Owner = Screen;
        switch (Showing) {
            case Overview -> drawList(Screen, Ui, X, Y, W, H);
            case Action -> drawAction(Ui, X, Y, W, H);
            case Step -> drawStep(Ui, X, Y, W, H);
            case Trigger -> drawTrigger(Ui, X, Y, W, H);
        }
    }

    private void drawList(ComputerScreen Screen, OsUi Ui, int X, int Y, int W, int H) {
        Ui.heading(Component.translatable(Prefix + "list", Actions.size(), Max).getString(), X, Y + 1, W - 70);
        Ui.right(Component.translatable(Prefix + "now", ComputerText.clock(Clock)).getString(), X + W, Y + 1, OsLayout.PhosphorTitle);
        int Top = Y + 13;
        Scroll = Math.max(0, Math.min(Scroll, Math.max(0, Actions.size() - VisibleActions)));
        Ui.wheel(X, Top, W, VisibleActions * ActionHeight, Step -> Scroll = Math.max(0, Math.min(Math.max(0, Actions.size() - VisibleActions), Scroll + Step)));
        if (Actions.isEmpty()) {
            Ui.centered(Component.translatable(Prefix + "empty").getString(), X + W / 2, Top + 30, OsLayout.PhosphorDim);
            Ui.centered(Component.translatable(Prefix + "empty.hint.0").getString(), X + W / 2, Top + 44, OsLayout.PhosphorFaint);
            Ui.centered(Component.translatable(Prefix + "empty.hint.1").getString(), X + W / 2, Top + 54, OsLayout.PhosphorFaint);
        }
        for (int Index = 0; Index < VisibleActions && Scroll + Index < Actions.size(); Index++) {
            drawActionRow(Screen, Ui, Actions.get(Scroll + Index), X, Top + Index * ActionHeight, W - 6);
        }
        Ui.scrollbar(X + W - 4, Top, VisibleActions * ActionHeight, Scroll, Actions.size(), VisibleActions);
        Ui.button(X, Y + H - 13, 90, OsLayout.ButtonHeight, Component.translatable(Prefix + "new"), Actions.size() < Max, this::create,
                Component.translatable(Prefix + "new.hint"));
    }

    private void drawActionRow(ComputerScreen Screen, OsUi Ui, ActionRow Row, int X, int Y, int W) {
        ScheduledAction Action = Row.Action();
        Ui.box(X, Y, W, ActionHeight - 2, Row.Active() ? OsLayout.Amber : OsLayout.PhosphorFaint, Row.Active() ? 0xFF2A2210 : 0xFF071A0D);
        Ui.lamp(X + 4, Y + 4, Row.Active(), 1);
        String State = Row.Active() ? Component.translatable(Prefix + "active", duration(Row.For())).getString() : Component.translatable(Prefix + "idle").getString();
        int Room = W - 98 - 13 - Ui.font().width(State) - 6;
        Ui.clipped(Action.Title(), X + 13, Y + 3, Room, Row.Active() ? OsLayout.Amber : OsLayout.PhosphorHot);
        String Summary = Component.translatable(Prefix + "summary", Action.OnSteps().size(), Action.OffSteps().size(), Action.Triggers().size()).getString();
        Ui.clipped(Summary, X + 13, Y + 11, Room, OsLayout.PhosphorDim);
        Ui.right(State, X + W - 98, Y + 7, Row.Active() ? OsLayout.Amber : OsLayout.PhosphorFaint);
        int Id = Action.Id();
        Ui.button(X + W - 94, Y + 4, 28, 12, Component.translatable(Prefix + "on"), !Row.Active(), () -> run(Screen, Id, true), Component.translatable(Prefix + "on.hint"));
        Ui.button(X + W - 64, Y + 4, 28, 12, Component.translatable(Prefix + "off"), Row.Active(), () -> run(Screen, Id, false), Component.translatable(Prefix + "off.hint"));
        Ui.button(X + W - 34, Y + 4, 30, 12, Component.translatable(Prefix + "edit"), true, () -> edit(Action), null);
    }

    private static String duration(long Ticks) {
        long Seconds = Ticks / 20L;
        return Seconds >= 3600L ? String.format(Locale.ROOT, "%d:%02d:%02d", Seconds / 3600L, Seconds / 60L % 60L, Seconds % 60L) : String.format(Locale.ROOT, "%d:%02d", Seconds / 60L, Seconds % 60L);
    }

    private void run(ComputerScreen Screen, int Id, boolean On) {
        CompoundTag Data = new CompoundTag();
        Data.putInt("Id", Id);
        Data.putBoolean("On", On);
        Screen.send(ComputerAction.ActionRun, Data);
    }

    private void create() {
        EditId = 0;
        Title.setValue("");
        OnSteps.clear();
        OffSteps.clear();
        Triggers.clear();
        Tab = 0;
        EntryScroll = 0;
        Showing = View.Action;
        Owner.ui().focus(Title);
    }

    private void edit(ScheduledAction Action) {
        EditId = Action.Id();
        Title.setValue(Action.Title());
        OnSteps.clear();
        OnSteps.addAll(Action.OnSteps());
        OffSteps.clear();
        OffSteps.addAll(Action.OffSteps());
        Triggers.clear();
        Triggers.addAll(Action.Triggers());
        Tab = 0;
        EntryScroll = 0;
        Showing = View.Action;
        Owner.ui().focus(null);
    }

    private void saveAction() {
        ScheduledAction Draft = new ScheduledAction(EditId, Title.getValue(), List.copyOf(OnSteps), List.copyOf(OffSteps), List.copyOf(Triggers));
        Tag Encoded = ComputerCodecs.encode(ScheduledAction.ActionCodec, Draft, Owner.registries());
        if (Encoded != null) {
            CompoundTag Data = new CompoundTag();
            Data.put("Action", Encoded);
            Owner.send(ComputerAction.ActionSave, Data);
        }
        Showing = View.Overview;
        Owner.ui().focus(null);
    }

    private void drawAction(OsUi Ui, int X, int Y, int W, int H) {
        Ui.text(Component.translatable(Prefix + "name").getString(), X, Y + 3, OsLayout.PhosphorTitle);
        Ui.field(Title, X + 30, Y, 118, Component.translatable(Prefix + "name.hint"), null);
        Ui.button(X + W - 132, Y, 42, OsLayout.ButtonHeight, Component.translatable(Prefix + "save"), true, this::saveAction, Component.translatable(Prefix + "save.hint"));
        if (EditId != 0) {
            Ui.button(X + W - 88, Y, 42, OsLayout.ButtonHeight, Component.translatable(Prefix + "delete"), true, () -> {
                CompoundTag Data = new CompoundTag();
                Data.putInt("Id", EditId);
                Owner.send(ComputerAction.ActionDelete, Data);
                Showing = View.Overview;
            }, null);
        }
        Ui.button(X + W - 44, Y, 44, OsLayout.ButtonHeight, Component.translatable(Prefix + "back"), true, () -> Showing = View.Overview,
                Component.translatable(Prefix + "back.hint"));
        int TabWidth = (W - 4) / 3;
        String[] Keys = {"tab.on", "tab.off", "tab.triggers"};
        int[] Counts = {OnSteps.size(), OffSteps.size(), Triggers.size()};
        for (int Index = 0; Index < 3; Index++) {
            int Picked = Index;
            Ui.toggle(X + Index * (TabWidth + 2), Y + 17, TabWidth, OsLayout.ButtonHeight, Component.translatable(Prefix + Keys[Index], Counts[Index]), Tab == Index, () -> {
                Tab = Picked;
                EntryScroll = 0;
            }, Component.translatable(Prefix + Keys[Index] + ".hint"));
        }
        int Top = Y + 34;
        int Count = Tab == 2 ? Triggers.size() : (Tab == 0 ? OnSteps : OffSteps).size();
        EntryScroll = Math.max(0, Math.min(EntryScroll, Math.max(0, Count - VisibleEntries)));
        Ui.wheel(X, Top, W, VisibleEntries * EntryHeight, Step -> EntryScroll = Math.max(0, Math.min(Math.max(0, Count - VisibleEntries), EntryScroll + Step)));
        if (Count == 0) {
            Ui.centered(Component.translatable(Prefix + (Tab == 2 ? "no_triggers" : "no_steps")).getString(), X + W / 2, Top + 30, OsLayout.PhosphorDim);
            Ui.centered(Component.translatable(Prefix + (Tab == 2 ? "manual" : "steps.hint")).getString(), X + W / 2, Top + 42, OsLayout.PhosphorFaint);
        }
        for (int Row = 0; Row < VisibleEntries && EntryScroll + Row < Count; Row++) {
            int Index = EntryScroll + Row;
            int RowY = Top + Row * EntryHeight;
            String Summary = Tab == 2 ? describe(Triggers.get(Index)) : describe((Tab == 0 ? OnSteps : OffSteps).get(Index));
            Ui.box(X, RowY, W - 6, EntryHeight - 1, OsLayout.PhosphorFaint, 0xFF071A0D);
            Ui.text(String.valueOf(Index + 1), X + 3, RowY + 2, OsLayout.PhosphorDim);
            Ui.clipped(Summary, X + 13, RowY + 2, W - 66, OsLayout.Phosphor);
            Ui.button(X + W - 54, RowY, 30, EntryHeight - 1, Component.translatable(Prefix + "edit"), true, () -> {
                if (Tab == 2) {
                    editTrigger(Index);
                } else {
                    editStep(Index);
                }
            }, null);
            Ui.glyphButton(X + W - 22, RowY, 15, EntryHeight - 1, OsLayout.GlyphClose, true, () -> {
                if (Tab == 2) {
                    Triggers.remove(Index);
                } else {
                    (Tab == 0 ? OnSteps : OffSteps).remove(Index);
                }
            }, Component.translatable(Prefix + "remove"));
        }
        Ui.scrollbar(X + W - 4, Top, VisibleEntries * EntryHeight, EntryScroll, Count, VisibleEntries);
        boolean Room = Tab == 2 ? Triggers.size() < ScheduledAction.MaxTriggers : (Tab == 0 ? OnSteps : OffSteps).size() < ScheduledAction.MaxSteps;
        Ui.button(X, Y + H - 13, 80, OsLayout.ButtonHeight, Component.translatable(Prefix + (Tab == 2 ? "add_trigger" : "add_step")), Room, () -> {
            if (Tab == 2) {
                editTrigger(-1);
            } else {
                editStep(-1);
            }
        }, null);
        Ui.clipped(Component.translatable(Prefix + "tab.footer." + Tab).getString(), X + 86, Y + H - 10, W - 86, OsLayout.PhosphorFaint);
    }

    private String describe(ActionStep Step) {
        return switch (Step.Type()) {
            case Message -> Component.translatable(Prefix + "step.message", targetName(Step.Target(), Step.TargetName()), Step.Text()).getString();
            case Redstone -> Step.Timed() ? Component.translatable(Prefix + "step.redstone.timed", ComputerText.port(Step.Port()), Step.Seconds()).getString()
                    : Component.translatable(Prefix + "step.redstone.held", ComputerText.port(Step.Port())).getString();
            case Sound -> {
                SoundEntry Entry = sound(Step.SoundId());
                String Name = Entry == null ? Component.translatable(Prefix + "missing").getString() : Entry.Title() + " P" + ComputerText.port(Entry.Port());
                yield Component.translatable(Step.Start() ? Prefix + "step.sound.start" : Prefix + "step.sound.stop", Name).getString();
            }
        };
    }

    private String describe(ActionTrigger Trigger) {
        String Event = Component.translatable(Prefix + "event." + Trigger.When().getSerializedName()).getString();
        return switch (Trigger.Type()) {
            case Time -> Component.translatable(Prefix + "trigger.time", ComputerText.clock(Trigger.Value()), Event).getString();
            case Every -> Component.translatable(Prefix + "trigger.every", Trigger.Value(), Event).getString();
            case After -> Component.translatable(Prefix + "trigger.after", Trigger.Value(), Event).getString();
            case Redstone -> Component.translatable(Prefix + "trigger.redstone", ComputerText.port(Trigger.Port()), Event).getString();
        };
    }

    private String targetName(String Key, String Cached) {
        if (ActionStep.AllChannels.equals(Key)) {
            return Component.translatable(Prefix + "target.all").getString();
        }
        if (ChatChannels.Network.equals(Key)) {
            return ChatService.NetworkName;
        }
        for (Target Each : Channels) {
            if (Each.Key().equals(Key)) {
                return Each.Name();
            }
        }
        return Cached.isEmpty() ? Component.translatable(Prefix + "target.missing").getString() : Cached;
    }

    @Nullable
    private SoundEntry sound(int Id) {
        for (SoundEntry Entry : Sounds) {
            if (Entry.Id() == Id) {
                return Entry;
            }
        }
        return null;
    }

    private void editStep(int Index) {
        StepIndex = Index;
        List<ActionStep> Side = Tab == 0 ? OnSteps : OffSteps;
        ActionStep Step = Index >= 0 && Index < Side.size() ? Side.get(Index) : ActionStep.message("", ActionStep.AllChannels, "");
        StepType = Step.Type();
        StepText.setValue(Step.Text());
        StepTarget = Step.Target();
        StepTargetName = Step.TargetName();
        StepPort.setValue(String.valueOf(Step.Port()));
        StepTimed = Step.Timed();
        StepSeconds = Step.Seconds();
        StepSound = Step.SoundId() >= 0 || Sounds.isEmpty() ? Step.SoundId() : Sounds.getFirst().Id();
        StepStart = Step.Start();
        Showing = View.Step;
        Owner.ui().focus(StepType == ActionStep.Kind.Message ? StepText : null);
    }

    private void confirmStep() {
        ActionStep Built = switch (StepType) {
            case Message -> ActionStep.message(StepText.getValue(), StepTarget, StepTargetName);
            case Redstone -> ActionStep.redstone(WireChannels.clampPort(StepPort.number(0)), StepTimed, StepSeconds);
            case Sound -> ActionStep.sound(StepSound, StepStart);
        };
        List<ActionStep> Side = Tab == 0 ? OnSteps : OffSteps;
        if (StepIndex >= 0 && StepIndex < Side.size()) {
            Side.set(StepIndex, Built);
        } else if (Side.size() < ScheduledAction.MaxSteps) {
            Side.add(Built);
        }
        Showing = View.Action;
        Owner.ui().focus(null);
    }

    private void drawStep(OsUi Ui, int X, int Y, int W, int H) {
        Ui.heading(Component.translatable(Prefix + (Tab == 0 ? "step.on" : "step.off")).getString(), X, Y + 1, W);
        ActionStep.Kind[] Kinds = ActionStep.Kind.values();
        int TabWidth = (W - 4) / Kinds.length;
        for (int Index = 0; Index < Kinds.length; Index++) {
            ActionStep.Kind Kind = Kinds[Index];
            Ui.toggle(X + Index * (TabWidth + 2), Y + 13, TabWidth, OsLayout.ButtonHeight, Component.translatable(Prefix + "type." + Kind.getSerializedName()),
                    StepType == Kind, () -> {
                        StepType = Kind;
                        Owner.ui().focus(Kind == ActionStep.Kind.Message ? StepText : null);
                    }, Component.translatable(Prefix + "type." + Kind.getSerializedName() + ".hint"));
        }
        int LabelX = X;
        int FieldX = X + 46;
        int RowY = Y + 33;
        switch (StepType) {
            case Message -> {
                Ui.text(Component.translatable(Prefix + "text").getString(), LabelX, RowY + 2, OsLayout.PhosphorTitle);
                Ui.field(StepText, FieldX, RowY, W - 46, Component.translatable(Prefix + "text.hint"), null);
                RowY += 18;
                Ui.text(Component.translatable(Prefix + "to").getString(), LabelX, RowY + 3, OsLayout.PhosphorTitle);
                Ui.cycler(FieldX, RowY, 160, OsLayout.ButtonHeight, targetName(StepTarget, StepTargetName), true, () -> cycleTarget(1), () -> cycleTarget(-1),
                        Component.translatable(Prefix + "to.hint"));
            }
            case Redstone -> {
                Ui.text(Component.translatable(Prefix + "port").getString(), LabelX, RowY + 2, OsLayout.PhosphorTitle);
                Ui.field(StepPort, FieldX, RowY, 36, null, Component.translatable("gui.scp_unleashed.computer.redstone.port.hint"));
                RowY += 18;
                Ui.text(Component.translatable(Prefix + "hold").getString(), LabelX, RowY + 3, OsLayout.PhosphorTitle);
                Ui.toggle(FieldX, RowY, 80, OsLayout.ButtonHeight, Component.translatable(Prefix + "hold.timed"), StepTimed, () -> StepTimed = true,
                        Component.translatable(Prefix + "hold.timed.hint"));
                Ui.toggle(FieldX + 84, RowY, 104, OsLayout.ButtonHeight, Component.translatable(Prefix + "hold.toggled"), !StepTimed, () -> StepTimed = false,
                        Component.translatable(Prefix + "hold.toggled.hint"));
                RowY += 18;
                if (StepTimed) {
                    Ui.text(Component.translatable(Prefix + "length").getString(), LabelX, RowY + 3, OsLayout.PhosphorTitle);
                    Ui.stepper(FieldX, RowY, 96, Component.translatable(Prefix + "seconds", StepSeconds).getString(), () -> stepSeconds(-1), () -> stepSeconds(1),
                            Component.translatable(Prefix + "length.hint"), Way -> stepSeconds(-Way));
                }
            }
            case Sound -> {
                Ui.text(Component.translatable(Prefix + "entry").getString(), LabelX, RowY + 3, OsLayout.PhosphorTitle);
                SoundEntry Entry = sound(StepSound);
                String Name = Sounds.isEmpty() ? Component.translatable(Prefix + "no_sounds").getString()
                        : Entry == null ? Component.translatable(Prefix + "missing").getString() : Entry.Title() + "  P" + ComputerText.port(Entry.Port());
                Ui.cycler(FieldX, RowY, 160, OsLayout.ButtonHeight, Name, !Sounds.isEmpty(), () -> cycleSound(1), () -> cycleSound(-1),
                        Component.translatable(Prefix + "entry.hint"));
                RowY += 18;
                Ui.text(Component.translatable(Prefix + "do").getString(), LabelX, RowY + 3, OsLayout.PhosphorTitle);
                Ui.toggle(FieldX, RowY, 60, OsLayout.ButtonHeight, Component.translatable(Prefix + "start"), StepStart, () -> StepStart = true, null);
                Ui.toggle(FieldX + 64, RowY, 60, OsLayout.ButtonHeight, Component.translatable(Prefix + "stop"), !StepStart, () -> StepStart = false, null);
            }
        }
        int ButtonY = Y + H - 13;
        boolean Valid = StepType != ActionStep.Kind.Message || !StepText.getValue().isBlank();
        Ui.button(X, ButtonY, 50, OsLayout.ButtonHeight, Component.translatable(Prefix + "ok"), Valid && (StepType != ActionStep.Kind.Sound || !Sounds.isEmpty()),
                this::confirmStep, null);
        Ui.button(X + 54, ButtonY, 50, OsLayout.ButtonHeight, Component.translatable(Prefix + "cancel"), true, () -> {
            Showing = View.Action;
            Owner.ui().focus(null);
        }, null);
    }

    private void cycleTarget(int Way) {
        List<Target> Options = new ArrayList<>();
        Options.add(new Target(ActionStep.AllChannels, ""));
        Options.add(new Target(ChatChannels.Network, ChatService.NetworkName));
        Options.addAll(Channels);
        int Current = 0;
        for (int Index = 0; Index < Options.size(); Index++) {
            if (Options.get(Index).Key().equals(StepTarget)) {
                Current = Index;
            }
        }
        Target Next = Options.get(Math.floorMod(Current + Way, Options.size()));
        StepTarget = Next.Key();
        StepTargetName = Next.Name();
    }

    private void cycleSound(int Way) {
        if (Sounds.isEmpty()) {
            return;
        }
        int Current = -1;
        for (int Index = 0; Index < Sounds.size(); Index++) {
            if (Sounds.get(Index).Id() == StepSound) {
                Current = Index;
            }
        }
        StepSound = Sounds.get(Math.floorMod(Current + Way, Sounds.size())).Id();
    }

    private void stepSeconds(int Way) {
        int Size = Screen.hasShiftDown() ? 60 : 5;
        StepSeconds = Mth.clamp(StepSeconds + Way * Size, ActionStep.MinSeconds, ActionStep.MaxSeconds);
    }

    private void editTrigger(int Index) {
        TriggerIndex = Index;
        ActionTrigger Trigger = Index >= 0 && Index < Triggers.size() ? Triggers.get(Index) : ActionTrigger.defaults(ActionTrigger.Kind.Time);
        TriggerType = Trigger.Type();
        TriggerEvent = Trigger.When();
        TriggerValue = Trigger.Value();
        TriggerPort.setValue(String.valueOf(Trigger.Port()));
        Showing = View.Trigger;
        Owner.ui().focus(null);
    }

    private void confirmTrigger() {
        ActionTrigger Built = new ActionTrigger(TriggerType, TriggerEvent, TriggerValue, WireChannels.clampPort(TriggerPort.number(0)));
        if (TriggerIndex >= 0 && TriggerIndex < Triggers.size()) {
            Triggers.set(TriggerIndex, Built);
        } else if (Triggers.size() < ScheduledAction.MaxTriggers) {
            Triggers.add(Built);
        }
        Showing = View.Action;
        Owner.ui().focus(null);
    }

    private void drawTrigger(OsUi Ui, int X, int Y, int W, int H) {
        Ui.heading(Component.translatable(Prefix + "trigger.title").getString(), X, Y + 1, W);
        ActionTrigger.Kind[] Kinds = ActionTrigger.Kind.values();
        int TabWidth = (W - 6) / Kinds.length;
        for (int Index = 0; Index < Kinds.length; Index++) {
            ActionTrigger.Kind Kind = Kinds[Index];
            Ui.toggle(X + Index * (TabWidth + 2), Y + 13, TabWidth, OsLayout.ButtonHeight, Component.translatable(Prefix + "kind." + Kind.getSerializedName()),
                    TriggerType == Kind, () -> pickTrigger(Kind), Component.translatable(Prefix + "kind." + Kind.getSerializedName() + ".hint"));
        }
        int LabelX = X;
        int FieldX = X + 46;
        int RowY = Y + 33;
        Ui.text(Component.translatable(Prefix + "then").getString(), LabelX, RowY + 3, OsLayout.PhosphorTitle);
        List<ActionTrigger.Event> Events = new ArrayList<>(List.of(ActionTrigger.Event.Activate, ActionTrigger.Event.Deactivate, ActionTrigger.Event.Toggle));
        if (TriggerType == ActionTrigger.Kind.Redstone) {
            Events.add(ActionTrigger.Event.Follow);
        }
        int EventX = X + 30;
        int EventWidth = (W - 30 - (Events.size() - 1) * 2) / Events.size();
        for (int Index = 0; Index < Events.size(); Index++) {
            ActionTrigger.Event Event = Events.get(Index);
            Ui.toggle(EventX + Index * (EventWidth + 2), RowY, EventWidth, OsLayout.ButtonHeight, Component.translatable(Prefix + "event." + Event.getSerializedName()),
                    TriggerEvent == Event, () -> TriggerEvent = Event, Component.translatable(Prefix + "event." + Event.getSerializedName() + ".hint"));
        }
        RowY += 19;
        switch (TriggerType) {
            case Time -> {
                Ui.text(Component.translatable(Prefix + "at").getString(), LabelX, RowY + 3, OsLayout.PhosphorTitle);
                Ui.stepper(FieldX, RowY, 70, Component.translatable(Prefix + "hour", TriggerValue / 60).getString(), () -> shiftTime(-60), () -> shiftTime(60),
                        null, Way -> shiftTime(-Way * 60));
                Ui.stepper(FieldX + 74, RowY, 70, Component.translatable(Prefix + "minute", TriggerValue % 60).getString(), () -> shiftTime(-step(1, 10)),
                        () -> shiftTime(step(1, 10)), null, Way -> shiftTime(-Way * step(1, 10)));
                Ui.text(ComputerText.clock(TriggerValue), FieldX + 152, RowY + 3, OsLayout.PhosphorHot);
            }
            case Every -> {
                Ui.text(Component.translatable(Prefix + "every").getString(), LabelX, RowY + 3, OsLayout.PhosphorTitle);
                Ui.stepper(FieldX, RowY, 100, Component.translatable(Prefix + "minutes", TriggerValue).getString(), () -> shiftValue(-step(1, 10)),
                        () -> shiftValue(step(1, 10)), null, Way -> shiftValue(-Way * step(1, 10)));
            }
            case After -> {
                Ui.text(Component.translatable(Prefix + "after").getString(), LabelX, RowY + 3, OsLayout.PhosphorTitle);
                Ui.stepper(FieldX, RowY, 100, Component.translatable(Prefix + "seconds", TriggerValue).getString(), () -> shiftValue(-step(5, 60)),
                        () -> shiftValue(step(5, 60)), null, Way -> shiftValue(-Way * step(5, 60)));
            }
            case Redstone -> {
                Ui.text(Component.translatable(Prefix + "port").getString(), LabelX, RowY + 2, OsLayout.PhosphorTitle);
                Ui.field(TriggerPort, FieldX, RowY, 36, null, Component.translatable("gui.scp_unleashed.computer.redstone.port.hint"));
            }
        }
        RowY += 20;
        List<int[]> Lines = Ui.wrap(Component.translatable(Prefix + "kind." + TriggerType.getSerializedName() + ".explain").getString(), W - 4);
        String Explain = Component.translatable(Prefix + "kind." + TriggerType.getSerializedName() + ".explain").getString();
        for (int Index = 0; Index < Lines.size() && Index < 3; Index++) {
            Ui.text(Explain.substring(Lines.get(Index)[0], Lines.get(Index)[1]), X + 2, RowY + Index * 9, OsLayout.PhosphorDim);
        }
        int ButtonY = Y + H - 13;
        Ui.button(X, ButtonY, 50, OsLayout.ButtonHeight, Component.translatable(Prefix + "ok"), true, this::confirmTrigger, null);
        Ui.button(X + 54, ButtonY, 50, OsLayout.ButtonHeight, Component.translatable(Prefix + "cancel"), true, () -> {
            Showing = View.Action;
            Owner.ui().focus(null);
        }, null);
    }

    private void pickTrigger(ActionTrigger.Kind Kind) {
        if (TriggerType == Kind) {
            return;
        }
        ActionTrigger Defaults = ActionTrigger.defaults(Kind);
        TriggerType = Kind;
        TriggerValue = Defaults.Value();
        TriggerEvent = Defaults.When();
        if (Kind == ActionTrigger.Kind.Redstone) {
            Owner.ui().focus(TriggerPort);
        }
    }

    private static int step(int Small, int Big) {
        return Screen.hasShiftDown() ? Big : Small;
    }

    private void shiftTime(int Minutes) {
        TriggerValue = Math.floorMod(TriggerValue + Minutes, ComputerText.MinutesPerDay);
    }

    private void shiftValue(int Amount) {
        int Limit = TriggerType == ActionTrigger.Kind.Every ? ActionTrigger.MaxEveryMinutes : ActionTrigger.MaxAfterSeconds;
        TriggerValue = Mth.clamp(TriggerValue + Amount, 1, Limit);
    }

    private enum View {
        Overview,
        Action,
        Step,
        Trigger
    }

    private record ActionRow(ScheduledAction Action, boolean Active, long For, int Holds) {
    }

    private record Target(String Key, String Name) {
    }
}
