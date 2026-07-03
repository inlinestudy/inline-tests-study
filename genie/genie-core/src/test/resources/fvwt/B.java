package fvwt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.hyperledger.fabric.sdk.Channel.PeerOptions;
import org.hyperledger.fabric.sdk.Peer.PeerRole;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.NetworkConfigurationException;
import org.hyperledger.fabric.sdk.identity.X509Enrollment;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import static java.lang.String.format;
import static org.hyperledger.fabric.sdk.helper.Utils.isNullOrEmpty;

class B {
    private void getTLSCerts(String nodeName, JsonObject jsonOrderer, Properties props) {
        JsonObject jsonTlsCaCerts = getJsonObject(jsonOrderer, "tlsCACerts");
        if (jsonTlsCaCerts != null) {
            String pemFilename = getJsonValueAsString(jsonTlsCaCerts.get("path"));
            if (pemFilename != null) {
                // let the sdk handle non existing errors could be they don't exist during parsing but are there later.
                props.put("pemFile", pemFilename);
            }

            byte[] pemBytes = getJsonValueAsList(jsonTlsCaCerts.get("pem"), NetworkConfig::getJsonValueAsString).stream()
                    .collect(Collectors.joining("\n"))
                    .getBytes();
            props.put("pemBytes", pemBytes);

            JsonObject jsonTlsClientCerts = getJsonObject(jsonTlsCaCerts, "client");

            if (jsonTlsClientCerts != null) {

                String keyfile = getJsonValueAsString(jsonTlsClientCerts.get("keyfile"));
                String certfile = getJsonValueAsString(jsonTlsClientCerts.get("certfile"));

                if (keyfile != null) {
                    props.put("tlsClientKeyFile", keyfile);
                }

                if (certfile != null) {
                    props.put("tlsClientCertFile", certfile);
                }

                String keyBytes = getJsonValueAsString(jsonTlsClientCerts.get("keyPem"));
                String certBytes = getJsonValueAsString(jsonTlsClientCerts.get("certPem"));

                if (keyBytes != null) {
                    props.put("tlsClientKeyBytes", keyBytes.getBytes());
                }

                if (certBytes != null) {
                    props.put("tlsClientCertBytes", certBytes.getBytes());
                }

            }
        }
    }

}
