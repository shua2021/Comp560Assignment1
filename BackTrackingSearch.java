import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;


public class BackTrackingSearch {
    private static ArrayList<String> colors = new ArrayList<String>();
    private static int numOfSteps = 0;

    public static class stateNode {
        String stateLabel;
        String selectedColor;
        ArrayList<String> availColors;
        ArrayList<stateNode> connectedTo;


        public stateNode(String label, ArrayList<String> availColors) {
            this.stateLabel = label;
            this.availColors = (ArrayList<String>) availColors.clone();
            this.connectedTo = new ArrayList<stateNode>();
            this.selectedColor = null;
        }
    }

    public static stateNode getStateByName(ArrayList<stateNode> stateList, String stateLabel) {
        for (stateNode nState : stateList) {
            if (nState.stateLabel.equals(stateLabel)) {
                return nState;
            }
        }
        return null;
    }

    public static ArrayList<stateNode> readFile(String pathName) throws FileNotFoundException {
        File file = new File(pathName);
        Scanner input = new Scanner(file);
        // ArrayList<String> colors = new ArrayList<String>();
        ArrayList<stateNode> states = new ArrayList<stateNode>();

        //Text file is broken into three sections: the color portion then state portion then the connection portion
        //Read the colors (while(current line is not a line break) --> once we encounter line break move on
        while (input.hasNext()) {
            String line = input.nextLine();
            if (line.equals("")) {
                break;
            }
            colors.add(line);
        }

        //next read in the states and as they come in create stateNode objects
        while (input.hasNext()) {
            String line = input.nextLine();
            if (line.equals("")) {
                break;
            }

            stateNode stateToAdd = new stateNode(line, colors);
            states.add(stateToAdd);
        }

        //Because we are in the third section there is no need to check for a line break
        while (input.hasNext()) {
            String line = input.nextLine();

            int curChar = 0;
            String stateName = "";
            String state1 = "";
            String state2 = "";

            //Need to separate the two states from each other and then add each one to the others connections list
            while (curChar < line.length()) {
                //Check to see if the current char is a space by concating to a string value
                if ((line.charAt(curChar) + "").equals(" ")) {
                    // Start by taking the string we already have and add it to the graph then reset to read the next state
                    state1 = state1 + stateName;
                    stateName = "";
                    curChar++;
                    continue;
                } else if (curChar == line.length() - 1) {
                    stateName = stateName + line.charAt(curChar);
                    state2 = state2 + stateName;
                    break;
                }
                stateName = stateName + line.charAt(curChar);
                curChar++;
            }

            // Get the two stateNodes that we are adding connections to
            stateNode s1 = getStateByName(states, state1);
            stateNode s2 = getStateByName(states, state2);

            s1.connectedTo.add(s2);
            s2.connectedTo.add(s1);
        }

        return states;
    }

    /**
     * Now I have stored all the states in the stateList now I need to perform the back tracking on it
     * */

    public static boolean checkComplete(ArrayList<stateNode> states) {
        int numAssigned = 0;
        for (stateNode state : states) {
            if (state.selectedColor != null) {
                numAssigned++;
            }
        }
        //The idea is to keep a count of how many states have been legally assigned a color
        //If we find that every state has been assigned a legal value then we are done "coloring" the map
        return numAssigned == states.size();
    }

    public static stateNode getMRV(ArrayList<stateNode> states) {
        int numOfStates = states.size();
        stateNode mostConstrained = null;

        for(int i = 0; i < numOfStates; i++) {
            //At the same time that we add how many colors each state has left, also find the state that is most constrained
            //In the way I've written it, if all values are tied then states.get(0) will be the first state to pick the color of
            //If the state already has a color selected for it, no need to consider for future MRVs
            if(states.get(i).selectedColor != null) {
                continue;
            }

            if(mostConstrained == null || states.get(i).availColors.size() < mostConstrained.availColors.size()  ) {
                mostConstrained = states.get(i);
            }
        }
        return mostConstrained;
    }


    public static ArrayList<stateNode> startSearch(ArrayList<stateNode> states) {
        stateNode mrv = getMRV(states);
        // System.out.println(mrv.stateLabel);
        if(backTrack(mrv, states)) {
            return states;
        }
        System.out.println("No Solution");
        return null;
    }

    public static boolean backTrack(stateNode mrv, ArrayList<stateNode> states) {
        //For each node we want to check for the remaining colors for a possible solution
        int colorN = 0;
        int numOfColors = mrv.availColors.size();
        numOfSteps++;

        //If the node were currently looking at has no remaining possible colors
        //Then that recursive branch fails and we need to pass that failure up the call chain
        if (numOfColors == 0) {
            return false;
        }

        while(colorN < numOfColors) {
            // int random = new Random().nextInt(mrv.availColors.size());
            String selectedColor = mrv.availColors.get(colorN);
            mrv.selectedColor = selectedColor;

            //After assigning the color check to see if performing this action has completed the backtrack
            //This should work because colors are removed from neighboring nodes as they are seen, therefore
            //When selecting the final node's color, any of the remaining choices would have to be legal
            if(checkComplete(states)) {
                return true;
            }

            //Now that we have picked a color for the current node, eliminate that color from the surrounding
            //Nodes and then find the new mrv
            eliminateColor(selectedColor, mrv);
            stateNode nextMRV = getMRV(states);
            if(backTrack(nextMRV, states)){
                return true;
            } else {
                restoreColors(selectedColor, mrv, colorN);
                colorN++;
            }
        }

        return false;
    }

    public static void eliminateColor(String toElim, stateNode state) {
        state.availColors.remove(toElim);
        for (stateNode s: state.connectedTo) {
            s.availColors.remove(toElim);
        }
    }

    public static void restoreColors(String toRestore, stateNode state, int index) {
        state.availColors.add(toRestore);
        state.availColors.add(index, toRestore);
        state.selectedColor = null;
        for(stateNode s: state.connectedTo) {
            s.availColors.add(toRestore);
        }

    }

    public static void toString(ArrayList<stateNode> states) {
        for(stateNode s: states) {
            System.out.println(s.stateLabel + ": " + s.selectedColor);
        }
    }

    public static boolean checkSolution(ArrayList<stateNode> states) {
        if(states == null) { return false;}
        for(stateNode s : states) {
            for(int i = 0; i < s.connectedTo.size(); i++) {
                if(s.connectedTo.get(i).selectedColor.equals(s.selectedColor)) {
                    return false;
                }
            }
        }
        return true;
    }
    public static void main(String[] args) throws FileNotFoundException {
        // ArrayList<stateNode> states = readFile("src/Australia.txt");


        //Need to add a variable to show how many steps it took to get to the solution
        ArrayList<stateNode> states = readFile("src/United States.txt");
        ArrayList<stateNode> solution = startSearch(states);
        toString(solution);
        System.out.println(checkSolution(solution) + " " + numOfSteps);

    }
}




