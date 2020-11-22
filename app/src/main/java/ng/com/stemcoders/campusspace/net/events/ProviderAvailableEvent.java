package ng.com.stemcoders.campusspace.net.events;

import java.util.List;

import ng.com.stemcoders.campusspace.net.models.CourseModel;
import ng.com.stemcoders.campusspace.net.models.DepartmentModel;
import ng.com.stemcoders.campusspace.net.models.FacultyModel;
import ng.com.stemcoders.campusspace.net.models.LevelModel;
import ng.com.stemcoders.campusspace.net.models.NewsCategoryModel;
import ng.com.stemcoders.campusspace.net.models.ResourceCategoryModel;

public class ProviderAvailableEvent extends BaseEvent
{
    private List<FacultyModel> facultyModels = null;
    private List<DepartmentModel> departmentModels = null;
    private List<LevelModel> levelModels = null;
    private List<CourseModel> courseModels = null;
    private List<ResourceCategoryModel> resourceCategoryModels = null;
    private List<NewsCategoryModel> newsCategoryModels = null;

    public List<ResourceCategoryModel> getResourceCategoryModels()
    {
        return resourceCategoryModels;
    }

    public void setResourceCategoryModels(List<ResourceCategoryModel> resourceCategoryModels)
    {
        this.resourceCategoryModels = resourceCategoryModels;
    }

    public List<NewsCategoryModel> getNewsCategoryModels()
    {
        return newsCategoryModels;
    }

    public void setNewsCategoryModels(List<NewsCategoryModel> newsCategoryModels)
    {
        this.newsCategoryModels = newsCategoryModels;
    }

    public List<FacultyModel> getFacultyModels()
    {
        return facultyModels;
    }

    public void setFacultyModels(List<FacultyModel> facultyModels)
    {
        this.facultyModels = facultyModels;
    }

    public List<DepartmentModel> getDepartmentModels()
    {
        return departmentModels;
    }

    public void setDepartmentModels(List<DepartmentModel> departmentModels)
    {
        this.departmentModels = departmentModels;
    }

    public List<LevelModel> getLevelModels()
    {
        return levelModels;
    }

    public void setLevelModels(List<LevelModel> levelModels)
    {
        this.levelModels = levelModels;
    }

    public List<CourseModel> getCourseModels()
    {
        return courseModels;
    }

    public void setCourseModels(List<CourseModel> courseModels)
    {
        this.courseModels = courseModels;
    }
}
