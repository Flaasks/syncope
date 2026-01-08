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
package org.apache.syncope.core.logic;

import org.apache.syncope.common.lib.to.SecurityQuestionTO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.SecurityQuestionDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.SecurityQuestion;
import org.apache.syncope.core.provisioning.api.data.SecurityQuestionDataBinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("SecurityQuestionLogic - Category Partition")
class SecurityQuestionLogicCategoryPartitionTest {

    @Mock private SecurityQuestionDAO securityQuestionDAO;
    @Mock private UserDAO userDAO; 
    @Mock private SecurityQuestionDataBinder binder;

    private SecurityQuestionLogic logic;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        logic = new SecurityQuestionLogic(securityQuestionDAO, userDAO, binder);
    }



    @Test
    @DisplayName("list: non vuota → mappa ogni entity in TO")
    void list_nonEmpty() {
        SecurityQuestion q1 = mock(SecurityQuestion.class);
        SecurityQuestion q2 = mock(SecurityQuestion.class);

        List<SecurityQuestion> backing = new ArrayList<>();
        backing.add(q1);
        backing.add(q2);
        doReturn(backing).when(securityQuestionDAO).findAll();

        SecurityQuestionTO to1 = new SecurityQuestionTO(); to1.setKey("q1");
        SecurityQuestionTO to2 = new SecurityQuestionTO(); to2.setKey("q2");
        when(binder.getSecurityQuestionTO(q1)).thenReturn(to1);
        when(binder.getSecurityQuestionTO(q2)).thenReturn(to2);

        List<SecurityQuestionTO> out = logic.list();

        assertNotNull(out);
        assertEquals(2, out.size());
        assertEquals(List.of("q1", "q2"), out.stream().map(SecurityQuestionTO::getKey).toList());
        verify(securityQuestionDAO, times(1)).findAll();
        verify(binder, times(1)).getSecurityQuestionTO(q1);
        verify(binder, times(1)).getSecurityQuestionTO(q2);
    }

    @Test
    @DisplayName("list: vuota → ritorna lista vuota")
    void list_empty() {

        doReturn(Collections.emptyList()).when(securityQuestionDAO).findAll();

        List<SecurityQuestionTO> out = logic.list();

        assertNotNull(out);
        assertTrue(out.isEmpty());
        verify(securityQuestionDAO, times(1)).findAll();
        verifyNoInteractions(binder);
    }



    @Test
    @DisplayName("read: entity presente → ritorna TO mappato")
    void read_existing() {
        String key = "K";
        SecurityQuestion entity = mock(SecurityQuestion.class);
        SecurityQuestionTO to = new SecurityQuestionTO(); to.setKey(key);


        doReturn(Optional.of(entity)).when(securityQuestionDAO).findById(key);
        when(binder.getSecurityQuestionTO(entity)).thenReturn(to);

        SecurityQuestionTO out = logic.read(key);

        assertNotNull(out);
        assertEquals("K", out.getKey());
        verify(securityQuestionDAO, times(1)).findById(key);
        verify(binder, times(1)).getSecurityQuestionTO(entity);
    }

    @Test
    @DisplayName("read: entity assente → NotFoundException")
    void read_notFound() {
        doReturn(Optional.empty()).when(securityQuestionDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.read("missing"));
        verify(securityQuestionDAO, times(1)).findById("missing");
        verifyNoInteractions(binder);
    }



    @Test
    @DisplayName("create: TO valido → crea entity e ritorna TO")
    void create_valid() {
        SecurityQuestionTO inputTO = new SecurityQuestionTO();
        inputTO.setContent("What is your favorite color?");

        SecurityQuestion entity = mock(SecurityQuestion.class);
        SecurityQuestion savedEntity = mock(SecurityQuestion.class);
        SecurityQuestionTO outputTO = new SecurityQuestionTO();
        outputTO.setKey("newKey");
        outputTO.setContent("What is your favorite color?");

        when(binder.create(inputTO)).thenReturn(entity);
        when(securityQuestionDAO.save(entity)).thenReturn(savedEntity);
        when(binder.getSecurityQuestionTO(savedEntity)).thenReturn(outputTO);

        SecurityQuestionTO result = logic.create(inputTO);

        assertNotNull(result);
        assertEquals("newKey", result.getKey());
        assertEquals("What is your favorite color?", result.getContent());
        verify(binder, times(1)).create(inputTO);
        verify(securityQuestionDAO, times(1)).save(entity);
        verify(binder, times(1)).getSecurityQuestionTO(savedEntity);
    }

    @Test
    @DisplayName("update: entity esistente → aggiorna e ritorna TO")
    void update_existing() {
        SecurityQuestionTO inputTO = new SecurityQuestionTO();
        inputTO.setKey("existingKey");
        inputTO.setContent("Updated question?");

        SecurityQuestion entity = mock(SecurityQuestion.class);
        SecurityQuestion updatedEntity = mock(SecurityQuestion.class);
        SecurityQuestionTO outputTO = new SecurityQuestionTO();
        outputTO.setKey("existingKey");
        outputTO.setContent("Updated question?");

        doReturn(Optional.of(entity)).when(securityQuestionDAO).findById("existingKey");
        doNothing().when(binder).update(entity, inputTO);
        when(securityQuestionDAO.save(entity)).thenReturn(updatedEntity);
        when(binder.getSecurityQuestionTO(updatedEntity)).thenReturn(outputTO);

        SecurityQuestionTO result = logic.update(inputTO);

        assertNotNull(result);
        assertEquals("existingKey", result.getKey());
        assertEquals("Updated question?", result.getContent());
        verify(securityQuestionDAO, times(1)).findById("existingKey");
        verify(binder, times(1)).update(entity, inputTO);
        verify(securityQuestionDAO, times(1)).save(entity);
        verify(binder, times(1)).getSecurityQuestionTO(updatedEntity);
    }

    @Test
    @DisplayName("delete: entity esistente → elimina e ritorna TO")
    void delete_existing() {
        String key = "toDelete";
        SecurityQuestion entity = mock(SecurityQuestion.class);
        SecurityQuestionTO outputTO = new SecurityQuestionTO();
        outputTO.setKey(key);
        outputTO.setContent("Question to delete");

        doReturn(Optional.of(entity)).when(securityQuestionDAO).findById(key);
        when(binder.getSecurityQuestionTO(entity)).thenReturn(outputTO);
        doNothing().when(securityQuestionDAO).deleteById(key);

        SecurityQuestionTO result = logic.delete(key);

        assertNotNull(result);
        assertEquals(key, result.getKey());
        assertEquals("Question to delete", result.getContent());
        verify(securityQuestionDAO, times(1)).findById(key);
        verify(binder, times(1)).getSecurityQuestionTO(entity);
        verify(securityQuestionDAO, times(1)).deleteById(key);
    }
}