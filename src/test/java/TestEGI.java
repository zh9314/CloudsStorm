
import java.io.File;
import provisioning.engine.VEngine.EGI.EGIAgent;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author S. Koulouzis
 */
public class TestEGI {

    public static void main(String[] args) {

        EGIAgent egi = new EGIAgent("/tmp/x509up_u1000", System.getProperty("user.home") + File.separator + ".globus" + File.separator + "certificates");
        egi.initClient("https://carach5.ics.muni.cz:11443");

    }
}
