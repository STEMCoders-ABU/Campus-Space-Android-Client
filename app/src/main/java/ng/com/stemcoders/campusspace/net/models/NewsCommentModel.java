package ng.com.stemcoders.campusspace.net.models;

public class NewsCommentModel extends BaseModel
{
    private int id;
    private int news_id;

    private String author;
    private String comment;
    private String date_added;

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public int getNews_id()
    {
        return news_id;
    }

    public void setNews_id(int news_id)
    {
        this.news_id = news_id;
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
}
