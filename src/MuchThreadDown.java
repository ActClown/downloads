import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.text.StyledEditorKit.BoldAction;

public class MuchThreadDown {

    /**
     * @param args
     */
    //线程数
    private static int threadCount = 3;
    //每个线程要下载的大小
    private static int blockSize;
    //要下载的文件
    private static String path = "http://192.168.0.112:8080/itheima74/feiq.exe";
    //当前的线程数
    private static int currentRunThreadCount = 0;
    public static void main(String[] args) {

        try {
            URL url = new URL(path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10 * 1000);
            int code = connection.getResponseCode();
            if(code == 200) {
                int fileLength = connection.getContentLength();
                RandomAccessFile randomAccessFile = new RandomAccessFile(new File(getFileName(path)), "rw");
                randomAccessFile.setLength(fileLength);
                blockSize = fileLength / threadCount;
                for(int i = 0; i < threadCount; i ++) {
                    int startThread = i * blockSize;
                    int endThread = (i + 1) * blockSize - 1;
                    if( i == blockSize - 1) endThread = fileLength -1;
                    new DownloadThread(i, startThread, endThread).start();

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public static class DownloadThread extends Thread {
        private int threadId;
        private int endThread;
        private int startThred;
        public DownloadThread(int threadId, int startThred, int endThread) {
            this.threadId = threadId;
            this.startThred = startThred;
            this.endThread = endThread;
        }
        public void run() {
            synchronized (DownloadThread.class) {
                currentRunThreadCount += 1;
            }
            //分段请求网络连接，分段保存在本地
            try {
                System.err.println("理论线程:"+threadId+",开始位置:"+startThred+",结束位置:"+endThread);
                URL url = new URL(path);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10 * 1000);
                File file = new File(threadId+".txt");
                if(file.exists()) {    //是否断点
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                    String lastPostion_str = bufferedReader.readLine();
                    startThred = Integer.parseInt(lastPostion_str);
                    bufferedReader.close();
                }
                //设置分段下载的头信息  Range:做分段
                connection.setRequestProperty("Range", "bytes:"+startThred+"-" + endThread);
                int code = connection.getResponseCode();
                System.out.println(code);
                if(code == 206) {    //200:请求全部资源成功  206:代表部分资源请求成功
                    InputStream inputStream = connection.getInputStream();
                    System.out.println(getFileName(path));
                    RandomAccessFile randomAccessFile = new RandomAccessFile(new File(getFileName(path)), "rw");
                    randomAccessFile.seek(startThred);
                    byte[] buffer = new byte[1024*10];
                    int length = -1;
                    int total = 0;//记录下载的总量
                    System.err.println("实际线程:"+threadId+",开始位置:"+startThred+",结束位置:"+endThread);
                    while((length = inputStream.read(buffer)) != -1) {
                        randomAccessFile.write(buffer, 0, length);
                        total += length;
                        int currentThreadPostion = startThred + total;
                        RandomAccessFile randomAccessFile2 = new RandomAccessFile(file, "rwd");
                        randomAccessFile2.write(String.valueOf(currentThreadPostion).getBytes());
                        randomAccessFile2.close();
                    }
                    randomAccessFile.close();
                    inputStream.close();
                    System.err.println("线程:"+threadId+"下载完毕");
                    synchronized (DownloadThread.class) {
                        currentRunThreadCount -= 1;
                        if(currentRunThreadCount == 0){
                            for(int i = 0; i < threadCount; i ++) {
                                File file2 = new File(i+".txt");
                                file2.delete();
                            }
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }


            super.run();
        }
    }

    public static String getFileName(String path) {
        return path.substring(path.lastIndexOf("/")+1);
    }

}