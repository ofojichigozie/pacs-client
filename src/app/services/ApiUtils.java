package app.services;

public class ApiUtils {

    public static final String BASE_URL = "http://127.0.0.1:5000/";

    public static WebService getWebService(){
        return RetrofitClient.getClient(BASE_URL).create(WebService.class);
    }
}
