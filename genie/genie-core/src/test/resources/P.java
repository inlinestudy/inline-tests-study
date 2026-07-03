package test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.StatusRuntimeException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.protos.common.Common.Block;
import org.hyperledger.fabric.protos.common.Common.BlockMetadata;
import org.hyperledger.fabric.protos.common.Common.ChannelHeader;
import org.hyperledger.fabric.protos.common.Common.Envelope;
import org.hyperledger.fabric.protos.common.Common.Header;
import org.hyperledger.fabric.protos.common.Common.HeaderType;
import org.hyperledger.fabric.protos.common.Common.LastConfig;
import org.hyperledger.fabric.protos.common.Common.Metadata;
import org.hyperledger.fabric.protos.common.Common.Payload;
import org.hyperledger.fabric.protos.common.Common.Status;
import org.hyperledger.fabric.protos.common.Configtx;
import org.hyperledger.fabric.protos.common.Configtx.ConfigEnvelope;
import org.hyperledger.fabric.protos.common.Configtx.ConfigGroup;
import org.hyperledger.fabric.protos.common.Configtx.ConfigSignature;
import org.hyperledger.fabric.protos.common.Configtx.ConfigUpdateEnvelope;
import org.hyperledger.fabric.protos.common.Configtx.ConfigValue;
import org.hyperledger.fabric.protos.common.Ledger;
import org.hyperledger.fabric.protos.discovery.Protocol;
import org.hyperledger.fabric.protos.msp.MspConfig;
import org.hyperledger.fabric.protos.orderer.Ab;
import org.hyperledger.fabric.protos.orderer.Ab.BroadcastResponse;
import org.hyperledger.fabric.protos.orderer.Ab.DeliverResponse;
import org.hyperledger.fabric.protos.orderer.Ab.SeekInfo;
import org.hyperledger.fabric.protos.orderer.Ab.SeekPosition;
import org.hyperledger.fabric.protos.orderer.Ab.SeekSpecified;
import org.hyperledger.fabric.protos.peer.Configuration;
import org.hyperledger.fabric.protos.peer.FabricProposal;
import org.hyperledger.fabric.protos.peer.FabricProposal.SignedProposal;
import org.hyperledger.fabric.protos.peer.FabricProposalResponse;
import org.hyperledger.fabric.protos.peer.FabricProposalResponse.Response;
import org.hyperledger.fabric.protos.peer.FabricTransaction.ProcessedTransaction;
import org.hyperledger.fabric.protos.peer.Query;
import org.hyperledger.fabric.protos.peer.Query.ChaincodeInfo;
import org.hyperledger.fabric.protos.peer.Query.ChaincodeQueryResponse;
import org.hyperledger.fabric.protos.peer.Query.ChannelQueryResponse;
import org.hyperledger.fabric.sdk.BlockEvent.TransactionEvent;
import org.hyperledger.fabric.sdk.Peer.PeerRole;
import org.hyperledger.fabric.sdk.ServiceDiscovery.SDChaindcode;
import org.hyperledger.fabric.sdk.ServiceDiscovery.SDEndorser;
import org.hyperledger.fabric.sdk.ServiceDiscovery.SDEndorserState;
import org.hyperledger.fabric.sdk.ServiceDiscovery.SDNetwork;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.EventHubException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.ServiceDiscoveryException;
import org.hyperledger.fabric.sdk.exception.TransactionEventException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.helper.Config;
import org.hyperledger.fabric.sdk.helper.DiagnosticFileDumper;
import org.hyperledger.fabric.sdk.helper.Utils;
import org.hyperledger.fabric.sdk.security.certgen.TLSCertificateBuilder;
import org.hyperledger.fabric.sdk.security.certgen.TLSCertificateKeyPair;
import org.hyperledger.fabric.sdk.transaction.GetConfigBlockBuilder;
import org.hyperledger.fabric.sdk.transaction.InstallProposalBuilder;
import org.hyperledger.fabric.sdk.transaction.InstantiateProposalBuilder;
import org.hyperledger.fabric.sdk.transaction.JoinPeerProposalBuilder;
import org.hyperledger.fabric.sdk.transaction.ProposalBuilder;
import org.hyperledger.fabric.sdk.transaction.ProtoUtils;
import org.hyperledger.fabric.sdk.transaction.QueryCollectionsConfigBuilder;
import org.hyperledger.fabric.sdk.transaction.QueryInstalledChaincodesBuilder;
import org.hyperledger.fabric.sdk.transaction.QueryInstantiatedChaincodesBuilder;
import org.hyperledger.fabric.sdk.transaction.QueryPeerChannelsBuilder;
import org.hyperledger.fabric.sdk.transaction.TransactionBuilder;
import org.hyperledger.fabric.sdk.transaction.TransactionContext;
import org.hyperledger.fabric.sdk.transaction.UpgradeProposalBuilder;

import static java.lang.String.format;
import static org.hyperledger.fabric.sdk.Channel.PeerOptions.createPeerOptions;
import static org.hyperledger.fabric.sdk.Channel.TransactionOptions.createTransactionOptions;
import static org.hyperledger.fabric.sdk.User.userContextCheck;
import static org.hyperledger.fabric.sdk.helper.Utils.isNullOrEmpty;
import static org.hyperledger.fabric.sdk.helper.Utils.toHexString;
import static org.hyperledger.fabric.sdk.transaction.ProtoUtils.createSeekInfoEnvelope;
import static org.hyperledger.fabric.sdk.transaction.ProtoUtils.getSignatureHeaderAsByteString;

/**
 * The class representing a channel with which the client SDK interacts.
 * <p>
 */
public class P implements Serializable {
    private static final long serialVersionUID = -3266164166893832538L;
    private static final Config config = Config.getConfig();
    private static final Log logger = LogFactory.getLog(Channel.class);
    private static final boolean IS_DEBUG_LEVEL = logger.isDebugEnabled();
    private static final boolean IS_WARN_LEVEL = logger.isWarnEnabled();
    private static final boolean IS_TRACE_LEVEL = logger.isTraceEnabled();

    private static final DiagnosticFileDumper diagnosticFileDumper = IS_TRACE_LEVEL
            ? config.getDiagnosticFileDumper()
            : null;
    private static final String SYSTEM_CHANNEL_NAME = "";

    private static final long ORDERER_RETRY_WAIT_TIME = config.getOrdererRetryWaitTime();
    private static final long CHANNEL_CONFIG_WAIT_TIME = config.getChannelConfigWaitTime();
    private static final Random RANDOM = new Random();
    private static final String BLOCK_LISTENER_TAG = "BLOCK_LISTENER_HANDLE";
    // final Set<Peer> eventingPeers = Collections.synchronizedSet(new HashSet<>());
    private static final long DELTA_SWEEP = config.getTransactionListenerCleanUpTimeout();
    private static final String CHAINCODE_EVENTS_TAG = "CHAINCODE_EVENTS_HANDLE";
    final Collection<Orderer> orderers = Collections.synchronizedCollection(new LinkedList<>());
    private transient Map<String, Orderer> ordererEndpointMap = Collections.synchronizedMap(new HashMap<>());
    final Collection<EventHub> eventHubs = Collections.synchronizedCollection(new LinkedList<>());
    // Name of the channel is only meaningful to the client
    private final String name;
    private transient String toString;

    // The peers on this channel to which the client can connect
    private final Collection<Peer> peers = Collections.synchronizedSet(new HashSet<>());
    private final Map<Peer, PeerOptions> peerOptionsMap = Collections.synchronizedMap(new HashMap<>());
    private transient Map<String, Peer> peerEndpointMap = Collections.synchronizedMap(new HashMap<>());
    private Map<String, Collection<Peer>> peerMSPIDMap = new HashMap<>();
    private Map<String, Collection<Orderer>> ordererMSPIDMap = new HashMap<>();
    private final Map<PeerRole, Set<Peer>> peerRoleSetMap = Collections.synchronizedMap(new HashMap<>());
    private transient String chaincodeEventUpgradeListenerHandle;
    private transient String transactionListenerProcessorHandle;
    private final boolean systemChannel;
    private transient LinkedHashMap<String, ChaincodeEventListenerEntry> chainCodeListeners = new LinkedHashMap<>();
    transient HFClient client;
    private Set<String> discoveryEndpoints = Collections.synchronizedSet(new HashSet<>());
    /**
     * Runs processing events from event hubs.
     */

    transient Thread eventQueueThread = null;
    private transient volatile boolean initialized = false;
    private transient volatile boolean shutdown = false;
    private transient Block genesisBlock;
    private transient Map<String, MSP> msps = new HashMap<>();
    /**
     * A queue each eventing hub will write events to.
     */

    private transient ChannelEventQue channelEventQue = new ChannelEventQue();
    private transient LinkedHashMap<String, BL> blockListeners = new LinkedHashMap<>();
    private transient LinkedHashMap<String, LinkedList<TL>> txListeners = new LinkedHashMap<>();
    // Cleans up any transaction listeners that will probably never complete.
    private transient ScheduledFuture<?> sweeper = null;
    private transient ScheduledExecutorService sweeperExecutorService;
    private transient String blh = null;
    private transient ServiceDiscovery serviceDiscovery;
    private static final boolean asLocalhost = config.discoverAsLocalhost();

    public static class AnchorPeersConfigUpdateResult {
        private UpdateChannelConfiguration updateChannelConfiguration = null;
        private Collection<String> peersAdded = Collections.emptyList();
        private Collection<String> peersRemoved = Collections.emptyList();
        private Collection<String> currentPeers = Collections.emptyList();
        private Collection<String> updatedPeers = Collections.emptyList();

        /*
         * @param peer peer to use to the channel configuration from.
         * 
         * @param userContext The usercontext to use.
         * 
         * @param peersToAdd Peers to add as Host:Port peer1.org2.com:7022
         * 
         * @param peersToRemove Peers to remove as Host:Port peer1.org2.com:7022
         * 
         * @return The AnchorPeersConfigUpdateResult @see {@link
         * AnchorPeersConfigUpdateResult}
         * 
         * @throws Exception
         */
        public AnchorPeersConfigUpdateResult getConfigUpdateAnchorPeers(Peer peer, User userContext,
                Collection<String> peersToAdd, Collection<String> peersToRemove) throws Exception {

            User.userContextCheck(userContext);

            checkPeer(peer);

            checkChannelState();

            final boolean reportOnly = peersToAdd == null && peersToRemove == null;

            if (!reportOnly && ((peersToAdd == null || peersToAdd.isEmpty())
                    && (peersToRemove == null || peersToRemove.isEmpty()))) {
                throw new InvalidArgumentException("No anchor peers to add or remove!");
            }

            if (IS_TRACE_LEVEL) {

                StringBuilder sbp = new StringBuilder("null");
                String sep = "";
                if (peersToAdd != null) {
                    sbp = new StringBuilder("[");
                    for (String s : peersToAdd) {
                        sbp.append(sep).append("'").append(s).append("'");
                        sep = ", ";
                    }
                    sbp.append("]");

                }
                StringBuilder sbr = new StringBuilder("null");
                sep = "";
                if (peersToRemove != null) {
                    sbr = new StringBuilder("[");

                    for (String s : peersToRemove) {

                        sbr.append(sep).append("'").append(s).append("'");
                        sep = ", ";
                    }
                    sbr.append("]");

                }
                logger.trace(format(
                        "getConfigUpdateAnchorPeers channel %s, peer: %s, user: %s, peers to add: %s, peers to remove: %s",
                        name, peer.toString(), userContext.getMspId() + ":" + userContext.getName(),
                        sbp.toString(), sbr.toString()));
            }

            Set<String> peersToAddHS = new HashSet<>(16);
            if (null != peersToAdd) {
                for (String s : peersToAdd) {
                    String[] ep = parseEndpoint(s);
                    peersToAddHS.add(ep[0] + ":" + ep[1]);
                }
                // peersToAddHS.addAll(peersToAdd);
            }

            Set<String> peersToRemoveHS = new HashSet<>(16);
            if (null != peersToRemove && !peersToRemove.isEmpty()) {
                for (String s : peersToRemove) {

                    String[] ep = parseEndpoint(s);
                    peersToRemoveHS.add(ep[0] + ":" + ep[1]);
                }
                peersToRemoveHS.removeAll(peersToAddHS); // add overrides remove;
            }
            Set<String> peersRemoved = new HashSet<>(peersToAddHS.size());
            Set<String> peersAdded = new HashSet<>(peersToRemoveHS.size());

            Block configBlock = getConfigBlock(Collections.singletonList(peer));
            if (IS_TRACE_LEVEL) {
                logger.trace(format("getConfigUpdateAnchorPeers  configBlock: %s",
                        toHexString(configBlock.toByteArray())));
            }

            Envelope envelope = Envelope.parseFrom(configBlock.getData().getData(0));
            Payload payload = Payload.parseFrom(envelope.getPayload());
            Header header = payload.getHeader();

            ChannelHeader channelHeader = ChannelHeader.parseFrom(header.getChannelHeader());
            if (!Objects.equals(name, channelHeader.getChannelId())) {
                throw new InvalidArgumentException(format("Expected config block for channel: %s, but got: %s", name,
                        channelHeader.getChannelId()));
            }

            ConfigEnvelope configEnvelope = ConfigEnvelope.parseFrom(payload.getData());
            // ConfigGroup channelGroup = configEnvelope.getConfig().getChannelGroup();

            Configtx.Config config = configEnvelope.getConfig();
            Configtx.Config.Builder configBuilderUpdate = config.toBuilder();

            ConfigGroup.Builder channelGroupBuild = configBuilderUpdate.getChannelGroup().toBuilder();
            Map<String, ConfigGroup> groupsMap = channelGroupBuild.getGroupsMap();
            ConfigGroup.Builder application = groupsMap.get("Application").toBuilder();
            final String mspid = userContext.getMspId();
            ConfigGroup peerOrgConfigGroup = application.getGroupsMap().get(mspid);

            if (null == peerOrgConfigGroup) {
                StringBuilder sb = new StringBuilder(1000);
                String sep = "";

                for (String amspid : application.getGroupsMap().keySet()) {
                    sb.append(sep).append(amspid);
                    sep = ", ";

                }
                throw new InvalidArgumentException(
                        format("Expected to find organization matching user context's mspid: %s, but only found %s.",
                                mspid, sb.toString()));
            }
            ConfigGroup.Builder peerOrgConfigGroupBuilder = peerOrgConfigGroup.toBuilder();

            String modPolicy = peerOrgConfigGroup.getModPolicy() != null ? peerOrgConfigGroup.getModPolicy() : "Admins";

            Map<String, ConfigValue> valuesMap = peerOrgConfigGroupBuilder.getValuesMap();

            ConfigValue anchorPeersCV = valuesMap.get("AnchorPeers");

            final Set<String> currentAP = new HashSet<>(36); // The anchor peers that exist already.

            if (null != anchorPeersCV && anchorPeersCV.getValue() != null) {
                modPolicy = anchorPeersCV.getModPolicy() != null ? "Admins" : modPolicy;

                Configuration.AnchorPeers anchorPeers = Configuration.AnchorPeers.parseFrom(anchorPeersCV.getValue());
                List<Configuration.AnchorPeer> anchorPeersList = anchorPeers.getAnchorPeersList();
                if (anchorPeersList != null) {
                    for (Configuration.AnchorPeer anchorPeer : anchorPeersList) {
                        currentAP.add(anchorPeer.getHost().toLowerCase() + ":" + anchorPeer.getPort());
                    }
                }
            }

            if (IS_TRACE_LEVEL) {

                StringBuilder sbp = new StringBuilder("[");
                String sep = "";

                for (String s : currentAP) {
                    sbp.append(sep).append("'").append(s).append("'");
                    sep = ", ";
                }
                sbp.append("]");

                logger.trace(format("getConfigUpdateAnchorPeers channel %s,  current anchor peers: %s",
                        name, sbp.toString()));

            }

            if (reportOnly) {
                logger.trace("getConfigUpdateAnchorPeers reportOnly");

                AnchorPeersConfigUpdateResult ret = new AnchorPeersConfigUpdateResult();
                ret.currentPeers = currentAP;
                ret.peersAdded = Collections.emptyList();
                ret.peersRemoved = Collections.emptyList();
                ret.updatedPeers = Collections.emptyList();

                if (IS_TRACE_LEVEL) {
                    logger.trace(format("getConfigUpdateAnchorPeers returned: %s",
                            ret.toString()));
                }
                return ret;

            }

            Set<String> peersFinalHS = new HashSet<>(16);

            Configuration.AnchorPeers.Builder anchorPeers = Configuration.AnchorPeers.newBuilder();
            for (String s : currentAP) {

                if (peersToRemoveHS.contains(s)) {
                    peersRemoved.add(s);
                    continue;
                }

                if (!peersToAddHS.contains(s)) {
                    String[] split = s.split(":");
                    anchorPeers.addAnchorPeers(Configuration.AnchorPeer.newBuilder().setHost(split[0])
                            .setPort(Integer.parseInt(split[1])).build());
                    peersFinalHS.add(s);
                }
            }

            for (String s : peersToAddHS) {
                if (!currentAP.contains(s)) {
                    peersAdded.add(s);
                    String[] split = s.split(":");
                    anchorPeers.addAnchorPeers(Configuration.AnchorPeer.newBuilder().setHost(split[0])
                            .setPort(Integer.parseInt(split[1])).build());
                    peersFinalHS.add(s);
                }
            }

            if (peersRemoved.isEmpty() && peersAdded.isEmpty()) {
                logger.trace("getConfigUpdateAnchorPeers no Peers need adding or removing.");
                AnchorPeersConfigUpdateResult ret = new AnchorPeersConfigUpdateResult();
                ret.currentPeers = currentAP;
                ret.peersAdded = Collections.emptyList();
                ret.peersRemoved = Collections.emptyList();
                ret.updatedPeers = Collections.emptyList();
                if (IS_TRACE_LEVEL) {
                    logger.trace(format("getConfigUpdateAnchorPeers returned: %s",
                            ret.toString()));
                }
                return ret;
            }

            Map m = new HashMap(valuesMap);
            m.remove("AnchorPeers");

            m.put("AnchorPeers", ConfigValue.newBuilder().setValue(anchorPeers.build().toByteString())
                    .setModPolicy(modPolicy).build());

            ConfigGroup build = peerOrgConfigGroupBuilder.putAllValues(m).build();

            m.clear();
            m.putAll(application.getGroupsMap());
            m.put(mspid, build);
            // application.putAllValues(m);
            application.putAllGroups(m);
            ConfigGroup applicationBuilt = application.build();
            m.clear();
            m.putAll(channelGroupBuild.getGroupsMap());
            m.put("Application", applicationBuilt);
            channelGroupBuild.putAllGroups(m);

            configBuilderUpdate.setChannelGroup(channelGroupBuild.build());

            Configtx.ConfigUpdate.Builder updateBlockBuilder = Configtx.ConfigUpdate.newBuilder();

            Configtx.Config updated = configBuilderUpdate.build();

            if (IS_TRACE_LEVEL) {
                logger.trace(format("getConfigUpdateAnchorPeers  updated configBlock: %s",
                        toHexString(updated.toByteArray())));
            }

            ProtoUtils.computeUpdate(name, config, updated, updateBlockBuilder);

            AnchorPeersConfigUpdateResult ret = new AnchorPeersConfigUpdateResult();
            ret.currentPeers = currentAP;
            ret.peersAdded = peersAdded;
            ret.peersRemoved = peersRemoved;
            ret.updatedPeers = peersFinalHS;
            ret.updateChannelConfiguration = new UpdateChannelConfiguration(updateBlockBuilder.build().toByteArray());
            if (IS_TRACE_LEVEL) {
                logger.trace(format("getConfigUpdateAnchorPeers returned: %s",
                        ret.toString()));
            }

            return ret;
        }
    }
}