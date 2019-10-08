package io.getstream.chat.example;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class FragmentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        if (savedInstanceState == null) {
            Fragment myFragment = new ChatFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragmentParentViewGroup, myFragment, "")
                    .commit();
        }
    }
}
