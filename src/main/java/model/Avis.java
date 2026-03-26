package model;

import java.io.Serializable;

public class Avis implements Serializable {
    private int idCommentaire;
    private int idClient;
    private int idProduit;
    private Integer idCommande;
    private String contenu;
    private Integer evaluation;
    private String image;

    public Avis() {}

    public Avis(int idCommentaire, int idClient, int idProduit, Integer idCommande, String contenu, Integer evaluation, String image) {
        this.idCommentaire = idCommentaire;
        this.idClient = idClient;
        this.idProduit = idProduit;
        this.idCommande = idCommande;
        this.contenu = contenu;
        this.evaluation = evaluation;
        this.image = image;
    }

    public int getIdCommentaire() { return idCommentaire; }
    public void setIdCommentaire(int idCommentaire) { this.idCommentaire = idCommentaire; }

    public int getIdClient() { return idClient; }
    public void setIdClient(int idClient) { this.idClient = idClient; }

    public int getIdProduit() { return idProduit; }
    public void setIdProduit(int idProduit) { this.idProduit = idProduit; }

    public Integer getIdCommande() { return idCommande; }
    public void setIdCommande(Integer idCommande) { this.idCommande = idCommande; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public Integer getEvaluation() { return evaluation; }
    public void setEvaluation(Integer evaluation) { this.evaluation = evaluation; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    private String nomClient;
    public String getNomClient() { return nomClient; }
    public void setNomClient(String nomClient) { this.nomClient = nomClient; }
}
