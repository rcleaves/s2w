package com.onebot.s2w;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.onebot.s2w.Models.Contest;
import com.onebot.s2w.Models.Post;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class ContestItemsAdapter extends ArrayAdapter<Contest> {

    private Context mContext;
    private List<Contest> contestList = new ArrayList<>();

    public ContestItemsAdapter(@NonNull Context context, ArrayList<Contest> list) {
        super(context, 0 , list);
        mContext = context;
        contestList = list;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if(listItem == null)
            listItem = LayoutInflater.from(mContext).inflate(R.layout.contest_item,parent,false);

        final Contest currentContest = contestList.get(position);

        ImageView image = (ImageView)listItem.findViewById(R.id.contest_photo);
        GlideUtil.loadImage(currentContest.url, image);

        TextView caption = (TextView) listItem.findViewById(R.id.contest_text);
        caption.setText(currentContest.caption);

        TextView numLikes = (TextView) listItem.findViewById(R.id.contest_num_likes);
        numLikes.setText(Long.toString(currentContest.numLikes));

        // click goes to post
        listItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setPostPosition(currentContest.key);
            }
        });

        return listItem;
    }

    private void setPostPosition(final String id) {
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
                        PostsFragment.mRecyclerView.scrollToPosition(count);
                        FeedsActivity.viewPager.setCurrentItem(1);
                        break;
                    }
                    count++;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
