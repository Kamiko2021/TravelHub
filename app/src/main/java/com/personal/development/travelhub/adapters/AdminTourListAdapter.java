package com.personal.development.travelhub.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.personal.development.travelhub.R;
import com.personal.development.travelhub.TravelsActivity;
import com.personal.development.travelhub.models.AddTourModel;

import java.util.ArrayList;
import java.util.List;

public class AdminTourListAdapter extends RecyclerView.Adapter<AdminTourListAdapter.TourViewHolder> {

    private Context context;
    private List<AddTourModel> tourList;
    private FirebaseFirestore db;

    public AdminTourListAdapter(Context context){
        this.context = context;
        this.tourList = new ArrayList<>();
        this.db = FirebaseFirestore.getInstance();
        fetchActivitiesFromFirestore();
    }

    @NonNull
    @Override
    public TourViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item layout
        View view = LayoutInflater.from(context).inflate(R.layout.tour_list_items, parent, false);
        return new TourViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminTourListAdapter.TourViewHolder holder, int position) {
        AddTourModel currentItem = tourList.get(position);
        holder.tourName.setText(currentItem.getTourName());  // Display the tour name
        holder.listDestination.setText(""); // Clear destination text initially

        holder.viewBtn.setOnClickListener(v -> {
            Intent intent = new Intent(context,TravelsActivity.class);
            intent.putExtra("tour_name",currentItem.getTourName());
            intent.putExtra("access", "agency");
            context.startActivity(intent);
        });

        holder.removeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                String tourName = currentItem.getTourName(); // Assuming currentItem has a method to get the tour name

                // Query Firestore for the document with the matching tour name
                db.collection("tour_package")
                        .whereEqualTo("tourName", tourName)
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && task.getResult() != null) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    // Get the document ID
                                    String documentId = document.getId();

                                    // Delete the document
                                    db.collection("tour_package").document(documentId)
                                            .delete()
                                            .addOnSuccessListener(aVoid -> {
                                                // Remove the item from the list
                                                int index = holder.getAdapterPosition();
                                                if (index != RecyclerView.NO_POSITION) {
                                                    tourList.remove(index);
                                                    notifyItemRemoved(index); // Notify the adapter
                                                }
                                                Toast.makeText(v.getContext(), "Tour package removed successfully!", Toast.LENGTH_SHORT).show();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(v.getContext(), "Failed to remove tour package.", Toast.LENGTH_SHORT).show();
                                            });
                                }
                            } else {
                                Toast.makeText(v.getContext(), "No matching tour package found.", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });


        Glide.with(context)
                .load(currentItem.getImage_link_1())
                .placeholder(R.drawable.default_picture)
                .error(R.drawable.error_icon)
                .into(holder.tourImage);
        // Call fetchDestinationList with holder and tourName
        fetchDestinationList(holder, currentItem.getTourName());
    }

    @Override
    public int getItemCount() {
        return tourList.size();
    }

    private void fetchActivitiesFromFirestore(){
        CollectionReference tourPackageList = db.collection("tour_package");

        tourPackageList.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()){
                        List<AddTourModel> fetchedItems = queryDocumentSnapshots.toObjects(AddTourModel.class);
                        tourList.clear();
                        tourList.addAll(fetchedItems);
                        notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Modify the method to accept TourViewHolder and tourName
    private void fetchDestinationList(TourViewHolder holder, String tourName) {
        // Get all tour package documents
        CollectionReference tourPackageListRef = db.collection("tour_package");

        // Query the collection
        tourPackageListRef.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        StringBuilder destinationNames = new StringBuilder();

                        // Iterate over each document in the query result
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            // Get the document ID (which is the tourName in this case)
                            String documentId = document.getId(); // This is the document ID

                            // If the documentId matches the tourName, fetch the destination_list subcollection
                            if (documentId.equals(tourName)) {
                                // Fetch the destination_list subcollection for this specific document
                                CollectionReference destinationListRef = document.getReference().collection("destination_list");

                                // Query the destination_list subcollection
                                destinationListRef.get()
                                        .addOnSuccessListener(destinationSnapshots -> {
                                            if (!destinationSnapshots.isEmpty()) {
                                                // Iterate through each destination document
                                                for (QueryDocumentSnapshot destinationDocument : destinationSnapshots) {
                                                    // Get the destination_name field
                                                    String destinationName = destinationDocument.getString("destination_name");
                                                    if (destinationName != null) {
                                                        // Append destination name to the StringBuilder
                                                        destinationNames.append(destinationName).append("\n");
                                                    } else {
                                                        // Log if destination_name is null
                                                        Log.d("destination list", "destination_name is null for document: " + destinationDocument.getId());
                                                    }
                                                }
                                            } else {
                                                // If no destinations are found
                                                destinationNames.append("No destinations available.").append("\n");
                                            }
                                            // Update the TextView with the destination names
                                            holder.listDestination.setText(destinationNames.toString());
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("fetchDestinationList", "Error fetching destinations: " + e.getMessage());
                                            Toast.makeText(context, "Error fetching destinations: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            }
                        }
                    } else {
                        Log.d("fetchDestinationList", "No tour packages found.");
                        Toast.makeText(context, "No tour packages found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("fetchDestinationList", "Error fetching tour packages: " + e.getMessage());
                    Toast.makeText(context, "Error fetching tour packages: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }




    public static class TourViewHolder extends RecyclerView.ViewHolder {
        TextView tourName, listDestination;
        ImageView tourImage;
        Button viewBtn, removeBtn;

        public TourViewHolder(View itemView){
            super(itemView);
            // Correct view binding
            tourName = itemView.findViewById(R.id.tour_name);
            listDestination = itemView.findViewById(R.id.list_destination);
            tourImage = itemView.findViewById(R.id.tourImageView);
            viewBtn = itemView.findViewById(R.id.view_btn);
            removeBtn = itemView.findViewById(R.id.remove_btn);
        }
    }
}
