package com.example.alpnr;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.github.dhaval2404.imagepicker.BuildConfig;
import com.github.dhaval2404.imagepicker.ImagePicker;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.*;

public class MainActivity extends AppCompatActivity {

    ProgressDialog dialog;

    private static final String GOOGLE_VISION_API_URL = "https://vision.googleapis.com/v1/images:annotate?key=GOOGLE_VISION_API_KEY";

    private ImageView imgPreview;
    private TextView tvDetectedText;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnSelectImage = findViewById(R.id.btnSelectImage);
        Button btnDetectText = findViewById(R.id.btnDetectText);
        imgPreview = findViewById(R.id.imgPreview);
        tvDetectedText = findViewById(R.id.tvDetectedText);

        dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setTitle("Loading");
        dialog.setMessage("Please wait...");
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);


        btnSelectImage.setOnClickListener(v -> {
            ImagePicker.with(MainActivity.this)
                    .crop()
                    .compress(1024)
                    .maxResultSize(1080,1080)
                    .start();
        });


        btnDetectText.setOnClickListener(v -> {
            if (imageUri != null) {
                detectTextFromImage();
            } else {
                Toast.makeText(this, "Please select/click an image first", Toast.LENGTH_SHORT).show();
            }
        });



    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == MainActivity.RESULT_OK && data != null){

            imageUri = data.getData();
            Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
            imgPreview.setImageURI(imageUri);

        }else{
            Toast.makeText(this, "Image not selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void detectTextFromImage() {

        try {

            ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

            if(activeNetwork != null && (activeNetwork.getType()
                    == ConnectivityManager.TYPE_WIFI || activeNetwork.getType()
                    == ConnectivityManager.TYPE_MOBILE))
            {

                //Online
                String encodedImage = encodeImageToBase64FromUri(imageUri);

                String jsonRequest = "{ \"requests\": [ { \"image\": { \"content\": \"" + encodedImage + "\" }, \"features\": [ { \"type\": \"TEXT_DETECTION\" } ] } ] }";

                OkHttpClient client = new OkHttpClient();
                RequestBody body = RequestBody.create(jsonRequest, MediaType.get("application/json; charset=utf-8"));
                Request request = new Request.Builder()
                        .url(GOOGLE_VISION_API_URL)
                        .post(body)
                        .build();

                runOnUiThread(() -> dialog.show());

                client.newCall(request).enqueue(new Callback() {

                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        runOnUiThread(() -> {

                            dialog.dismiss();
                            Toast.makeText(MainActivity.this, "API Request Failed!", Toast.LENGTH_SHORT).show();
                        });
                    }


                    @Override
                    public void onResponse(Call call, Response response) throws IOException {

                        runOnUiThread(() -> dialog.dismiss());

                        assert response.body() != null;
                        String responseData = response.body().string();
                        Log.d("API_RESPONSE", responseData);

                        if (response.isSuccessful()) {
                            String extractedText = parseDetectedText(responseData);
                            runOnUiThread(() -> tvDetectedText.setText(extractedText));
                        } else {
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to get a response", Toast.LENGTH_SHORT).show());
                        }
                    }
                });

            }else{

                //offline
                Toast.makeText(this, "Please connect to the internet first", Toast.LENGTH_SHORT).show();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String encodeImageToBase64FromUri(Uri imageUri) throws IOException {

        InputStream imageStream = getContentResolver().openInputStream(imageUri);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];

        int bytesRead;
        while ((bytesRead = imageStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }

    private String parseDetectedText(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONArray responses = jsonObject.getJSONArray("responses");

            if (responses.length() > 0) {
                JSONObject firstResponse = responses.getJSONObject(0);

                if (firstResponse.has("textAnnotations")) {
                    JSONArray textAnnotations = firstResponse.getJSONArray("textAnnotations");
                    if (textAnnotations.length() > 0) {

                        String detectedText = textAnnotations.getJSONObject(0).getString("description");

                        Log.d("DEBUG", "Detected text: "+detectedText);

//                        return textAnnotations.getJSONObject(0).getString("description");
                        detectedText = detectedText.replaceAll("(?i)\\bIND\\b", "").trim();

                        Log.d("DEBUG", "After removing IND: "+detectedText);

                        detectedText = detectedText.replaceAll("\\s+", "");

                        Log.d("DEBUG", "After removing spaces: " + detectedText);

                        //Regex pattern for Indian vehicle number plate
                        Pattern pattern = Pattern.compile("([A-Z]{2}[0-9]{2}[A-Z]{1,2}[0-9]{4})");
                        Matcher matcher = pattern.matcher(detectedText);


                        if (matcher.find()) {
                            String vehicleNumber = matcher.group(1); // Return only the vehicle number
                            Log.d("DEBUG", "Filtered number: "+vehicleNumber);

                            return vehicleNumber;
                        }else{
                            Log.e("DEBUG", "No number found ");
                        }
                    }
                }
            }
            return "No text found!";
        } catch (Exception e) {
            e.printStackTrace();
            return "No text found!";
        }
    }
}