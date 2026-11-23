package pe.net.ws.kpiweb.features.auth.models;
import com.google.gson.annotations.SerializedName;
public final class AndroidAccessChallenge {
    @SerializedName("id_acceso") public int accessId;
    @SerializedName("challenge_id") public int challengeId;
    @SerializedName("message") public String message;
}
