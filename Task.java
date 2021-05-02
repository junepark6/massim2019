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
    
    public String toString(){
        String output = "Deadline:" + deadline + " Reward:" + reward;
    }

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