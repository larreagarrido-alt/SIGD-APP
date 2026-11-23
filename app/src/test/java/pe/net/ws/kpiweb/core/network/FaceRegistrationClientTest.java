package pe.net.ws.kpiweb.core.network;

import okhttp3.OkHttpClient;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class FaceRegistrationClientTest {
    @Test public void registrationTransportCannotAutomaticallyRetryOrFollowRedirects(){
        OkHttpClient client=FaceRegistrationClient.newHttpClient();
        assertFalse(client.retryOnConnectionFailure());assertFalse(client.followRedirects());assertFalse(client.followSslRedirects());
        assertEquals(1,client.interceptors().size());assertTrue(client.networkInterceptors().isEmpty());
    }
    @Test(timeout=10000) public void http503RetryAfterZeroActuallySendsExactlyOnePost() throws Exception {
        AtomicInteger requests=new AtomicInteger();
        try(ServerSocket server=new ServerSocket(0,2,java.net.InetAddress.getByName("127.0.0.1"))){
            server.setSoTimeout(1200);
            Thread responder=new Thread(()->{
                try {
                    for(int n=0;n<2;n++)try(Socket socket=server.accept()){
                        socket.setSoTimeout(1500);
                        BufferedReader reader=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));
                        String line=reader.readLine();if(line==null)continue;int length=0;
                        while((line=reader.readLine())!=null&&!line.isEmpty())if(line.toLowerCase(java.util.Locale.ROOT).startsWith("content-length:"))length=Integer.parseInt(line.substring(15).trim());
                        for(int i=0;i<length;i++)if(reader.read()<0)break;
                        requests.incrementAndGet();
                        socket.getOutputStream().write("HTTP/1.1 503 Service Unavailable\r\nRetry-After: 0\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                        socket.getOutputStream().flush();
                    }
                }catch(SocketTimeoutException expected){}catch(Exception unexpected){requests.set(-99);}
            });
            responder.start();
            Request request=new Request.Builder().url("http://127.0.0.1:"+server.getLocalPort()+"/auth/face/device/register")
                .post(RequestBody.create(MediaType.parse("application/json"),"{\"fixture\":true}")).build();
            try(okhttp3.Response response=FaceRegistrationClient.newHttpClient().newCall(request).execute()){
                assertEquals(503,response.code());assertTrue(response.request().body().isOneShot());
            }
            responder.join(3000);assertFalse(responder.isAlive());assertEquals(1,requests.get());
        }
    }
    @Test(timeout=10000) public void realRetrofitStreamsAndParsesOnWorkerWithActualHttpStatus() throws Exception {
        java.util.concurrent.ExecutorService worker=java.util.concurrent.Executors.newSingleThreadExecutor(r->new Thread(r,"face-registration-test-worker"));
        java.util.concurrent.CountDownLatch done=new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<String> result=new java.util.concurrent.atomic.AtomicReference<>("NO_CALLBACK");
        try(ServerSocket server=new ServerSocket(0,1,java.net.InetAddress.getByName("127.0.0.1"))){
            server.setSoTimeout(3000);
            Thread responder=new Thread(()->{
                try(Socket socket=server.accept()){
                    socket.setSoTimeout(3000);BufferedReader reader=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));
                    reader.readLine();String line;int length=0;
                    while((line=reader.readLine())!=null&&!line.isEmpty())if(line.toLowerCase(java.util.Locale.ROOT).startsWith("content-length:"))length=Integer.parseInt(line.substring(15).trim());
                    for(int i=0;i<length;i++)if(reader.read()<0)break;
                    byte[] json="{\"status\":\"success\",\"data\":{\"id_clave\":17,\"challenge_id\":8,\"message\":\"WEBSITEPLUS_AUTH:REGISTRO_DISPOSITIVO:fixture\"}}".getBytes(StandardCharsets.UTF_8);
                    socket.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: "+json.length+"\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                    socket.getOutputStream().write(json);socket.getOutputStream().flush();
                }catch(Exception unexpected){result.set("SERVER_FAILURE");done.countDown();}
            });responder.start();
            FaceRegistrationApi api=FaceRegistrationClient.createService("http://127.0.0.1:"+server.getLocalPort()+"/",worker);
            pe.net.ws.kpiweb.features.auth.FaceRegistrationRequest.enqueue(api.register("Bearer synthetic-fixture",java.util.Collections.singletonMap("id_acceso",1)),
                pe.net.ws.kpiweb.features.auth.FaceRegistrationRequest.Stage.REGISTER,new pe.net.ws.kpiweb.features.auth.FaceRegistrationRequest.Listener(){
                    public void success(pe.net.ws.kpiweb.features.auth.models.FaceDeviceResponse value,int http){
                        result.set(value.id_clave==17&&http==200&&Thread.currentThread().getName().equals("face-registration-test-worker")?"OK":"WRONG_RESULT");done.countDown();
                    }
                    public void failure(pe.net.ws.kpiweb.features.auth.FaceRegistrationRequest.Diagnostic diagnostic){result.set("FAILURE");done.countDown();}
                });
            assertTrue(done.await(5,java.util.concurrent.TimeUnit.SECONDS));assertEquals("OK",result.get());responder.join(1000);
        }finally{worker.shutdownNow();}
    }
}
