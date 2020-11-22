package ng.com.stemcoders.campusspace.net.models;

public class FacultyModel extends BaseModel
{
    private int id;
    private String faculty;

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getFaculty()
    {
        return faculty;
    }

    public void setFaculty(String faculty)
    {
        this.faculty = faculty;
    }
}
