import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;

public class MainForm implements UIUpdateCallBack {
    private JPanel MainPanel;
    private JTextField txtFileName;
    private JButton apriButton;
    private JButton browseEApriButton;
    private JList<String> lstImages;
    private JLabel txtImage;
    private JLabel lblStatusBar;
    private JList<String> lstImageDimensions;
    private JLabel lblImgTrovate;
    private JButton salvaTutteButton;
    private JButton cancellaTuttoButton;
    private JSplitPane pnlSplit;
    private JScrollPane pnlScrollImage;
    private JSlider slidZoom;
    private JCheckBox adattaCheckBox;
    private File selectedFile;
    private ArrayList<HARParser.HARImageItem> harImageArray;
    HARParser.HARImageItem currentImageItem;

    private final long widthMinDefault = 0;
    private final long widthMaxDefault = Long.MAX_VALUE;

    private static final String IMG_LIST_ALL = "--- TUTTE ---";

    private float mainResizeScale=1.0f;

    ResourceBundle resourceBundle = ResourceBundle.getBundle("strings");

    public MainForm() {

        // Inizializzazione GUI
        txtImage.setText("");
        lblStatusBar.setText(" ");
        salvaTutteButton.setEnabled(false);

        // Click & drag
        HandScrollListener scrollListener = new HandScrollListener(txtImage);
        pnlScrollImage.getViewport().addMouseMotionListener(scrollListener);    // Drag
        pnlScrollImage.getViewport().addMouseListener(scrollListener);          // Cambio cursore

        // Slider labels
        Hashtable<Integer,JLabel> sliderLabels = new Hashtable<>();
        sliderLabels.put (1,new JLabel("Zoom"));
        sliderLabels.put (10,new JLabel("Full size"));
        for (int i=20;i<=100;i+=10){
            sliderLabels.put (i,new JLabel((100-i)/10+"/10"));
        }
        sliderLabels.put (100,new JLabel("Min"));

        //sliderLabels.put (Integer.valueOf(10),new JLabel("1/10"));
        slidZoom.setLabelTable(sliderLabels);
        slidZoom.setPaintLabels(true);
        slidZoom.setPaintTicks(true);

        /////// DEBUG
        //txtFileName.setText("C:\\Users\\utente\\Downloads\\reader.paperlit.com_Archive [22-04-05 09-04-41].har");
        /////////////

        pnlScrollImage.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                mostraImmagineCorrente(mainResizeScale);
            }
        });

        lstImages.addListSelectionListener(e -> {
            // Esco subito se la JList non è in uno stato stabile
            if (e.getValueIsAdjusting()) {
                return;
            }

            String selectedValue = lstImages.getSelectedValue();

            if (selectedValue == null) {
                return;
            }

            int id = Integer.parseInt(selectedValue.split("-")[0].trim());
            currentImageItem = harImageArray.get(id);

            if (currentImageItem.getSize() > 0) {

                float resizeScale=mostraImmagineCorrente(mainResizeScale);

                lblStatusBar.setText(
                        currentImageItem.getID() + " - dimensione: " + currentImageItem.getSize() + " - larghezza x altezza: " +
                                currentImageItem.getWidth() + " x " + currentImageItem.getHeight()+
                        " - scala di riduzione: "+resizeScale);

            }
        });

        lstImageDimensions.addListSelectionListener(e -> {
            // Esco subito se la JList non è in uno stato stabile
            if (e.getValueIsAdjusting() || lstImageDimensions.getSelectedValue() == null) {
                return;
            }

            String selectedValue = lstImageDimensions.getSelectedValue();
            long selectedWidthMin = widthMinDefault;
            long selectedWidthMax = widthMaxDefault;

            if (selectedValue.contains(" x ")) {
                String[] widthHeight = lstImageDimensions.getSelectedValue().split("x");

                // lstImages.clearSelection();
                selectedWidthMin = Long.parseLong(widthHeight[0].trim());
                selectedWidthMax = selectedWidthMin;
            }

            int numImgAggiornate = aggiornaListaImmagini(selectedWidthMin, selectedWidthMax);

            lblImgTrovate.setText(resourceBundle.getString("lblImmaginiTrovate") + " " + numImgAggiornate);
        });

        browseEApriButton.addActionListener(e -> {

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(
                    new File(AppPrefs.LoadFileLocation.get(
                            System.getProperty("user.home")
                    ))
            );

            int userChoose = fileChooser.showOpenDialog(MainPanel);
            if (userChoose == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
                AppPrefs.LoadFileLocation.put(selectedFile.getParentFile().getAbsolutePath());  // TODO: Fare attenzione se il file chooser è per le directory, questo codice vale per i file
            } else {
                return;
            }

            txtFileName.setText(selectedFile.getAbsolutePath());
            apriFile(selectedFile);
        });

        apriButton.addActionListener(e -> {
            File temp = new File(txtFileName.getText());
            if (temp.exists()) {
                selectedFile = temp;
                apriFile(temp);
            } else {
                JOptionPane.showMessageDialog(MainPanel, "Il file non esiste", "Errore", JOptionPane.ERROR_MESSAGE);
            }
        });

        salvaTutteButton.addActionListener(e -> {
            JFileChooser dirChooser = new JFileChooser();
            dirChooser.setCurrentDirectory(
                    new File(AppPrefs.SaveFolderLocation.get(
                            System.getProperty("user.home")
                    ))
            );
            dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            dirChooser.setAcceptAllFileFilterUsed(false);

            File selectedDir;

            int userChoose = dirChooser.showOpenDialog(MainPanel);
            if (userChoose == JFileChooser.APPROVE_OPTION) {
                selectedDir = dirChooser.getSelectedFile();
                if (selectedDir.isFile()){
                    selectedDir=selectedDir.getParentFile();
                }

                AppPrefs.SaveFolderLocation.put(selectedDir.getAbsolutePath());  // TODO: Fare attenzione se il file chooser è per le directory, questo codice vale per i file

                // TODO: Fare attenzione e testare se in Linux può rotnare sempre true per le dir "." e ".."
                if (Objects.requireNonNull(selectedDir.list()).length > 0) {
                    JOptionPane.showMessageDialog(MainPanel, "La directory non è vuota, mi fermo", "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                   // salvaTutteLeImmagini(selectedDir);
                    SalvaImmaginiWorker salvaImmaginiWorker = new SalvaImmaginiWorker(selectedDir);
                    salvaImmaginiWorker.execute();
                }
                //JOptionPane.showMessageDialog(MainPanel,"TODO");
            } else {
                return;
            }
        });

        cancellaTuttoButton.addActionListener(e -> {
            DefaultListModel<String> temp = new DefaultListModel<>();
            lstImageDimensions.setModel(temp);
            temp = new DefaultListModel<>();
            lstImages.setModel(temp);
            salvaTutteButton.setEnabled(false);
            slidZoom.setEnabled(false);
            adattaCheckBox.setEnabled(false);
            txtImage.setIcon(null);
        });

        slidZoom.addChangeListener(e -> {
            if (slidZoom.getValueIsAdjusting()){
                return;
            }

            float tempScaleFactor = slidZoom.getValue()/10.0f;
            if (tempScaleFactor==0){
                mainResizeScale=1;
            } else {
                mainResizeScale = tempScaleFactor;
            }
            mostraImmagineCorrente(mainResizeScale);

            lblStatusBar.setText(
                    currentImageItem.getID() + " - dimensione: " + currentImageItem.getSize() + " - larghezza x altezza: " +
                            currentImageItem.getWidth() + " x " + currentImageItem.getHeight()+
                            " - scala di riduzione: "+mainResizeScale);

        });


        adattaCheckBox.addItemListener(e -> {

            if (e.getStateChange()== ItemEvent.SELECTED) {
                mainResizeScale=-1;
                slidZoom.setEnabled(false);
            } else {
                mainResizeScale=slidZoom.getValue()/10.0f;
                slidZoom.setEnabled(true);
            }
            mostraImmagineCorrente(mainResizeScale);

        });

    }

    /**
     * Mostra l'immagine scalata
     * @param customResizeScale 1 - full size, tra o e 1 zoomma maggiore di 1 scala la dimensione proporzionalmente, -1 per il calcolo automatico
     * @return la dimensione calcolata se (customResizeScale == -1) oppure il customResizeScale
     */
    private float mostraImmagineCorrente(float customResizeScale) {
        if (currentImageItem==null){
            return customResizeScale;
        }

        BufferedImage image = currentImageItem.getDecodedImage();
        float resizeScale;

        if (customResizeScale<0) {
            resizeScale = Math.max(
                    (1.0f * image.getHeight()) / pnlScrollImage.getHeight(),
                    (1.0f * image.getWidth()) / pnlScrollImage.getWidth()
            );
        } else{
            resizeScale = customResizeScale;
        }
        Image scaledImage= image.getScaledInstance((int)(image.getWidth()/resizeScale),(int)(image.getHeight()/resizeScale),Image.SCALE_SMOOTH);
        ImageIcon imageIcon = new ImageIcon(scaledImage);
        txtImage.setIcon(imageIcon);
        return resizeScale;
    }

    //////////////////////////////////////////////////////
    /////////////////// Business logic ///////////////////
    //////////////////////////////////////////////////////

    /**
     * Apre il file HAR e lo parsa
     *
     * @param selectedFile
     */
    private void apriFile(File selectedFile) {

        lstImages.setModel(new DefaultListModel<>());
        lstImageDimensions.setModel(new DefaultListModel<>());

        // Lancia il worker per aprire e parsare il file
        HARParserSwingWorker harParserSwingWorker = new HARParserSwingWorker(selectedFile, this);
        harParserSwingWorker.execute();

    }

    /**
     * Azioni da compiere una volta completato il parsing
     *
     * @param harParser
     */
    @Override
    public void completed(HARParser harParser) {
        harImageArray = harParser.getHarImageArray();
        aggiornaListaDimensioni();
        int numImgAggiornate = aggiornaListaImmagini(widthMinDefault, widthMaxDefault);

        lblImgTrovate.setText(resourceBundle.getString("lblImmaginiTrovate") + " " + numImgAggiornate);

        if (numImgAggiornate > 0) {
            salvaTutteButton.setEnabled(true);
            slidZoom.setEnabled(false);
            adattaCheckBox.setEnabled(true); // Parto con l'auto adapter
            mainResizeScale=-1;
        }
    }

    /**
     * Aggiorna la JList con le dimensioni delle immagini trovate
     */
    private void aggiornaListaDimensioni() {
        if (harImageArray.isEmpty()) {
            return;
        }

        DefaultListModel<String> defaultListModel = new DefaultListModel<>();
        HashSet<String> dimensioniImmagini = new HashSet<>();

        dimensioniImmagini.add(IMG_LIST_ALL);

        for (HARParser.HARImageItem imageEncoded : harImageArray) {
            dimensioniImmagini.add(imageEncoded.getWidth() + " x " + imageEncoded.getHeight());
        }

        defaultListModel.addAll(dimensioniImmagini);
        lstImageDimensions.setModel(defaultListModel);
    }

    /**
     * Aggiorna la JList delle immagini
     *
     * @param widthMin
     */
    public int aggiornaListaImmagini(long widthMin, long widthMax) {
        DefaultListModel<String> defaultListModel = new DefaultListModel<>();

        int i = 0;
        for (HARParser.HARImageItem imageEncoded : harImageArray) {
            if (imageEncoded.getWidth() >= widthMin && imageEncoded.getWidth() <= widthMax) {
                defaultListModel.add(i++,
                        imageEncoded.getID() + " - " + imageEncoded.getMimeType() + " - " + imageEncoded.getImageEncodingType());
            }
        }

        // lstImages.clearSelection();
        lstImages.setModel(defaultListModel);
        return i;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("MainForm");
        frame.setContentPane(new MainForm().MainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // make the frame half the height and width
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int height = screenSize.height;
        int width = screenSize.width;
        frame.setSize(width / 2, height / 2);
        frame.setLocationRelativeTo(null); // Centra nello schermo

        frame.pack();
        frame.setVisible(true);
    }

    @Override
    public void update(String message) {
        lblStatusBar.setText(message);
    }

    ///////////////////// Internal swing worker per il salvataggio delle immagini
    /**
     * Salva tutte le immagini nella directory specificata come parametro
     */
    class SalvaImmaginiWorker extends SwingWorker<String, String> {
        File saveDir;

        public SalvaImmaginiWorker(File saveDir) {
            this.saveDir = saveDir;
        }

        @Override
        protected String doInBackground() {

                ListModel<String> listModel = lstImages.getModel();
                String listElement;
                int imgIndex;
                int imgAll=listModel.getSize();
                HARParser.HARImageItem imageItem;
                BufferedImage tempImage;

                boolean qualcheErrore = false;

                for (int i = 0; i < imgAll; i++) {
                    listElement = listModel.getElementAt(i);
                    imgIndex = Integer.parseInt(listElement.split("-")[0].trim());
                    imageItem = harImageArray.get(imgIndex);
                    tempImage = imageItem.getDecodedImage();

                    File imageItemFile = new File(this.saveDir, imgIndex + ".jpg");
                    try {
                        ImageIO.write(tempImage, "jpg", imageItemFile);
                        publish("Salvo l'immagine "+(i+1)+" di "+ imgAll);
                        //lblStatusBar.setText("Salvo l'immagine "+(i+1)+" di "+ imgAll);
                    } catch (IOException e) {
                        qualcheErrore = true;
                        e.printStackTrace();
                    }
                    //System.out.println(imgIndex+" - "+listElement); // DEBUG
                }
                if (qualcheErrore) {
                    JOptionPane.showMessageDialog(MainPanel, "C'è stato qualche errore salvando in " + saveDir.getAbsolutePath(), "Errore", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(MainPanel, "Salvate " + listModel.getSize() + " immagini in\n" + saveDir.getAbsolutePath(), "Tutto ok", JOptionPane.PLAIN_MESSAGE);
                }
            return null;
        }

        @Override
        protected void process(List<String> chunks) {
            super.process(chunks);
            lblStatusBar.setText(chunks.get(0));
        }
    }
}
