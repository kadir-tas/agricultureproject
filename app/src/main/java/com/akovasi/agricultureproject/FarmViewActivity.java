package com.akovasi.agricultureproject;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.akovasi.agricultureproject.Adapter.PopupRecyclerViewAdapter;
import com.akovasi.agricultureproject.ShortCut.ShortCut;
import com.akovasi.agricultureproject.datatypes.farmdata.Farm;
import com.akovasi.agricultureproject.datatypes.farmdata.ProductData;
import com.akovasi.agricultureproject.module_selector.ProductSelectorFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class FarmViewActivity extends AppCompatActivity implements ProductSelectorFragment.OnFragmentInteractionListener {

    TextView moduleId, airHum, airTemp, soilHum, soilTemp, ph;


    //EDIT TEXT
//    EditText farm_size_edit_text;

    FarmEditView edit_farm;
    Farm farm;
    Button save_button;
    Button clean_button;
    Button toggle_button;
    FragmentTransaction ft;

    Dialog dialogFarmName, dialogSensorInfo;
    RecyclerView popupRecyclerView;
    PopupRecyclerViewAdapter popupRecyclerViewAdapter;
    ArrayList<String> farmNames;
    DatabaseReference ref;

    String user_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farm_view);

        edit_farm = findViewById(R.id.edit_view);
        toggle_button = findViewById(R.id.toggle_button);
        clean_button = findViewById(R.id.clean_button);
        save_button = findViewById(R.id.save_button);


        farmNames = new ArrayList<>();


        edit_farm.on_long_click_listener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Log.v("ASD", "HEREE");
                if (edit_farm.getCurrent_edit_phase() == FarmEditView.EditPhases.PRODUCT_PLACEMENT_PHASE
                        && edit_farm.isAllow_putting_data()) {
                    spawn_product_selection_fragment();
                } else if (edit_farm.getCurrent_edit_phase() == FarmEditView.EditPhases.PRODUCT_PLACEMENT_PHASE && edit_farm.isCan_remove_data()) {
                    edit_farm.remove_data_last_selected_place();
                } else if (edit_farm.getCurrent_display_phase() == FarmEditView.DisplayPhases.DISPLAY_FIXES && edit_farm.isCan_fix_cell()) {
                    show_fixup_module_dialog();
                }
                return true;
            }
        };


        edit_farm.on_module_click_listener = new FarmEditView.OnModuleClick() {
            @Override
            public void on_module_click(String module_id) {
                ShowSensorInfoPopup(module_id);
            }
        };

        save_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edit_farm.save_farm();

                FirebaseHelper.putFarmInfo(FirebaseAuth.getInstance().getUid(), farm, farm.farm_id);
                Intent intent = new Intent(FarmViewActivity.this, EditUserActivity.class);
                startActivity(intent);
                finish();
            }
        });

        clean_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edit_farm.clean();
            }
        });

        toggle_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edit_farm.next_display_phase();
                update_the_buttons_with_correct_text_according_to_current_phase();
            }
        });

        Bundle extras = getIntent().getExtras();
        user_id = extras.getString("userID");
        farm = (Farm) extras.getSerializable("Farm");

        if (farm == null) {
            farm = new Farm();
            farm.size = 16;
            dialogFarmName = new Dialog(this);
            ShowPopup();
        }

        edit_farm.load_farm(farm);

        update_the_buttons_with_correct_text_according_to_current_phase();
    }

    //TODO: RENAME THIS
    public void update_the_buttons_with_correct_text_according_to_current_phase() {
        if (edit_farm.getCurrent_display_phase() == FarmEditView.DisplayPhases.DISPLAY_PRODUCTS) {
            toggle_button.setText(R.string.show_product);
            toggle_button.setBackgroundResource(R.drawable.rounded_products_button);
        } else if (edit_farm.getCurrent_display_phase() == FarmEditView.DisplayPhases.DISPLAY_MODULES) {
            toggle_button.setText(R.string.show_modules);
            toggle_button.setBackgroundResource(R.drawable.rounded_modules_button);
        } else if (edit_farm.getCurrent_display_phase() == FarmEditView.DisplayPhases.DISPLAY_FIXES) {
            toggle_button.setText(R.string.show_fixes);
            toggle_button.setBackgroundResource(R.drawable.rounded_fixes_button);
        }
    }

    public void spawn_product_selection_fragment() {
        ft = getSupportFragmentManager().beginTransaction();
        ProductSelectorFragment module_selector_fragment = new ProductSelectorFragment();

        module_selector_fragment.setOn_item_selected_callback(new ProductSelectorFragment.OnSelectedItemCallback() {
            @Override
            public void on_selected_item_callback(ProductData productData) {
                edit_farm.put_data_onto_last_selected_place(productData);
                ft = null;
            }
        });

        module_selector_fragment.load_product_data();

        ft.replace(R.id.container, module_selector_fragment);
        ft.addToBackStack(null);
        ft.commit();
    }


    @Override
    public void onBackPressed() {
        if (ft == null) {
            Intent intent = new Intent(FarmViewActivity.this, EditUserActivity.class);
            startActivity(intent);
            this.finish();
        } else {
            ft.addToBackStack(null);
            ft = null;
        }
        super.onBackPressed();
    }

    private void ShowSensorInfoPopup(final String module_Id) {
        dialogSensorInfo = new Dialog(FarmViewActivity.this);
        dialogSensorInfo.setContentView(R.layout.sensor_info_popup);
        //TODO: pop up görüntüsü düzenlendi. Kadir
        dialogSensorInfo.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        moduleId = dialogSensorInfo.findViewById(R.id.moduleId);
        airHum = dialogSensorInfo.findViewById(R.id.air_hum);
        airTemp = dialogSensorInfo.findViewById(R.id.air_temp);
        soilHum = dialogSensorInfo.findViewById(R.id.soil_hum);
        soilTemp = dialogSensorInfo.findViewById(R.id.soil_temp);
        ph = dialogSensorInfo.findViewById(R.id.ph);

        ref = FirebaseDatabase.getInstance().getReference("User").child(user_id)
                .child("Modules").child(farm.farm_id).child("ModuleOfFarms");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot d : dataSnapshot.getChildren()) {
                    if ((d.child("moduleId").getValue() + "").equals(module_Id)) {
                        moduleId.setText("Modul No : " + module_Id);
                        airHum.setText("Hava Nem: " + ((d.child("ahWorking").getValue() + "").equals("true") ? d.child("airHumidity").getValue() : "Çalışmıyor"));
                        airTemp.setText("Hava Sıcaklık: " + ((d.child("atWorking").getValue() + "").equals("true") ? d.child("airTemperature").getValue() : "Çalışmıyor"));
                        soilHum.setText("Toprak Nem : " + ((d.child("shWorking").getValue() + "").equals("true") ? d.child("soilHumidity").getValue() : "Çalışmıyor"));
                        soilTemp.setText("Toprak Sıcaklık : " + ((d.child("stWorking").getValue() + "").equals("true") ? d.child("soilTemperature").getValue() : "Çalışmıyor"));
                        ph.setText("ph: " + ((d.child("phWorking").getValue() + "").equals("true") ? d.child("ph").getValue() : "Çalışmıyor"));
                        dialogSensorInfo.show();
                        return;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void show_fixup_module_dialog() {
        final Dialog d = new Dialog(FarmViewActivity.this);
        d.setContentView(R.layout.fixup_confirm_popup);
        d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        Button yes_butt = d.findViewById(R.id.check_button);
        Button no_butt = d.findViewById(R.id.reject_button);

        yes_butt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edit_farm.fixup_cell_in_last_selected_place();
                d.dismiss();
            }
        });

        no_butt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });

        d.show();
    }

    private void ShowPopup() {

        dialogFarmName.setContentView(R.layout.popup_recyclerview);
        popupRecyclerView = (RecyclerView) dialogFarmName.findViewById(R.id.popup_rec_view);
        LinearLayoutManager lm = new LinearLayoutManager(getApplicationContext());
        lm.setOrientation(LinearLayoutManager.VERTICAL);
        lm.scrollToPosition(0);
        popupRecyclerView.setHasFixedSize(true);
        popupRecyclerView.setLayoutManager(lm);

        ref = FirebaseDatabase.getInstance().getReference("User").child(user_id);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ArrayList<String> fname = new ArrayList<>();
                for (DataSnapshot d : dataSnapshot.child("Modules").getChildren()) {
                    String a = d.getKey() + "";
                    fname.add(a);
                }
                for (DataSnapshot d : dataSnapshot.child("Farms").getChildren()) {
                    if (fname.contains(d.getKey() + "")) {
                        fname.remove(d.getKey() + "");
                    }
                }
                if (fname.size() == 0) {
                    Intent intent = new Intent(FarmViewActivity.this, EditUserActivity.class);
                    startActivity(intent);
                    ShortCut.displayMessageToast(FarmViewActivity.this, "Oluşturulabilecek arazi yoktur. Lütfen arazi silip tekrar deneyiniz.");
                    FarmViewActivity.this.finish();
                }
                popupRecyclerViewAdapter = new PopupRecyclerViewAdapter(FarmViewActivity.this, fname, new OnClickFarmName() {
                    @Override
                    public void onClickFarmName(String farmName) {
                        edit_farm.getEdited_farm().farm_id = farmName;
                        dialogFarmName.dismiss();
                    }
                });
                popupRecyclerView.setAdapter(popupRecyclerViewAdapter);
                dialogFarmName.show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        //        final EditText editingFarmNameText;
//        Button okBtn;
//        editingFarmNameText = (EditText) dialogFarmName.findViewById(R.id.editing_farm_name);
//        okBtn = (Button) dialogFarmName.findViewById(R.id.popup_ok_btn);
//        okBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                farmName = editingFarmNameText.getText() + "";
//                dialogFarmName.dismiss();
//                edit_farm.getEdited_farm().farm_id = farmName;
//            }
//        });
//        dialogFarmName.show();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
    }

    public interface OnClickFarmName {
        public void onClickFarmName(String farmName);
    }
}