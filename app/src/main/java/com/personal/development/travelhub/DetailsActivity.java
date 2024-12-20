package com.personal.development.travelhub;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.personal.development.travelhub.models.Destination;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class DetailsActivity extends AppCompatActivity {
    // Declare UI elements
    TextView backBtn, placeName, highlight, time_open, btnAllDatePicker, tripCount, bus_fare, whatToExpect,
            location_txtView, OtherDetails, entranceFee_txtView;
    Button saveTripBtn;
    ImageView placeImage;
    FirebaseFirestore db;
    Intent intent;

    private FirebaseAuth auth;
    private String userUid;
    private String accessVal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.details_layout);

        auth = FirebaseAuth.getInstance();
        userUid = auth.getCurrentUser().getUid();
        // Initialize views
        backBtn = findViewById(R.id.add_new_trip);
        btnAllDatePicker = findViewById(R.id.allDate);
        placeName = findViewById(R.id.place_name);
        placeImage = findViewById(R.id.place_image);
        highlight = findViewById(R.id.description_txtV);
        time_open = findViewById(R.id.open_time_details);
        tripCount = findViewById(R.id.trip_count_text);
        bus_fare = findViewById(R.id.bus_fare_Details);
        whatToExpect = findViewById(R.id.expect_details);
        location_txtView = findViewById(R.id.location_details);
        OtherDetails = findViewById(R.id.other_details);
        entranceFee_txtView = findViewById(R.id.entrance_fee_details);
        saveTripBtn = findViewById(R.id.save_trip_btn);


        // Initialize Firestore instance
        db = FirebaseFirestore.getInstance();

        // Get document ID passed from the previous activity
        String documentId = getIntent().getStringExtra("DOCUMENT_ID");
        String imageString = getIntent().getStringExtra("IMAGE_URL");
        accessVal = getIntent().getStringExtra("access");

        // Fetch place details from Firestore if document ID exists
        if (documentId != null) {
            fetchPlaceDetails(documentId);
        }

        // Set back button to return to Dashboard
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ("agency".equals(accessVal)) {
                    // If access is "agency", go back to the AgencyDashboard
                    intent = new Intent(DetailsActivity.this, AgencyDashboard.class);
                } else {
                    // If access is not "agency", go back to the Dashboard or Admin activity
                    intent = new Intent(DetailsActivity.this, Dashboard.class);
                }
                startActivity(intent);
            }
        });

        placeName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createWishlist(placeName.getText().toString(),imageString,tripCount.getText().toString(),userUid,documentId);
            }
        });

        saveTripBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(DetailsActivity.this, TravelPlanActivity.class);

                // Add extra data to the intent
                intent.putExtra("destination_name", placeName.getText().toString()); // Example of a string
                intent.putExtra("formType","0");

                // Start the activity
                startActivity(intent);

            }
        });


        // Open the date picker on button click
//        btnAllDatePicker.setOnClickListener(v -> openDateRangePicker());
    }

    private void getTripCount(String destinationName) {
        // Reference to the specific document in "saved_tripCount"
        DocumentReference tripCountDocRef = db.collection("saved_tripCount").document(destinationName);

        tripCountDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String destination = documentSnapshot.getString("destinationName");
                long count = documentSnapshot.getLong("count");

                // Save data into SharedPreferences
//                SharedPreferences sharedPreferences = getSharedPreferences("TripPreferences", MODE_PRIVATE);
//                SharedPreferences.Editor editor = sharedPreferences.edit();
//                editor.putString("destinationName", destination);
//                editor.putLong("count", count);
//                editor.apply();

                // Navigate to the next activity
                tripCount.setText(String.valueOf(count));
            } else {
                Toast.makeText(this, "No data found for this destination.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error retrieving trip count: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }



    private void createTripList(String tripDate, String reviews, String tripCounts, String userId, List<Destination> destinations) {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("tripDateFromAndTo", tripDate);
        dataMap.put("tripReviews", reviews);  // Store review or rating count
        dataMap.put("tripCounts", tripCounts);  // Trip count or order
        dataMap.put("destinations", destinations);  // Store destinations list

        saveTrip(userId, dataMap);
    }


    private void createWishlist(String place_name, String image_url, String reviews, String userId, String attraction_uid){
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("tripDescription", place_name);
        dataMap.put("imageUrl", image_url);
        dataMap.put("reviews", reviews);
        dataMap.put("attraction_uid", attraction_uid);

        saveWishlistData(userId, dataMap);
    }

    private void saveWishlistData(String userId,Map<String, Object> dataMap){
        CollectionReference wishlistRef = db.collection("users").document(userId).collection("wishlist");

        wishlistRef.add(dataMap)
                .addOnSuccessListener(documentReference -> Toast.makeText(DetailsActivity.this, "Saved to wishlist!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(DetailsActivity.this,  "Error submitting data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void saveTrip(String userId, Map<String, Object> dataMap) {
        CollectionReference tripsRef = db.collection("users").document(userId).collection("trips");

        tripsRef.add(dataMap)
                .addOnSuccessListener(documentReference ->
                        Toast.makeText(DetailsActivity.this, "Saved to trips!", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(DetailsActivity.this, "Error submitting data: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // Fetch place details from Firestore using document ID
    private void fetchPlaceDetails(String documentId) {
        db.collection("attractions").document(documentId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            // Extract data from Firestore document
                            String name = documentSnapshot.getString("destination_name");
                            String imageUrl = documentSnapshot.getString("image_link_1");
                            String highlightData = documentSnapshot.getString("highlight");
                            String time = documentSnapshot.getString("time");
                            String busFare = documentSnapshot.getString("bus_fare");
                            String what_to_expect = documentSnapshot.getString("what_to_expect");
                            String location = documentSnapshot.getString("location");
                            String other_details = documentSnapshot.getString("other_details");
                            String entranceFee = documentSnapshot.getString("entrance_fee");


                            placeName.setText(name);
                            highlight.setText(highlightData);
                            time_open.setText(time);
                            bus_fare.setText("Bus Fare: "+busFare);
                            whatToExpect.setText(what_to_expect);
                            location_txtView.setText(location);
                            OtherDetails.setText("Other Details: "+ other_details);
                            entranceFee_txtView.setText("Entrance Fee: "+ entranceFee);

                            getTripCount(name);

                            Glide.with(DetailsActivity.this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.default_picture)  // Default image placeholder
                                    .into(placeImage);
                        } else {
                            Toast.makeText(DetailsActivity.this, "No data found", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(DetailsActivity.this, "Error fetching data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Open a Date Range Picker
    private void openDateRangePicker() {
        // Build the DateRangePicker with constraints to block past dates
        MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker();
        builder.setTitleText("Select a Date Range");

        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();
        constraintsBuilder.setValidator(DateValidatorPointForward.now());  // Only future dates allowed
        builder.setCalendarConstraints(constraintsBuilder.build());

        // Build and show the date picker dialog
        MaterialDatePicker<Pair<Long, Long>> materialDatePicker = builder.build();
        materialDatePicker.show(getSupportFragmentManager(), "DATE_PICKER");

        // Handle the result when a date range is selected
        materialDatePicker.addOnPositiveButtonClickListener(
                (MaterialPickerOnPositiveButtonClickListener<? super Pair<Long, Long>>) selection -> {

                    // Format the selected dates
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    Calendar calendarStart = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    calendarStart.setTimeInMillis(selection.first);

                    Calendar calendarEnd = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    calendarEnd.setTimeInMillis(selection.second);

                    // Display the formatted date range
                    String startDate = sdf.format(calendarStart.getTime());
                    String endDate = sdf.format(calendarEnd.getTime());

                    // Set the text on the button to show selected date range
                    btnAllDatePicker.setText("from " + startDate + " to " + endDate);
                });
    }
}
