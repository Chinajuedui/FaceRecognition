package com.absolute.facerecognition.activity;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.absolute.facerecognition.R;
import com.absolute.facerecognition.adapter.ViewPagerAdapter;
import com.absolute.facerecognition.fragment.FaceComparsionFragment;
import com.absolute.facerecognition.fragment.FaceDetectFragment;
import com.absolute.facerecognition.fragment.FaceRecognitionFragment;
import com.absolute.facerecognition.fragment.FaceRetrievalFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private ViewPager main_vp;
    private BottomNavigationView main_nv;
    private MenuItem menuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        main_vp = findViewById(R.id.main_vp);
        main_nv = findViewById(R.id.main_nv);
        main_nv.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.face_recognition:
                        main_vp.setCurrentItem(0);
                        break;
                    case R.id.face_detect:
                        main_vp.setCurrentItem(1);
                        break;
                    case R.id.face_comparse:
                        main_vp.setCurrentItem(2);
                        break;
                    case R.id.face_retrieval:
                        main_vp.setCurrentItem(3);
                    default:
                        break;
                }
                return false;
            }
        });
        main_vp.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (menuItem != null) {
                    menuItem.setChecked(false);
                } else {
                    main_nv.getMenu().getItem(0).setChecked(false);
                }
                menuItem = main_nv.getMenu().getItem(position);
                menuItem.setChecked(true);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new FaceRecognitionFragment());
        adapter.addFragment(new FaceDetectFragment());
        adapter.addFragment(new FaceComparsionFragment());
        adapter.addFragment(new FaceRetrievalFragment());
        main_vp.setAdapter(adapter);
    }

}
