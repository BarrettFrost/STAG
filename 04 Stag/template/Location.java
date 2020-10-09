
import java.util.ArrayList;

public class Location extends Entity implements MoveEntity{

    private ArrayList <Entity> entities;
    private ArrayList<String> paths;

    public Location(ArrayList<Entity> objects, String description, String Id)
    {
        super(Id, description, "location");
        paths = new ArrayList<String>();
        entities = objects;
    }

    public void removeEntity(String name)
    {
       for(int i = 0; i < entities.size(); i++){
           if(entities.get(i).getId().equals(name)){
               entities.remove(i);
           }
       }
    }

    public ArrayList<Entity> getEntities ()
    {
        return entities;
    }

    public String getPath(String line){
        for(String i: paths){
            if(line.contains(i)){
                return i;
            }
        }
        return null;
    }

    public ArrayList<String> getPaths(){
        return paths;
    }

    public void addEntity(Entity newEntity)
    {
        entities.add(newEntity);
    }

    public void addPath(String route)
    {
        paths.add(route);
    }

}
