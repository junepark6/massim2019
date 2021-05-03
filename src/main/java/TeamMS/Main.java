package TeamMS;
import java.io.File;
import massim.eismassim.EnvironmentInterface;
import eis.iilang.EnvironmentState;
import eis.exceptions.ManagementException;

public class Main {
    public static void main(String[] args) {
        String configDir = "conf/BasicAgents/";
        Scheduler scheduler = new Scheduler(configDir);
        EnvironmentInterface ei = new EnvironmentInterface(configDir + File.separator + "eismassimconfig.json");

        try {
            ei.start();
        } catch (ManagementException e) {
            e.printStackTrace();
        }
        scheduler.setEnvironment(ei);

        int step = 0;

        while ((ei.getState() == EnvironmentState.RUNNING)) {
            System.out.println("SCHEDULER STEP " + step);
            scheduler.step();
            step++;
        }
    }
}
