package com.onebot.s2w;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

import com.onebot.s2w.R;
//import com.github.chrisbanes.photoview.PhotoView;
//import com.onebot.s2wtest.R;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(com.onebot.s2w.R.layout.activity_fullscreen);

        String url = getIntent().getExtras().getString("URL");

        ImageView fullScreenImageView = (ImageView) findViewById(com.onebot.s2w.R.id.fullscreen_imageview);
        Intent callingActivityIntent = getIntent();
        /*if (callingActivityIntent != null) {
            Uri imageUri = callingActivityIntent.getData();
            if (imageUri != null && fullScreenImageView != null) {
                Glide.with(this)
                        .load(imageUri)
                        .into(fullScreenImageView);
            }
        }*/

        if (callingActivityIntent != null) {
            if (url != null && fullScreenImageView != null) {
                GlideUtil.loadImage(url, fullScreenImageView);
            }
        }

        fullScreenImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}

