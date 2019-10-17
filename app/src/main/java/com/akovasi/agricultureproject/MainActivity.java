package com.akovasi.agricultureproject;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.akovasi.agricultureproject.Adapter.FragPagerAdapter;
import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements LoginPageFragment.OnFragmentInteractionListener {

    FirebaseHelper firebaseHelper;
    Dialog dialogNoConn;
    Button button;
    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        firebaseHelper = new FirebaseHelper();
        dialogNoConn = new Dialog(MainActivity.this);
        dialogNoConn.setContentView(R.layout.internet_connection_popup);
        button = (Button) dialogNoConn.findViewById(R.id.button_internet);
        dialogNoConn.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        if (isOnline()) {
            if (firebaseHelper.getFirebaseUserAuthID() == null) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.container, new LoginPageFragment());
                ft.addToBackStack(null);
                ft.setTransition(1);
                ft.commit();
            } else {
                ViewPager vp = (ViewPager) findViewById(R.id.mViewPager_ID);
                this.addPages(vp);

                TabLayout tabLayout = (TabLayout) findViewById(R.id.mTab_ID);
                tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
                tabLayout.setupWithViewPager(vp);
                tabLayout.addOnTabSelectedListener(listener(vp));
//                firebaseAuth = FirebaseHelper.getmFirebaseAuth();
//                if(firebaseAuth.getCurrentUser() != null){
//                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
//                    ft.replace(R.id.container, new UserPageFragment());
//                    ft.addToBackStack(null);
//                    ft.setTransition(1);
//                    ft.commit();
//                }
            }
        } else {

            dialogNoConn.show();
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialogNoConn.dismiss();
                    finish();
                }
            });

        }
    }

    private void addPages(ViewPager pager) {
        FragPagerAdapter adapter = new FragPagerAdapter(getSupportFragmentManager());
        adapter.addPage(new Tarlahane());
        adapter.addPage(new UserPageFragment());
        adapter.addPage(new SimulationFragment());
        pager.setAdapter(adapter);
    }

    private TabLayout.OnTabSelectedListener listener(final ViewPager pager) {
        return new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                pager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        };
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    public boolean isOnline() {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int exitValue = ipProcess.waitFor();
            return (exitValue == 0);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }
}