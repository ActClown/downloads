
    多线程断点下载：顾名思义是用多线程实现的，断点是当第三方因素（断电、断网等）中断下载时，
下次下载可以继续上次下载的地方下载。
1、通过getContentLength可以获取要下载文件的大小，这样可以在本机上创建一个相同大小的文件用来下载。

int fileLength = connection.getContentLength();

2、由于是多线程，所以要给每一个线程均分分配要下载的位置。
复制代码

for(int i = 0; i < threadCount; i ++) {
    int startThread = i * blockSize;
    int endThread = (i + 1) * blockSize - 1;
    if( i == blockSize - 1) endThread = fileLength -1;
    new DownloadThread(i, startThread, endThread).start();

}

复制代码

3、启动每个线程下载时，请求头需要Range参数，值是bytes:xxx-xxx某事。比如"Range:0-10100",代表要下载的位置是从0到10100。

connection.setRequestProperty("Range", "bytes:"+startThred+"-" + endThread);

4、然后每次用RandomAccessFile写入数据到本机文件里。

while((length = inputStream.read(buffer)) != -1) {
    randomAccessFile.write(buffer, 0, length);
}



5、当然每次下载时需要记录本线程下载了多少，以便断点时，下载的时候可以从下次下载的地方下载。

total += length;
int currentThreadPostion = startThred + total;
RandomAccessFile randomAccessFile2 = new RandomAccessFile(file, "rwd");
randomAccessFile2.write(String.valueOf(currentThreadPostion).getBytes());
randomAccessFile2.close();

