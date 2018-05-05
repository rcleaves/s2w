package com.onebot.s2w;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.onebot.s2w.Models.Contest;
import com.onebot.s2w.Models.ContestComparator;
import com.onebot.s2w.Models.Post;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


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

    private static long mostLikes = 0;
    private void setMostLikes(long l) { mostLikes = l; }
    private long getMostLikes() { return mostLikes; }

    private static String mostLikedPost = "";
    private void setMostLikedPost(String s ) { mostLikedPost = s; }
    private String getMostLikedPost() { return mostLikedPost; }

    private View mView;

    public static ContestItemsAdapter adapter;
    public static ArrayList<Contest> contestItems = new ArrayList<Contest>();

    //private OnFragmentInteractionListener mListener;

    public ContestFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ContestFragment.
     */
    // TODO: Rename and change types and number of parameters
    /*public static ContestFragment newInstance(String param1, String param2) {
        ContestFragment fragment = new ContestFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }*/

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
        setupFollowers();
        //Log.d(TAG, "most liked: " + getMostLikedPost() + ", likes: " + getMostLikes());
    }

    private static int CONTEST_LIST_SIZE = 12;

    public static void syncItems() {
        // remove dups
        /*HashSet<Contest> hashSet = new HashSet<Contest>();
        hashSet.addAll(contestItems);
        contestItems.removeAll(contestItems);
        contestItems.clear();
        contestItems.addAll(hashSet);*/


        // truncate
        ArrayList<Contest> keep = null;
        if (contestItems.size() > CONTEST_LIST_SIZE) {
            keep = new ArrayList<>(contestItems.subList(0, CONTEST_LIST_SIZE));
            keep = removeDups(keep);
            // sort and redraw
            Collections.sort(keep, new ContestComparator());
            adapter.clear();
            adapter.addAll(keep);
            //adapter = new ContestItemsAdapter(mCtx, keep);
        }

        adapter.notifyDataSetChanged();
    }

    private static Context mCtx;

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null)
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() != null)
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        mView = inflater.inflate(R.layout.fragment_contest, container, false);

        mCtx = getContext();

        contestItems = removeDups(contestItems);

        adapter = new ContestItemsAdapter(mCtx, contestItems);

        syncItems();

        ListView list = (ListView)mView.findViewById(R.id.contest_list);
        list.setAdapter(adapter);

        return mView;
    }

    private static ArrayList<Contest> removeDups(ArrayList<Contest> items) {
        ArrayList<Contest> result = new ArrayList<>();

        HashMap<String, Contest> map = new HashMap<>();
        for(int i=0; i<items.size(); i++) {
            Contest item = items.get(i);
            map.put(item.key, item);
        }

        Iterator iterator = map.entrySet().iterator();
        while(iterator.hasNext()) {
            Map.Entry pair = (Map.Entry)iterator.next();
            result.add((Contest)pair.getValue());
        }

        return result;
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
                                if (getMostLikes() < count ) {
                                    setMostLikes(count);
                                    setMostLikedPost(postId);
                                }
                                // add all to contest items
                                getContestPost(postId, count);
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

    private int CONTEST_COUNT = 5;

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

    private void getContestPost(final String id, final long numLikes) {
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
                        Post post = item.getValue(Post.class);

                        // add to contest item list
                        Contest contest = new Contest();
                        contest.url = post.getThumb_url();
                        contest.numLikes = numLikes;
                        contest.caption = post.getText();
                        contest.key = key;
                        contestItems.add(contest);
                        break;
                    }
                    count++;
                }
                // sort and truncate contest items
                syncItems();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

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

        Log.d(TAG, "contest items: " + contestItems.toString());

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

    private void setupFollowers() {
        FirebaseUtil.getCurrentUserRef().child("following").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(final DataSnapshot followedUserSnapshot, String s) {
                String followedUserId = followedUserSnapshot.getKey();
                String lastKey = "";
                if (followedUserSnapshot.getValue() instanceof String) {
                    lastKey = followedUserSnapshot.getValue().toString();
                }
                Log.d(TAG, "followed user id: " + followedUserId);
                Log.d(TAG, "last key: " + lastKey);
                FirebaseUtil.getPeopleRef().child(followedUserId).child("posts")
                        .orderByKey().startAt(lastKey).addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(final DataSnapshot postSnapshot, String s) {
                        HashMap<String, Object> addedPost = new HashMap<String, Object>();
                        addedPost.put(postSnapshot.getKey(), true);
                        FirebaseUtil.getFeedRef().child(FirebaseUtil.getCurrentUserId())
                                .updateChildren(addedPost).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                FirebaseUtil.getCurrentUserRef().child("following")
                                        .child(followedUserSnapshot.getKey())
                                        .setValue(postSnapshot.getKey());
                            }
                        });
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }
}
