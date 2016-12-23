package com.example.ees37.frame;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.ExponentialBackOff;

import com.google.api.services.gmail.GmailScopes;

import com.google.api.services.gmail.model.*;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.*;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_IMMERSIVE;

public class MainActivity extends Activity
        implements EasyPermissions.PermissionCallbacks {
    GoogleAccountCredential mCredential;
    private ImageView mImageView;
    ProgressDialog mProgress;


    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    private static final String BUTTON_TEXT = "Call Gmail API";
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { GmailScopes.MAIL_GOOGLE_COM };

    /**
     * Create the main activity.
     * @param savedInstanceState previously saved instance data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageView = (ImageView)findViewById(R.id.image_id);
        //mImageView.setSystemUiVisibility(SYSTEM_UI_FLAG_FULLSCREEN );
        mImageView.setSystemUiVisibility(SYSTEM_UI_FLAG_IMMERSIVE | SYSTEM_UI_FLAG_HIDE_NAVIGATION | SYSTEM_UI_FLAG_FULLSCREEN );

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getResultsFromApi();
                    }
                });

            }
        }, 0, 10000);

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                changeImage();
            }
        }, 0, 8000);
    }


    public void changeImage()
    {
        File filesDir = new File(getFilesDir(), "/pics/");

        int index = 0, newIndex = 1;

        if (filesDir.list() != null) {
            for (String s : filesDir.list()) {
                //File file = new File(filesDir + "/", s);
               //file.delete();
                if (s.startsWith("switchon")) {
                    File file = new File(filesDir + "/", s);
                    file.delete();

                    Pattern p = Pattern.compile("-?\\d+");
                    Matcher m = p.matcher(s);
                    m.find();
                    index = Integer.parseInt(m.group());

                    if (index < getMaxImage()) {
                        newIndex = index + 1;
                    } else {
                        newIndex = 0;
                    }
                }
            }
        }

        try {
            new File(filesDir, "switchon" + Integer.toString(newIndex) + ".txt").createNewFile();
        }
        catch (IOException e) {}

        final File dir = filesDir;
        final int finalIndex = index;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                File file = new File(getImagePath(finalIndex));
                if (file.exists()) {
                    try {
                        ExifInterface exif = new ExifInterface(file.getAbsolutePath());
                        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
                        float rotate = 0;

                        if (orientation == 6)
                        {
                            rotate = 90;
                        }
                        else if (orientation == 3)
                        {
                            rotate = 180;
                        }
                        else if (orientation == 8)
                        {
                            rotate = 270;
                        }

                        Matrix matrix = new Matrix();
                        matrix.postRotate(rotate);

                        Bitmap image = BitmapFactory.decodeFile(file.getAbsolutePath());

                        Bitmap rotatedBitmap = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
                        mImageView.setImageBitmap(rotatedBitmap);

                    } catch (IOException e) {}


                    //mImageView.setImageURI(Uri.fromFile(file));
                }
            }
        });

        //final Bitmap bmImg = BitmapFactory.decodeFile(getImagePath(index));
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                mImageView.setImageBitmap(bmImg);
//            }
//        });
    }

    public int getMaxImage()
    {
        int max = 0;
        File filesDir = new File(getFilesDir(), "/pics/");
        for (String s : filesDir.list()) {
            Pattern p = Pattern.compile("-?\\d+");
            Matcher m = p.matcher(s);
            m.find();
            int index = Integer.parseInt(m.group());

            if (index > max)
            {
                max = index;
            }
        }
        return max;

    }


    public String getImagePath(int index)
    {
        File filesDir = new File(getFilesDir() + "/pics/");
        String path = "";
        log("index " + Integer.toString(index));
        if (filesDir.list() != null) {
            for (String s : filesDir.list()) {
                log(s);
                if (index >= 10 && s.startsWith(Integer.toString(index))) {
                    path = filesDir.getAbsolutePath() + "/" + s;
                    log("Image path: " + path);
                    break;
                }
                else if (index < 10 && s.charAt(0) == Integer.toString(index).charAt(0))
                {
                    path = filesDir.getAbsolutePath() + "/" + s;
                    log("Image path: " + path);
                    break;
                }
            }
        }
        return path;
    }


    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi() {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (! isDeviceOnline()) {
           // mOutputText.setText("No network connection available.");
        } else {
            new MakeRequestTask(mCredential).execute();
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    //mOutputText.setText(
                           // "This app requires Google Play Services. Please install " +
                             //       "Google Play Services on your device and relaunch this app.");
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    /**
     * An asynchronous task that handles the Gmail API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.gmail.Gmail mService = null;
        private Exception mLastError = null;

        MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.gmail.Gmail.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Gmail API Android Quickstart")
                    .build();
        }

        /**
         * Background task to call Gmail API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of Gmail labels attached to the specified account.
         * @return List of Strings labels.
         * @throws IOException
         */
        private List<String> getDataFromApi() throws IOException {
            // Get the labels in the user's account.
            log("hello");
            String user = "me";
            List<String> labels = new ArrayList<String>();

            ListMessagesResponse listResponse =
                    mService.users().messages().list(user).setQ("is:unread").execute();
            log("hello again");
            for (Message msg : listResponse.getMessages()) {
                Message message = mService.users().messages().get(user, msg.getId()).execute();
                log(message.toString());
                markAsRead(message.getId());

                if (message.getPayload() != null) {
                    List<MessagePart> parts = message.getPayload().getParts();

                    for (MessagePart part: parts) {
                        String filename = part.getFilename();
                        String attId = part.getBody().getAttachmentId();
                        String msgId = message.getId();

                        if (filename != null && filename.length() > 0) {
                            File filesDir = new File(getFilesDir() + "/pics/");

                            if (filesDir.mkdirs())
                            {
                                log("Success creating internal storage directory:");
                                log(filesDir.getAbsolutePath());
                            }
                           
                            int index = 0;; 
                            // list the files that are in the directory 
                            for (String s: filesDir.list())
                            {
                                log(s);

                                if (s.startsWith("on"))
                                {
                                    File file = new File(filesDir +"/", s);
                                    file.delete();

                                    Pattern p = Pattern.compile("-?\\d+"); 
                                    Matcher m = p.matcher(s);
                                    m.find(); 
                                    index = Integer.parseInt(m.group());
                                    
                                    if (index < 30)
                                    {
                                        index = index + 1; 
                                    }
                                    else
                                    {
                                        index = 0;
                                    }
                                    log("We are at index " + Integer.toString(index)); 
                                }
                            }


                            File file = new File(getImagePath(index));
                            file.delete();

                            filename = Integer.toString(index) + filename.substring(filename.indexOf("."));
                            log("creating a file at " + filename);                         

                            MessagePartBody partBody = mService.users().messages().attachments().get(user, msgId, attId).execute();

                            if (filename.toLowerCase().contains("png") || filename.toLowerCase().contains("jpg") || filename.toLowerCase().contains("jpeg")) {

                                byte[] fileByteArray = Base64.decodeBase64(partBody.getData());
                                FileOutputStream fileOutFile =
                                        new FileOutputStream(filesDir.getAbsolutePath() + "/" + filename);
                                fileOutFile.write(fileByteArray);
                                fileOutFile.close();

                                file = new File(filesDir, "on" + Integer.toString(index) + ".txt");
                                file.createNewFile();
                            }
                        }
                    }
                }
            }
            return labels;
        }

        public void markAsRead(String msgId) throws IOException
        {
            ModifyMessageRequest mods = new ModifyMessageRequest().setRemoveLabelIds(Arrays.asList("UNREAD")); 
            mService.users().messages().modify("me", msgId, mods).execute();
        }

        @Override
        protected void onPreExecute() {
           // mOutputText.setText("");
           // mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            //mProgress.hide();
            if (output == null || output.size() == 0) {
              //  mOutputText.setText("No results returned.");
            } else {
                output.add(0, "Data retrieved using the Gmail API:");
               // mOutputText.setText(TextUtils.join("\n", output));
            }
        }

        @Override
        protected void onCancelled() {
            //mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                   // mOutputText.setText("The following error occurred:\n"
                           // + mLastError.getMessage());
                }
            } else {
               // mOutputText.setText("Request cancelled.");
            }
        }
    }

    public void log(String msg)
    {
        //Log.d("MyApp", msg);
    }

}
