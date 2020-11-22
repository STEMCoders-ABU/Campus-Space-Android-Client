package ng.com.stemcoders.campusspace.net.events;

import java.util.List;

import ng.com.stemcoders.campusspace.net.models.ResourceCategoryCommentModel;

public class ResourceCategoryCommentAvailableEvent extends BaseEvent
{
    public final List<ResourceCategoryCommentModel> resourceCategoryCommentModels;

    public ResourceCategoryCommentAvailableEvent(List<ResourceCategoryCommentModel> resourceCategoryCommentModels)
    {
        this.resourceCategoryCommentModels = resourceCategoryCommentModels;
    }
}
