package TeamMS;

import java.util.List;
import java.util.Vector;
import java.util.Random;

import eis.iilang.Percept;
import eis.iilang.Action;
import eis.iilang.Identifier;
import java.util.Collection;

public class Agent {
    private List<Percept> percepts = new Vector<>();
    private String name = null;

    Agent(String name) {
        this.name = name;
    }

    public void setPercepts(List<Percept> percepts) {
        this.percepts = percepts;
    }

    List<Percept> getPercepts(){
        return percepts;
    }

    public String getName() {
        return name;
    }

    public Action step() {
        Collection<Percept> percepts = getPercepts();
        int r = getRandomNumberUsingNextInt(0,4);
        String direc = null;

        switch (r) {
            case 0:
                direc = "n";
                break;
            case 1:
                direc = "s";
                break;
            case 2:
                direc = "e";
                break;
            case 3:
                direc = "w";
                break;
        }

        return new Action("move", new Identifier(direc));
    }

    public int getRandomNumberUsingNextInt(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min) + min;
    }

}

