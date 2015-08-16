/*
 * This app can be used to acces the Source.Python forums at
 * http://forums.sourcepython.com
 */

package apps.backraw.sourcepython.network;


// ==================================================
// >> IMPORTS
// ==================================================
// Android Imports
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

// Apache Imports
import org.apache.http.Header;

// Library Imports
//   LoopJ's HTTP Client
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.PersistentCookieStore;
import com.loopj.android.http.RequestParams;
//   JSOUP
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

// Project Imports
import apps.backraw.sourcepython.MainActivity;
import apps.backraw.sourcepython.R;


// This class handles logging in to http://forums.sourcepython.com using LoopJ's HTTP Client.
public class LoginActivity extends Activity {

    // Store constants
    private final String FORUMS_HOME = "http://forums.sourcepython.com";
    private final String FORUMS_LOGIN = "http://forums.sourcepython.com/login.php";

    // UI references
    private EditText mUsernameView;
    private EditText mPasswordView;
    private TextView mErrorView;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Call Activity's constructor
        super.onCreate(savedInstanceState);

        // Set the layout
        setContentView(R.layout.activity_login);

        // We don't need any progress dialog right now
        mProgressDialog = null;

        // Reference UI elements
        mUsernameView = (EditText) findViewById(R.id.username);
        mPasswordView = (EditText) findViewById(R.id.password);
        mErrorView = (TextView) findViewById(R.id.login_failure_view);

        Button mSubmitButton = (Button) findViewById(R.id.submit_button);

        // Connect the button's OnClick listener to the attemptLogin() method below
        mSubmitButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });
    }

    // TODO: need to change this...
    public void showProgress(boolean state) {

        if (state) {
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }

            mProgressDialog = ProgressDialog.show(
                    this,
                    getString(R.string.progress_please_wait),
                    getString(R.string.progress_logging_in),
                    true
            );
        } else if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    public void attemptLogin() {

        // Clear the error text view
        mErrorView.setText("");

        // Remove errors
        mUsernameView.setError(null);
        mPasswordView.setError(null);

        // Get the username and password strings
        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();

        if (TextUtils.isEmpty(username)) {
            // If the username is empty, set an error at the username view
            mUsernameView.setError(getString(R.string.error_field_required));

            // And request its focus
            mUsernameView.requestFocus();

        } else if (TextUtils.isEmpty(password)) {
            // If the password is empty, set an error at the password view
            mPasswordView.setError(getString(R.string.error_field_required));

            // And request its focus
            mPasswordView.requestFocus();

        } else {
            // If no fields are empty, login
            login();
        }
    }

    public void login() {

        // Prepare the query string for the login URL
        // ?do=login&vb_login_username=...&vb_login_password=...
        final RequestParams params = new RequestParams();
        params.put("do", "login");
        params.put("vb_login_username", mUsernameView.getText().toString());
        params.put("vb_login_password", mPasswordView.getText().toString());

        // Get the app's cookie store
        final PersistentCookieStore store = new PersistentCookieStore(this);

        // Clear it
        store.clear();

        // Create a new asynchronous HTTP client
        final AsyncHttpClient client = new AsyncHttpClient();

        // Make it use the cookie store
        client.setCookieStore(store);

        // Show the progress dialog 'Logging in...'
        showProgress(true);

        // Handle a GET request to http://forums.sourcepython.com
        client.get(FORUMS_HOME, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                // If successful, handle a POST request to http://forums.sourcepython.com/login.php
                // using the prepared query string 'params'
                client.post(FORUMS_LOGIN, params, new AsyncHttpResponseHandler() {

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                        // If successful, dismiss the dialog
                        showProgress(false);

                        // Soup the response's body
                        Document document = Jsoup.parse(new String(responseBody));

                        // Are there any links to 'register.php'?
                        if (document.select("a[href=register.php]").size() > 0) {
                            // If yes, set an error text to notify the user
                            mErrorView.setText("Wrong Username/Password?");

                        } else {
                            // If not, start MainActivity - credentials are correct.
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody,
                                          Throwable error) {
                        // If the POST request failed, dismiss the dialog
                        showProgress(false);

                        // And log the error message
                        Log.d("LoginErrorP", error.getMessage());
                    }
                });
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody,
                                  Throwable error) {
                // If the GET request failed, dismiss the dialog
                showProgress(false);

                // Is the status code 0?
                if (statusCode == 0) {

                    // Most probably the user doesn't havy any internet connection...
                    mErrorView.setText(getString(R.string.error_no_internet_connection));
                }
            }
        });
    }
}
