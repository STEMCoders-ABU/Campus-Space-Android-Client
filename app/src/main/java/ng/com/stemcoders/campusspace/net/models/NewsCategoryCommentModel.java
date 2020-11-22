package ng.com.stemcoders.campusspace.net.models;

public class NewsCategoryCommentModel extends BaseModel
{
    private int id;
    private int category_id;
    private int department_id;
    private int level_id;

    private String author;
    private String comment;
    private String date_added;
    private String department;
    private String level;
    private String category;

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

    public String getAuthor()
    {
        return author;
    }

    public void setAuthor(String author)
    {
        this.author = author;
    }

    public String getComment()
    {
        return comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    public String getDate_added()
    {
        return date_added;
    }

    public void setDate_added(String date_added)
    {
        this.date_added = date_added;
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

    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }
}
