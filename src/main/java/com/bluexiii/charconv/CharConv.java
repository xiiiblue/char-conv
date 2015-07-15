package com.bluexiii.charconv;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * NIO字符集转换类
 *
 * @author BlueXIII
 */
public class CharConv {
    private String inChar;// 解码
    private String outChar;// 编码
    
    private FileInputStream fis;// 文件输入流:读取文件中内容
    private FileChannel in;// 文件通道:双向,流从中而过
    private FileChannel out;// 文件通道:双向,流从中而过
    private FileOutputStream fos;// 文件输出流:向文件中写入内容
    private final ByteBuffer b = ByteBuffer.allocate(1024 * 3);// 设置缓存区的大小

    private Charset inSet;// 解码字符集
    private Charset outSet;// 编码字符集
    private CharsetDecoder de;// 解码器
    private CharsetEncoder en;// 编码器
    private CharBuffer convertion;// 中间的字符数据
    private ByteBuffer temp = ByteBuffer.allocate(1024 * 3);// 设置缓存区的大小:临时
    private final byte[] by = new byte[1024];
    private InputStreamReader isr;
    private final char[] ch = new char[1024];

    public String getInChar() {
        return inChar;
    }

    public void setInChar(String inChar) {
        this.inChar = inChar;
    }

    public String getOutChar() {
        return outChar;
    }

    public void setOutChar(String outChar) {
        this.outChar = outChar;
    }

    public CharConv() {
        //默认GBK转UTF-8
        this.inChar = "gbk";
        this.outChar = "utf-8";
    }

    public CharConv(String inChar, String outChar) {
        this.inChar = inChar;
        this.outChar = outChar;
    }

    /**
     * 目录文件转换
     * @param srcPath 源目录
     * @param destPath 目标目录
     * @param extension 扩展名
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public List<String> convertDir(String srcPath, String destPath, String extension) throws FileNotFoundException, IOException {
        List<String> retLog = new ArrayList<>();
        try {
            List<String> relativePaths = CharConv.getRelativePaths(srcPath, extension);
            for (String relativePath : relativePaths) {
                try {
                    //新建目录
                    Path newDir = Paths.get(destPath, relativePath).getParent();
                    Files.createDirectories(newDir);
                    //单文件转换
                    this.convert(srcPath + relativePath , destPath  + relativePath);
                    retLog.add(srcPath + relativePath);
                } catch (IOException ex) {
                    Logger.getLogger(CharConv.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (IOException ex) {
            //Logger.getLogger(CharConv.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        }
        return retLog;
    }
    
    /**
     * 单文件字符转换
     *
     * @param src 源路径
     * @param dest 目的路径
     * @throws java.io.FileNotFoundException
     */
    public void convert(String src, String dest) throws FileNotFoundException, IOException {
        try {
            fis = new FileInputStream(src);
            in = fis.getChannel();
            fos = new FileOutputStream(dest);
            out = fos.getChannel();
            Logger.getLogger(CharConv.class.getName()).log(Level.INFO, "==src:{0}  ==dest:{1}", new Object[]{src, dest});

            //编码器、解码器
            inSet = Charset.forName(inChar);
            outSet = Charset.forName(outChar);
            de = inSet.newDecoder();
            en = outSet.newEncoder();

            while (fis.available() > 0) {
                b.clear();// 清除标记
                in.read(b); // 将文件内容读入到缓冲区内:将标记位置从0-b.capacity(),
                // 读取完毕标记在0-b.capacity()之间
                b.flip();// 调节标记,下次读取从该位置读起
                convertion = de.decode(b);// 开始编码

                temp.clear();// 清除标记
                temp = en.encode(convertion);
                b.flip(); // 将标记移到缓冲区的开始,并保存其中所有的数据:将标记移到开始0
                out.write(temp); // 将缓冲区内的内容写入文件中:从标记处开始取出数据
            }
        } catch (FileNotFoundException ex) {
            //Logger.getLogger(CharConv.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        } catch (IOException ex) {
            //Logger.getLogger(CharConv.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
            }

            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
            }

            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
            }

            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
            }
        }

    }

    /**
     * 测试转码是否成功, 指定字符集读取文件
     *
     * @param path
     * @param charset
     * @throws IOException
     */
    private void read(String path, String charset) throws IOException {
        fis = new FileInputStream(path);
        isr = new InputStreamReader(fis, charset);
        while (fis.available() > 0) {
            int length = isr.read(ch);
            System.out.println(new String(ch));
        }
    }
    
    /**
     * 获取目录下所有文件相对路径
     * @param basePath
     * @param extension
     * @return
     * @throws IOException 
     */
    private static List<String> getRelativePaths(final String basePath, final String extension) throws IOException {
        final List<String> fileList = new ArrayList();
        Files.walkFileTree(Paths.get(basePath), new SimpleFileVisitor<Path>() {
            // 访问文件时候触发该方法
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {

                String absolutefileName = path.toString();
                if (absolutefileName.toLowerCase().endsWith(extension)) {
                    String relativeFileName = absolutefileName.replace(basePath, "");
                    fileList.add(relativeFileName);
                }

                return FileVisitResult.CONTINUE;
            }
        });
        return fileList;
    }

    public static void main(String[] args) {
        //测试getRelativePaths
        /*List<String> fileList;
        try {
            fileList = CharConv.getRelativePaths("d:\\Downloads\\source\\", "txt");
            for (String file : fileList) {
                System.out.println(file);
            }
        } catch (IOException ex) {
            Logger.getLogger(FileProcess.class.getName()).log(Level.SEVERE, null, ex);
        }*/
        
        //测试convert
        /*CharConv n = new CharConv();
        try {
            n.convert("d:\\Downloads\\new3.txt", "d:\\Downloads\\new4.txt");
            //n.read("C:/nio_write.txt", "utf-8");// 正确
            //n.read("C:/nio_write.txt", "gbk");//乱码  
        } catch (IOException ex) {
            Logger.getLogger(CharConv.class.getName()).log(Level.SEVERE, null, ex);
        }*/
        
        //测试convertDir
        /*CharConv n = new CharConv("gbk","utf-8");
        try {
            n.convertDir("d:\\Downloads\\source", "d:\\Downloads\\destiny", "txt");
        } catch (IOException ex) {
            Logger.getLogger(CharConv.class.getName()).log(Level.SEVERE, null, ex);
        }*/
        
        // 创建多级目录
        /*Path newdir2 = Paths.get("d:\\Downloads\\destiny\\sub","new4.txt").getParent();
        System.out.println(newdir2);
        try {
            Files.createDirectories(newdir2); 
        } catch (IOException e) {  
            System.err.println(e);  
        }  */
    }

}
