package test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.protos.discovery.Protocol;
import org.hyperledger.fabric.protos.gossip.Message;
import org.hyperledger.fabric.protos.msp.Identities;
import org.hyperledger.fabric.protos.msp.MspConfig;
import org.hyperledger.fabric.sdk.Channel.ServiceDiscoveryChaincodeCalls;
import org.hyperledger.fabric.sdk.ServiceDiscovery.SDLayout.SDGroup;
import org.hyperledger.fabric.sdk.exception.InvalidProtocolBufferRuntimeException;
import org.hyperledger.fabric.sdk.exception.ServiceDiscoveryException;
import org.hyperledger.fabric.sdk.helper.Config;
import org.hyperledger.fabric.sdk.helper.DiagnosticFileDumper;
import org.hyperledger.fabric.sdk.transaction.TransactionContext;

import static java.lang.String.format;
import static org.hyperledger.fabric.sdk.helper.Utils.toHexString;

public class N {
    private static final Log logger = LogFactory.getLog(ServiceDiscovery.class);
    private static final boolean DEBUG = logger.isDebugEnabled();
    private static final Config config = Config.getConfig();
    private static final boolean IS_TRACE_LEVEL = logger.isTraceEnabled();
    private static final DiagnosticFileDumper diagnosticFileDumper = IS_TRACE_LEVEL
            ? config.getDiagnosticFileDumper()
            : null;
    private static final int SERVICE_DISCOVERY_WAITTIME = config.getServiceDiscoveryWaitTime();
    private static final Random random = new Random();
    private final Collection<Peer> serviceDiscoveryPeers;
    private final Channel channel;
    private final TransactionContext transactionContext;
    private final String channelName;
    private volatile Map<String, SDChaindcode> chaindcodeMap = new HashMap<>();
    private static final boolean asLocalhost = config.discoverAsLocalhost();

    public static class SDEndorser {

        private List<Message.Chaincode> chaincodesList;
        // private final Protocol.Peer proto;
        private String endPoint = null;
        private String name = null;
        private String mspid;
        private long ledgerHeight = -1L;
        private final Collection<byte[]> tlsCerts;
        private final Collection<byte[]> tlsIntermediateCerts;
        private final boolean asLocalhost;
        private final boolean tls;

        private String parseEndpoint(Protocol.Peer peerRet) throws InvalidProtocolBufferRuntimeException {

            if (null == endPoint) {
                try {
                    Message.Envelope membershipInfo = peerRet.getMembershipInfo();
                    final ByteString membershipInfoPayloadBytes = membershipInfo.getPayload();
                    final Message.GossipMessage gossipMessageMemberInfo = Message.GossipMessage
                            .parseFrom(membershipInfoPayloadBytes);

                    if (Message.GossipMessage.ContentCase.ALIVE_MSG.getNumber() != gossipMessageMemberInfo
                            .getContentCase().getNumber()) {
                        throw new RuntimeException(format("Error %s", "bad"));
                    }
                    Message.AliveMessage aliveMsg = gossipMessageMemberInfo.getAliveMsg();
                    name = aliveMsg.getMembership().getEndpoint();
                    if (name != null) {
                        if (asLocalhost) {
                            endPoint = "localhost" + name.substring(name.lastIndexOf(':'));
                        } else {
                            endPoint = name.toLowerCase().trim();
                        }
                    }
                } catch (InvalidProtocolBufferException e) {
                    throw new InvalidProtocolBufferRuntimeException(e);
                }

            }
            return endPoint;
        }
    }
}