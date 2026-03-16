package org.example;
import dao.ConnexionBDD;
import java.sql.Connection;

public class Main {
    public static void main(String[] args) {
        Connection testConn = ConnexionBDD.getConnection();
        if (testConn != null) {
            System.out.println(" TEST RÉUSSI : Vous pouvez commencer à utiliser les DAO.");
            ConnexionBDD.closeConnection();
        } else {
            System.err.println(" TEST ÉCHOUÉ : Vérifiez vos paramètres MySQL.");
        }
    }
}
