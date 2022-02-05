package app.services;

import com.pixelmed.dicom.AttributeList;
import com.pixelmed.display.ConsumerFormatImageMaker;
import com.pixelmed.display.SourceImage;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.apache.commons.io.FileUtils;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;

public class DicomService {

    private final boolean[] LFSR_SEED = {
            false, true, true, false, true,
            false, false, false, false, true,
            false, true, false, false, false,
            true, false, false, false, false
    };
    private int generatedLfsrNumber1 = 0;
    private int generatedLfsrNumber2 = 0;
    private int generatedLfsrNumber3 = 0;

    private String encTempDir = "";
    private String decTempDir = "";

    public DicomService(String encTempDir, String decTempDir){
        LFSR lfsr1 = new LFSR(this.LFSR_SEED, 4, 8, 16);
        this.generatedLfsrNumber1 = lfsr1.generate(8);

        LFSR lfsr2 = new LFSR(this.LFSR_SEED, 3, 6, 12);
        this.generatedLfsrNumber2 = lfsr2.generate(8);

        LFSR lfsr3 = new LFSR(this.LFSR_SEED, 2, 4, 8);
        this.generatedLfsrNumber3 = lfsr3.generate(8);

        this.encTempDir = encTempDir;
        this.decTempDir = decTempDir;

        File encDir = new File(encTempDir);
        if (!encDir.exists()) {
            encDir.mkdir();
        }

        File decDir = new File(decTempDir);
        if (!decDir.exists()) {
            decDir.mkdir();
        }
    }

    /**
     * Combines all generated LFSR number and returns the result
     */
    private int getCombinedLfsrNumber(){
        return (
                this.generatedLfsrNumber1 ^
                        this.generatedLfsrNumber2 ^
                                this.generatedLfsrNumber3
        );
    }

    /**
     * Encrypts a plain DICOM file and returns a reference to the encrypted file.
     *
     * @param   originalFile   The original DICOM file to be encrypted.
     */
    public File encrypt(File originalFile){
        File outputFile = new File( this.encTempDir + "\\encrypted.dcm");

        try {
            FileInputStream fis = new FileInputStream(originalFile);
            FileOutputStream fos = new FileOutputStream(outputFile);

            while(fis.available() != 0){
                int data = fis.read() ^ getCombinedLfsrNumber();
                fos.write(data);
            }

            fis.close();
            fos.flush();
            fos.close();

        }catch(Exception error){

        }

        return outputFile;
    }

    /**
     * Decrypts an encrypted DICOM file and returns a reference to the decrypted file.
     *
     * @param   encryptedFile   The encrypted DICOM file to be decrypted.
     */
    public File decrypt(File encryptedFile){
        if(!encryptedFile.exists()){
            (new Alert(Alert.AlertType.NONE, "DICOM image not found", ButtonType.CLOSE)).showAndWait();
            return null;
        }

        File outputFile = new File( this.decTempDir + "\\decrypted.dcm");

        try {
            FileInputStream fis = new FileInputStream(encryptedFile);
            FileOutputStream fos = new FileOutputStream(outputFile);

            while(fis.available() != 0){
                int data = fis.read() ^ getCombinedLfsrNumber();
                fos.write(data);
            }

            fis.close();
            fos.close();

        }catch(Exception error){

        }

        return outputFile;
    }

    /**
     * Retrieves a file from the specified URl and returns a reference to the retrieved file.
     *
     * @param   url   The URL of a remote file on a server
     */
    public File getFileFromURL(URL url){
        File outputFile = new File( decTempDir + "\\source.dcm");

        try {
            FileUtils.copyURLToFile(url, outputFile);
        }catch(Exception error){

        }

        return outputFile;
    }

    /**
     * Converts a DICOM file to a BufferedImage and returns a reference to the BufferedImage object.
     *
     * @param   dicomFile   The DICOM file to be converted
     */
    public BufferedImage convertDcmToImage(File dicomFile){
        BufferedImage bufferedImage = null;

        try {
            AttributeList list = new AttributeList();
            list.read(dicomFile.getAbsolutePath());
            SourceImage img = new com.pixelmed.display.SourceImage(list);

            bufferedImage = ConsumerFormatImageMaker.makeEightBitFrame(img, img.getNumberOfFrames() - 1);
        }catch(Exception error){

        }

        return bufferedImage;
    }

    /**
     * Converts a DICOM file to PNG image(s) and returns a reference to the file(s).
     *
     * @param   dicomFile   The DICOM file to be converted
     */
    public File[] convertDcmToImageFile(File dicomFile){
        // Delete previously converted file(s)
        deleteSpecificFiles(".png", decTempDir);

        try {
            File output = new File( decTempDir + "\\converted.png");
            ConsumerFormatImageMaker.convertFileToEightBitImage(dicomFile.getAbsolutePath(), output.getAbsolutePath(), "png");
            File filesDir = new File(decTempDir);
            return filesDir.listFiles((d, f) -> f.toLowerCase().endsWith(".png"));
        }catch(Exception error){
            // DO NOTHING
        }

        return null;
    }

    /**
     * Deletes files of the specified type in the given directory
     *
     * @param   type   The type of file to be deleted e.g .png
     * @param   dir   The directory containing the files to delete
     */
    public void deleteSpecificFiles(String type, String dir){
        File filesDir = new File(dir);
        File[] files = filesDir.listFiles((d, f) -> f.toLowerCase().endsWith(type.toLowerCase()));

        for(File file : files){
            file.delete();
        }
    }

}
