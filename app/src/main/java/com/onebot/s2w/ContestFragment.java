package com.onebot.s2w;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.onebot.s2w.Models.Post;

import java.util.Iterator;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ContestFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ContestFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ContestFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private final String TAG = "ContestFragment";

    //private OnFragmentInteractionListener mListener;

    public ContestFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ContestFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ContestFragment newInstance(String param1, String param2) {
        ContestFragment fragment = new ContestFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public static ContestFragment newInstance() {
        ContestFragment fragment = new ContestFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        findMostLikes();
        Log.d(TAG, "most liked: " + getMostLikedPost() + ", likes: " + getMostLikes());
    }

    private View mView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        mView = inflater.inflate(R.layout.cont_layout, container, false);
        return mView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        /*if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }*/
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        /*if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }*/
    }

    @Override
    public void onDetach() {
        super.onDetach();
        ///mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    private static long mostLikes = 0;
    private void setMostLikes(long l) { mostLikes = l; }
    private long getMostLikes() { return mostLikes; }

    private static String mostLikedPost = "";
    private void setMostLikedPost(String s ) { mostLikedPost = s; }
    private String getMostLikedPost() { return mostLikedPost; }

    private void findMostLikes(){

        try {
            FirebaseUtil.getBaseRef().child("likes")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Iterator<DataSnapshot> likedPosts = dataSnapshot.getChildren().iterator();
                            while(likedPosts.hasNext()) {
                                DataSnapshot ds = likedPosts.next();
                                String postId = ds.getKey();
                                long count = ds.getChildrenCount();
                                if (getMostLikes()< count ) {
                                    setMostLikes(count);
                                    setMostLikedPost(postId);
                                }
                            }
                            getPost(getMostLikedPost());
                            updateUI();
                        }
                        @Override
                        public void onCancelled(DatabaseError firebaseError) {

                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "caught: " + e);
        }
    }

    private Post mostLiked;

    private int mostLikePosition;

    /*private void getPostPosition(final String id) {
        DatabaseReference ref = FirebaseUtil.getPostsRef();
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterator<DataSnapshot> iter = dataSnapshot.getChildren().iterator();
                int count = 0;
                while(iter.hasNext()) {
                    count++;
                    DataSnapshot item = iter.next();
                    String key = item.getKey();
                    if (key.equals(id)) {
                        mostLikePosition = count;
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }*/

    private void getPost(final String id) {
        DatabaseReference ref = FirebaseUtil.getPostsRef();
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterator<DataSnapshot> iter = dataSnapshot.getChildren().iterator();
                int count = 0;
                while(iter.hasNext()) {
                    DataSnapshot item = iter.next();
                    String key = item.getKey();
                    if (key.equals(id)) {
                        mostLiked = item.getValue(Post.class);
                        mostLikePosition = count;
                        break;
                    }
                    count++;
                }
                if (mostLiked != null)
                    updateUI();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void updateUI() {

        ImageView photo = (ImageView)mView.findViewById(R.id.photo);
        TextView text = (TextView)mView.findViewById(R.id.text);
        TextView likesText = (TextView)mView.findViewById(R.id.num_likes);
        if (mostLiked != null) {
            GlideUtil.loadImage(mostLiked.getThumb_url(), photo);
            text.setText(mostLiked.getText());
            likesText.setText(Long.toString(getMostLikes()));
        }

        // click goes to post
        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "most like position: " + mostLikePosition);
                PostsFragment.mRecyclerView.scrollToPosition(mostLikePosition);
                FeedsActivity.viewPager.setCurrentItem(1);
            }
        });

    }
}
