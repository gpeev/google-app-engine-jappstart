/*
 *  Copyright (C) 2010 Taylor Leese (tleese22@gmail.com)
 *
 *  This file is part of jappstart.
 *
 *  jappstart is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  jappstart is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with jappstart.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.jappstart.service.auth;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.labs.taskqueue.Queue;
import com.google.appengine.api.labs.taskqueue.QueueFactory;
import com.google.appengine.api.labs.taskqueue.TaskOptions;
import com.jappstart.exception.DuplicateUserException;
import com.jappstart.model.auth.UserAccount;

/**
 * The user details service implementation.
 */
@Service
public class UserDetailsServiceImpl implements EnhancedUserDetailsService {

	/**
     * The username field name.
     */
    private static final String USERNAME = "username";

    /**
     * The entity manager.
     */
    @PersistenceContext
    private transient EntityManager entityManager;

    /**
     * The mail task name.
     */
    private String mailTaskName;

    /**
     * The mail task URL.
     */
    private String mailTaskUrl;

    /**
     * Returns the mail task name.
     *
     * @return the mail task name
     */
    public final String getMailTaskName() {
        return mailTaskName;
    }

    /**
     * Sets the mail task name.
     *
     * @param mailTaskName the mail task name
     */
    public final void setMailTaskName(final String mailTaskName) {
        this.mailTaskName = mailTaskName;
    }

    /**
     * Returns the mail task URL.
     *
     * @return the mail task URL
     */
    public final String getMailTaskUrl() {
        return mailTaskUrl;
    }

    /**
     * Sets the mail task URL.
     *
     * @param mailTaskUrl the mail task URL
     */
    public final void setMailTaskUrl(final String mailTaskUrl) {
        this.mailTaskUrl = mailTaskUrl;
    }

    /**
     * Locates the user based on the username.
     *
     * @param username string the username
     * @return the user details
     */
    @Override
    public final UserDetails loadUserByUsername(final String username) {
        final List<GrantedAuthority> authorities =
            new ArrayList<GrantedAuthority>();

        final Query query = entityManager.createQuery(
            "SELECT u FROM UserAccount u WHERE username = :username");
        query.setParameter("username", username);

        try {
            final UserAccount user = (UserAccount) query.getSingleResult();

            authorities.add(new GrantedAuthorityImpl(user.getRole()));

            final UserDetails userDetails = new EnhancedUser(user.getUsername(),
                user.getPassword(), user.getSalt(), user.isEnabled(),
                user.isAccountNonExpired(), user.isCredentialsNonExpired(),
                user.isAccountNonLocked(), authorities);

            return userDetails;
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Returns the user account for the given username.
     *
     * @param username the username
     * @return the user account
     */
    @Override
    public final UserAccount getUser(final String username) {
        final Query query = entityManager.createQuery(
            "SELECT u FROM UserAccount u WHERE username = :username");
        query.setParameter(USERNAME, username);

        try {
            return (UserAccount) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Adds a user.
     *
     * @param user the user
     */
    @Override
    @Transactional
    public final void addUser(final UserAccount user) {
        final Query query = entityManager.createQuery(
            "SELECT u FROM UserAccount u WHERE username = :username");
        query.setParameter(USERNAME, user.getUsername());

        @SuppressWarnings("unchecked")
        final List results = query.getResultList();
        if (results != null && !results.isEmpty()) {
            throw new DuplicateUserException();
        }

        entityManager.persist(user);

        final TaskOptions taskOptions =
            TaskOptions.Builder.url(mailTaskUrl)
            .param("username", user.getUsername());

        final Queue queue = QueueFactory.getQueue(mailTaskName);
        queue.add(DatastoreServiceFactory.getDatastoreService()
            .getCurrentTransaction(), taskOptions);
    }

    /**
     * Activates the user with the given activation key.
     *
     * @param key the activation key
     * @return true if successful; false otherwise
     */
    @Override
    @Transactional
    public final boolean activateUser(final String key) {
        final Query query = entityManager.createQuery(
            "SELECT u FROM UserAccount u WHERE activationKey = :key");
        query.setParameter("key", key);

        try {
            final UserAccount user = (UserAccount) query.getSingleResult();
            user.setEnabled(true);
            entityManager.persist(user);

            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

}
