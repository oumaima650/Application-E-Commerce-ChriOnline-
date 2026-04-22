package test;

import server.ServeurTCP;

public class ServeurTCPTestable extends ServeurTCP {

    @Override
    protected boolean checkIpLimit(String ip) {
        // On désactive la limite par IP pour pouvoir tester la limite globale (10 connexions)
        return true;
    }

    public static void main(String[] args) {
        new ServeurTCPTestable().start();
    }
}
