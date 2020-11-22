package ng.com.stemcoders.campusspace.net.events;

import java.util.List;

import ng.com.stemcoders.campusspace.net.models.NewsCommentModel;

public class NewsCommentAvailableEvent extends BaseEvent
{
    public final List<NewsCommentModel> newsCommentModels;

    public NewsCommentAvailableEvent(List<NewsCommentModel> newsCommentModels)
    {
        this.newsCommentModels = newsCommentModels;
    }
}
