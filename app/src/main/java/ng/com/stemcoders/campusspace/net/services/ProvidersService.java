package ng.com.stemcoders.campusspace.net.services;

import java.util.List;

import ng.com.stemcoders.campusspace.net.models.CourseModel;
import ng.com.stemcoders.campusspace.net.models.DepartmentModel;
import ng.com.stemcoders.campusspace.net.models.FacultyModel;
import ng.com.stemcoders.campusspace.net.models.LevelModel;

import ng.com.stemcoders.campusspace.net.models.NewsCategoryModel;
import ng.com.stemcoders.campusspace.net.models.ResourceCategoryModel;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ProvidersService
{
    @GET("1.0/provider/faculties")
    Call<List<FacultyModel>> getFaculties();

    @GET("1.0/provider/departments")
    Call<List<DepartmentModel>> getDepartments(@Query("faculty_id") Integer faculty_id);

    @GET("1.0/provider/levels")
    Call<List<LevelModel>> getLevels();

    @GET("1.0/provider/resource_categories")
    Call<List<ResourceCategoryModel>> getResourceCategories();

    @GET("1.0/provider/news_categories")
    Call<List<NewsCategoryModel>> getNewsCategories();

    @GET("1.0/provider/courses")
    Call<List<CourseModel>> getCourses(@Query("department_id") Integer department_id, @Query("level_id") Integer level_id);
}