package model;

import java.time.LocalDateTime;
import java.io.Serializable;
import java.math.BigDecimal;
import model.enums.StatutPaiement;
import model.enums.ModePaiement;

public class Paiement implements Serializable {
    private int idPaiement;
    private int idCommande;
    private Integer idCarte;
    private BigDecimal montant;
    private StatutPaiement statutPaiement;
    private ModePaiement methodePaiement;
    private LocalDateTime datePaiement;

    public Paiement() {}

    public Paiement(int idPaiement, int idCommande, Integer idCarte, BigDecimal montant, 
                    StatutPaiement statutPaiement, ModePaiement methodePaiement, LocalDateTime datePaiement) {
        this.idPaiement = idPaiement;
        this.idCommande = idCommande;
        this.idCarte = idCarte;
        this.montant = montant;
        this.statutPaiement = statutPaiement;
        this.methodePaiement = methodePaiement;
        this.datePaiement = datePaiement;
    }

    public int getIdPaiement() { return idPaiement; }
    public void setIdPaiement(int idPaiement) { this.idPaiement = idPaiement; }

    public int getIdCommande() { return idCommande; }
    public void setIdCommande(int idCommande) { this.idCommande = idCommande; }

    public Integer getIdCarte() { return idCarte; }
    public void setIdCarte(Integer idCarte) { this.idCarte = idCarte; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public StatutPaiement getStatutPaiement() { return statutPaiement; }
    public void setStatutPaiement(StatutPaiement statutPaiement) { this.statutPaiement = statutPaiement; }

    public ModePaiement getMethodePaiement() { return methodePaiement; }
    public void setMethodePaiement(ModePaiement methodePaiement) { this.methodePaiement = methodePaiement; }

    public LocalDateTime getDatePaiement() { return datePaiement; }
    public void setDatePaiement(LocalDateTime datePaiement) { this.datePaiement = datePaiement; }
}
