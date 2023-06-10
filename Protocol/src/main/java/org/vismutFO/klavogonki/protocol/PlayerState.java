package org.vismutFO.klavogonki.protocol;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

public class PlayerState implements Serializable {

    final public static int SERVER_START_TEAM = 0;
    final public static int SERVER_BEGIN_GAME = 1;
    final public static int SERVER_END_GAME = 2;
    final public static int SERVER_UPDATE = 3;
    final public static int CLIENT_UPDATE = 4;
    final public static int CLIENT_DISCONNECTED = 5;
    final public static int DEFAULT = 6;

    public int type;
    public int playerId;
    public int errors;
    public int symbols;
    public String playerName;
    public int status; // 0 - default, 1 - thisPlayer, 2 - disconnected

    public PlayerState(int playerId) {
        type = DEFAULT;
        this.playerId = playerId;
        playerName = "";
        errors = 0;
        symbols = 0;
        status = 0;
        //isThisPlayer = false;
        //isDisconnected = false;
    }

    public PlayerState(PlayerState other) {
        type = other.type;
        playerId = other.playerId;
        playerName = other.playerName;
        errors = other.errors;
        symbols = other.symbols;
        status = other.status;
    }

    public PlayerState (JSONObject source) throws IOException {

        if (!source.has("type")) {
            throw new IOException("state hasn't type");
        }
        type = source.getInt("type");

        if (!source.has("playerId")) {
            throw new IOException("state hasn't playerId");
        }
        playerId = source.getInt("playerId");

        if (!source.has("playerName")) {
            throw new IOException("state hasn't playerName");
        }
        playerName = source.getString("playerName");

        if (!source.has("errors")) {
            throw new IOException("state hasn't errors");
        }
        errors = source.getInt("errors");

        if (!source.has("symbols")) {
            throw new IOException("state hasn't symbols");
        }
        symbols = source.getInt("symbols");

        if (!source.has("status")) {
            throw new IOException("state hasn't status");
        }
        status = source.getInt("status");

        //if (!source.has("isDisconnected")) {
            //throw new IOException("state hasn't isDisconnected");
        //}
        //isDisconnected = source.getBoolean("isDisconnected");
    }

    public PlayerState (String all) throws IOException {
        // there should be only correct states
        this(new JSONObject(all));
    }

    public JSONObject toJSONObject() {
        JSONObject temp = new JSONObject();
        temp.put("type", type);
        temp.put("playerId", playerId);
        temp.put("playerName", playerName);
        temp.put("errors", errors);
        temp.put("symbols", symbols);
        temp.put("status", status);
        //temp.put("isThisPlayer", isThisPlayer);
        //temp.put("isDisconnected", isDisconnected);
        return temp;
    }

    @Override
    public String toString() {
        return this.toJSONObject().toString();
    }

    public static ArrayList<PlayerState> getStates(String source) throws IOException {
        JSONObject obj = new JSONObject(source);
        if (!obj.has("states")) {
            throw new IOException("json hasn't states");
        }
        JSONArray temp = obj.getJSONArray("states");
        ArrayList<PlayerState> ans = new ArrayList<>(temp.length());
        for (int i = 0; i < temp.length(); i++) {
            ans.add(new PlayerState(temp.getJSONObject(i)));
        }
        return ans;
    }

    public static String getSource(ArrayList<PlayerState> source) {
        JSONArray temp = new JSONArray();
        for (PlayerState state : source) {
            temp.put(state.toJSONObject());
        }
        JSONObject ans = new JSONObject();
        ans.put("states", temp);
        return ans.toString();
    }
}
