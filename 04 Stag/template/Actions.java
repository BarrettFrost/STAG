import java.util.ArrayList;
import java.util.HashMap;

public class Actions {         //used to creates hashmaps from data from the JSON file

    private String allActions;
    private ArrayList<String> actions;

    Actions(String JSON)
    {
        allActions = JSON;
        actions = new ArrayList<>();
        actions = separateActions();
    }

    // puts each action with all the elements in an array to be used as keys for the hashmap
    private ArrayList<String> separateActions()
    {
        int j;
        for(int i = 1; i < allActions.length(); i++){    // i = 1 because first character is a {
            if(allActions.charAt(i) == '{'){
                j = i;
                while(allActions.charAt(j) != '}'){
                    if(allActions.charAt(j+1) == '}'){
                        actions.add(allActions.substring(i,j+1));
                    }
                    j++;
                }
            }
        }
        return actions;
    }

    // all the triggers are put in the first column of the 2d array and the corresponding actions string put in the second column
    public ArrayList<ArrayList<String>> getTriggers()
    {
        int position, j;
        ArrayList<ArrayList<String>> triggers = new ArrayList<>();
        for(String i: actions){
            position = i.indexOf("triggers") + 11;
            j = position;
            getTriggerWords(i, j, triggers);
        }
        return triggers;
    }

     //get triggers words without the quotes
    private void getTriggerWords(String i, int j, ArrayList<ArrayList<String>> triggers)
    {
        int quoteCount = 0, startQuote = 0;
        while(i.charAt(j) != ']'){
            if(i.charAt(j) == '"') {
                quoteCount++;
                if (quoteCount == 1) {
                    startQuote = j;
                }
                if (quoteCount == 2) {
                    quoteCount = 0;
                    addTriggerWord(i, j, triggers, startQuote+1);
                }
            }
            j++;
        }
    }

    //adds trigger words to the 2d array
    private void addTriggerWord(String i, int j, ArrayList<ArrayList<String>> triggers, int startQuote)
    {
        int size = triggers.size();
        triggers.add(new ArrayList<>());
        triggers.get(size).add(i.substring(startQuote, j));
        triggers.get(size).add(i);
    }

    //creates hashmap with the action string as the key and all the entity names for that element as the value
    public HashMap<String, String> getElementMap (String elementName, char contain, int skipChar )
    {
        HashMap<String, String> element = new HashMap<>();
        int j, start;
        for(String i: actions){
            start = i.indexOf(elementName) + skipChar;
            j = start;
            while(i.charAt(j) != contain){
                if(i.charAt(j+1) == contain){
                    element.put(i, i.substring(start, j+1));
                }
                j++;
            }
        }
        return element;
    }

}
