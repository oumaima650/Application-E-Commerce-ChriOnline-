package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnexionBDD {
    // Paramètres de connexion
    private static final String URL = "jdbc:mysql://localhost:3306/chri_online";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static Connection connection = null;

    private ConnexionBDD() {}


    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                // Charger le driver explicitement (optionnel dans les versions récentes de JDBC)
                Class.forName("com.mysql.cj.jdbc.Driver");
                
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println(" Connexion à la base de données 'chri_online' réussie !");
            }
        } catch (ClassNotFoundException e) {
            System.err.println(" Erreur : Driver MySQL non trouvé !");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println(" Erreur lors de la connexion à la base de données !");
            System.err.println("Assurez-vous que MySQL tourne et que la base 'chri_online' existe");
            e.printStackTrace();
        }
        return connection;
    }


    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println(" Connexion fermée.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}
