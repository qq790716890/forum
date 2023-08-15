package top.ysqorz.forum.upload;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author passerbyYSQ
 * @create 2021-05-18 1:22
 */
public interface UploadRepository {

    /**
     * @param inputStream
     * @param filename      文件名。包含后缀
     * @return
     */
    String[] uploadImage(InputStream inputStream, String filename);

    default void getImage(String fileName, HttpServletResponse response) throws IOException {

    }

    /**
     * @param inputStream
     * @param filePath      文件的完整路径
     */
    void upload(InputStream inputStream, String filePath);

}
