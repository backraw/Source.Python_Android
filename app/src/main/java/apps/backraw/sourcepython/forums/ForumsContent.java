package apps.backraw.sourcepython.forums;


import java.util.ArrayList;
import java.util.LinkedHashMap;


public class ForumsContent {

    public static LinkedHashMap<String, ArrayList<ForumItem>> ITEMS = new LinkedHashMap<>();

    public static class ForumItem {

        public String title;
        public String description;
        public String href;

        public ForumItem(String title, String description, String href) {

            this.title = title;
            this.description = description;
            this.href = href;
        }

        @Override
        public String toString() {

            return title;
        }
    }
}
