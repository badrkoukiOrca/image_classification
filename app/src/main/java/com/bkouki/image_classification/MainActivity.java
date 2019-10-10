package com.bkouki.image_classification;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.common.modeldownload.FirebaseLocalModel;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.common.modeldownload.FirebaseRemoteModel;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceAutoMLImageLabelerOptions;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int pic_id = 123;

    ImageView imageView = null ;
    TextView textView = null ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.image_view) ;
        textView = findViewById(R.id.text_label);

        FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions.Builder()
                .requireWifi()
                .build();

        FirebaseRemoteModel remoteModel = new FirebaseRemoteModel.Builder("docs")
                .enableModelUpdates(true)
                .setInitialDownloadConditions(conditions)
                .setUpdatesDownloadConditions(conditions)
                .build();

        FirebaseModelManager.getInstance().registerRemoteModel(remoteModel);

        FirebaseLocalModel localModel = new FirebaseLocalModel.Builder("model")
                .setAssetFilePath("manifest.json")
                .build();
        FirebaseModelManager.getInstance().registerLocalModel(localModel);


        Intent camera_intent
                = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(camera_intent, pic_id);

    }
    // This method will help to retrieve the image
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data) {

        // Match the request 'pic id with requestCode
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == pic_id) {

            Bitmap photo = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(photo);
            try {
                FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(photo);
                FirebaseVisionOnDeviceAutoMLImageLabelerOptions labelerOptions =
                        new FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder()
                                .setLocalModelName("model")
                                .setConfidenceThreshold(0)
                                .build();
                FirebaseVisionImageLabeler labeler =
                        FirebaseVision.getInstance().getOnDeviceAutoMLImageLabeler(labelerOptions);
                labeler.processImage(image)
                        .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                            @Override
                            public void onSuccess(List<FirebaseVisionImageLabel> labels) {
                                String result = "";
                                double min_score = 0 ;
                                for (FirebaseVisionImageLabel label: labels) {
                                    String text = label.getText();
                                    float confidence = label.getConfidence();
                                    if(confidence>min_score){
                                        min_score  = confidence ;
                                        result = text ;
                                    }
                                }
                                textView.setText("this document is "+result+" score = "+min_score);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {

                            }
                        });

            } catch (FirebaseMLException e) {
                e.printStackTrace();
            }
        }
    }

}
