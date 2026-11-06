package net.rai.scp_unleashed.computer.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.rai.scp_unleashed.wire.WireChannels;

public record ActionTrigger(Kind Type, Event When, int Value, int Port) {
    public static final int MaxEveryMinutes = 1440;
    public static final int MaxAfterSeconds = 3600;
    public static final Codec<ActionTrigger> TriggerCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Kind.KindCodec.optionalFieldOf("type", Kind.Time).forGetter(ActionTrigger::Type),
            Event.EventCodec.optionalFieldOf("event", Event.Activate).forGetter(ActionTrigger::When),
            Codec.INT.optionalFieldOf("value", 0).forGetter(ActionTrigger::Value),
            Codec.INT.optionalFieldOf("port", 0).forGetter(ActionTrigger::Port)
    ).apply(Instance, ActionTrigger::new));

    public ActionTrigger {
        Value = switch (Type) {
            case Time -> Math.floorMod(Value, ComputerText.MinutesPerDay);
            case Every -> Mth.clamp(Value, 1, MaxEveryMinutes);
            case After -> Mth.clamp(Value, 1, MaxAfterSeconds);
            case Redstone -> 0;
        };
        Port = WireChannels.clampPort(Port);
        if (When == Event.Follow && Type != Kind.Redstone) {
            When = Event.Toggle;
        }
    }

    public static ActionTrigger defaults(Kind Type) {
        return switch (Type) {
            case Time -> new ActionTrigger(Kind.Time, Event.Activate, 18 * 60, 0);
            case Every -> new ActionTrigger(Kind.Every, Event.Toggle, 10, 0);
            case After -> new ActionTrigger(Kind.After, Event.Deactivate, 60, 0);
            case Redstone -> new ActionTrigger(Kind.Redstone, Event.Follow, 0, 0);
        };
    }

    public enum Kind implements StringRepresentable {
        Time("time"),
        Every("every"),
        After("after"),
        Redstone("redstone");

        public static final Codec<Kind> KindCodec = StringRepresentable.fromEnum(Kind::values);

        private final String SerializedName;

        Kind(String SerializedName) {
            this.SerializedName = SerializedName;
        }

        @Override
        public String getSerializedName() {
            return SerializedName;
        }
    }

    public enum Event implements StringRepresentable {
        Activate("activate"),
        Deactivate("deactivate"),
        Toggle("toggle"),
        Follow("follow");

        public static final Codec<Event> EventCodec = StringRepresentable.fromEnum(Event::values);

        private final String SerializedName;

        Event(String SerializedName) {
            this.SerializedName = SerializedName;
        }

        @Override
        public String getSerializedName() {
            return SerializedName;
        }
    }
}
