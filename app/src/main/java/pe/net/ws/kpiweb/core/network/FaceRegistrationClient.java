package pe.net.ws.kpiweb.core.network;

import java.util.concurrent.TimeUnit;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.BufferedSink;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Only facial registration: never replay a mutation after a connection failure or redirect. */
public final class FaceRegistrationClient {
    private FaceRegistrationClient() {}
    static OkHttpClient newHttpClient() {
        return new OkHttpClient.Builder().retryOnConnectionFailure(false)
            .followRedirects(false).followSslRedirects(false)
            // OkHttp may otherwise replay HTTP 503 + Retry-After: 0 even with retries disabled.
            .addInterceptor(chain->{
                Request request=chain.request();RequestBody delegate=request.body();
                if(delegate==null)return chain.proceed(request);
                RequestBody oneShot=new RequestBody(){
                    @Override public MediaType contentType(){return delegate.contentType();}
                    @Override public long contentLength() throws IOException{return delegate.contentLength();}
                    @Override public boolean isOneShot(){return true;}
                    @Override public void writeTo(BufferedSink sink) throws IOException{delegate.writeTo(sink);}
                };
                return chain.proceed(request.newBuilder().method(request.method(),oneShot).build());
            })
            .connectTimeout(15,TimeUnit.SECONDS).readTimeout(15,TimeUnit.SECONDS).build();
    }
    private static final class Holder {
        private static final FaceRegistrationApi API=createService(ApiClient.BASE_URL,java.util.concurrent.Executors.newSingleThreadExecutor());
    }
    static FaceRegistrationApi createService(String baseUrl,java.util.concurrent.Executor callbackExecutor){
        return new Retrofit.Builder().baseUrl(baseUrl)
            .client(newHttpClient()).addConverterFactory(GsonConverterFactory.create())
            .callbackExecutor(callbackExecutor)
            .build().create(FaceRegistrationApi.class);
    }
    public static FaceRegistrationApi getService(){return Holder.API;}
    private static final class DeviceHolder {
        private static final FaceDeviceApi API=new Retrofit.Builder().baseUrl(ApiClient.BASE_URL)
            .client(newHttpClient()).addConverterFactory(GsonConverterFactory.create()).build().create(FaceDeviceApi.class);
    }
    public static FaceDeviceApi getDeviceService(){return DeviceHolder.API;}
}
