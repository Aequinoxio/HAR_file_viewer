/**
 * Classe che mantiene la struttura del file HAR
 */

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import javax.json.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Set;

public class HARParser {
    private final File HARFile;
    private JsonObject HARStructure;

    private final UIUpdateCallBack callback;

    ArrayList<HARImageItem> harImageArray = new ArrayList<>();
    Set<String> imageJsonParameters = Set.of("text", "encoding", "size");

    private HARParser(File HARFile,UIUpdateCallBack callback) {
        this.HARFile = HARFile;
        this.callback=callback;
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull
    HARParser createHARParser(File HARFile, UIUpdateCallBack callback) {
        return new HARParser(HARFile, callback);
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull
    HARParser createHARParser(String HARFileName,UIUpdateCallBack callback) {
        return new HARParser(new File(HARFileName),callback);
    }

    public void parse() throws IOException {
        // HARStructure = Json.createParser(new FileReader(HARFile, StandardCharsets.UTF_8)).getObject();
        HARStructure = Json.createReader(new FileReader(HARFile, StandardCharsets.UTF_8)).readObject();

        JsonArray entries = HARStructure.getJsonObject("log").getJsonArray("entries");

        String imageEncoded;
        String imageEncodingType;
        String mimeType;
        long size;
        JsonObject entryObject;
        JsonObject responseContent;

        int entriesSize= entries.size();
        int imgTrovate=0;

        for (int i = 0; i < entriesSize; i++) {
            entryObject = entries.getJsonObject(i);
            mimeType = entryObject.getJsonObject("response").getJsonObject("content").getString("mimeType").trim();
            responseContent = entryObject.getJsonObject("response").getJsonObject("content");


            if ("image/jpeg".equalsIgnoreCase(mimeType)) {
                if (responseContent.keySet().containsAll(imageJsonParameters)) {
                    imageEncoded = responseContent.getString("text");
                    imageEncodingType = responseContent.getString("encoding");
                    size = responseContent.getJsonNumber("size").longValue();
                    harImageArray.add(new HARImageItem(String.valueOf(imgTrovate), imageEncodingType.trim(), imageEncoded, mimeType, size));
                    imgTrovate++;
                }
            }

            // Update callback
            callback.update("Parsing: "+ (i+1) + " oggetto di "+ entriesSize +" totali. Immagini trovate: "+imgTrovate);
            /////////////////

        }
    }

    public JsonArray getEntries() {
        return HARStructure.getJsonArray("entries");
    }

    public ArrayList<HARImageItem> getHarImageArray() {
        return harImageArray;
    }

    static class HARImageItem {
        private final String ID;
        private final String imageEncodingType;
        private final String imageEncoded;
        private final String mimeType;
        private final long size;

        private long width = 0;
        private long height = 0;
        private BufferedImage decodedImage;

        public HARImageItem(String id, String imageEncodingType, String imageEncoded, String mimeType, long size) throws IOException {
            ID = id;
            this.imageEncodingType = imageEncodingType;
            this.imageEncoded = imageEncoded;
            this.mimeType = mimeType;
            this.size = size;

            Base64.Decoder base64Decoder = Base64.getDecoder();
            if ("image/jpeg".equalsIgnoreCase(mimeType) && "base64".equalsIgnoreCase(imageEncodingType)) {
                byte[] imageBytes = base64Decoder.decode(imageEncoded);
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageBytes);
                decodedImage = ImageIO.read(byteArrayInputStream);
                width = decodedImage.getWidth();
                height = decodedImage.getHeight();
            }
        }

        public String getImageEncodingType() {
            return imageEncodingType;
        }

        public String getImageEncoded() {
            return imageEncoded;
        }

        public String getID() {
            return ID;
        }

        public String getMimeType() {
            return mimeType;
        }

        public long getSize() {
            return size;
        }

        public BufferedImage getDecodedImage() {
            return decodedImage;
        }

        public long getWidth() {
            return width;
        }

        public long getHeight() {
            return height;
        }
    }
}