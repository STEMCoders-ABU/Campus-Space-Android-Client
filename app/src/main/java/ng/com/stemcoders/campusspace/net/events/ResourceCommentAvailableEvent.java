package ng.com.stemcoders.campusspace.net.events;

import java.util.List;

import ng.com.stemcoders.campusspace.net.models.ResourceCommentModel;

public class ResourceCommentAvailableEvent extends BaseEvent
{
    public final List<ResourceCommentModel> resourceCommentModels;

    public ResourceCommentAvailableEvent(List<ResourceCommentModel> resourceCommentModels)
    {
        this.resourceCommentModels = resourceCommentModels;
    }
}
