/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.onebot.s2w;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import com.onebot.s2w.Models.Author;
import com.onebot.s2w.Models.Contest;
import com.onebot.s2w.Models.Post;
import com.onebot.s2w.R;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class NewPostUploadTaskFragment extends Fragment {
    private static final String TAG = "NewPostTaskFragment";

    public interface TaskCallbacks {
        void onBitmapResized(Bitmap resizedBitmap, int mMaxDimension);
        void onPostUploaded(String error);
    }
    private Context mApplicationContext;
    private TaskCallbacks mCallbacks;
    private Bitmap selectedBitmap;
    private Bitmap thumbnail;
    private static int MAX_IMAGE_DIMENSION = 640;

    public NewPostUploadTaskFragment() {
        // Required empty public constructor
    }

    public static NewPostUploadTaskFragment newInstance() {
        return new NewPostUploadTaskFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retain this fragment across config changes.
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof TaskCallbacks) {
            mCallbacks = (TaskCallbacks) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement TaskCallbacks");
        }
        mApplicationContext = context.getApplicationContext();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    public void setSelectedBitmap(Bitmap bitmap) {
        this.selectedBitmap = bitmap;
    }

    public Bitmap getSelectedBitmap() {
        return selectedBitmap;
    }

    public void setThumbnail(Bitmap thumbnail) {
        this.thumbnail = thumbnail;
    }

    public Bitmap getThumbnail() {
        return thumbnail;
    }

    public void resizeBitmap(Uri uri, int maxDimension) {
        LoadResizedBitmapTask task = new LoadResizedBitmapTask(maxDimension);
        task.execute(uri);
    }

    public void uploadPost(Bitmap bitmap, String inBitmapPath, Bitmap thumbnail, String inThumbnailPath,
                           String inFileName, String inPostText) {
        UploadPostTask uploadTask = new UploadPostTask(bitmap, inBitmapPath, thumbnail, inThumbnailPath, inFileName, inPostText);
        uploadTask.execute();
    }

    class UploadPostTask extends AsyncTask<Void, Void, Void> {
        private WeakReference<Bitmap> bitmapReference;
        private WeakReference<Bitmap> thumbnailReference;
        private String postText;
        private String fileName;
        private String bitmapPath;
        private String thumbnailPath;

        public UploadPostTask(Bitmap bitmap, String inBitmapPath, Bitmap thumbnail, String inThumbnailPath,
                              String inFileName, String inPostText) {
            bitmapReference = new WeakReference<Bitmap>(bitmap);
            thumbnailReference = new WeakReference<Bitmap>(thumbnail);
            postText = inPostText;
            fileName = inFileName;
            bitmapPath = inBitmapPath;
            thumbnailPath = inThumbnailPath;
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(Void... params) {
            Bitmap fullSize = bitmapReference.get();
            final Bitmap thumbnail = thumbnailReference.get();
            if (fullSize == null || thumbnail == null) {
                return null;
            }
            FirebaseStorage storageRef = FirebaseStorage.getInstance();
            StorageReference photoRef = storageRef.getReferenceFromUrl("gs://" + getString(R.string.google_storage_bucket));


            Long timestamp = System.currentTimeMillis();
            final StorageReference fullSizeRef = photoRef.child(FirebaseUtil.getCurrentUserId()).child("full").child(timestamp.toString()).child(fileName + ".jpg");
            final StorageReference thumbnailRef = photoRef.child(FirebaseUtil.getCurrentUserId()).child("thumb").child(timestamp.toString()).child(fileName + ".jpg");
            Log.d(TAG, fullSizeRef.toString());
            Log.d(TAG, thumbnailRef.toString());

            ByteArrayOutputStream fullSizeStream = new ByteArrayOutputStream();
            fullSize.compress(Bitmap.CompressFormat.JPEG, 90, fullSizeStream);
            byte[] bytes = fullSizeStream.toByteArray();
            fullSizeRef.putBytes(bytes).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    final Uri fullSizeUrl = taskSnapshot.getDownloadUrl();

                    ByteArrayOutputStream thumbnailStream = new ByteArrayOutputStream();
                    thumbnail.compress(Bitmap.CompressFormat.JPEG, 70, thumbnailStream);
                    thumbnailRef.putBytes(thumbnailStream.toByteArray())
                            .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            final DatabaseReference ref = FirebaseUtil.getBaseRef();
                            DatabaseReference postsRef = FirebaseUtil.getPostsRef();
                            final String newPostKey = postsRef.push().getKey();
                            final Uri thumbnailUrl = taskSnapshot.getDownloadUrl();

                            Author author = FirebaseUtil.getAuthor();
                            if (author == null) {
                                FirebaseCrash.logcat(Log.ERROR, TAG, "Couldn't upload post: Couldn't get signed in user.");
                                mCallbacks.onPostUploaded(mApplicationContext.getString(
                                        R.string.error_user_not_signed_in));
                                return;
                            }
                            Post newPost = new Post(author, fullSizeUrl.toString(), fullSizeRef.toString(),
                                    thumbnailUrl.toString(), thumbnailRef.toString(), postText, ServerValue.TIMESTAMP);

                            // add to contest items
                            Contest contest = new Contest();
                            contest.url = newPost.getThumb_url();
                            contest.key = newPostKey;
                            contest.caption = newPost.getText();
                            ContestFragment.contestItems.add(contest);

                            Map<String, Object> updatedUserData = new HashMap<>();
                            updatedUserData.put(FirebaseUtil.getPeoplePath() + author.getUid() + "/posts/"
                                    + newPostKey, true);
                            updatedUserData.put(FirebaseUtil.getPostsPath() + newPostKey,
                                    new ObjectMapper().convertValue(newPost, Map.class));
                            ref.updateChildren(updatedUserData, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError firebaseError, DatabaseReference databaseReference) {
                                    if (firebaseError == null) {
                                        mCallbacks.onPostUploaded(null);
                                    } else {
                                        Log.e(TAG, "Unable to create new post: " + firebaseError.getMessage());
                                        FirebaseCrash.report(firebaseError.toException());
                                        mCallbacks.onPostUploaded(mApplicationContext.getString(
                                                R.string.error_upload_task_create));
                                    }
                                }
                            });
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            FirebaseCrash.logcat(Log.ERROR, TAG, "Failed to upload post to database.");
                            FirebaseCrash.report(e);
                            mCallbacks.onPostUploaded(mApplicationContext.getString(
                                    R.string.error_upload_task_create));
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    FirebaseCrash.logcat(Log.ERROR, TAG, "Failed to upload post to database.");
                    FirebaseCrash.report(e);
                    mCallbacks.onPostUploaded(mApplicationContext.getString(
                            R.string.error_upload_task_create));
                }
            });
            // TODO: Refactor these insanely nested callbacks.
            return null;
        }
    }

    class LoadResizedBitmapTask extends AsyncTask<Uri, Void, Bitmap> {
        private int mMaxDimension;

        public LoadResizedBitmapTask(int maxDimension) {
            mMaxDimension = maxDimension;
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(Uri... params) {
            Uri uri = params[0];
            if (uri != null) {
                // TODO: Currently making these very small to investigate modulefood bug.
                // Implement thumbnail + fullsize later.
                Bitmap bitmap = null;
                /*try {
                    bitmap = decodeSampledBitmapFromUri(uri, mMaxDimension, mMaxDimension);
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "Can't find file to resize: " + e.getMessage());
                    FirebaseCrash.report(e);
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred during resize: " + e.getMessage());
                    FirebaseCrash.report(e);
                }
                return bitmap;*/


                try {
                    //InputStream in = new java.net.URL(uri.toString()).openStream();
                    //bitmap = BitmapFactory.decodeStream(in);
                    bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), uri);
                    bitmap = getCorrectlyOrientedImage(getActivity(), uri);
                } catch (Exception e) {
                    Log.e("Error", e.getMessage());
                    e.printStackTrace();
                }
                return bitmap;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            mCallbacks.onBitmapResized(bitmap, mMaxDimension);
        }
    }

    public static Bitmap getCorrectlyOrientedImage(Context context, Uri photoUri) throws IOException {
        InputStream is = context.getContentResolver().openInputStream(photoUri);
        BitmapFactory.Options dbo = new BitmapFactory.Options();
        dbo.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, dbo);
        is.close();

        int rotatedWidth, rotatedHeight;
        int orientation = getOrientation(context, photoUri);

        if (orientation == 90 || orientation == 270) {
            rotatedWidth = dbo.outHeight;
            rotatedHeight = dbo.outWidth;
        } else {
            rotatedWidth = dbo.outWidth;
            rotatedHeight = dbo.outHeight;
        }

        Bitmap srcBitmap;
        is = context.getContentResolver().openInputStream(photoUri);
        if (rotatedWidth > MAX_IMAGE_DIMENSION || rotatedHeight > MAX_IMAGE_DIMENSION) {
            float widthRatio = ((float) rotatedWidth) / ((float) MAX_IMAGE_DIMENSION);
            float heightRatio = ((float) rotatedHeight) / ((float) MAX_IMAGE_DIMENSION);
            float maxRatio = Math.max(widthRatio, heightRatio);

            // Create the bitmap from file
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = (int) maxRatio;
            srcBitmap = BitmapFactory.decodeStream(is, null, options);
        } else {
            srcBitmap = BitmapFactory.decodeStream(is);
        }
        is.close();

        /*
         * if the orientation is not 0 (or -1, which means we don't know), we
         * have to do a rotation.
         */
        if (orientation > 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(orientation);

            srcBitmap = Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.getWidth(),
                    srcBitmap.getHeight(), matrix, true);
        }

        return srcBitmap;
    }

    public static int getOrientation(Context context, Uri photoUri) {
        /* it's on the external media. */
        Cursor cursor = context.getContentResolver().query(photoUri,
                new String[] { MediaStore.Images.ImageColumns.ORIENTATION }, null, null, null);

        if (cursor.getCount() != 1) {
            return -1;
        }

        cursor.moveToFirst();
        return cursor.getInt(0);
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public Bitmap decodeSampledBitmapFromUri(Uri fileUri, int reqWidth, int reqHeight)
            throws IOException {
        InputStream stream = new BufferedInputStream(
                mApplicationContext.getContentResolver().openInputStream(fileUri));
        Bitmap bm = null;
        stream.mark(stream.available());
        BitmapFactory.Options options = new BitmapFactory.Options();
        // First decode with inJustDecodeBounds=true to check dimensions
        options.inJustDecodeBounds = true;
        bm = BitmapFactory.decodeStream(stream, null, options);
        stream.reset();
        /*options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        BitmapFactory.decodeStream(stream, null, options);
        // Decode bitmap with inSampleSize set
        stream.reset();*/
        bm = BitmapFactory.decodeStream(stream, null, options);
        return bm;
    }
}
