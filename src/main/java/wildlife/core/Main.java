package wildlife.core;
import wildlife.view.Mobs;
import wildlife.view.entry_point;

public class Main {
    public static void main(String[] args) {

        Mobs.MobsList.add(("vip1"));
        Mobs.MobsList.add(("vip2"));
        Mobs.MobsList.add(("vip3"));
        Mobs.MobsList.add(("vip5"));
        Mobs.MobsList.add(("vip6"));

        entry_point.main(args);
    }
}
