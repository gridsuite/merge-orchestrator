/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.merge.orchestrator.server;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.NetworkFactory;
import org.gridsuite.merge.orchestrator.server.dto.CaseInfos;
import org.gridsuite.merge.orchestrator.server.dto.MergeInfos;
import org.gridsuite.merge.orchestrator.server.repositories.MergeEntity;
import org.gridsuite.merge.orchestrator.server.repositories.MergeEntityKey;
import org.gridsuite.merge.orchestrator.server.repositories.MergeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * @author Jon Harper <jon.harper at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@Service
public class MergeOrchestratorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MergeOrchestratorService.class);

    private static final String CATEGORY_BROKER_INPUT = MergeOrchestratorService.class.getName()
            + ".input-broker-messages";

    private static final String DATE_HEADER_KEY         = "date";
    private static final String GEO_CODE_HEADER_KEY     = "geographicalCode";

    private MergeRepository mergeRepository;

    private CaseFetcherService caseFetcherService;

    private BalancesAdjustmentService balancesAdjustmentService;

    private CopyToNetworkStoreService copyToNetworkStoreService;

    private LoadFlowService loadFlowService;

    private MergeEventService mergeEventService;

    private MergeOrchestratorConfigService mergeConfigService;

    public MergeOrchestratorService(CaseFetcherService caseFetchService,
                                    BalancesAdjustmentService balancesAdjustmentService,
                                    CopyToNetworkStoreService copyToNetworkStoreService,
                                    MergeEventService mergeEventService,
                                    LoadFlowService loadFlowService,
                                    MergeRepository mergeRepository,
                                    MergeOrchestratorConfigService mergeConfigService) {
        this.caseFetcherService = caseFetchService;
        this.balancesAdjustmentService = balancesAdjustmentService;
        this.copyToNetworkStoreService = copyToNetworkStoreService;
        this.mergeEventService = mergeEventService;
        this.loadFlowService = loadFlowService;
        this.mergeRepository = mergeRepository;
        this.mergeConfigService = mergeConfigService;
    }

    @Bean
    public Consumer<Flux<Message<String>>> consumeNotification() {
        return f -> f.log(CATEGORY_BROKER_INPUT, Level.FINE).subscribe(this::consume);
    }

    public void consume(Message<String> message) {
        try {
            List<String> tsos = mergeConfigService.getTsos();
            MessageHeaders mh = message.getHeaders();
            String date = (String) mh.get(DATE_HEADER_KEY);
            String tso = (String) mh.get(GEO_CODE_HEADER_KEY);

            LOGGER.info("**** MERGE ORCHESTRATOR : message received : date={} tso={} ****", date, tso);

            if (tsos.contains(tso)) {
                // required tso received
                ZonedDateTime dateTime = ZonedDateTime.parse(date);

                mergeEventService.addMergeEvent("", tso, "TSO_IGM", dateTime, null, mergeConfigService.getProcess());

                List<CaseInfos> list = caseFetcherService.getCases(tsos, dateTime);

                if (list.size() == tsos.size()) {
                    // all tsos are available for the merging process
                    mergeEventService.addMergeEvent("", tsos.toString(), "MERGE_PROCESS_STARTED", dateTime, null, mergeConfigService.getProcess());

                    // creation of an empty merge network
                    Network merged = NetworkFactory.findDefault().createNetwork("merged", "iidm");

                    mergeEventService.addMergeEvent("", tsos.toString(), "READ_NETWORKS_STARTED", dateTime, null, mergeConfigService.getProcess());

                    // merge of the tsos networks into merge network
                    List<Network> listNetworks = new ArrayList<>();
                    for (CaseInfos info : list) {
                        UUID id = info.getUuid();
                        Network network = caseFetcherService.getCase(id);
                        listNetworks.add(network);
                    }

                    mergeEventService.addMergeEvent("", tsos.toString(), "READ_NETWORKS_FINISHED", dateTime, null, mergeConfigService.getProcess());

                    LOGGER.info("**** MERGE ORCHESTRATOR : merging cases ******");

                    mergeEventService.addMergeEvent("", tsos.toString(), "MERGE_NETWORKS_STARTED", dateTime, null, mergeConfigService.getProcess());

                    merged.merge(listNetworks.toArray(new Network[listNetworks.size()]));

                    mergeEventService.addMergeEvent("", tsos.toString(), "MERGE_NETWORKS_FINISHED", dateTime, null, mergeConfigService.getProcess());

                    LOGGER.info("**** MERGE ORCHESTRATOR : copy to network store ******");

                    // store the merge network in the network store
                    UUID mergeUuid = copyToNetworkStoreService.copy(merged);

                    mergeEventService.addMergeEvent("", tsos.toString(), "MERGED_NETWORK_STORED", dateTime, mergeUuid, mergeConfigService.getProcess());

                    if (mergeConfigService.isRunBalancesAdjustment()) {
                        // balances adjustment on the merge network
                        LOGGER.info("**** MERGE ORCHESTRATOR : balances adjustment ******");
                        mergeEventService.addMergeEvent("", tsos.toString(), "BALANCE_ADJUSTMENT_STARTED", dateTime, mergeUuid, mergeConfigService.getProcess());
                        balancesAdjustmentService.doBalance(mergeUuid);
                        mergeEventService.addMergeEvent("", tsos.toString(), "BALANCE_ADJUSTMENT_FINISHED", dateTime, mergeUuid, mergeConfigService.getProcess());

                    } else {
                        // load flow on the merged network
                        LOGGER.info("**** MERGE ORCHESTRATOR : load flow ******");
                        mergeEventService.addMergeEvent("", tsos.toString(), "LOAD_FLOW_STARTED", dateTime, mergeUuid, mergeConfigService.getProcess());
                        loadFlowService.run(mergeUuid);
                        mergeEventService.addMergeEvent("", tsos.toString(), "LOAD_FLOW_FINISHED", dateTime, mergeUuid, mergeConfigService.getProcess());
                    }

                    mergeEventService.addMergeEvent("", tsos.toString(), "MERGE_PROCESS_FINISHED", dateTime, mergeUuid, mergeConfigService.getProcess());

                    LOGGER.info("**** MERGE ORCHESTRATOR : end ******");
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error : {}", e.getMessage());
        }
    }

    List<MergeInfos> getMergesList() {
        List<MergeEntity> mergeList = mergeRepository.findAll();
        return mergeList.stream().map(m -> new MergeInfos(m.getKey().getProcess(),
                ZonedDateTime.ofInstant(m.getKey().getDate().toInstant(ZoneOffset.UTC), ZoneId.of("UTC")),
                m.getStatus())).collect(Collectors.toList());
    }

    List<MergeInfos> getProcessMergesList(String process) {
        List<MergeEntity> mergeList = mergeRepository.findByProcess(process);
        return mergeList.stream().map(m -> new MergeInfos(m.getKey().getProcess(),
                ZonedDateTime.ofInstant(m.getKey().getDate().toInstant(ZoneOffset.UTC), ZoneId.of("UTC")),
                m.getStatus())).collect(Collectors.toList());
    }

    Optional<MergeInfos> getMerge(String process, ZonedDateTime dateTime) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(dateTime.toInstant(), ZoneOffset.UTC);
        Optional<MergeEntity> merge = mergeRepository.findById(new MergeEntityKey(process, localDateTime));
        return merge.map(this::toMergeInfo);
    }

    private MergeInfos toMergeInfo(MergeEntity mergeEntity) {
        return new MergeInfos(mergeEntity.getKey().getProcess(),
                ZonedDateTime.ofInstant(mergeEntity.getKey().getDate().toInstant(ZoneOffset.UTC), ZoneId.of("UTC")),
                mergeEntity.getStatus());
    }
}
