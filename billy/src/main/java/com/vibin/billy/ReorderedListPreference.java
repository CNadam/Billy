package com.vibin.billy;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ArrayAdapter;

import com.mobeta.android.dslv.DragSortItemViewCheckable;
import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.SimpleFloatViewManager;

import java.util.ArrayList;
import java.util.Arrays;

public class ReorderedListPreference extends DialogPreference {
    Context c;
    DragSortListView listView;
    ArrayAdapter<String> arrayAdapter;

    public ReorderedListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.c = context;
        setDialogLayoutResource(R.layout.reorderedlist_preference);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);

        setDialogIcon(null);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        String[] array = {"Most Popular", "Pop", "Rock", "Dance"};
        ArrayList<String> arrayList = new ArrayList<String>();
        arrayList.addAll(Arrays.asList(array));
        arrayAdapter = new ArrayAdapter<String>(c, R.layout.reorderedlist_row, R.id.text, arrayList);
        listView = (DragSortListView) view.findViewById(android.R.id.list);
        listView.setAdapter(arrayAdapter);
        listView.setDropListener(onDrop);

        // Make the floating row view transparent
        SimpleFloatViewManager simpleFloatViewManager = new SimpleFloatViewManager(listView);
        simpleFloatViewManager.setBackgroundColor(Color.TRANSPARENT);
        listView.setFloatViewManager(simpleFloatViewManager);

/*        LayoutInflater lif = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v  = lif.inflate(R.layout.reorderedlist_row, null, false);
        final CheckedTextView ctv =  (CheckedTextView) v.findViewById(R.id.text);
        ctv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ctv.isChecked()) {
                    ctv.setChecked(false);
                } else {
                    ctv.setChecked(true);
                }
            }
        });*/
/*        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Toast.makeText(getContext(), i+" is touched",
                   Toast.LENGTH_LONG).show();
            }
        });*/

        final DragSortItemViewCheckable dstvc = (DragSortItemViewCheckable) listView.getChildAt(1);
        dstvc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dstvc.isChecked()) {
                    dstvc.setChecked(false);
                } else {
                    dstvc.setChecked(true);
                }
            }
        });
    }


    private DragSortListView.DropListener onDrop =
            new DragSortListView.DropListener() {
                @Override
                public void drop(int from, int to) {
                    if (from != to) {
                        String item = arrayAdapter.getItem(from);
                        arrayAdapter.remove(item);
                        arrayAdapter.insert(item, to);
                        listView.moveCheckState(from, to);
                        arrayAdapter.notifyDataSetChanged();
                    }
                }
            };

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
    }
}
