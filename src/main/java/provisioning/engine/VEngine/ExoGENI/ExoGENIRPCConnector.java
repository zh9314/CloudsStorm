package provisioning.engine.VEngine.ExoGENI;
import java.io.File;
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;

import orca.util.ssl.ContextualSSLProtocolSocketFactory;
import orca.util.ssl.MultiKeyManager;
import orca.util.ssl.MultiKeySSLContextFactory;

public class ExoGENIRPCConnector {
	
	private static final Logger logger = Logger.getLogger(ExoGENIRPCConnector.class);
	
	public String apiURL;
	public String userKeyPath, keyAlias, keyPass;
	private static final String CREATE_SLICE = "orca.createSlice";
	private static final String SLICE_STATUS = "orca.sliceStatus";
	private static final String DELETE_SLICE = "orca.deleteSlice";
	private static final String ERR_RET_FIELD = "err";
	private static final String RET_RET_FIELD = "ret";
	private static final String MSG_RET_FIELD = "msg";
	
	
	
	private static final int HTTPS_PORT = 443;
	private static MultiKeyManager mkm = null;
	private static ContextualSSLProtocolSocketFactory regSslFact = null;
	boolean sslIdentitySet = false;
	// alternative names set on the cert that is in use. Only valid when identity is set
	Collection<List<?>> altNames = null;
	
	
	public ExoGENIRPCConnector(String apiURL, String userKeyPath,
			String keyAlias, String keyPassword){
		this.apiURL = apiURL;
		this.userKeyPath = userKeyPath;
		this.keyAlias = keyAlias;
		this.keyPass = keyPassword;
	}
	
	
	static {
		mkm = new MultiKeyManager();
		regSslFact = new ContextualSSLProtocolSocketFactory();
		
		// register the protocol (Note: All xmlrpc clients must use XmlRpcCommonsTransportFactory
		// for this to work). See ContextualSSLProtocolSocketFactory.
		
		Protocol reghhttps = new Protocol("https", (ProtocolSocketFactory)regSslFact, HTTPS_PORT); 
		Protocol.registerProtocol("https", reghhttps);
	}
	
	TrustManager[] trustAllCerts = new TrustManager[] {
			new X509TrustManager() {
				public X509Certificate[] getAcceptedIssuers() {
					// return 0 size array, not null, per spec
					return new X509Certificate[0];
				}

				public void checkClientTrusted(X509Certificate[] certs, String authType) {
					// Trust always
				}

				public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateExpiredException, CertificateNotYetValidException {
					// Trust always, unless expired
					// FIXME: should check the cert of controller we're talking to
					for(X509Certificate c: certs) {
						c.checkValidity();	
					}
				}
			}
	};
	
	private KeyStore loadJKSData(FileInputStream jksIS, String keyAlias, String keyPassword)
			throws Exception {

		KeyStore ks = KeyStore.getInstance("jks");
		ks.load(jksIS, keyPassword.toCharArray());

		return ks;
	}
	
	
	private File loadUserFile(String pathStr) {
		File f;

		if (pathStr.startsWith("~/")) {
			pathStr = pathStr.replaceAll("~/", "/");
			f = new File(System.getProperty("user.home"), pathStr);
		}
		else {
			f = new File(pathStr);
		}

		return f;
	}

	/**
	 * Set the identity for the communications to the XMLRPC controller. Eventually
	 * we may talk to several controller with different identities. For now only
	 * one is configured.
	 */
	protected void setSSLIdentity() throws Exception {
		
		if (sslIdentitySet)
			return;

		try {
			URL ctrlrUrl = new URL(apiURL);

			KeyStore ks = null;
			File keyStorePath = loadUserFile(userKeyPath);

			if (keyStorePath.exists()) {
				FileInputStream jksIS = new FileInputStream(keyStorePath);
				ks = loadJKSData(jksIS, keyAlias, keyPass);
				
				jksIS.close();
			}

			if (ks == null)
				throw new Exception("Was unable to find either: " + keyStorePath.getCanonicalPath());

			// check that the spelling of key alias is proper
			Enumeration<String> as = ks.aliases();
			while (as.hasMoreElements()) {
				String a = as.nextElement();
				if (keyAlias.toLowerCase().equals(a.toLowerCase())) {
					keyAlias = a;
					break;
				}
			}

			// alias has to exist and have a key and cert present
			if (!ks.containsAlias(keyAlias)) {
				throw new Exception("Alias " + keyAlias + " does not exist in keystore " + keyStorePath + ".");
			}

			if (ks.getKey(keyAlias, keyPass.toCharArray()) == null)
				throw new Exception("Key with alias " + keyAlias + " does not exist in keystore " + keyStorePath + ".");

			if (ks.getCertificate(keyAlias) == null) {
				throw new Exception("Certificate with alias " + keyAlias + " does not exist in keystore " + keyStorePath + ".");
			}

			if (ks.getCertificate(keyAlias).getType().equals("X.509")) {
				X509Certificate x509Cert = (X509Certificate)ks.getCertificate(keyAlias);
				altNames = x509Cert.getSubjectAlternativeNames();
				try {
					x509Cert.checkValidity();
				} catch (Exception e) {
					throw new Exception("Certificate with alias " + keyAlias + " is not yet valid or has expired.");
				}
			}

			// add the identity into it
			mkm.addPrivateKey(keyAlias, 
					(PrivateKey)ks.getKey(keyAlias, keyPass.toCharArray()), 
					ks.getCertificateChain(keyAlias));

			// before we do SSL to this controller, set our identity
			mkm.setCurrentGuid(keyAlias);

			// add this multikey context factory for the controller host/port
			int port = ctrlrUrl.getPort();
			if (port <= 0)
				port = HTTPS_PORT;
			regSslFact.addHostContextFactory(new MultiKeySSLContextFactory(mkm, trustAllCerts), 
					ctrlrUrl.getHost(), port);

			sslIdentitySet = true;
			
		} catch (Exception e) {
			e.printStackTrace();

			throw new Exception("Unable to load user private key and certificate from the keystore: " + e);
		}
	}
	
	
	
	/** submit an ndl request to create a slice, using explicitly specified users array
	 * 
	 * @param sliceId
	 * @param resReq
	 * @param users
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public String createSlice(String sliceId, String resReq, List<Map<String, ?>> users) throws Exception {
		assert(sliceId != null);
		assert(resReq != null);

		String result = null;
		setSSLIdentity();

		Map<String, Object> rr = null;
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(apiURL));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);

			// set this transport factory for host-specific SSLContexts to work
			XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
			client.setTransportFactory(f);

			// create sliver
			rr = (Map<String, Object>)client.execute(CREATE_SLICE, new Object[]{ sliceId, new Object[]{}, resReq, users});
		} catch (MalformedURLException e) {
			logger.error("Please check the SM URL " + apiURL);
			throw new Exception("Please check the SM URL " + apiURL);
		} catch (XmlRpcException e) {
			logger.error("Unable to contact SM " + apiURL + " due to " + e);
			throw new Exception("Unable to contact SM " + apiURL + " due to " + e);
		} catch (Exception e) {
			logger.error("Unable to submit slice to SM:  " + apiURL + " due to " + e);
			return "Unable to submit slice to SM:  " + apiURL + " due to " + e;
		}

		if (rr == null){
			logger.error("Unable to contact SM " + apiURL);
			throw new Exception("Unable to contact SM " + apiURL);
		}

		if ((Boolean)rr.get(ERR_RET_FIELD)){
			logger.error("Unable to create slice: " + (String)rr.get(MSG_RET_FIELD));
			throw new Exception("Unable to create slice: " + (String)rr.get(MSG_RET_FIELD));
		}

		result = (String)rr.get(RET_RET_FIELD);
		return result;
	}
	
	
	/**
	 * submit an ndl request to create a slice using this user's credentials
	 * @param sliceId
	 * @param resReq
	 * @param userKey
	 * @return
	 */
	public String createSlice(String sliceId, String resReq, String userKey) throws Exception {
		setSSLIdentity();

		// create an array
		List<Map<String, ?>> users = new ArrayList<Map<String, ?>>();
		
		if (userKey == null) {
			logger.error("Unable to load public ssh key to access the slice of '"+sliceId+"'!");
			throw new Exception("Unable to load public ssh key to access the slice of '"+sliceId+"'!");
		}

		Map<String, Object> userEntry = new HashMap<String, Object>();

		userEntry.put("login", "root");
		List<String> keys = new ArrayList<String>();
		keys.add(userKey);
		userEntry.put("keys", keys);
		users.add(userEntry);

		// submit the request
		return createSlice(sliceId, resReq, users);
	}
	
	
	@SuppressWarnings("unchecked")
	public boolean deleteSlice(String sliceId)  throws Exception {
		boolean res = false;
		setSSLIdentity();

		Map<String, Object> rr = null;
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(apiURL));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);

			// set this transport factory for host-specific SSLContexts to work
			XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
			client.setTransportFactory(f);

			// delete sliver
			rr = (Map<String, Object>)client.execute(DELETE_SLICE, new Object[]{ sliceId, new Object[]{}});
		} catch (MalformedURLException e) {
			throw new Exception("Please check the SM URL " + apiURL);
		} catch (XmlRpcException e) {
			throw new Exception("Unable to contact SM " + apiURL + " due to " + e);
		} catch (Exception e) {
			throw new Exception("Unable to contact SM " + apiURL);
		}

		if (rr == null)
                        throw new Exception("Unable to contact SM " + apiURL);

		if ((Boolean)rr.get(ERR_RET_FIELD))
			throw new Exception("Unable to delete slice: " + (String)rr.get(MSG_RET_FIELD));
		else
			res = (Boolean)rr.get(RET_RET_FIELD);

		return res;
	}

	
	@SuppressWarnings("unchecked")
	public String sliceStatus(String sliceId)  throws Exception {
		assert(sliceId != null);

		String result = null;
		setSSLIdentity();

		Map<String, Object> rr = null;
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(apiURL));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);

			// set this transport factory for host-specific SSLContexts to work
			XmlRpcCommonsTransportFactory f = new XmlRpcCommonsTransportFactory(client);
			client.setTransportFactory(f);

			// sliver status
			rr = (Map<String, Object>)client.execute(SLICE_STATUS, new Object[]{ sliceId, new Object[]{}});

		} catch (MalformedURLException e) {
			throw new Exception("Please check the SM URL " + apiURL);
		} catch (XmlRpcException e) {
			throw new Exception("Unable to contact SM " + apiURL + " due to " + e);
		} catch (Exception e) {
			throw new Exception("Unable to contact SM " + apiURL);
		}

		if (rr == null)
			throw new Exception("Unable to contact SM " + apiURL);

		if ((Boolean)rr.get(ERR_RET_FIELD))
			throw new Exception("Unable to get slice status: " + rr.get(MSG_RET_FIELD));

		result = (String)rr.get(RET_RET_FIELD);

		return result;
	}

	

}
