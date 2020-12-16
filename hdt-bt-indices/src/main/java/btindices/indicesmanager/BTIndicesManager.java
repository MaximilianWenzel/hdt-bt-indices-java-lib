package btindices.indicesmanager;

import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.options.ControlInformation;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public abstract class BTIndicesManager {

    /**
     * Represents an array of all RDF class URIs which occur in the index.
     */
    public URI[] typeURIs;

    /**
     * Variable which is needed in order to load and save BitmapTriples.
     */
    ControlInformation ci;

    /**
     * Represents the path to the directory with all the indices.
     */
    String directoryPath;

    HDT hdt;
    Dictionary dic;

    /**
     * Parses the file of the BTIndex which contains all the class URIs of the
     * specific HDT file.
     *
     * @param path
     */
    public static URI[] readClassURIs(String path) {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String result = reader.readLine();
            String[] classURIsStr = result.split(";");
            URI[] typeURIs = new URI[classURIsStr.length];

            for (int i = 0; i < classURIsStr.length; i++) {
                typeURIs[i] = new URI(classURIsStr[i]);
            }

            return typeURIs;


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return null;
    }
}
