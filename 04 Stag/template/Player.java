
import java.util.ArrayList;

public class Player extends Entity implements MoveEntity
{
    private ArrayList<Artefact> inventory;
    private Location currentLoc;
    private ArrayList<Health> hp;

    Player(String name, String description, Location current) {
        super(name, description, "player");
        inventory = new ArrayList<Artefact>();
        currentLoc = current;
        hp = new ArrayList<>();
        for(int i = 0; i < 3; i++) {
            hp.add(new Health("health", ""));
        }
    }

    public void changeLocation (Location newLocation)
    {
        currentLoc = newLocation;
    }

    public Location getCurrentLoc ()
    {
        return currentLoc;
    }

    public ArrayList<Artefact> getInv ()
    {
        return inventory;
    }

    public void addEntity (Entity newEntity)
    {
        inventory.add((Artefact) newEntity);
    }

    public void removeEntity(String name)
    {
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.get(i).getId().equals(name)) {
                inventory.remove(i);
            }
        }
    }

    public Entity getHP ()
    {
        return hp.get(0);
    }

    public void drainHP ()
    {
        hp.remove(0);
    }

    public void addHP()
    {
        hp.add(new Health("health", ""));
    }

    public boolean isHPEmpty()
    {
        if(hp.isEmpty()){
            return true;
        }
        return false;
    }

    public int getHealth(){
        return hp.size();
    }
    public void dropAllItems() {
        for (Entity i : inventory) {
            currentLoc.addEntity(i);
        }
        inventory.clear();
    }
}
