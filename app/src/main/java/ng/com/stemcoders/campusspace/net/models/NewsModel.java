package ng.com.stemcoders.campusspace.net.models;

import java.io.Serializable;

public class NewsModel extends BaseModel
{
    private int id;
    private int category_id;
    private int faculty_id;
    private int department_id;
    private int level_id;

    private String title;
    private String content;
    private String date_added;
    private String category;
    private String faculty;
    private String department;
    private String level;

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public int getCategory_id()
    {
        return category_id;
    }

    public void setCategory_id(int category_id)
    {
        this.category_id = category_id;
    }

    public int getFaculty_id()
    {
        return faculty_id;
    }

    public void setFaculty_id(int faculty_id)
    {
        this.faculty_id = faculty_id;
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

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getDate_added()
    {
        return date_added;
    }

    public void setDate_added(String date_added)
    {
        this.date_added = date_added;
    }

    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    public String getFaculty()
    {
        return faculty;
    }

    public void setFaculty(String faculty)
    {
        this.faculty = faculty;
    }

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
}
