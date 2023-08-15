package top.ysqorz.forum;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import top.ysqorz.forum.dto.req.RegisterDTO;
import top.ysqorz.forum.service.PostService;
import top.ysqorz.forum.service.UserService;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = ForumApplication.class)
public class TestPost {

    @Autowired
    private PostService postService;

    @Test
    public void initDataForTest() {


    }




}
