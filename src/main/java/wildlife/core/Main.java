package wildlife.core;
import wildlife.view.organism;
import wildlife.view.environment;
import wildlife.view.viewSetup;

public class Main {
    public static void main(String[] args) {

        organism.mobsList.add("");
        organism.mobsList.add("");
        organism.plantsList.add("");
        organism.plantsList.add("");
        environment.materialLists.add("");
        environment.materialLists.add("");

        viewSetup.main(args);
    }
}
