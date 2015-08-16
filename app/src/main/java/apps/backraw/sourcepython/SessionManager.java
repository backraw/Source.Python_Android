package apps.backraw.sourcepython;

import android.app.ProgressDialog;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.apache.http.Header;


public class SessionManager {

    public static AsyncHttpClient httpClient = new AsyncHttpClient();

    public static AsyncHttpResponseHandler loginResponse(
            String username, String password, final ProgressDialog dialog) {

        return new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                dialog.dismiss();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                dialog.dismiss();
            }
        };
    }
}
