package TeamMS.agents;

import TeamMS.utility.Direction;
import TeamMS.utility.PathFinding;
import eis.iilang.*;
import TeamMS.MailService;
import TeamMS.utility.*;


import java.util.*;


public class ExploratoryAgent extends PercievingAgent{

    public ExploratoryAgent(String name, MailService mailbox) {
        super(name, mailbox);
    }

    public ExploratoryAgent(String name) {
        super(name, null);
    }

    @Override
    public Action step() {
        // processPercepts();
        // say(mapToString());
        // return validMove();
        processPercepts();
        Action action = valuableMoveOrClear();

        if(getName().equals("agentA9")){
            say("LAST ACTTION INFO "+Arrays.asList(info).toString());
            say(submapString(x-10, x+10, y-10, y+10));
            say("ACTION: "+action.toString());
        } 
        // say(calcDirectionValue().toString());
        // Action act = valuableMove(); // move, if possible, else null
        // if(act == null){
        //     valuableMoveOrClear
        // }

        // clear
        
        
        return action;
    }

}
