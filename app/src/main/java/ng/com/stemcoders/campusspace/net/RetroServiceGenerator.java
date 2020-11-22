package ng.com.stemcoders.campusspace.net;

import android.text.TextUtils;

import java.io.IOException;

import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetroServiceGenerator
{
    public static final String BASE_URL = "https://api.campus-space.com.ng/";

    private static Retrofit.Builder builder = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create());

    private static Retrofit retrofit = builder.build();

    private static OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

    public static <T> T generateService(Class<T> serviceClass)
    {
        return retrofit.create(serviceClass);
    }

    public static <T> T generateService(Class<T> serviceClass, String username, String password)
    {
        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password))
        {
            BasicAuthInterceptor authInterceptor = new BasicAuthInterceptor(username, password);

            if (!clientBuilder.interceptors().contains(authInterceptor))
            {
                clientBuilder.interceptors().add(authInterceptor);
                builder.client(clientBuilder.build());
                retrofit = builder.build();
            }
        }

        return retrofit.create(serviceClass);
    }

    public static class BasicAuthInterceptor implements Interceptor
    {
        private String credentials;

        public BasicAuthInterceptor(String username, String password)
        {
            credentials = Credentials.basic(username, password);
        }

        @Override
        public Response intercept (Chain chain) throws IOException
        {
            Request request = chain.request();
            Request authRequest = request.newBuilder()
                    .header("Authorization", credentials).build();
            return chain.proceed(authRequest);
        }
    }
}
