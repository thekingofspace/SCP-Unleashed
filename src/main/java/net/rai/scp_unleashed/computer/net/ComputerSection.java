package net.rai.scp_unleashed.computer.net;

public enum ComputerSection {
    System,
    Files,
    Chat,
    Documents,
    Redstone,
    Sounds,
    Scheduler,
    Shop;

    public static final int All = (1 << values().length) - 1;

    public int bit() {
        return 1 << ordinal();
    }

    public static ComputerSection byId(int Id) {
        ComputerSection[] Every = values();
        return Every[Math.max(0, Math.min(Every.length - 1, Id))];
    }

    public static ComputerSection ofApp(ComputerApp App) {
        return switch (App) {
            case Desktop -> System;
            case Files -> Files;
            case Chat -> Chat;
            case Documents -> Documents;
            case Redstone -> Redstone;
            case Sounds -> Sounds;
            case Scheduler -> Scheduler;
            case Shop -> Shop;
        };
    }
}
