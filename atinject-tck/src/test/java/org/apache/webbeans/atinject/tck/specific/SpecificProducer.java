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
package org.apache.webbeans.atinject.tck.specific;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import org.atinject.tck.auto.Drivers;
import org.atinject.tck.auto.DriversSeat;
import org.atinject.tck.auto.Seat;
import org.atinject.tck.auto.accessories.SpareTire;


@ApplicationScoped
public class SpecificProducer
{
    public SpecificProducer()
    {
        
    }
    
    @Produces @Drivers
    public Seat produceDrivers(DriversSeat seat)
    {
        return seat;
    }
    
    
    @Produces @DriverBinding @Typed(value={DriversSeat.class})
    public DriversSeat produceDriverSeat(DriversSeat seat)
    {
        return seat;
    }
    
    
    @Produces @Named("spare") @SpareBinding
    public SpareTire produceSpare(SpareTire tire)
    {
        return tire;
    }
    
    @Produces @Default @Typed(value={SpareTire.class})
    public SpareTire produceSpareTire(@TckNew SpareTire tire)
    {
        return tire;
    }
    
}
