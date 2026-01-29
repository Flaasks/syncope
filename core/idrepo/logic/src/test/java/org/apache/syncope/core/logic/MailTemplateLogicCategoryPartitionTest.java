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
 * Unless required by applicable law or agreed to in writing
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.logic;

import org.apache.syncope.common.lib.to.MailTemplateTO;
import org.apache.syncope.common.lib.types.MailTemplateFormat;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.MailTemplateDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.NotificationDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.MailTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;

/**
 * Category Partition Tests for MailTemplateLogic
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MailTemplateLogic - Category Partition Tests")
class MailTemplateLogicCategoryPartitionTest {

    @Mock
    private MailTemplateDAO mailTemplateDAO;

    @Mock
    private NotificationDAO notificationDAO;

    @Mock
    private EntityFactory entityFactory;

    @Mock
    private MailTemplate mailTemplate;

    private MailTemplateLogic mailTemplateLogic;

    @BeforeEach
    void setUp() {
        mailTemplateLogic = new MailTemplateLogic(mailTemplateDAO, notificationDAO, entityFactory);
    }


    @Test
    @DisplayName("CP1: create() con chiave già esistente nel DB -> DuplicateException")
    void testCreate_ExistingKey_ThrowsDuplicateException() {
        String existingKey = "existingTemplate";
        when(mailTemplateDAO.existsById(existingKey)).thenReturn(true);

        DuplicateException exception = assertThrows(DuplicateException.class,
                () -> mailTemplateLogic.create(existingKey));
        
        assertEquals(existingKey, exception.getMessage());
        verify(mailTemplateDAO, times(1)).existsById(existingKey);
        verify(mailTemplateDAO, never()).save(any());
    }

    @Test
    @DisplayName("CP2: create() con chiave nuova (non esistente) -> successo")
    void testCreate_NewKey_Success() {
        String newKey = "newTemplate";
        when(mailTemplateDAO.existsById(newKey)).thenReturn(false);
        when(entityFactory.newEntity(MailTemplate.class)).thenReturn(mailTemplate);
        when(mailTemplateDAO.save(mailTemplate)).thenReturn(mailTemplate);

        MailTemplateTO result = mailTemplateLogic.create(newKey);

        assertNotNull(result);
        assertEquals(newKey, result.getKey());
        verify(mailTemplateDAO, times(1)).existsById(newKey);
        verify(mailTemplate, times(1)).setKey(newKey);
        verify(mailTemplateDAO, times(1)).save(mailTemplate);
    }


    @Test
    @DisplayName("CP3: read() con chiave non esistente -> NotFoundException")
    void testRead_NonExistentKey_ThrowsNotFoundException() {
        String nonExistentKey = "nonExistentTemplate";
        when(mailTemplateDAO.findById(nonExistentKey)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> mailTemplateLogic.read(nonExistentKey));
        
        assertTrue(exception.getMessage().contains(nonExistentKey));
        verify(mailTemplateDAO, times(1)).findById(nonExistentKey);
    }

    @Test
    @DisplayName("CP4: read() con chiave esistente -> successo")
    void testRead_ExistingKey_Success() {
        String existingKey = "existingTemplate";
        doReturn(Optional.of(mailTemplate)).when(mailTemplateDAO).findById(existingKey);

        MailTemplateTO result = mailTemplateLogic.read(existingKey);

        assertNotNull(result);
        assertEquals(existingKey, result.getKey());
        verify(mailTemplateDAO, times(1)).findById(existingKey);
    }


    @Test
    @DisplayName("CP5: delete() con chiave esistente e nessuna notifica -> successo")
    void testDelete_ExistingKeyNoNotifications_Success() {
        String existingKey = "templateToDelete";
        doReturn(Optional.of(mailTemplate)).when(mailTemplateDAO).findById(existingKey);
        doReturn(Collections.emptyList()).when(notificationDAO).findByTemplate(mailTemplate);

        MailTemplateTO result = mailTemplateLogic.delete(existingKey);

        assertNotNull(result);
        assertEquals(existingKey, result.getKey());
        verify(mailTemplateDAO, times(1)).findById(existingKey);
        verify(mailTemplateDAO, times(1)).deleteById(existingKey);
    }

    @Test
    @DisplayName("CP6: delete() con chiave non esistente -> NotFoundException")
    void testDelete_NonExistentKey_ThrowsNotFoundException() {
        String nonExistentKey = "nonExistentTemplate";
        when(mailTemplateDAO.findById(nonExistentKey)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> mailTemplateLogic.delete(nonExistentKey));
        
        assertTrue(exception.getMessage().contains(nonExistentKey));
        verify(mailTemplateDAO, times(1)).findById(nonExistentKey);
        verify(mailTemplateDAO, never()).deleteById(anyString());
    }


    @Test
    @DisplayName("CP7: getFormat() HTML con template HTML vuoto -> NotFoundException")
    void testGetFormat_HtmlFormatBlankContent_ThrowsNotFoundException() {
        String key = "templateKey";
        doReturn(Optional.of(mailTemplate)).when(mailTemplateDAO).findById(key);
        when(mailTemplate.getHTMLTemplate()).thenReturn("");  // Blank content

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> mailTemplateLogic.getFormat(key, MailTemplateFormat.HTML));
        
        assertTrue(exception.getMessage().contains(key));
        assertTrue(exception.getMessage().contains("HTML"));
        verify(mailTemplateDAO, times(1)).findById(key);
    }

    @Test
    @DisplayName("CP8: getFormat() TEXT con template TEXT vuoto -> NotFoundException")
    void testGetFormat_TextFormatBlankContent_ThrowsNotFoundException() {
        String key = "templateKey";
        doReturn(Optional.of(mailTemplate)).when(mailTemplateDAO).findById(key);
        when(mailTemplate.getTextTemplate()).thenReturn(null);  // Blank content

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> mailTemplateLogic.getFormat(key, MailTemplateFormat.TEXT));
        
        assertTrue(exception.getMessage().contains(key));
        assertTrue(exception.getMessage().contains("TEXT"));
        verify(mailTemplateDAO, times(1)).findById(key);
    }

    @Test
    @DisplayName("CP9: getFormat() HTML con contenuto valido -> successo")
    void testGetFormat_HtmlFormatWithContent_Success() {
        String key = "templateKey";
        String htmlContent = "<html><body>Welcome {{username}}</body></html>";
        doReturn(Optional.of(mailTemplate)).when(mailTemplateDAO).findById(key);
        when(mailTemplate.getHTMLTemplate()).thenReturn(htmlContent);

        String result = mailTemplateLogic.getFormat(key, MailTemplateFormat.HTML);

        assertEquals(htmlContent, result);
        verify(mailTemplateDAO, times(1)).findById(key);
        verify(mailTemplate, times(1)).getHTMLTemplate();
    }

    @Test
    @DisplayName("CP10: getFormat() TEXT con contenuto valido -> successo")
    void testGetFormat_TextFormatWithContent_Success() {
        String key = "templateKey";
        String textContent = "Welcome {{username}}\nYour account is ready.";
        doReturn(Optional.of(mailTemplate)).when(mailTemplateDAO).findById(key);
        when(mailTemplate.getTextTemplate()).thenReturn(textContent);

        String result = mailTemplateLogic.getFormat(key, MailTemplateFormat.TEXT);
            
        assertEquals(textContent, result);
        verify(mailTemplateDAO, times(1)).findById(key);
        verify(mailTemplate, times(1)).getTextTemplate();
    }

    @Test
    @DisplayName("CP11: getFormat() con chiave non esistente -> NotFoundException")
    void testGetFormat_NonExistentKey_ThrowsNotFoundException() {
        String nonExistentKey = "nonExistentTemplate";
        when(mailTemplateDAO.findById(nonExistentKey)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> mailTemplateLogic.getFormat(nonExistentKey, MailTemplateFormat.HTML));
        
        assertTrue(exception.getMessage().contains(nonExistentKey));
        verify(mailTemplateDAO, times(1)).findById(nonExistentKey);
    }


    @Test
    @DisplayName("CP12: setFormat() HTML -> aggiorna template HTML correttamente")
    void testSetFormat_HtmlFormat_UpdatesSuccessfully() {
        String key = "templateKey";
        String htmlContent = "<html><body>New content</body></html>";
        doReturn(Optional.of(mailTemplate)).when(mailTemplateDAO).findById(key);
        when(mailTemplateDAO.save(mailTemplate)).thenReturn(mailTemplate);

        mailTemplateLogic.setFormat(key, MailTemplateFormat.HTML, htmlContent);

        verify(mailTemplateDAO, times(1)).findById(key);
        verify(mailTemplate, times(1)).setHTMLTemplate(htmlContent);
        verify(mailTemplate, never()).setTextTemplate(anyString());
        verify(mailTemplateDAO, times(1)).save(mailTemplate);
    }

    @Test
    @DisplayName("CP13: setFormat() TEXT -> aggiorna template TEXT correttamente")
    void testSetFormat_TextFormat_UpdatesSuccessfully() {
        String key = "templateKey";
        String textContent = "New plain text content";
        doReturn(Optional.of(mailTemplate)).when(mailTemplateDAO).findById(key);
        when(mailTemplateDAO.save(mailTemplate)).thenReturn(mailTemplate);

        mailTemplateLogic.setFormat(key, MailTemplateFormat.TEXT, textContent);

        verify(mailTemplateDAO, times(1)).findById(key);
        verify(mailTemplate, times(1)).setTextTemplate(textContent);
        verify(mailTemplate, never()).setHTMLTemplate(anyString());
        verify(mailTemplateDAO, times(1)).save(mailTemplate);
    }

    @Test
    @DisplayName("CP14: setFormat() con chiave non esistente -> NotFoundException")
    void testSetFormat_NonExistentKey_ThrowsNotFoundException() {
        String nonExistentKey = "nonExistentTemplate";
        String content = "Some content";
        when(mailTemplateDAO.findById(nonExistentKey)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> mailTemplateLogic.setFormat(nonExistentKey, MailTemplateFormat.HTML, content));
        
        assertTrue(exception.getMessage().contains(nonExistentKey));
        verify(mailTemplateDAO, times(1)).findById(nonExistentKey);
        verify(mailTemplateDAO, never()).save(any());
    }


    @Test
    @DisplayName("CP15: list() con DB vuoto -> lista vuota")
    void testList_EmptyDatabase_ReturnsEmptyList() {
        when(mailTemplateDAO.findAll()).thenReturn(Collections.emptyList());

        List<MailTemplateTO> result = mailTemplateLogic.list();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(mailTemplateDAO, times(1)).findAll();
    }

    @Test
    @DisplayName("CP16: list() con template nel DB -> ritorna lista corretta")
    void testList_WithTemplates_ReturnsCorrectList() {
        MailTemplate template1 = mock(MailTemplate.class);
        MailTemplate template2 = mock(MailTemplate.class);
        MailTemplate template3 = mock(MailTemplate.class);
        
        when(template1.getKey()).thenReturn("template1");
        when(template2.getKey()).thenReturn("template2");
        when(template3.getKey()).thenReturn("template3");
        
        doReturn(List.of(template1, template2, template3)).when(mailTemplateDAO).findAll();

        List<MailTemplateTO> result = mailTemplateLogic.list();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("template1", result.get(0).getKey());
        assertEquals("template2", result.get(1).getKey());
        assertEquals("template3", result.get(2).getKey());
        verify(mailTemplateDAO, times(1)).findAll();
    }
}
