package com.nfc.emulator;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class CardStore {
    private static final String PREF = "cards";
    private static final String KEY = "card_list";
    private final SharedPreferences prefs;

    public static class Card {
        public String name;
        public String data; // hex string
        public String uid;
        public Card(String name, String uid, String data) {
            this.name = name; this.uid = uid; this.data = data;
        }
    }

    public CardStore(Context ctx) {
        prefs = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public void save(Card card) {
        try {
            JSONArray arr = getAll();
            JSONObject obj = new JSONObject();
            obj.put("name", card.name);
            obj.put("uid", card.uid);
            obj.put("data", card.data);
            arr.put(obj);
            prefs.edit().putString(KEY, arr.toString()).apply();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public JSONArray getAll() {
        try {
            String s = prefs.getString(KEY, "[]");
            return new JSONArray(s);
        } catch (Exception e) { return new JSONArray(); }
    }

    public List<String> getNames() {
        List<String> names = new ArrayList<>();
        try {
            JSONArray arr = getAll();
            for (int i = 0; i < arr.length(); i++)
                names.add(arr.getJSONObject(i).getString("name"));
        } catch (Exception e) { e.printStackTrace(); }
        return names;
    }

    public Card get(int index) {
        try {
            JSONObject o = getAll().getJSONObject(index);
            return new Card(o.getString("name"), o.getString("uid"), o.getString("data"));
        } catch (Exception e) { return null; }
    }
}
