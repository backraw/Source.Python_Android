package apps.backraw.sourcepython;


import android.app.ProgressDialog;
import android.content.Context;

public class SpProgressDialog extends ProgressDialog {

    public SpProgressDialog(Context context, int resId) {
        super(context);

        setTitle(R.string.progress_please_wait);
        setMessage(context.getString(resId));
        setIndeterminate(true);
    }
}
