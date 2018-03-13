package org.mozilla.geckoview;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.content.Context;
import android.os.Bundle;

public class GeckoSession {

	public GeckoSession(GeckoSessionSettings settings) {
	}

	public void loadUri(String uri) {
	}

	public void loadUri(Uri uri) {
	}

	public static void preload(final @NonNull Context context) {
		preload(context, /* geckoArgs */ null,
				/* extras */ null, /* multiprocess */ false);
	}

	public static void preload(final @NonNull Context context,
			final @Nullable String[] geckoArgs,
			final @Nullable Bundle extras,
			final boolean multiprocess) {

	}

	private ContentDelegate mContentDelegate;
	public void setContentDelegate(ContentDelegate delegate) {
		mContentDelegate = delegate;
	}
	public ContentDelegate getContentDelegate() {
		return mContentDelegate;
	}
	public interface ContentDelegate {
		void onTitleChange(GeckoSession session, String title);
		void onFocusRequest(GeckoSession session);
		void onCloseRequest(GeckoSession session);
		void onFullScreen(GeckoSession session, boolean fullScreen);
		void onContextMenu(GeckoSession session, int screenX, int screenY, String uri, String elementSrc);
	}

	private ProgressDelegate mProgressDelegate;
	public void setProgressDelegate(ProgressDelegate delegate) {
		mProgressDelegate = delegate;
	}
	public ProgressDelegate getProgressDelegate() {
		return mProgressDelegate;
	}
	public interface ProgressDelegate {
		public class SecurityInformation {
			public static final int SECURITY_MODE_UNKNOWN = 0;
			public static final int SECURITY_MODE_IDENTIFIED = 1;
			public static final int SECURITY_MODE_VERIFIED = 2;
			public static final int CONTENT_UNKNOWN = 0;
			public static final int CONTENT_BLOCKED = 1;
			public static final int CONTENT_LOADED = 2;
			public final boolean isSecure = false;
			public final boolean isException = false;
			public final String origin = null;
			public final String host = null;
			public final String organization = null;
			public final String subjectName = null;
			public final String issuerCommonName = null;
			public final String issuerOrganization = null;
			public final int securityMode = 0;
			public final int mixedModePassive = 0;
			public final int mixedModeActive = 0;
			public final int trackingMode = 0;
		}
		void onPageStart(GeckoSession session, String url);
		void onPageStop(GeckoSession session, boolean success);
		void onSecurityChange(GeckoSession session, SecurityInformation securityInfo);
	}

	private TrackingProtectionDelegate mTrackingProtectionDelegate;
	public void setTrackingProtectionDelegate(TrackingProtectionDelegate delegate) {
		mTrackingProtectionDelegate = delegate;
	}
	public TrackingProtectionDelegate getTrackingProtectionDelegate() {
		return mTrackingProtectionDelegate;
	}
	public interface TrackingProtectionDelegate {
		static final int CATEGORY_AD = 1 << 0;
		static final int CATEGORY_ANALYTIC = 1 << 1;
		static final int CATEGORY_SOCIAL = 1 << 2;
		static final int CATEGORY_CONTENT = 1 << 3;
		void onTrackerBlocked(GeckoSession session, String uri, int categories);
	}

	private NavigationDelegate mNavigationDelegate;
	public void setNavigationDelegate(NavigationDelegate delegate) {
		mNavigationDelegate = delegate;
	}
	public NavigationDelegate getNavigationDelegate() {
		return mNavigationDelegate;
	}
	public interface NavigationDelegate {
		enum TargetWindow {
			DEFAULT(0),
			CURRENT(1),
			NEW(2);
			private static final TargetWindow[] sValues = TargetWindow.values();
			private int mValue;
			private TargetWindow(int value) {
				mValue = value;
			}
			public static TargetWindow forValue(int value) {
				return sValues[value];
			}
			public static TargetWindow forGeckoValue(int value) {
				// DEFAULT(0),
				// CURRENT(1),
				// NEW(2),
				// NEWTAB(3),
				// SWITCHTAB(4);
				final TargetWindow[] sMap = {
					DEFAULT,
					CURRENT,
					NEW,
					NEW,
					NEW
				};
				return sMap[value];
			}
		}
		boolean onLoadUri(GeckoSession session, String uri, TargetWindow where);
		void onNewSession(GeckoSession session, String uri, Response<GeckoSession> response);
	}

	private PromptDelegate mPromptDelegate;
	public void setPromptDelegate(PromptDelegate delegate) {
		mPromptDelegate = delegate;
	}
	public PromptDelegate getPromptDelegate() {
		return mPromptDelegate;
	}
	public interface PromptDelegate {
		interface AlertCallback {
			void dismiss();
			boolean hasCheckbox();
			String getCheckboxMessage();
			boolean getCheckboxValue();
			void setCheckboxValue(boolean value);
		}
		void onAlert(GeckoSession session, String title, String msg, AlertCallback callback);
		interface ButtonCallback extends AlertCallback {
			void confirm(int button);
		}
		static final int BUTTON_TYPE_POSITIVE = 0;
		static final int BUTTON_TYPE_NEUTRAL = 1;
		static final int BUTTON_TYPE_NEGATIVE = 2;
		void onButtonPrompt(GeckoSession session, String title, String msg, String[] btnMsg, ButtonCallback callback);
		interface TextCallback extends AlertCallback {
			void confirm(String text);
		}
		void onTextPrompt(GeckoSession session, String title, String msg, String value, TextCallback callback);
		interface AuthCallback extends AlertCallback {
			void confirm(String password);
			void confirm(String username, String password);
		}
		class AuthOptions {
			public static final int AUTH_FLAG_HOST = 1;
			public static final int AUTH_FLAG_PROXY = 2;
			public static final int AUTH_FLAG_ONLY_PASSWORD = 8;
			public static final int AUTH_FLAG_PREVIOUS_FAILED = 16;
			public static final int AUTH_FLAG_CROSS_ORIGIN_SUB_RESOURCE = 32;
			public static final int AUTH_LEVEL_NONE = 0;
			public static final int AUTH_LEVEL_PW_ENCRYPTED = 1;
			public static final int AUTH_LEVEL_SECURE = 2;
			public int flags = 0;
			public String uri = null;
			public int level = 0;
			public String username = null;
			public String password = null;
		}
		void onAuthPrompt(GeckoSession session, String title, String msg, AuthOptions options, AuthCallback callback);
		class Choice {
			public static final int CHOICE_TYPE_MENU = 1;
			public static final int CHOICE_TYPE_SINGLE = 2;
			public static final int CHOICE_TYPE_MULTIPLE = 3;
			public final boolean disabled = false;
			public final String icon = null;
			public final String id = null;
			public final Choice[] items = null;
			public final String label = null;
			public final boolean selected = false;
			public final boolean separator = false;
		}
		interface ChoiceCallback extends AlertCallback {
			void confirm(String id);
			void confirm(String[] ids);
			void confirm(Choice item);
			void confirm(Choice[] items);
		}
		void onChoicePrompt(GeckoSession session, String title, String msg, int type, Choice[] choices, ChoiceCallback callback);
		void onColorPrompt(GeckoSession session, String title, String value, TextCallback callback);
		static final int DATETIME_TYPE_DATE = 1;
		static final int DATETIME_TYPE_MONTH = 2;
		static final int DATETIME_TYPE_WEEK = 3;
		static final int DATETIME_TYPE_TIME = 4;
		static final int DATETIME_TYPE_DATETIME_LOCAL = 5;
		void onDateTimePrompt(GeckoSession session, String title, int type, String value, String min, String max, TextCallback callback);
		interface FileCallback extends AlertCallback {
			void confirm(Context context, Uri uri);
			void confirm(Context context, Uri[] uris);
		}
		static final int FILE_TYPE_SINGLE = 1;
		static final int FILE_TYPE_MULTIPLE = 2;
		void onFilePrompt(GeckoSession session, String title, int type, String[] mimeTypes, FileCallback callback);
	}

	private PermissionDelegate mPermissionDelegate;
	public void setPermissionDelegate(PermissionDelegate delegate) {
		mPermissionDelegate = delegate;
	}
	public PermissionDelegate getPermissionDelegate() {
		return mPermissionDelegate;
	}
	public interface PermissionDelegate {
		interface Callback {
			void grant();
			void reject();
		}
		interface MediaCallback {
			void grant(final String video, final String audio);
			void grant(final MediaSource video, final MediaSource audio);
			void reject();
		}
		void onMediaPermissionRequest(GeckoSession session, String uri, MediaSource[] video, MediaSource[] audio, MediaCallback callback);
		class MediaSource {
			public static final int SOURCE_CAMERA = 0;
			public static final int SOURCE_SCREEN  = 1;
			public static final int SOURCE_APPLICATION = 2;
			public static final int SOURCE_WINDOW = 3;
			public static final int SOURCE_BROWSER = 4;
			public static final int SOURCE_MICROPHONE = 5;
			public static final int SOURCE_AUDIOCAPTURE = 6;
			public static final int SOURCE_OTHER = 7;
			public static final int TYPE_VIDEO = 0;
			public static final int TYPE_AUDIO = 1;
			public final String id = null;
			public final String rawId = null;
			public final String name = null;
			public final int source = 7;
			public final int type = 0;
		}
	}

	public interface ScrollDelegate {
		public void onScrollChanged(GeckoSession session, int scrollX, int scrollY);
	}

	private ScrollDelegate mScrollDelegate;
	public void setScrollDelegate(ScrollDelegate delegate) {
		mScrollDelegate = delegate;
	}
	public ScrollDelegate getScrollDelegate() {
		return mScrollDelegate;
	}


	public boolean isOpen() {
		return true;
	}
	public void openWindow(final @Nullable Context appContext) {
	}
	public void closeWindow() {
	}
	// public @NonNull TextInputController getTextInputController() {
	// }
	public void reload() {
	}
	public void stop() {
	}
	public void goBack() {
	}
	public void goForward() {
	}
	public void setActive(boolean active) {
	}
	// public GeckoSessionSettings getSettings() {
	// }
	public void importScript(final String url) {
	}
	public void exitFullScreen() {
	}
	public void enableTrackingProtection(int categories) {
	}
	public void disableTrackingProtection() {
	}

	public interface Response<T> {
		void respond(T val);
	}

}
