import java.awt.desktop.SystemEventListener;
import java.io.*;
import java.net.*;
import java.util.*;

import com.alexmerz.graphviz.ParseException;
import com.alexmerz.graphviz.Parser;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

class StagServer {
    private static ArrayList<Location> areas;
    private static ArrayList<Player> pc;
    private Player currentPc;
    private static String allActions;

    public static void main(String args[]) throws IOException {
        Parser Dotmap;
        Dotmap = parseDot(args[0]);
        areas = buildMap(Dotmap);
        allActions = getJsonActions(args[1]);
        pc = new ArrayList<>();
        if (args.length != 2) System.out.println("Usage: java StagServer <entity-file> <action-file>");
        else new StagServer(args[0], args[1], 8888);
    }

    public StagServer(String entityFilename, String actionFilename, int portNumber) {
        try {
            ServerSocket ss = new ServerSocket(portNumber);
            System.out.println("Server Listening");
            while (true) acceptNextConnection(ss);
        } catch (IOException ioe) {
            System.err.println(ioe);
        }
    }

    private void acceptNextConnection(ServerSocket ss) {
        try {
            // Next line will block until a connection is received
            Socket socket = ss.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            processNextCommand(in, out);
            out.close();
            in.close();
            socket.close();
        } catch (IOException ioe) {
            System.err.println(ioe);
        }
    }

    private void processNextCommand(BufferedReader in, BufferedWriter out) throws IOException {
        String line = in.readLine(), output, playerName;
        playerName = getPlayerName(line);
        addPlayer(playerName, out);
        assignCurrentPlayer(playerName);
        line = processLine(line);
        out.write("You said... " + line + "\n");
        output = chooseCommand(line);
        out.write(output);
        slainRestartGame(out);
    }

    //process input from user and gets the player name
    String getPlayerName (String line)
    {
        String name = null;
        int endOfName = line.indexOf(':');
        name = line.substring(0, endOfName);
        return name;
    }

    //adds the player to the array by name and at the starting location if player isn't already added
    void addPlayer(String playerName, BufferedWriter out) throws IOException {
        for(Player i: pc){
            if(playerName.equals(i.getId())){
                return;
            }
        }
        pc.add(new Player(playerName, "", areas.get(0)));
        out.write("Welcome to a text based adventure game\n");
    }

    //assigns the player who inputted the command as the current player
    void assignCurrentPlayer (String playerName)
    {
        for(Player i: pc){
            if(playerName.equals(i.getId())){
                currentPc = i;
            }
        }
    }

    void slainRestartGame(BufferedWriter out) throws IOException {  //when health is zero drop items, add 3 health
        if (currentPc.isHPEmpty()) {                                    // and return to start
            currentPc.dropAllItems();
            currentPc.changeLocation(areas.get(0));
            for(int i = 0; i < 3; i++) {
                currentPc.addHP();
            }
            out.write(" You have been slain returning to start ");
        }
    }

    //put the entity triggers in a 2d array and checks whether the user input contains the trigger else choose default command
    // or if trigger chosen is associated with multiple actions use the processCorrectAction function
    private String chooseCommand(String line){
        String output;
        Actions command = new Actions(allActions);
        ArrayList<ArrayList<String>> triggers = command.getTriggers();
        if(checkForDuplicateTriggers(triggers, line)){
            output = processCorrectAction(triggers, line);
            return output;
        }
        else {
            for (int i = 0; i < triggers.size(); i++) {
                if (line.contains(triggers.get(i).get(0))) {
                    output = processActions(triggers.get(i).get(1));
                    return output;
                }
            }
        }
        output = chooseDefaultCommands(line);
        return output;
    }

    //checks if the trigger chosen by the user is associated with multiple actions
    private boolean checkForDuplicateTriggers(ArrayList<ArrayList<String>> triggers , String line){
        int triggerCount = 0;
        for (int i= 0; i < triggers.size(); i++) {
            if (line.contains(triggers.get(i).get(0))) {
                triggerCount++;
            }
        }
        if(triggerCount > 1){
            return true;
        }
        return false;
    }

    //process the correct actions based on whether the subjects are present for the actions if all subjects are present
    // for more than one action then the user will need to specify the subjects of the action in the input.
    private String processCorrectAction(ArrayList<ArrayList<String>> triggers , String line)
    {
        Actions command = new Actions(allActions);
        String output;
        ArrayList<String> keys = new ArrayList<>();
        HashMap<String, String> subjects = command.getElementMap("subjects", ']', 11);
        for (int i= 0; i < triggers.size(); i++) {
            if (line.contains(triggers.get(i).get(0))){
                if(checkSubject(subjects.get(triggers.get(i).get(1)))){
                    keys.add(triggers.get(i).get(1));
                }
            }
        }
        if(keys.size() == 0){
            output = ("Subjects not available");
            return  output;
        }
         output = getCorrectKeyToProcess(keys, subjects, line);
        return output;
    }

    //if one action has all subjects there is no need to specify subjects if multiple sets of subjects are present user needs to specify subjects
    private String getCorrectKeyToProcess( ArrayList<String> keys, HashMap<String, String> subjects, String line)
    {
        String output, key;
        if(keys.size() == 1){
            output = processActions(keys.get(0));
            return output;
        }
        else{
            key = getKeyFromSubjectsInput(subjects,keys,line);
        }
        if(key == null){
            output = "More information needed please specify all the subjects you want to perform the action on.";
        }
        else{
            output = processActions(key);
        }
        return output;
    }

     //returns the key to the action which has all subjects in the line
     String getKeyFromSubjectsInput(HashMap<String, String> subjects, ArrayList<String> keys, String line)
     {
         ArrayList<String> subjectsNoQuotes;
         int subjectsPresent = 0;
         for (String key : keys) {
             subjectsNoQuotes = getIndividualSubjects(subjects.get(key));
              subjectsPresent = checkInputHasSubjects(line, subjectsNoQuotes, subjectsPresent);
             if(subjectsPresent == subjectsNoQuotes.size()){
                 return key;
             }
             subjectsNoQuotes.clear();
             subjectsPresent = 0;
         }
         return null;
     }

     //removes the quotes and commas and puts subjects in an array
     private ArrayList<String> getIndividualSubjects(String subjectList)
     {
        ArrayList <String> subjectNoQuotes = new ArrayList<>();
        int startQuote = 0, quoteCount = 0;
         for(int i = 0; i < subjectList.length(); i++) {
             if (subjectList.charAt(i) == '"') {
                 quoteCount++;
                 if (quoteCount == 1) {
                     startQuote = i;
                 }
                 if (quoteCount == 2) {
                     quoteCount = 0;
                     subjectNoQuotes.add(subjectList.substring(startQuote+1, i));
                 }
             }
         }
         return subjectNoQuotes;
     }

     //checks whether the line contains all the subjects
     private int checkInputHasSubjects(String line, ArrayList<String> subjectNoQuotes, int subjectsPresent)
     {
         for(String name :subjectNoQuotes){
             if(line.contains(name)){
                 subjectsPresent++;
             }
         }
         return subjectsPresent;
     }

    // if input from the user contains a default command the associated function is called
    private String chooseDefaultCommands(String line) {
        String output;
        if (line.contains("look")) {
            output = processLook();
        } else if (line.contains("inv") || line.contains("inventory")) {
            output = processInv();
        } else if (line.contains("goto")) {
            output = processGoto(line);
        } else if (line.contains("get")) {
            output = processGet(line);
        } else if (line.contains("drop")) {
            output = processDrop(line);
        } else if (line.contains("health")) {
            output = processHealth();
        } else {
            output = "I can't do that";
        }
        return output;
    }

    // parses the JSON file and returns the JSON file as a string
    private static String getJsonActions(String fileName) throws IOException {
        JSONParser parser = new JSONParser();
        File JSONFile = new File(fileName);
        String JsonString = null;
        try {
            FileReader in = new FileReader(JSONFile);
            Object JSON = parser.parse(in);
            JSONValue actions = new JSONValue();
            JsonString = actions.toJSONString(JSON);
            return JsonString;
        } catch (org.json.simple.parser.ParseException e) {
            System.out.println("JSON parse failed");
            System.exit(0);
        } catch (FileNotFoundException e) {
            System.out.println("JSON file not found");
            System.exit(0);
        }
        return JsonString;
    }

    //gets consume and produce string based on the key to the corresponding action then checks for the presence of all the
    //subjects if true the ConsumeProduceAction function will execute else return
    private String processActions(String key){
        Actions command = new Actions(allActions);
        HashMap<String, String> subjects = command.getElementMap("subjects", ']', 11);
        String output = null;
        for (String i : subjects.keySet()) {
            if (key.contains(i)) {
                if (!checkSubject(subjects.get(i))) {
                    return ("Subjects not available");
                }
            }
        }
        ConsumeProduceAction(command, key);
        output = NarrateAction(command, key);
        return output;
    }

    //gets consume and produce string based on the key to the corresponding action executes corresponding functions
    private void ConsumeProduceAction(Actions command, String key) {
        HashMap<String, String> consumed = command.getElementMap("consumed", ']',11);
        for (String i : consumed.keySet()) {
            if (key.contains(i)) {
                consumeEntity(consumed.get(i));
            }
        }
        HashMap<String, String> produced = command.getElementMap("produced", ']',11);
        for (String i : produced.keySet()) {
            if (key.contains(i)) {
                produceEntity(produced.get(i));
            }
        }
    }

    //gets narration string based on the key to the corresponding action and returns the narration
    private String NarrateAction(Actions command, String key){
        HashMap<String, String> narration = command.getElementMap("narration", '\"', 12);
        String output = "";
        for (String i : narration.keySet()) {
            if (key.contains(i)) {
                output = narration.get(i);
            }
        }
        return output;
    }

    // check whether produce is a location then searches unplaced location and or health for the entity
    private void produceEntity(String produceNames) {
        Location unplaced = null;
        Entity produce = null;
        for (int i = 0; i < areas.size(); i++) {
            if (areas.get(i).getId().equals("unplaced")) {
                unplaced = areas.get(i);
            }
            if (produceNames.contains(areas.get(i).getId())) {
                produce = areas.get(i);
            }
        }
        for (int j = 0; j < unplaced.getEntities().size(); j++) {
            if (produceNames.contains(unplaced.getEntities().get(j).getId())) {
                produce = unplaced.getEntities().get(j);
                unplaced.removeEntity(produce.getId());
            }
        }
        if (produceNames.contains(currentPc.getHP().getId())) {
            produce = currentPc.getHP();
            currentPc.addHP();
        }
        placeProduce(produce);
    }

    // places artifact in inventory, places character and furniture in the location if its a location adds a new path
    private void placeProduce(Entity produce) {
        if (produce.getEntityType().equals("artefact")) {
            currentPc.addEntity(produce);
        }
        if (produce.getEntityType().equals("character") || produce.getEntityType().equals("furniture")) {
            currentPc.getCurrentLoc().addEntity(produce);
        }
        if (produce.getEntityType().equals("location")) {
            currentPc.getCurrentLoc().addPath(produce.getId());
        }
    }

    // remove the entity if its in inv, current location or health bar
    private void consumeEntity(String consumeNames) {
        String consumedEnt;
        ArrayList<Entity> entities = currentPc.getCurrentLoc().getEntities();
        for (int i = 0; i < entities.size(); i++) {
            if (consumeNames.contains(entities.get(i).getId())) {
                consumedEnt = entities.get(i).getId();
                currentPc.getCurrentLoc().removeEntity(consumedEnt);
            }
        }
        if (consumeNames.contains(currentPc.getHP().getId())) {
            currentPc.drainHP();
        }
        ArrayList<Artefact> inv = currentPc.getInv();
        for (int i = 0; i < inv.size(); i++) {
            if (consumeNames.contains(inv.get(i).getId())) {
                 consumedEnt = inv.get(i).getId();
                currentPc.removeEntity(consumedEnt);
            }
        }
    }

    //check whether all the subjects are present in the location or inventory or health bar
    private boolean checkSubject(String subjectNames) {
        int subjectCount, subjectPresent = 0;
        ArrayList<Entity> entities = currentPc.getCurrentLoc().getEntities();
        for (int i = 0; i < entities.size(); i++) {
            if (subjectNames.contains(entities.get(i).getId())) {
                subjectPresent++;
            }
        }
        ArrayList<Artefact> inv = currentPc.getInv();
        for (int i = 0; i < inv.size(); i++) {
            if (subjectNames.contains(inv.get(i).getId())) {
                subjectPresent++;
            }
        }
        if (subjectNames.contains(currentPc.getHP().getId())) {
            subjectPresent++;
        }
        subjectCount = countCommas(subjectNames) +1;

        if (subjectPresent == subjectCount) {
            return true;
        }
        return false;
    }

    // count the number of comma in the string to find of the number of subjects in the string
    private int countCommas (String entityNames)
    {
        int comma = 0;
        for (int i = 0; i < entityNames.length(); i++) {
            if (entityNames.charAt(i) == ',') {
                comma++;
            }
        }
        return comma;
    }

    //String with the amount of health player has
    private String processHealth() {
        return  ("I have " + currentPc.getHealth() + "HP");
    }

    //checks whether inventory is empty and checks whether the specified artefacts are in the inventory
    // and adds them to the current location
    private String processDrop(String line)  {
        if (currentPc.getInv().isEmpty()) {
            return ("inventory is empty");
        }
        ArrayList<String> items = new ArrayList<>();
        ArrayList<Artefact> inv = currentPc.getInv();
        String output = "";
        for (Artefact i : inv) {
            if (line.contains(i.getId())) {
                output = output.concat(i.getDescription() + " dropped at location ");
                currentPc.getCurrentLoc().addEntity(i);
                items.add(i.getId());
            }
        }
        if (items.isEmpty()) {
            return ("You don't have that Artefact.");
        }
        dropArtFromInv(items);
        return output;
    }

    //remove artifacts from the inventory
    private void dropArtFromInv (ArrayList<String> items) {
        for(String i: items){
            currentPc.removeEntity(i);
        }
    }

    //checks whether specified artefacts is in the location and adds artefacts to inventory
    private String processGet(String line) {
        ArrayList<Entity> entities = currentPc.getCurrentLoc().getEntities();
        ArrayList<String> items = new ArrayList<>();
        String output = "";
        for (Entity i : entities) {
            if (i.getEntityType().equals("artefact") && line.contains(i.getId())) {
                output = output.concat(i.getDescription() + " added to inventory ");
                currentPc.addEntity(i);
                items.add(i.getId());
            }
        }
        if(items.isEmpty()){
            return ("Artefact doesn't exits in this location");
        }
        removeArtFromLoc(items);
        return output;
    }

    //remove artefacts from the current location of the player
    private void removeArtFromLoc(ArrayList<String> items) {
        for(String i: items){
            currentPc.getCurrentLoc().removeEntity(i);
        }
    }

    //checks whether the location specified is a path then changes player location
    private String processGoto (String line){
        String connectLoc = currentPc.getCurrentLoc().getPath(line);
        if(connectLoc == null){
            return"Can't go there";
        }
        String output  = "moving to " + connectLoc + "\n";
        for (Location i : areas) {
            if (i.getId().equals(connectLoc)) {
                currentPc.changeLocation(i);
            }
        }
        return output;
    }

    //loop through player inventory and writes to output the descriptions
    private String processInv () {
        ArrayList<Artefact> inv = currentPc.getInv();
        if(inv.isEmpty()){
            return "nothing in it :(";
        }
        String output = "Your inventory has\n";
        for(Artefact p: inv){
            output = output.concat(p.getDescription() + " ");
         }
        return output;
    }

    //writes to output describing the location and any entities and connecting paths.
    private String processLook() {
        String output;
        Location currentLoc = currentPc.getCurrentLoc();
        output = "You are in " + currentLoc.getDescription() + " with a path leading to ";
        for(int i = 0; i < currentLoc.getPaths().size(); i++){
            output = output.concat(currentLoc.getPaths().get(i));
            if(i != currentLoc.getPaths().size() -1){
                output = output.concat(" and ");
            }
        }
        output = output.concat(". There is ");
        for(int i = 0; i < currentLoc.getEntities().size(); i++) {
            output = output.concat(currentLoc.getEntities().get(i).getDescription());
            if(i != currentLoc.getEntities().size() -1){
                output = output.concat(" and ");
            }
        }
        output = output.concat(".");
        output= output.concat(lookForPlayer());
        return output;
    }

    //looks for another player in the location and writes to output there name.
    private String lookForPlayer(){
        String output = "Also ";
        boolean playerFound = false;
        for(Player i : pc){
            if(i.getCurrentLoc() == currentPc.getCurrentLoc() && i != currentPc){
                output = output.concat(i.getId()+ ", ");
                playerFound = true;
            }
        }
        output = output.concat("is here.");
        if(playerFound){
            return output;
        }
        return "";
    }

    //removes name of player from the line
    private String processLine (String line)
    {
        int i = line.indexOf(':');
        i = i+2;
        String newLine = line.substring(i);
        return newLine;
    }

    //parses the dot file and return Parser object
    private static Parser parseDot(String dot) {
        FileReader in;
        Parser Dotmap = null;
        try {
            File dotfile = new File(dot);
            in = new FileReader(dotfile);
            Dotmap = new Parser();
            Dotmap.parse(in);
            return Dotmap;
        } catch (FileNotFoundException e) {
            System.out.println("Dot file not found");
            System.exit(0);
        } catch (ParseException e) {
            System.out.println("Parse failed");
            System.exit(0);
        }
        return Dotmap;
    }

    // build map with all locations and each entities in the correct locations with ID and descriptions
    private static ArrayList<Location> buildMap(Parser Dotmap)
    {
        Map world = new Map(Dotmap);
        ArrayList<ArrayList<String>> paths = world.getPaths();
        HashMap<String, String> desc = world.getDescriptions();
        HashMap<String, String> entityType = world.getEntityType();
        HashMap<String, Entity> entities = makeEntities(entityType, desc);
        HashMap<String, String> entityLoc = world.getEntityLocation();
        ArrayList<String> LocationList = world.getLocationList();
        ArrayList<Location> areas = makeLocations(entityLoc, entities, desc, LocationList);
        areas = addPaths(areas, paths);
        return areas;
    }

    //uses hash maps with entity name being the key to produce a list of entities objects for each location and a correct description
    // for each location.
    private static ArrayList<Location> makeLocations (HashMap<String, String> entityLoc, HashMap<String, Entity> entities, HashMap<String, String> desc, ArrayList<String> LocationList)
    {
        ArrayList<Entity> entitiesList;
        ArrayList<Location> areas = new ArrayList<>();
        for(String Location: LocationList) {
            entitiesList = new ArrayList<>();
            for (String entity : entityLoc.keySet()) {
                if (Location.equals(entityLoc.get(entity))) {
                    entitiesList.add(entities.get(entity));
                }
            }
            areas.add(new Location(entitiesList, desc.get(Location), Location));
        }
        return areas;
    }

    // adds the paths to the locations object from a 2d array where 1st column is the location and 2nd column is connected location
    private static  ArrayList<Location> addPaths (ArrayList<Location> areas, ArrayList<ArrayList<String>> paths)
    {
        for(int i = 0; i < paths.size(); i++){
            for(int j =0 ; j < areas.size(); j++){
                if(paths.get(i).get(0).equals(areas.get(j).getId())){
                    areas.get(j).addPath(paths.get(i).get(1));
                }
            }
        }
        return areas;
    }

    //loops through entity type hashmap creates corresponding entity object with specified entity type. The description
    //with the same key is placed in the constructor and the key is the entity id also placed i
    private static HashMap<String, Entity> makeEntities(HashMap<String, String> entityType, HashMap<String, String> desc)
    {
        HashMap<String, Entity> entities = new HashMap<>();
        for (String i : entityType.keySet()) {
            if(entityType.get(i).equals("artefacts")){
                entities.put(i, new Artefact(i,desc.get(i)));
            }
            else if(entityType.get(i).equals("furniture")){
                entities.put(i, new Furniture(i,desc.get(i)));
            }
            else if(entityType.get(i).equals("characters")){
                entities.put(i, new Character(i, desc.get(i)));
            }
        }
        return entities;
    }
}
