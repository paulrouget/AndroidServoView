package org.mozilla.gecko;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GeckoProfile {
    public static void enableDirectoryChanges() {
    }

    public static final String DEFAULT_PROFILE = "default";
    public static final String CUSTOM_PROFILE = "";
    public static final String GUEST_PROFILE_DIR = "guest";
    public static final String GUEST_MODE_PREF = "guestMode";
    public static boolean shouldUseGuestMode(final Context context) {
      return false;
    }

    public static void enterGuestMode(final Context context) {
    }

    public static void leaveGuestMode(final Context context) {
    }

    public static void setIntentArgs(final String intentArgs) {
    }

    public static GeckoProfile initFromArgs(final Context context, final String args) {
      return new GeckoProfile();
    }

    public static GeckoProfile get(Context context) {
      return new GeckoProfile();
    }

    public static GeckoProfile get(Context context, String profileName) {
      return new GeckoProfile();
    }

    public static GeckoProfile get(Context context, String profileName, String profilePath) {
      return new GeckoProfile();
    }

    public static GeckoProfile get(Context context, String profileName, File profileDir) {
      return new GeckoProfile();
    }

    public static boolean removeProfile(final Context context, final GeckoProfile profile) {
      return false;
    }

    public static GeckoProfile getGuestProfile(final Context context) {
      return new GeckoProfile();
    }

    public static boolean isGuestProfile(final Context context, final String profileName, final File profileDir) {
      return false;
    }

    private Object mData;
    public Object getData() {
      return mData;
    }

    public void setData(final Object data) {
      mData = data;
    }

    public String getName() {
      return "";
    }

    public boolean isCustomProfile() {
      return false;
    }

    public boolean inGuestMode() {
      return false;
    }

    public Object getLock() {
      return this;
    }

    public synchronized File getDir() {
      return new File("/tmp");
    }

    public File getFile(String aFile) {
      return null;
    }

    public String getClientId() throws IOException {
      throw new IOException("not implemented");
    }

    public static boolean isClientIdValid(final String clientId) {
      return true;
    }

    public long getAndPersistProfileCreationDate(final Context context) {
      return -1;
    }

    public void updateSessionFile(boolean shouldRestore) {
    }

    public void waitForOldSessionDataProcessing() {
    }

    public String readSessionFile(boolean readBackup) {
      return null;
    }

    public String readPreviousSessionFile() {
      return null;
    }

    public boolean sessionFileExists() {
      return false;
    }

    public void writeFile(final String filename, final String data) {
    }

    public JSONObject readJSONObjectFromFile(final String filename) throws IOException{
      throw new IOException("not implemented");
    }

    public JSONArray readJSONArrayFromFile(final String filename) {
      return new JSONArray();
    }

    public String readFile(String filename) throws IOException {
      throw new IOException("not implemented");
    }

    public boolean deleteFileFromProfileDir(String fileName) {
      return false;
    }

    public static String getDefaultProfileName(final Context context) {
      return "";
    }

    public void enqueueInitialization(final File profileDir) {
    }
}

