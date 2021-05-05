package TeamMS.agents;

/**
 * An agent that contains different role-specific agents 
 * (each of which handles the logic for its specific role).
 * A MultiAgent recognizes when to switch between roles, 
 * and ensures each of its role-agents has the data require to make correct decisions .
 * This is not extremely efficient because we might end up with multiple mental maps mantained by a
 * single MultiAgent, but it seems like the easiest way to structure things for now.
 * (I'm hoping this will make it easier to test the behavior of each specific role.)
 * All role agents, as well as MultiAgent should extend BasicAgent, 
 * so we can put any common functionality in BasicAgent.
 */
public class MultiAgent extends BasicAgent {
    
    private Agent currentRole;
    private Agent exploratoryRole;
    private Agent finderRole;
    private Agent builderRole;
    
    public MultiAgent(String name, MailService mailbox) {
        super(name, mailbox);
        currentRole = ExploratoryAgent();
    }

    private Action chooseAction(){
        Collection<Percept> percepts = getPercepts();
        // check for obstacles to n, s, e, w, and pass 0 to appropriate arg in 
        // randomBiasedMove to prevent a particular direction 
        System.out.println("Percepts:"+percepts);
        return randomMove();
    }

    // @Override
    // public Action step() {
    //     Action act = currentRole.step();
    //     return act;
    // }


}