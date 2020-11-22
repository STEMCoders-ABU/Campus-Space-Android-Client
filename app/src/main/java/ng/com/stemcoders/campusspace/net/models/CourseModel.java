package ng.com.stemcoders.campusspace.net.models;

public class CourseModel extends BaseModel
{
    private int id;
    private int department_id;
    private int level_id;

    private String course_title;
    private String course_code;
    private String department;
    private String level;

    public String getDepartment()
    {
        return department;
    }

    public void setDepartment(String department)
    {
        this.department = department;
    }

    public String getLevel()
    {
        return level;
    }

    public void setLevel(String level)
    {
        this.level = level;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public int getDepartment_id()
    {
        return department_id;
    }

    public void setDepartment_id(int department_id)
    {
        this.department_id = department_id;
    }

    public int getLevel_id()
    {
        return level_id;
    }

    public void setLevel_id(int level_id)
    {
        this.level_id = level_id;
    }

    public String getCourse_title()
    {
        return course_title;
    }

    public void setCourse_title(String course_title)
    {
        this.course_title = course_title;
    }

    public String getCourse_code()
    {
        return course_code;
    }

    public void setCourse_code(String course_code)
    {
        this.course_code = course_code;
    }
}
