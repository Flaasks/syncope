/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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

    // ========== TEST 1: Lunghezza sempre in range [min, max] ==========
    @RepeatedTest(50)
    @DisplayName("generate: pwd.length ∈ [min, max] per config random")
    void generatedPasswordLengthInRange() {
        DefaultPasswordGenerator gen = new DefaultPasswordGenerator();
        int min = RND.nextInt(5, 12);  // 5..11
        int max = min + RND.nextInt(1, 20);  // min+1..min+19

        DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();
        conf.setMinLength(min);
        conf.setMaxLength(max);

        String pwd = gen.generate(conf);

        assertNotNull(pwd);
        assertTrue(pwd.length() >= min, "Password length must be >= min");
        assertTrue(pwd.length() <= max, "Password length must be <= max");
    }

    // ========== TEST 2: Fallback quando nessuna regola di caratteri ==========
    @RepeatedTest(30)
    @DisplayName("generate: fallback (lettere+cifre 50/50) quando nessuna CharacterRule")
    void fallbackGenerationWithRandomLengths() {
        DefaultPasswordGenerator gen = new DefaultPasswordGenerator();
        int min = RND.nextInt(6, 16);
        int max = min + 20;

        DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();
        conf.setMinLength(min);
        conf.setMaxLength(max);
        // NO uppercase/lowercase/digit/special configurati → fallback

        String pwd = gen.generate(conf);

        assertNotNull(pwd);
        assertTrue(pwd.length() >= min);
        int letters = countLetters(pwd);
        int digits = countDigits(pwd);
        assertTrue(letters > 0, "Fallback deve contenere lettere");
        assertTrue(digits > 0, "Fallback deve contenere cifre");
        // Verifica approssimativamente che siano mixed
        assertTrue(letters + digits >= pwd.length() * 0.8, "Fallback è ~80% lettere+cifre");
    }

    // ========== TEST 3: Merge conservativo ==========
    @RepeatedTest(40)
    @DisplayName("merge: prende max(minLength), min(maxLength), max di ogni requisito")
    void mergeConservativelyWithRandomPolicies() {
        DefaultPasswordGenerator gen = new DefaultPasswordGenerator();

        List<DefaultPasswordRuleConf> confs = new ArrayList<>();
        int expectedMin = 0;
        int expectedMax = 64;
        int expectedUpper = 0;
        int expectedLower = 0;
        int expectedDigit = 0;

        for (int i = 0; i < RND.nextInt(2, 5); i++) {
            DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();
            int min = RND.nextInt(5, 15);
            int max = min + RND.nextInt(10, 30);

            conf.setMinLength(min);
            conf.setMaxLength(max);
            conf.setUppercase(RND.nextInt(0, 4));
            conf.setLowercase(RND.nextInt(0, 4));
            conf.setDigit(RND.nextInt(0, 4));

            confs.add(conf);

            expectedMin = Math.max(expectedMin, min);
            expectedMax = Math.min(expectedMax, max > 0 ? max : 64);
            expectedUpper = Math.max(expectedUpper, conf.getUppercase());
            expectedLower = Math.max(expectedLower, conf.getLowercase());
            expectedDigit = Math.max(expectedDigit, conf.getDigit());
        }

        DefaultPasswordRuleConf merged = gen.merge(confs);

        assertEquals(expectedMin, merged.getMinLength(), "Merge must take max(minLength)");
        assertEquals(expectedMax, merged.getMaxLength(), "Merge must take min(maxLength)");
        assertEquals(expectedUpper, merged.getUppercase(), "Merge must take max(uppercase)");
        assertEquals(expectedLower, merged.getLowercase(), "Merge must take max(lowercase)");
        assertEquals(expectedDigit, merged.getDigit(), "Merge must take max(digit)");
    }

    // ========== TEST 4: Somma requisiti vs minLength ==========
    @RepeatedTest(35)
    @DisplayName("generate: length = max(sum(requisiti), min) per config random")
    void lengthIsMaxOfSumAndMin() {
        DefaultPasswordGenerator gen = new DefaultPasswordGenerator();
        int min = RND.nextInt(8, 20);
        int upper = RND.nextInt(1, 5);
        int lower = RND.nextInt(1, 5);
        int digit = RND.nextInt(1, 5);
        int sum = upper + lower + digit;

        DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();
        conf.setMinLength(min);
        conf.setMaxLength(min + 30);
        conf.setUppercase(upper);
        conf.setLowercase(lower);
        conf.setDigit(digit);

        String pwd = gen.generate(conf);

        assertNotNull(pwd);
        int expectedLen = Math.max(sum, min);
        assertTrue(pwd.length() >= expectedLen, 
            "Password length should be >= max(sum, min). Expected: " + expectedLen + ", Got: " + pwd.length());
    }

    // ========== TEST 5: Tutti i requisiti sono rispettati ==========
    @RepeatedTest(40)
    @DisplayName("generate: count(uppercase)>=req.upper && count(lower)>=req.lower && count(digit)>=req.digit")
    void allRequirementsRespected() {
        DefaultPasswordGenerator gen = new DefaultPasswordGenerator();

        int upper = RND.nextInt(1, 4);
        int lower = RND.nextInt(1, 4);
        int digit = RND.nextInt(1, 4);
        int min = upper + lower + digit + RND.nextInt(0, 10);

        DefaultPasswordRuleConf conf = new DefaultPasswordRuleConf();
        conf.setMinLength(min);
        conf.setMaxLength(min + 20);
        conf.setUppercase(upper);
        conf.setLowercase(lower);
        conf.setDigit(digit);

        String pwd = gen.generate(conf);

        assertTrue(countUpper(pwd) >= upper, 
            "Password must have >= " + upper + " uppercase, got " + countUpper(pwd));
        assertTrue(countLower(pwd) >= lower, 
            "Password must have >= " + lower + " lowercase, got " + countLower(pwd));
        assertTrue(countDigits(pwd) >= digit, 
            "Password must have >= " + digit + " digits, got " + countDigits(pwd));
    }

    // ========== TEST 6: Merge con min=0 e min>max ==========
    @RepeatedTest(25)
    @DisplayName("merge: corregge min=0→minLength se max≥8, e min>max→max=min")
    void mergeCorrectsBoundaryConditions() {
        DefaultPasswordGenerator gen = new DefaultPasswordGenerator();

        // Test caso 1: min=0, max=5 (< 8) → min diventa max
        DefaultPasswordRuleConf c1 = new DefaultPasswordRuleConf();
        c1.setMinLength(0);
        c1.setMaxLength(5);

        DefaultPasswordRuleConf merged1 = gen.merge(List.of(c1));
        // min=0 con max=5 < 8 → min diventa 5
        assertEquals(5, merged1.getMinLength(), "min=0 con max<8 deve diventare max");

        // Test caso 2: min=0, max=10 (>= 8) → min diventa 8
        DefaultPasswordRuleConf c2 = new DefaultPasswordRuleConf();
        c2.setMinLength(0);
        c2.setMaxLength(10);

        DefaultPasswordRuleConf merged2 = gen.merge(List.of(c2));
        assertEquals(8, merged2.getMinLength(), "min=0 con max≥8 deve diventare 8");

        // Test caso 3: min=15, max=12 (min > max) → max diventa min
        DefaultPasswordRuleConf c3 = new DefaultPasswordRuleConf();
        c3.setMinLength(15);
        c3.setMaxLength(12);

        DefaultPasswordRuleConf merged3 = gen.merge(List.of(c3));
        assertEquals(15, merged3.getMinLength(), "min deve rimanere 15");
        assertEquals(15, merged3.getMaxLength(), "Se min>max, max diventa min");
    }

}
