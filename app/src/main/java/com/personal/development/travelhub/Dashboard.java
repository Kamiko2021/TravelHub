package com.personal.development.travelhub;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.personal.development.travelhub.adapters.AttractionAdapter;
import com.personal.development.travelhub.adapters.HomeAdapter;
import com.personal.development.travelhub.models.AttractionsModel;
import com.personal.development.travelhub.models.CardModel;

import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;


import java.util.ArrayList;
import java.util.List;

public class Dashboard extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private RecyclerView reco_recyclerView;
    private HomeAdapter adapter;
    private AttractionAdapter adapter2;
    private List<CardModel> dataList;
    private List<AttractionsModel> dataList2;
    private BottomNavigationView bottomNavigationView;
    private String user_interest;
    private TextView addNewTrip,saveTripCount;
    private TextView add_btn_;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.recommended_recycler_view);
        reco_recyclerView = findViewById(R.id.attractions_recyclerView);
        bottomNavigationView = findViewById(R.id.bottom_navigation_view);
        addNewTrip = findViewById(R.id.add_new_trip);


        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        addNewTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSelectNumDaysDialog(); // Call the dialog method
            }
        });


        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_wishlist) {
                startActivity(new Intent(this, Wishlist.class));
                return true;
            } else if (itemId == R.id.nav_trip) {
                startActivity(new Intent(this, TripsActivity.class));
                return true;
            } else if (itemId == R.id.nav_account) {
                startActivity(new Intent(this, Profile.class));
                return true;
            } else {
                return false;
            }
        });

        // Create separate layout managers for each RecyclerView
        LinearLayoutManager layoutManager1 = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
//        LinearLayoutManager layoutManager2 = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false); // Vertical layout

        // Set the layout managers to different RecyclerViews
        reco_recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setLayoutManager(layoutManager1);
//        reco_recyclerView.setLayoutManager(layoutManager2);

        // Set up data and adapters
        dataList = new ArrayList<>();
//        dataList.add(new CardModel("https://drive.google.com/uc?export=view&id=1OgDYv9jxQcKwJmTQUDzQLZ1nJLUZ4I9-", "Airplane"));
//        dataList.add(new CardModel("https://drive.google.com/uc?export=view&id=1_jK0fXLw-zqnYeiqrS09PEDMbddh47ab", "Magpayong Rock"));

        dataList2 = new ArrayList<>();
//        dataList2.add(new AttractionsModel("https://drive.google.com/uc?export=view&id=1Xwr2iJTFxsV7xnGGxl6Xi4irwLzAsnPx", "WHITE BEACH, SAAVEDRA",
//                "https://drive.google.com/uc?export=view&id=1g79NkbSd6gVCR-OJH0syP9PY9qcLlu9h", "PANAGSAMA BEACH, BASDIOT"));

        adapter = new HomeAdapter(this,dataList);
        adapter2 = new AttractionAdapter(this,dataList2);

        recyclerView.setAdapter(adapter);
        reco_recyclerView.setAdapter(adapter2);

        fetchInterest();
        fetchRecommendedData();
    }

    public void fetchInterest(){
        FirebaseUser user = mAuth.getCurrentUser();
        String userUid = user.getUid().toString();

        db.collection("users")
                .document(userUid)
                .get()
                .addOnCompleteListener(task -> {
                   if (task.isSuccessful()){
                       DocumentSnapshot documentSnapshot = task.getResult();
                        user_interest = documentSnapshot.getString("interest");
                   }
                });
    }

    private void fetchRecommendedData() {

        db.collection("attractions")
                .get()
                .addOnCompleteListener(task -> {
                   if (task.isSuccessful()){
                       dataList.clear();
                       dataList2.clear();
                      for (QueryDocumentSnapshot document : task.getResult()) {
                          String interest = document.getString("recommend_interest");
                          String imageUrl = document.getString("image_link_1");
                          String title = document.getString("destination_name");
                          String documentId = document.getId();

                          if (interest.equals(user_interest)){
                              dataList.add(new CardModel(imageUrl,title, documentId));
                          }
                          if (!interest.equals(user_interest)) {
                              dataList2.add(new AttractionsModel(imageUrl, title, documentId));
                          }
                      }
                       adapter.notifyDataSetChanged();
                   }
                });
    }

    private void openSelectNumDaysDialog() {
        // Inflate the custom layout
        LayoutInflater inflater = LayoutInflater.from(this);
        View viewSelectNumDays = inflater.inflate(R.layout.select_num_days, null);

        // Find views in the inflated layout
        TextView closeButton = viewSelectNumDays.findViewById(R.id.close_button);
        Spinner selectDaysSpinner = viewSelectNumDays.findViewById(R.id.select_days_spinner);
        Button generateButton = viewSelectNumDays.findViewById(R.id.generate_btn);

        // Populate Spinner with numbers (1 to 30 days)
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        for (int i = 1; i <= 3; i++) {
            adapter.add(String.valueOf(i));
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        selectDaysSpinner.setAdapter(adapter);

        // Build the AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(viewSelectNumDays);
        builder.setCancelable(true);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Close button functionality
        closeButton.setOnClickListener(v -> dialog.dismiss());

        // Generate button functionality
        generateButton.setOnClickListener(v -> {
            String selectedDays = selectDaysSpinner.getSelectedItem().toString();
            Toast.makeText(this, "Selected Days: " + selectedDays, Toast.LENGTH_SHORT).show();

            // You can call a method to generate the itinerary here

            generateItinerary(Integer.parseInt(selectedDays));

            dialog.dismiss();
        });
    }

    // Dummy method for generating itinerary
    private void generateItinerary(int days) {
        // Logic for itinerary generation can be added here
        Toast.makeText(this, "Generating itinerary for " + days + " days", Toast.LENGTH_SHORT).show();
    }

}