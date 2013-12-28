package github.madmarty.madsonic.util;


import github.madmarty.madsonic.domain.SrvSettings;
import github.madmarty.madsonic.domain.SrvSettings.ServerEntry;

import java.io.File;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException; 

import android.util.Log;

public class TemplateReader{

   	private static final String TAG = TemplateReader.class.getSimpleName();
   	
    	
	public static <serverEntries> SrvSettings getTemplates(){
  		
    try {
    		SrvSettings serverSettings = new SrvSettings();
    	
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse (new File(FileUtil.getMadsonicDirectory(), "madsonic.xml"));
            doc.getDocumentElement ().normalize ();
            Log.v(TAG, "Root element of the doc is " + doc.getDocumentElement().getNodeName());

            NodeList listOfServer = doc.getElementsByTagName("server");
            int totalServer = listOfServer.getLength();
            Log.v(TAG, "Found Server : " + totalServer);
            
            for(int s=0; s<listOfServer.getLength() ; s++){

            	ServerEntry se = new ServerEntry();

            	Node firstSettingsNode = listOfServer.item(s);
                if(firstSettingsNode.getNodeType() == Node.ELEMENT_NODE){
				Element ServerElement = (Element)firstSettingsNode;
				NodeList ServerNameList = ServerElement.getElementsByTagName("name");
				Element  ServerNameElement = (Element)ServerNameList.item(0);
				NodeList textFNList = ServerNameElement.getChildNodes();
				se.setServername(((Node)textFNList.item(0)).getNodeValue().trim());
				NodeList urlList = ServerElement.getElementsByTagName("url");
				Element urlElement = (Element)urlList.item(0);
				NodeList textLNList = urlElement.getChildNodes();
            	se.setServerURL(((Node)textLNList.item(0)).getNodeValue().trim());
				NodeList usernameList = ServerElement.getElementsByTagName("username");
				Element usernameElement = (Element)usernameList.item(0);
				NodeList textArgList = usernameElement.getChildNodes();
				se.setUsername(((Node)textArgList.item(0)).getNodeValue().trim());
				NodeList passwordList = ServerElement.getElementsByTagName("password");
				Element passwordElement = (Element)passwordList.item(0);
				NodeList textPassList = passwordElement.getChildNodes();
				se.setPassword(((Node)textPassList.item(0)).getNodeValue().trim());

				serverSettings.addChild(se);				
                }
            }
            return serverSettings;

        } catch (SAXParseException err) {
            Log.i(TAG, "** Parsing error" + ", line " + err.getLineNumber () + ", uri " + err.getSystemId ());
            Log.i(TAG, " " + err.getMessage ());

        } catch (SAXException e) {
        Exception x = e.getException ();
        ((x == null) ? e : x).printStackTrace ();

        } catch (Throwable t) {
            Log.e(TAG, " " + t.getMessage());        	
        //t.printStackTrace ();
        }
    	return null;
    }


}
