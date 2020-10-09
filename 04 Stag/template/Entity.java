public abstract class Entity     //used to create objects in the game world
{
    private String id;
    private String desc;
    private String entityType;

     public Entity(String name, String description, String type)
    {
        id = name;
        desc = description;
        entityType = type;
    }

    String getId()
    {
        return id;
    }

    String getDescription()
    {
        return desc;
    }

    String getEntityType(){return entityType;}
}
