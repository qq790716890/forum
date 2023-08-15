package top.ysqorz.forum.upload;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletResponse;
import java.io.*;

/**
 * @author passerbyYSQ
 * @create 2021-05-18 22:58
 */
@Component
public class LocalRepository implements UploadRepository {

    @Value("${upload.local.path}")
    private String uploadPath;

    @Override
    public String[] uploadImage(InputStream inputStream, String filename) {
        try {
            File destDir = new File(uploadPath);

            if (!destDir.exists()) {
                destDir.mkdirs(); // 递归创建创建多级
            }
            File destFile = new File(destDir, filename);
            upload(inputStream, destFile.getAbsolutePath());
            String original = generateUrl(filename);

            // 制作缩略图
            Thumbnails.of(destFile)
                    .size(256, 256)
                    .keepAspectRatio(true)
                    .toFile(new File(destDir, "thumb_" + filename));
            String thumb = generateUrl("thumb_" + filename);

            return new String[]{original, thumb};

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void getImage(String fileName, HttpServletResponse response) throws IOException {
        fileName = uploadPath + "/" + fileName;
        String suffix = fileName.substring(fileName.lastIndexOf('.'));
        response.setContentType("image/" + suffix);

        try (
                FileInputStream fis = new FileInputStream(fileName);
                OutputStream os = response.getOutputStream();
        ) {
            byte[] buffer = new byte[1024];
            int b = 0;
            while ((b = fis.read(buffer)) != -1) {
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            throw new IOException("读取图片失败");
        }
    }

    private String generateUrl(String filePath) {
        return ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/upload/images/" + filePath)
                .queryParam("timestamp", System.currentTimeMillis())
                .toUriString();

    }

    @Override
    public void upload(InputStream inputStream, String filePath) {
        FileOutputStream outStream = null;
        try {
            outStream = new FileOutputStream(filePath);
            FileCopyUtils.copy(inputStream, outStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (outStream != null) {
                outStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
