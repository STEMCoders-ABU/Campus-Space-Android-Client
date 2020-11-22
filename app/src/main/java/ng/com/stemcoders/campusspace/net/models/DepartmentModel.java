package ng.com.stemcoders.campusspace.net.models;

public class DepartmentModel extends BaseModel
{
    private int id;
    private int faculty_id;
    private String department;

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public int getFaculty_id()
    {
        return faculty_id;
    }

    public void setFaculty_id(int faculty_id)
    {
        this.faculty_id = faculty_id;
    }

    public String getDepartment()
    {
        return department;
    }

    public void setDepartment(String department)
    {
        this.department = department;
    }
}
