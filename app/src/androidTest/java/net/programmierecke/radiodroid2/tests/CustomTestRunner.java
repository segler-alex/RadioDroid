package net.programmierecke.radiodroid2.tests;

import android.app.Application;
import android.content.Intent;
import android.os.StrictMode;

import androidx.test.runner.AndroidJUnitRunner;

import net.programmierecke.radiodroid2.RadioDroidApp;
import net.programmierecke.radiodroid2.tests.utils.http.HttpToMockInterceptor;
import net.programmierecke.radiodroid2.tests.utils.http.MockHttpDispatcher;

import java.io.IOException;

import javax.annotation.Nullable;

import okhttp3.mockwebserver.MockWebServer;

public class CustomTestRunner extends AndroidJUnitRunner {

    private MockWebServer mockWebServer;
    private HttpToMockInterceptor httpToMockInterceptor;
    private MockHttpDispatcher mockHttpDispatcher;

    @Override
    public void callApplicationOnCreate(Application app) {
        resetGlobalState(app);
        setupMockWebServer();

        RadioDroidApp radioDroidApp = (RadioDroidApp) app;
        radioDroidApp.setTestsInterceptor(httpToMockInterceptor);

        super.callApplicationOnCreate(app);
    }

    public void setCustomRequestDispatcher(@Nullable MockHttpDispatcher.CustomRequestDispatcher customRequestDispatcher) {
        mockHttpDispatcher.setCustomRequestDispatcher(customRequestDispatcher);
    }

    private void resetGlobalState(Application app) {
        // We may have opened notifications
        Intent closeIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        app.sendBroadcast(closeIntent);
    }

    private void setupMockWebServer() {
        // Changing ThreadPolicy to circumvent NetworkOnMainThreadException is BAD.
        // However we want to initialize local web server here, without additional mating dances.
        StrictMode.ThreadPolicy newPolicy =
                new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.ThreadPolicy oldPolicy = StrictMode.getThreadPolicy();
        StrictMode.setThreadPolicy(newPolicy);

        mockHttpDispatcher = new MockHttpDispatcher();

        mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(mockHttpDispatcher);

        try {
            mockWebServer.start();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        httpToMockInterceptor = new HttpToMockInterceptor(mockWebServer);

        StrictMode.setThreadPolicy(oldPolicy);
    }
}
