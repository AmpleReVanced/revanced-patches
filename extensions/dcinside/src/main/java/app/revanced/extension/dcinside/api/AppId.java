package app.revanced.extension.dcinside.api;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AppId {

    private static final String TAG = "AppId";
    private static final String DC_APP_PACKAGE = "com.dcinside.app.android";
    private static final String DC_APP_VERSION_CODE = "100128";
    private static final String DC_APP_VERSION_NAME = "5.1.4";
    private static final String DC_APP_SIGNATURE = "5rJxRKJ2YLHgBgj6RdMZBl2X0KcftUuMoXVug0bsKd0=";
    private static final String USER_AGENT = "dcinside.app";

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    /**
     * Generates an app ID based on the provided parameters.
     * @param context the application context
     * @param packageName the package name of the app
     * @param date from <a href="https://json2.dcinside.com/json0/app_check_A_rina_new.php">...</a>
     * @param firebaseInstanceId the Firebase instance ID of the app
     * @return AppID
     */
    public static String getAppId(Context context, String packageName, String date, String firebaseInstanceId) {
        try {
            String hashedAppKey = generateHashedAppKey(date);

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("value_token", hashedAppKey)
                    .addFormDataPart("signature", DC_APP_SIGNATURE)
                    .addFormDataPart("pkg", DC_APP_PACKAGE)
                    .addFormDataPart("vCode", DC_APP_VERSION_CODE)
                    .addFormDataPart("vName", DC_APP_VERSION_NAME)
                    .addFormDataPart("client_token", firebaseInstanceId)
                    .build();

            Request request = new Request.Builder()
                    .url("https://msign.dcinside.com/auth/mobile_app_verification")
                    .header("User-Agent", USER_AGENT)
                    .header("Referer", "http://www.dcinside.com")
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    return "S:" + jsonResponse.getString("app_id");
                } else {
                    Log.e(TAG, "HTTP request failed with response code: " + response.code());
                    return "F:Failed to get app ID";
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error generating app ID", e);
            return "F:Error generating app ID: " + e.getMessage();
        }
    }

    /**
     * Generates SHA256 hash of "dcArdchk_" + date
     */
    private static String generateHashedAppKey(String date) throws Exception {
        String input = "dcArdchk_" + date;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
