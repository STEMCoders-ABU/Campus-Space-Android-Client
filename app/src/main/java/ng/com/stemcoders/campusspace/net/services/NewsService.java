package ng.com.stemcoders.campusspace.net.services;

import java.util.List;

import ng.com.stemcoders.campusspace.net.models.NewsCategoryCommentModel;
import ng.com.stemcoders.campusspace.net.models.NewsCommentModel;
import ng.com.stemcoders.campusspace.net.models.NewsModel;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface NewsService
{
    @GET("1.0/news/show")
    Call<List<NewsModel>> getNews(@Query("faculty_id") Integer faculty_id, @Query("department_id") Integer department_id,
                                  @Query("level_id") Integer level_id, @Query("category_id") Integer category_id,
                                  @Query("join") Boolean join);

    @POST("1.0/news/search")
    @FormUrlEncoded
    Call<List<NewsModel>> searchNews(@Field("search") String search, @Query("faculty_id") Integer faculty_id, @Query("department_id") Integer department_id,
                                     @Query("level_id") Integer level_id,
                                     @Query("category_id") Integer category_id, @Query("join") Boolean join);

    @GET("1.0/news/comments/category")
    Call<List<NewsCategoryCommentModel>> getCategoryComments(@Query("department_id") Integer department_id,
                                                             @Query("level_id") Integer level_id,
                                                             @Query("category_id") Integer category_id, @Query("join") Boolean join);

    @GET("1.0/news/comments")
    Call<List<NewsCommentModel>> getNewsComments(@Query("news_id") Integer news_id, @Query("join") Boolean join);

    @POST("1.0/news/comments/category")
    @FormUrlEncoded
    Call<Void> addCategoryComment(@Field("author") String author, @Field("comment") String comment, @Field("category_id") Integer category_id,
                                  @Field("department_id") Integer department_id,
                                  @Field("level_id") Integer level_id);

    @POST("1.0/news/comments")
    @FormUrlEncoded
    Call<Void> addNewsComment(@Field("author") String author, @Field("comment") String comment, @Field("news_id") Integer news_id);
}
























