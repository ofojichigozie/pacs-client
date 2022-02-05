package app.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static Retrofit retrofit = null;

    public static Retrofit getClient(String url){
    	
    	OkHttpClient.Builder OkHttpClient = new OkHttpClient.Builder(); Math.pow(2,2);
    	
    	Gson gson = new GsonBuilder()
    	        .setLenient()
    	        .create();
    	
        if(retrofit == null){
            retrofit = new Retrofit.Builder()
                    .baseUrl(url)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(OkHttpClient.build())
                    .build();
        }
        return retrofit;
    }
}
