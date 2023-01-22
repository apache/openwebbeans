/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.reservation.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.apache.webbeans.reservation.controller.admin.AdminController;

/**
 * Initialize an admin and a normal user in the db.
 */
@ApplicationScoped
public class InitializeDatabase
{
    private @Inject RegisterController registerController;
    private @Inject AdminController adminController;

    public void initUsers(@Observes @Initialized(ApplicationScoped.class) Object payload)
    {
        registerController.registerUser("administrator", "administrator", "administrator", "administrator", 66, true);
        registerController.registerUser("customer", "customer", "customer", "customer", 27, false);

        adminController.createNewHotel("Pillton", 5, "New York", "USA");
        adminController.createNewHotel("Karriot", 5, "Paris", "France");
        adminController.createNewHotel("Oper", 5, "Vienna", "Austria");
    }

}
