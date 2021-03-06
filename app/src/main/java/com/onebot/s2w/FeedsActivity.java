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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.onebot.s2w.Models.Contest;
import com.onebot.s2w.Models.ContestComparator;
import com.onebot.s2w.Models.Post;
import com.onebot.s2w.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FeedsActivity extends AppCompatActivity implements PostsFragment.OnPostSelectedListener {
    private static final String TAG = "FeedsActivity";
    private FloatingActionButton mFab;

    public static ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feeds);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        viewPager = (ViewPager) findViewById(R.id.feeds_view_pager);
        FeedsPagerAdapter adapter = new FeedsPagerAdapter(getSupportFragmentManager());
        //adapter.addFragment(PostsFragment.newInstance(PostsFragment.TYPE_HOME), "HOME");
        adapter.addFragment(ContestFragment.newInstance(), "CONTEST");
        adapter.addFragment(PostsFragment.newInstance(PostsFragment.TYPE_FEED), "FEED");
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(1);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.feeds_tab_layout);
        tabLayout.setupWithViewPager(viewPager);

        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                //if (user == null || user.isAnonymous()) {
                if (user == null) {
                    Toast.makeText(FeedsActivity.this, "You must sign-in to post.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent newPostIntent = new Intent(FeedsActivity.this, NewPostActivity.class);
                startActivity(newPostIntent);
            }
        });
    }

    @Override
    public void onPostComment(String postKey) {
        Intent intent = new Intent(this, CommentsActivity.class);
        intent.putExtra(CommentsActivity.POST_KEY_EXTRA, postKey);
        startActivity(intent);
    }

    @Override
    public void onFullscreenPost(final String postKey) {
        final DatabaseReference postsRef = FirebaseUtil.getPostsRef();
        //DatabaseReference child = postsRef.child(postKey).child("full_storage_uri");
        DatabaseReference child = postsRef.child(postKey).child("full_url");

        child.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    //String uri = (String)dataSnapshot.getValue();
                    String url = (String)dataSnapshot.getValue();
                    Intent fullScreenIntent = new Intent(getApplicationContext(), FullscreenActivity.class);
                    fullScreenIntent.putExtra("URL", url);
                    //fullScreenIntent.setData(Uri.parse(uri));
                    startActivity(fullScreenIntent);
                } else {
                    return;
                }
            }

            @Override
            public void onCancelled(DatabaseError firebaseError) {

            }
        });
    }

    @Override
    public void onPostLike(final String postKey) {
        final String userKey = FirebaseUtil.getCurrentUserId();
        final DatabaseReference postLikesRef = FirebaseUtil.getLikesRef();
        postLikesRef.child(postKey).child(userKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // User already liked this post, so we toggle like off.
                    postLikesRef.child(postKey).child(userKey).removeValue();
                } else {
                    postLikesRef.child(postKey).child(userKey).setValue(ServerValue.TIMESTAMP);
                }
                // update contest items
                updateContestItems(dataSnapshot, postKey);
            }

            @Override
            public void onCancelled(DatabaseError firebaseError) {

            }
        });
    }

    private void updateContestItems(DataSnapshot ds, String key) {
        for(int i=0; i<ContestFragment.contestItems.size(); i++) {
            Contest item = ContestFragment.contestItems.get(i);
            if (item.key.equals(key)) {
                if (ds.exists())
                    item.numLikes--;
                else
                    item.numLikes++;
                Collections.sort(ContestFragment.contestItems, new ContestComparator());
                ContestFragment.contestItems.set(i, item);
                break;
            }

        }
        ContestFragment.syncItems();
    }

    private void showDeleteAlert(final DatabaseReference dbr) {

        AlertDialog.Builder builder;
        final boolean result = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle("Delete Post")
                .setMessage("Are you sure you want to delete this post?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                        dbr.removeValue();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();

    }

    @Override
    public void onPostDelete(final String postKey) {
        final String userKey = FirebaseUtil.getCurrentUserId();

        final DatabaseReference postsRef = FirebaseUtil.getPostsRef();

        showDeleteAlert(postsRef.child(postKey));

        /*postsRef.child(postKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Post post = (Post)dataSnapshot.getValue(Post.class));
                    String userId = FirebaseUtil.getCurrentUserId();

                    if (post.getAuthor().equals(userId)) {
                        showDeleteAlert(postsRef.child(postKey));
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError firebaseError) {

            }
        });*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_feeds, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            // TODO: Add settings screen.
            return true;
        } else if (id == R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    class FeedsPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public FeedsPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }
}
