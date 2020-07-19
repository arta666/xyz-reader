package com.example.xyzreader.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.xyzreader.R;
import com.example.xyzreader.databinding.ActivitySplashBinding;

public class SplashActivity extends AppCompatActivity {

    ActivitySplashBinding binding;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        Animation animation = AnimationUtils.loadAnimation(this, R.anim.splash_transition);
        binding.ivLogo.startAnimation(animation);
        final Intent intent = new Intent(this,ArticleListActivity.class);
        if (savedInstanceState == null){
            Thread thread = new Thread(){
                @Override
                public void run() {
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }finally {
                        startActivity(intent);
                        finish();
                    }
                }
            };

            thread.start();
        }else {
            startActivity(intent);
        }

    }

}