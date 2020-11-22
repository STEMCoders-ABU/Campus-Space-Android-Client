package ng.com.stemcoders.campusspace.net.models;

public class ResourceCommentModel extends BaseModel
{
    private int id;
    private int resource_id;

    private String author;
    private String comment;
    private String date;

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public int getResource_id()
    {
        return resource_id;
    }

    public void setResource_id(int resource_id)
    {
        this.resource_id = resource_id;
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

    public String getDate()
    {
        return date;
    }

    public void setDate(String date)
    {
        this.date = date;
    }
}
