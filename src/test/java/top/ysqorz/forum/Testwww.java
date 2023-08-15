package top.ysqorz.forum;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import top.ysqorz.forum.ForumApplication;
import top.ysqorz.forum.dto.req.RegisterDTO;
import top.ysqorz.forum.service.UserService;

import java.util.Date;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = ForumApplication.class)
public class Testwww {

    @Autowired
    private UserService userService;

    @Test
    public void initDataForTest() {

        RegisterDTO req = new RegisterDTO();
        req.setEmail("790716890@qq.com");
        req.setPassword("  sdasd");
        req.setUsername("  sdasd");
        req.setRePassword("  sdasd");
        req.setCaptcha("  sdasd");
        req.setToken("  sdasd");
        userService.register(req);
    }




}
