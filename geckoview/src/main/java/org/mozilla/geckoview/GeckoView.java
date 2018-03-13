package org.mozilla.geckoview;

import android.widget.FrameLayout;
import android.content.Context;


public class GeckoView  extends FrameLayout {
    public GeckoView(final Context context) {
        super(context);
    }

    private GeckoSession mSession;

    public void setSession(GeckoSession session) {
	    mSession = session;
    }

    public GeckoSession getSession() {
        return mSession;
    }
}
