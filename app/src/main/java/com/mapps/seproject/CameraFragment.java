package com.mapps.seproject;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

/**
 * Created by kishore on 3/7/2017.
 */

public class CameraFragment extends Fragment {


    String UserEmail = null;
    final DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("users");

    private static final String IMAGE_DIRECTORY_NAME = "Hello_Camera";
    View view;
    private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
    public static final int MEDIA_TYPE_IMAGE = 1;
    private static int RESULT_LOAD_IMAGE = 2;
    private Uri fileUri;
    private Uri imageUri;
    public static Uri images;
    private Button btnCapturePicture;
    private Button choose_image;
    StorageReference mStorageRef;


    FirebaseUser firebaseUser;
    FirebaseAuth firebaseAuth;

    private com.mapps.seproject.TrackGPS gps;
    double longitude;
    double latitude;
    String city;
    String postalCode;

    final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("feed");

    String UID="";
    int flag = 0;




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_camera, container, false);
        firebaseAuth = FirebaseAuth.getInstance();

        firebaseUser = firebaseAuth.getCurrentUser();
        UID = firebaseUser.getUid();

        final String data = firebaseAuth.getCurrentUser().getEmail();

        mStorageRef = FirebaseStorage.getInstance().getReference();
        btnCapturePicture = (Button) view.findViewById(R.id.btnCapturePicture);


        choose_image = (Button) view.findViewById(R.id.choose_image);
        btnCapturePicture.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // capture picture
                captureImage();
            }
        });
        choose_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent cameraIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                startActivityForResult(cameraIntent,RESULT_LOAD_IMAGE);
            }
        });



        return view;
    }


    @Override
    public void onViewCreated(View view,Bundle savedInstanceState){
        super.onViewCreated(view,savedInstanceState);
        getActivity().setTitle("Camera Utilities");
    }

    private boolean isDeviceSupportCamera() {
        if (getActivity().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    private void captureImage() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

       // fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
        fileUri = FileProvider.getUriForFile(getActivity(), "com.mapps.seproject.provider", getOutputMediaFile(MEDIA_TYPE_IMAGE));

        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

        // start the image capture Intent
        startActivityForResult(intent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        // if the result is capturing Image
        if (requestCode == CAMERA_CAPTURE_IMAGE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                uploadFile();
                gps = new TrackGPS(getActivity());


                if(gps.canGetLocation()){


                    longitude = gps.getLongitude();
                    latitude = gps .getLatitude();
                    city = gps.getCity();
                    postalCode = gps.getPostalCode();
                    Toast.makeText(getActivity(), city, Toast.LENGTH_SHORT).show();
    /*                mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            for(DataSnapshot snapshot : dataSnapshot.getChildren()){

                                UserEmail = snapshot.child("email").toString().split("value =")[1].split(" ")[1].replaceAll("\\s+","");
                                if(UserEmail.equals(data))  {

                                    snapshot.getRef().child("location").setValue(city);
                                    Toast.makeText(getActivity(), UserEmail, Toast.LENGTH_SHORT).show();
                                    Log.e("Location:",city);

                                    break;
                                }



                            }

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });*/

                    // Toast.makeText(getActivity(),"Longitude:"+Double.toString(longitude)+"\nLatitude:"+Double.toString(latitude)+"\nCity:"+city+"\nPostal:"+postalCode,Toast.LENGTH_SHORT).show();
                }
                else
                {

                    gps.showSettingsAlert();
                }
            } else if (resultCode == RESULT_CANCELED) {
                // user cancelled Image capture
                Toast.makeText(getActivity(),
                        "User cancelled image capture", Toast.LENGTH_SHORT)
                        .show();
            } else {
                // failed to capture image
                Toast.makeText(getActivity(),
                        "Sorry! Failed to capture image", Toast.LENGTH_SHORT)
                        .show();
            }
        }
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && data != null) {

            imageUri = data.getData();
            Toast.makeText(getActivity().getBaseContext(), "Image Added. Press Compose to transfer to Mail Window", Toast.LENGTH_SHORT).show();
            uploadFiles();
        }

    }






    private static File getOutputMediaFile(int type) {



        // External sdcard location
        File mediaStorageDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                IMAGE_DIRECTORY_NAME);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(IMAGE_DIRECTORY_NAME, "Oops! Failed create "
                        + IMAGE_DIRECTORY_NAME + " directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "IMG_" + timeStamp + ".jpg");
        } else {
            return null;
        }

        return mediaFile;
    }



    private void uploadFile() {
        //if there is a file to upload
        if (fileUri != null) {
            //displaying a progress dialog while upload is going on
            final ProgressDialog progressDialog = new ProgressDialog(getActivity());
            progressDialog.setTitle("Uploading");
            progressDialog.show();

            Long tsLong = System.currentTimeMillis()/1000;
            final String timeStamp = tsLong.toString();

            StorageReference riversRef = mStorageRef.child("images/"+ timeStamp+"pic.jpg");
            riversRef.putFile(fileUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @SuppressWarnings("VisibleForTests")
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //if the upload is successfull
                            //hiding the progress dialog
                            progressDialog.dismiss();

                            //and displaying a success toast
                            Toast.makeText(getActivity().getBaseContext(), "File Uploaded ", Toast.LENGTH_LONG).show();
                            final Uri downloadUri = taskSnapshot.getDownloadUrl();
                            final String downloadUrl = downloadUri.toString();

                            databaseReference.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {



                                    if(flag == 0) {


                                        databaseReference.child(UID).child("feed").child(String.valueOf(MainActivity.individualIds)).child("image").setValue(downloadUrl);
                                        databaseReference.child(UID).child("feed").child(String.valueOf(MainActivity.individualIds)).child("timeStamp").setValue(timeStamp);
                                        databaseReference.child("feed").child(String.valueOf(MainActivity.ids)).child("image").setValue(downloadUrl);
                                        databaseReference.child("feed").child(String.valueOf(MainActivity.ids)).child("timeStamp").setValue(timeStamp);
                                        flag = 1;
                                    }

                                }


                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            //if the upload is not successfull
                            //hiding the progress dialog
                            progressDialog.dismiss();

                            //and displaying error message
                            Toast.makeText(getActivity().getBaseContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @SuppressWarnings("VisibleForTests")
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            //calculating progress percentage

                            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

                            //displaying percentage in progress dialog
                            progressDialog.setMessage("Uploaded " + ((int) progress) + "%...");
                        }
                    });
        }
        //if there is not any file
        else {
            //you can display an error toast
        }
    }

    private void uploadFiles() {
        //if there is a file to upload
        if (imageUri != null) {
            //displaying a progress dialog while upload is going on
            final ProgressDialog progressDialog = new ProgressDialog(getActivity());
            progressDialog.setTitle("Uploading");
            progressDialog.show();

            Long tsLong = System.currentTimeMillis()/1000;
            final String timeStamp = tsLong.toString();

            StorageReference riversRef = mStorageRef.child("images/"+ timeStamp+"pic.jpg");
            riversRef.putFile(imageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @SuppressWarnings("VisibleForTests")
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //if the upload is successfull
                            //hiding the progress dialog
                            progressDialog.dismiss();

                            //and displaying a success toast
                            Toast.makeText(getActivity().getBaseContext(), "File Uploaded ", Toast.LENGTH_LONG).show();
                            final Uri downloadUri = taskSnapshot.getDownloadUrl();
                            final String downloadUrl = downloadUri.toString();

                            databaseReference.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {

                                    if(flag == 0) {


                                        databaseReference.child(UID).child("feed").child(String.valueOf(MainActivity.individualIds)).child("image").setValue(downloadUrl);
                                        databaseReference.child(UID).child("feed").child(String.valueOf(MainActivity.individualIds)).child("timeStamp").setValue(timeStamp);
                                        databaseReference.child("feed").child(String.valueOf(MainActivity.ids)).child("image").setValue(downloadUrl);
                                        databaseReference.child("feed").child(String.valueOf(MainActivity.ids)).child("timeStamp").setValue(timeStamp);
                                        flag = 1;
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            //if the upload is not successfull
                            //hiding the progress dialog
                            progressDialog.dismiss();

                            //and displaying error message
                            Toast.makeText(getActivity().getBaseContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @SuppressWarnings("VisibleForTests")
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            //calculating progress percentage

                            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

                            //displaying percentage in progress dialog
                            progressDialog.setMessage("Uploaded " + ((int) progress) + "%...");
                        }
                    });
        }
        //if there is not any file
        else {
            //you can display an error toast
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // save file url in bundle as it will be null on scren orientation
        // changes
        outState.putParcelable("file_uri", fileUri);
    }


    /*
    @Override
    public void onStart(){
        super.onStart();
        dispatchTakePictureIntent();

    }
    */
}
