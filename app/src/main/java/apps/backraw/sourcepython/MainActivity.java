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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Android Imports
import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

// Apache Imports
import org.apache.http.Header;
import org.apache.http.cookie.Cookie;

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
import apps.backraw.sourcepython.network.LoginActivity;


// This class converts the forums into a ViewPager and its fragments
public class MainActivity extends AppCompatActivity implements
        ForumFragment.OnFragmentInteractionListener {

    // Store constants
    private final String FORUMS_HOME = "http://forums.sourcepython.com";
    private final String FORUMS_LOGIN = "http://forums.sourcepython.com/login.php";

    // Store the cookie store
    private PersistentCookieStore mCookieStore;

    // UI references
    private ProgressDialog mProgressDialog;
    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Call AppCompatActivity's constructor
        super.onCreate(savedInstanceState);

        // Set the layout
        setContentView(R.layout.activity_main);

        // Get the app's cookie store
        mCookieStore = new PersistentCookieStore(this);

        // Get its actual cookies
        List<Cookie> cookies = mCookieStore.getCookies();

        // Are any saved?
        if (cookies.size() < 1) {
            // If not, start LoginActivity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);

        } else {
            // If yes, go on initialising...

            // We don't need any dialog right now
            mProgressDialog = null;

            // Reference the ViewPager
            mPager = (ViewPager) findViewById(R.id.pager);

            // Create a new ForumsAdapter object
            mPagerAdapter = new ForumsAdapter(getSupportFragmentManager());

            // Create an asynchronous HTTP client
            final AsyncHttpClient client = new AsyncHttpClient();

            // Make it use the cookie store
            client.setCookieStore(mCookieStore);

            // Show the progress dialog 'Checking session...'
            showProgress(true, R.string.progress_checking_session);

            // GET request to http://forums.sourcepython.com
            client.get(FORUMS_HOME, new AsyncHttpResponseHandler() {

                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                    // Parse the response's body
                    Document document = Jsoup.parse(new String(responseBody));

                    // Filter out the forums: Title => (Title => URL) map
                    Elements forumData = document.select("ol#forums").select("li");
                    Map<String, Map<String, String>> forums = new LinkedHashMap<>();

                    // Loop through all forums
                    // (General, User Content and Development, Source.Python Development)
                    for (Element forum : forumData) {

                        // Get the 'a' tag representing the link to the actual forum
                        Element link = forum.select("div > h2 > span > a").first();

                        // Store its title
                        String title = link.text();

                        // Filter out the sub forums (Title => URL) map
                        Map<String, String> subForums = new LinkedHashMap<>();
                        Elements subForumsList = forum.select("ol > li");

                        // Loop through all sub forums of this forum
                        for (Element subForum : subForumsList) {

                            // Dig deep...
                            Elements sub = subForum.select("div > div > div > div > div");

                            // Get the 'a' tag representing the actual sub forum
                            Element subLink = sub.select("h2 > a").first();

                            // TODO: This paragraph explains the forum, show it somewhere...
                            //Element subParagraph = sub.select("p").first();
                            //String subParagraphText = subParagraph.text();

                            // Store its title and URL
                            String subLinkTitle = subLink.text();
                            String subLinkHref = subLink.attr("href");

                            // Map the title to the URL
                            subForums.put(subLinkTitle, subLinkHref);
                        }

                        // Map the title to the sub forum map
                        forums.put(title, subForums);
                    }

                    // Fill the PagerAdapter using the generated map
                    fillAdapter(forums);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                    // If there was an error, dismiss the dialog
                    showProgress(false, R.string.progress_checking_session);
                }
            });
        }
    }

    private void fillAdapter(Map<String, Map<String, String>> forums) {

        // Create a temporary ArrayList object to hold ForumItem objects
        ArrayList<ForumsContent.ForumItem> items;

        // Loop through the map...
        for (String title : forums.keySet()) {

            // Create a new ArrayList for this title
            items = new ArrayList<>();

            // Get the sub forum map
            Map<String, String> forum = forums.get(title);

            // Loop through it...
            for (String subTitle : forum.keySet()) {

                // Add ForumItem objects to the map
                items.add(new ForumsContent.ForumItem(subTitle, forum.get(subTitle)));
            }

            // Map the title to the generated ArrayList
            ForumsContent.ITEMS.put(title, items);
        }

        // Set the PagerAdapter (now, because we have items added to ForumsContent.ITEMS)
        mPager.setAdapter(mPagerAdapter);

        // Dismiss the dialog
        showProgress(false, R.string.progress_checking_session);
    }

    // TODO: need to change this...
    public void showProgress(boolean state, int textId) {
        if (state) {
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }

            mProgressDialog = ProgressDialog.show(
                    this,
                    getString(R.string.progress_please_wait),
                    getString(textId),
                    true
            );
        } else if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Get the clicked item's ID
        int id = item.getItemId();

        // Did we hit the 'Logout' item?
        if (id == R.id.action_logout) {

            // If yes, create an asynchronous HTTP client
            AsyncHttpClient client = new AsyncHttpClient();

            // Make it use our cookie store
            client.setCookieStore(mCookieStore);

            // Create the query string ?do=logout
            RequestParams params = new RequestParams();
            params.put("do", "logout");

            // Show the progress dialog
            showProgress(true, R.string.progress_logging_out);

            // GET request to http://forums.sourcepython.com/login.php
            client.get(FORUMS_LOGIN, params, new AsyncHttpResponseHandler() {

                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                    // If it was successful, clear the cookie store
                    mCookieStore.clear();

                    // Dismiss the progress dialog
                    showProgress(false, R.string.progress_checking_session);

                    // Start the LoginActivity
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                    // If it failed, dismiss the progress dialog
                    showProgress(false, R.string.progress_checking_session);

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

            // Return the title at the position given
            return items[position];
        }
    }
}
