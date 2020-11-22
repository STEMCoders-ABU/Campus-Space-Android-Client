package ng.com.stemcoders.campusspace.net.events;

import java.util.List;

import ng.com.stemcoders.campusspace.net.models.NewsCategoryCommentModel;

public class NewsCategoryCommentAvailableEvent extends BaseEvent
{
    public final List<NewsCategoryCommentModel> newsCategoryCommentModels;

    public NewsCategoryCommentAvailableEvent(List<NewsCategoryCommentModel> newsCategoryCommentModels)
    {
        this.newsCategoryCommentModels = newsCategoryCommentModels;
    }
}
