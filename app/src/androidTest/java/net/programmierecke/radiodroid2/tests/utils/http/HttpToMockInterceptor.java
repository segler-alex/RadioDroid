package net.programmierecke.radiodroid2.tests.utils.http;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.InetSocketAddress;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockWebServer;

public class HttpToMockInterceptor implements Interceptor {

    private InetSocketAddress address;

    public HttpToMockInterceptor(MockWebServer mockWebServer) {
        address = new InetSocketAddress(mockWebServer.getHostName(), mockWebServer.getPort());
    }

    @NonNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        HttpUrl httpUrl = new HttpUrl.Builder().scheme("http")
                .host(address.getHostName())
                .port(address.getPort())
                .addQueryParameter("url", request.url().toString())
                .build();

        request = request.newBuilder()
                .url(httpUrl)
                .build();

        return chain.proceed(request);
    }
}
