package ng.com.stemcoders.campusspace.net.events;

import java.util.List;

import ng.com.stemcoders.campusspace.net.models.NewsModel;

public class NewsAvailableEvent extends BaseEvent
{
    public final List<NewsModel> newsModels;

    public NewsAvailableEvent(List<NewsModel> newsModels)
    {
        this.newsModels = newsModels;
    }
}
