import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class HARParserSwingWorker extends SwingWorker<HARParser,String> {
    File selectedFile;
    UIUpdateCallBack callBack;
    HARParser harParser;

    public HARParserSwingWorker(File selectedFile, UIUpdateCallBack callBack) {
        this.selectedFile = selectedFile;
        this.callBack = callBack;
    }

    @Override
    protected HARParser doInBackground() throws Exception {
        harParser = HARParser.createHARParser(selectedFile,callBack);
        try {
            harParser.parse();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return harParser;
    }

    @Override
    protected void process(List<String> chunks) {
        super.process(chunks);
        callBack.update(chunks.get(0)); // TODO: Da implementare la call di tutta la lista
    }

    @Override
    protected void done() {
        super.done();
        callBack.completed(harParser);
    }
}
