package net.rai.scp_unleashed.computer.net;

public enum ComputerApp {
    Desktop("desktop"),
    Files("files"),
    Chat("chat"),
    Documents("documents"),
    Redstone("redstone"),
    Sounds("sounds"),
    Scheduler("scheduler"),
    Shop("shop");

    private final String Name;

    ComputerApp(String Name) {
        this.Name = Name;
    }

    public String getName() {
        return Name;
    }

    public String getTitleKey() {
        return "gui.scp_unleashed.computer.app." + Name;
    }

    public static ComputerApp byId(int Id) {
        ComputerApp[] Every = values();
        return Every[Math.max(0, Math.min(Every.length - 1, Id))];
    }
}
