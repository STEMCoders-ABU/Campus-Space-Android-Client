package ng.com.stemcoders.campusspace.net.services;

import java.util.List;

import ng.com.stemcoders.campusspace.net.models.ResourceCategoryCommentModel;
import ng.com.stemcoders.campusspace.net.models.ResourceCommentModel;
import ng.com.stemcoders.campusspace.net.models.ResourceModel;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Streaming;

public interface ResourcesService
{
    @GET("1.0/resources/show")
    Call<List<ResourceModel>> getResources(@Query("faculty_id") Integer faculty_id, @Query("department_id") Integer department_id,
                                           @Query("level_id") Integer level_id, @Query("course_id") Integer course_id,
                                           @Query("category_id") Integer category_id, @Query("order_by_downloads") Boolean order_by_downloads,
                                           @Query("join") Boolean join);

    @POST("1.0/resources/search")
    @FormUrlEncoded
    Call<List<ResourceModel>> searchResources(@Field("search") String search, @Query("faculty_id") Integer faculty_id, @Query("department_id") Integer department_id,
                                              @Query("level_id") Integer level_id, @Query("course_id") Integer course_id,
                                              @Query("category_id") Integer category_id, @Query("join") Boolean join);

    @GET("1.0/resources/comments/category")
    Call<List<ResourceCategoryCommentModel>> getCategoryComments(@Query("department_id") Integer department_id,
                                                          @Query("level_id") Integer level_id, @Query("course_id") Integer course_id,
                                                          @Query("category_id") Integer category_id, @Query("join") Boolean join);

    @GET("1.0/resources/comments")
    Call<List<ResourceCommentModel>> getResourceComments(@Query("resource_id") Integer resource_id, @Query("join") Boolean join);

   @POST("1.0/resources/comments/category")
   @FormUrlEncoded
    Call<Void> addCategoryComment(@Field("author") String author, @Field("comment") String comment, @Field("category_id") Integer category_id,
                                  @Field("course_id") Integer course_id, @Field("department_id") Integer department_id,
                                  @Field("level_id") Integer level_id);

    @POST("1.0/resources/comments")
    @FormUrlEncoded
    Call<Void> addResourceComment(@Field("author") String author, @Field("comment") String comment, @Field("resource_id") Integer resource_id);

    @Streaming
    @GET("1.0/resources/download")
    Call<ResponseBody> downloadResource(@Query("resource_id") Integer resource_id);
}


















