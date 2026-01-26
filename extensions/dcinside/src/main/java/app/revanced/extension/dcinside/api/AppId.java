package app.revanced.extension.dcinside.api;

import android.content.Context;
import android.util.Log;

import com.dcinside.app.util.Save;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AppId {

    private static final String TAG = "AppId";
    private static final String DC_APP_PACKAGE = "com.dcinside.app.android";
    private static final String DC_APP_VERSION_CODE = "100148";
    private static final String DC_APP_VERSION_NAME = "5.2.5";
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

                    boolean result = jsonResponse.getBoolean("result");
                    String appId = jsonResponse.getString("app_id");

                    if (!result) {
                        String cause  = jsonResponse.getString("cause");
                        return "F:" + cause;
                    }

                    Save.appAccessIdTime(System.currentTimeMillis());
                    Save.appAccessIdValue(appId);

                    return "S:" + appId;
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

    public static String getApkSignatureHex() {
        return "E6:B2:71:44:A2:76:60:B1:E0:06:08:FA:45:D3:19:06:5D:97:D0:A7:1F:B5:4B:8C:A1:75:6E:83:46:EC:29:DD";
    }

    public static ArrayList<String> getApkSignatureByType(String type) {
        if (type.equals("hex")) {
            return new ArrayList<>(List.of("59:E7:78:9C:3F:45:49:62:EF:CC:BC:47:88:44:52:E4:88:5D:AE:0F:BD:16:E0:B4:48:00:F1:46:63:04:F9:13"));
        } else if (type.equals("base")) {
            return new ArrayList<>(List.of("Wed4nD9FSWLvzLxHiERS5Ihdrg+9FuC0SADxRmME+RM="));
        } else {
            throw new IllegalArgumentException("Invalid type: " + type);
        }
    }
}
