package com.example.testcontentresolver;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.Manifest.permission;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {
    private ContentObserver contentObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final TextView view = findViewById(R.id.logText);
        view.setMovementMethod(new ScrollingMovementMethod());

        // CHECK permission
        askForPermissionIfNeeded();

        ContentResolver contentResolver = this.getContentResolver();
        contentObserver = new NewImageContentObserver(
                this.getApplicationContext(),
                new UpdateUiCallback() {
                    @Override
                    public void onUpdate(final String text) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String currentText = view.getText().toString();
                                view.setText(currentText + "\n" + text);
                            }
                        });
                    }
                });
        contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                contentObserver
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.getContentResolver().unregisterContentObserver(contentObserver);
    }

    private void askForPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), permission.READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[] { permission.READ_EXTERNAL_STORAGE },
                        12);
            }
        }
    }
}
