package com.roberto.climbingwiki.services;

import com.roberto.climbingwiki.exceptions.CatalogItemInUseException;
import com.roberto.climbingwiki.exceptions.ForbiddenException;
import com.roberto.climbingwiki.model.entities.CatalogEntity;
import com.roberto.climbingwiki.repositories.UserRepository;
import com.roberto.climbingwiki.security.AuthenticatedUser;
import org.springframework.stereotype.Component;

/**
 * Who may change climbing data.
 * <ul>
 *   <li>Any logged-in user can add data. It is unverified unless a moderator or admin added it.</li>
 *   <li>Regular users can edit or delete only their own unverified items, and delete only when nothing
 *       is under them (deletes cascade to child data that may belong to others).</li>
 *   <li>Moderators and admins can edit, delete and verify anything.</li>
 * </ul>
 */
@Component
public class CatalogAccess {

    private final UserRepository userRepository;

    public CatalogAccess(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void stampCreation(CatalogEntity item, AuthenticatedUser requester) {
        item.setCreatedBy(userRepository.getReferenceById(requester.id()));
        item.setVerified(requester.isModeratorOrAdmin());
    }

    public void checkCanEdit(CatalogEntity item, AuthenticatedUser requester) {
        if (requester.isModeratorOrAdmin()) {
            return;
        }
        boolean ownsUnverifiedItem = !item.isVerified() && requester.id().equals(item.getCreatedById());
        if (!ownsUnverifiedItem) {
            throw new ForbiddenException();
        }
    }

    public void checkCanDelete(CatalogEntity item, AuthenticatedUser requester, boolean hasChildren) {
        checkCanEdit(item, requester);
        if (hasChildren && !requester.isModeratorOrAdmin()) {
            throw new CatalogItemInUseException();
        }
    }

    public void setVerified(CatalogEntity item, boolean verified, AuthenticatedUser requester) {
        if (!requester.isModeratorOrAdmin()) {
            throw new ForbiddenException();
        }
        item.setVerified(verified);
    }
}
