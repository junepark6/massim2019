package TeamMS;
import eis.exceptions.ActException;
import eis.exceptions.AgentException;
import eis.exceptions.PerceiveException;
import eis.exceptions.RelationException;
import massim.eismassim.EnvironmentInterface;
import eis.EnvironmentListener;
import eis.AgentListener;
import eis.iilang.EnvironmentState;
import eis.iilang.Action;
import eis.iilang.Percept;

import org.json.JSONObject;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.*;

public class Scheduler implements AgentListener, EnvironmentListener{
    @Override
    public void handleStateChange(EnvironmentState environmentState) {
    }

    @Override
    public void handleFreeEntity(String s, Collection<String> collection) {
    }

    @Override
    public void handleDeletedEntity(String s, Collection<String> collection) {
    }

    @Override
    public void handleNewEntity(String s) {
    }

    @Override
    public void handlePercept(String s, Percept percept) {
    }

    private class AgentConf {
        String name;
        String entity;
        String team;
        String className;

        AgentConf(String name, String entity, String team, String className){
            this.name = name;
            this.entity = entity;
            this.team = team;
            this.className = className;
        }
    }

    private EnvironmentInterface eis;
    private List<AgentConf> agentConfigurations = new Vector<>();
    private Map<String, Agent> agents = new HashMap<>();

    Scheduler(String path) {
        String content = null;
        try {
            content = readFile(path+"javaagentsconfig.json");
            JSONObject config = new JSONObject(content);
            JSONObject agents = config.optJSONObject("agents");
            if (agents != null) {
                agents.keySet().forEach(agName -> {
                    JSONObject agConf = agents.getJSONObject(agName);
                    agentConfigurations.add(new AgentConf(agName, agConf.getString("entity"), agConf.getString("team"),
                            agConf.getString("class")));
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void setEnvironment(EnvironmentInterface ei) {
        this.eis = ei;
        for (AgentConf agentConf: agentConfigurations) {
            Agent agent = new Agent(agentConf.className);
            try {
                ei.registerAgent(agent.getName());
            } catch (AgentException e) {
                e.printStackTrace();
            }

            try {
                ei.associateEntity(agent.getName(), agentConf.entity);
                System.out.println("associated agent \"" + agent.getName() + "\" with entity \"" + agentConf.entity + "\"");
            } catch (RelationException e) {
                e.printStackTrace();
            }

            ei.attachAgentListener(agent.getName(), this);
            agents.put(agentConf.name, agent);
        }
    }

    void step() {
        List<Agent> newPerceptAgents = new Vector<>();
        agents.values().forEach(ag -> {
            List<Percept> percepts = new Vector<>();
            try {
                eis.getAllPercepts(ag.getName()).values().forEach(percepts::addAll);
                newPerceptAgents.add(ag);
            } catch (PerceiveException e) {
                System.out.println("No percepts for " + ag.getName());
            }
            ag.setPercepts(percepts);
        });

        // step all agents which have new percepts
        newPerceptAgents.forEach(agent -> {
            Runnable runnable = () -> {
                Action action = agent.step();
                try {
                    eis.performAction(agent.getName(), action);
                } catch (ActException e) {
                    if(action != null)
                        System.out.println("Could not perform action " + action.getName() + " for " + agent.getName());
                }
            };
            Thread thread = new Thread(runnable);
            thread.start();

        });

        if(newPerceptAgents.size() == 0) try {
            Thread.sleep(1000); // wait a bit in case no agents have been executed
        } catch (InterruptedException ignored) {}

    }

    public static String readFile(String path) throws IOException {
        return Files.readString(Paths.get(path));
    }

}
