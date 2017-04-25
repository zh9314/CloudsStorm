

import java.io.IOException;


import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;




public class testSSLKeyGEN {

	public static void main(String[] args) throws IOException, JSchException {
		
		/*KeyPairGenerator keyPairGen= null;  
        try {  
            keyPairGen= KeyPairGenerator.getInstance("RSA");  
        } catch (NoSuchAlgorithmException e) {  
            e.printStackTrace();  
        }  
        keyPairGen.initialize(1024, new SecureRandom());  
        KeyPair keyPair= keyPairGen.generateKeyPair();  
        RSAPrivateKey privateKey= (RSAPrivateKey) keyPair.getPrivate();  
        RSAPublicKey publicKey= (RSAPublicKey) keyPair.getPublic();  
        
        byte[] encodedPublicKey = publicKey.getEncoded();
        String b64PublicKey = new String(Base64.encodeBase64(encodedPublicKey));
        System.out.println(b64PublicKey);*/
		
		/*JDKKeyPairGenerator.RSA keyPairGen = new JDKKeyPairGenerator.RSA();
		keyPairGen.initialize(1024, new SecureRandom()); 
		KeyPair keyPair = keyPairGen.generateKeyPair(); 
		StringWriter stringWriter = new StringWriter(); 
		PEMWriter pemFormatWriter = new PEMWriter(stringWriter); 
		pemFormatWriter.writeObject(keyPair.getPublic()); 
		pemFormatWriter.close();
		String test = stringWriter.toString();
		System.out.println(test);*/
		
		JSch jsch=new JSch();
		KeyPair kpair=KeyPair.genKeyPair(jsch, KeyPair.RSA);
        kpair.writePrivateKey("test");
        kpair.writePublicKey("test.pub", "123");
        System.out.println("Finger print: "+kpair.getFingerPrint());
        kpair.dispose();
        
	}

}
