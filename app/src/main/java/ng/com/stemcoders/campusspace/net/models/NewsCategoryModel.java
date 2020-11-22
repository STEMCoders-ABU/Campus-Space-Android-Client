package ng.com.stemcoders.campusspace.net.models;

import java.io.Serializable;

public class NewsCategoryModel extends BaseModel
{
    private int id;
    private String category;

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
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
