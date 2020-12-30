/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.merge.orchestrator.server;

import org.gridsuite.merge.orchestrator.server.repositories.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import javax.inject.Inject;
import java.util.ArrayList;

import static com.powsybl.network.store.model.NetworkStoreApi.VERSION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com>
 */
@RunWith(SpringRunner.class)
@WebMvcTest(ProcessConfigController.class)
@ContextConfiguration(classes = {MergeOrchestratorApplication.class})
public class ProcessConfigControllerTest extends AbstractEmbeddedCassandraSetup {
    @Autowired
    private MockMvc mvc;

    @Inject
    ProcessConfigRepository processConfigRepository;

    @MockBean
    private IgmQualityCheckService igmQualityCheckService;

    @MockBean
    private CaseFetcherService caseFetcherService;

    @MockBean
    private BalancesAdjustmentService balancesAdjustmentService;

    @MockBean
    private LoadFlowService loadFlowService;

    @MockBean
    private NetworkConversionService networkConversionService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ArrayList<String> tsos = new ArrayList<>();
        tsos.add("FR");
        tsos.add("ES");
        tsos.add("PT");
        processConfigRepository.save(new ProcessConfigEntity("SWE", tsos, false));
    }

    @Test
    public void configTest() throws Exception {
        mvc.perform(get("/" + VERSION + "/configs")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[{\"process\":\"SWE\",\"tsos\":[\"FR\",\"ES\",\"PT\"]}]"));

        mvc.perform(get("/" + VERSION + "/configs/SWE")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("{\"process\":\"SWE\",\"tsos\":[\"FR\",\"ES\",\"PT\"]}"));

        mvc.perform(delete("/" + VERSION + "/configs/SWE")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        mvc.perform(get("/" + VERSION + "/configs")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[]"));

        mvc.perform(post("/" + VERSION + "/configs")
                .contentType(APPLICATION_JSON)
                .content("[{\"process\":\"A\",\"tsos\":[\"FR\",\"ES\",\"PT\"], \"runBalancesAdjustment\":\"false\"}, " +
                        "{\"process\":\"B\",\"tsos\":[\"ES\",\"PT\"], \"runBalancesAdjustment\":\"false\"}]"))
                .andExpect(status().isOk());

        mvc.perform(get("/" + VERSION + "/configs")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[{\"process\":\"A\",\"tsos\":[\"FR\",\"ES\",\"PT\"]}, " +
                        "{\"process\":\"B\",\"tsos\":[\"ES\",\"PT\"]}]"));
    }
}