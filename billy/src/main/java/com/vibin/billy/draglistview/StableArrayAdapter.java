/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.vibin.billy.draglistview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.vibin.billy.R;

import java.util.HashMap;
import java.util.List;

public class StableArrayAdapter extends ArrayAdapter<String> {

    final int INVALID_ID = -1;
    Context c;
    HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();
    List<String> data;
    private static final String TAG = StableArrayAdapter.class.getSimpleName();

    public StableArrayAdapter(Context context, int textViewResourceId, List<String> objects) {
        super(context, textViewResourceId, objects);
        c = context;
        data = objects;
        for (int i = 0; i < objects.size(); ++i) {
            mIdMap.put(objects.get(i), i);
        }
    }

    @Override
    public long getItemId(int position) {
        if (position < 0 || position >= mIdMap.size()) {
            return INVALID_ID;
        }
        String item = getItem(position);
        return mIdMap.get(item);
    }

    static class MyViewHolder {
        TextView textView;
        ImageView dragHandler;
        CheckBox checkBox;

        MyViewHolder(View row) {
            textView = (TextView) row.findViewById(R.id.checkedTV);
            dragHandler = (ImageView) row.findViewById(R.id.drag_handler);
            checkBox = (CheckBox) row.findViewById(R.id.checkBox);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        MyViewHolder holder;
        if (row == null) {
            LayoutInflater lif = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = lif.inflate(R.layout.reorderedlist_row, parent, false);
            holder = new MyViewHolder(row);

            row.setTag(holder);
        } else {
            holder = (MyViewHolder) row.getTag();
        }

        holder.textView.setText(data.get(position));

        return row;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
