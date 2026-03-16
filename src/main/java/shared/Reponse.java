package shared;

import java.io.Serializable;
import java.util.Map;

public class Reponse implements Serializable {
    private boolean succes;
    private String message;
    private Map<String, Object> donnees;

    public Reponse() {}

    public Reponse(boolean succes, String message, Map<String, Object> donnees) {
        this.succes = succes;
        this.message = message;
        this.donnees = donnees;
    }

    public boolean isSucces() { return succes; }
    public void setSucces(boolean succes) { this.succes = succes; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Map<String, Object> getDonnees() { return donnees; }
    public void setDonnees(Map<String, Object> donnees) { this.donnees = donnees; }
}
