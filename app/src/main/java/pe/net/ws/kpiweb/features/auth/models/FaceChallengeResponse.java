package pe.net.ws.kpiweb.features.auth.models;

import com.google.gson.annotations.SerializedName;

public class FaceChallengeResponse {
    @SerializedName("challenge_id")
    public int challengeId;

    @SerializedName("nonce")
    public String nonce;

    @SerializedName("actions")
    public String[] actions;

    @SerializedName("guide")
    public String guide;

    @SerializedName("action")
    public String action;

    @SerializedName("message")
    public String message;
}
