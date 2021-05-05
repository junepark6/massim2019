package TeamMS.agents;

/**
 * An agent that contains agents, each of which performs a different role.
 * It recognizes when to switch between roles, and what data to transfer.
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