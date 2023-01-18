package com.example.catordog;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import org.tensorflow.lite.Interpreter;
import android.os.Handler;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 100;
    private Interpreter tflite;
    private Bitmap Image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView imageView = findViewById(R.id.imageView);
        imageView.setVisibility(View.INVISIBLE);

        TextView result = findViewById(R.id.result);
        TextView status = findViewById(R.id.status);
        status.setText("Loading model...");

        try {
            tflite = new Interpreter(loadModelFile());
            status.setText("Press the button to select an image");
        } catch (Exception ex) {
            ex.printStackTrace();
            status.setText("ERROR: Could not load model");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                ImageView imageView = findViewById(R.id.imageView);
                imageView.setVisibility(View.VISIBLE);

                Bitmap originalImage = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);

                Bitmap croppedImage = PreprocessImage(originalImage);

                Image = croppedImage;

                imageView.setImageBitmap(croppedImage);

                TextView result = findViewById(R.id.result);
                result.setText("Press \"check\" to check if this is a cat or a dog");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Bitmap PreprocessImage(Bitmap originalImage) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        int newWidth = 256;
        int newHeight = 256;

        if (width < height) {
            newHeight = (int) Math.floor((float) height * ((float) newWidth / (float) width));
        } else {
            newWidth = (int) Math.floor((float) width * ((float) newHeight / (float) height));
        }
        Bitmap resizedImage = Bitmap.createScaledBitmap(originalImage, newWidth, newHeight, true);

        //crop image to 256x256 from center
        int startX = (resizedImage.getWidth() - 256) / 2;
        int startY = (resizedImage.getHeight() - 256) / 2;
        Bitmap croppedImage = Bitmap.createBitmap(resizedImage, startX, startY, 256, 256);
        return croppedImage;
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("model_update.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public void ChoosePicture(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Dog or Cat picture"), PICK_IMAGE);
    }

    public void CheckImage(View view) {
        TextView result = findViewById(R.id.result);

        if (Image == null) {
            result.setText("Please select an image first");
            return;
        }

        result.setText("Checking...");
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                RunModel();
            }
        }, 1);
    }

    private void RunModel() {
        TextView result = findViewById(R.id.result);

        float[][][][] input = new float[1][256][256][3];
        for (int x = 0; x < 256; x++) {
            for (int y = 0; y < 256; y++) {
                int pixel = Image.getPixel(x, y);
                input[0][x][y][0] = Color.red(pixel) / 255.0f;
                input[0][x][y][1] = Color.green(pixel) / 255.0f;
                input[0][x][y][2] = Color.blue(pixel) / 255.0f;
            }
        }


        float[][] output = new float[1][1];
        tflite.run(input, output);
        result.setText("cat in " + 100 * (1 - output[0][0]) + "%\ndog in " + 100 * output[0][0] + "%");
    }
}