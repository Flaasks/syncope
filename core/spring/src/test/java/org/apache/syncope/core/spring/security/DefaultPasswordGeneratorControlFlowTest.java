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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.provisioning.api.rules.PasswordRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Control Flow tests for DefaultPasswordGenerator
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultPasswordGenerator - Control Flow coverage")
class DefaultPasswordGeneratorControlFlowTest {

    private TestableDefaultPasswordGenerator generator;


    private static class TestableDefaultPasswordGenerator extends DefaultPasswordGenerator {

        @Override
        protected List<PasswordRule> getPasswordRules(final PasswordPolicy policy) {
            return new ArrayList<>();
        }

        DefaultPasswordRuleConf mergeForTest(final List<DefaultPasswordRuleConf> defaultRuleConfs) {
            return super.merge(defaultRuleConfs);
        }

        String generateForTest(final DefaultPasswordRuleConf ruleConf) {
            return super.generate(ruleConf);
        }
    }

    @BeforeEach
    void setUp() {
        generator = new TestableDefaultPasswordGenerator();
    }

    @Test
    @DisplayName("CF1: Multiple realms with duplicate password policy - filters duplicates")
    void testMultipleRealmsWithDuplicatePolicy() {
        PasswordPolicy sharedPolicy = mock(PasswordPolicy.class);
        PasswordPolicy resourcePolicy = mock(PasswordPolicy.class);
        
        ExternalResource resource = mock(ExternalResource.class);
        when(resource.getPasswordPolicy()).thenReturn(resourcePolicy);
        
        Realm realm1 = mock(Realm.class);
        when(realm1.getPasswordPolicy()).thenReturn(sharedPolicy);
        
        Realm realm2 = mock(Realm.class);
        when(realm2.getPasswordPolicy()).thenReturn(sharedPolicy);
        
        List<Realm> realms = List.of(realm1, realm2);
        
        String password = generator.generate(resource, realms);
        
        assertNotNull(password);
    }

    @Test
    @DisplayName("CF2: Alphabetical character requirement in merge - alphabetical=1")
    void testAlphabeticalRequirement() {
        DefaultPasswordRuleConf ruleConf = new DefaultPasswordRuleConf();
        ruleConf.setMinLength(8);
        ruleConf.setMaxLength(64);
        ruleConf.setAlphabetical(1); 
        
        List<DefaultPasswordRuleConf> configs = List.of(ruleConf);
        DefaultPasswordRuleConf merged = generator.mergeForTest(configs);
        
        assertEquals(1, merged.getAlphabetical());
        
        String password = generator.generateForTest(merged);
        assertNotNull(password);
        assertEquals(8, password.length());
    }
}
