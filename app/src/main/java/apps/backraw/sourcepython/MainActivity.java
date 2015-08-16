/*
 * This app can be used to acces the Source.Python forums at
 * http://forums.sourcepython.com
 */

package apps.backraw.sourcepython;


// ==================================================
// >> IMPORTS
// ==================================================
// Java Imports
import java.util.ArrayList;

// Android Imports
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

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
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


// Project Imports
import apps.backraw.sourcepython.forums.ForumFragment;
import apps.backraw.sourcepython.forums.ForumsContent;


// This class converts the forums into a ViewPager and its fragments
public class MainActivity extends AppCompatActivity implements
        ForumFragment.OnFragmentInteractionListener {

    // Store constants
    private final String FORUMS_HOME = "http://forums.sourcepython.com";
    private final String FORUMS_LOGIN = "http://forums.sourcepython.com/login.php";

    // Store the cookie store
    private PersistentCookieStore mCookieStore;

    // UI references
    private SpProgressDialog mProgressLoadingForums;
    private SpProgressDialog mProgressLogout;

    private ViewPager mPager;
    private Menu mMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Call AppCompatActivity's constructor
        super.onCreate(savedInstanceState);

        // Set the layout
        setContentView(R.layout.activity_main);

        // Get the app's cookie store
        mCookieStore = new PersistentCookieStore(this);

        // We don't need any dialogs right now
        mProgressLoadingForums = new SpProgressDialog(this, R.string.progress_loading_forums);
        mProgressLogout = new SpProgressDialog(this, R.string.progress_logging_out);

        // Reference the ViewPager
        mPager = (ViewPager) findViewById(R.id.pager);

        // Create an asynchronous HTTP client
        final AsyncHttpClient client = new AsyncHttpClient();

        // Make it use the cookie store
        client.setCookieStore(mCookieStore);

        // Show the progress dialog 'Loading forums...'
        mProgressLoadingForums.show();

        // GET request to http://forums.sourcepython.com
        client.get(FORUMS_HOME, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                // Create a temporary ArrayList object to hold ForumItem objects
                ArrayList<ForumsContent.ForumItem> forumItems;

                // Parse the response's body
                Document document = Jsoup.parse(new String(responseBody));

                // Filter out the forums: Title => (Title => URL) map
                Elements forums = document.select("ol#forums").select("li");

                // Loop through all forums
                // (General, User Content and Development, Source.Python Development)
                for (Element section : forums) {

                    // Get the 'a' tag representing the link to the actual forum
                    Elements links = section.select("div > h2 > span > a");

                    if (links.size() > 0) {

                        // Create a new ArrayList to hold ForumItem objects
                        forumItems = new ArrayList<>();

                        Element sectionLink = links.first();

                        // Store its title
                        String title = sectionLink.text();

                        // Filter out the forum's sub forums
                        Elements subForums = section.select("ol > li");
                        Elements forumLinks = subForums.select("h2.forumtitle > a");
                        Elements forumDescriptions = subForums.select("p.forumdescription");

                        // Loop through them
                        for (int i = 0; i < forumLinks.size(); i++) {

                            // Get the 'a' tag representing the link to the sub forum
                            Element link = forumLinks.get(i);

                            // And the 'p' tag representing the forum's description
                            Element description = forumDescriptions.get(i);

                            // Add a new ForumItem object to the list
                            forumItems.add(new ForumsContent.ForumItem(
                                    link.text(), description.text(), link.attr("href")
                            ));
                        }

                        // Map the generated list to the forum title
                        ForumsContent.ITEMS.put(title, forumItems);
                    }
                }

                // Set the ViewPager's adapter to view the items
                mPager.setAdapter(new ForumsAdapter(getSupportFragmentManager()));

                // And dismuss the 'Loading forums...' dialog
                mProgressLoadingForums.dismiss();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                // If there was an error, dismiss the dialog
                mProgressLoadingForums.dismiss();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mMenu = menu;

        // Store menu items
        final MenuItem login = menu.getItem(0);
        final MenuItem logout = menu.getItem(1);

        // Handle their state
        login.setEnabled(true);
        logout.setEnabled(false);

        // Have any cookies been saved?
        if (mCookieStore.getCookies().size() > 0) {

            // Store an instance of SharedPreferences
            SharedPreferences preferences = getSharedPreferences("SP_CREDS", MODE_PRIVATE);
            final String username = preferences.getString("username", null);
            final String password = preferences.getString("password", null);

            // Create an asynchronous HTTP client
            AsyncHttpClient client = new AsyncHttpClient();

            // Make it use our cookie store
            client.setCookieStore(mCookieStore);

            client.get(FORUMS_HOME, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    // Soup the response's body
                    Document document = Jsoup.parse(new String(responseBody));

                    // Are there any links to 'register.php'?
                    if (document.select("a[href=register.php]").size() < 1) {

                        // If not, disable the 'Login' item
                        login.setEnabled(false);

                        // And enable the 'Logout' button
                        logout.setEnabled(true);

                        if (username != null) {
                            logout.setTitle(String.format(
                                            "%s: %s",
                                            getString(R.string.action_logout), username)
                            );
                        }
                    } else {

                        // Session expired; do re-login
                        if (username != null && password != null) {

                        }
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                }
            });
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {

        // Get the clicked item's ID
        int id = item.getItemId();

        if (id == R.id.action_login) {

            // If we hit the 'Login' item, start the LoginActivity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);

        } else if (id == R.id.action_logout) {

            // If we hit the 'Logout' item, create the query string ?do=logout
            RequestParams params = new RequestParams();
            params.put("do", "logout");

            // Create an asynchronous HTTP client
            AsyncHttpClient client = new AsyncHttpClient();

            // Make it use our cookie store
            client.setCookieStore(mCookieStore);

            // Show the progress dialog
            mProgressLogout.show();

            // GET request to http://forums.sourcepython.com/login.php
            client.get(FORUMS_LOGIN, params, new AsyncHttpResponseHandler() {

                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                    // If it was successful, clear the cookie store
                    mCookieStore.clear();

                    // Disable 'Logout' and enable 'Login'
                    item.setEnabled(false);
                    item.setTitle(getString(R.string.action_logout));
                    mMenu.getItem(0).setEnabled(true);

                    // Dismiss the progress dialog
                    mProgressLogout.dismiss();
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                    // If it failed, dismiss the progress dialog
                    mProgressLogout.dismiss();

                    // And log the error message
                    Log.d("FAIL", error.getMessage());
                }
            });

            // Return true to tell Android 'the event has been handled'
            return true;
        }

        // If not, do what Android would do
        return super.onOptionsItemSelected(item);
    }

    // Called when the user clicks an item in the forums list
    public void onFragmentInteraction(ForumsContent.ForumItem forumItem) {

        // For now, just log what was clicked
        Log.d("FRAGMENT_TITLE", forumItem.title);
        Log.d("FRAGMENT_HREF", forumItem.href);
    }

    private class ForumsAdapter extends FragmentPagerAdapter {

        public ForumsAdapter(FragmentManager fm) {

            // Call FragmentPagerAdapter's constructor
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            // Return a new ForumFragment by the title
            return ForumFragment.newInstance((String) getPageTitle(position));
        }

        @Override
        public int getCount() {

            // Return the size of ForumsContent.ITEMS
            return ForumsContent.ITEMS.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {

            // Convert ForumsContent.ITEMS to a string array
            String[] items = ForumsContent.ITEMS.keySet().toArray(
                    new String[ForumsContent.ITEMS.size()]
            );

            // Return the title at the given position
            return items[position];
        }
    }
}
