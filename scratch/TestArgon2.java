import de.mkammerer.argon2.Argon2Advanced;
import de.mkammerer.argon2.Argon2Factory;
import java.lang.reflect.Method;

public class TestArgon2 {
    public static void main(String[] args) {
        System.out.println("--- Argon2 Methods ---");
        for (Method m : Argon2Advanced.class.getMethods()) {
            if (m.getName().equals("rawHash")) {
                System.out.print("rawHash(");
                Class<?>[] params = m.getParameterTypes();
                for (int i = 0; i < params.length; i++) {
                    System.out.print(params[i].getSimpleName() + (i < params.length - 1 ? ", " : ""));
                }
                System.out.println(")");
            }
        }
    }
}
