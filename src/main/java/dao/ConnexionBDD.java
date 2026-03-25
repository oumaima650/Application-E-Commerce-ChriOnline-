package dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConnexionBDD {

    private static String URL;
    private static String USER;
    private static String PASSWORD;

    // Chargement des credentials depuis le fichier db.properties (non committé)
    static {
        try (InputStream input = ConnexionBDD.class.getClassLoader().getResourceAsStream("db.properties")) {
            Properties props = new Properties();
            if (input == null) {
                throw new RuntimeException("Fichier db.properties introuvable dans le classpath !");
            }
            props.load(input);
            URL      = props.getProperty("db.url");
            USER     = props.getProperty("db.user");
            PASSWORD = props.getProperty("db.password");
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors du chargement de db.properties", e);
        }
    }

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