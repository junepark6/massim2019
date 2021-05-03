package massim2019; // package teamms;

import java.util.HashMap;

public class Task{
    private int deadline;
    private int reward;
    HashMap<Location, String> shape;

    public Task(int deadline, int reward, HashMap<Location, String> shape){
        this.deadline = deadline;
        this.reward = reward;
        this.shape = shape;
    }

    /** count number of blocks of each type */
    public HashMap<String, Integer> blockTypes(){
        Collection<String> blocktypeCounts = shape.values();
        HashMap<String, Integer> counts = new HashMap<>();
        for(String st: shape.values()){
            int count = Collections.frequency(blocktypeCounts, st);
            counts.put(st, new Integer(count));
        }
        return blockTypes;
    }

    /** count number of block of type st */
    public int blockTypes(String st){
        Collection<String> blocktypeCounts = shape.values();
        return Collections.frequency(blocktypeCounts, st);
    }
    
    public String toString(){
        String output = "Deadline:" + deadline + " Reward:" + reward;
    }

    // public String toPDDL(){}

    public print_shape(){
        MentalMap m = new MentalMap(this.shape);
        m.print_array();
    }
}


// "name": "task2",
//               "deadline": 188,
//               "reward" : 44,
//               "requirements": [
//                   {
//                      "x": 1,
//                      "y": 1,
//                      "details": "",
//                      "type": "b0"
//                   },
//                   {
//                      "x": 0,
//                      "y": 1,
//                      "details": "",
//                      "type": "b1"
//                   },
//                   {
//                      "x": 0,
//                      "y": 2,
//                      "details": "",
//                      "type": "b1"
//                   }
//                ]
//             },
//          ],