package TeamMS.agents;

import TeamMS.utility.Direction;
import TeamMS.utility.PathFinding;
import eis.iilang.*;
import TeamMS.MailService;
import TeamMS.utility.*;


import java.util.*;


public class ExploratoryAgent extends PercievingAgent{
    private int steps=0;

    public ExploratoryAgent(String name, MailService mailbox) {
        super(name, mailbox);
    }

    public ExploratoryAgent(String name) {
        super(name, null);
    }

    @Override
    public Action step() {
        Action action;
        steps += 1;

        processPercepts();
        Map<String, Boolean> valid = calcValidDirections(x,y);
        if(trapped(valid)){
            return actionDispatch.clearAction(x,y);
        }

        // If we are trying to align, we need to hold still for a turn
        if(waitFlag>=1) return null; // hold still so you can align with other agent

        say(submapString(x-6, x+6, y-6, y+6));
        if(steps%10 ==0) say(submapString(20, 80, 20, 80));
        // action = valuableMoveOrClear();
        action = randomProgressMove();

        // action = validMove();
        // if(getName().equals("agentA9")){
        //     say("LAST ACTION INFO "+Arrays.asList(info).toString());
        //     // say("ACTION: "+action.toString());
        //     // if(steps%5 ==0) say(submapString(20, 80, 20, 80));
        //     say(submapString(x-10, x+10, y-10, y+10));
        // } 

        // say("ACTION: "+action.toString());
        // say(path.toString());
        // say(calcDirectionValue().toString());
        // Action act = valuableMove(); // move, if possible, else null
        // if(act == null){
        //     valuableMoveOrClear
        // }

        // clear
        
        
        return action;
    }

}
