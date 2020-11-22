package ng.com.stemcoders.campusspace.net.services;

import java.util.List;

import ng.com.stemcoders.campusspace.net.models.CourseModel;
import ng.com.stemcoders.campusspace.net.models.ModeratorModel;
import ng.com.stemcoders.campusspace.net.models.NewsModel;
import ng.com.stemcoders.campusspace.net.models.ResourceModel;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface ModeratorService
{
    @GET("1.0/moderator/show")
    Call<ModeratorModel> getModerator(@Query("join") Boolean join);

    @POST("1.0/moderator/update")
    @FormUrlEncoded
    Call<ModeratorModel> updateModerator(@Field("email") String email, @Field("password") String password, @Field("full_name") String fullName,
                               @Field("gender") String gender, @Field("phone") String phone, @Query("join") Boolean join);

    @GET("1.0/moderator/courses")
    Call<List<CourseModel>> getModeratorCourses(@Query("join") Boolean join);

    @Multipart
    @POST("1.0/moderator/resource")
    Call<Void> addResource(@Part MultipartBody.Part file, @Part("title") RequestBody title, @Part("description") RequestBody description,
                              @Part("category_id") RequestBody categoryId, @Part("course_id") RequestBody courseId);

    @POST("1.0/moderator/news_item")
    @FormUrlEncoded
    Call<Void> addNews(@Field("title") String title, @Field("content") String content, @Field("category_id") Integer categoryId);

    @GET("1.0/moderator/resources")
    Call<List<ResourceModel>> getModeratorResources(@Query("course_id") Integer course_id, @Query("category_id") Integer category_id,
                                                    @Query("join") Boolean join);

    @GET("1.0/moderator/news")
    Call<List<NewsModel>> getModeratorNews(@Query("category_id") Integer category_id, @Query("join") Boolean join);

    @DELETE("1.0/moderator/resource")
    Call<Void> removeResource(@Query("resource_id") Integer resourceId);

    @DELETE("1.0/moderator/news_item")
    Call<Void> removeNews(@Query("news_id") Integer newsId);

    @GET("1.0/moderator/resource")
    Call<ResourceModel> getModeratorResource(@Query("resource_id") Integer resourceId, @Query("join") Boolean join);

    @GET("1.0/moderator/news_item")
    Call<NewsModel> getModeratorNewsItem(@Query("news_id") Integer newsId, @Query("join") Boolean join);

    @POST("1.0/moderator/resource/update")
    @FormUrlEncoded
    Call<Void> updateResource(@Query("resource_id") Integer resourceId, @Field("title") String title, @Field("description") String description,
                              @Field("course_id") Integer courseId);

    @POST("1.0/moderator/news_item/update")
    @FormUrlEncoded
    Call<Void> updateNews(@Query("news_id") Integer newsId, @Field("title") String title, @Field("content") String content,
                          @Field("category_id") Integer categoryId);

    @POST("1.0/moderator/course")
    @FormUrlEncoded
    Call<Void> addCourse(@Field("course_title") String courseTitle, @Field("course_code") String courseCode);
}






























