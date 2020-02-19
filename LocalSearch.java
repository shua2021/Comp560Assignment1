import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class LocalSearch {
    private static ArrayList<String> colors = new ArrayList<String>();
    private static double timeToFind;

    public static class stateNode {
        String stateLabel;
        String selectedColor;
        ArrayList<String> availColors;
        ArrayList<stateNode> connectedTo;
//        int numOfConf;


        public stateNode(String label, ArrayList<String> availColors) {
            this.stateLabel = label;
            this.availColors = (ArrayList<String>) availColors.clone();
            this.connectedTo = new ArrayList<stateNode>();
            this.selectedColor = null;
           //  this.numOfConf = 0;
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
     * According to the slides the idea of a local search is to randomly color every state and then
     * change states in order to resolve conflicts
     */

    public static ArrayList<stateNode> randomColor(ArrayList<stateNode> states) {
        for(stateNode s: states) {
            s.availColors = (ArrayList<String>) colors.clone();
            int random = new Random().nextInt(colors.size());
            String selectedColor = colors.get(random);
            s.selectedColor = selectedColor;
            s.availColors.remove(selectedColor);
        }
        return states;
    }

    /*
        Get the number violations on each state by checking how many of the neighboring states have the same selected color
        Find the one that violates the most and change its color ---> change it randomly or find the one that resolves
        the most conflicts
     */

    public static int getNumOfConflicts(stateNode state) {
        int numOfConflicts = 0;
        for(stateNode s : state.connectedTo) {
            if(state.selectedColor.equals(s.selectedColor)) {
                numOfConflicts++;
            }
        }
        return numOfConflicts;
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

    /*
    * The idea of simulated annealing is to be more willing to take a "bad" move at the beginning problem according to some
    * probability and the longer the problem has been running, as measured by the value of temperature, we will
    * be less likely to accept a move that would worsen our overall state
     */
    public static ArrayList<stateNode> simulatedAnnealing (ArrayList<stateNode> states) {
        double Temp = .99;

        while(Temp >= 0.01) {
            //First selected a random state:
            int random = new Random().nextInt(states.size());
            stateNode randomState = states.get(random);
            //Get the number of conflicts on the current state I am looking at and store the current color in case it is better
            String currentSelectedColor = randomState.selectedColor;
            int curConflicts = getNumOfConflicts(randomState);

            //Change the color and see if that resolves any conflicts
            int randomForColor = new Random().nextInt(randomState.availColors.size());
            String randomColor = randomState.availColors.get(randomForColor);
            randomState.selectedColor = randomColor;
            int newConflicts = getNumOfConflicts(randomState);

            //Compare the number of conflicts for each of the two choices: this gives us our delta E from the book
            double dE = curConflicts - newConflicts;
            // System.out.println(Math.exp(dE/Temp));
            //In this case the new random color selected was a better move and we always accept moves that better the
            //Problem space
            if(dE > 0) {
                //Add the color we started with back to the list of the available colors and get rid of the
                //color that I chose randomly above
                randomState.availColors.add(currentSelectedColor);
                randomState.availColors.remove(randomColor);
            } else {
                //Create a random value between 0 and 1, assuming a uniform distribution:
                //p values should be less than dE/temp and 1-p should be greater than dE/temp
                double probOfBadMove = Math.exp(dE/Temp);
                double randomForChecking = Math.random();
                //I am willing to take the "badness" of the move we generated because it is above
                //my probability threshold
                if(randomForChecking > probOfBadMove){
                    // In this case I will still take the random move and do as I did before to just
                    // add what I had before to my new possible colors and take away what I now have
                    //Selected from future choices
                    randomState.availColors.add(currentSelectedColor);
                    randomState.availColors.remove(randomColor);
                } else {
                    //In this case the move we wanted was too bad and we want to keep our original color
                    randomState.selectedColor = currentSelectedColor;
                }
            }

            //I feel as though after changing the color each time I should see if that move solved the
            //Problem space, however when I spoke with Juan, he said that I should let the annealing run its course
            //Since the problem will eventually get to a point at which it won't accept bad move easily, and will
            //more than likely take moves that help resolve the problem once the probability is low enough

            //Slowly decrease the "temperature"
            //Numbers I tried:
            //-.01 ----> No solutions found
            //-.001 ----> No solns
            Temp *= .999;
        }

        // toString(states);
        return states;
    }

    public static boolean searchingFunction(ArrayList<stateNode> states) throws InterruptedException {
        long start = System.nanoTime();

        while(true) {
            //Start looking for a solution, and if we do not find one within the specified search, re-randomize
            //the problem space and start over again
            ArrayList<stateNode> randomizedStates = randomColor(states);
            ArrayList<stateNode> annealedStates = simulatedAnnealing(randomizedStates);
            if(checkSolution(annealedStates)) {
                long finish = System.nanoTime();
                timeToFind = (double) (finish - start) / 1000000000;
                return true;
            }

            long finish = System.nanoTime();
            double timePassed = (double) (finish - start) / 1000000000;

            //Having converted from nano seconds, check to see if we have been searching for a solution for
            //More than a minute and if we have, then exit the while loop
            if(timePassed > 60) {
                break;
            }
        }
        System.out.println("No solutions found");
        return false;
    }

    public static ArrayList<stateNode> simulatedAnnealingNoRestart (ArrayList<stateNode> states) {
        double Temp = .99;

        long start = System.nanoTime();
        //End the search if we're looking for more than a minute
        while(((double) (System.nanoTime() - start) / 1000000000) < 60) {
            //First selected a random state:
            int random = new Random().nextInt(states.size());
            stateNode randomState = states.get(random);
            //Get the number of conflicts on the current state I am looking at and store the current color in case it is better
            String currentSelectedColor = randomState.selectedColor;
            int curConflicts = getNumOfConflicts(randomState);

            //Change the color and see if that resolves any conflicts
            int randomForColor = new Random().nextInt(randomState.availColors.size());
            String randomColor = randomState.availColors.get(randomForColor);
            randomState.selectedColor = randomColor;
            int newConflicts = getNumOfConflicts(randomState);

            //Compare the number of conflicts for each of the two choices: this gives us our delta E from the book
            double dE = curConflicts - newConflicts;
            // System.out.println(Math.exp(dE/Temp));
            //In this case the new random color selected was a better move and we always accept moves that better the
            //Problem space
            if(dE > 0) {
                //Add the color we started with back to the list of the available colors and get rid of the
                //color that I chose randomly above
                randomState.availColors.add(currentSelectedColor);
                randomState.availColors.remove(randomColor);
            } else {
                //Create a random value between 0 and 1, assuming a uniform distribution:
                //p values should be less than dE/temp and 1-p should be greater than dE/temp
                double probOfBadMove = Math.exp(dE/Temp);
                double randomForChecking = Math.random();
                //I am willing to take the "badness" of the move we generated because it is above
                //my probability threshold
                if(randomForChecking > probOfBadMove){
                    // In this case I will still take the random move and do as I did before to just
                    // add what I had before to my new possible colors and take away what I now have
                    //Selected from future choices
                    randomState.availColors.add(currentSelectedColor);
                    randomState.availColors.remove(randomColor);
                } else {
                    //In this case the move we wanted was too bad and we want to keep our original color
                    randomState.selectedColor = currentSelectedColor;
                }
            }

            if(checkSolution(states)) {
                timeToFind = (double) (System.nanoTime() - start) / 1000000000;
                return states;
            }

            Temp *= .999;
        }

        toString(states);
        System.out.println("No solution found");
        return null;
    }


    public static void toString(ArrayList<stateNode> states) {
        for(stateNode s: states) {
            System.out.println(s.stateLabel + ": " + s.selectedColor);
        }
    }

    public static void main(String[] args) throws FileNotFoundException, InterruptedException {
        // ArrayList<stateNode> states = readFile("src/Australia.txt");
        ArrayList<stateNode> states = readFile("src/United States.txt");

//        if(searchingFunction(states)) {
//            toString(states);
//            System.out.println("Time to find solution: " + timeToFind);
//        }

         randomColor(states);
         if(simulatedAnnealingNoRestart(states) != null) {
             toString(states);
             System.out.println("Time to find solution: " + timeToFind);
         }
    }
}
