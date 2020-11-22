package ng.com.stemcoders.campusspace.net.events;

import java.util.List;

import ng.com.stemcoders.campusspace.net.models.ResourceModel;

public class ResourcesAvailableEvent extends BaseEvent
{
    public final List<ResourceModel> resourceModels;

    public ResourcesAvailableEvent(List<ResourceModel> resourceModels)
    { this.resourceModels = resourceModels; }
}
