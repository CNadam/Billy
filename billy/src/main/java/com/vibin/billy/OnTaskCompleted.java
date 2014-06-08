package com.vibin.billy;

import org.json.JSONException;

public interface OnTaskCompleted {
    void onTaskCompleted(String result) throws JSONException;
}
