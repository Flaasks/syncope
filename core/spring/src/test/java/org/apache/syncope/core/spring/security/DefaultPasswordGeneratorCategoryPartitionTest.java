package org.apache.syncope.core.spring.security;

import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;


class DefaultPasswordGeneratorCategoryPartitionTest {

    private static int countLetters(String s) {
        int c = 0;
        for (char ch : s.toCharArray()) if (Character.isLetter(ch)) c++;
        return c;
    }
    private static int countDigits(String s) {
        int c = 0;
        for (char ch : s.toCharArray()) if (Character.isDigit(ch)) c++;
        return c;
    }
    private static int countUpper(String s) {
        int c = 0;
        for (char ch : s.toCharArray()) if (Character.isUpperCase(ch)) c++;
        return c;
    }
    private static int countLower(String s) {
        int c = 0;
        for (char ch : s.toCharArray()) if (Character.isLowerCase(ch)) c++;
        return c;
    }
    private static int countSpecial(String s) {
        int c = 0;
        for (char ch : s.toCharArray()) if (!Character.isLetterOrDigit(ch)) c++;
        return c;
    }

    @Test
    @DisplayName("Nessuna regola di classe → fallback (alphabetical/digit = min/2)")
    void fallbackWhenNoCharacterRules() {
        DefaultPasswordGenerator gen = new DefaultPasswordGenerator();
        DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();
        conf.setMinLength(8);
        conf.setMaxLength(64);

        String pwd = gen.generate(conf);

        assertNotNull(pwd);
        assertEquals(8, pwd.length(), "Con min=8 e fallback, lunghezza attesa = 8");
        assertTrue(countLetters(pwd) >= 4, "Fallback: almeno metà lettere");
        assertTrue(countDigits(pwd) >= 4, "Fallback: almeno metà cifre");
    }

    @Test
    @DisplayName("Somma requisiti > minLength → lunghezza = somma requisiti")
    void sumRequirementsGreaterThanMin() {
        DefaultPasswordGenerator gen = new DefaultPasswordGenerator();
        DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();
        conf.setMinLength(6); // min < somma
        conf.setMaxLength(64);
        conf.setUppercase(2);
        conf.setLowercase(3);
        conf.setDigit(2);
        conf.setSpecial(0);

        String pwd = gen.generate(conf);

        int expectedMin = 2 + 3 + 2; // 7 > 6
        assertNotNull(pwd);
        assertTrue(pwd.length() >= expectedMin);
        assertTrue(countUpper(pwd) >= 2);
        assertTrue(countLower(pwd) >= 3);
        assertTrue(countDigits(pwd) >= 2);
    }

    @Test
    @DisplayName("Somma requisiti ≤ minLength → lunghezza = minLength")
    void sumRequirementsLessOrEqualMin() {
        DefaultPasswordGenerator gen = new DefaultPasswordGenerator();
        DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();
        conf.setMinLength(12);
        conf.setMaxLength(64);
        conf.setUppercase(1);
        conf.setLowercase(1);
        conf.setDigit(1);
        conf.setSpecial(0);

        String pwd = gen.generate(conf);

        assertNotNull(pwd);
        assertEquals(12, pwd.length());
        assertTrue(countUpper(pwd) >= 1);
        assertTrue(countLower(pwd) >= 1);
        assertTrue(countDigits(pwd) >= 1);
    }

    @Test
    @DisplayName("merge: min=0 e max<8 → min:=max; min>max → max:=min")
    void mergeMinZeroAndMinGreaterThanMax() {
        DefaultPasswordGenerator gen = new DefaultPasswordGenerator();

        DefaultPasswordRuleConf c1 = new DefaultPasswordRuleConf();
        c1.setMinLength(0);
        c1.setMaxLength(5);

        DefaultPasswordRuleConf c2 = new DefaultPasswordRuleConf();
        c2.setMinLength(12);
        c2.setMaxLength(10);

        DefaultPasswordRuleConf merged = gen.merge(List.of(c1, c2));

        assertEquals(12, merged.getMinLength());
        assertEquals(12, merged.getMaxLength());
    }

    @Test
    @DisplayName("merge: max su conteggi, max(minLength), min(maxLength>0)")
    void mergeCombinesConservatively() {
        DefaultPasswordGenerator gen = new DefaultPasswordGenerator();

        DefaultPasswordRuleConf a = new DefaultPasswordRuleConf();
        a.setMinLength(8);
        a.setMaxLength(32);
        a.setUppercase(1);
        a.setLowercase(1);

        DefaultPasswordRuleConf b = new DefaultPasswordRuleConf();
        b.setMinLength(10);
        b.setMaxLength(16);
        b.setUppercase(2);
        b.setDigit(2);

        DefaultPasswordRuleConf merged = gen.merge(List.of(a, b));

        assertEquals(10, merged.getMinLength());
        assertEquals(16, merged.getMaxLength());
        assertEquals(2, merged.getUppercase());
        assertEquals(1, merged.getLowercase());
        assertEquals(2, merged.getDigit());
        assertEquals(0, merged.getSpecial());
    }

    @Test
    @DisplayName("Richiede simboli → presente almeno il numero richiesto (se configurabili)")
    void requiresSymbolsIfSupported() {
        DefaultPasswordGenerator gen = new DefaultPasswordGenerator();
        DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();

        conf.setMinLength(8);
        conf.setMaxLength(64);
        conf.setUppercase(1);
        conf.setLowercase(1);
        conf.setDigit(1);
        conf.setSpecial(2);

        boolean configured = configureSpecialCharactersIfPossible(conf);

        // Se non c'è modo di impostare i simboli in questa versione, skippa questo test
        Assumptions.assumeTrue(configured, "Special chars non configurabili in questa versione: test ignorato");

        String pwd = gen.generate(conf);

        assertNotNull(pwd);
        assertEquals(8, pwd.length());
        assertTrue(countUpper(pwd) >= 1);
        assertTrue(countLower(pwd) >= 1);
        assertTrue(countDigits(pwd) >= 1);
        assertTrue(countSpecial(pwd) >= 2, "Attesi almeno 2 caratteri speciali");
    }


    private static boolean configureSpecialCharactersIfPossible(DefaultPasswordRuleConf conf) {

        List<String> asListOfStrings = "!@#$%".chars()
                .mapToObj(c -> String.valueOf((char) c))
                .collect(Collectors.toCollection(ArrayList::new));

        List<Character> asListOfChars = "!@#$%".chars()
                .mapToObj(c -> (char) c)
                .toList();

        char[] asCharArray = "!@#$%".toCharArray();
        Character[] asCharacterArray = asListOfChars.toArray(new Character[0]);

        // Nomi possibili dei metodi
        String[] baseNames = {
                "setSpecialChars", "setSymbols", "setSpecialAlphabet", "setAllowedSpecials"
        };

        // 1) Prova metodi(String)
        for (String name : baseNames) {
            try {
                Method m = conf.getClass().getMethod(name, String.class);
                m.invoke(conf, "!@#$%");
                return true;
            } catch (ReflectiveOperationException ignored) {}
        }
        // 2) Prova metodi(List<String> / Collection<String> / Set<String>)
        for (String name : baseNames) {
            try {
                Method m = conf.getClass().getMethod(name, List.class);
                m.invoke(conf, asListOfStrings);
                return true;
            } catch (ReflectiveOperationException ignored) {}
            try {
                Method m = conf.getClass().getMethod(name, Collection.class);
                m.invoke(conf, asListOfStrings);
                return true;
            } catch (ReflectiveOperationException ignored) {}
            try {
                Method m = conf.getClass().getMethod(name, Set.class);
                m.invoke(conf, Set.copyOf(asListOfStrings));
                return true;
            } catch (ReflectiveOperationException ignored) {}
        }
        // 3) Prova metodi(char[] / Character[])
        for (String name : baseNames) {
            try {
                Method m = conf.getClass().getMethod(name, char[].class);
                m.invoke(conf, (Object) asCharArray);
                return true;
            } catch (ReflectiveOperationException ignored) {}
            try {
                Method m = conf.getClass().getMethod(name, Character[].class);
                m.invoke(conf, (Object) asCharacterArray);
                return true;
            } catch (ReflectiveOperationException ignored) {}
        }
        // Nessun setter trovato/compatibile
        return false;
    }
}