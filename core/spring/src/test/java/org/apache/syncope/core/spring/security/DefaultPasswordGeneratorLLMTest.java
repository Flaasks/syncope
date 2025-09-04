package org.apache.syncope.core.spring.security;

import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Assumptions;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DefaultPasswordGenerator - LLM-style property & fuzz tests")
class DefaultPasswordGeneratorLLMTest {

    private static final SecureRandom RND = new SecureRandom();

    private static int countLetters(String s) { int c=0; for (char ch: s.toCharArray()) if (Character.isLetter(ch)) c++; return c; }
    private static int countDigits(String s)  { int c=0; for (char ch: s.toCharArray()) if (Character.isDigit(ch))  c++; return c; }
    private static int countUpper(String s)   { int c=0; for (char ch: s.toCharArray()) if (Character.isUpperCase(ch)) c++; return c; }
    private static int countLower(String s)   { int c=0; for (char ch: s.toCharArray()) if (Character.isLowerCase(ch)) c++; return c; }
    private static int countSpecial(String s) { int c=0; for (char ch: s.toCharArray()) if (!Character.isLetterOrDigit(ch)) c++; return c; }

    /** Prova a configurare i simboli se la versione dell’API lo consente (setter pubblici). */
    private static boolean configureSpecialsIfPossible(DefaultPasswordRuleConf conf, String specials) {
        try {
            for (var m : conf.getClass().getMethods()) {
                if (!m.getName().toLowerCase().contains("special")) continue;
                if (m.getParameterCount() != 1) continue;
                Class<?> t = m.getParameterTypes()[0];
                if (t == String.class) {
                    m.invoke(conf, specials);
                    return true;
                } else if (List.class.isAssignableFrom(t)) {
                    List<String> list = new ArrayList<>();
                    for (char ch : specials.toCharArray()) list.add(String.valueOf(ch));
                    m.invoke(conf, list);
                    return true;
                } else if (t.isArray() && t.getComponentType() == char.class) {
                    m.invoke(conf, (Object) specials.toCharArray());
                    return true;
                }
            }
        } catch (ReflectiveOperationException ignored) {}
        return false;
    }

    /*
    @RepeatedTest(200)
    @DisplayName("Fuzz: invarianti su 200 configurazioni casuali (robustezza e proprietà)")
    void fuzzInvariants() {
        DefaultPasswordGenerator gen = new DefaultPasswordGenerator();
        DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();

        // 1) Genera min/max coerenti: max > min
        int min = 1 + RND.nextInt(12);          // 1..12
        int max = min + 1 + RND.nextInt(6);     // min+1 .. min+6  => sempre > min
        conf.setMinLength(min);
        conf.setMaxLength(max);

        // 2) Genera requisiti di classe piccoli (0..2) in modo casuale
        int up = RND.nextBoolean() ? RND.nextInt(3) : 0;
        int lo = RND.nextBoolean() ? RND.nextInt(3) : 0;
        int di = RND.nextBoolean() ? RND.nextInt(3) : 0;
        int sp = RND.nextBoolean() ? RND.nextInt(3) : 0;

        conf.setUppercase(up);
        conf.setLowercase(lo);
        conf.setDigit(di);
        conf.setSpecial(sp);

        // 3) Se richiediamo special, proviamo a configurare la pool; altrimenti testiamo l’eccezione e usciamo
        boolean needsSpecials = sp > 0;
        boolean specialsConfigured = !needsSpecials || configureSpecialsIfPossible(conf, "!@#$%^&*");
        if (needsSpecials && !specialsConfigured) {
            assertThrows(IllegalArgumentException.class, () -> gen.generate(conf),
                    "Se special>0 ma la pool di special è intrinsecamente vuota, è lecito fallire");
            return; // questa iterazione termina qui
        }

        // 4) Evita casi impossibili: garantisci che maxLength >= somma requisiti
        int reqSum = up + lo + di + sp;
        if (reqSum > 0 && max < reqSum) {
            max = reqSum;                   // aumenta max per poter soddisfare i requisiti
            if (min > max) min = max;       // coerenza extra (di fatto non dovrebbe accadere)
            conf.setMinLength(min);
            conf.setMaxLength(max);
        }

        // 5) Genera e verifica invarianti
        String pwd = assertDoesNotThrow(() -> gen.generate(conf));
        assertNotNull(pwd);
        assertTrue(pwd.length() >= min && pwd.length() <= max, "Lunghezza fuori [min,max]");

        if (reqSum == 0) {
            // Fallback: almeno metà lettere e metà cifre
            int half = pwd.length() / 2;
            assertTrue(countLetters(pwd) >= half, "Fallback: almeno metà lettere");
            assertTrue(countDigits(pwd)  >= half, "Fallback: almeno metà cifre");
        } else {
            if (up > 0) assertTrue(countUpper(pwd) >= up);
            if (lo > 0) assertTrue(countLower(pwd) >= lo);
            if (di > 0) assertTrue(countDigits(pwd) >= di);
            if (sp > 0) assertTrue(countSpecial(pwd) >= sp);
        }
    }
     */
}
