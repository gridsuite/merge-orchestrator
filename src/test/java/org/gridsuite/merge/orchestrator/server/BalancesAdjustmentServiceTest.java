/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.merge.orchestrator.server;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@RunWith(MockitoJUnitRunner.class)
public class BalancesAdjustmentServiceTest {

    @Mock
    private RestTemplate balancesAdjustmentServerRest;

    private BalancesAdjustmentService balancesAdjustmentService;

    private UUID networkUuid1 = UUID.fromString("47b85a5c-44ec-4afc-9f7e-29e63368e83d");
    private UUID networkUuid2 = UUID.fromString("da47a173-22d2-47e8-8a84-aa66e2d0fafb");
    private UUID networkUuid3 = UUID.fromString("4d6ac8c0-eaea-4b1c-8d28-a4297ad480b5");

    @Before
    public void setUp() {
        balancesAdjustmentService = new BalancesAdjustmentService(balancesAdjustmentServerRest);
    }

    @Test
    public void test() {
        when(balancesAdjustmentServerRest.exchange(anyString(),
                eq(HttpMethod.PUT),
                any(),
                eq(String.class),
                eq(networkUuid1.toString())))
                .thenReturn(ResponseEntity.ok("{\"status\": \"SUCCESS\"}"));
        String res = balancesAdjustmentService.doBalance(Arrays.asList(networkUuid1, networkUuid2, networkUuid3));
        assertEquals("{\"status\": \"SUCCESS\"}", res);
    }
}
