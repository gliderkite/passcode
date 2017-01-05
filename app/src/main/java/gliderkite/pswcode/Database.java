package gliderkite.pswcode;

import android.content.Context;
import android.util.Base64;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


/**
 * Credentials database manager.
 */
final class Database
{

    /**
     * Saves the list of credentials into an encrypted database.
     * @param context Application context.
     * @param output Output file name.
     * @param credentials List of credentials.
     * @param password Master password.
     * @return Returns the operation's outcome.
     */
    static boolean write(Context context, String output, List<Credential> credentials, String password)
    {
        try
        {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root element
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("db");
            doc.appendChild(rootElement);

            // iterate over each credential
            for (Credential credential : credentials)
            {
                // create xml node for the current credential
                Element element = doc.createElement("credential");
                rootElement.appendChild(element);
                // set service
                Attr attr = doc.createAttribute("service");
                attr.setValue(credential.Service);
                element.setAttributeNode(attr);
                // set username
                attr = doc.createAttribute("username");
                attr.setValue(credential.Username);
                element.setAttributeNode(attr);
                // set password
                attr = doc.createAttribute("password");
                attr.setValue(credential.Password);
                element.setAttributeNode(attr);
            }

            // get the string representing the xml content
            String xml = XMLtoString(doc);

            if (xml == null)
                return false;

            // encrypt the XML string
            byte[] encrypted = encrypt(password, xml);

            if (encrypted == null)
                return false;

            // write database
            FileOutputStream outputStream = context.openFileOutput(output, Context.MODE_PRIVATE);
            outputStream.write(encrypted);
            outputStream.close();

            return true;
        }
        catch (Exception ex)
        {
            return false;
        }
    }


    /**
     * Reads the given encrypted database.
     * @param context Application context.
     * @param input Input file name.
     * @param password Master password.
     * @return Returns the list of credentials, if an error occurs the list will be empty.
     */
    static List<Credential> read(Context context, String input, String password)
    {
        List<Credential> credentials = new ArrayList<>();

        try
        {
            // read file content
            FileInputStream inputStream = context.openFileInput(input);
            File file = new File(context.getFilesDir(), input);
            byte[] data = new byte[(int) file.length()];
            int n = inputStream.read(data);

            if (n == data.length)
            {
                String xml = Database.decrypt(password, data);

                if (xml != null)
                {
                    // parse xml content
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    InputSource is = new InputSource(new StringReader(xml));
                    Document doc = builder.parse(is);

                    NodeList nodeList = doc.getDocumentElement().getChildNodes();

                    for (int i = 0; i < nodeList.getLength(); i++)
                    {
                        Node currentNode = nodeList.item(i);

                        if (currentNode.getNodeType() == Node.ELEMENT_NODE)
                        {
                            NamedNodeMap attributes = currentNode.getAttributes();
                            String service = attributes.getNamedItem("service").getNodeValue();
                            String username = attributes.getNamedItem("username").getNodeValue();
                            String psw = attributes.getNamedItem("password").getNodeValue();

                            // add credential
                            credentials.add(new Credential(service, username, psw));
                        }
                    }
                }
            }

            inputStream.close();
        }
        catch (Exception ex)
        {
            credentials.clear();
        }

        return credentials;
    }


    /**
     * Returns a filename as an encrypted and encoded into a base64 String.
     * @param username Original username.
     * @param password Encryption password.
     * @return Returns a file name, otherwise null in case of error.
     */
    static String getFilename(String username, String password)
    {
        try
        {
            byte[] user = Database.encrypt(username, password);
            byte[] encoded = Base64.encode(user, Base64.DEFAULT);
            return new String(encoded, "UTF-8").replaceAll("[^a-zA-Z0-9.-]", "_");
        }
        catch (Exception ex)
        {
            return null;
        }
    }


    /**
     * Deletes the database.
     * @param context Application context.
     * @param username Username.
     * @param password Master password.
     * @return Returns the operation's outcome.
     */
    static boolean delete(Context context, String username, String password)
    {
        try
        {
            String filename = getFilename(username, password);
            File file = new File(context.getFilesDir(), filename);
            return file.delete();
        }
        catch (Exception ex)
        {
            return false;
        }
    }


    /**
     * Gets the String representing the XML document content.
     * @param doc XML document.
     * @return Returns the XML String, otherwise null in case of error.
     */
    private static String XMLtoString(Document doc)
    {
        try
        {
            StringWriter sw = new StringWriter();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(new DOMSource(doc), new StreamResult(sw));

            return sw.toString();
        }
        catch (Exception ex)
        {
            return null;
        }
    }



    /**
     * The salt.
     */
    private final static byte[] salt =  {
            (byte)232, (byte)207, (byte)153, (byte)139, 73, 67, 22, (byte)203, 39, 81, 2, (byte)120,
            2, 41, (byte)135, 53, 22, 72, (byte)171, 104, (byte)205, (byte)194, 87, 0, (byte)221, 60,
            (byte)238, (byte)202, 67, 38, (byte)229, (byte)160,
    };

    /**
     * The initialization vector.
     */
    private final static byte[] initVector = {
            -63, -86, -91, -65, -83, 58, 6, 50, 26, -64, -108, -50, 70, 2, 56, 188
    };

    /**
     * Number of iterations.
     */
    private final static int iterations = 0x642;


    /**
     * Encrypts the given string with the given password.
     * @param password Password used by the encryption method.
     * @param value Value to encrypt.
     * @return Returns a byte array containing the value encrypted, otherwise null in case of error.
     */
    private static byte[] encrypt(String password, String value)
    {
        try
        {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256); // AES-256
            SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] key = f.generateSecret(spec).getEncoded();

            IvParameterSpec iv = new IvParameterSpec(initVector);
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

            return cipher.doFinal(value.getBytes());
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        return null;
    }


    /**
     * Decrypts the given string with the given password.
     * @param password Password used by the decryption method.
     * @param value Value to decrypt.
     * @return Returns a string containing the value decrypted, otherwise null in case of error.
     */
    private static String decrypt(String password, byte[] value)
    {
        try
        {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256); // AES-256
            SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] key = f.generateSecret(spec).getEncoded();

            IvParameterSpec iv = new IvParameterSpec(initVector);
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            byte[] original = cipher.doFinal(value);
            return new String(original);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        return null;
    }

}
