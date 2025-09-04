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

@DisplayName("SecurityQuestionLogic - Category Partition (semplice)")
class SecurityQuestionLogicCategoryPartitionTest {

    @Mock private SecurityQuestionDAO securityQuestionDAO;
    @Mock private UserDAO userDAO; // non usato qui, ma richiesto dal costruttore
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

        // niente thenReturn(List.of(...)): usiamo una ArrayList di backing
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
}