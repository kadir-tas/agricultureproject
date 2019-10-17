package com.akovasi.agricultureproject;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class UserPageFragment extends Fragment {

    private TextView currentUserIdText;
    private TextView currentUserNameText;
    private TextView currentUserEmailText;
    private TextView currentUserPhoneText;
    private TextView currentUserAddressText;

    private Button editUserButton;
    private Button buttonSignOut;
    private Button editProfileButton;
    private Button editAccountButton;

    private ImageView imageAsset;
    private Dialog editDialog;
    private ProgressBar progressBar;

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference myRef = database.getReference("User");
    private String uid;

    public void initializeScreen() {

        Log.v("Aloooo1", "asdadasdasd");
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String fullName = (dataSnapshot.child(uid).child("profile").child("userName").getValue() + "")
                        + " " + (dataSnapshot.child(uid).child("profile").child("userLastname").getValue() + "");
                currentUserIdText.setText(dataSnapshot.child(uid).getKey().toString());
                currentUserNameText.setText(fullName);
                currentUserEmailText.setText(dataSnapshot.child(uid).child("profile").child("userEmail").getValue() + "");
                currentUserPhoneText.setText(dataSnapshot.child(uid).child("profile").child("userPhone").getValue() + "");
                currentUserAddressText.setText(dataSnapshot.child(uid).child("profile").child("userAddress").getValue() + "");
                progressBar.setVisibility(View.INVISIBLE);
                buttonSignOut.setEnabled(true);
                imageAsset.setEnabled(true);
                editUserButton.setEnabled(true);
                Log.v("ALOOOOOOO2 ", dataSnapshot.child(uid).child("profile").child("userName").getValue().toString());


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        };
        Log.v("POSTLISTENER ", postListener.toString());
        myRef.addValueEventListener(postListener);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        while (uid == null) {
            Log.v("asdasd", "asdasd");
        }
        this.initializeScreen();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_user_page, null);

//        getActivity().setContentView(R.layout.fragment_user_page);
        progressBar = (ProgressBar) v.findViewById(R.id.progress_bar_userpage);
        uid = FirebaseHelper.getmFirebaseAuth().getUid();
        currentUserIdText = (TextView) v.findViewById(R.id.uidField);
        currentUserNameText = (TextView) v.findViewById(R.id.fullNameField);
        currentUserEmailText = (TextView) v.findViewById(R.id.mailField);
        currentUserPhoneText = (TextView) v.findViewById(R.id.phoneField);
        currentUserAddressText = (TextView) v.findViewById(R.id.adresField);
        editUserButton = (Button) v.findViewById(R.id.buttonEditUser);
        buttonSignOut = (Button) v.findViewById(R.id.buttonSignOut);
        imageAsset = (ImageView) v.findViewById(R.id.edit_info_button);
        editDialog = new Dialog(getActivity());
        editDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);


        buttonSignOut.setEnabled(false);
        imageAsset.setEnabled(false);
        editUserButton.setEnabled(false);


        editUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonSignOut.setEnabled(false);
                editUserButton.setEnabled(false);
                imageAsset.setEnabled(false);
                progressBar.setVisibility(View.VISIBLE);
                Intent intent = new Intent(getActivity(), EditUserActivity.class);
                startActivity(intent);
                getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                getActivity().finish();
            }
        });

        buttonSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonSignOut.setEnabled(false);
                editUserButton.setEnabled(false);
                imageAsset.setEnabled(false);
                progressBar.setVisibility(View.VISIBLE);
                FirebaseHelper.signOut();
                Intent goLoginPage = new Intent(getActivity(), MainActivity.class);
                getActivity().finish();
                startActivity(goLoginPage);

            }
        });

        imageAsset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editDialog.setContentView(R.layout.edit_user_popup);
                editAccountButton = (Button) editDialog.findViewById(R.id.change_pass_email);
                editProfileButton = (Button) editDialog.findViewById(R.id.change_profile_info);
                editProfileButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        editProfileButton.setEnabled(false);
                        editAccountButton.setEnabled(false);
                        editUserButton.setEnabled(false);
                        buttonSignOut.setEnabled(false);
                        editDialog.dismiss();
                        Intent intent = new Intent(getActivity(), UpdateUserProfileActivity.class);
                        startActivity(intent);
                        getActivity().finish();
                    }
                });
                editAccountButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        editProfileButton.setEnabled(false);
                        editAccountButton.setEnabled(false);
                        editUserButton.setEnabled(false);
                        buttonSignOut.setEnabled(false);
                        editDialog.dismiss();
                        Intent intent = new Intent(getActivity(), UpdateUserAccountActivity.class);
                        startActivity(intent);
                        getActivity().finish();
                    }
                });
                editDialog.show();
            }
        });

        return v;
    }


//    @Override
//    public void onBackPressed() {
//        editUserButton.setEnabled(true);
//        buttonSignOut.setEnabled(true);
//        super.onBackPressed();
//    }

    @Override
    public String toString() {
        return "Tarla Takip";
    }
}
