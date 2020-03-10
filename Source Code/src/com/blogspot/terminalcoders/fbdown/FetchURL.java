package com.blogspot.terminalcoders.fbdown;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

public class FetchURL {

    private final String fburl;
    private String fileName, vidURL;
    URL url;
    int contentLength;
    JProgressBar progressBar;
    Document html;

    public FetchURL(String fburl, JProgressBar progressBar) {
        this.fburl = fburl;
        this.progressBar = progressBar;
    }

    public void readHTML() {
        try {
            if (!fburl.contains("facebook")) {
                JOptionPane.showMessageDialog(null, "Enter Facebook video URL. \nRight click on video and select 'Copy video URL at current time' option. \nPaste the URL in the form.", "Enter correct URL", JOptionPane.ERROR_MESSAGE);
                return;
            }
            this.fileName = Paths.get(new URI(fburl).getPath()).getFileName().toString() + ".mp4";
            html = Jsoup.connect(fburl).get();
            vidURL = html.select("meta[property=og:video]").first().attr("content");
            parseHTML();
        } catch (Exception e) {
            int opt = JOptionPane.showConfirmDialog(null, "The URL Seems private video. \nOpen the copied URL in browser and press Ctrl+U. \nCopy all the content and paste in next form.", "Failed to open URL", JOptionPane.OK_CANCEL_OPTION);
            if (opt == JOptionPane.OK_OPTION) {
                SourceCodeInput sci = new SourceCodeInput(this);
                sci.setVisible(true);
                sci.setLocationRelativeTo(null);
            }
        }
    }

    void parseHTML() throws Exception {
        url = new URL(vidURL);
        HttpURLConnection connection;
        contentLength = -1;
        try {
            connection = (HttpURLConnection) url.openConnection();
            contentLength = connection.getContentLength();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Error Message", JOptionPane.ERROR_MESSAGE);
        }
        if(contentLength==-1){
            JOptionPane.showMessageDialog(null, "Failed to locate video file. \nTry again.", "Error Message", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Runnable downloadFile = new Runnable() {
            public void run() {
                DownloadFile df = new DownloadFile();
            }
        };
        Thread t = new Thread(downloadFile);
        t.start();
    }
    
    void setHTML(String html){
        //this.html = html;
        Pattern pattern = Pattern.compile("sd_src_no_ratelimit:\"(.+?)\""); 
        Matcher m = pattern.matcher(html); 
        m.find();
        vidURL = m.group(1);
        /*while (m.find()) {
            for (int i = 1; i <= m.groupCount(); i++) {
                //
            }
        }*/
        try {
            parseHTML();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.toString(), "Error Message", JOptionPane.ERROR_MESSAGE);
        }
    }

    interface ProgressCallBack {

        public void updateProgress(DownloadByteFile rbc, int progress);
    }

    class DownloadFile implements ProgressCallBack {

        ReadableByteChannel rbc;

        public DownloadFile() {
            try {
                rbc = new DownloadByteFile(Channels.newChannel(url.openStream()), contentLength, this);
                FileOutputStream fos = new FileOutputStream(fileName);
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, e.toString(), "Error Message", JOptionPane.ERROR_MESSAGE);
            }
        }

        @Override
        public void updateProgress(DownloadByteFile rbc, int progress) {
            progressBar.setValue(progress);
            if (progress == 100) {
                JOptionPane.showMessageDialog(null, "File downloaded successfully", "Success Message", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    class DownloadByteFile implements ReadableByteChannel {

        long fileSize;
        ReadableByteChannel rbc;
        long sizeRead;
        ProgressCallBack pcb;

        DownloadByteFile(ReadableByteChannel rbc, long fileSize, ProgressCallBack pcb) {
            this.fileSize = fileSize;
            this.rbc = rbc;
            this.pcb = pcb;
        }

        @Override
        public int read(ByteBuffer bb) throws IOException {
            int n;
            double progress;
            if ((n = rbc.read(bb)) > 0) {
                sizeRead += n;
                progress = fileSize > 0 ? (double) sizeRead / (double) fileSize * 100.0 : -1.0;
                pcb.updateProgress(this, (int) progress);
            }
            return n;
        }

        @Override
        public boolean isOpen() {
            return rbc.isOpen();
        }

        @Override
        public void close() throws IOException {
            rbc.close();
        }

    }
}
