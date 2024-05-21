package com.example.savorsayapplication.utils;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseUtils {


    public static void deleteActiveSessions(String uid) {

        DatabaseReference activeSessionsRef = FirebaseDatabase.getInstance().getReference("active_sessions").child(uid);

        // Remove the active_sessions data for the user
        activeSessionsRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    // Data removed successfully
                    Log.d("FirebaseUtils", "Active sessions data deleted for user with UID: " + uid);
                })
                .addOnFailureListener(e -> {
                    // Failed to remove data
                    Log.e("FirebaseUtils", "Error deleting active sessions data: " + e.getMessage());
                });
    }


    public static void signOut() {
        FirebaseAuth.getInstance().signOut();
    }
}
