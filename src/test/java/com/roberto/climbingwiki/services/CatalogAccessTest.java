package com.roberto.climbingwiki.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.roberto.climbingwiki.exceptions.CatalogItemInUseException;
import com.roberto.climbingwiki.exceptions.ForbiddenException;
import com.roberto.climbingwiki.model.entities.Crag;
import com.roberto.climbingwiki.model.entities.User;
import com.roberto.climbingwiki.repositories.UserRepository;
import com.roberto.climbingwiki.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogAccessTest {

    private static final AuthenticatedUser OWNER = new AuthenticatedUser(1L, "owner", User.Role.USER);
    private static final AuthenticatedUser OTHER = new AuthenticatedUser(2L, "other", User.Role.USER);
    private static final AuthenticatedUser MODERATOR = new AuthenticatedUser(3L, "mod", User.Role.MODERATOR);
    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(4L, "admin", User.Role.ADMIN);

    @Mock
    private UserRepository userRepository;

    private CatalogAccess access;

    @BeforeEach
    void setUp() {
        access = new CatalogAccess(userRepository);
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private static Crag crag(Long ownerId, boolean verified) {
        Crag crag = new Crag();
        crag.setCreatedBy(ownerId != null ? user(ownerId) : null);
        crag.setVerified(verified);
        return crag;
    }

    @Test
    void stampCreation_byRegularUser_setsCreatorAndLeavesUnverified() {
        when(userRepository.getReferenceById(1L)).thenReturn(user(1L));
        Crag crag = new Crag();

        access.stampCreation(crag, OWNER);

        assertThat(crag.getCreatedBy().getId()).isEqualTo(1L);
        assertThat(crag.isVerified()).isFalse();
    }

    @Test
    void stampCreation_byModerator_isVerifiedStraightAway() {
        when(userRepository.getReferenceById(3L)).thenReturn(user(3L));
        Crag crag = new Crag();

        access.stampCreation(crag, MODERATOR);

        assertThat(crag.isVerified()).isTrue();
    }

    @Test
    void checkCanEdit_ownerOfUnverifiedItem_isAllowed() {
        assertThatCode(() -> access.checkCanEdit(crag(1L, false), OWNER)).doesNotThrowAnyException();
    }

    @Test
    void checkCanEdit_ownerOfVerifiedItem_isForbidden() {
        assertThatThrownBy(() -> access.checkCanEdit(crag(1L, true), OWNER)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void checkCanEdit_otherUser_isForbidden() {
        assertThatThrownBy(() -> access.checkCanEdit(crag(1L, false), OTHER)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void checkCanEdit_itemWithoutCreator_isForbiddenForRegularUsers() {
        assertThatThrownBy(() -> access.checkCanEdit(crag(null, false), OWNER)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void checkCanEdit_moderatorAndAdmin_canEditAnything() {
        assertThatCode(() -> access.checkCanEdit(crag(1L, true), MODERATOR)).doesNotThrowAnyException();
        assertThatCode(() -> access.checkCanEdit(crag(null, true), ADMIN)).doesNotThrowAnyException();
    }

    @Test
    void checkCanDelete_ownerOfEmptyUnverifiedItem_isAllowed() {
        assertThatCode(() -> access.checkCanDelete(crag(1L, false), OWNER, false)).doesNotThrowAnyException();
    }

    @Test
    void checkCanDelete_ownerOfItemWithChildren_isRejected() {
        assertThatThrownBy(() -> access.checkCanDelete(crag(1L, false), OWNER, true))
                .isInstanceOf(CatalogItemInUseException.class);
    }

    @Test
    void checkCanDelete_otherUser_isForbiddenEvenWithoutChildren() {
        assertThatThrownBy(() -> access.checkCanDelete(crag(1L, false), OTHER, false))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void checkCanDelete_moderator_canDeleteItemWithChildren() {
        assertThatCode(() -> access.checkCanDelete(crag(1L, true), MODERATOR, true)).doesNotThrowAnyException();
    }

    @Test
    void setVerified_byModerator_changesState() {
        Crag crag = crag(1L, false);

        access.setVerified(crag, true, MODERATOR);

        assertThat(crag.isVerified()).isTrue();
    }

    @Test
    void setVerified_byRegularUser_isForbidden() {
        Crag crag = crag(1L, false);

        assertThatThrownBy(() -> access.setVerified(crag, true, OWNER)).isInstanceOf(ForbiddenException.class);
        assertThat(crag.isVerified()).isFalse();
    }
}
