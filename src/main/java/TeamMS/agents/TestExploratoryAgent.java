package TeamMS.agents;
import TeamMS.MailService;
import java.lang.Process;
import java.lang.ProcessBuilder;
import eis.iilang.*;
import java.util.*;
import java.lang.Math.*;
import massim.protocol.data.*;

import java.util.stream.Stream;
import java.util.stream.Collectors;
// import mof217.MentalMap;
// import vacworld.Direction;

// compile with "javac -cp .:vacworld/:mof217/ mof217/TestMentalMap.java -verbose"
// run with "java -cp . mof217.TestMentalMap"



public class TestExploratoryAgent{

    public static Percept make_obstacle(int x,int y){
        // List<Parameter> params = Arrays.asList(new Numeral []{new Numeral(x),new Numeral(y)});
        x = x+50;
        y = y+50;
        return new Percept("obstacle", new Numeral(x), new Numeral(y));
    }
    public static Percept make_identifier_percept(String name, String arg){
        return new Percept(name, new Identifier(arg));
    }

    public static int [][] initial_grid(int xdim, int ydim, int numObstacles){
        
        return initial_grid(xdim, ydim, numObstacles, 0);
    }


    public static int [][] initial_grid(int xdim, int ydim, int numObstacles, int seed){
        Random r = new Random((long)seed);
        int [][] map = new int [xdim][ydim];
        for(int i=0; i<numObstacles; i++){
            int x = r.nextInt(xdim);
            int y = r.nextInt(ydim);
            map[x][y] = 1;
        }
        return map;
    }

    public static void print(String s){
        System.out.println(s);
    }

    public void testAlignment(){

        // Position A = new Position(20,10); // really Aoa
        // Position B = new Position(31,20);
        // Position Ba = new Position(3,-2); // position of B wrt A (from A's perspective)
        // Position Ab = new Position(-3, 2); // position of A wrt B (from B's perspective)

        Position A = new Position(20,10); // really Aoa
        Position B = new Position(31,20);
        Position Ba = new Position(3,-2); // position of B wrt A (from A's perspective)
        Position Ab = new Position(-3, 2); // position of A wrt B (from B's perspective)


        //otherPos is our position according to other
        // sender, other, global
        // [ agentA9 ]  agentA4 P(-2,1) P(48,51)
        // [ agentA9 ]  CANDIDATES for agentA4 [Thing((2,-1), entity, A)]
        // [ agentA9 ]  	message from agentA4 P(-2,1) P(52,49) P(-2,1) P(-2,1)
        // [ agentA9 ]  	 otherPos=P(-2,1) globalPos=P(48,51) self=P(50,50)otherPosInv=P(2,-1)

        // [ agentA4 ]  agentA9 P(2,-1) P(52,49)
        // [ agentA4 ]  CANDIDATES for agentA9 [Thing((-2,1), entity, A)]
        // [ agentA4 ]  	message from agentA9 P(2,-1) P(48,51) P(2,-1) P(2,-1)
        // [ agentA4 ]  	 otherPos=P(2,-1) globalPos=P(52,49) self=P(50,50)otherPosInv=P(-2,1)
        // [ agentA4 ]  agentA9 P(2,-1) P(52,49)
        // [ agentA4 ]  CANDIDATES for agentA9 [Thing((-2,1), entity, A)]
        // [ agentA4 ]  	message from agentA9 P(2,-1) P(48,51) P(2,-1) P(2,-1)
        // [ agentA4 ]  	 otherPos=P(2,-1) globalPos=P(52,49) self=P(50,50)otherPosInv=P(-2,1)



        Position Aob = B.translate(Ba);
        Position Boa = A.translate(Ab);
        print("A "+A);
        print("B "+B);

        print("Ba "+Ba);
        print("Ab "+Ab);
        print("Aob "+Aob);
        print("Boa "+Boa);
        print(""+Boa.toLocal(Aob));
        print(""+Aob.toLocal(Boa));
        print(""+Aob.toLocal(A)); // this one is 14,8
        print(""+A.toLocal(Aob));
        print(""+Boa.toLocal(B));
        print(""+B.toLocal(Boa)); //this one is 14,8


    }

    public static void main(String[] args) {




        // // lastAction(move), lastActionResult(success), lastActionParams([s])

        List<Percept> s = Stream.of(make_obstacle(0,0), make_obstacle(1,1), make_obstacle(3,0), 
                                    make_obstacle(5,1), make_obstacle(5,2), 
                                    make_identifier_percept("lastAction", "move"), 
                                    make_identifier_percept("lastActionResult","failure"),
                                    make_obstacle(5,3)
                                    ).collect(Collectors.toList());
        // // System.out.println(s);
        // int [][] map = initial_grid(10,10, 5, 0);

        MailService mailService = new MailService();
        ExploratoryAgent a1 = new ExploratoryAgent("agentA1", mailService);
        ExploratoryAgent a2 = new ExploratoryAgent("agentA2", mailService);

        // a1.setPercepts(s);
        s.stream().forEach(p->a1.handlePercept(p));
        System.out.println(a1.step());

        // for(int i=0; i<10; i++){
        //     System.out.println(a1.randomBiasedMove(1,1,3,1));
        // }
    }
}