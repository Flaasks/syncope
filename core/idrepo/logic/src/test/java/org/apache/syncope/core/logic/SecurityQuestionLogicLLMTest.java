package org.apache.syncope.core.logic;

import org.apache.syncope.common.lib.to.SecurityQuestionTO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.SecurityQuestionDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.SecurityQuestion;
import org.apache.syncope.core.provisioning.api.data.SecurityQuestionDataBinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.syncope.core.persistence.api.entity.user.User;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import org.mockito.*;

import java.util.stream.IntStream;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityQuestionLogic - LLM style fuzz/property tests")
class SecurityQuestionLogicLLMTest {

    @Mock private SecurityQuestionDAO securityQuestionDAO;
    @Mock private UserDAO userDAO;
    @Mock private SecurityQuestionDataBinder binder;

    private SecurityQuestionLogic logic;

    @BeforeEach
    void setUp() {
        logic = new SecurityQuestionLogic(securityQuestionDAO, userDAO, binder);
    }


    @RepeatedTest(50)
    @DisplayName("list: numero TO corrisponde a numero entity (fuzz su size 0..10)")
    void list_randomSizedCollections() {
        int size = ThreadLocalRandom.current().nextInt(0, 11); // 0..10
        List<SecurityQuestion> entities = new ArrayList<>();
        List<SecurityQuestionTO> mapped = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            SecurityQuestion e = mock(SecurityQuestion.class);
            SecurityQuestionTO to = new SecurityQuestionTO();
            to.setKey("K" + i);
            entities.add(e);
            mapped.add(to);

            when(binder.getSecurityQuestionTO(e)).thenReturn(to);
        }

        doReturn(entities).when(securityQuestionDAO).findAll();

        List<SecurityQuestionTO> out = logic.list();

        assertNotNull(out);
        assertEquals(size, out.size(), "il numero di TO deve corrispondere al numero di entity");
        for (SecurityQuestionTO to : mapped) {
            assertTrue(out.contains(to), "l'output deve contenere tutti i TO mappati");
        }
    }


    @RepeatedTest(30)
    @DisplayName("read: se entity esiste → TO non nullo; se non esiste → NotFoundException")
    void read_randomKeys() {
        String key = "key-" + UUID.randomUUID();
        boolean exists = ThreadLocalRandom.current().nextBoolean();

        if (exists) {
            SecurityQuestion e = mock(SecurityQuestion.class);
            SecurityQuestionTO to = new SecurityQuestionTO();
            to.setKey(key);

            doReturn(Optional.of(e)).when(securityQuestionDAO).findById(key);
            when(binder.getSecurityQuestionTO(e)).thenReturn(to);

            SecurityQuestionTO out = logic.read(key);

            assertNotNull(out);
            assertEquals(key, out.getKey());
            verify(binder).getSecurityQuestionTO(e);

        } else {
            doReturn(Optional.empty()).when(securityQuestionDAO).findById(key);

            assertThrows(NotFoundException.class, () -> logic.read(key));
            verifyNoInteractions(binder);
        }
    }

    // ========== list(): cardinalità casuale 0..15 ==========
    @RepeatedTest(30)
    @DisplayName("list(): |TO| == |entity| per size random 0..15")
    void list_sizeMatchesEntities_random() {
        int size = ThreadLocalRandom.current().nextInt(0, 16);

        List<SecurityQuestion> entities = new ArrayList<>(size);
        List<SecurityQuestionTO> mapped = new ArrayList<>(size);

        IntStream.range(0, size).forEach(i -> {
            SecurityQuestion e = mock(SecurityQuestion.class);
            SecurityQuestionTO to = new SecurityQuestionTO(); to.setKey("K" + i);
            entities.add(e);
            mapped.add(to);
            when(binder.getSecurityQuestionTO(e)).thenReturn(to);
        });

        doReturn(entities).when(securityQuestionDAO).findAll();

        List<SecurityQuestionTO> out = logic.list();

        assertNotNull(out);
        assertEquals(size, out.size(), "la dimensione dei TO deve eguagliare le entity");
        mapped.forEach(to -> assertTrue(out.contains(to), "tutti i TO mappati devono comparire in output"));
    }

    // ========== read(): presenza/mancanza casuale ==========
    @RepeatedTest(30)
    @DisplayName("read(): exists→TO coerente; notFound→NotFoundException (random)")
    void read_randomPresence() {
        String key = "key-" + UUID.randomUUID();
        boolean exists = ThreadLocalRandom.current().nextBoolean();

        if (exists) {
            SecurityQuestion entity = mock(SecurityQuestion.class);
            SecurityQuestionTO to = new SecurityQuestionTO(); to.setKey(key);

            doReturn(Optional.of(entity)).when(securityQuestionDAO).findById(key);
            when(binder.getSecurityQuestionTO(entity)).thenReturn(to);

            SecurityQuestionTO out = logic.read(key);
            assertNotNull(out);
            assertEquals(key, out.getKey());
            verify(binder).getSecurityQuestionTO(entity);
        } else {
            doReturn(Optional.empty()).when(securityQuestionDAO).findById(key);
            assertThrows(NotFoundException.class, () -> logic.read(key));
            verifyNoInteractions(binder);
        }
    }

    // ========== create(): TO random “plausibile” ⇒ binder.create→dao.save→mapping ==========
    @RepeatedTest(20)
    @DisplayName("create(): TO plausibile → binder.create → dao.save → mapping")
    void create_randomValidTO_pipeline() {
        SecurityQuestionTO in = new SecurityQuestionTO();
        // contenuto casuale non vuoto (ma il binder è mock: evitiamo errori)
        in.setContent("Q-" + UUID.randomUUID());

        SecurityQuestion created = mock(SecurityQuestion.class);
        SecurityQuestion saved = mock(SecurityQuestion.class);
        SecurityQuestionTO out = new SecurityQuestionTO(); out.setKey("NEW-" + UUID.randomUUID());

        when(binder.create(in)).thenReturn(created);
        when(securityQuestionDAO.save(created)).thenReturn(saved);
        when(binder.getSecurityQuestionTO(saved)).thenReturn(out);

        SecurityQuestionTO res = logic.create(in);

        assertNotNull(res, "create deve restituire un TO");
        assertEquals(out.getKey(), res.getKey());
        InOrder order = inOrder(binder, securityQuestionDAO, binder);
        order.verify(binder).create(in);
        order.verify(securityQuestionDAO).save(created);
        order.verify(binder).getSecurityQuestionTO(saved);
    }

    // ========== update(): presenza random, binder.update(void) + save ==========
    @RepeatedTest(30)
    @DisplayName("update(): exists→update+save; notFound→eccezione (random)")
    void update_randomPresence() {
        String key = "U-" + UUID.randomUUID();
        SecurityQuestionTO in = new SecurityQuestionTO(); in.setKey(key);
        in.setContent("C-" + UUID.randomUUID());

        boolean exists = ThreadLocalRandom.current().nextBoolean();

        if (exists) {
            SecurityQuestion entity = mock(SecurityQuestion.class);
            SecurityQuestion saved = mock(SecurityQuestion.class);
            SecurityQuestionTO out = new SecurityQuestionTO(); out.setKey(key);

            doReturn(Optional.of(entity)).when(securityQuestionDAO).findById(key);
            // update è void → doNothing
            doNothing().when(binder).update(entity, in);
            when(securityQuestionDAO.save(entity)).thenReturn(saved);
            when(binder.getSecurityQuestionTO(saved)).thenReturn(out);

            SecurityQuestionTO res = logic.update(in);

            assertNotNull(res);
            assertEquals(key, res.getKey());
            InOrder inOrder = inOrder(securityQuestionDAO, binder, securityQuestionDAO, binder);
            inOrder.verify(securityQuestionDAO).findById(key);
            inOrder.verify(binder).update(entity, in);
            inOrder.verify(securityQuestionDAO).save(entity);
            inOrder.verify(binder).getSecurityQuestionTO(saved);

        } else {
            doReturn(Optional.empty()).when(securityQuestionDAO).findById(key);
            assertThrows(NotFoundException.class, () -> logic.update(in));
            verify(binder, never()).update(any(), any());
            verify(securityQuestionDAO, never()).save(any());
        }
    }

    // ========== delete(): presenza random, deleteById(void) ==========
    @RepeatedTest(30)
    @DisplayName("delete(): exists→deleteById + ritorno TO; notFound→eccezione (random)")
    void delete_randomPresence() {
        String key = "D-" + UUID.randomUUID();
        boolean exists = ThreadLocalRandom.current().nextBoolean();

        if (exists) {
            SecurityQuestion entity = mock(SecurityQuestion.class);
            SecurityQuestionTO to = new SecurityQuestionTO(); to.setKey(key);

            doReturn(Optional.of(entity)).when(securityQuestionDAO).findById(key);
            when(binder.getSecurityQuestionTO(entity)).thenReturn(to);
            doNothing().when(securityQuestionDAO).deleteById(key);

            SecurityQuestionTO res = logic.delete(key);

            assertNotNull(res);
            assertEquals(key, res.getKey());
            InOrder inOrder = inOrder(securityQuestionDAO, binder, securityQuestionDAO);
            inOrder.verify(securityQuestionDAO).findById(key);
            inOrder.verify(binder).getSecurityQuestionTO(entity);
            inOrder.verify(securityQuestionDAO).deleteById(key);

        } else {
            doReturn(Optional.empty()).when(securityQuestionDAO).findById(key);
            assertThrows(NotFoundException.class, () -> logic.delete(key));
            verify(securityQuestionDAO, never()).deleteById(anyString());
            verifyNoInteractions(binder);
        }
    }

    // ========== readByUser(): scenario random tra 4 stati ==========
    @RepeatedTest(30)
    @DisplayName("readByUser(): null / missing / noSQ / ok (random scenario)")
    void readByUser_randomScenarios() {
        int scenario = ThreadLocalRandom.current().nextInt(0, 4);
        String username = "u-" + UUID.randomUUID();

        switch (scenario) {
            case 0: // username null → NotFound
                assertThrows(NotFoundException.class, () -> logic.readByUser(null));
                break;

            case 1: // user inesistente → NotFound
                doReturn(Optional.empty()).when(userDAO).findByUsername(username);
                assertThrows(NotFoundException.class, () -> logic.readByUser(username));
                verify(userDAO).findByUsername(username);
                verifyNoInteractions(binder);
                break;

            case 2: // user senza SQ → NotFound
                User uNo = mock(User.class);
                doReturn(Optional.of(uNo)).when(userDAO).findByUsername(username);
                when(uNo.getSecurityQuestion()).thenReturn(null);
                assertThrows(NotFoundException.class, () -> logic.readByUser(username));
                verify(userDAO).findByUsername(username);
                verify(uNo).getSecurityQuestion();
                verifyNoInteractions(binder);
                break;

            case 3: // user con SQ → mapping OK
                User uOk = mock(User.class);
                SecurityQuestion sq = mock(SecurityQuestion.class);
                SecurityQuestionTO to = new SecurityQuestionTO(); to.setKey("SQ-" + UUID.randomUUID());

                doReturn(Optional.of(uOk)).when(userDAO).findByUsername(username);
                when(uOk.getSecurityQuestion()).thenReturn(sq);
                when(binder.getSecurityQuestionTO(sq)).thenReturn(to);

                SecurityQuestionTO res = logic.readByUser(username);
                assertNotNull(res);
                assertEquals(to.getKey(), res.getKey());

                InOrder inOrder = inOrder(userDAO, binder);
                inOrder.verify(userDAO).findByUsername(username);
                inOrder.verify(binder).getSecurityQuestionTO(sq);
                break;

            default:
                fail("scenario inatteso");
        }
    }
}
