package shared;

import java.io.Serializable;
import java.util.Map;

public class Requete implements Serializable {
    private RequestType type;
    private Map<String, Object> parametres;
    private String tokenSession;
    private long timestamp;
    private String nonce;

    public Requete() {}

    public Requete(RequestType type, Map<String, Object> parametres, String tokenSession) {
        this.type = type;
        this.parametres = parametres;
        this.tokenSession = tokenSession;
    }

    public RequestType getType() { return type; }
    public void setType(RequestType type) { this.type = type; }

    public Map<String, Object> getParametres() { return parametres; }
    public void setParametres(Map<String, Object> parametres) { this.parametres = parametres; }

    public String getTokenSession() { return tokenSession; }
    public void setTokenSession(String tokenSession) { this.tokenSession = tokenSession; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getNonce() { return nonce; }
    public void setNonce(String nonce) { this.nonce = nonce; }
}
