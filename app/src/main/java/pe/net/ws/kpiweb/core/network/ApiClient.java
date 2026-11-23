package pe.net.ws.kpiweb.core.network;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public final class ApiClient {

    // API de WebSite+ en el servidor.
    public static final String BASE_URL = "https://ws.net.pe/websiteplus-api/api/v1/";

    private static Retrofit retrofit = null;
    public static FaceDeviceApi getFaceDeviceService(){getService();return retrofit.create(FaceDeviceApi.class);}

    public static FaceEnrollmentApi getFaceEnrollmentService() {
        getService();return retrofit.create(FaceEnrollmentApi.class);
    }

    public static ScheduleApi getScheduleService() {
        getService();
        return retrofit.create(ScheduleApi.class);
    }

    public static KpiTemplateApi getKpiTemplateService() {
        getService();
        return retrofit.create(KpiTemplateApi.class);
    }

    public static ApiService getService() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            // Login responses contain tokens; never log request/response bodies.
            logging.setLevel(HttpLoggingInterceptor.Level.NONE);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
