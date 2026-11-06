package net.rai.scp_unleashed.scp085;

public enum Scp085Form {
    Woman("woman", 8.0f),
    Cat("cat", 9.0f),
    Bird("bird", 7.0f),
    Rabbit("rabbit", 10.0f);

    private final String Name;
    private final float Speed;

    Scp085Form(String Name, float Speed) {
        this.Name = Name;
        this.Speed = Speed;
    }

    public float speed() {
        return Speed;
    }

    public String nameKey() {
        return "jade.scp_unleashed.scp085_form." + Name;
    }

    public static Scp085Form byIndex(int Index) {
        Scp085Form[] Values = values();
        return Index >= 0 && Index < Values.length ? Values[Index] : Woman;
    }
}
