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
package com.jappstart.controller.task;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.jappstart.service.auth.EnhancedUserDetailsService;
import com.jappstart.service.mail.MailService;

/**
 * The registration controller.
 */
@Controller
@RequestMapping("/task/mail")
public class MailTask {

    /**
     * The mail service.
     */
    private MailService mailService;

    /**
     * The user details service.
     */
    private EnhancedUserDetailsService userDetailsService;

    /**
     * Gets the mail service.
     *
     * @return the mail service
     */
    public final MailService getMailService() {
        return mailService;
    }

    /**
     * Sets the mail service.
     *
     * @param mailService the mail service
     */
    @Autowired
    public final void setMailService(final MailService mailService) {
        this.mailService = mailService;
    }

    /**
     * Gets the user details service.
     *
     * @return the user details service
     */
    public final EnhancedUserDetailsService getUserDetailsService() {
        return userDetailsService;
    }

    /**
     * Sets the user details service.
     *
     * @param userDetailsService the user details service
     */
    @Autowired
    public final void setUserDetailsService(
        final EnhancedUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    /**
     * Sends the activation e-mail.
     *
     * @param username the username
     * @param response the servlet response
     */
    @RequestMapping(value = "/activate", method = RequestMethod.POST)
    public final void sendActivationMail(@RequestParam final String username,
        final HttpServletResponse response) {
        try {
            mailService.sendActivationEmail(
                userDetailsService.getUser(username));
        } catch (MessagingException e) {
            response.setStatus(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

}
