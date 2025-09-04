package org.apache.syncope.core.spring.security;

import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DefaultPasswordGenerator - Control Flow coverage")
class DefaultPasswordGeneratorControlFlowTest {

    private static int countUpper(String s) { int c=0; for (char ch: s.toCharArray()) if (Character.isUpperCase(ch)) c++; return c; }
    private static int countLower(String s) { int c=0; for (char ch: s.toCharArray()) if (Character.isLowerCase(ch)) c++; return c; }
    private static int countDigits(String s) { int c=0; for (char ch: s.toCharArray()) if (Character.isDigit(ch)) c++; return c; }
    private static int countSpecial(String s){ int c=0; for (char ch: s.toCharArray()) if (!Character.isLetterOrDigit(ch)) c++; return c; }

    private static boolean configureSpecialsIfPossible(DefaultPasswordRuleConf conf) {
        try {
            for (var m : conf.getClass().getMethods()) {
                if (!m.getName().toLowerCase().contains("special")) continue;
                if (m.getParameterCount() != 1) continue;
                Class<?> t = m.getParameterTypes()[0];
                if (t == String.class) { m.invoke(conf, "!@#$%"); return true; }
                if (t == char[].class) { m.invoke(conf, (Object) "!@#$%".toCharArray()); return true; }
                if (List.class.isAssignableFrom(t)) {
                    var list = "!@#$%".chars().mapToObj(c -> String.valueOf((char)c)).toList();
                    m.invoke(conf, list); return true;
                }
            }
        } catch (ReflectiveOperationException ignored) {}
        return false;
    }

    @Test
    @DisplayName("reqSum > min → la scelta di lunghezza segue la somma dei requisiti")
    void branchReqSumGTMin() {
        DefaultPasswordGenerator gen = new DefaultPasswordGenerator();
        DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();
        conf.setMinLength(6);
        conf.setMaxLength(64);
        conf.setUppercase(3);
        conf.setLowercase(3);
        conf.setDigit(2);
        conf.setSpecial(0);

        String pwd = gen.generate(conf);
        assertTrue(pwd.length() >= 8); // 3+3+2 = 8 > min(6)
        assertTrue(countUpper(pwd) >= 3);
        assertTrue(countLower(pwd) >= 3);
        assertTrue(countDigits(pwd) >= 2);
    }

    @Test
    @DisplayName("reqSum ≤ min → la lunghezza resta min")
    void branchReqSumLEMin() {
        DefaultPasswordGenerator gen = new DefaultPasswordGenerator();
        DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();
        conf.setMinLength(12);
        conf.setMaxLength(64);
        conf.setUppercase(1);
        conf.setLowercase(1);
        conf.setDigit(1);
        conf.setSpecial(0);

        String pwd = gen.generate(conf);
        assertEquals(12, pwd.length());
    }

    @Test
    @DisplayName("merge: min=0 e max piccolo + min>max → normalizzazioni applicate")
    void mergeNormalizationBranches() {
        DefaultPasswordGenerator gen = new DefaultPasswordGenerator();

        DefaultPasswordRuleConf a = new DefaultPasswordRuleConf();
        a.setMinLength(0);
        a.setMaxLength(5);

        DefaultPasswordRuleConf b = new DefaultPasswordRuleConf();
        b.setMinLength(12);
        b.setMaxLength(10);

        DefaultPasswordRuleConf merged = gen.merge(List.of(a, b));

        assertEquals(12, merged.getMinLength());
        assertEquals(12, merged.getMaxLength());
    }

    @Test
    @DisplayName("reqSum > max: l'implementazione dà priorità ai requisiti → length = reqSum")
    void reqSumExceedsMaxLengthBranch() {
        DefaultPasswordGenerator gen = new DefaultPasswordGenerator();
        DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();
        conf.setMinLength(8);
        conf.setMaxLength(8);
        conf.setUppercase(3);
        conf.setLowercase(3);
        conf.setDigit(3);
        conf.setSpecial(0);

        String pwd = gen.generate(conf);


        assertEquals(9, pwd.length(), "Quando reqSum > max, l'implementazione eleva la lunghezza a reqSum");
        assertTrue(pwd.chars().filter(Character::isUpperCase).count() >= 3);
        assertTrue(pwd.chars().filter(Character::isLowerCase).count() >= 3);
        assertTrue(pwd.chars().filter(Character::isDigit).count()     >= 3);
    }

    @Test
    @DisplayName("Ramo 'impossibile': special>0 ma pool non configurabile → eccezione attesa")
    void impossiblePolicyBranch() {
        DefaultPasswordGenerator gen = new DefaultPasswordGenerator();
        DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();
        conf.setMinLength(8);
        conf.setMaxLength(12);
        conf.setUppercase(1);
        conf.setLowercase(1);
        conf.setDigit(1);
        conf.setSpecial(1); // richiede simboli

        boolean specialsConfigured = configureSpecialsIfPossible(conf);

        Assumptions.assumeFalse(specialsConfigured, "Special configurabili: salto il ramo 'impossibile'");

        assertThrows(IllegalArgumentException.class, () -> gen.generate(conf),
                "Senza pool di special, special>0 porta a 'bound must be positive'");
    }
}
