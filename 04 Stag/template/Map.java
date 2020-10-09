import com.alexmerz.graphviz.Parser;
import com.alexmerz.graphviz.objects.Edge;
import com.alexmerz.graphviz.objects.Graph;
import com.alexmerz.graphviz.objects.Node;

import java.util.ArrayList;
import java.util.HashMap;

public class Map {                         //creates hashmaps, 1d and 2d arrays from data from the dot file

    private ArrayList<Graph> file;
    private ArrayList<Graph> map;
    private ArrayList<Graph> Locations;
    private Graph connections;

    Map(Parser Dotmap){
        file = Dotmap.getGraphs();
        map = file.get(0).getSubgraphs();
        Locations = map.get(0).getSubgraphs();
        connections = map.get(1);
    }

    //looks through graph and adds locations name to an array in order
    public ArrayList<String> getLocationList ()
    {
        ArrayList<String> LocationList = new ArrayList<>();
        for (int i = 0; i < Locations.size(); i++){
            LocationList.add(Locations.get(i).getNodes(false).get(0).getId().getId());
        }
        return LocationList;
    }

    // traverses graph and makes hashmap where entity name is the key and entity type is the value (eg furniture)
    public HashMap<String, String> getEntityType ()
    {
        HashMap<String, String> entityType = new HashMap<>();
        for(int j = 0; j < Locations.size(); j++) {
            for(int k = 0; k < Locations.get(j).getSubgraphs().size();k++) {
                ArrayList<Graph> locale = Locations.get(j).getSubgraphs();
                putEntityTypeToMap(locale, entityType, k);
            }
        }
        return entityType;
    }

    //put entity name as the key into hashmap and entity type as the value
    private void putEntityTypeToMap (ArrayList<Graph> locale, HashMap<String, String> entityType , int k)
    {
        for(int i = 0; i < locale.get(k).getNodes(false).size(); i++ ) {
            entityType.put(locale.get(k).getNodes(false).get(i).getId().getId(), locale.get(k).getId().getId());
        }
    }

    // traverses graph and makes hashmap where entity name is the key and the location of the entity is the value
    public HashMap<String, String> getEntityLocation()
    {
        HashMap<String, String> EntityLoc = new HashMap<>();
        for(int j = 0;j < Locations.size(); j++) {
            for(int k = 0; k < Locations.get(j).getSubgraphs().size(); k++) {
                ArrayList<Node> nodes = Locations.get(j).getSubgraphs().get(k).getNodes(false);
                putEntiyLocToMap(nodes, EntityLoc, j);
            }
        }
        return EntityLoc;
    }

    //put entity name as the key into hashmap and entity's location as the value
    private void putEntiyLocToMap (ArrayList<Node> nodes, HashMap<String, String> EntityLoc, int j)
    {
        for (int i = 0; i < nodes.size(); i++) {
            EntityLoc.put(nodes.get(i).getId().getId(), Locations.get(j).getNodes(false).get(0).getId().getId());
        }
    }

    // traverses graph and makes hashmap where entity name is the key and description is the value
    public HashMap<String, String> getDescriptions ()
    {
        HashMap<String, String> desc = new HashMap<>();
        for(int j = 0; j < Locations.size(); j++) {
            desc.put(Locations.get(j).getNodes(false).get(0).getId().getId(),Locations.get(j).getNodes(false).get(0).getAttribute("description"));
        }
        for(int i = 0; i < Locations.size(); i++){
            ArrayList<Graph> entities = Locations.get(i).getSubgraphs();
            for(int k = 0; k < entities.size(); k++){
                ArrayList<Node> nodes = entities.get(k).getNodes(false);
                putDescToMap(nodes, desc);
            }
        }
        return desc;
    }

    //put entity name as the key into hashmap and description as the value
    private void putDescToMap (ArrayList<Node> nodes, HashMap<String, String> desc )
    {
        for(int x = 0; x < nodes.size(); x++) {
            desc.put(nodes.get(x).getId().getId(), nodes.get(x).getAttribute("description"));
        }
    }

    // gets the paths from the graph and puts the source in the first column of a 2d array and target in the second column
    public ArrayList<ArrayList<String>> getPaths()
    {
        ArrayList<ArrayList<String>> paths = new ArrayList<>();
        ArrayList<Edge> path = new ArrayList<Edge>();
        path = connections.getEdges();
        for(int i = 0;i < path.size(); i++){
            Node source = path.get(i).getSource().getNode();
            Node target = path.get(i).getTarget().getNode();
            paths.add(new ArrayList<String>());
            paths.get(i).add(source.getId().getId());
            paths.get(i).add(target.getId().getId());
        }
        return paths;
    }
}
