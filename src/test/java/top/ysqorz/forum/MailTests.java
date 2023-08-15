package top.ysqorz.forum;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import top.ysqorz.forum.utils.MailClient;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = ForumApplication.class)
public class MailTests {

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Test
    public void testTextMail(){
        String to = "20215227073@stu.suda.edu.cn";
        String subject = "TEST";
        String content = "WELCOME.";
        mailClient.sendMail(to,subject,content);
    }

    @Test
    public void testHtmlMail(){
        Context context = new Context();
        context.setVariable("username","LZY");

        String process = templateEngine.process("/mail/demo", context);
        System.out.println(process);
        String to = "20215227073@stu.suda.edu.cn";
        String subject = "TEST";
        mailClient.sendMail(to,subject,process);
    }

}
