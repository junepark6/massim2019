package TeamMS.agents;
import TeamMS.MailService;
import java.lang.Process;
import java.lang.ProcessBuilder;

// import mof217.MentalMap;
// import vacworld.Direction;

// compile with "javac -cp .:vacworld/:mof217/ mof217/TestMentalMap.java -verbose"
// run with "java -cp . mof217.TestMentalMap"

public class TestExploratoryAgent{
    public static void main(String[] args) {

        MailService mailService = new MailService();
        ExploratoryAgent a1 = new ExploratoryAgent("agentA1", mailService);
        System.out.println(a1.step());
        // for(int i=0; i<10; i++){
        //     System.out.println(a1.randomBiasedMove(1,1,3,1));
        // }
    }
}